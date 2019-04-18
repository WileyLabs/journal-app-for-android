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

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class StickyHeaderComponent {
    private static final String TAG = ArticleRefComponent.class.getSimpleName();

    private Theme theme;

    private View stickyHeader;

    private Set<Integer> headerInitialPositions;
    private Map<Integer, View> headers;

    public StickyHeaderComponent(Theme theme) {
        this.theme = theme;
    }

    public void refreshStickyHeaders(Context context, int scrollPosition) {
        refreshStickyHeaders(context, scrollPosition, 0, false);
    }

    public void refreshStickyHeaders(Context context, int scroll, int offset) {
        refreshStickyHeaders(context, scroll, offset, true);
    }

    public void refreshStickyHeaders(Context context, int scroll, int offset, boolean hideNonStickyHeaders) {
        if (headerInitialPositions == null) {
            return;
        }

        Integer stickyHeaderId = null;
        int stickyHeaderRealPosition = Integer.MIN_VALUE;
        boolean isStickyHeaderPositionSet = false;
        for (Integer initialPosition : headerInitialPositions) {
            final int headerPosition = offset - scroll +initialPosition;

            if ((headerPosition) < 0 && headerPosition > stickyHeaderRealPosition) {
                stickyHeaderRealPosition = headerPosition;
                stickyHeaderId = initialPosition;
            }

            View headerView = headers.get(initialPosition);
            if ((headerPosition) > 0 && (headerPosition) <= getHeaderHeight(context)) {
                stickyHeader = getPrevHeader(initialPosition);
                headerView.setY(headerPosition);
                if (stickyHeader != null) {
                    stickyHeader.setY(headerPosition - getHeaderHeight(context));
                    stickyHeader.setVisibility(View.VISIBLE);
                }
                isStickyHeaderPositionSet = true;
            } else if ((headerPosition) <= 0 && (headerPosition) > -1 * getHeaderHeight(context)) {
                stickyHeader = headerView;
                stickyHeader.setY(0);
                stickyHeader.setVisibility(View.VISIBLE);
                isStickyHeaderPositionSet = true;
            } else {
                if (hideNonStickyHeaders) {
                    headerView.setVisibility(View.GONE);
                } else {
                    headerView.setY(headerPosition);
                }
                headerView.setY(headerPosition);
            }
        }

        if (!isStickyHeaderPositionSet && stickyHeaderId != null) {
            stickyHeader = headers.get(stickyHeaderId);
            stickyHeader.setY(0);
            stickyHeader.setVisibility(View.VISIBLE);
        }
    }

    public void initStickyHeaders(final ArticleComponentHost componentHost, final CustomWebView webView, final RelativeLayout relativeLayout) {
        webView.setOnContentHeightChangedListener(new CustomWebView.OnContentHeightChangedListener() {
            @Override
            public void onContentHeightChanged() {
                getHeadersInfo(componentHost, webView, relativeLayout, true);
            }
        });

        getHeadersInfo(componentHost, webView, relativeLayout, true);
    }

    public void initStickyHeaders(final ArticleComponentHost componentHost, final CustomWebView webView) {
        final RelativeLayout relativeLayout;
        if (webView.getParent().getClass() == RelativeLayout.class) {
            relativeLayout = (RelativeLayout) webView.getParent();
        } else {
            return;
        }

        webView.setOnContentHeightChangedListener(new CustomWebView.OnContentHeightChangedListener() {
            @Override
            public void onContentHeightChanged() {
                getHeadersInfo(componentHost, webView, relativeLayout, false);
            }
        });

        getHeadersInfo(componentHost, webView, relativeLayout, false);
    }

    private void getHeadersInfo(final ArticleComponentHost componentHost, final CustomWebView webView,
                                final RelativeLayout relativeLayout, final boolean hideHeaders) {
        componentHost.onRenderStarted();
        getHeadersInfo(webView, new CustomWebView.JavaScriptExecutionJsonCallback() {
            @Override
            public void onJavaScriptResult(JSONObject json) {
                try {
                    if (headers != null) {
                        removeAllHeadersFromLayout(relativeLayout);
                    }

                    headers = new HashMap<>(json.length());
                    headerInitialPositions = new TreeSet<>();
                    Iterator keys = json.keys();
                    while (keys.hasNext()) {
                        final String key = (String) keys.next();
                        final int initialPosition = UIUtils.dpToPx(componentHost.getContext(), Integer.parseInt(key));

                        boolean journalHasDarkBackground = theme.isJournalHasDarkBackground();
                        final TextView headerView = new TextView(componentHost.getContext());
                        headerView.setGravity(Gravity.CENTER_VERTICAL);
                        headerView.setTextSize(UIUtils.pxToDp(componentHost.getContext(), (int) componentHost.getContext().getResources().getDimension(R.dimen.issues_list_year_separator_txt_size)));
                        headerView.setTextColor(componentHost.getContext().getResources().getColor(journalHasDarkBackground ? R.color.list_article_header_title_color : R.color.list_article_header_title_color_dark));
                        headerView.setPadding((int) componentHost.getContext().getResources().getDimension(R.dimen.article_list_header_padding_left), 0, 0, 0);

                        if (journalHasDarkBackground) {
                            headerView.setBackgroundColor(ColorUtils.brighterColorByPercent(theme.getMainColor(), 75));
                        } else {
                            headerView.setBackgroundColor(theme.getMainColor());
                        }
                        headerView.setText(json.getString(key).trim());

                        if (hideHeaders) {
                            headerView.setVisibility(View.GONE);
                        }

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getHeaderHeight(componentHost.getContext()));

                        relativeLayout.addView(headerView, params);
                        if (initialPosition == 0) {
                            stickyHeader = headerView;
                        }
                        headers.put(initialPosition, headerView);
                        headerInitialPositions.add(initialPosition);
                    }

                    if (stickyHeader == null && headerInitialPositions.size() > 0) {
                        stickyHeader = headers.get(headerInitialPositions.iterator().next());
                    }

                } catch (Exception e) {
                    Logger.s(TAG, e);
                }

                if (!hideHeaders) {
                    refreshStickyHeaders(componentHost.getContext(), webView.getScrollY());
                }
                componentHost.onRenderCompleted();
            }
        });
    }

    private void getHeadersInfo(CustomWebView webView, CustomWebView.JavaScriptExecutionJsonCallback callback) {
        webView.executeJavaScriptAndGetJsonResult("return getHeadersInfo();", callback);
    }

    private View getPrevHeader(Integer headerId) {
        final int position = headerId;
        if (position == headerInitialPositions.iterator().next()) {
            return null;
        }

        int prevHeaderInitialPosition = Integer.MIN_VALUE;
        for (Integer initialPosition : headerInitialPositions) {
            if (position > initialPosition && initialPosition > prevHeaderInitialPosition) {
                prevHeaderInitialPosition = initialPosition;
            }
        }

        return headers.get(prevHeaderInitialPosition);
    }

    private int getHeaderHeight(Context context) {
        return UIUtils.dpToPx(context, context.getResources().getInteger(R.integer.article_list_header_height));
    }

    private void removeAllHeadersFromLayout(RelativeLayout relativeLayout) {
        for (View header : headers.values()) {
            relativeLayout.removeView(header);
        }
    }
}
