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
package com.wiley.android.journalApp.fragment.access;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.wiley.android.journalApp.R;
import com.wiley.wol.client.android.data.utils.GANHelper;

/**
 * Created by taraskreknin on 27.06.14.
 */
public abstract class BaseCDScreenFragment extends AbstractScreenFragment {

    private WebView mWebView;
    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http")) {
                mWebController.openUrlInternal(url);
            } else {
                mWebController.openUrlExternal(url);
            }
            return true;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_c_d, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        mWebView = findView(R.id.get_access_web_view);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // getting rid of webview render bug
        loadMessageToWebView();

        final View descMessageView = findView(R.id.get_access_desc_message_root_view);
        descMessageView.setVisibility(needDescriptionMessage() ? View.VISIBLE : View.GONE);
        final View moreInfoBlockView = findView(R.id.get_access_more_info_block);
        moreInfoBlockView.setVisibility(needMoreInfoBlock() ? View.VISIBLE : View.GONE);
        findView(R.id.get_access_log_in_to_wol).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GANHelper.trackEventWithoutOrientation(getGANHelperEvent(), GANHelper.ACTION_LINK, GANHelper.LABEL_WOL_LOGIN, -1L);
                onNeedWolLogIn();
            }
        });
    }

    protected void loadMessageToWebView() {
        mWebView.loadDataWithBaseURL(null, getMessageHtml(), "text/html", "utf-8", null);
    }

    protected abstract String getMessageHtml();
    protected abstract boolean needMoreInfoBlock();
    protected abstract boolean needDescriptionMessage();

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    public void updateUi() {
        super.updateUi();
        loadMessageToWebView();
    }

    @Override
    protected void openPreviousScreen() {
        getAccessDialogFragment().backToScreenB();
    }
}
