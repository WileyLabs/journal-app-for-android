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
package com.wiley.android.journalApp.fragment.articleView;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalListFragment;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.search.ArticleSearcher;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.FiguresHandler;
import com.wiley.wol.client.android.data.http.DocumentsDownloader;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.domain.entity.SupportingInfoMO;

/**
 * Created by taraskreknin on 10.06.14.
 */
public class CenterBarListFragment extends JournalListFragment {

    @Inject
    private WebController mWebController;
    @Inject
    private DocumentsDownloader mDocumentDownloader;

    private ArticleSearcher mSearcher;
    private TextView mPlaceholderText;
    private FiguresHandler mFiguresHandler;
    private final AdapterView.OnItemClickListener onFigureClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            FigureMO figure = (FigureMO) parent.getAdapter().getItem(position);
            mFiguresHandler.scrollToFigure(figure);
        }
    };
    private final AdapterView.OnItemLongClickListener onFigureLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            FigureMO figure = (FigureMO) parent.getAdapter().getItem(position);
            mFiguresHandler.showFigure(figure);
            return true;
        }
    };

    private AdapterView.OnItemClickListener onReferenceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    };

    private final AdapterView.OnItemClickListener onSupportingInfoClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SupportingInfoMO info = (SupportingInfoMO) parent.getAdapter().getItem(position);
            mDocumentDownloader.getSupportingInfo(info);
        }
    };

    private final DataSetObserver dataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            mPlaceholderText.setVisibility(getListAdapter().isEmpty() ? View.VISIBLE : View.GONE);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFiguresHandler = (FiguresHandler) getParentFragment();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFiguresHandler = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_center_bar_article_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DeviceUtils.isTablet(getActivity())) {
            mSearcher = (ArticleSearcher) getFragmentManager().findFragmentById(R.id.article_searcher_fragment);
            hideSearcher();
        }
        mPlaceholderText = findView(R.id.article_center_menu_placeholder);
        hidePlaceHolder();
    }

    private void hideSearcher() {
        if (mSearcher != null) {
            mSearcher.hideSearcher(false);
        }
    }

    public void showArticleInfoContent(BaseAdapter infoAdapter) {
        hideSearcher();
        hidePlaceHolder();
        clearClickListeners();
        setListAdapterAndResetDataObserver(infoAdapter, false);
    }

    public void showPlaceholderWithMessage(String msg) {
        hideSearcher();
        clearClickListeners();
        setListAdapterAndResetDataObserver(null, false);
        mPlaceholderText.setVisibility(View.VISIBLE);
        mPlaceholderText.setText(msg);
    }

    private void clearClickListeners() {
        getListView().setOnItemClickListener(null);
        getListView().setOnLongClickListener(null);
    }

    public void showArticleFiguresContent(BaseAdapter figuresAdapter) {
        hideSearcher();
        setListAdapterAndResetDataObserver(figuresAdapter, true);
        getListView().setOnItemClickListener(onFigureClickListener);
        getListView().setOnItemLongClickListener(onFigureLongClickListener);
        if (figuresAdapter.getCount() == 0) {
            showListIsEmptyMessage(R.string.no_figures);
        } else {
            hidePlaceHolder();
        }
    }

    private void setListAdapterAndResetDataObserver(BaseAdapter adapter, boolean readdObserver) {
        if (adapter != getListAdapter()) {
            removeObserverFromAdapter();
            setListAdapter(adapter);
            if (readdObserver) {
                addObserverToAdapter();
            }
        }
    }

    public void resetAdapter() {
        setListAdapter(getListAdapter());
    }

    public void showReferencesContent(BaseAdapter referencesAdapter) {
        hideSearcher();
        setListAdapterAndResetDataObserver(referencesAdapter, true);
        getListView().setOnItemClickListener(onReferenceClickListener);
        getListView().setOnItemLongClickListener(null);
        if (referencesAdapter.getCount() == 0) {
            showListIsEmptyMessage(R.string.no_references);
        } else {
            hidePlaceHolder();
        }
    }

    public void showArticleSupportingInfoContent(BaseAdapter supportingInfoAdapter) {
        hideSearcher();
        setListAdapterAndResetDataObserver(supportingInfoAdapter, true);
        getListView().setOnItemClickListener(onSupportingInfoClickListener);
        getListView().setOnItemLongClickListener(null);
        if (supportingInfoAdapter.getCount() == 0) {
            showListIsEmptyMessage(R.string.no_supporting_info);
        } else {
            hidePlaceHolder();
        }
    }

    public void showSearcherContent(CustomWebView webView) {
        if (mSearcher == null) {
            mSearcher = (ArticleSearcher) getFragmentManager().findFragmentById(R.id.article_searcher_fragment);
        }

        if (mSearcher != null) {
            hidePlaceHolder();
            mSearcher.showSearcher(webView);
            setListAdapterAndResetDataObserver(mSearcher.getAdapter(), false);
        }
    }

    private void addObserverToAdapter() {
        if (getListAdapter() != null) {
            getListAdapter().registerDataSetObserver(dataSetObserver);
        }
    }

    private void removeObserverFromAdapter() {
        if (getListAdapter() != null) {
            try {
                getListAdapter().unregisterDataSetObserver(dataSetObserver);
            } catch (IllegalStateException ignored) {}
        }
    }

    private void hidePlaceHolder() {
        mPlaceholderText.setVisibility(View.GONE);
    }

    private void showListIsEmptyMessage(int resId) {
        mPlaceholderText.setVisibility(View.VISIBLE);
        mPlaceholderText.setText(resId);
    }
}