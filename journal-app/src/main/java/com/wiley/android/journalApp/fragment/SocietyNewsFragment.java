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
package com.wiley.android.journalApp.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.base.Society;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.HomePageComponent;
import com.wiley.android.journalApp.components.HomePageHost;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.android.journalApp.fragment.announcement.AnnouncementFragment;
import com.wiley.android.journalApp.progress.ProgressHandler;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.http.UpdateManager;
import com.wiley.wol.client.android.data.service.AnnouncementService;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import java.io.File;
import java.util.Map;

public class SocietyNewsFragment extends JournalFragment implements HomePageHost, Society {

    private static final String TAG = SocietyNewsFragment.class.getSimpleName();
    private static final String TAG_LIFE = SocietyNewsFragment.class.getSimpleName() + ".LIFE";

    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private UpdateManager updateManager;
    @Inject
    private AnnouncementService announcementService;
    @Inject
    private Settings settings;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;
    @Inject
    private ErrorManager errorManager;

    private ImageLoader imageLoader = ImageLoader.getInstance();
    private InterfaceHelper mInterfaceHelper;
    private HomePageComponent mDataModelHelper;

    private Fragment announcements;
    private Bundle savedInstanceState;

    private final NotificationProcessor homeRssFeedsUpdateStartedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            Logger.d(TAG, "homeRssFeedsUpdateStartedProcessor()");
            mInterfaceHelper.homeRssFeedsUpdateStarted();
        }
    };

    private final NotificationProcessor homeRssFeedsUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            Logger.d(TAG, "homeRssFeedsUpdateFinishedProcessor()");
            final ParamsReader pr = new ParamsReader(params);
            if (pr.succeed()) {
                findView(R.id.error_message_layout).setVisibility(View.GONE);
                mDataModelHelper.render();
            }

            mInterfaceHelper.homeRssFeedsUpdateFinished();
            showSocietyLogo();
        }
    };

    private final NotificationProcessor rssFeedUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            Logger.d(TAG, "rssFeedUpdateFinishedProcessor()");
            final ParamsReader pr = new ParamsReader(params);
            if (pr.succeed() && pr.getFeedMO() != null) {
                mDataModelHelper.updateFeedContent(pr.getFeedMO());
            }
        }
    };

    private final NotificationProcessor homeFeedUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            Logger.d(TAG, "homeFeedUpdateErrorProcessor()");
            mDataModelHelper.render();
            mInterfaceHelper.homeFeedUpdateFinished();
        }
    };

    private final NotificationProcessor homeFeedUpdateNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            Logger.d(TAG, "homeFeedUpdateNotModifiedProcessor()");
            mDataModelHelper.render();
            mInterfaceHelper.homeFeedUpdateFinished();
        }
    };

    private final NotificationProcessor announcementsUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            Logger.d(TAG, "announcementsUpdateSuccessProcessor()");
            final long actualAnnouncementsCount = announcementService.getActualAnnouncementsCount();
            if (announcements == null && actualAnnouncementsCount > 0) {
                announcements = Fragment.instantiate(getActivity(), AnnouncementFragment.class.getName(), savedInstanceState);
                final FragmentTransaction transaction = getChildFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.announcement_frame, announcements);
                transaction.commit();
            } else if (announcements != null && actualAnnouncementsCount == 0) {
                final FragmentTransaction transaction = getChildFragmentManager()
                        .beginTransaction();
                transaction.remove(announcements);
                transaction.commit();
                announcements = null;
            }
        }
    };

    private final NotificationProcessor announcementsUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (announcements != null) {
                final FragmentTransaction transaction = getChildFragmentManager()
                        .beginTransaction();
                transaction.remove(announcements);
                transaction.commit();
                announcements = null;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onCreateView()");
        return inflater.inflate(R.layout.frag_home_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        findView(R.id.error_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetUtils.isOnline(getActivity())) {
                    errorManager.alertWithErrorCode(getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
                } else {
                    updateManager.updateHomePageFeeds();
                }
            }
        });

        Logger.d(TAG_LIFE, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);

        this.savedInstanceState = savedInstanceState;

        mInterfaceHelper = new InterfaceHelper(this);

        final CustomWebView webView = findView(R.id.home_page_content_view);

        mDataModelHelper = new HomePageComponent(this, webView);

        mDataModelHelper.render();

        if (announcementService.getActualAnnouncementsCount() > 0) {
            announcements = Fragment.instantiate(getActivity(), AnnouncementFragment.class.getName(), savedInstanceState);
            final FragmentTransaction transaction = getChildFragmentManager()
                    .beginTransaction();
            transaction.replace(R.id.announcement_frame, announcements);
            transaction.commit();
        }

        showSocietyLogo();
    }

    private void showSocietyLogo() {
        final String societyFooterLogoImageUrl = settings.getSocietyFooterLogoImageUrl();
        if (societyFooterLogoImageUrl != null) {
            final ImageView societyLogo = findView(R.id.society_logo);
            final String logoImageUrl = Uri.fromFile(new File(societyFooterLogoImageUrl)).toString();

            final File imageFile = imageLoader.getDiskCache().get(logoImageUrl);
            if (imageFile.exists() && !imageFile.delete()) {
                Logger.w(TAG, "Unable to delete society logo image file from disk cache");
            }
            MemoryCacheUtils.removeFromCache(logoImageUrl, imageLoader.getMemoryCache());

            imageLoader.displayImage(logoImageUrl, societyLogo);
        }
    }

    @Override
    public void onStart() {
        Logger.d(TAG_LIFE, "onStart()");
        super.onStart();

        notificationCenter.subscribeToNotification(EventList.HOME_PAGE_FEEDS_UPDATE_STARTED.getEventName(), homeRssFeedsUpdateStartedProcessor);
        notificationCenter.subscribeToNotification(EventList.HOME_PAGE_FEEDS_UPDATE_FINISHED.getEventName(), homeRssFeedsUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_ERROR.getEventName(), homeFeedUpdateErrorProcessor);
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_NOT_MODIFIED.getEventName(), homeFeedUpdateNotModifiedProcessor);
        notificationCenter.subscribeToNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(), rssFeedUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(EventList.ANNOUNCEMENTS_UPDATE_SUCCESS.getEventName(), announcementsUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(EventList.ANNOUNCEMENTS_UPDATE_ERROR.getEventName(), announcementsUpdateErrorProcessor);
    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Logger.d(TAG_LIFE, "onHiddenChanged(): hidden = " + hidden);
        super.onHiddenChanged(hidden);

        // feature: quick link menu
        if (!hidden) {
            mInterfaceHelper.showQuickLinkMenu();
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
        notificationCenter.unSubscribeFromNotification(homeRssFeedsUpdateStartedProcessor);
        notificationCenter.unSubscribeFromNotification(homeRssFeedsUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(homeFeedUpdateErrorProcessor);
        notificationCenter.unSubscribeFromNotification(homeFeedUpdateNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(rssFeedUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(announcementsUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(announcementsUpdateErrorProcessor);
    }

    @Override
    public void onDestroyView() {
        Logger.d(TAG_LIFE, "onDestroyView()");
        super.onDestroyView();
    }

    @Override
    public Context getContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void showNoContentAvailableMessage() {
        final ErrorMessage errorMessage = errorManager.getErrorMessageForErrorCode(getActivity(), AppErrorCode.NO_FEED_AVAILABLE);

        final TextView titleView = findView(R.id.error_title);
        if (errorMessage.getMessage() == null) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(errorMessage.getTitle());
        }
        ((TextView) findView(R.id.error_text)).setText(errorMessage.getMessage() == null ?
                errorMessage.getTitle() : errorMessage.getMessage());

        findView(R.id.error_message_layout).setVisibility(View.VISIBLE);
    }

    private class InterfaceHelper {

        private ProgressHandler progress;
        private SwipeRefreshLayout swipeLayout;
        private ActionBarSherlockCompat mSherlock;
        private Fragment mHost;

        public InterfaceHelper(final Fragment fragment) {
            mHost = fragment;
            progress = new ProgressHandler(mHost);

            // action bar

            if (DeviceUtils.isPhone(mHost.getActivity())) {
                mSherlock = new ActionBarSherlockCompat(getJournalActivity(), 0,
                        (ViewGroup)findView(R.id.abs__screen_action_bar));
                mSherlock.setupActionBar()
                        .setBackgroundDrawable(theme.getMainColor())
                        .setTitleActionBar(getResources().getString(R.string.society_home_title),
                                (int) getActivity().getResources().getDimension(R.dimen.action_bar_title_padding_left));
                mSherlock.setListener(new ActionBarSherlock.OnCreatePanelMenuListener() {
                    @Override
                    public boolean onCreatePanelMenu(int featureId, Menu menu) {
                        if (DeviceUtils.isPhone(getJournalActivity())) {
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
                });
            }

            swipeLayout = findView(R.id.swipe_container);
            swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                @Override
                public void onRefresh() {
                    swipeLayout.setRefreshing(true);
                    updateManager.updateHomePageFeeds();
                }
            });

            swipeLayout.setColorSchemeResources(R.color.green_swipe, R.color.red_swipe, R.color.blue_swipe, R.color.orange_swipe);

            // feature: quick link menu
            if (DeviceUtils.isPhone(getJournalActivity())) {
                // touch layout
                ((TouchRefreshLayout)findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        showQuickLinkMenu();
                    }
                });

                quickLinkMenuComponent.initQuickLink(getActivity(), SocietyNewsFragment.this);
            }
        }

        /**
         *  feature: quick link menu
         */
        private void showQuickLinkMenu() {
            quickLinkMenuComponent.showQuickLinkMenu();
        }

        public void homeRssFeedsUpdateStarted() {
            progress.showProgress(getString(R.string.loading_label));
        }

        public void homeRssFeedsUpdateFinished() {
            progress.hideProgress();
            swipeLayout.setRefreshing(false);
        }

        public void homeFeedUpdateFinished() {
            progress.hideProgress();
            swipeLayout.setRefreshing(false);
        }
    }
}
