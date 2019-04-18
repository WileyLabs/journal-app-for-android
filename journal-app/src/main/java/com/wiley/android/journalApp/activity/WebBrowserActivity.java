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
package com.wiley.android.journalApp.activity;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.ActivityWithActionBar;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.utils.UIUtils;

/**
 * Created by taraskreknin on 07.05.14.
 */
public class WebBrowserActivity extends ActivityWithActionBar implements View.OnClickListener {

    protected static final String EXTRA_START_URL = "param_starting_url";
    protected static final String EXTRA_SHOW_OPEN_IN_BROWSER = "param_show_open_in_browser";

    @Inject
    protected WebController mWebController;
    protected WebView mWebView;
    protected View mBottomBar;
    protected ImageButton mBack, mForward, mReload;
    protected ProgressBar mCircleProgress;
    protected String mStartingUrl = "";
    protected String mCurrentUrl = mStartingUrl;
    protected boolean mShowOpenInBrowser = true;

    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            int progress = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * newProgress;
            setProgress(progress);
        }
    };
    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mCircleProgress.setVisibility(View.VISIBLE);
            mCurrentUrl = url;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mCircleProgress.setVisibility(View.GONE);
            updateBottomBar();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            showErrorMessage(failingUrl, description);
        }
    };

    protected void showErrorMessage(String failingUrl, String description) {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.error_loading_page))
               .append(" ")
               .append(failingUrl)
               .append(".");
        if (!TextUtils.isEmpty(description)) {
            builder.append(" ").append(description);
        }
        UIUtils.showShortToast(this, builder.toString());
    }

    public static Intent getStartingIntent(Context context, String startingUrl, boolean showOpenInBrowser) {
        Intent intent = new Intent(context, WebBrowserActivity.class);
        intent.putExtra(EXTRA_START_URL, TextUtils.isEmpty(startingUrl) ? "" : startingUrl);
        intent.putExtra(EXTRA_SHOW_OPEN_IN_BROWSER, showOpenInBrowser);
        return intent;
    }

    public static Intent getStartingIntent(Context context, String startingUrl) {
        return getStartingIntent(context, startingUrl, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void setupActionBar(ActionBar actionBar) {
        super.setupActionBar(actionBar);
        setTitle("");
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.web_activity);
        mWebView = findView(R.id.web_view);
        mBack = findView(R.id.web_go_back);
        mBack.setOnClickListener(this);
        mBottomBar = findView(R.id.web_bottom_bar);
        mForward = findView(R.id.web_go_forward);
        mForward.setOnClickListener(this);
        mReload = findView(R.id.web_reload_page);
        mReload.setOnClickListener(this);
        mCircleProgress = findView(R.id.web_progress);
        mCircleProgress.setVisibility(View.GONE);
        processIntent();
        setupWebView();
        load();
    }

    private void setupWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(mWebViewClient);

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimetype, final long contentLength) {
                if ("application/pdf".equals(mimetype)) {
                    final String newUrl = "http://drive.google.com/viewerng/viewer?embedded=true&url=" + url;
                    mWebView.loadUrl(newUrl);
                }
            }
        });
    }

    protected void load() {
        mWebView.loadUrl(mStartingUrl);
    }

    @Override
    protected boolean canRotateToLandscape() {
        return true;
    }

    protected void processIntent() {
        Intent intent = getIntent();
        mStartingUrl = intent.getExtras().getString(EXTRA_START_URL, "");
        mShowOpenInBrowser = intent.getBooleanExtra(EXTRA_SHOW_OPEN_IN_BROWSER, true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.web_browser_menu, menu);
        if (!mShowOpenInBrowser) {
            menu.findItem(R.id.open_in_browser).setVisible(false).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.open_in_browser) {
            if (mShowOpenInBrowser) {
                mWebController.openUrlExternalIfCan(mCurrentUrl);
            }
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.web_go_back) {
            goBack();
        } else if (id == R.id.web_go_forward) {
            goForward();
        } else if (id == R.id.web_reload_page) {
            reloadPage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        updateBottomBar();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    private void reloadPage() {
        mWebView.reload();
    }

    private void goForward() {
        if (mWebView.canGoForward()) {
            mWebView.goForward();
        }
    }

    private void goBack() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        }
    }

    private void updateBottomBar() {
        mBack.setEnabled(mWebView.canGoBack());
        mForward.setEnabled(mWebView.canGoForward());
    }
}