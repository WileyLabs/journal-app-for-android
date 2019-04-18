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
package com.wiley.wol.client.android.data.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.adobe.mobile.Analytics;
import com.adobe.mobile.Config;
import com.google.inject.Inject;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.settings.Environment;
import com.wiley.wol.client.android.settings.Settings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alobachev on 11/11/14.
 */
public class AANHelper {

    private static final String TAG = AANHelper.class.getSimpleName();

    public static final String TAB_TITLE_EARLY_VIEW = "Early View";
    public static final String TAB_TITLE_ISSUES = "Issues";
    public static final String TAB_TITLE_SPECIAL_SECTIONS = "Special Sections";
    public static final String TAB_TITLE_SAVED_ARTICLES = "Saved Articles";
    public static final String TAB_TITLE_SETTINGS = "Settings";
    public static final String TAB_TITLE_ABOUT = "About";
    public static final String TAB_TITLE_SEARCH = "Search";

    private static final String KEY_PAGE_TYPE = "pagetype";
    private static final String KEY_PAGE_SUBTYPE = "pagesubtype";
    private static final String KEY_OVERLAY_TYPE = "overlaytype";
    private static final String KEY_ARTICLE_DOI= "articledoi";
    private static final String KEY_ARTICLE_AVAILABILITY = "articleavailability";
    private static final String KEY_ARTICLE_AGE_IN_DAYS = "articleageindays";
    private static final String KEY_ARTICLE_SAVED_OR_NOT = "articlesavedornot";
    private static final String KEY_ISSUE_DOI = "issuedoi";
    private static final String KEY_ISSUE_DOWNLOADED_OR_NOT = "issuedownloadedornot";
    private static final String KEY_FIGURE_ID = "figureid";
    private static final String KEY_SIDEBAR_TYPE = "sidebartype";
    private static final String KEY_EMAIL_FORM_TYPE = "emailformtype";
    private static final String KEY_DESTINATION_LINK = "destinationlink";
    private static final String KEY_ACTION_TO_TRIGGER_GET_ACCESS = "actiontotriggergetaccess";
    private static final String KEY_SAVED_CONTENT_ID = "savedcontentid";
    private static final String KEY_SAVED_SOCIETY_CONTENT_ID = "savedsocietycontentid";
    private static final String KEY_DOWNLOADED_ISSUE_DOI = "downloadedissuedoi";
    private static final String KEY_SOCIETY_CONTENT_PANEL = "societycontentpanel";
    private static final String KEY_VIDEO_NAME = "videoname";
    private static final String KEY_ARTICLE_SEARCH_PHRASE = "articlesearchphrase";

    public static final String KEY_MANUFACTURER = "MobileManufacturer";
    public static final String KEY_BRAND = "MobileBrand";
    public static final String KEY_DEVICE_TYPE = "MobileDeviceType";
    public static final String KEY_SCREEN_SIZE = "MobileScreenSize";

    private static final int CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_LAUNCH_APP = 0;
    private static final int CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_MAIN_SCREEN = 1;
    private static final int CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ISSUE_TOC = 2;
    private static final int CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ARTICLE_VIEWER = 3;

    //
    private static boolean mFirstLaunchGetAccessDialogue = true;
    private static final String STATE_FOR_GET_ACCESS_OVERLAY_LAUNCH_APP = "Launch App";
    private static String mStateForGetAccessOverlay_MainScreen = "";
    private static String mStateForGetAccessOverlay_IssueTOC = "";
    private static String mStateForGetAccessOverlay_ArticleViewer = "";
    private static int mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_LAUNCH_APP;

    // target variables
    private static String mVersionNumber = "";
    private static String mJournalName = "";
    private static String mAppPrefix = "";

    private boolean isPhone;
    private String screenSize;

    //
    private String lastCallActionOpenWebViewerForArticleOnWOL_articleDoi = "";

    @Inject
    private Context context;
    @Inject
    private Theme mTheme;
    @Inject
    private Environment mEnvironment;
    @Inject
    private Settings mSettings;
    @Inject
    private IssueService mIssueService;
    @Inject
    private ArticleService mArticleService;

