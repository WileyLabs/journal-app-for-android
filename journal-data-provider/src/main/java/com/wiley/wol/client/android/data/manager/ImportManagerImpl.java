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
package com.wiley.wol.client.android.data.manager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.wiley.wol.client.android.data.dao.ArticleDao;
import com.wiley.wol.client.android.data.dao.IssueDao;
import com.wiley.wol.client.android.data.http.Resource;
import com.wiley.wol.client.android.data.http.ResourceManager;
import com.wiley.wol.client.android.data.manager.listener.AdvertisementFeedListener;
import com.wiley.wol.client.android.data.manager.listener.AffiliationFeedListener;
import com.wiley.wol.client.android.data.manager.listener.AnnouncementFeedListener;
import com.wiley.wol.client.android.data.manager.listener.EarlyViewFeedListener;
import com.wiley.wol.client.android.data.manager.listener.FeedItemContentListener;
import com.wiley.wol.client.android.data.manager.listener.HomePageFeedListener;
import com.wiley.wol.client.android.data.manager.listener.InAppListener;
import com.wiley.wol.client.android.data.manager.listener.IssueListFeedListener;
import com.wiley.wol.client.android.data.manager.listener.IssueTocFeedListener;
import com.wiley.wol.client.android.data.manager.listener.RestrictedStatusFeedListener;
import com.wiley.wol.client.android.data.manager.listener.SocietyFeedListener;
import com.wiley.wol.client.android.data.manager.listener.SpecialSectionFeedListener;
import com.wiley.wol.client.android.data.manager.listener.SpecialSectionsFeedListener;
import com.wiley.wol.client.android.data.manager.listener.TPSFeedListener;
import com.wiley.wol.client.android.data.manager.notification.ArticleLoadNotificationProcessor;
import com.wiley.wol.client.android.data.manager.notification.IssueLoadNotificationProcessor;
import com.wiley.wol.client.android.data.xml.ArticleSimpleParser;
import com.wiley.wol.client.android.data.xml.IssueSimpleParser;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.LastModifiedManager;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wiley.wol.client.android.data.http.DownloadManager.TPS;
import static com.wiley.wol.client.android.data.http.DownloadOperation.DOI;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE_TYPE;
import static com.wiley.wol.client.android.data.manager.ResourceType.ARTICLE_ZIP;
import static com.wiley.wol.client.android.data.manager.ResourceType.ISSUE_ZIP;
import static com.wiley.wol.client.android.notification.EventList.AFFILIATION_INFO_NEED_UPDATE;
import static com.wiley.wol.client.android.notification.EventList.DONE_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.ERROR_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.IN_APP_NEED_UPDATE;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_NEED_UPDATE;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_REMOVED;
import static com.wiley.wol.client.android.notification.EventList.PROCESS_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;

public class ImportManagerImpl implements ImportManager {
    private static final String TAG = ImportManagerImpl.class.getSimpleName();
    private static final String TAG_KEYWORDS = ImportManagerImpl.class.getSimpleName() + ".keywords";

    @Inject
    private ResourceManager resourceManager;
    @Inject
    private IssueSimpleParser issueParser;
    @Inject
    private IssueListFeedListener issueListFeedListener;
    @Inject
    private IssueTocFeedListener issueTocFeedListener;
    @Inject
    private InAppListener inAppListener;
    @Inject
    private SocietyFeedListener societyFeedListener;
    @Inject
    private SpecialSectionsFeedListener specialSectionsFeedListener;
    @Inject
    private SpecialSectionFeedListener specialSectionFeedListener;
    @Inject
    private TPSFeedListener tpsFeedListener;
    @Inject
    private AnnouncementFeedListener announcementFeedListener;
    @Inject
    private IssueLoadNotificationProcessor issueLoadNotificationProcessor;
    @Inject
    private AdvertisementFeedListener advertisementFeedListener;
    @Inject
    private HomePageFeedListener homePageFeedListener;
    @Inject
    private RssUpdateListenerProvider rssUpdateListenerProvider;
    @Inject
    private FeedItemContentListener feedItemContentListener;
    @Inject
    private AffiliationFeedListener affiliationFeedListener;
    @Inject
    private RestrictedStatusFeedListener restrictedFeedListener;
    @Inject
    private LastModifiedManager lastModifiedManager;
    @Inject
    private Settings settings;

