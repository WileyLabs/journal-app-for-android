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
package com.wiley.android.journalApp.fragment.figures;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.ArticleHtmlUtils;
import com.wiley.wol.client.android.journalApp.theme.Theme;

public class FigureTableFragment extends FigureFragment {
    private final static String TAG = FigureTableFragment.class.getSimpleName();
    public static FigureTableFragment newInstance(int figureIndex) {
        FigureTableFragment fragment = new FigureTableFragment();
        Bundle args = new Bundle();
        args.putInt(Argument_FigureIndex, figureIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Inject
    private AANHelper aanHelper;
    @Inject
    protected Theme theme;
    @Inject
    protected WebController webController;
    @Inject
    protected EmailSender emailSender;

    protected final Templates templates = new Templates();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_figure_table, container, false);
    }

    private CustomWebView tableView;

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        tableView = findView(R.id.table);
        tableView.setWebViewClient(tableViewClient);
        String caption = figure.getCaption();
        if (TextUtils.isEmpty(caption)) {
            tableView.setVisibility(View.GONE);
        } else {
            tableView.setVisibility(View.VISIBLE);
            caption = ArticleHtmlUtils.expandLinksInFigureCaption(caption, figure, theme.isJournalHasNoReferenceNumbers());
            String html = templates.useAssetsTemplate(getActivity(), "table_caption")
                    .putParam("caption_body", caption)
                    .proceed();
            tableView.loadData(html);
        }
    }

    private WebViewClient tableViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http")) {
                webController.openUrlInternal(url);
                return true;
            } else if (url.startsWith("openfig")) {
                Uri uri = Uri.parse(url);
                String figureShortCaption = uri.getHost();
                getHost_().openFigureByShortCaption(figureShortCaption);
                return true;
            } else if (url.startsWith("bodytouched")) {
                getHost_().toggleUiFullscreen();
                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }
    };
}