    public void takeOff(final boolean isPhone, final String screenSize) {
        this.isPhone = isPhone;
        this.screenSize = screenSize;

        /*
         * Adobe Tracking - Analytics
         *
         * set the context for the SDK
         * this is necessary for access to sharedPreferences and file i/o
         */
        Config.setContext(context.getApplicationContext());

        /*
         * Adobe Tracking - Config
         *
         * turn on debug logging for the ADBMobile SDK
         */
        Config.setDebugLogging(false);

        mJournalName = mTheme.getJournalName();
        mVersionNumber = mEnvironment.getAppVersion();
        mAppPrefix = mTheme.getAppPrefix();

        trackActionAppLaunched();
    }

    public void onPause() {
        /*
		 * Adobe Tracking - Config
		 *
		 * call pauseCollectingLifecycleData() in case leaving this activity also means leaving the app
		 * must be in the onPause() of every activity in your app
		 */
         Config.pauseCollectingLifecycleData();
    }

    public void onResume() {
        /*
		 * Adobe Tracking - Config
		 *
		 * call collectLifecycleData() to begin collecting lifecycle data
		 * must be in the onResume() of every activity in your app
		 */
        Config.collectLifecycleData();
    }

    private void trackState(final String state, final Map<String, Object> contextData) {
        Log.d(TAG, "STATE - " + state);
        Log.d(TAG, contextData.toString());
        Analytics.trackState(state, contextData);
    }