    private final NotificationCenter notificationCenter;

    @Inject
    private IssueDao issueDao;

    private FeedsInfo feedsInfo;
    @Inject
    private EarlyViewFeedListener earlyViewFeedListener;

    @Inject
    public ImportManagerImpl(final ArticleSimpleParser articleParser,
                             final ArticleDao articleDao,
                             final NotificationCenter notificationCenter,
                             final Provider<FeedsInfo> feedsInfoProvider) {

        this.notificationCenter = notificationCenter;

        feedsInfo = feedsInfoProvider.get();

        this.notificationCenter.subscribeToNotification(DONE_RESOURCE_DOWNLOADING.getEventName(),
                new ArticleLoadNotificationProcessor(
                        articleParser,
                        articleDao,
                        notificationCenter)
        );

        this.notificationCenter.subscribeToNotification(DONE_RESOURCE_DOWNLOADING.getEventName(),
                new NotificationProcessor() {
                    @Override
                    public void processNotification(Map<String, Object> params) {
                        if (paramsHaveIssueZipResource(params)) {
                            issueLoadNotificationProcessor.processNotification(params);
                        }
                    }
                });

        this.notificationCenter.subscribeToNotification(ERROR_RESOURCE_DOWNLOADING.getEventName(),
                new NotificationProcessor() {
                    @Override
                    public void processNotification(Map<String, Object> params) {
                        if (paramsHaveIssueZipResource(params)) {
                            notificationCenter.sendNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), params);
                        }
                    }
                });

        this.notificationCenter.subscribeToNotification(PROCESS_RESOURCE_DOWNLOADING.getEventName(),
                new NotificationProcessor() {
                    @Override
                    public void processNotification(Map<String, Object> params) {
                        if (paramsHaveIssueZipResource(params)) {
                            notificationCenter.sendNotification(ISSUE_DOWNLOAD_PROGRESS.getEventName(), params);
                        }
                    }
                });

        this.notificationCenter.subscribeToNotification(SETTINGS_CHANGED.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                if ("current_server".equals(params.get(SETTING_NAME_KEY))) {
                    feedsInfo = feedsInfoProvider.get();
                }
            }
        });

        this.notificationCenter.subscribeToNotification(ISSUE_REMOVED.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                final String url = feedsInfo.getIssueTocPrefix() + ((DOI) params.get("doi")).getValue();
                lastModifiedManager.removeLastModified(url);
            }
        });

        this.notificationCenter.subscribeToNotification(ISSUE_LIST_NEED_UPDATE.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                updateIssueList();
            }
        });

        this.notificationCenter.subscribeToNotification(IN_APP_NEED_UPDATE.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                updateInAppContent();
            }
        });

        this.notificationCenter.subscribeToNotification(AFFILIATION_INFO_NEED_UPDATE.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                updateAffiliationFeed();
            }
        });
    }

    private boolean paramsHaveIssueZipResource(Map<String, Object> params) {
        final Resource resource = (Resource) params.get(RESOURCE);
        final ResourceType resourceType = resource.getResourceType();
        return ISSUE_ZIP == resourceType;
    }

    @Override
    public void updateEarlyViewFeed() {
        Logger.d(TAG, "updateEarlyViewFeed");
        resourceManager.addSmallTask(feedsInfo.getEarlyViewFeed(),
                earlyViewFeedListener);
    }

    @Override
    public void updateIssueList() {
        Logger.d(TAG, "updateIssueList");
        resourceManager.addSmallTask(feedsInfo.getIssueListFeed(), issueListFeedListener);
    }

    @Override
    public void updateRestrictedFeed(final JSONObject knownArticlesJson) {
        resourceManager.executeJsonRequest(feedsInfo.getRestrictedFeed(), knownArticlesJson, restrictedFeedListener);
    }

    @Override
    public void updateSpecialSections() {
        Logger.d(TAG, "updateSpecialSections");
        resourceManager.addSmallTask(feedsInfo.getSpecialSectionsListFeed(), specialSectionsFeedListener);
    }

    @Override
    public boolean isUpdatingEarlyView() {
        return resourceManager.hasRunningListenerTask(feedsInfo.getEarlyViewFeed());
    }

    @Override
    public boolean isUpdatingIssueList() {
        return resourceManager.hasRunningListenerTask(feedsInfo.getIssueListFeed());
    }

    @Override
    public boolean isUpdatingSpecialSections() {
        return resourceManager.hasRunningListenerTask(feedsInfo.getSpecialSectionsListFeed());
    }

    @Override
    public boolean isUpdating() {
        return resourceManager.hasRunningListenerTask();
    }

    @Override
    public void loadArticle(final DOI doi) {
        Logger.d(TAG, "loadArticle");
        final String url = feedsInfo.getArticleZipPrefix() + doi.getValue();
        final HashMap<String, Object> params = new HashMap<>();
        params.put(DOI, doi);
        params.put(RESOURCE_TYPE, ARTICLE_ZIP);
        resourceManager.addBigTask(url, doi.getArticleZipCompatibleValue() + ".zip", params);
    }

    @Override
    public void loadIssue(DOI doi) {
        Logger.d(TAG, "loadIssue");
        final String issueFileName = "issue_"
                + doi.getAssetCompatibleValue() + ".zip";

        final String url = feedsInfo.getIssueZipPrefix() + doi.getValue();
        final HashMap<String, Object> params = new HashMap<>();
        params.put(Settings.DOWNLOAD_ISSUE, doi);
        params.put(RESOURCE_TYPE, ISSUE_ZIP);
        resourceManager.addSmallTask(url, "zips", issueFileName, true, params);
    }

    @Override
    public void stopIssueLoading(DOI doi) {
        final String url = feedsInfo.getIssueZipPrefix() + doi.getValue();
        resourceManager.cancelTask(url);
    }

    @Override
    public void removeLoadedIssue(DOI doi) {
        final String issueFileName = "issue_" + doi.getAssetCompatibleValue() + ".zip";
        final String url = feedsInfo.getIssueZipPrefix() + doi.getValue();
        resourceManager.removeLoadedFile(url, issueFileName);
    }

    @Override
    public boolean isIssueLoading(DOI doi) {
        final String url = feedsInfo.getIssueZipPrefix() + doi.getValue();
        return resourceManager.hasRunningTask(url);
    }

    @Override
    public void updateIssuesTOC(final DOI doi) {
        Logger.d(TAG, "updateIssuesTOC");
        resourceManager.addSmallTask(feedsInfo.getIssueTocPrefix() + doi.getValue(), issueTocFeedListener);
    }

    @Override
    public void updateSpecialSection(final String id) {
        Logger.d(TAG, "SpecialSection: id = ");
        String encodedId = id;
        try {
            encodedId = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.s(TAG, e);
        }

        resourceManager.addSmallTask(feedsInfo.getSpecialSectionPrefix() + encodedId, specialSectionFeedListener);
    }

    @Override
    public void updateInAppContent() {
        Logger.d(TAG, "updateInAppContent");
        resourceManager.addSmallTask(feedsInfo.getInAppContentFeed(), inAppListener);
    }

    @Override
    public void updateSocietyFeed() {
        Logger.d(TAG, "updateSocietyFeed");
        resourceManager.addSmallTask(feedsInfo.getSocietyFeed(), societyFeedListener);
    }

    @Override
    public void updateTPSFeed() {
        Logger.d(TAG, "updateTPSFeed");
        final HashMap<String, Object> params = new HashMap<>(1);
        params.put(TPS, true);
        resourceManager.addSmallTask(feedsInfo.getTPSFeed(), tpsFeedListener, params);
    }

    @Override
    public void updateAdvertisementFeed() {
        Logger.d(TAG, "updateAdvertisementFeed");
        resourceManager.addSmallTask(feedsInfo.getAdvertisementFeed(), advertisementFeedListener);
    }

    @Override
    public void updateHomePageFeed() {
        Logger.d(TAG, "updateHomePageFeed(): url = " + feedsInfo.getHomeScreenFeed());
        // url=http://spa-as-dev.wiley.com/JAS-ACIE/journalApp/inAppContent.feed?contentKey=ALL_SOCIETY_CONTENT&contentKey=feedback_email
        resourceManager.addSmallTask(feedsInfo.getHomeScreenFeed(), homePageFeedListener);
    }

    @Override
    public void updateRssFeed(FeedMO feed) {
        resourceManager.addSmallTask(feed.getUrl(),
                rssUpdateListenerProvider.getListener(feed),
                new ParamsBuilder()
                        .withParam(NotificationCenter.IGNORE_LAST_MODIFIED, true)
                        .get()
        );
    }

    @Override
    public void updateFeedItemContent(final FeedItemMO feedItem) {
        Logger.d(TAG, "updateFeedItemContent. url = " + feedItem.getUrl());
        // http://www.ejnnews.org/news/review-article-the-diverse-roles-and-multiple-forms-of-focal-adhesion-kinase-in-brain
        notificationCenter.sendNotification(EventList.FEED_ITEM_CONTENT_STARTED.getEventName());
        resourceManager.updateFeedItemContent(feedItem.getUrl(), feedItem.getFeed().getParams(), feedItemContentListener, feedsInfo.getRssContentFeed());
    }

    @Override
    public void updateArticleInfoHtmlBody(final ArticleMO article) {
        Logger.d(TAG, "updateArticleInfoHtmlBody. doi = " + article.getDOI().getValue());

        final String deviceToken = settings.getDeviceToken();
        if (null == deviceToken || deviceToken.equals("")) {
            Logger.d(TAG, "INFO_ARTICLE_UPDATE_ERROR: DEVICE TOKEN UNDEFINED");
            notificationCenter.sendNotification(EventList.INFO_ARTICLE_UPDATE_FINISHED.getEventName());
            return;
        }
        final String url = feedsInfo.getArticleInfoPrefix() + article.getDOI().getValue() + "&devToken=" + deviceToken;

        resourceManager.updateArticleInfoHtmlBody(url, new Listener<InputStream>() {
            @Override
            public void onComplete(InputStream result, Object... additionalData) throws Exception {
                Logger.d(TAG, "INFO_ARTICLE_UPDATE_SUCCESS");

                // prepare output stream
                final File saveDir = new File(resourceManager.getArticleLocalPath(article.getDOI()));
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                }

                final String filePath = resourceManager.getArticleLocalPath(article.getDOI()) + "/article_info.html";

                final File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }

                FileOutputStream fileOutputStream = new FileOutputStream(file);

                // copy stream
                IOUtils.copy(result, fileOutputStream);

                notificationCenter.sendNotification(EventList.INFO_ARTICLE_UPDATE_FINISHED.getEventName());
            }

            @Override
            public void onNotModified() {
                Logger.d(TAG, "INFO_ARTICLE_UPDATE_NOT_MODIFIED");
                notificationCenter.sendNotification(EventList.INFO_ARTICLE_UPDATE_FINISHED.getEventName());
            }

            @Override
            public void onError(Exception ex) {
                Logger.d(TAG, "INFO_ARTICLE_UPDATE_ERROR");
                final HashMap<String, Object> params = new HashMap<>(1);
                params.put(NotificationCenter.ERROR, ex);
                notificationCenter.sendNotification(EventList.INFO_ARTICLE_UPDATE_FINISHED.getEventName(), params);
            }
        });
    }

    @Override
    public void changeKeyword(final String keyword, final String action) {
        notificationCenter.sendNotification(EventList.KEYWORD_UPDATE_STARTED.getEventName());
        final String deviceToken = settings.getDeviceToken();
        if (null == deviceToken || deviceToken.equals("") || !settings.isRegisteredDeviceOnMCS()) {
            Logger.d(TAG_KEYWORDS, "changeKeyword(): device not registered on MCS");
            notificationCenter.sendNotification(EventList.KEYWORD_UPDATE_FINISHED.getEventName(),
                    new ParamsBuilder()
                            .succeed(false)
                            .get());
            return;
        }
        // http://spa-as-qa.wiley.com/JAS-ACIE/journalApp/keywords.feed
        final String url = feedsInfo.getKeywordsFeed();

        Logger.d(TAG_KEYWORDS, "changeKeyword(): action = " + action
                + "\n keyword = " + keyword
                + "\n url = " + url
                + "\n deviceToken = " + deviceToken);

        resourceManager.changeKeyword(url, keyword, action, deviceToken, new Listener<JSONObject>() {
            @Override
            public void onComplete(JSONObject json, Object... additionalData) throws Exception {
                Logger.d(TAG_KEYWORDS, "changeKeyword(): KEYWORD_UPDATE_SUCCESS");

                try {
                    final String action = json.getString("styleClass");
                    final String keyword = json.getString("keyword");
                    if (null != action && !action.equals("") && null != keyword && !keyword.equals("")) {
                        if (action.equals("subscribed")) {
                            settings.addKeyword(keyword);
                        } else if (action.equals("unsubscribed")) {
                            settings.removeKeyword(keyword);
                        }
                    }
                } catch (JSONException ignored) {
                }

                notificationCenter.sendNotification(EventList.KEYWORD_UPDATE_FINISHED.getEventName(),
                        new ParamsBuilder()
                                .succeed(true)
                                .withParam(NotificationCenter.KEYWORD_JSON, json)
                                .get());
            }

            @Override
            public void onNotModified() {
            }

            @Override
            public void onError(Exception ex) {
                Logger.s(TAG_KEYWORDS, "changeKeyword(): KEYWORD_UPDATE_ERROR", ex);
                notificationCenter.sendNotification(EventList.KEYWORD_UPDATE_FINISHED.getEventName(),
                        new ParamsBuilder()
                                .succeed(false)
                                .get());
            }
        });
    }

    @Override
    public void updateListOfSubscribedKeywords() {
        final String deviceToken = settings.getDeviceToken();
        if (null == deviceToken || deviceToken.equals("")) {
            Logger.d(TAG_KEYWORDS, "updateListOfSubscribedKeywords(): SUBSCRIBED_KEYWORDS_UPDATE_ERROR: DEVICE TOKEN UNDEFINED");
            return;
        }

        // http://spa-as-qa.wiley.com/JAS-ACIE/journalApp/keywordsInfo.feed?devToken=d97316505dc41220eb8aa1120d133d972aadf05fcc19cd418b5a2f448c5d42c4
        final String url = feedsInfo.getSubscribedKeywordsFeed() + "devToken=" + deviceToken;

        Logger.d(TAG_KEYWORDS, "updateListOfSubscribedKeywords():"
                + "\n url = " + url
                + "\n deviceToken = " + deviceToken);

        resourceManager.updateListOfSubscribedKeywords(url, new Listener<JSONObject>() {
            @Override
            public void onComplete(JSONObject json, Object... additionalData) throws Exception {
                Logger.d(TAG_KEYWORDS, "updateListOfSubscribedKeywords(): SUBSCRIBED_KEYWORDS_UPDATE_SUCCESS: " + json.toString());

                if (!settings.isRegisteredDeviceOnMCS()) {
                    if (json.toString().equals("{\"arr\":[]}")) {
                        settings.setRegisteredDeviceOnMCS(true);
                        notificationCenter.sendNotification(EventList.KEYWORDS_DEVICE_REGISTERED_ON_MCS.getEventName());
                        Logger.d(TAG_KEYWORDS, "updateListOfSubscribedKeywords(): registered device on MCS");
                    }
                } else {
                    List<String> keywordList = new ArrayList<String>(json.length());
                    JSONArray jsonArray = json.getJSONArray("arr");
                    for (int i=0; i<jsonArray.length(); i++) {
                        keywordList.add(jsonArray.getString(i));
                    }
                    settings.updateKeywords(keywordList);

                    notificationCenter.sendNotification(EventList.KEYWORDS_UPDATED.getEventName());
                }
            }

            @Override
            public void onNotModified() {
            }

            @Override
            public void onError(Exception ex) {
                Logger.d(TAG_KEYWORDS, "updateListOfSubscribedKeywords(): SUBSCRIBED_KEYWORDS_UPDATE_ERROR");
            }
        });

    }

    @Override
    public void updateAnnouncementFeed() {
        resourceManager.addSmallTask(feedsInfo.getAnnouncementFeed(), announcementFeedListener);
    }

    @Override
    public void updateAffiliationFeed() {
        resourceManager.addSmallTask(feedsInfo.getAffiliationFeed(), affiliationFeedListener);
    }
}
