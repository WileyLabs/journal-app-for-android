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

import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;
import com.wiley.android.journalApp.controller.FeedsController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.settings.Settings;

import roboguice.RoboGuice;

/**
 * Created by admin on 18/12/14.
 */
public class FeedItemDetailsComponent {

    @Inject
    protected HomePageService mHomePageService;
    @Inject
    private FeedsController mFeedsController;
    @Inject
    private Settings mSettings;
    @Inject
    private Theme mTheme;
    @Inject
    private WebController mWebController;
    private static final Templates sTemplates = new Templates();

    protected CustomWebView mWebView;
    protected HomePageHost mHost;

    public FeedItemDetailsComponent(HomePageHost host, CustomWebView webView) {
        mHost = host;
        mWebView = webView;
        setupWebView();
        RoboGuice.injectMembers(mHost.getActivity(), this);
    }

    private void setupWebView() {
        mWebView.setInitialScale(1);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http")) {
                    mWebView.loadUrl(url);
                }
                return true;
            }
        });
    }

    public void render(final String description) {
        mWebView.loadData(generateHtml(description));
    }

    public void loadOriginalContent(final String url) {
        mWebView.loadUrl(url);
    }

    private String generateHtml(final String description) {
        return description;
    }
}
