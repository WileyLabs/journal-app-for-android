/*  Journal App for Android
 *  Copyright (C) 2019 John Wiley & Sons, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wiley.android.journalApp.components;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.wol.client.android.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CustomWebView extends WebView {

    private static final String TAG = CustomWebView.class.getSimpleName();
    private int currentContentHeight = 0;
    private OnContentHeightChangedListener onContentHeightChangedListener;

    public static String makeJsSafeString(final String str) {
        final StringBuilder builder = new StringBuilder();
        boolean inWhitespace = false;
        for (int index = 0; index < str.length(); index++) {
            final char c = str.charAt(index);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\"':
                    builder.append("\\\"");
                    break;
                case '\t':
                case '\r':
                case '\n':
                    if (!inWhitespace) {
                        builder.append(" ");
                        inWhitespace = true;
                    }
                    break;
                default:
                    builder.append(c);
                    inWhitespace = false;
                    break;
            }
        }
        return builder.toString();
    }

    protected void initJavascriptSupport() {
        if (isInEditMode()) {
            return;
        }
        getSettings().setJavaScriptEnabled(true);
        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(final ConsoleMessage consoleMessage) {
                Logger.s("WebView JavaScript", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }

            @Override
            public boolean onJsAlert(final WebView view, final String url, final String message, final JsResult result) {
                final Context context = view.getContext();
                new AlertDialog.Builder(context)
                        .setTitle("WebView JavaScript")
                        .setMessage(message)
                        .show();
                result.confirm();
                return true;
            }
        });

        addJavascriptInterface(javaScriptCallbackInterface, "hostCallbacks");
    }

    protected void releaseJavascriptSupport() {
        releaseJavascriptCallbacks();
    }

    protected void releaseJavascriptCallbacks() {
        this.removeJavascriptInterface("hostCallbacks");
        javaScriptCallbackHandlers.clear();
    }

    public void scrollToElement(final String elementId, int topOffset) {
        final String js = String.format("$('body').scrollTo('#%s', 500%s);",
                elementId,
                topOffset != 0 ? ", {offset:" + topOffset + "}" : "");
        executeJavaScript(js);
    }

    public void scrollToElement(final String elementId) {
        scrollToElement(elementId, 0);
    }

    public void scrollToElementUsingSmoothScrollJs(final String elementId) {
        final String js = String.format(
                "var el = document.getElementById(\"%s\"); if (Scroller) { Scroller.scrollToElement(el); } else { el.scrollIntoView(true); }",
                elementId);
        executeJavaScript(js);
    }

    public void updateHtmlFor(final String selector, final String html) {
        final String js = String.format("$('%s').html(\"%s\");", selector, makeJsSafeString(html));
        executeJavaScript(js);
    }

    public void setElementPropertyValue(final String elementId, final String propertyName, final String propertyValue) {
        final String javaScript = String.format("document.getElementById(\"%s\").%s = \"%s\";", elementId, propertyName, propertyValue);
        executeJavaScript(javaScript);
    }

    public void setElementVisibility(final String selector, final boolean visible) {
        final String javaScript = String.format("$('%s').%s();", selector, visible ? "show" : "hide");
        executeJavaScript(javaScript);
    }

    public void executeJavaScript(final String javaScript) {
        assert (getSettings().getJavaScriptEnabled());
        Logger.d(TAG, "execute javaScript: \n" + javaScript);
        final String javaScriptUrl = "javascript:(function() { try{" + javaScript + "}catch(err){hostCallbacks.handleJavascriptErrors(err.message)}})()";
        loadUrl(javaScriptUrl);
    }

    public void scrollToPositionDp(int xDp, int yDp) {
        final String js = String.format("$('body').scrollTo({left:%d, top:%d}, 500);", xDp, yDp);
        executeJavaScript(js);
    }

    public void setContentHeightLimitDp(int newContentHeightDp) {
        executeJavaScript("$('.article_body').css({'height':'" + newContentHeightDp + "px', 'overflow':'hidden'});");
    }

    public void resetContentHeightLimit() {
        executeJavaScript("$('.article_body').css({'height':'initial', 'overflow':'initial'});");
    }

    public interface JavaScriptExecutionCallback {
        void onJavaScriptResult(String result);
    }

    public void executeJavaScriptAndGetResult(final String beforeScript, final String javaScript, final JavaScriptExecutionCallback callback) {
        final String handlerId = IdUtils.generateId();
        final String wrappedJs = String.format("%s var result = (function() {%s})(); hostCallbacks.onResult(\"%s\", result);", beforeScript, javaScript, handlerId);
        final JavaScriptCallbackHandler handler = new JavaScriptCallbackHandler(handlerId, callback);
        javaScriptCallbackHandlers.add(handler);
        executeJavaScript(wrappedJs);
    }

    public void executeJavaScriptAndGetResult(final String javaScript, final JavaScriptExecutionCallback callback) {
        executeJavaScriptAndGetResult("", javaScript, callback);
    }

    public interface JavaScriptExecutionJsonCallback {
        void onJavaScriptResult(JSONObject json);
    }

    public void executeJavaScriptAndGetJsonResult(final String javaScript, final JavaScriptExecutionJsonCallback callback) {
        final JavaScriptExecutionCallback callbackWrapper = new JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(final String result) {
                JSONObject json = null;
                try {
                    json = new JSONObject(result);
                } catch (final JSONException e) {
                    Logger.s(TAG, e);
                }
                callback.onJavaScriptResult(json);
            }
        };
        executeJavaScriptAndGetResult(javaScript, callbackWrapper);
    }

    protected void onJavaScriptResult(final String handlerId, final String result) {
        JavaScriptCallbackHandler handler = null;
        for (final JavaScriptCallbackHandler h : javaScriptCallbackHandlers) {
            if (h.id.equals(handlerId)) {
                handler = h;
                break;
            }
        }
        if (handler != null) {
            javaScriptCallbackHandlers.remove(handler);
            handler.callback.onJavaScriptResult(result);
        }
    }

    private final JavaScriptCallbackInterface javaScriptCallbackInterface = new JavaScriptCallbackInterface(this);

    private static class JavaScriptCallbackHandler {
        public final String id;
        public final JavaScriptExecutionCallback callback;

        public JavaScriptCallbackHandler(final String id, final JavaScriptExecutionCallback callback) {
            this.id = id;
            this.callback = callback;
        }
    }

    private final List<JavaScriptCallbackHandler> javaScriptCallbackHandlers = new ArrayList<JavaScriptCallbackHandler>();

    public interface OnScrollListener {
        void onScrollStarted(int l, int t);

        void onScrollChanged(int l, int t, int oldl, int oldt);

        void onScrollEnded(int l, int t);
    }

    public static class OnScrollListenerBase implements OnScrollListener {
        @Override
        public void onScrollStarted(final int l, final int t) {
        }

        @Override
        public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        }

        @Override
        public void onScrollEnded(int l, int t) {
        }
    }

    private static final long TIME_TO_POST_SCROLL_ENDED_MSC = 500L;

    private String mimeType;
    private String encoding;
    private OnScrollListener onScrollListener = null;
    private boolean mScrolling = false;
    private boolean mTouching = false;
    private final Runnable postScrollEnd = new Runnable() {
        @Override
        public void run() {
            if (mScrolling && !mTouching) {
                mScrolling = false;
                notifyScrollingEnded();
                return;
            }
            postDelayed(this, TIME_TO_POST_SCROLL_ENDED_MSC);
        }
    };

    public CustomWebView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    protected void init(final AttributeSet attrs) {
        mimeType = attrs.getAttributeValue(null, "mime_type");
        encoding = attrs.getAttributeValue(null, "encoding");
        initJavascriptSupport();
    }

    @Override
    public void destroy() {
        super.destroy();
        releaseJavascriptSupport();
    }

    public Bundle getExtras() {
        return javaScriptCallbackInterface.getExtras();
    }

    public String pushTempData(String data) {
        return javaScriptCallbackInterface.pushTempData(data);
    }

    public String popTempData(String name) {
        return javaScriptCallbackInterface.popTempData(name);
    }

    public OnScrollListener getOnScrollListener() {
        return onScrollListener;
    }

    public void setOnScrollListener(final OnScrollListener listener) {
        this.onScrollListener = listener;
    }

    /**
     * original WebView.loadData works incorrect it doesn't sense encoding
     */
    @Override
    public void loadData(final String data, final String mimeType, final String encoding) {
        loadDataWithBaseURL(null, data, mimeType, encoding, null);
    }

    public void loadData(final String data) {
        loadData(data, mimeType, encoding);
    }

    public void loadDataByInnerHtml(final String data) {
        javaScriptCallbackInterface.setNextBodyInnerHtml(data);
        final String js = "document.documentElement.innerHTML = hostCallbacks.getNextBodyInnerHtml();";
        executeJavaScript(js);
    }

    public void loadHighlightedHtml(final String hHtml) {
        loadDataByInnerHtml(hHtml);
    }

    public void highlightElement(final String elementId) {
        final String js = String.format("highlightElement('%s');", elementId);
        executeJavaScript(js);
    }

    public void unhighlightElement(final String elementId) {
        final String js = String.format("dehighlightElement('%s');", elementId);
        executeJavaScript(js);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mTouching = false;
        } else {
            mTouching = true;
        }
        return super.onTouchEvent(event);
    }

    public void callSuperOnScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        removeCallbacks(postScrollEnd);
        if (!mScrolling) {
            mScrolling = true;
            notifyScrollingStarted(oldl, oldt);
        }
        notifyScrollChanged(l, t, oldl, oldt);
        postDelayed(postScrollEnd, TIME_TO_POST_SCROLL_ENDED_MSC);
    }

    private void notifyScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        if (onScrollListener != null) {
            onScrollListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    private void notifyScrollingStarted(final int froml, final int fromt) {
        if (onScrollListener != null) {
            onScrollListener.onScrollStarted(froml, fromt);
        }
    }

    private void notifyScrollingEnded() {
        if (onScrollListener != null) {
            onScrollListener.onScrollEnded(getScrollX(), getScrollY());
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return getExtras().getBoolean("bodyScrolled", false);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (onContentHeightChangedListener != null && currentContentHeight != getContentHeight()) {
            currentContentHeight = getContentHeight();
            onContentHeightChangedListener.onContentHeightChanged();
        }
    }

    public void setOnContentHeightChangedListener(OnContentHeightChangedListener onContentHeightChangedListener) {
        this.onContentHeightChangedListener = onContentHeightChangedListener;
    }

    public interface OnContentHeightChangedListener {
        void onContentHeightChanged();
    }
}
