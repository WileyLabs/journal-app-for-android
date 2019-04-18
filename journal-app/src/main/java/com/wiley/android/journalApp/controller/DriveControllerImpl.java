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
package com.wiley.android.journalApp.controller;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Environment;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import static com.wiley.wol.client.android.notification.EventList.KEYWORDS_DEVICE_REGISTERED_ON_MCS;
import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;
import static com.wiley.wol.client.android.settings.Settings.SETTING_GOOGLE_DRIVE;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class DriveControllerImpl implements
        DriveController,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int FIVE_MINUTES_DELAY =  5 * 60;

    private static final String TAG = "DriveControllerImpl" + ".keywords";

    private static final String DATE_KEY = "date";
    private static final String STATE_KEY = "state";
    private static final String STATUS_KEY = "status";
    private static final String LAST_MODIFIED_DATE = "lastModifiedDate";

    private static final int REQUEST_CODE_RESOLUTION = 1;
    public static final String ISSUE_KEY = "issue";
    public static final String SPECIAL_SECTION_KEY = "specialSection";
    public static final String IS_EARLY_VIEW_KEY = "isEarlyView";

    private GoogleApiClient googleApiClient;
    private Activity context;

    private boolean isStop = false;
    private boolean isForceStart = false;
    private boolean isConnection = false;
    private boolean isRunning = false;
    private boolean isDebug = false;
    private volatile boolean registrationDeviceOnMCS = false;
    private int errorCode = 0;

    SimpleDateFormat formatDateGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US);

    private ExecutorService executor = newSingleThreadExecutor();

    @Inject
    private Theme theme;
    @Inject
    private Settings settings;
    @Inject
    private ArticleService articleService;
    @Inject
    private IssueService issueService;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private HomePageService homePageService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ImportManager importManager;
    @Inject
    protected Environment environment;

    private final NotificationProcessor articleFavoriteStateUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            addSavedArticleToLocalFile((ArticleMO) params.get(ArticleService.ARTICLE_MO));
        }
    };
    private final NotificationProcessor feedItemFavoriteStateUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final String uid = (String) params.get(NotificationCenter.UID);
            final FeedItemMO feedItem = homePageService.getFeedItem(uid);
            addSavedFeedItemToLocalFile(feedItem);
        }
    };

    private final NotificationProcessor keywordsDeviceRegisteredOnMcsProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            registrationDeviceOnMCS = true;
        }
    };

    private final NotificationProcessor keywordUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            final ParamsReader pr = new ParamsReader(params);
            if (pr.succeed()) {
                final JSONObject json = pr.getParam(NotificationCenter.KEYWORD_JSON);
                try {
                    final String action = json.getString("styleClass");
                    final String keyword = json.getString("keyword");
                    if (null != action && !action.equals("") && null != keyword && !keyword.equals("")) {
                        if (action.equals("subscribed")) {
                            addKeywordsToLocalFile(keyword, "subscribe");
                        } else if (action.equals("unsubscribed")) {
                            addKeywordsToLocalFile(keyword, "unsubscribe");
                        }
                    }
                } catch (JSONException ignored) {
                }
            }
        }
    };

    private final NotificationProcessor keywordsUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            List<String> keywords = settings.getKeywords();
            updateSubscribedKeywordsFromLocalFile(keywords);
        }
    };

    private final NotificationProcessor articleMarkAsReadProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            final ParamsReader pr = new ParamsReader(params);
            addReadArticleToLocalFile(pr.getArticleDoi());
        }
    };

    @Override
    public void onCreate(Activity context) {
        Logger.d(TAG, "onCreate()");

        isDebug = environment.isDebug();

        formatDateGMT.setTimeZone(TimeZone.getTimeZone("GMT"));

        this.context = context;

        notificationCenter.subscribeToNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), articleFavoriteStateUpdatedProcessor);
        notificationCenter.subscribeToNotification(EventList.ARTICLE_MARK_AS_READ.getEventName(), articleMarkAsReadProcessor);
        notificationCenter.subscribeToNotification(EventList.SOCIETY_FAVORITES_COUNT_CHANGED.getEventName(), feedItemFavoriteStateUpdatedProcessor);
        notificationCenter.subscribeToNotification(KEYWORDS_DEVICE_REGISTERED_ON_MCS.getEventName(), keywordsDeviceRegisteredOnMcsProcessor);
        notificationCenter.subscribeToNotification(EventList.KEYWORD_UPDATE_FINISHED.getEventName(), keywordUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(EventList.KEYWORDS_UPDATED.getEventName(), keywordsUpdatedProcessor);
    }

    @Override
    public void onStart() {
        Logger.d(TAG, "onStart()");

        if (null == googleApiClient) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        syncICloudStart();
    }

    @Override
    public void onStop() {
        Logger.d(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        notificationCenter.unSubscribeFromNotification(articleFavoriteStateUpdatedProcessor);
        notificationCenter.unSubscribeFromNotification(articleMarkAsReadProcessor);
        notificationCenter.unSubscribeFromNotification(feedItemFavoriteStateUpdatedProcessor);
        notificationCenter.unSubscribeFromNotification(keywordsDeviceRegisteredOnMcsProcessor);
        notificationCenter.unSubscribeFromNotification(keywordUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(keywordsUpdatedProcessor);

        syncICloudStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.d(TAG, "onConnected()");
        setConnected();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.d(TAG, "onConnectionSuspended(): code = " + i);
        setDisconnected();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            errorCode = connectionResult.getErrorCode();
            Logger.s(TAG, "Unable to connect to Google Drive: error code = " + errorCode + "; " + connectionResult.toString());
            return;
        }
        Logger.s(TAG, "onConnectionFailed()");

        try {
            connectionResult.startResolutionForResult(context, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Logger.s(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void reconnect() {
        Logger.d(TAG, "reconnect()");
        connectICloud();
    }

    private boolean isServiceAvailable() {
        boolean isSuccess = DeviceUtils.isGooglePlayServicesAvailable(context);
        Logger.d(TAG, "isGooglePlusServiceAvailable = " + isSuccess);
        return isSuccess;
    }

    private void syncICloudForceStart() {
        isForceStart = true;
    }

    private void syncICloudStop() {
        isStop = true;
    }

    private void syncICloudStart() {
        if (isRunning) {
            return;
        }
        isRunning = true;

        final DriveControllerImpl driveController = this;

        Thread syncICloudThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Logger.d(TAG, "syncICloudStart(): START");

                    final SyncContentSavedArticles syncContentSavedArticles = new SyncContentSavedArticles(driveController, googleApiClient);
                    final SyncContentReadArticles syncContentReadArticles = new SyncContentReadArticles(driveController, googleApiClient);
                    final SyncContentKeywords syncContentKeywords = new SyncContentKeywords(driveController, googleApiClient);
                    final SyncContentSavedFeedItems syncContentSavedFeedItems = new SyncContentSavedFeedItems(driveController, googleApiClient);

                    final List<SyncContent> syncContentList = new ArrayList<>();
                    syncContentList.add(syncContentSavedArticles);
                    syncContentList.add(syncContentReadArticles);
                    syncContentList.add(syncContentKeywords);
                    syncContentList.add(syncContentSavedFeedItems);

                    isStop = false;
                    isForceStart = true;
                    connectICloud();
                    while (!isStop) {
                        if (!waitStart()) {
                            continue;
                        }

                        if (!isServiceAvailable()) {
                            if (isDebug) {
                                final HashMap<String, Object> params = new HashMap<>();
                                params.put(SETTING_NAME_KEY, SETTING_GOOGLE_DRIVE);
                                params.put("connection", "No service");
                                notificationCenter.sendNotification(SETTINGS_CHANGED.getEventName(), params);
                            }
                            continue;
                        }

                        if (!isConnected()) {
                            if (isDebug) {
                                final HashMap<String, Object> params = new HashMap<>();
                                params.put(SETTING_NAME_KEY, SETTING_GOOGLE_DRIVE);
                                params.put("connection", "disconnected. error=" + errorCode);
                                notificationCenter.sendNotification(SETTINGS_CHANGED.getEventName(), params);
                            }
                            continue;
                        }

                        for (SyncContent syncContent : syncContentList) {
                            if (isStop) {
                                break;
                            }
                            if (syncContent.openDriveFile() && syncContent.needToMerge() && syncContent.merge()) {
                            }
                        }

                        if (isDebug) {
                            final HashMap<String, Object> params = new HashMap<>();
                            params.put(SETTING_NAME_KEY, SETTING_GOOGLE_DRIVE);
                            params.put("connection", "connected." +formatDateGMT.format(new Date()));
                            params.put("saved_articles_file", formatDateGMT.format(syncContentSavedArticles.getModifiedDate()) + "; size=" + syncContentSavedArticles.getFileSize());
                            params.put("read_articles_file", formatDateGMT.format(syncContentReadArticles.getModifiedDate()) + "; size=" + syncContentReadArticles.getFileSize());
                            params.put("keywords_file", formatDateGMT.format(syncContentKeywords.getModifiedDate()) + "; size=" + syncContentKeywords.getFileSize());
                            params.put("saved_feed_items_file", formatDateGMT.format(syncContentSavedFeedItems.getModifiedDate()) + "; size=" + syncContentSavedFeedItems.getFileSize());
                            notificationCenter.sendNotification(SETTINGS_CHANGED.getEventName(), params);
                        }
                    }
                    disconnectICloud();
                    isRunning = false;
                    Logger.d(TAG, "syncICloudStart(): STOP");
                } catch (Exception ignored) {
                }

            }
        });
        syncICloudThread.start();
    }

    private boolean waitStart() {
        Logger.d(TAG, "waitStart()");

        if (!settings.isRegisteredDeviceOnMCS()) {
            articleService.updateListOfSubscribedKeywords();
        }

        for (int delay = FIVE_MINUTES_DELAY; !isStop && !isForceStart && delay > 0; delay--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return false;
            }
        }

        isForceStart = false;
        return !isStop;
    }

    private boolean waitUpdatingContent() {
        //Logger.d(TAG, "waitUpdatingContent(): START");
        while (!isStop && importManager.isUpdating()) {
            if (!isConnected()) {
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }

        //Logger.d(TAG, "waitUpdatingContent(): STOP");
        return !isStop;
    }

    private void connectICloud() {
        Logger.d(TAG, "connectICloud()");
        setDisconnected();
        googleApiClient.connect();
    }

    private boolean disconnectICloud() {
        Logger.d(TAG, "disconnectICloud()");
        setDisconnected();
        if (null != googleApiClient) {
            googleApiClient.disconnect();
        }
        return true;
    }

    private boolean isConnected() {
        return isConnection;
    }

    private void setConnected() {
        errorCode = 0;
        isConnection = true;
    }

    private void setDisconnected() {
        isConnection = false;
    }

    private void addSavedArticleToLocalFile(final ArticleMO article) {
        if (null == article.getAddedToFavoriteDate()) {
            Logger.d(TAG, "addSavedArticleToLocalFile(): SKIP " + article.getDOI().getValue() + " - AddedToFavoriteDate == null ");
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String doi = article.getDOI().getValue();

                    final JSONObject localSavedArticlesInfo = loadLocalSavedArticlesFromLocalFile();
                    if (!localSavedArticlesInfo.has(doi) || localSavedArticlesInfo.getJSONObject(doi).getBoolean(STATE_KEY) != article.isFavorite()) {
                        // create json
                        final JSONObject json = new JSONObject();
                        json.put(DATE_KEY, formatDateGMT.parse(formatDateGMT.format(article.getAddedToFavoriteDate())).getTime());
                        json.put(STATE_KEY, article.isFavorite());
                        json.put(IS_EARLY_VIEW_KEY, article.isEarlyView());

                        if (article.getParentIssue() != null) {
                            json.put(ISSUE_KEY, article.getParentIssue().getDoi());
                        }

                        if (article.getSpecialSections() != null && article.getSpecialSections().size() > 0) {
                            json.put(SPECIAL_SECTION_KEY, article.getSpecialSections().iterator().next().getUid());
                        }

                        localSavedArticlesInfo.put(doi, json);
                        saveLocalSavedArticlesToLocalFile(localSavedArticlesInfo);

                        Logger.d(TAG, "addSavedArticleToLocalFile(): ADDED " + doi + "; isFavorite=" + json.getBoolean(STATE_KEY) + "; date=" + formatDateGMT.format(json.getLong(DATE_KEY)));

                    } else {
                        Logger.d(TAG, "addSavedArticleToLocalFile(): SKIPPED " + doi);
                    }
                } catch (JSONException | ParseException e) {
                    Logger.s(TAG, e);
                }
            }
        });
    }

    public void addSavedFeedItemToLocalFile(final FeedItemMO feedItem) {
        if (null == feedItem) {
            Logger.d(TAG, "addSavedFeedItemToLocalFile(): SKIP - feedItem == null ");
            return;
        }


        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String key = feedItem.getUid();

                    final JSONObject localSavedFeedItemsInfo = loadLocalSavedFeedItemsFromLocalFile();
                    if (!localSavedFeedItemsInfo.has(key) || localSavedFeedItemsInfo.getJSONObject(key).getBoolean(STATE_KEY) != feedItem.isFavorite()) {
                        final JSONObject json = new JSONObject();
                        json.put(DATE_KEY, new Date().getTime());
                        json.put(STATE_KEY, feedItem.isFavorite());

                        Logger.d(TAG, "addSavedFeedItemToLocalFile(): ADDED " + key + "; isFavorite=" + json.getBoolean(STATE_KEY) + "; date=" + formatDateGMT.format(json.getLong(DATE_KEY)));

                        localSavedFeedItemsInfo.put(key, json);
                        saveLocalSavedFeedItemsToLocalFile(localSavedFeedItemsInfo);
                    } else {
                        Logger.d(TAG, "addSavedFeedItemToLocalFile(): SKIPPED " + key);
                    }
                } catch (JSONException e) {
                    Logger.s(TAG, e);
                }
            }
        });
    }

    private void addReadArticleToLocalFile(final DOI doi) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ArticleMO article = articleService.getArticleQuietly(doi);
                    if (null != article && article.isRead()) {
                        JSONObject readArticlesInfo = loadLocalReadArticlesFromLocalFile();

                        if (!readArticlesInfo.has(doi.getValue())) {
                            Logger.d(TAG, "addReadArticleToLocalFile(): ADDED " + doi.getValue());
                            final JSONObject jsonState = new JSONObject();
                            jsonState.put(STATE_KEY, true);
                            readArticlesInfo.put(doi.getValue(), jsonState);
                            saveLocalReadArticlesToLocalFile(readArticlesInfo);
                        } else {
                            Logger.d(TAG, "addReadArticleToLocalFile(): SKIPPED " + doi.getValue());
                        }
                    }
                } catch (JSONException e) {
                    Logger.s(TAG, e);
                }
            }
        });
    }

    private void addKeywordsToLocalFile(final String keyword, final String status) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject localKeywordsInfo = loadLocalKeywordsFromLocalFile();

                    if (!localKeywordsInfo.has(keyword) || !status.equals(localKeywordsInfo.getJSONObject(keyword).getString(STATUS_KEY))) {
                        Logger.d(TAG, "addKeywordsToLocalFile(): ADDED keyword = " + keyword + "; status = " + status);
                        // save
                        JSONObject json = new JSONObject();
                        json.put(DATE_KEY, formatDateGMT.parse(formatDateGMT.format(new Date())).getTime());
                        json.put(STATUS_KEY, status);
                        localKeywordsInfo.put(keyword, json);
                        saveLocalKeywordsToLocalFile(localKeywordsInfo);
                    } else {
                        Logger.d(TAG, "addKeywordsToLocalFile(): SKIPPED keyword = " + keyword + "; status = " + status);
                    }
                } catch (ParseException | JSONException e) {
                    Logger.s(TAG, e);
                }
            }
        });
    }

    private void updateSubscribedKeywordsFromLocalFile(final List<String> subscribedKeywords) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // merge keywords
                    final JSONObject mergeResult = new JSONObject();

                    JSONObject localKeywordsInfo = loadLocalKeywordsFromLocalFile();
                    Iterator keys = localKeywordsInfo.keys();
                    while (keys.hasNext()) {
                        final String key = (String) keys.next();
                        final JSONObject json = localKeywordsInfo.getJSONObject(key);
                        boolean found = false;
                        for (String subscribedKeyword : subscribedKeywords) {
                            if (key.equals(subscribedKeyword)) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            Logger.d(TAG, "updateSubscribedKeywordsFromLocalFile(): CHANGED keyword = " + key + "; status = 'subscribe'");
                            json.put(STATUS_KEY, "subscribe");
                            mergeResult.put(key, json);
                        } else if (!key.equals(LAST_MODIFIED_DATE)) {
                            Logger.d(TAG, "updateSubscribedKeywordsFromLocalFile(): CHANGED keyword = " + key + "; status = 'unsubscribe'");
                            json.put(STATUS_KEY, "unsubscribe");
                            mergeResult.put(key, json);
                        }
                    }

                    // add from settings
                    for (String subscribedKeyword : subscribedKeywords) {
                        if (!localKeywordsInfo.has(subscribedKeyword)) {
                            JSONObject json = new JSONObject();
                            json.put(DATE_KEY, formatDateGMT.parse(formatDateGMT.format(new Date())).getTime());
                            json.put(STATUS_KEY, "subscribe");
                            localKeywordsInfo.put(subscribedKeyword, json);
                        }
                    }

                    saveLocalKeywordsToLocalFile(mergeResult);
                } catch (ParseException | JSONException e) {
                    Logger.s(TAG, e);
                }
            }
        });
    }

    private void saveLocalSavedArticlesToLocalFile(final JSONObject json) throws JSONException {
        final JSONObject jsonLastModifiedDate = new JSONObject();
        jsonLastModifiedDate.put(DATE_KEY, new Date().getTime());
        json.put(LAST_MODIFIED_DATE, jsonLastModifiedDate);
        settings.saveLocalSavedArticlesToLocalFile(json.toString());
    }

    private void saveDriveSavedArticlesToLocalFile(final JSONObject json) throws JSONException {
        settings.saveDriveSavedArticlesToLocalFile(json.toString());
    }

    private void saveLocalReadArticlesToLocalFile(final JSONObject json) throws JSONException {
        final JSONObject jsonLastModifiedDate = new JSONObject();
        jsonLastModifiedDate.put(DATE_KEY, new Date().getTime());
        json.put(LAST_MODIFIED_DATE, jsonLastModifiedDate);
        settings.saveLocalReadArticlesToLocalFile(json.toString());
    }

    private void saveDriveReadArticlesToLocalFile(final JSONObject json) throws JSONException {
        settings.saveDriveReadArticlesToLocalFile(json.toString());
    }

    private void saveLocalKeywordsToLocalFile(final JSONObject json) throws JSONException {
        final JSONObject jsonLastModifiedDate = new JSONObject();
        jsonLastModifiedDate.put(DATE_KEY, new Date().getTime());
        json.put(LAST_MODIFIED_DATE, jsonLastModifiedDate);
        settings.saveLocalKeywordsToLocalFile(json.toString());
    }

    private void saveDriveKeywordsToLocalFile(final JSONObject json) throws JSONException {
        settings.saveDriveKeywordsToLocalFile(json.toString());
    }

    private void saveLocalSavedFeedItemsToLocalFile(final JSONObject json) throws JSONException {
        final JSONObject jsonLastModifiedDate = new JSONObject();
        jsonLastModifiedDate.put(DATE_KEY, new Date().getTime());
        json.put(LAST_MODIFIED_DATE, jsonLastModifiedDate);
        settings.saveLocalSavedFeedItemsToLocalFile(json.toString());
    }

    private void saveDriveSavedFeedItemsToLocalFile(final JSONObject json) throws JSONException {
        settings.saveDriveSavedFeedItemsToLocalFile(json.toString());
    }

    private JSONObject loadLocalSavedArticlesFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadLocalSavedArticlesFromLocalFile());
    }

    private JSONObject loadDriveSavedArticlesFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadDriveSavedArticlesFromLocalFile());
    }

    private JSONObject loadLocalReadArticlesFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadLocalReadArticlesFromLocalFile());
    }

    private JSONObject loadDriveReadArticlesFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadDriveReadArticlesFromLocalFile());
    }

    private JSONObject loadLocalKeywordsFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadLocalKeywordsFromLocalFile());
    }

    private JSONObject loadDriveKeywordsFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadDriveKeywordsFromLocalFile());
    }

    private JSONObject loadLocalSavedFeedItemsFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadLocalSavedFeedItemsFromLocalFile());
    }

    private JSONObject loadDriveSavedFeedItemsFromLocalFile() throws JSONException {
        return getJsonObject(settings.loadDriveSavedFeedItemsFromLocalFile());
    }

    private JSONObject getJsonObject(String jsonString) throws JSONException {
        if (jsonString != null && jsonString.length() > 0) {
            return new JSONObject(jsonString);
        } else {
            return new JSONObject();
        }
    }

    private class SyncContentSavedArticles extends SyncContent {

        private static final String SAVED_ARTICLES_TITLE = "jasConfigSavedArticles.txt";

        public SyncContentSavedArticles(DriveControllerImpl driveController, GoogleApiClient googleApiClient) {
            super(driveController, googleApiClient);
        }

        @Override
        public boolean openDriveFile() {
            Logger.d(TAG, "SavedArticles.openDriveFile()");

            driveFile = _openDriveFile(SAVED_ARTICLES_TITLE);
            if (null == driveFile) {
                Logger.s(TAG, "SavedArticles.openDriveFile(): SAVED ARTICLES FILE CREATED or FAILED");
                return false;
            }

            return !isStop;
        }

        @Override
        public boolean needToMerge() {
            Logger.d(TAG, "SavedArticles.needToMerge()");

            // check drive SavedArticles file
            if (lastModifiedDateDriveFile != metadata.getModifiedDate().getTime()) {
                logMetadata();
                final String contentDriveFile = getDrivePropertyInfo(driveFile);
                if (null == contentDriveFile) {
                    Logger.s(TAG, "SavedArticles.needToMerge(): drive SavedArticles file cannot read content");
                    return false;
                }

                try {
                    final JSONObject json = getJsonObject(contentDriveFile);
                    saveDriveSavedArticlesToLocalFile(json);
                    fileSize = metadata.getFileSize();
                    Logger.d(TAG, "SavedArticles.needToMerge(): update drive SavedArticles file");
                } catch (JSONException e) {
                    Logger.s(TAG, "SavedArticles.needToMerge(): drive SavedArticles file has invalid content: " + contentDriveFile);
                    Logger.s(TAG, e);
                    return false;
                }

                lastModifiedDateDriveFile = metadata.getModifiedDate().getTime();
            } else {
                // check local SavedArticles file
                final JSONObject localSavedArticlesInfo;
                try {
                    localSavedArticlesInfo = loadLocalSavedArticlesFromLocalFile();
                    long lastModifiedDate = 0;
                    if (localSavedArticlesInfo.has(LAST_MODIFIED_DATE)) {
                        final JSONObject json = localSavedArticlesInfo.getJSONObject(LAST_MODIFIED_DATE);
                        lastModifiedDate = json.getLong(DATE_KEY);
                    } else if (0 == metadata.getFileSize()) {
                        Logger.d(TAG, "SavedArticles.needToMerge():  local SavedArticles file is empty");
                        return false;
                    }

                    if (lastModifiedDateLocalFile == lastModifiedDate) {
                        if (!mNeedToMerge) {
                            Logger.d(TAG, "SavedArticles.needToMerge(): 'modified date file' property not changed");
                            return false;
                        }
                    } else {
                        lastModifiedDateLocalFile = lastModifiedDate;
                    }
                } catch (JSONException e) {
                    Logger.s(TAG, "SavedArticles.needToMerge(): local SavedArticles file has invalid content");
                    Logger.s(TAG, e);
                    return false;
                }
            }

            mNeedToMerge = true;
            return !isStop;

        }

        @Override
        public boolean merge() {
            Logger.d(TAG, "SavedArticles.merge(): mNeedToMergeSavedArticles = " + mNeedToMerge);

            if (!mNeedToMerge) {
                return !isStop;
            }

            boolean isInvalidFormat = false;
            try {
                if (!waitUpdatingContent()) {
                    return false;
                }

                //setDriveProperty(savedArticlesFile, "{}");

                StringBuilder log = new StringBuilder("SavedArticles.merge(): before merge");

                final JSONObject driveSavedArticlesInfo = loadDriveSavedArticlesFromLocalFile();
                final JSONObject localSavedArticlesInfo = loadLocalSavedArticlesFromLocalFile();

                // log
                log.append("\ndriveSavedArticles:");
                logDriveSavedArticles(log, driveSavedArticlesInfo);
                log.append("\nlocalSavedArticles:");
                logLocalSavedArticles(log, localSavedArticlesInfo);


                final JSONObject mergeResult = new JSONObject();
                Iterator keys = driveSavedArticlesInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (!waitUpdatingContent()) {
                        return false;
                    }
                    if (localSavedArticlesInfo.has(key)) {
                        final boolean localState = localSavedArticlesInfo.getJSONObject(key).getBoolean(STATE_KEY);
                        final boolean driveState = driveSavedArticlesInfo.getJSONObject(key).getBoolean(STATE_KEY);
                        long localDate = localSavedArticlesInfo.getJSONObject(key).getLong(DATE_KEY);
                        long driveDate = driveSavedArticlesInfo.getJSONObject(key).getLong(DATE_KEY);

                        if (driveState == localState) {
                            mergeResult.put(key, driveSavedArticlesInfo.getJSONObject(key));
                        } else if (driveDate > localDate) {
                            mergeResult.put(key, driveSavedArticlesInfo.getJSONObject(key));
                            if (!updateFavoriteState(key, driveSavedArticlesInfo.getJSONObject(key))) {
                                return false;
                            }
                        } else {
                            mergeResult.put(key, localSavedArticlesInfo.getJSONObject(key));
                        }
                    } else {
                        mergeResult.put(key, driveSavedArticlesInfo.getJSONObject(key));
                        if (!updateFavoriteState(key, driveSavedArticlesInfo.getJSONObject(key))) {
                            return false;
                        }
                    }
                }

                // add from local
                keys = localSavedArticlesInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (!key.equals(LAST_MODIFIED_DATE) && !mergeResult.has(key)) {
                        log.append("\n'" + key + "' new article for drive");
                        mergeResult.put(key, localSavedArticlesInfo.getJSONObject(key));
                    }
                }

                // save to drive
                if (!JsonUtils.jsonObjectEquals(driveSavedArticlesInfo, mergeResult)) {
                    log.append("\nCOMMIT to drive:");
                    logDriveSavedArticles(log, mergeResult);

                    setDriveProperty(driveFile, mergeResult.toString());
                }

                //
                localSavedArticlesInfo.remove(LAST_MODIFIED_DATE);
                if (driveSavedArticlesInfo.length() == localSavedArticlesInfo.length()) {
                    boolean equalsContent = true;
                    keys = localSavedArticlesInfo.keys();
                    while (keys.hasNext()) {
                        final String key = (String) keys.next();
                        if (driveSavedArticlesInfo.has(key)) {
                            final String localStatus = localSavedArticlesInfo.getJSONObject(key).getString(STATE_KEY);
                            final String driveStatus = driveSavedArticlesInfo.getJSONObject(key).getString(STATE_KEY);
                            if (!localStatus.equals(driveStatus)) {
                                equalsContent = false;
                                break;
                            }
                        } else {
                            equalsContent = false;
                            break;
                        }
                    }

                    if (equalsContent) {
                        mNeedToMerge = false;
                    }
                }

                Logger.d(TAG, log.toString());

            } catch (JSONException e) {
                isInvalidFormat = true;
            }

            if (isInvalidFormat) {
                try {
                    Logger.s(TAG, "SavedArticles.merge(): INVALID CONTENT OF 'SAVED ARTICLES' FILE. RESET CONTENT: ===" + loadDriveSavedArticlesFromLocalFile().toString() + "===");
                    setDriveProperty(driveFile, "{}");
                } catch (JSONException e) {
                    Logger.s(TAG, e);
                }
            }

            return !isStop;
        }

        private void logDriveSavedArticles(final StringBuilder log, final JSONObject driveSavedArticlesInfo) throws JSONException {
            final Iterator keys = driveSavedArticlesInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = driveSavedArticlesInfo.getJSONObject(key);
                final Date date = new Date(json.getLong(DATE_KEY));
                log.append(String.format("\n.... state=%5b; date=%30s(%13d); doi=%30s;  ", json.getBoolean(STATE_KEY), formatDateGMT.format(date), date.getTime(), key));
            }
            log.append("\n....");
        }

        private void logLocalSavedArticles(final StringBuilder log, final JSONObject localSavedArticlesInfo) throws JSONException {
            final Iterator keys = localSavedArticlesInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = localSavedArticlesInfo.getJSONObject(key);
                final Date date = new Date(json.getLong(DATE_KEY));
                if (key.equals(LAST_MODIFIED_DATE)) {
                    log.append("\n.... last modified date=" + formatDateGMT.format(date));
                } else {
                    log.append(String.format("\n.... state=%5b; date=%30s(%13d); doi=%30s;  ", json.getBoolean(STATE_KEY), formatDateGMT.format(date), date.getTime(), key));
                }
            }
            log.append("\n....");
        }

        private boolean updateFavoriteState(String doi, JSONObject state) {
            ArticleMO article = articleService.getArticleQuietly(new DOI(doi));

            try {
                if (null == article) {
                    if (state.has("issue")) {
                        final String issueDoi = state.getString("issue");
                        if (null != issueDoi && !issueDoi.isEmpty()) {
                            try {
                                issueService.getIssue(new DOI(issueDoi));
                                importManager.updateIssuesTOC(new DOI(issueDoi));
                                Logger.d(TAG, "SavedArticles.updateFavoriteState(): for article[" + doi + "] updating issuesTOC[" + issueDoi + "]");
                            } catch (ElementNotFoundException e) {
                                Logger.d(TAG, "SavedArticles.updateFavoriteState(): for  article[" + doi + "] not found issue[" + issueDoi + "]");
                            }
                        } else {
                            Logger.s(TAG, "SavedArticles.updateFavoriteState(): for article[" + doi + "] issue.doi is EMPTY");
                        }
                    } else if (state.has("specialSection")) {
                        final String uid = state.getString("specialSection");
                        if (null == uid || uid.isEmpty()) {
                            final SpecialSectionMO specialSection = specialSectionService.getSpecialSectionById(uid);
                            if (specialSection != null) {
                                importManager.updateSpecialSection(uid);
                                Logger.d(TAG, "SavedArticles.updateFavoriteState(): for article[" + doi + "] updating SpecialSection[" + uid + "]");
                            } else {
                                Logger.d(TAG, "SavedArticles.updateFavoriteState(): for  article[" + doi + "] not found Special Section[" + uid + "]");
                            }
                        } else {
                            Logger.d(TAG, "SavedArticles.updateFavoriteState(): for  article[" + doi + "] updateSpecialSection.doi is EMPTY ");
                        }
                    }

                    return true;
                }

                Logger.d(TAG, "SavedArticles.updateFavoriteState(): doi = " + doi + "; state = " + state.getBoolean("state"));
                if (state.getBoolean("state")) {
                    articleService.addArticleRefToFavorites(article);
                } else {
                    articleService.removeArticleRefFromFavorites(article);
                }

            } catch (JSONException e) {
                return false;
            }

            return true;
        }
    }

    private class SyncContentReadArticles extends SyncContent {

        private static final String READ_ARTICLES_TITLE = "jasConfigReadArticles.txt";

        public SyncContentReadArticles(DriveControllerImpl driveController, GoogleApiClient googleApiClient) {
            super(driveController, googleApiClient);
        }

        @Override
        public boolean openDriveFile() {
            Logger.d(TAG, "ReadArticles.openDriveFile()");

            driveFile = _openDriveFile(READ_ARTICLES_TITLE);
            if (null == driveFile) {
                Logger.s(TAG, "ReadArticles.openDriveFile(): READ ARTICLES FILE CREATED or FAILED");
                return false;
            }

            return !isStop;
        }

        @Override
        public boolean needToMerge() {
            Logger.d(TAG, "ReadArticles.needToMerge()");

            // check drive ReadArticles file
            if (lastModifiedDateDriveFile != metadata.getModifiedDate().getTime()) {
                logMetadata();
                final String contentDriveFile = getDrivePropertyInfo(driveFile);
                if (null == contentDriveFile) {
                    Logger.s(TAG, "ReadArticles.needToMerge(): drive ReadArticles file cannot read content");
                    return false;
                }

                try {
                    final JSONObject json = getJsonObject(contentDriveFile);
                    saveDriveReadArticlesToLocalFile(json);
                    fileSize = metadata.getFileSize();
                    Logger.d(TAG, "ReadArticles.needToMerge(): update drive ReadArticles file");
                } catch (JSONException e) {
                    Logger.s(TAG, "ReadArticles.needToMerge(): drive ReadArticles file has invalid content: " + contentDriveFile);
                    Logger.s(TAG, e);
                    return false;
                }

                lastModifiedDateDriveFile = metadata.getModifiedDate().getTime();
            } else {
                // check local ReadArticles file
                final JSONObject localReadArticlesInfo;
                try {
                    localReadArticlesInfo = loadLocalReadArticlesFromLocalFile();
                    long lastModifiedDate = 0;
                    if (localReadArticlesInfo.has(LAST_MODIFIED_DATE)) {
                        final JSONObject json = localReadArticlesInfo.getJSONObject(LAST_MODIFIED_DATE);
                        lastModifiedDate = json.getLong(DATE_KEY);
                    } else if (0 == metadata.getFileSize()) {
                        Logger.d(TAG, "ReadArticles.needToMerge():  local ReadArticles file is empty");
                        return false;
                    }

                    if (lastModifiedDateLocalFile == lastModifiedDate) {
                        if (!mNeedToMerge) {
                            Logger.d(TAG, "ReadArticles.needToMerge(): 'modified date file' property not changed");
                            return false;
                        }
                    } else {
                        lastModifiedDateLocalFile = lastModifiedDate;
                    }
                } catch (JSONException e) {
                    Logger.s(TAG, "ReadArticles.needToMerge(): local ReadArticles file has invalid content");
                    Logger.s(TAG, e);
                    return false;
                }
            }

            mNeedToMerge = true;
            return !isStop;
        }

        @Override
        public boolean merge() {
            Logger.d(TAG, "ReadArticles.merge(): mNeedToMergeReadArticles = " + mNeedToMerge);

            if (!mNeedToMerge) {
                return !isStop;
            }

            boolean isInvalidFormat = false;
            try {
                if (!waitUpdatingContent()) {
                    return false;
                }

                StringBuilder log = new StringBuilder("ReadArticles.merge(): before merge");

                final JSONObject driveReadArticlesInfo = loadDriveReadArticlesFromLocalFile();
                final JSONObject localReadArticlesInfo = loadLocalReadArticlesFromLocalFile();

                log.append("\ndriveReadArticles:");
                logDriveReadArticles(log, driveReadArticlesInfo);
                log.append("\nlocalReadArticles:");
                logLocalReadArticles(log, localReadArticlesInfo);

                // merge
                Iterator keys = driveReadArticlesInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (!localReadArticlesInfo.has(key)) {
                        log.append("\n'" + key + "' new read article for local");
                        updateReadArticleProperty(new DOI(key));
                    }
                }

                // save drive in ICloud
                boolean isAdded = false;
                keys = localReadArticlesInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (!key.equals(LAST_MODIFIED_DATE) && !driveReadArticlesInfo.has(key)) {
                        log.append("\n'" + key + "' new read article for drive");
                        isAdded = true;
                        driveReadArticlesInfo.put(key, localReadArticlesInfo.getJSONObject(key));
                    }
                }

                if (isAdded) {
                    log.append("\nCOMMIT to drive:");
                    logDriveReadArticles(log, driveReadArticlesInfo);

                    setDriveProperty(driveFile, driveReadArticlesInfo.toString());
                }

                //
                localReadArticlesInfo.remove(LAST_MODIFIED_DATE);
                if (JsonUtils.jsonObjectEquals(driveReadArticlesInfo, localReadArticlesInfo)) {
                    mNeedToMerge = false;
                }

                Logger.d(TAG, log.toString());

            } catch (JSONException e) {
                isInvalidFormat = true;
            }

            if (isInvalidFormat) {
                try {
                    Logger.s(TAG, "ReadArticles.merge(): INVALID CONTENT OF 'READ ARTICLES' FILE. RESET CONTENT: ===" + loadDriveReadArticlesFromLocalFile().toString() + "===");
                    setDriveProperty(driveFile, "{}");
                } catch (JSONException e) {
                    Logger.s(TAG, e);
                }
            }

            return !isStop;
        }

        private void logDriveReadArticles(final StringBuilder log, final JSONObject driveReadArticlesInfo) throws JSONException {
            final Iterator keys = driveReadArticlesInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = driveReadArticlesInfo.getJSONObject(key);
                log.append("\n.... doi=" + key + "; state=" + json.getBoolean(STATE_KEY));
            }
            log.append("\n....");
        }

        private void logLocalReadArticles(final StringBuilder log, final JSONObject localReadArticlesInfo) throws JSONException {
            final Iterator keys = localReadArticlesInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = localReadArticlesInfo.getJSONObject(key);
                if (key.equals(LAST_MODIFIED_DATE)) {
                    log.append("\n.... last modified date=" + formatDateGMT.format(new Date(json.getLong(DATE_KEY))));
                } else {
                    log.append("\n.... doi=" + key + "; state=" + json.getBoolean(STATE_KEY));
                }
            }
            log.append("\n....");
        }

        private void updateReadArticleProperty(final DOI doi) {
            ArticleMO article = articleService.getArticleQuietly(doi);
            if (null == article) {
                Logger.d(TAG, "ReadArticles.merge(): article[" + doi.getValue() + "] not found");
            } else {
                articleService.markArticleAsRead(doi);
            }
        }
    }

    private class SyncContentKeywords extends SyncContent {

        private static final String KEYWORDS_TITLE = "jasConfigKeywords.txt";

        public SyncContentKeywords(DriveControllerImpl driveController, GoogleApiClient googleApiClient) {
            super(driveController, googleApiClient);
        }

        @Override
        public boolean openDriveFile() {
            Logger.d(TAG, "Keywords.openDriveFile()");

            if (!settings.isRegisteredDeviceOnMCS()) {
                articleService.updateListOfSubscribedKeywords();
                return false;
            }

            if (mChangedContent) {
                if (!waitUpdatingContent()) {
                    return false;
                }
                mChangedContent = false;
                articleService.updateListOfSubscribedKeywords();
            }

            driveFile = _openDriveFile(KEYWORDS_TITLE);
            if (null == driveFile) {
                Logger.s(TAG, "Keywords.openDriveFile(): KEYWORDS FILE CREATED or FAILED");
                return false;
            }

            return !isStop;
        }

        @Override
        public boolean needToMerge() {
            Logger.d(TAG, "Keywords.needToMerge()");

            // check drive keywords file
            if (lastModifiedDateDriveFile != metadata.getModifiedDate().getTime()) {
                logMetadata();
                final String contentDriveFile = getDrivePropertyInfo(driveFile);
                if (null == contentDriveFile) {
                    Logger.s(TAG, "Keywords.needToMerge(): drive keywords file cannot read content");
                    return false;
                }

                try {
                    final JSONObject json = getJsonObject(contentDriveFile);
                    saveDriveKeywordsToLocalFile(json);
                    fileSize = metadata.getFileSize();
                    Logger.d(TAG, "Keywords.needToMerge(): update drive keywords file");
                } catch (JSONException e) {
                    Logger.s(TAG, "Keywords.needToMerge(): drive keywords file has invalid content: " + contentDriveFile);
                    Logger.s(TAG, e);
                    return false;
                }

                lastModifiedDateDriveFile = metadata.getModifiedDate().getTime();
            } else {
                // check local keywords file
                final JSONObject localKeywordsInfo;
                try {
                    localKeywordsInfo = loadLocalKeywordsFromLocalFile();
                    long lastModifiedDate = 0;
                    if (localKeywordsInfo.has(LAST_MODIFIED_DATE)) {
                        final JSONObject json = localKeywordsInfo.getJSONObject(LAST_MODIFIED_DATE);
                        lastModifiedDate = json.getLong(DATE_KEY);
                    } else if (0 == metadata.getFileSize()) {
                        Logger.d(TAG, "Keywords.needToMerge():  local keywords file is empty");
                        return false;
                    }

                    if (lastModifiedDateLocalFile == lastModifiedDate) {
                        if (!mNeedToMerge) {
                            Logger.d(TAG, "Keywords.needToMerge(): 'modified date file' property not changed");
                            return false;
                        }
                    } else {
                        lastModifiedDateLocalFile = lastModifiedDate;
                    }
                } catch (JSONException e) {
                    Logger.s(TAG, "Keywords.needToMerge(): local keywords file has invalid content");
                    Logger.s(TAG, e);
                    return false;
                }
            }

            mNeedToMerge = true;
            return !isStop;

        }

        @Override
        public boolean merge() {
            Logger.d(TAG, "Keywords.merge(): mNeedToMergeKeywords = " + mNeedToMerge);

            if (!mNeedToMerge) {
                return !isStop;
            }

            boolean isInvalidFormat = false;
            try {
                if (!waitUpdatingContent()) {
                    return false;
                }

                StringBuilder log = new StringBuilder("Keywords.merge(): before merge");

                final JSONObject driveKeywordsInfo = loadDriveKeywordsFromLocalFile();
                final JSONObject localKeywordsInfo = loadLocalKeywordsFromLocalFile();

                // log
                log.append("\ndriveKeywords:");
                logDriveKeywords(log, driveKeywordsInfo);
                logLocalKeywords(log, localKeywordsInfo);

                // merge keywords
                final JSONObject mergeResult = new JSONObject();

                Iterator keys = driveKeywordsInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (localKeywordsInfo.has(key)) {
                        final String localStatus = localKeywordsInfo.getJSONObject(key).getString(STATUS_KEY);
                        final String driveStatus = driveKeywordsInfo.getJSONObject(key).getString(STATUS_KEY);
                        long localDate = localKeywordsInfo.getJSONObject(key).getLong(DATE_KEY);
                        long driveDate = driveKeywordsInfo.getJSONObject(key).getLong(DATE_KEY);

                        if (driveStatus.equals(localStatus)) {
                            log.append("\n'" + key + "' status equals. copy from drive");
                            mergeResult.put(key, driveKeywordsInfo.getJSONObject(key));
                        } else if (driveDate > localDate) {
                            log.append("\n'" + key + "' update status for local");
                            mergeResult.put(key, driveKeywordsInfo.getJSONObject(key));
                            articleService.changeKeyword(key, driveKeywordsInfo.getJSONObject(key).getString(STATUS_KEY));
                            mChangedContent = true;
                        } else {
                            log.append("\n'" + key + "' update status for drive");
                            mergeResult.put(key, localKeywordsInfo.getJSONObject(key));
                        }
                    } else {
                        log.append("\n'" + key + "' new keyword for local");
                        mergeResult.put(key, driveKeywordsInfo.getJSONObject(key));
                        articleService.changeKeyword(key, driveKeywordsInfo.getJSONObject(key).getString(STATUS_KEY));
                        mChangedContent = true;
                    }
                }

                // add keywords from local
                keys = localKeywordsInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (!key.equals(LAST_MODIFIED_DATE)) {
                        if (!mergeResult.has(key)) {
                            log.append("\n'" + key + "' new keyword for drive");
                            mergeResult.put(key, localKeywordsInfo.getJSONObject(key));
                        }
                    }
                }

                // save in iCloud
                if (!JsonUtils.jsonObjectEquals(driveKeywordsInfo, mergeResult)) {
                    // log
                    log.append("\nCOMMIT to drive:");
                    logDriveKeywords(log, mergeResult);

                    // save
                    setDriveProperty(driveFile, mergeResult.toString());
                }

                //
                localKeywordsInfo.remove(LAST_MODIFIED_DATE);
                if (driveKeywordsInfo.length() == localKeywordsInfo.length()) {
                    boolean equalsContent = true;
                    keys = localKeywordsInfo.keys();
                    while (keys.hasNext()) {
                        final String key = (String) keys.next();
                        if (driveKeywordsInfo.has(key)) {
                            final String localStatus = localKeywordsInfo.getJSONObject(key).getString(STATUS_KEY);
                            final String driveStatus = driveKeywordsInfo.getJSONObject(key).getString(STATUS_KEY);
                            if (!localStatus.equals(driveStatus)) {
                                equalsContent = false;
                                break;
                            }
                        } else {
                            equalsContent = false;
                            break;
                        }
                    }

                    if (equalsContent) {
                        mNeedToMerge = false;
                    }
                }

                Logger.d(TAG, log.toString());

            } catch (JSONException e) {
                isInvalidFormat = true;
            }

            if (isInvalidFormat) {
                try {
                    Logger.s(TAG, "Keywords.merge(): INVALID CONTENT OF 'KEYWORDS' FILE. RESET CONTENT: ===" + loadDriveKeywordsFromLocalFile().toString() + "===");
                    setDriveProperty(driveFile, "{}");
                } catch (JSONException e) {
                    Logger.s(TAG, e);
                }
            }

            return !isStop;
        }

        private void logDriveKeywords(final StringBuilder log, final JSONObject driveKeywordsInfo) throws JSONException {
            final Iterator keys = driveKeywordsInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = driveKeywordsInfo.getJSONObject(key);
                final Date date = new Date(json.getLong(DATE_KEY));
                log.append(String.format("\n.... status=%12s; date=%30s(%13d); keyword=%30s;  ", json.getString(STATUS_KEY), formatDateGMT.format(date), date.getTime(), key));
            }
            log.append("\n....");
        }

        private void logLocalKeywords(final StringBuilder log, final JSONObject localKeywordsInfo) throws JSONException {
            final Iterator keys = localKeywordsInfo.keys();
            log.append("\nlocalKeywords:");
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                JSONObject json = localKeywordsInfo.getJSONObject(key);
                if (key.equals(LAST_MODIFIED_DATE)) {
                    log.append("\n.... last modified date=" + formatDateGMT.format(new Date(json.getLong(DATE_KEY))));
                } else {
                    final Date date = new Date(json.getLong(DATE_KEY));
                    log.append(String.format("\n.... status=%12s; date=%30s(%13d); keyword=%30s;  ", json.getString(STATUS_KEY), formatDateGMT.format(date), date.getTime(), key));
                }
            }
            log.append("\n....");
        }
    }

    private class SyncContentSavedFeedItems extends SyncContent {

        private static final String SAVED_FEED_TITLE = "jasConfigSavedFeeds.txt";

        public SyncContentSavedFeedItems(DriveControllerImpl driveController, GoogleApiClient googleApiClient) {
            super(driveController, googleApiClient);
        }

        @Override
        public boolean openDriveFile() {
            Logger.d(TAG, "SavedFeedItems.openDriveFile()");

            driveFile = _openDriveFile(SAVED_FEED_TITLE);
            if (null == driveFile) {
                Logger.s(TAG, "SavedFeedItems.openDriveFile(): SAVED FEED ITEMS FILE CREATED or FAILED");
                return false;
            }

            return !isStop;
        }

        @Override
        public boolean needToMerge() {
            Logger.d(TAG, "SavedFeedItems.needToMerge()");

            // check drive SavedFeedItems file
            if (lastModifiedDateDriveFile != metadata.getModifiedDate().getTime()) {
                logMetadata();
                final String contentDriveFile = getDrivePropertyInfo(driveFile);
                if (null == contentDriveFile) {
                    Logger.s(TAG, "SavedFeedItems.needToMerge(): drive SavedFeedItems file cannot read content");
                    return false;
                }

                try {
                    final JSONObject json = getJsonObject(contentDriveFile);
                    saveDriveSavedFeedItemsToLocalFile(json);
                    fileSize = metadata.getFileSize();
                    Logger.d(TAG, "SavedFeedItems.needToMerge(): update drive SavedFeedItems file");
                } catch (JSONException e) {
                    Logger.s(TAG, "SavedFeedItems.needToMerge(): drive SavedFeedItems file has invalid content: " + contentDriveFile);
                    Logger.s(TAG, e);
                    return false;
                }

                lastModifiedDateDriveFile = metadata.getModifiedDate().getTime();
            } else {
                // check local SavedFeedItems file
                final JSONObject localSavedFeedItemsInfo;
                try {
                    localSavedFeedItemsInfo = loadLocalSavedFeedItemsFromLocalFile();
                    long lastModifiedDate = 0;
                    if (localSavedFeedItemsInfo.has(LAST_MODIFIED_DATE)) {
                        final JSONObject json = localSavedFeedItemsInfo.getJSONObject(LAST_MODIFIED_DATE);
                        lastModifiedDate = json.getLong(DATE_KEY);
                    } else if (0 == metadata.getFileSize()) {
                        Logger.d(TAG, "SavedFeedItems.needToMerge():  local SavedFeedItems file is empty");
                        return false;
                    }

                    if (lastModifiedDateLocalFile == lastModifiedDate) {
                        if (!mNeedToMerge) {
                            Logger.d(TAG, "SavedFeedItems.needToMerge(): 'modified date file' property not changed");
                            return false;
                        }
                    } else {
                        lastModifiedDateLocalFile = lastModifiedDate;
                    }
                } catch (JSONException e) {
                    Logger.s(TAG, "SavedFeedItems.needToMerge(): local SavedFeedItems file has invalid content");
                    Logger.s(TAG, e);
                    return false;
                }
            }

            mNeedToMerge = true;
            return !isStop;
        }

        @Override
        public boolean merge() {
            Logger.d(TAG, "SavedFeedItems.merge(): mNeedToMergeSavedFeedItems = " + mNeedToMerge);

            if (!mNeedToMerge) {
                return !isStop;
            }

            boolean isInvalidFormat = false;
            try {
                if (!waitUpdatingContent()) {
                    return false;
                }

                StringBuilder log = new StringBuilder("SavedFeedItems.merge(): before merge");

                final JSONObject driveSavedFeedItemsInfo = loadDriveSavedFeedItemsFromLocalFile();
                final JSONObject localSavedFeedItemsInfo = loadLocalSavedFeedItemsFromLocalFile();

                // log
                log.append("\ndriveSavedFeedItems:");
                logDriveSavedFeedItems(log, driveSavedFeedItemsInfo);
                log.append("\nlocalSavedArticles:");
                logLocalSavedFeedItems(log, localSavedFeedItemsInfo);


                final JSONObject mergeResult = new JSONObject();
                Iterator keys = driveSavedFeedItemsInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (localSavedFeedItemsInfo.has(key)) {
                        final boolean localState = localSavedFeedItemsInfo.getJSONObject(key).getBoolean(STATE_KEY);
                        final boolean driveState = driveSavedFeedItemsInfo.getJSONObject(key).getBoolean(STATE_KEY);
                        long localDate = localSavedFeedItemsInfo.getJSONObject(key).getLong(DATE_KEY);
                        long driveDate = driveSavedFeedItemsInfo.getJSONObject(key).getLong(DATE_KEY);

                        if (driveState == localState) {
                            log.append("\n'" + key + "' state equals.  copy from drive");
                            mergeResult.put(key, driveSavedFeedItemsInfo.getJSONObject(key));
                        } else if (driveDate > localDate) {
                            log.append("\n'" + key + "' change favorite=" + driveState + " for local");
                            mergeResult.put(key, driveSavedFeedItemsInfo.getJSONObject(key));
                            if (!updateFeedItemFavoriteState(key, driveState)) {
                                return false;
                            }
                        } else {
                            log.append("\n'" + key + "' change favorite=" + driveState + " for drive");
                            mergeResult.put(key, localSavedFeedItemsInfo.getJSONObject(key));
                        }
                    } else {
                        log.append("\n'" + key + "' new article for local ");
                        mergeResult.put(key, driveSavedFeedItemsInfo.getJSONObject(key));
                        if (!updateFeedItemFavoriteState(key, driveSavedFeedItemsInfo.getJSONObject(key).getBoolean(STATE_KEY))) {
                            return false;
                        }
                    }
                }

                // add from local
                keys = localSavedFeedItemsInfo.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    if (!key.equals(LAST_MODIFIED_DATE) && !mergeResult.has(key)) {
                        log.append("\n'" + key + "' new article for drive");
                        mergeResult.put(key, localSavedFeedItemsInfo.getJSONObject(key));
                    }
                }

                // save to drive
                if (!JsonUtils.jsonObjectEquals(driveSavedFeedItemsInfo, mergeResult)) {
                    log.append("\nCOMMIT to drive:");
                    logDriveSavedFeedItems(log, mergeResult);

                    setDriveProperty(driveFile, mergeResult.toString());
                }

                //
                localSavedFeedItemsInfo.remove(LAST_MODIFIED_DATE);
                if (driveSavedFeedItemsInfo.length() == localSavedFeedItemsInfo.length()) {
                    boolean equalsContent = true;
                    keys = localSavedFeedItemsInfo.keys();
                    while (keys.hasNext()) {
                        final String key = (String) keys.next();
                        if (driveSavedFeedItemsInfo.has(key)) {
                            final String localStatus = localSavedFeedItemsInfo.getJSONObject(key).getString(STATE_KEY);
                            final String driveStatus = driveSavedFeedItemsInfo.getJSONObject(key).getString(STATE_KEY);
                            if (!localStatus.equals(driveStatus)) {
                                equalsContent = false;
                                break;
                            }
                        } else {
                            equalsContent = false;
                            break;
                        }
                    }

                    if (equalsContent) {
                        mNeedToMerge = false;
                    }
                }

                Logger.d(TAG, log.toString());

            } catch (JSONException e) {
                isInvalidFormat = true;
            }

            if (isInvalidFormat) {
                try {
                    Logger.s(TAG, "SavedFeedItems.merge(): INVALID CONTENT OF 'SAVED FEED ITEMS' FILE. RESET CONTENT: ===" + loadDriveSavedFeedItemsFromLocalFile().toString() + "===");
                    setDriveProperty(driveFile, "{}");
                } catch (JSONException e) {
                    Logger.s(TAG, e);
                }
            }

            return !isStop;
        }


        private void logDriveSavedFeedItems(final StringBuilder log, final JSONObject driveSavedFeedItemsInfo) throws JSONException {
            final Iterator keys = driveSavedFeedItemsInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = driveSavedFeedItemsInfo.getJSONObject(key);
                final Date date = new Date(json.getLong(DATE_KEY));
                log.append(String.format("\n.... state=%5b; date=%30s(%13d); uid=%30s;  ", json.getBoolean(STATE_KEY), formatDateGMT.format(date), date.getTime(), key));
            }
            log.append("\n....");
        }

        private void logLocalSavedFeedItems(final StringBuilder log, final JSONObject localSavedFeedItemsInfo) throws JSONException {
            final Iterator keys = localSavedFeedItemsInfo.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                final JSONObject json = localSavedFeedItemsInfo.getJSONObject(key);
                final Date date = new Date(json.getLong(DATE_KEY));
                if (key.equals(LAST_MODIFIED_DATE)) {
                    log.append("\n.... last modified date=" + formatDateGMT.format(date));
                } else {
                    log.append(String.format("\n.... state=%5b; date=%30s(%13d); uid=%30s;  ", json.getBoolean(STATE_KEY), formatDateGMT.format(date), date.getTime(), key));
                }
            }
            log.append("\n....");
        }

        private boolean updateFeedItemFavoriteState(final String uid, final boolean favoriteState) {
            homePageService.updateFavoriteState(uid, favoriteState);
            return true;
        }
    }

    private abstract static class SyncContent {

        private final GoogleApiClient googleApiClient;
        private final DriveControllerImpl driveController;

        private boolean isMetaDataRetrieved;
        protected Metadata metadata;

        protected long lastModifiedDateDriveFile = -1;
        protected long lastModifiedDateLocalFile = -1;
        protected long fileSize = -1;
        protected boolean mNeedToMerge = false;
        protected boolean mChangedContent = false;

        protected DriveFile driveFile;

        ResultCallback<DriveResource.MetadataResult> metadataRetrievedCallback = new
                ResultCallback<DriveResource.MetadataResult>() {
                    @Override
                    public void onResult(DriveResource.MetadataResult result) {
                        isMetaDataRetrieved = true;
                        if (!result.getStatus().isSuccess()) {
                            metadata = null;
                            Logger.s(TAG, "Problem while trying to fetch metadata");
                            return;
                        }

                        metadata = result.getMetadata();
                    }
                };

        public SyncContent(final DriveControllerImpl driveController, final GoogleApiClient googleApiClient) {
            this.driveController = driveController;
            this.googleApiClient = googleApiClient;
        }

        public long getModifiedDate() {
            return lastModifiedDateDriveFile;
        }

        public long getFileSize() {
            return fileSize;
        }

        abstract public boolean openDriveFile();

        abstract public boolean needToMerge();

        abstract public boolean merge();

        protected DriveFile _openDriveFile(final String title) {
            Drive.DriveApi.requestSync(googleApiClient).await();

            final DriveFile driveFile = getDriveFile(title);
            if (null == driveFile) {
                return null;
            }

            if (!waitMetadataFile(driveFile)) {
                return null;
            }

            if (null == metadata) {
                return null;
            }

            return driveFile;
        }

        private boolean waitMetadataFile(final DriveFile driveFile) {
            isMetaDataRetrieved = false;
            driveFile.getMetadata(googleApiClient).setResultCallback(metadataRetrievedCallback);
            while (!driveController.isStop && !isMetaDataRetrieved) {
                if (!driveController.isConnected()) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return false;
                }
            }

            return !driveController.isStop;
        }

        private DriveFile getDriveFile(String title) {
            final DriveApi.MetadataBufferResult result = queryForFile(title);

            if (!result.getStatus().isSuccess()) {
                Logger.s(TAG, "Problem while retrieving files");
                return null;
            }

            if (result.getMetadataBuffer().getCount() > 0) {
                final Metadata metadata = result.getMetadataBuffer().get(0);
                return Drive.DriveApi.getFile(googleApiClient, metadata.getDriveId());
            } else {
                createNewFile(title);
                return null;
            }
        }

        private DriveApi.MetadataBufferResult queryForFile(String title) {
            final SortOrder sortOrder = new SortOrder.Builder()
                    .addSortDescending(SortableField.MODIFIED_DATE).build();

            final Query query = new Query.Builder()
                    .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, title),
                            Filters.eq(SearchableField.MIME_TYPE, "text/plain")))
                    .setSortOrder(sortOrder)
                    .build();

            return Drive.DriveApi.getAppFolder(googleApiClient)
                    .queryChildren(googleApiClient, query)
                    .await();
        }

        private DriveFile createNewFile(final String title) {
            DriveApi.DriveContentsResult result = Drive.DriveApi.newDriveContents(googleApiClient)
                    .await();

            if (!result.getStatus().isSuccess()) {
                Logger.s(TAG, "Error while trying to create new file contents");
                return null;
            }

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(title)
                    .setMimeType("text/plain")
                    .build();
            DriveFolder.DriveFileResult fileResult = Drive.DriveApi.getAppFolder(googleApiClient)
                    .createFile(googleApiClient, changeSet, null)
                    .await();

            if (!fileResult.getStatus().isSuccess()) {
                Logger.s(TAG, "Error while trying to create the file");
                return null;
            }
            final DriveFile file = fileResult.getDriveFile();
            Drive.DriveApi.requestSync(googleApiClient).await();
            return file;
        }

        protected void logMetadata() {
            Logger.d(TAG, "Metadata:"
                    //+ "\n   Title: " + metadata.getTitle()
                    + "\n   Original File name     = " + metadata.getOriginalFilename()
                    //+ "\n   isPinnable             = " + metadata.isPinnable()
                    //+ "\n   isPinned               = " + metadata.isPinned()
                    //+ "\n   isRestricted           = " + metadata.isRestricted()
                    //+ "\n   isStarred              = " + metadata.isStarred()
                    //+ "\n   isEditable             = " + metadata.isEditable()
                    + "\n   File size              = " + metadata.getFileSize()
                    + "\n   Created date           = " + driveController.formatDateGMT.format(metadata.getCreatedDate()) + "(" + metadata.getCreatedDate().getTime() + ")"
                    + "\n   Modified date          = " + driveController.formatDateGMT.format(metadata.getModifiedDate()) + "(" + metadata.getModifiedDate().getTime() + ")"
                    + "\n   Modified by me date    = " + driveController.formatDateGMT.format(metadata.getModifiedByMeDate()) + (null == metadata.getModifiedByMeDate() ? "" : "(" + metadata.getModifiedByMeDate().getTime() + ")")
                    + "\n   Last viewed by me date = " + driveController.formatDateGMT.format(metadata.getLastViewedByMeDate()) + (null == metadata.getLastViewedByMeDate() ? "" : "(" + metadata.getLastViewedByMeDate().getTime() + ")"));
        }

        protected String getDrivePropertyInfo(final DriveFile file) {
            if (file == null) {
                return null;
            }

            DriveApi.DriveContentsResult driveContentsResult = file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            DriveContents driveContents = driveContentsResult.getDriveContents();
            if (null == driveContents) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(driveContents.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                return builder.toString();
            } catch (IOException e) {
                Logger.s(TAG, e);
            } finally {
                driveContents.discard(googleApiClient);
            }
            return null;
        }


        protected void setDriveProperty(final DriveFile file, final String value) throws JSONException {
            if (file == null) {
                return;
            }

            final DriveApi.DriveContentsResult driveContentsResult =
                    file.open(googleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();

            if (!driveContentsResult.getStatus().isSuccess()) {
                return;
            }

            final DriveContents driveContents = driveContentsResult.getDriveContents();

            try {
                OutputStream outputStream = driveContents.getOutputStream();
                outputStream.write(value.getBytes());
                final Status status = driveContents.commit(googleApiClient, null).await();
                if (status.isSuccess()) {
                    Drive.DriveApi.requestSync(googleApiClient).await();
                }
            } catch (IOException e) {
                Logger.s(TAG, e);
                driveContents.discard(googleApiClient);
            }
        }
    }
}
