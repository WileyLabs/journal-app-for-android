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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.EarlyViewComponent;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.widget.SwipeRefreshLayout;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.Map;

import static com.wiley.wol.client.android.notification.EventList.ALL_CONTENT_UPDATE_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.ALL_CONTENT_UPDATE_STARTED;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

public class EarlyViewContent extends BaseTabArticleComponentHostFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = EarlyViewContent.class.getSimpleName();

    @Inject
    private ArticleService articleService;
    @Inject
    private ErrorManager errorManager;
    private EarlyViewComponent articleRefComponent;
    private SwipeRefreshLayout swipeLayout;
    private boolean firstShow = false;

    private final NotificationProcessor successProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "successProcessor.processNotification(final HashMap params)");
            updateUi(true);
            swipeLayout.setRefreshing(false);
        }
    };

    private final NotificationProcessor allContentUpdateStartedProcessor= new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            showProgress();
        }
    };

    private final NotificationProcessor notModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            Logger.d(TAG, "notModifiedProcessor.processNotification(final HashMap params)");
            swipeLayout.setRefreshing(false);
            hideProgress();
        }
    };

    private final NotificationProcessor errorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "errorProcessor.processNotification(final HashMap params)");
            swipeLayout.setRefreshing(false);
            hideProgress();
            Logger.s(TAG, (Throwable) params.get(ERROR));
        }
    };

    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.article_ref_content_swipe, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUi();
        updateUi(true);
    }

    protected void initUi() {
        Logger.d(TAG, "initUi()");
        final CustomWebView webView = findView(R.id.articleRefContent);
        final View progress = findView(R.id.progress);
        this.articleRefComponent = new EarlyViewComponent(this, webView);
        this.articleRefComponent.onCreateHost();

        swipeLayout = findView(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setScrollQualifier(articleRefComponent);
        int mainColor = theme.getMainColor();
        int color1 = ColorUtils.modifyHsv(mainColor, 0.5f, 1.0f);
        int color2 = ColorUtils.modifyHsv(mainColor, 0.75f, 1.0f);
        int color3 = ColorUtils.modifyHsv(mainColor, 1.0f, 1.0f);
        int color4 = ColorUtils.modifyHsv(mainColor, 1.0f, 0.75f);
        swipeLayout.setColorSchemeWithColors(color1, color2, color3, color4);

        progress.setVisibility(View.GONE);
    }

    protected void updateUi(boolean showProgressIfNeed) {
        Logger.d(TAG, "updateUi(" + showProgressIfNeed + ")");
        if (showProgressIfNeed) {
            showProgress();
        }
        articleRefComponent.update();
    }

    @Override
    public void onStart() {
        super.onStart();
        articleRefComponent.onStart();
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_NOT_MODIFIED.getEventName(), notModifiedProcessor);
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_ERROR.getEventName(), errorProcessor);
        notificationCenter.subscribeToNotification(ALL_CONTENT_UPDATE_STARTED.getEventName(), allContentUpdateStartedProcessor);
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_UPDATED.getEventName(), successProcessor);
        notificationCenter.subscribeToNotification(ALL_CONTENT_UPDATE_FINISHED.getEventName(), successProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(notModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(errorProcessor);
        notificationCenter.unSubscribeFromNotification(allContentUpdateStartedProcessor);
        notificationCenter.unSubscribeFromNotification(successProcessor);
        articleRefComponent.onStop();
    }

    @Override
    public void onDestroyView() {
        articleRefComponent.onDestroyHost();
        super.onDestroyView();
    }

    @Override
    public void onShow() {
        super.onShow();
        GANHelper.trackPageView("/home/early-view", true);
        if (null != articleRefComponent && !firstShow) {
            firstShow = true;
            articleRefComponent.update();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            articleRefComponent.executePostponedTasks();

            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk <= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        articleRefComponent.refreshStickyHeaders();
                    }
                }, 10);
            }
        }
    }

    @Override
    protected int getTabId() {
        return R.id.early_view_tab;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.articleRefComponent.onOrientationChanged();
    }

    @Override
    public void onRefresh() {
        GANHelper.trackEvent(GANHelper.EVENT_APP,
                GANHelper.ACTION_REFRESH,
                GANHelper.LABEL_USER_GENERATED,
                0L);
        swipeLayout.setRefreshing(true);
        articleService.updateEarlyViewFeed();
        articleRefComponent.refreshStickyHeaders();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final boolean handled = articleRefComponent.onActivityResult(requestCode, resultCode, data);
        if (!handled) {
            super.onActivityResult(requestCode, resultCode, data);
        }
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