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
package com.wiley.android.journalApp.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.inject.Inject;
import com.urbanairship.UAirship;
import com.urbanairship.push.BaseIntentReceiver;
import com.urbanairship.push.PushMessage;
import com.wiley.android.journalApp.MainApplication;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.controller.ArticleController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.Settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import roboguice.RoboGuice;

import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_UPDATED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.MAIN_ACTIVITY_IS_SHOWN;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_UPDATED;

public class IntentReceiver
        extends
        BaseIntentReceiver {

    private static final String TAG = IntentReceiver.class.getSimpleName() + ".PushNotification";

    private Context mContext = null;
    @Inject private NotificationCenter notificationCenter;
    @Inject private WebController webController;
    @Inject private ImportManager importManager;
    @Inject private ArticleController articleController;
    @Inject private IssueService issueService;
    @Inject private SpecialSectionService specialSectionService;
    @Inject private ArticleService articleService;
    @Inject private Settings settings;

    private enum NotificationType {
        NONE,
        APS,
        URL,
        ARTICLE_EARLY_VIEW,
        ARTICLE_ISSUE_TOC,
        ARTICLE_SPECIAL_SECTION,
        ISSUE
    };

    private NotificationType mNotificationType;
    private String mUrl;
    private DOI mIssueDoi;
    private DOI mArticleDoi;
    private String mSpecialSectionId;

    public IntentReceiver() {
        mNotificationType = NotificationType.NONE;
    }

    private final NotificationProcessor mainActivityIsShownProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "mainActivityIsShownProcessor");
            notificationCenter.unSubscribeFromNotification(mainActivityIsShownProcessor);
            if (!MainApplication.isMainActivityVisible()) {
                mNotificationType = NotificationType.NONE;
            }
            else {
                startAction();
            }
        }
    };

    private final NotificationProcessor earlyViewUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "earlyViewUpdateSuccessProcessor");

            if (NotificationType.ARTICLE_EARLY_VIEW == mNotificationType) {
                if (null != loadArticleEarlyView(mArticleDoi)) {
                    openArticleEarlyView(mArticleDoi);
                }
            }
            resetAction();
        }
    };

    private final NotificationProcessor earlyViewUpdateNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "earlyViewUpdateNotModifiedProcessor");
            resetAction();
        }
    };

    private final NotificationProcessor earlyViewUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "earlyViewUpdateErrorProcessor");
            resetAction();
        }
    };

    private final NotificationProcessor issueTocUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "issueTocUpdateSuccessProcessor");

            if (NotificationType.ARTICLE_ISSUE_TOC == mNotificationType) {
                if (null != loadArticleIssue(mArticleDoi, mIssueDoi)) {
                    openArticleIssue(mArticleDoi, mIssueDoi);
                }
            }
            resetAction();
        }
    };

    private final NotificationProcessor issueTocUpdateNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "issueTocUpdateNotModifiedProcessor");
            resetAction();
        }
    };

    private final NotificationProcessor issueTocUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "issueTocUpdateErrorProcessor: after update issueToc with doi '" + mIssueDoi.getValue() + "' was error.");
            resetAction();
        }
    };

    private final NotificationProcessor specialSectionUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "specialSectionUpdateSuccessProcessor");

            if (NotificationType.ARTICLE_SPECIAL_SECTION == mNotificationType) {
                if (null != loadArticleSpecialSection(mArticleDoi, mSpecialSectionId)) {
                    openArticleSpecialSection(mArticleDoi, mSpecialSectionId);
                }
            }
            resetAction();
        }
    };

    private final NotificationProcessor specialSectionUpdateNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "specialSectionUpdateNotModifiedProcessor");
            resetAction();
        }
    };

    private final NotificationProcessor specialSectionUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "specialSectionUpdateErrorProcessor: after update issueToc with doi '" + mIssueDoi.getValue() + "' was error.");
            resetAction();
        }
    };

    private ArticleMO loadArticleEarlyView(DOI articleDoi) {
        final ArticleMO article = articleService.getArticleQuietly(articleDoi);
        if (null == article) {
            Logger.d(TAG, "checkArticleEarlyView(): article[" + articleDoi.getValue() + "] not found.");
            return null;
        }
        if (!article.isEarlyView()) {
            Logger.d(TAG, "checkArticleEarlyView(): article[" + articleDoi.getValue() + "] is not Early View.");
            return null;
        }

        return article;
    }

    private ArticleMO loadArticleIssue(DOI articleDoi, DOI issueDoi) {
        final ArticleMO article = articleService.getArticleQuietly(articleDoi);
        if (null == article) {
            Logger.d(TAG, "checkArticleIssue(): article[" + articleDoi.getValue() + "] not found.");
            return null;
        }
        if(null == article.getSection() || !article.getSection().getIssue().getDOI().getValue().equals(issueDoi.getValue())) {
            Logger.d(TAG, "checkArticleIssue(): article[" + articleDoi.getValue() + "] is not exists in issue[" + issueDoi.getValue() + "].");
            return null;
        }

        return article;
    }

    private ArticleMO loadArticleSpecialSection(DOI articleDoi, String specialSectionId) {
        final ArticleMO article = articleService.getArticleQuietly(articleDoi);
        if (null == article) {
            Logger.d(TAG, "checkArticleSpecialSection(): article[" + articleDoi.getValue() + "] not found.");
            return null;
        }

        Collection<SpecialSectionMO> specialSections = article.getSpecialSections();
        if (null != specialSections) {
            for (SpecialSectionMO specialSection : specialSections) {
                if (specialSectionId.equals(specialSection.getUid())) {
                    return article;
                }
            }
        }

        Logger.d(TAG, "checkArticleSpecialSection(): article[" + articleDoi.getValue() + "] is not exists in specialSection[" + specialSectionId + "].");
        return null;
    }

    private void openArticleEarlyView(DOI articleDoi) {
        // prepare parameters
        final ArticleMO storedArticle = loadArticleEarlyView(articleDoi);
        if (null == storedArticle) {
            return;
        }

        final List<DOI> doiList = new ArrayList<>();
        for (ArticleMO article : articleService.getArticlesForEarlyView()) {
            doiList.add(article.getDOI());
        }

        // open early view article
        final ParamsBuilder paramsBuilder = new ParamsBuilder()
                .withDoiList(doiList)
                .withArticleDoi(articleDoi)
                .withTitleList(mContext.getString(R.string.early_view_articles_title));
        notificationCenter.sendNotification(EventList.PUSH_NOTIFICATION_OPEN_EARLY_VIEW_ARTICLE.getEventName(), paramsBuilder.get());
    }

    private void openArticleIssue(DOI articleDoi, DOI issueDoi) {

        // prepare parameters
        final ArticleMO storedArticle = loadArticleIssue(articleDoi, issueDoi);
        if (null == storedArticle) {
            return;
        }

        final IssueMO storedIssue = storedArticle.getSection().getIssue();

        final List<DOI> doiList = new ArrayList<>();
        for (ArticleMO article : articleService.getArticlesForIssueTOC(issueDoi)) {
            doiList.add(article.getDOI());
        }

        // open issue article
        final ParamsBuilder paramsBuilder = new ParamsBuilder()
                .withIssueDoi(issueDoi)
                .withDoiList(doiList)
                .withArticleDoi(articleDoi)
                .withTitleList(String.format(mContext.getString(R.string.issue_volume_title), storedIssue.getVolumeNumber(), storedIssue.getIssueNumber()));
        notificationCenter.sendNotification(EventList.PUSH_NOTIFICATION_OPEN_ISSUE_ARTICLE.getEventName(), paramsBuilder.get());
    }

    private void openArticleSpecialSection(DOI articleDoi, String specialSectionId) {
        // prepare parameters
        final ArticleMO storedArticle = loadArticleSpecialSection(articleDoi, specialSectionId);
        if (null == storedArticle) {
            return;
        }

        final List<DOI> doiList = new ArrayList<>();
        for (ArticleMO article : specialSectionService.getSpecialSectionById(specialSectionId).getArticles()) {
            doiList.add(article.getDOI());
        }

        // open special section article
        final ParamsBuilder paramsBuilder = new ParamsBuilder()
                .withSpecialSectionId(specialSectionId)
                .withDoiList(doiList)
                .withArticleDoi(articleDoi)
                .withTitleList(specialSectionService.getSpecialSectionById(specialSectionId).getUnescapedTitle());
        notificationCenter.sendNotification(EventList.PUSH_NOTIFICATION_OPEN_SPECIAL_SECTION_ARTICLE.getEventName(), paramsBuilder.get());
    }

    private void openIssue(DOI issueDoi) {
        try {
            // prepare parameters
            issueService.getIssue(issueDoi);

            // open issue
            final ParamsBuilder paramsBuilder = new ParamsBuilder()
                    .withIssueDoi(issueDoi);

            notificationCenter.sendNotification(EventList.PUSH_NOTIFICATION_OPEN_ISSUE.getEventName(), paramsBuilder.get());

        } catch (ElementNotFoundException ignored) {
            Logger.d(TAG, "openIssue(): issue[" + issueDoi.getValue() + "] not found.");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null == this.mContext && null != context) {
            this.mContext = context;
            RoboGuice.getInjector(context).injectMembers(this);
            UAirship.shared().getPushManager().setPushEnabled(true);
            UAirship.shared().getPushManager().setUserNotificationsEnabled(true);
        }

        super.onReceive(context, intent);
    }

    @Override
    protected void onChannelRegistrationSucceeded(Context context, String channelId) {
        Log.i(TAG, "Channel registration updated. Channel Id:" + channelId);

        settings.setDeviceToken(channelId);
    }

    @Override
    protected void onChannelRegistrationFailed(Context context) {
        Log.i(TAG, "Channel registration failed.");
    }

    @Override
    protected void onPushReceived(Context context, PushMessage message, int notificationId) {
        Log.i(TAG, "Received push notification. Alert: " + message.getAlert() + ". Notification ID: " + notificationId);
    }

    @Override
    protected void onBackgroundPushReceived(Context context, PushMessage message) {
        Log.i(TAG, "Received background push message: " + message);
    }

    @Override
    public boolean onNotificationOpened(Context context, PushMessage message, int notificationId) {
        Log.i(TAG, "User clicked notification. Alert: " + message.getAlert());
        Bundle bundle = message.getPushBundle();
        logPushExtras(bundle);

        if (NotificationType.NONE != mNotificationType) {
            Log.i(TAG, "onNotificationOpened(): executing action " + mNotificationType.name() + "; alert = " + message.getAlert() + " SKIPPED");
            return true;
        }

        mNotificationType = NotificationType.NONE;
        if (bundle.containsKey("aps")) {
            final String aps = bundle.getString("aps");
            if (null != aps && aps.equals("content-available")) {
                mNotificationType = NotificationType.APS;
            }
        }
        else if (bundle.containsKey("url")) {
            String value = bundle.getString("url");
            if (null != value && !value.isEmpty()) {
                if (!value.startsWith("http")) {
                    value = "http://" + value;
                }
                mUrl = value;
                mNotificationType = NotificationType.URL;
            }
        }
        else if (bundle.containsKey("article_doi")) {
            String value = bundle.getString("article_doi");
            if (null != value && !value.isEmpty()) {
                mArticleDoi = new DOI(value);

                if (bundle.containsKey("issue_doi")) {
                    value = bundle.getString("issue_doi");
                    if (null != value && !value.isEmpty()) {
                        mIssueDoi = new DOI(value);
                        mNotificationType = NotificationType.ARTICLE_ISSUE_TOC;
                    }
                }
                else if (bundle.containsKey("special_section_id")) {
                    value = bundle.getString("special_section_id");
                    if (null != value && !value.isEmpty()) {
                        mSpecialSectionId = value;
                        mNotificationType = NotificationType.ARTICLE_SPECIAL_SECTION;

                    }
                }
                else if (bundle.containsKey("early_view_flag")) {
                    value = bundle.getString("early_view_flag");
                    if (null != value && value.equals("true")) {
                        mNotificationType = NotificationType.ARTICLE_EARLY_VIEW;
                    }
                }
            }
        }
        else if (bundle.containsKey("issue_doi")) {
            final String value = bundle.getString("issue_doi");
            if (null != value && !value.isEmpty()) {
                mIssueDoi = new DOI(value);
                mNotificationType = NotificationType.ISSUE;
            }
        }

        if (!MainApplication.isMainActivityVisible()) {
            Log.i(TAG, "onNotificationOpened(): OPEN  MainActivity");
            notificationCenter.subscribeToNotification(MAIN_ACTIVITY_IS_SHOWN.getEventName(), mainActivityIsShownProcessor);
            Intent i = new Intent(mContext, MainActivity.class);
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.putExtra("fromNotification", true);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.getApplicationContext().startActivity(i);
        }
        else {
            Log.i(TAG, "onNotificationOpened(): startAction");
            startAction();
        }

        return true;
    }

    private void startAction() {
        Log.i(TAG, "startAction(): NotificationType = " + mNotificationType.name());
        switch (mNotificationType) {
            case APS:
                importManager.updateIssueList();
                importManager.updateEarlyViewFeed();
                break;
            case URL:
                webController.openUrlInternal(mUrl);
                break;
            case ARTICLE_ISSUE_TOC:
                if (null != loadArticleIssue(mArticleDoi, mIssueDoi)) {
                    openArticleIssue(mArticleDoi, mIssueDoi);
                } else {
                    subscribeIssueTocUpdate();
                    importManager.updateIssuesTOC(mIssueDoi);
                    return;
                }
                break;
            case ARTICLE_SPECIAL_SECTION:
                if (null != loadArticleSpecialSection(mArticleDoi, mSpecialSectionId)) {
                    openArticleSpecialSection(mArticleDoi, mSpecialSectionId);
                } else {
                    subscribeSpecialSectionUpdate();
                    importManager.updateSpecialSection(mSpecialSectionId);
                    return;
                }
                break;
            case ARTICLE_EARLY_VIEW:
                if (null != loadArticleEarlyView(mArticleDoi)) {
                    openArticleEarlyView(mArticleDoi);
                } else {
                    subscribeEarlyViewUpdate();
                    importManager.updateEarlyViewFeed();
                    return;
                }
                break;
            case ISSUE:
                openIssue(mIssueDoi);
                break;
        }

        mNotificationType = NotificationType.NONE;
    }

    private void resetAction() {
        switch (mNotificationType) {
            case APS:
                break;
            case URL:
                break;
            case ARTICLE_ISSUE_TOC:
                unSubscribeIssueTocUpdate();
                break;
            case ARTICLE_SPECIAL_SECTION:
                unSubscribeSpecialSectionUpdate();
                break;
            case ARTICLE_EARLY_VIEW:
                unSubscribeEarlyViewUpdate();
                break;
            case ISSUE:
                break;
        }
        mNotificationType = NotificationType.NONE;
    }

    @Override
    protected boolean onNotificationActionOpened(Context context, PushMessage message, int notificationId, String buttonId, boolean isForeground) {
        Log.i(TAG, "User clicked notification button. Button ID: " + buttonId + " Alert: " + message.getAlert());
        return false;
    }


    @Override
    protected void onNotificationDismissed(Context context, PushMessage message, int notificationId) {
        Log.i(TAG, "Notification dismissed. Alert: " + message.getAlert() + ". Notification ID: " + notificationId);
    }

    private void logPushExtras(Bundle bundle) {
        Set<String> keys = bundle.keySet();
        StringBuilder stringBuilder = new StringBuilder("");
        for (String key : keys) {
            stringBuilder.append(key + " : '" + bundle.get(key) + "'\n");
        }
        Logger.d(TAG, "Push Notification Extra: \n" + stringBuilder.toString());
    }

    private void subscribeEarlyViewUpdate() {
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_UPDATED.getEventName(), earlyViewUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_NOT_MODIFIED.getEventName(), earlyViewUpdateNotModifiedProcessor);
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_ERROR.getEventName(), earlyViewUpdateErrorProcessor);
    }

    private void unSubscribeEarlyViewUpdate() {
        notificationCenter.unSubscribeFromNotification(earlyViewUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(earlyViewUpdateNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(earlyViewUpdateErrorProcessor);
    }

    private void subscribeIssueTocUpdate() {
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_SUCCESS.getEventName(), issueTocUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_NOT_MODIFIED.getEventName(), issueTocUpdateNotModifiedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_ERROR.getEventName(), issueTocUpdateErrorProcessor);
    }

    private void unSubscribeIssueTocUpdate() {
        notificationCenter.unSubscribeFromNotification(issueTocUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(issueTocUpdateNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(issueTocUpdateErrorProcessor);
    }

    private void subscribeSpecialSectionUpdate() {
        notificationCenter.subscribeToNotification(SPECIAL_SECTION_FEED_UPDATED.getEventName(), specialSectionUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(SPECIAL_SECTION_FEED_NOT_MODIFIED.getEventName(), specialSectionUpdateNotModifiedProcessor);
        notificationCenter.subscribeToNotification(SPECIAL_SECTION_FEED_ERROR.getEventName(), specialSectionUpdateErrorProcessor);
    }

    private void unSubscribeSpecialSectionUpdate() {
        notificationCenter.unSubscribeFromNotification(specialSectionUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(specialSectionUpdateNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(specialSectionUpdateErrorProcessor);
    }


}

