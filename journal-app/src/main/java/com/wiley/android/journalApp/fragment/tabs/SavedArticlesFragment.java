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
package com.wiley.android.journalApp.fragment.tabs;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.ArticleRefComponent;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.SavedArticlesComponent;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.List;
import java.util.Map;

/**
 * Created by taraskreknin on 23.06.14.
 */
public class SavedArticlesFragment extends BaseTabArticleComponentHostFragment {

    @Inject
    private ArticleService mArticleService;
    @Inject
    private NotificationCenter mNotificationCenter;
    private ArticleRefComponent mArticlesList;

    private NotificationProcessor mSavedArticlesListChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateUi();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_saved_articles, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CustomWebView webView = findView(R.id.saved_articles_content_view);
        mArticlesList = new SavedArticlesComponent(this, webView);
        mArticlesList.onCreateHost();
        updateUi();
        mNotificationCenter.subscribeToNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), mSavedArticlesListChanged);
        GANHelper.trackEvent(GANHelper.EVENT_FAVORITES,
                GANHelper.ACTION_OPEN,
                GANHelper.LABEL_MENU,
                0L);
    }

    @Override
    public void onDestroyView() {
        mNotificationCenter.unSubscribeFromNotification(mSavedArticlesListChanged);
        mArticlesList.onDestroyHost();
        super.onDestroyView();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mArticlesList.executePostponedTasks();
        }
    }

    private void updateUi() {
        List<ArticleMO> saved = mArticleService.getSavedArticles();
        mArticlesList.render(saved);
    }

    @Override
    public void onStart() {
        super.onStart();
        mArticlesList.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mArticlesList.onStop();
    }

    @Override
    protected int getTabId() {
        return R.id.saved_articles_tab;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mArticlesList.onOrientationChanged();
    }

    @Override
    public void onRenderStarted() {
        showProgress();
    }

    @Override
    public void onRenderCompleted() {
        hideProgress();
    }
}
