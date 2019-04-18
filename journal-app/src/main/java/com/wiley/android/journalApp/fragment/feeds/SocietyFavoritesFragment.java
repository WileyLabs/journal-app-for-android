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
package com.wiley.android.journalApp.fragment.feeds;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.base.Society;
import com.wiley.android.journalApp.components.ArticleComponentHost;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.FeedListComponent;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SocietyFavoritesFragment
        extends
        JournalFragment
        implements
        ArticleComponentHost,
        ActionBarSherlock.OnCreatePanelMenuListener,
        Society {

    private static final String TAG_LIFE = SocietyFavoritesFragment.class.getSimpleName() + ".LIFE";

    private View progress;
    private FeedListComponent component;
    private Handler hideProgressHandler = new Handler();

    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private HomePageService homePageService;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;
    @Inject
    private Settings settings;
    private ImageLoader imageLoader = ImageLoader.getInstance();


    private NotificationProcessor societyFavoritesCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            component.render(homePageService.getFavoriteItems());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onCreateView()");
        return inflater.inflate(R.layout.frag_society_favorites, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);

        final CustomWebView webView = findView(R.id.saved_articles_content_view);
        component = new FeedListComponent(this, webView, true);

        progress = findView(R.id.progress);
        showProgress();
        List<FeedItemMO> feedItems = homePageService.getFavoriteItems();

        component.render(feedItems);

        // action bar
        float titlePaddingLeft = DeviceUtils.isPhone(getContext()) ?
                getActivity().getResources().getDimension(R.dimen.action_bar_title_padding_left) : 0;
        final ActionBarSherlockCompat mSherlock = new ActionBarSherlockCompat(getJournalActivity(), 0,
                (ViewGroup) findView(R.id.abs__screen_action_bar));
        mSherlock.setupActionBar()
                .setBackgroundDrawable(theme.getMainColor())
                .setTitleActionBar(getResources().getString(R.string.society_favorites_title), (int) titlePaddingLeft);
        mSherlock.setListener(this);

        hideProgress();

        // feature: quick link menu
        if (DeviceUtils.isPhone((getJournalActivity()))) {
            // touch layout
            ((TouchRefreshLayout) findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    showQuickLinkMenu();
                }
            });

            quickLinkMenuComponent.initQuickLink(getActivity(), this);
        }

        showSocietyLogo();
    }

    @Override
    public void onStart() {
        Logger.d(TAG_LIFE, "onStart()");
        super.onStart();

        notificationCenter.subscribeToNotification(EventList.SOCIETY_FAVORITES_COUNT_CHANGED.getEventName(),
                societyFavoritesCountChangedProcessor);

    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();

        // feature: quick link menu
        showQuickLinkMenu();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Logger.d(TAG_LIFE, "onHiddenChanged(): hidden = " + hidden);
        super.onHiddenChanged(hidden);

        // feature: quick link menu
        if (!hidden) {
            showQuickLinkMenu();
        }
    }

    @Override
    public void onPause() {
        Logger.d(TAG_LIFE, "onPause()");
        super.onPause();
    }

    @Override
    public void onStop() {
        Logger.d(TAG_LIFE, "onStop()");
        super.onStop();

        notificationCenter.unSubscribeFromNotification(societyFavoritesCountChangedProcessor);

    }

    @Override
    public void onDestroyView() {
        Logger.d(TAG_LIFE, "onDestroyView()");
        super.onDestroyView();
    }

    /**
     * feature: quick link menu
     */
    private void showQuickLinkMenu() {
        quickLinkMenuComponent.showQuickLinkMenu();
    }

    protected void showProgress() {
        if (progress == null) {
            return;
        }
        hideProgressHandler.removeCallbacksAndMessages(null);
        progress.setVisibility(View.VISIBLE);
    }

    protected void hideProgress() {
        if (progress == null) {
            return;
        }
        hideProgressHandler.removeCallbacksAndMessages(null);
        hideProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                fadeOut.setDuration(500);
                progress.startAnimation(fadeOut);
            }
        }, 200);
    }

    @Override
    public void onAccessForbiddenArticle() {
    }

    @Override
    public void onSaveArticleNoInternetConnection(ArticleMO article) {
    }

    @Override
    public StartActivityForResultHelper getStartActivityForResultHelper() {
        return null;
    }

    @Override
    public Context getContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void onRenderStarted() {

    }

    @Override
    public void onRenderCompleted() {

    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {

        if (DeviceUtils.isPhone(this.getActivity())) {
            menu.add(getString(R.string.action_show_menu))
                    .setIcon(ActionBarUtils.getMenuIconResource(theme))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ((MainActivity) getJournalActivity()).onSideMenuButtonClicked();
                            return true;
                        }
                    });
        }

        return true;
    }

    private void showSocietyLogo() {
        final String societyFooterLogoImageUrl = settings.getSocietyFooterLogoImageUrl();
        if (societyFooterLogoImageUrl != null) {
            final ImageView societyLogo = findView(R.id.society_logo);
            String logoImageUrl = Uri.fromFile(new File(societyFooterLogoImageUrl)).toString();
            imageLoader.displayImage(logoImageUrl, societyLogo);
        }
    }
}
