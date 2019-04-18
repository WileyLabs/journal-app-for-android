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
package com.wiley.wol.client.android.data.http;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.journalApp.receiver.CustomBroadcastReceiver;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;
import static com.wiley.wol.client.android.settings.Settings.SETTING_ACCESS_CODE;
import static com.wiley.wol.client.android.settings.Settings.SETTING_AUTH_TOKEN;
import static com.wiley.wol.client.android.settings.Settings.SETTING_CURRENT_SERVER;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class UpdateManagerImpl implements UpdateManager {
    private final static String TAG = UpdateManagerImpl.class.getSimpleName();
    private static final String SOCIETY_UPDATE_ENTITY = "society";
    private static final String TPS_UPDATE_ENTITY = "TPS";
    private static final String EARLY_VIEW_UPDATE_ENTITY = "ev";
    private static final String ISSUES_LIST_UPDATE_ENTITY = "issues";
    private static final String SPECIAL_SECTIONS_UPDATE_ENTITY = "spec_sec";
    private static final String UPDATE_TASK_WITHOUT_EARLY_VIEW = "update_without_early_view";

    @Inject
    private ImportManager importManager;
    @Inject
    private HomePageService homePageService;
    @Inject
    private IssueService issueService;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private ArticleService articleService;

    private boolean waitForAffiliationFeedUpdate;

    private NotificationCenter notificationCenter;

    private final List<String> allContentUpdateState = new ArrayList<>();
    private final List<String> rssFeedsUpdateState = new ArrayList<>();

    private final Executor executor = newSingleThreadExecutor();
    private final Executor homePageExecutor = newSingleThreadExecutor();

    private final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            runUpdate(UPDATE_TASK);
        }
    };

    private final Runnable updateTaskWithoutEarlyView = new Runnable() {
        @Override
        public void run() {
            runUpdate(UPDATE_TASK_WITHOUT_EARLY_VIEW);
        }
    };

    private final Runnable updateTaskForce = new Runnable() {
        @Override
        public void run() {
            runUpdate(UPDATE_TASK_FORCE);
        }
    };

    private void runUpdate(final String mode) {
        GANHelper.trackEvent(GANHelper.EVENT_APP,
                GANHelper.ACTION_REFRESH,
                GANHelper.LABEL_AUTOMATIC,
                0L);

        Logger.d(TAG, "all content update started: mode = " + mode);

        boolean updateEarlyView = true;
        if (UPDATE_TASK_WITHOUT_EARLY_VIEW.equals(mode)) {
            updateEarlyView = false;
        }

        resetAllContentUpdateState(updateEarlyView);
        notificationCenter.sendNotification(EventList.ALL_CONTENT_UPDATE_STARTED.getEventName());
        importManager.updateIssueList();
        if (updateEarlyView) {
            importManager.updateEarlyViewFeed();
        }
        importManager.updateSpecialSections();
        importManager.updateSocietyFeed();
        importManager.updateTPSFeed();
        importManager.updateInAppContent();
        importManager.updateAdvertisementFeed();

        waitContentUpdateCompleted();

        Logger.d(TAG, "all content update finished: mode = " + mode);
        ParamsBuilder paramsBuilder = new ParamsBuilder();
        paramsBuilder.withMode(mode);
        notificationCenter.sendNotification(EventList.ALL_CONTENT_UPDATE_FINISHED.getEventName(), paramsBuilder.get());
    }

    private void waitContentUpdateCompleted() {
        synchronized (allContentUpdateState) {
            while (!allContentUpdateState.isEmpty()) {
                try {
                    allContentUpdateState.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private final Runnable updateTaskHomePageFeeds = new Runnable() {
        @Override
        public void run() {
            Logger.d(TAG, "HOME_PAGE_FEEDS_UPDATE_STARTED");
            notificationCenter.sendNotification(EventList.HOME_PAGE_FEEDS_UPDATE_STARTED.getEventName());
            importManager.updateAnnouncementFeed();
            importManager.updateHomePageFeed();
        }
    };
    private final Runnable updateAffiliationFeedTask = new Runnable() {
        @Override
        public void run() {
            importManager.updateAffiliationFeed();
        }
    };

    @Inject
    public UpdateManagerImpl(final NotificationCenter notificationCenter,
                             final CustomBroadcastReceiver broadcastReceiver, final Settings settings, final Theme theme) {
        final NotificationProcessor settingsChangedProcessor = new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                final String settingName = params.get(SETTING_NAME_KEY).toString();

                if (SETTING_AUTH_TOKEN.equals(settingName) && settings.isAuthorized()) {
                    broadcastReceiver.startUpdateAffiliationSchedulerAndTrigger();
                    waitForAffiliationFeedUpdate = true;
                } else if (SETTING_AUTH_TOKEN.equals(settingName) && !settings.isAuthorized()) {
                    broadcastReceiver.stopUpdateAffiliationScheduler();
                    refreshContent();
                } else if (SETTING_ACCESS_CODE.equals(settingName) ||
                        SETTING_CURRENT_SERVER.equals(settingName)) {
                    Logger.d(TAG, "received settingChanged for " + settingName);
                    updateFeeds();
                    updateHomePageFeeds();
                }
            }
        };

        final NotificationProcessor affiliationInfoUpdateFinishedProcessor = new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                if (waitForAffiliationFeedUpdate) {
                    waitForAffiliationFeedUpdate = false;
                    refreshContent();
                }
            }
        };

        this.notificationCenter = notificationCenter;
        this.notificationCenter.subscribeToNotification(EventList.SETTINGS_CHANGED.getEventName(), settingsChangedProcessor);
        this.notificationCenter.subscribeToNotification(EventList.AFFILIATION_INFO_UPDATE_FINISHED.getEventName(), affiliationInfoUpdateFinishedProcessor);
        subscribeToUpdateEvents();

        if (!theme.getApplicationVersion().equals(settings.getStoredAppVersion())) {
            settings.setStoredAppVersion(theme.getApplicationVersion());
            settings.setAppLastUpgradeDate(new Date());
        }
    }

    private void subscribeToUpdateEvents() {
        // society feed
        notificationCenter.subscribeToNotification(EventList.SOCIETY_UPDATED_SUCCESS.getEventName(), new UpdateSuccessProcessor(SOCIETY_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.SOCIETY_UPDATED_ERROR.getEventName(), new UpdateErrorProcessor(SOCIETY_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.SOCIETY_UPDATED_NOT_MODIFIED.getEventName(), new UpdateNotModifiedProcessor(SOCIETY_UPDATE_ENTITY));
        // TPS feed
        notificationCenter.subscribeToNotification(EventList.TPS_SITES_UPDATED_SUCCESS.getEventName(), new UpdateSuccessProcessor(TPS_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.TPS_SITES_UPDATED_ERROR.getEventName(), new UpdateErrorProcessor(TPS_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.TPS_SITES_UPDATED_NOT_MODIFIED.getEventName(), new UpdateNotModifiedProcessor(TPS_UPDATE_ENTITY));
        // ev feed
        notificationCenter.subscribeToNotification(EventList.EARLY_VIEW_FEED_UPDATED.getEventName(), new UpdateSuccessProcessor(EARLY_VIEW_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.EARLY_VIEW_FEED_ERROR.getEventName(), new UpdateErrorProcessor(EARLY_VIEW_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.EARLY_VIEW_FEED_NOT_MODIFIED.getEventName(), new UpdateNotModifiedProcessor(EARLY_VIEW_UPDATE_ENTITY));
        // issues list
        notificationCenter.subscribeToNotification(EventList.ISSUE_LIST_UPDATED.getEventName(), new UpdateSuccessProcessor(ISSUES_LIST_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.ISSUE_LIST_ERROR.getEventName(), new UpdateErrorProcessor(ISSUES_LIST_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.ISSUE_LIST_NOT_MODIFIED.getEventName(), new UpdateNotModifiedProcessor(ISSUES_LIST_UPDATE_ENTITY));
        // special sections
        notificationCenter.subscribeToNotification(EventList.SPECIAL_SECTIONS_UPDATED.getEventName(), new UpdateSuccessProcessor(SPECIAL_SECTIONS_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.SPECIAL_SECTIONS_ERROR.getEventName(), new UpdateErrorProcessor(SPECIAL_SECTIONS_UPDATE_ENTITY));
        notificationCenter.subscribeToNotification(EventList.SPECIAL_SECTIONS_NOT_MODIFIED.getEventName(), new UpdateNotModifiedProcessor(SPECIAL_SECTIONS_UPDATE_ENTITY));
        // in app content
        // TODO ??? no notifications

        // home page feed
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_SUCCESS.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                Logger.d(TAG, "HOME_FEED_UPDATED_SUCCESS");
                rssFeedsUpdateState.clear();
                for (final FeedMO feed : homePageService.getFeeds()) {
                    importManager.updateRssFeed(feed);
                    rssFeedsUpdateState.add(feed.getUid());
                }
            }
        });
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_NOT_MODIFIED.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                Logger.d(TAG, "HOME_FEED_UPDATED_NOT_MODIFIED");
            }
        });
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_ERROR.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                Logger.d(TAG, "HOME_FEED_UPDATED_ERROR");
            }
        });

        // rss feed
        notificationCenter.subscribeToNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                final String feedUid = (String) params.get("uid");
                rssFeedsUpdateState.remove(feedUid);
                if (rssFeedsUpdateState.isEmpty()) {
                    Logger.d(TAG, "HOME_PAGE_FEEDS_UPDATE_FINISHED");
                    notificationCenter.sendNotification(EventList.HOME_PAGE_FEEDS_UPDATE_FINISHED.getEventName(),
                            new ParamsBuilder()
                                    .succeed(true)
                                    .notModified(false)
                                    .get());
                }
            }
        });
    }

    @Override
    public void updateFeeds() {
        Logger.d(TAG, "updateFeeds");
        executor.execute(updateTask);
    }

    @Override
    public void updateFeeds(boolean updateEarlyView) {
        Logger.d(TAG, "updateFeeds");
        if (updateEarlyView) {
            executor.execute(updateTask);
        } else {
            executor.execute(updateTaskWithoutEarlyView);
        }
    }

    @Override
    public void updateFeedsForce() {
        Logger.d(TAG, "updateFeedsForce");
        executor.execute(updateTaskForce);
    }

    @Override
    public void updateHomePageFeeds() {
        Logger.d(TAG, "updateHomePageFeeds");
        homePageExecutor.execute(updateTaskHomePageFeeds);
    }

    @Override
    public void updateAffiliationFeed() {
        Logger.d(TAG, "updateAffiliationFeeds");
        executor.execute(updateAffiliationFeedTask);
    }

    private void refreshContent() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                setContentUpdateStateForRefreshOperation();

                importManager.updateIssueList();
                importManager.updateInAppContent();
                updateRestrictedStatusFeed();
                waitContentUpdateCompleted();

                notificationCenter.sendNotification(EventList.ALL_CONTENT_UPDATE_FINISHED.getEventName());
            }
        });
    }

    private void setContentUpdateStateForRefreshOperation() {
        synchronized (allContentUpdateState) {
            allContentUpdateState.add(ISSUES_LIST_UPDATE_ENTITY);
        }
    }

    private void updateRestrictedStatusFeed() {
        final JSONArray issueDOIs = getKnownIssueDOIs();
        final JSONArray specialSectionDOIs = getSpecialSectionDOIs();

        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("issues", issueDOIs);
            jsonObject.put("specialSections", specialSectionDOIs);
            importManager.updateRestrictedFeed(jsonObject);
        } catch (JSONException e) {
            Logger.s(TAG, e);
        }
    }

    private JSONArray getKnownIssueDOIs() {
        final List<IssueMO> knownIssues = issueService.getIssues();
        final JSONArray issueDOIs = new JSONArray();
        for (IssueMO issue : knownIssues) {
            if (!issue.isLocal() && articleService.getNumOfArticlesForIssueTOC(issue.getDOI()) > 0 && issue.getDOI() != null) {
                issueDOIs.put(issue.getDOI().getValue());
            }
        }
        return issueDOIs;
    }

    private JSONArray getSpecialSectionDOIs() {
        final List<SpecialSectionMO> specialSections = specialSectionService.getSpecialSections();
        final JSONArray specialSectionDOIs = new JSONArray();
        for (SpecialSectionMO section : specialSections) {
            if (!section.getArticles().isEmpty() && section.getUid() != null) {
                specialSectionDOIs.put(section.getUid());
            }
        }
        return specialSectionDOIs;
    }

    private void resetAllContentUpdateState(boolean updateEarlyView) {
        synchronized (allContentUpdateState) {
            allContentUpdateState.clear();
            allContentUpdateState.add(SOCIETY_UPDATE_ENTITY);
            allContentUpdateState.add(TPS_UPDATE_ENTITY);
            if (updateEarlyView) {
                allContentUpdateState.add(EARLY_VIEW_UPDATE_ENTITY);
            }
            allContentUpdateState.add(ISSUES_LIST_UPDATE_ENTITY);
            allContentUpdateState.add(SPECIAL_SECTIONS_UPDATE_ENTITY);
        }
    }

    private void onEntityUpdated(String entity) {
        synchronized (allContentUpdateState) {
            allContentUpdateState.remove(entity);
            if (allContentUpdateState.isEmpty()) {
                allContentUpdateState.notifyAll();
            }
        }
    }

    private abstract class BaseUpdateSuccessProcessor implements NotificationProcessor {

        protected final String entity;

        public BaseUpdateSuccessProcessor(String updateEntity) {
            entity = updateEntity;
        }

    }

    private class UpdateSuccessProcessor extends BaseUpdateSuccessProcessor {

        public UpdateSuccessProcessor(String updateEntity) {
            super(updateEntity);
        }

        @Override
        public void processNotification(Map<String, Object> params) {
            onEntityUpdated(entity);
        }

    }

    private class UpdateErrorProcessor extends BaseUpdateSuccessProcessor {

        public UpdateErrorProcessor(String updateEntity) {
            super(updateEntity);
        }

        @Override
        public void processNotification(Map<String, Object> params) {
            onEntityUpdated(entity);
        }

    }

    private class UpdateNotModifiedProcessor extends BaseUpdateSuccessProcessor {

        public UpdateNotModifiedProcessor(String updateEntity) {
            super(updateEntity);
        }

        @Override
        public void processNotification(Map<String, Object> params) {
            onEntityUpdated(entity);
        }

    }

}