    private void trackAction(final String action, final Map<String, Object> contextData) {
        Log.d(TAG, "ACTION - " + action);
        Log.d(TAG, contextData.toString());
        Analytics.trackAction(action, contextData);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    public boolean isOnline() {
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private boolean isAuthorized()  {
        return mSettings.isAuthorized();
    }

    private String propertyJournalName() {
        return mTheme.getJournalName();
    }

    private boolean isOpenAccessJournal() {
        return mTheme.isOpenAccessJournal();
    }

    private boolean isFullAccess() {
        List<IssueMO> issues = mIssueService.getIssues();
        for (final IssueMO each : issues) {
            if (each.isSampleIssue()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> createContextData(final String state) {
        final String userFullAccessOrNot;
        if (isOpenAccessJournal()) {
            userFullAccessOrNot = "Open Access";
        } else if (!isAuthorized()) {
            userFullAccessOrNot = "Not Logged In";
        } else if (isFullAccess()) {
            userFullAccessOrNot = "Full Access";
        } else {
            userFullAccessOrNot = "Partial";
        }
        final String subscriptionCodes = mSettings.hasAccessCode() ? mSettings.getAccessCode() : "";
        final String userOnlineOrNot = isOnline() ? "Online" : "Offline";
        final String userLoggedInOrNot = isAuthorized() ? "Logged In" : "Not Logged In";

        HashMap<String, Object> contextData = new HashMap<>();
        contextData.put("rdis", "wileyjournalapps");
        contextData.put("channel", mJournalName);
        contextData.put("journalsocietycode", mAppPrefix);
        contextData.put("appversion", mVersionNumber);
        contextData.put("osvendor", "Android");
        contextData.put("useronlineornot", userOnlineOrNot);
        contextData.put("userloggedinornot", userLoggedInOrNot);
        contextData.put("userfullaccessornot", userFullAccessOrNot);
        contextData.put("subscriptioncodes", subscriptionCodes);
        contextData.put(KEY_MANUFACTURER, Build.MANUFACTURER);
        contextData.put(KEY_BRAND, Build.BRAND);
        contextData.put(KEY_DEVICE_TYPE, isPhone ? "phone" : "tablet");
        contextData.put(KEY_SCREEN_SIZE, screenSize);

        if (null != state) {
            contextData.put("state", state);
        }

        return contextData;
    }

    private Map<String, Object> createContextDataForArticle(final String state, final ArticleMO article) {
        String articleAgeInDays;
        {
            final String firstOnlineDate = article.getFirstOnlineDate();
            if (null == firstOnlineDate || firstOnlineDate.equals("")) {
                articleAgeInDays = "-1. = " + firstOnlineDate;
            } else {
                try {
                    Date now = new Date();
                    Date articleDate = (new SimpleDateFormat("yyyy-MM-dd")).parse(firstOnlineDate);
                    final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
                    int diff = (int)((now.getTime() / MILLIS_IN_DAY) - (articleDate.getTime() / MILLIS_IN_DAY));
                    articleAgeInDays = "" + diff;
                } catch (ParseException e) {
                    articleAgeInDays = "-2. = " + firstOnlineDate;
                }
            }
        }

        String issueDoi, issueDownloadedOrNot;
        {
            if (article.isEarlyView()) {
                issueDoi = "Early View";
                issueDownloadedOrNot = "";
            } else {
                final Collection<SpecialSectionMO> specialSections = article.getSpecialSections();
                if (null != specialSections && 0 != specialSections.size()) {
                    issueDoi = "Special Sections";
                    issueDownloadedOrNot = "";
                } else {
                    final SectionMO section = article.getSection();
                    if (null != section) {
                        final IssueMO issue = section.getIssue();
                        if (null != issue) {
                            issueDoi = issue.getDoi();
                            issueDownloadedOrNot = issue.isLocal() ? "Downloaded" : "Not Downloaded";
                        } else {
                            issueDoi = "Issue Not Found";
                            issueDownloadedOrNot = "";
                        }
                    } else {
                        issueDoi = "Section Not Found";
                        issueDownloadedOrNot = "";
                    }
                }
            }
        }

        String articleAvailability;
        {
            if (isOpenAccessJournal())
                articleAvailability = "open-access";
            else if (article.isRestricted())
                articleAvailability = "licensed";
            else if (article.isOpenAccess())
                articleAvailability = "online-open";
            else
                articleAvailability = "free";
        }

        String articleSavedOrNot = article.isFavorite() ? "Saved" : "Not Saved";

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_ARTICLE_DOI, article.getDOI().getValue());
        contextData.put(KEY_ARTICLE_AVAILABILITY, articleAvailability);
        contextData.put(KEY_ARTICLE_AGE_IN_DAYS, articleAgeInDays);
        contextData.put(KEY_ARTICLE_SAVED_OR_NOT, articleSavedOrNot);
        contextData.put(KEY_ISSUE_DOI, issueDoi);
        contextData.put(KEY_ISSUE_DOWNLOADED_OR_NOT, issueDownloadedOrNot);

        return  contextData;
    }

    private Map<String, Object> createContextDataForAction(final String event) {
        Map <String, Object> contextData = createContextData(null);
        contextData.put(event, event);

        return contextData;
    }

    private Map<String, Object> addArticleProperties(final Map<String, Object> contextData, final ArticleMO article) {
        if (null == article) {
            return contextData;
        }

        String articleAgeInDays;
        {
            final String firstOnlineDate = article.getFirstOnlineDate();
            if (null == firstOnlineDate || firstOnlineDate.equals("")) {
                articleAgeInDays = "-1. = " + firstOnlineDate;
            } else {
                try {
                    Date now = new Date();
                    Date articleDate = (new SimpleDateFormat("yyyy-MM-dd")).parse(firstOnlineDate);
                    final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
                    int diff = (int)((now.getTime() / MILLIS_IN_DAY) - (articleDate.getTime() / MILLIS_IN_DAY));
                    articleAgeInDays = "" + diff;
                } catch (ParseException e) {
                    articleAgeInDays = "-2. = " + firstOnlineDate;
                }
            }
        }

        String issueDoi, issueDownloadedOrNot;
        {
            if (article.isEarlyView()) {
                issueDoi = "Early View";
                issueDownloadedOrNot = "";
            } else {
                final Collection<SpecialSectionMO> specialSections = article.getSpecialSections();
                if (null != specialSections && 0 != specialSections.size()) {
                    issueDoi = "Special Sections";
                    issueDownloadedOrNot = "";
                } else {
                    final SectionMO section = article.getSection();
                    if (null != section) {
                        final IssueMO issue = section.getIssue();
                        if (null != issue) {
                            issueDoi = issue.getDoi();
                            issueDownloadedOrNot = issue.isLocal() ? "Downloaded" : "Not Downloaded";
                        } else {
                            issueDoi = "Issue Not Found";
                            issueDownloadedOrNot = "";
                        }
                    } else {
                        issueDoi = "Section Not Found";
                        issueDownloadedOrNot = "";
                    }
                }
            }
        }

        String articleAvailability;
        {
            if (isOpenAccessJournal())
                articleAvailability = "open-access";
            else if (article.isRestricted())
                articleAvailability = "licensed";
            else if (article.isOpenAccess())
                articleAvailability = "online-open";
            else
                articleAvailability = "free";
        }

        String articleSavedOrNot = article.isFavorite() ? "Saved" : "Not Saved";

        contextData.put(KEY_ARTICLE_DOI, article.getDOI().getValue());
        contextData.put(KEY_ARTICLE_AVAILABILITY, articleAvailability);
        contextData.put(KEY_ARTICLE_AGE_IN_DAYS, articleAgeInDays);
        contextData.put(KEY_ARTICLE_SAVED_OR_NOT, articleSavedOrNot);
        contextData.put(KEY_ISSUE_DOI, issueDoi);
        contextData.put(KEY_ISSUE_DOWNLOADED_OR_NOT, issueDownloadedOrNot);

        return  contextData;
    }

    ////////////////////////////////////////////////////////////////////////////////////

    private void trackStateScreen(final String screenName,
                                  final String state,
                                  final String pageType,
                                  final String event) {

        mStateForGetAccessOverlay_MainScreen = String.format("%s - %s", propertyJournalName(), pageType);
        mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_MAIN_SCREEN;

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE,pageType);
        contextData.put(KEY_PAGE_SUBTYPE,pageType);

        if (null != event)
            contextData.put(event, event);

        trackState(screenName, contextData);
    }

    public void trackEarlyViewScreen() {
        final String pageType = "Early View";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackIssuesListScreen() {
        final String pageType = "List of Issues";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackSpecialSectionsScreen() {
        final String pageType = "Special Sections";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackSavedArticlesScreen() {
        final String pageType = "Saved Articles";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackGlobalSearchScreen() {
        final String pageType = "Global Search";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackSettingsScreen() {
        final String pageType = "Settings";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackJournalInfoScreen() {
        final String pageType = "App Info";
        final String state = String.format("%s - %s", propertyJournalName(), pageType);

        trackStateScreen(pageType, state, pageType, null);
    }

    public void trackIssueTocScreen(final IssueMO issue) {
        if (null == issue) {
            return;
        }

        final String state = String.format("%s - %s/%s", propertyJournalName(), issue.getVolumeNumber(), issue.getIssueNumber());
        mStateForGetAccessOverlay_IssueTOC = String.format("%s", state);
        mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ISSUE_TOC;
        final String issueDownloadedOrNot = issue.isLocal() ? "Downloaded" : "Not Downloaded";

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE,"Table of Contents");
        contextData.put(KEY_PAGE_SUBTYPE,"Table of Contents");
        contextData.put(KEY_ISSUE_DOI, issue.getDOI().getValue());
        contextData.put(KEY_ISSUE_DOWNLOADED_OR_NOT, issueDownloadedOrNot);

        trackState("Table of Contents", contextData);

    }

    public void trackArticleView(final ArticleMO article) {
        if (null == article) {
            return;
        }

        final String nameTrackState, state, eventFirst, eventSecond, pageType, pageSubtype;
        {
            if (article.isRestricted()) {
                if (isOnline()) {
                    nameTrackState = "Article Abstract Online No Access";
                    state = String.format("%s - Abstract Access Denied", article.getTitle());
                    eventFirst = "event1";
                    eventSecond = null;
                    pageType = "Abstract";
                    pageSubtype = "Abstract - Access Denied";
                } else {
                    nameTrackState = "Article Abstract User Offline";
                    state = String.format("%s - Abstract User Offline", article.getTitle());
                    eventFirst = "event1";
                    eventSecond = "event18";
                    pageType = "Abstract";
                    pageSubtype = "Abstract - User Offline";
                }
            } else {
                final String fullHtml = mArticleService.getFullHtmlBody(article);
                if (null != fullHtml && !fullHtml.equals("")) {
                    nameTrackState = "Article Fulltext HTML";
                    state = String.format("%s - Fulltext HTML", article.getTitle());
                    eventFirst = "event2";
                    eventSecond = null;
                    pageType = "Fulltext";
                    pageSubtype = "Fulltext HTML";
                } else {
                    nameTrackState = "Article Abstract No Fulltext";
                    state = String.format("%s - Abstract No Fulltext", article.getTitle());
                    eventFirst = "event1";
                    eventSecond = null;
                    pageType = "Abstract";
                    pageSubtype = "Abstract - No Fulltext";
                }
            }

        }
        mStateForGetAccessOverlay_ArticleViewer = String.format("%s", state);
        mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ARTICLE_VIEWER;

        Map<String, Object> contextData = createContextDataForArticle(state, article);
        contextData.put(KEY_PAGE_TYPE, pageType);
        contextData.put(KEY_PAGE_SUBTYPE, pageSubtype);
        contextData.put(eventFirst, eventFirst);
        if (null != eventSecond)
            contextData.put(eventSecond, eventSecond);

        trackState(nameTrackState, contextData);
    }

    public void trackArticleFulltextPDF(final ArticleMO article) {
        if (null == article) {
            return;
        }

        final String state = String.format("%s - Fulltext PDF", article.getTitle());

        Map<String, Object> contextData = createContextDataForArticle(state, article);
        contextData.put(KEY_PAGE_TYPE, "Fulltext");
        contextData.put(KEY_PAGE_SUBTYPE, "Fulltext PDF");
        contextData.put("event3", "event3");

        trackState("Article Fulltext PDF", contextData);
    }

    public void trackArticleViewSupportingInfoFile(final ArticleMO article, final String fileName) {
        if (null == article || null == fileName) {
            return;
        }

        final String state = String.format("%s - %s", article.getTitle(), fileName);

        Map<String, Object> contextData = createContextDataForArticle(state, article);
        contextData.put(KEY_PAGE_TYPE, "Supporting Info");
        contextData.put(KEY_PAGE_SUBTYPE, "Supporting Info");
        contextData.put("event8", "event8");

        trackState("Supporting Info File", contextData);
    }

    public void trackFigureViewer(final ArticleMO article, final String figureName) {
        if (null == article || null == figureName) {
            return;
        }

        final String state = String.format("%s - %s", article.getTitle(), figureName);

        Map<String, Object> contextData = createContextDataForArticle(state, article);
        contextData.put(KEY_PAGE_TYPE, "Figure View");
        contextData.put(KEY_PAGE_SUBTYPE, "Figure View");
        contextData.put(KEY_FIGURE_ID, figureName);
        contextData.put("event28", "event28");

        trackState("Figure Viewer", contextData);
    }

    public void setCurrentPageForGetAccess_MainScreen() {
        mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_MAIN_SCREEN;
    }

    public void setCurrentPageForGetAccess_IssueTOC() {
        mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ISSUE_TOC;
    }

    public void setCurrentPageForGetAccess_ArticleViewer() {
        mCurrentPageForGetAccessOverlay = CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ARTICLE_VIEWER;
    }

    private String getCurrentState() {
        final String currentState;
        {
            if (mCurrentPageForGetAccessOverlay == CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_MAIN_SCREEN) {
                currentState = mStateForGetAccessOverlay_MainScreen;
            } else if (mCurrentPageForGetAccessOverlay == CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ISSUE_TOC) {
                currentState = mStateForGetAccessOverlay_IssueTOC;
            } else if (mCurrentPageForGetAccessOverlay == CURRENT_PAGE_FOR_GET_ACCESS_OVERLAY_ARTICLE_VIEWER) {
                currentState = mStateForGetAccessOverlay_ArticleViewer;
            } else {
                currentState = STATE_FOR_GET_ACCESS_OVERLAY_LAUNCH_APP;
            }
        }
        return currentState;
    }
    private void trackGetAccessDialogue(final String screenName, final String event) {
        final String currentState = getCurrentState();
        final String state = String.format("%s - %s", currentState, screenName);

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_OVERLAY_TYPE, screenName);
        if (null != event) {
            contextData.put(event, event);
        }

        trackState(screenName, contextData);
    }

    public void trackGetAccessDialogueDoYouHaveAccess() {
        trackGetAccessDialogue("Get Access Dialogue Do You Have Access", null);
    }

    public void trackGetAccessDialogueAccessType() {
        trackGetAccessDialogue("Get Access Dialogue Access Type", null);
    }

    public void trackGetAccessDialogueInstitutionInfo() {
        trackGetAccessDialogue("Get Access Dialogue Institution Info", null);
    }

    public void trackGetAccessDialogueSocietyInfo() {
        trackGetAccessDialogue("Get Access Dialogue Society Info", null);
    }

    public void trackGetAccessDialogueSponsoredSubscriptionInfo() {
        trackGetAccessDialogue("Get Access Dialogue Sponsored Subscription Info", null);
    }

    public void trackLoggedInNotAccessToArticleOverlay() {
        trackGetAccessDialogue("Logged in Not Access to Article Overlay", null);
    }

    public void trackLoggedInNotAccessToIssueOverlay() {
        trackGetAccessDialogue("Logged in Not Access to Issue Overlay", null);
    }

    public void trackRegistrationForm() {
        trackGetAccessDialogue("Registration Form", null);
    }

    public void trackRegistrationConfirmedOverlay() {
        trackGetAccessDialogue("Registration Confirmed", "event43");
    }

    public void trackRegistrationErrorDuplicateEmailOverlay() {
        trackGetAccessDialogue("Registration Error Duplicate Email", "event44");
    }

    public void trackSubscriptionCodeAddedOverlay() {
        trackGetAccessDialogue("Subscription Code Added", "event45");
    }

    public void trackSubscriptionCodeInvalidOverlay() {
        trackGetAccessDialogue("Code Invalid", "event46");
    }

    public void trackSubscriptionExpirationWarning(final int timePeriodUntilExpiration) {
        final String overlayType = "Subscription Expiration Warning";
        final String state = getCurrentState() + " - " + overlayType + "(" + timePeriodUntilExpiration + ")";

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_OVERLAY_TYPE, overlayType);

        trackState(overlayType + " Overlay", contextData);
    }

    public void trackSocietyHome() {
        String  pagetype = "Society Home";
        final String state = String.format("%s - %s", mAppPrefix, pagetype);

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE, pagetype);
        contextData.put(KEY_PAGE_SUBTYPE, pagetype);

        trackState(pagetype, contextData);
    }

    public void trackSocietyFavorites() {
        String  pagetype = "Society Favorites";
        final String state = String.format("%s - %s", mAppPrefix, pagetype);

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE, pagetype);
        contextData.put(KEY_PAGE_SUBTYPE, pagetype);

        trackState(pagetype, contextData);
    }

    public void trackSocietyListings(final FeedMO feed) {
        if (null == feed) {
            return;
        }

        String  pagetype = "Society Listings";
        String  pageSubtype = String.format("%s - %s", pagetype , feed.getTitle());
        String  state = String.format("%s - %s", mAppPrefix, feed.getTitle());

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE, pagetype);
        contextData.put(KEY_PAGE_SUBTYPE, pageSubtype);

        trackState(pagetype, contextData);
    }

    public void trackSocietyContentSearch() {
        String  pagetype = "Society Content Search";
        final String state = String.format("%s - %s", mAppPrefix, pagetype);

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE, pagetype);
        contextData.put(KEY_PAGE_SUBTYPE, pagetype);

        trackState(pagetype, contextData);
    }

    public void trackSocietyContentPage(final FeedItemMO feedItem) {
        if (null == feedItem) {
            return;
        }

        String societyContentPanel = String.format("%s", feedItem.getFeed().getTitle());
        String pageType = "Society Content Page";
        String state = String.format("%s Content Page - %s", mAppPrefix, feedItem.getTitle());

        Map<String, Object> contextData = createContextData(state);
        contextData.put(KEY_PAGE_TYPE, pageType);
        contextData.put(KEY_PAGE_SUBTYPE, pageType);
        contextData.put(KEY_SOCIETY_CONTENT_PANEL, societyContentPanel);

        trackState(pageType, contextData);
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    public void trackActionAppLaunched() {
        Map<String, Object> contextData = createContextDataForAction("event80");

        trackAction("App Launch (Custom Event)", contextData);
    }

    public void trackActionAuthorInformation(final ArticleMO article) {
        if (null == article) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event29");
        addArticleProperties(contextData, article);

        trackAction("Author Information", contextData);
    }

    public void trackActionIssueDownload(final String issueDoi) {
        if (null == issueDoi) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event5");
        contextData.put(KEY_DOWNLOADED_ISSUE_DOI, issueDoi);

        trackAction("Issue Download", contextData);
    }

    public void trackActionCancelIssueDownload(final String issueDoi) {
        if (null == issueDoi) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event6");
        contextData.put(KEY_DOWNLOADED_ISSUE_DOI, issueDoi);

        trackAction("Cancel Issue Download", contextData);
    }

    public void trackActionDeleteIssue(final String issueDoi) {
        if (null == issueDoi) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event7");
        contextData.put(KEY_DOWNLOADED_ISSUE_DOI, issueDoi);

        trackAction("Delete Issue", contextData);
    }

    public void trackActionCancelPDFDownload(final ArticleMO article) {
        if (null == article) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event4");
        addArticleProperties(contextData, article);

        trackAction("Cancel PDF Download", contextData);
    }

    public void trackActionCancelSupportingInfoDownload(final ArticleMO article) {
        if (null == article) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event9");
        addArticleProperties(contextData, article);

        trackAction("Cancel Supporting Info Download", contextData);
    }

    public void trackActionSaveArticle(final String articleDOI) {
        if (null == articleDOI) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event10");
        contextData.put(KEY_SAVED_CONTENT_ID, articleDOI);

        trackAction("Save Article", contextData);
    }

    public void trackActionDeleteArticle(final String articleDOI) {
        if (null == articleDOI) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event11");
        contextData.put(KEY_SAVED_CONTENT_ID, articleDOI);

        trackAction("Delete Article", contextData);
    }

    public void trackActionSelectArticleSidebar(final String buttonTitle, final ArticleMO article) {
        if (null == article || null == buttonTitle) {
            return;
        }
        Map<String, Object> contextData = createContextDataForAction("event12");
        addArticleProperties(contextData, article);
        contextData.put(KEY_SIDEBAR_TYPE, buttonTitle);

        trackAction("Select Article Sidebar", contextData);
    }

    public void trackActionGoToSavedArticle(final String articleDOI) {
        if (null == articleDOI) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event13");
        contextData.put(KEY_SAVED_CONTENT_ID, articleDOI);

        trackAction("Go To Saved Article", contextData);
    }

    public void trackActionJumpToSearchTerm(final String searchTerm, final ArticleMO article) {
        if (null == article || null == searchTerm) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event25");
        addArticleProperties(contextData, article);
        contextData.put(KEY_ARTICLE_SEARCH_PHRASE, searchTerm);

        trackAction("Jump To Search Term", contextData);
    }

    public void trackActionJumpToReference(final ArticleMO article) {
        if (null == article) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event26");
        addArticleProperties(contextData, article);

        trackAction("Jump To Reference", contextData);
    }

    public void trackActionJumpToFigure(final String figureName, final ArticleMO article) {
        if (null == article || null == figureName) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event27");
        addArticleProperties(contextData, article);
        contextData.put(KEY_FIGURE_ID, figureName);

        trackAction("Jump To Figure", contextData);
    }

    public void trackActionOpenEmailForm(final String emailFormType) {
        if (null == emailFormType) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event30");
        contextData.put(KEY_EMAIL_FORM_TYPE, emailFormType);

        trackAction("Open Email Form", contextData);
    }

    public void trackActionCopyCitation(final ArticleMO article) {
        if (null == article) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event32");
        addArticleProperties(contextData, article);

        trackAction("Copy Citation", contextData);
    }

    public void trackActionOpenWebViewerForReference(final String url) {
        if (null == url) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event34");
        contextData.put(KEY_DESTINATION_LINK, url);

        trackAction("Open Web Viewer for Reference", contextData);
    }

    public void trackActionOpenWebViewerForArticleOnWOL(final String url, final ArticleMO article) {
        if (null == url || null == article || lastCallActionOpenWebViewerForArticleOnWOL_articleDoi.equals(article.getDOI().getValue())) {
            return;
        }
        lastCallActionOpenWebViewerForArticleOnWOL_articleDoi = article.getDOI().getValue();

        Map<String, Object> contextData = createContextDataForAction("event35");
        contextData.put(KEY_DESTINATION_LINK, url);

        trackAction("Open Web Viewer for Article on WOL", contextData);
    }

    public void trackActionOpenWebViewerForOtherPage(final String url) {
        if (null == url) {
            return;
        }

        String event, actionName;
        {
            if (url.contains("onlinelibrary.wiley.com")) {
                event = "event36";
                actionName = "Open Web Viewer for other WOL page";
            } else {
                event = "event37";
                actionName = "Open Web Viewer for other Website";
            }
        }
        Map<String, Object> contextData = createContextDataForAction(event);
        contextData.put(KEY_DESTINATION_LINK, url);

        trackAction(actionName, contextData);
    }

    public void trackActionLaunchGetAccessDialogue(final String descriptionPage) {
        if (null == descriptionPage) {
            return;
        }

        if (mFirstLaunchGetAccessDialogue) {
            mFirstLaunchGetAccessDialogue = false;
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event38");
        contextData.put(KEY_ACTION_TO_TRIGGER_GET_ACCESS, descriptionPage);

        trackAction("Launch Get Access Dialogue", contextData);
    }

    public void trackActionBrowseFreeContentOnly() {
        Map<String, Object> contextData = createContextDataForAction("event39");

        trackAction("Browse Free Content Only", contextData);
    }

    public void trackActionAbandonGetAccessDialogue() {
        Map<String, Object> contextData = createContextDataForAction("event40");

        trackAction("Abandon Get Access Dialogue", contextData);
    }

    public void trackActionLoginToWOL() {
        Map<String, Object> contextData = createContextDataForAction("event41");

        trackAction("Login to WOL", contextData);
    }

    public void trackActionAbandonLoginToWOL() {
        Map<String, Object> contextData = createContextDataForAction("event42");

        trackAction("Abandon Login to WOL", contextData);
    }

    public void trackActionRefreshAccount() {
        Map<String, Object> contextData = createContextDataForAction("event48");

        trackAction("Refresh Account", contextData);
    }

    public void trackActionLogout() {
        Map<String, Object> contextData = createContextDataForAction("event49");

        trackAction("Logout", contextData);
    }

    public void trackActionRestoreAllPurchases() {
        Map<String, Object> contextData = createContextDataForAction("event52");

        trackAction("Restore All Purchases", contextData);
    }

    public void trackActionIWantToBuyASubscription() {
        Map<String, Object> contextData = createContextDataForAction("event51");

        trackAction("I Want To Buy A Subscription", contextData);
    }

    public void trackActionOpenArticleFromGlobalSearchResult(final String searchPhrase, final String sortMethod) {
        if (null == searchPhrase || null == sortMethod) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event21");
        contextData.put("globalsearchphrase", searchPhrase);
        contextData.put("globalsearchresultsorting", sortMethod);

        trackAction("Open Article from Global Search Result", contextData);
    }

    public void trackActionSortGlobalSearchResults(final String searchPhrase, final String sortMethod) {
        if (null == searchPhrase || null == sortMethod) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event22");
        contextData.put("globalsearchphrase", searchPhrase);
        contextData.put("globalsearchresultsorting", sortMethod);

        trackAction("Sort Global Search Results", contextData);
    }

    public void trackActionSubscribeToKeyword(final String keyword, final ArticleMO article) {
        if (null == keyword || null == article) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event16");
        addArticleProperties(contextData, article);
        contextData.put("authorkeywordalertset", keyword);

        trackAction("Subscribe to Keyword", contextData);
    }

    public void trackActionUnsubscribeFromKeyword(final String keyword, final ArticleMO article) {
        if (null == keyword) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event17");
        addArticleProperties(contextData, article);
        contextData.put("authorkeywordalertset", keyword);

        trackAction("Unsubscribe from Keyword", contextData);
    }

    public void trackActionOpenSocietyContentItem(final FeedMO feed) {
        if (null == feed) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event19");
        contextData.put("societycontentpanel", null == feed.getTitle() ? "" : feed.getTitle());

        trackAction("Open Society Content Item", contextData);
    }


    public void trackActionPlayVideo(final String video) {
        if (null == video) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event60");
        contextData.put(KEY_VIDEO_NAME, video);

        trackAction("Play Video", contextData);
    }

    public void trackActionOpenEmailClient() {
        Map<String, Object> contextData = createContextDataForAction("event31");

        trackAction("Open Email Client", contextData);
    }

    public void trackActionRefreshInstitutionalAccess() {
        Map<String, Object> contextData = createContextDataForAction("event65");

        trackAction("Refresh Institutional Access", contextData);
    }

    public void trackActionCancelSearch() {
        Map<String, Object> contextData = createContextDataForAction("event70");

        trackAction("Cancel Search", contextData);
    }

    public void trackActionOpenItemFromSocietyContentSearchResults(final String searchPhrase, final String sortMethod) {
        if (null == searchPhrase || null == sortMethod) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event82");
        contextData.put("societysearchphrase", searchPhrase);
        contextData.put("societysearchresultsorting", sortMethod);

        trackAction("Open Item from Society Content Search Results", contextData);
    }

    public void trackActionSortSocietyContentSearchResults(final String searchPhrase, final String sortMethod) {
        if (null == searchPhrase || null == sortMethod) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event83");
        contextData.put("societysearchphrase", searchPhrase);
        contextData.put("societysearchresultsorting", sortMethod);

        trackAction("Sort Society Content Search Results", contextData);
    }

    public void trackActionSaveFeedItem(final FeedItemMO feedItem) {
        if (null == feedItem ) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event85");
        contextData.put(KEY_SAVED_SOCIETY_CONTENT_ID, feedItem.getTitle());

        trackAction("Select Society Content Item as Favorite", contextData);
    }

    public void trackActionRemoveFeedItem(final FeedItemMO feedItem) {
        if (null == feedItem ) {
            return;
        }

        Map<String, Object> contextData = createContextDataForAction("event86");
        contextData.put(KEY_SAVED_SOCIETY_CONTENT_ID, feedItem.getTitle());

        trackAction("Unselect Society Content Item as Favorite", contextData);
    }
}
