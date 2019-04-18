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
package com.wiley.wol.client.android.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.xml.AdvertisementConfigUnit;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.utils.PreferencesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsImpl implements Settings {

    private static final String SETTING_ACCESS_TOKEN = "access_token";
    private static final String SETTING_ACCESS_TOKEN_SECRET = "access_token_secret";

    private static final String ADV_UNIT_ID = "adv_unit_id";
    private static final String SPONSORED_AD_UNIT_ID = "sponsored_adv_unit_id";
    private static final String FIRST_AD_ARTICLE_VIEW = "first_ad_article_view";
    private static final String OTHER_AD_ARTICLE_VIEW = "other_ad_article_view";

    private static final String SHOW_GET_ACCESS_ON_START = "show_get_access_on_start";
    private static final String HAS_LAST_LOGIN_INFO = "has_last_login_info";
    private static final String LOGGED_IN_VIA_TPS = "via_tps";
    private static final String LOGIN_SITE_NAME = "login_site_name";
    private static final String LOGIN_SCREEN = "login_screen";

    private static final String SETTING_ACCESS_CODE_EXPIRATION_DATE = "access_code_expiration_date";
    private static final String SHOW_SPONSORED_PROMO = "show_sponsored_promo_image";

    private static final String COUNT_DOWNLOADED_ARTICLE_PDF = "count_downloaded_article_pdf";

    public static final String DEBUG_IP = "debug_ip";

    private static final String SOCIETY_SCREEN_SECTION_TITLE_COLOR = "society_screen_section_title_color";
    private static final String SOCIETY_SPONSORED_SUBSCRIPTION_AD_UNIT_ID = "society_sponsored_subscription_ad_unit_id";
    private static final String SOCIETY_ALTERNATE_TAB_LABEL_FOR_SOCIETY_FEED_PAGE = "society_alternate_tab_label_for_society_feed_page";
    private static final String SOCIETY_IS_SOCIETY_CONTENT_ENABLED = "society_is_society_content_enabled";
    private static final String SOCIETY_FOOTER_LOGO_IMAGE_URL = "society_footer_logo_image_url";

    private static final String DEVICE_TOKEN = "device_token";
    private static final String KEYWORD_SET = "keyword_set";
    private static final String KEYWORDS = "keywords";

    private static final String SUBSCRIPTION_RECEIPT = "subscription_receipt";
    private static final String ANNOUNCEMENT_PANEL_OPEN = "announcement_panel_open";
    public static final String USER_LOGIN = "userLogin";

    public static final String IS_FIRST_LAUNCH = "is_first_launch";

    // feature: google drive
    private static final String LOCAL_SAVED_ARTICLES = "local_savedArticles";
    private static final String DRIVE_SAVED_ARTICLES = "drive_savedArticles";
    private static final String LOCAL_READ_ARTICLES = "local_readArticles";
    private static final String DRIVE_READ_ARTICLES = "drive_readArticles";
    private static final String LOCAL_KEYWORDS = "local_keywords";
    private static final String DRIVE_KEYWORDS = "drive_keywords";
    private static final String LOCAL_SAVED_FEED_ITEMS = "local_savedFeedItems";
    private static final String DRIVE_SAVED_FEED_ITEMS = "drive_savedFeedItems";

    // feature: keywords
    private static final String KEYWORDS_REGISTERED_DEVICE_ON_MCS = "keywords_registered_device_on_mcs";
    public static final String STORED_APP_VERSION = "storedAppVersion";
    public static final String LAST_UPGRADE_DATE = "lastUpgradeDate";
    public static final String SOCIETY_PAGE_BY_DEFAULT = "societyPageByDefault";


    private final SharedPreferences preferences;
    @Inject
    private Environment environment;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Theme theme;
    @Inject
    private LastModifiedManager lastModifiedManager;

    @Inject
    public SettingsImpl(final SharedPreferences preferences, final Context context) {
        this.preferences = preferences;
    }

    protected void onSettingsChanged(final String settingName) {
        final HashMap<String, Object> params = new HashMap<>();
        params.put(SETTING_NAME_KEY, settingName);
        notificationCenter.sendNotification(SETTINGS_CHANGED.getEventName(), params);
    }

    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    @Override
    public int getArticleFontSize() {
        return preferences.getInt(SETTING_ARTICLE_FONT_SIZE, theme.getDefaultArticleFontSize());
    }

    @Override
    public boolean hasArticleFontSize() {
        return preferences.contains(SETTING_ARTICLE_FONT_SIZE);
    }

    @Override
    public void changeArticleFontSize(int newSize) {
        if (newSize < ARTICLE_FONT_SIZE_MIN) {
            newSize = ARTICLE_FONT_SIZE_MIN;
        }
        if (newSize > ARTICLE_FONT_SIZE_MAX) {
            newSize = ARTICLE_FONT_SIZE_MAX;
        }
        if (newSize != getArticleFontSize()) {
            preferences.edit().putInt(SETTING_ARTICLE_FONT_SIZE, newSize).apply();
            onSettingsChanged(SETTING_ARTICLE_FONT_SIZE);
        }
    }

    @Override
    public void setStoredAppVersion(final String storedAppVersion) {
        preferences.edit().putString(STORED_APP_VERSION, storedAppVersion).apply();
    }

    @Override
    public String getStoredAppVersion() {
        return preferences.getString(STORED_APP_VERSION, null);
    }

    @Override
    public boolean getArticleShowAbstract() {
        return preferences.getBoolean(SETTING_ARTICLE_SHOW_ABSTRACT, false);
    }

    @Override
    public boolean isArticleShowAbstractInitialized() {
        return preferences.contains(SETTING_ARTICLE_SHOW_ABSTRACT);
    }

    @Override
    public void changeArticleShowAbstract(final boolean newShow) {
        preferences.edit().putBoolean(SETTING_ARTICLE_SHOW_ABSTRACT, newShow).apply();
        onSettingsChanged(SETTING_ARTICLE_SHOW_ABSTRACT);
    }

    @Override
    public String getCurrentServer() {
        return preferences.getString(SETTING_CURRENT_SERVER, environment.getDebugServers()[0]);
    }

    @Override
    public boolean hasDebugServer() {
        return preferences.contains(SETTING_CURRENT_SERVER);
    }

    @Override
    public void changeCurrentServer(final String newServer) {
        if (!TextUtils.equals(newServer, getCurrentServer())) {
            preferences.edit().putString(SETTING_CURRENT_SERVER, newServer).apply();
            lastModifiedManager.clearLastModified();
            onSettingsChanged(SETTING_CURRENT_SERVER);
        }
    }

    @Override
    public String getDebugOAuthLink() {
        return preferences.getString(SETTING_DEBUG_OAUTH_LINK, environment.getDebugOAuthLinks()[0]);
    }

    @Override
    public boolean hasDebugOAuthLink() {
        return preferences.contains(SETTING_DEBUG_OAUTH_LINK);
    }

    @Override
    public void changeDebugOAuthLink(final String newLink) {
        if (!TextUtils.equals(newLink, getDebugOAuthLink())) {
            preferences.edit().putString(SETTING_DEBUG_OAUTH_LINK, newLink).apply();
            onSettingsChanged(SETTING_DEBUG_OAUTH_LINK);
        }
    }

    @Override
    public String getAccessToken() {
        return getAuthToken().getAuthTokenKey();
    }

    private AuthToken getAuthToken() {
        return new AuthToken(
                preferences.getString(SETTING_ACCESS_TOKEN, null),
                preferences.getString(SETTING_ACCESS_TOKEN_SECRET, null)
        );
    }

    @Override
    public String getAccessTokenSecret() {
        return getAuthToken().getAuthTokenSecret();
    }

    @Override
    public void refreshAccount() {
        onSettingsChanged(SETTING_AUTH_TOKEN);
    }

    @Override
    public void resetAuthToken() {
        preferences.edit().putString(SETTING_ACCESS_TOKEN, null).apply();
        preferences.edit().putString(SETTING_ACCESS_TOKEN_SECRET, null).apply();
        lastModifiedManager.clearLastModified();
        setUserLogin(null);
        onSettingsChanged(SETTING_AUTH_TOKEN);
    }

    @Override
    public boolean isAuthorized() {
        return (getAccessToken() != null && getAccessTokenSecret() != null);
    }

    @Override
    public void setAuthToken(final AuthToken authToken) {
        preferences.edit().putString(SETTING_ACCESS_TOKEN, authToken.getAuthTokenKey()).apply();
        preferences.edit().putString(SETTING_ACCESS_TOKEN_SECRET, authToken.getAuthTokenSecret()).apply();
        lastModifiedManager.clearLastModified();
        onSettingsChanged(SETTING_AUTH_TOKEN);
    }

    @Override
    public boolean getSocietyExists() {
        return preferences.getBoolean(SettingsImpl.SOCIETY_EXISTS, false);
    }

    @Override
    public void setSocietyExists(final boolean exists) {
        preferences.edit().putBoolean(SettingsImpl.SOCIETY_EXISTS, exists).apply();
    }

    @Override
    public String getSocietyLoginInstructions() {
        return preferences.getString(SettingsImpl.SOCIETY_LOGIN_INSTRUCTIONS, null);
    }

    @Override
    public void setSocietyLoginInstructions(final String societyLoginInstructions) {
        preferences.edit().putString(SettingsImpl.SOCIETY_LOGIN_INSTRUCTIONS, null == societyLoginInstructions ? "" : societyLoginInstructions).apply();
    }

    @Override
    public String getSocietyUrl() {
        return preferences.getString(SettingsImpl.SOCIETY_URL, null);
    }

    @Override
    public void setSocietyUrl(final String url) {
        preferences.edit().putString(SettingsImpl.SOCIETY_URL, null == url ? "" : url).apply();
    }

    @Override
    public String getSocietyInformation() {
        return preferences.getString(SettingsImpl.SOCIETY_INFORMATION, null);
    }

    @Override
    public void setSocietyInformation(final String information) {
        preferences.edit().putString(SettingsImpl.SOCIETY_INFORMATION, null == information ? "" : information).apply();
    }

    @Override
    public boolean getTPSExists() {
        return preferences.getBoolean(SettingsImpl.TPS_EXISTS, false);
    }

    @Override
    public void setTPSExists(final boolean exists) {
        preferences.edit().putBoolean(SettingsImpl.TPS_EXISTS, exists).apply();
    }

    @Override
    public String getTPSUsername() {
        return preferences.getString(SettingsImpl.TPS_USERNAME, "");
    }

    @Override
    public void setTPSUsername(final String username) {
        preferences.edit().putString(SettingsImpl.TPS_USERNAME, username).apply();
    }

    @Override
    public String getTPSPassword() {
        return preferences.getString(SettingsImpl.TPS_PASSWORD, "");
    }

    @Override
    public void setTPSPassword(final String password) {
        preferences.edit().putString(SettingsImpl.TPS_PASSWORD, password).apply();
    }

    @Override
    public int getTPSTimeout() {
        return preferences.getInt(SettingsImpl.TPS_TIMEOUT, 1);
    }

    @Override
    public void setTPSTimeout(final int timeout) {
        preferences.edit().putInt(SettingsImpl.TPS_TIMEOUT, timeout).apply();
    }

    @Override
    public AdvertisementConfigUnit getAdvertisementConfig() {
        final AdvertisementConfigUnit advertisementConfigUnit = new AdvertisementConfigUnit();
        advertisementConfigUnit.setAdUnitId(preferences.getString(SettingsImpl.ADV_UNIT_ID, null));
        advertisementConfigUnit.setFirstAdArticleView(preferences.getInt(FIRST_AD_ARTICLE_VIEW, -1));
        advertisementConfigUnit.setOtherAdArticleView(preferences.getInt(OTHER_AD_ARTICLE_VIEW, -1));
        advertisementConfigUnit.setSponsoredAdUnitId(preferences.getString(SettingsImpl.SPONSORED_AD_UNIT_ID, null));

        return advertisementConfigUnit;
    }

    @Override
    public void setAdvertisementConfig(final AdvertisementConfigUnit configUnit) {
        preferences.edit().putString(SettingsImpl.ADV_UNIT_ID, configUnit.getAdUnitId()).apply();
        preferences.edit().putInt(SettingsImpl.FIRST_AD_ARTICLE_VIEW, configUnit.getFirstAdArticleView()).apply();
        preferences.edit().putInt(SettingsImpl.OTHER_AD_ARTICLE_VIEW, configUnit.getOtherAdArticleView()).apply();
        preferences.edit().putString(SettingsImpl.SPONSORED_AD_UNIT_ID, configUnit.getSponsoredAdUnitId()).apply();
    }

    @Override
    public void setNeedShowGetAccessScreenOnStart(boolean show) {
        preferences.edit().putBoolean(SHOW_GET_ACCESS_ON_START, show).apply();
    }

    @Override
    public boolean getNeedShowGetAccessScreenOnStart() {
        return preferences.getBoolean(SHOW_GET_ACCESS_ON_START, true);

    }

    @Override
    public void setHasLastLoginInfo(boolean hasInfo) {
        preferences.edit().putBoolean(HAS_LAST_LOGIN_INFO, hasInfo).apply();
    }

    @Override
    public boolean hasLastLoginInfo() {
        return preferences.getBoolean(HAS_LAST_LOGIN_INFO, false);
    }

    @Override
    public void setLoggedInViaTps(boolean viaTps) {
        preferences.edit().putBoolean(LOGGED_IN_VIA_TPS, viaTps).apply();
    }

    @Override
    public boolean getLoggedInViaTps() {
        return preferences.getBoolean(LOGGED_IN_VIA_TPS, false);
    }

    @Override
    public void setTpsLoginSite(String siteName) {
        preferences.edit().putString(LOGIN_SITE_NAME, siteName).apply();
    }

    @Override
    public String getTpsLoginSite() {
        return preferences.getString(LOGIN_SITE_NAME, "");
    }

    @Override
    public void setLoginScreen(int screen) {
        preferences.edit().putInt(LOGIN_SCREEN, screen).apply();
    }

    @Override
    public int getLoginScreen() {
        return preferences.getInt(LOGIN_SCREEN, 0);
    }

    @Override
    public boolean hasAccessCode() {
        return getAccessCode() != null;
    }

    @Override
    public void setAccessCode(String code) {
        String userLogin = getUserLogin();
        setAccessCode(code, userLogin);
    }

    @Override
    public void setAccessCode(String code, String userLogin) {
        if (userLogin != null && !userLogin.isEmpty()) {
            preferences.edit().putString(SETTING_ACCESS_CODE + "_" + userLogin, code).apply();
            onSettingsChanged(SETTING_ACCESS_CODE);
        }
    }

    @Override
    public String getAccessCode() {
        return preferences.getString(SETTING_ACCESS_CODE + "_" + getUserLogin(), null);
    }

    @Override
    public void setAccessCodeExpirationDate(Date date) {
        PreferencesUtils.putDate(preferences, SETTING_ACCESS_CODE_EXPIRATION_DATE, date);
    }

    @Override
    public Date getAccessCodeExpirationDate() {
        return PreferencesUtils.getDate(preferences, SETTING_ACCESS_CODE_EXPIRATION_DATE);
    }

    @Override
    public void setShowSponsoredPromo(boolean show) {
        preferences.edit().putBoolean(SHOW_SPONSORED_PROMO, show).apply();
    }

    @Override
    public boolean getNeedShowSponsoredPromo() {
        if (!hasAccessCode()) {
            return false;
        }

        AuthorizationService.AccessCodeInformation information = new AuthorizationService.AccessCodeInformation(getAccessCode(), getAccessCodeExpirationDate());
        return !information.isExpired() && preferences.getBoolean(SHOW_SPONSORED_PROMO, false);
    }

    @Override
    public void setAffiliationInfo(String affiliationInfo) {
        preferences.edit().putString(Settings.AFFILIATION_INFO + "_" + getUserLogin(), affiliationInfo).apply();
    }

    @Override
    public String getAffiliationInfo() {
        return preferences.getString(AFFILIATION_INFO + "_" + getUserLogin(), "");
    }

    @Override
    public void setDebugIp(String affiliationInfo) {
        preferences.edit().putString(DEBUG_IP, affiliationInfo).apply();
        notificationCenter.sendNotification(EventList.AFFILIATION_INFO_NEED_UPDATE.getEventName());
    }

    @Override
    public String getDebugIp() {
        return preferences.getString(DEBUG_IP, "");
    }

    @Override
    public int getCountDownloadedArticlePdf() {
        return preferences.getInt(COUNT_DOWNLOADED_ARTICLE_PDF, 0);
    }

    @Override
    public void incCountDownloadedArticlePdf() {
        int count = preferences.getInt(COUNT_DOWNLOADED_ARTICLE_PDF, 0);
        count++;
        preferences.edit().putInt(COUNT_DOWNLOADED_ARTICLE_PDF, count).apply();
    }

    @Override
    public void setSocietyScreenSectionTitleColor(String value) {
        preferences.edit().putString(SettingsImpl.SOCIETY_SCREEN_SECTION_TITLE_COLOR, null == value ? "" : value).apply();
    }

    @Override
    public String getSocietyScreenSectionTitleColor() {
        return preferences.getString(SettingsImpl.SOCIETY_SCREEN_SECTION_TITLE_COLOR, null);
    }

    @Override
    public void setSocietySponsoredSubscriptionAdUnitId(String value) {
        preferences.edit().putString(SettingsImpl.SOCIETY_SPONSORED_SUBSCRIPTION_AD_UNIT_ID, null == value ? "" : value).apply();
    }

    @Override
    public String getSocietySponsoredSubscriptionAdUnitId() {
        return preferences.getString(SettingsImpl.SOCIETY_SPONSORED_SUBSCRIPTION_AD_UNIT_ID, null);
    }

    @Override
    public void setSocietyAlternateTabLabelForSocietyFeedPage(String value) {
        preferences.edit().putString(SettingsImpl.SOCIETY_ALTERNATE_TAB_LABEL_FOR_SOCIETY_FEED_PAGE, null == value ? "" : value).apply();
    }

    @Override
    public String getSocietyAlternateTabLabelForSocietyFeedPage() {
        return preferences.getString(SettingsImpl.SOCIETY_ALTERNATE_TAB_LABEL_FOR_SOCIETY_FEED_PAGE, null);
    }

    @Override
    public void setSocietyIsSocietyContentEnabled(String value) {
        preferences.edit().putString(SettingsImpl.SOCIETY_IS_SOCIETY_CONTENT_ENABLED, null == value ? "" : value).apply();
    }

    @Override
    public boolean isSocietyContentEnabled() {
        return "Y".equals(preferences.getString(SettingsImpl.SOCIETY_IS_SOCIETY_CONTENT_ENABLED, null));
    }

    @Override
    public void setSocietyFooterLogoImageUrl(String value) {
        preferences.edit().putString(SettingsImpl.SOCIETY_FOOTER_LOGO_IMAGE_URL, null == value ? "" : value).apply();
    }

    @Override
    public String getSocietyFooterLogoImageUrl() {
        return preferences.getString(SettingsImpl.SOCIETY_FOOTER_LOGO_IMAGE_URL, null);
    }

    @Override
    public void setDeviceToken(String value) {
        preferences.edit().putString(SettingsImpl.DEVICE_TOKEN, null == value ? "" : value).apply();
    }

    @Override
    public String getDeviceToken() {
        return preferences.getString(SettingsImpl.DEVICE_TOKEN, null);
        //return "d97316505dc41220eb8aa1120d133d972aadf05fcc19cd418b5a2f448c5d42c4";
    }

    @Override
    public void setAnnouncementPanelOpen(boolean panelOpen) {
        preferences.edit().putBoolean(ANNOUNCEMENT_PANEL_OPEN, panelOpen).apply();
    }

    @Override
    public boolean isAnnouncementPanelOpen() {
        return preferences.getBoolean(ANNOUNCEMENT_PANEL_OPEN, true);
    }

    @Override
    public void setSocietyPageByDefault(boolean societyPageByDefault) {
        preferences.edit().putBoolean(SOCIETY_PAGE_BY_DEFAULT, societyPageByDefault).apply();
    }

    @Override
    public boolean isSocietyPageByDefault() {
        return preferences.getBoolean(SOCIETY_PAGE_BY_DEFAULT, false);
    }

    @Override
    public boolean isSocietyPageByDefaultInitialised() {
        return preferences.contains(SOCIETY_PAGE_BY_DEFAULT);
    }

    /**
     *    feature: keywords
     */

    @Override
    public void addKeyword(final String keyword) {
        if (null == keyword || keyword.equals("")) {
            return;
        }

        String set = preferences.getString(SettingsImpl.KEYWORD_SET, "");
        if (set.contains(keyword)) {
            return;
        }

        final String newSet = set + keyword + ',';
        preferences.edit().putString(SettingsImpl.KEYWORD_SET, newSet).apply();
    }

    @Override
    public void removeKeyword(final String keyword) {
        if (null == keyword || keyword.equals("")) {
            return;
        }

        String set = preferences.getString(SettingsImpl.KEYWORD_SET, null);
        if (null == set) {
            return;
        }

        int index = set.indexOf(keyword);
        if (index >= 0) {
            final String newSet = set.substring(0, index) + set.substring(index + keyword.length() + 1);
            preferences.edit().putString(SettingsImpl.KEYWORD_SET, newSet).apply();
        }
    }

    @Override
    public void updateKeywords(List<String> keywords) {
        String set = "";
        for (String keyword : keywords) {
            set = set + keyword + ',';
        }

        preferences.edit().putString(SettingsImpl.KEYWORD_SET, set).apply();
    }

    @Override
    public List<String> getKeywords() {
        String set = preferences.getString(SettingsImpl.KEYWORD_SET, null);
        if (null == set || set.equals("")) {
            return new ArrayList<>();
        }

        String[] arr = set.split(",");
        List<String> list = new ArrayList<>(arr.length);
        Collections.addAll(list, arr);

        return list;
    }

    @Override
    public void saveICloudKeywords(String keywords) {
        preferences.edit().putString(KEYWORDS, keywords).apply();
    }

    @Override
    public String getICloudKeywords() {
        return preferences.getString(KEYWORDS, null);
    }

    @Override
    public boolean isFullAccess() {
        return preferences.getBoolean(SETTING_IS_FULL_ACCESS, false);
    }

    @Override
    public void setFullAccess(boolean value) {
        preferences.edit().putBoolean(SETTING_IS_FULL_ACCESS, value).apply();
        onSettingsChanged(SETTING_IS_FULL_ACCESS);
    }

    @Override
    public boolean isFirstLaunch() {
        return preferences.getBoolean(IS_FIRST_LAUNCH, true);
    }

    @Override
    public void setFirstLaunch(boolean value) {
        preferences.edit().putBoolean(IS_FIRST_LAUNCH, value).apply();
    }

    /**
    *    feature: in-app purchase
    */

    @Override
    public void setSubscriptionReceipt(String receipt) {
        preferences.edit().putString(SUBSCRIPTION_RECEIPT, receipt).apply();
        onSettingsChanged(SETTING_IN_APP_PURCHASE);
    }

    @Override
    public String getSubscriptionReceipt() {
        return preferences.getString(SUBSCRIPTION_RECEIPT, null);
    }

    @Override
    public boolean hasSubscriptionReceipt() {
        final String subscriptionReceipt = getSubscriptionReceipt();
        return null != subscriptionReceipt && !subscriptionReceipt.equals("");
    }

    @Override
    public void setUserLogin(String userLogin) {
        preferences.edit().putString(USER_LOGIN, userLogin).apply();
    }

    public String getUserLogin() {
        return preferences.getString(USER_LOGIN, null);
    }

    /**
    * feature: google drive
    */

    @Override
    public void saveLocalSavedArticlesToLocalFile(String content) {
        preferences.edit().putString(LOCAL_SAVED_ARTICLES, content).apply();
    }

    @Override
    public void saveDriveSavedArticlesToLocalFile(String content) {
        preferences.edit().putString(DRIVE_SAVED_ARTICLES, content).apply();
    }

    @Override
    public String loadLocalSavedArticlesFromLocalFile() {
        return preferences.getString(LOCAL_SAVED_ARTICLES, "{}");
    }

    @Override
    public String loadDriveSavedArticlesFromLocalFile() {
        return preferences.getString(DRIVE_SAVED_ARTICLES, "{}");
    }

    @Override
    public void saveLocalReadArticlesToLocalFile(String content) {
        preferences.edit().putString(LOCAL_READ_ARTICLES, content).apply();
    }

    @Override
    public void saveDriveReadArticlesToLocalFile(String content) {
        preferences.edit().putString(DRIVE_READ_ARTICLES, content).apply();
    }

    @Override
    public String loadLocalReadArticlesFromLocalFile() {
        return preferences.getString(LOCAL_READ_ARTICLES, "{}");
    }

    @Override
    public String loadDriveReadArticlesFromLocalFile() {
        return preferences.getString(DRIVE_READ_ARTICLES, "{}");
    }

    @Override
    public void saveLocalKeywordsToLocalFile(String content) {
        preferences.edit().putString(LOCAL_KEYWORDS, content).apply();
    }

    @Override
    public void saveDriveKeywordsToLocalFile(String content) {
        preferences.edit().putString(DRIVE_KEYWORDS, content).apply();
    }

    @Override
    public String loadLocalKeywordsFromLocalFile() {
        return preferences.getString(LOCAL_KEYWORDS, "{}");
    }

    @Override
    public String loadDriveKeywordsFromLocalFile() {
        return preferences.getString(DRIVE_KEYWORDS, "{}");
    }

    @Override
    public void saveLocalSavedFeedItemsToLocalFile(String content) {
        preferences.edit().putString(LOCAL_SAVED_FEED_ITEMS, content).apply();
    }

    @Override
    public void saveDriveSavedFeedItemsToLocalFile(String content) {
        preferences.edit().putString(DRIVE_SAVED_FEED_ITEMS, content).apply();
    }

    @Override
    public String loadLocalSavedFeedItemsFromLocalFile() {
        return preferences.getString(LOCAL_SAVED_FEED_ITEMS, "{}");
    }

    @Override
    public String loadDriveSavedFeedItemsFromLocalFile() {
        return preferences.getString(DRIVE_SAVED_FEED_ITEMS, "{}");
    }

    /**
     *    feature: keywords
     */

    @Override
    public boolean isRegisteredDeviceOnMCS() {
        return preferences.getBoolean(KEYWORDS_REGISTERED_DEVICE_ON_MCS, false);
    }

    @Override
    public void  setRegisteredDeviceOnMCS(boolean value) {
        preferences.edit().putBoolean(KEYWORDS_REGISTERED_DEVICE_ON_MCS, value).apply();
    }

    @Override
    public void setAppLastUpgradeDate(Date date) {
        preferences.edit().putLong(LAST_UPGRADE_DATE, date.getTime()).apply();
    }

    @Override
    public long getAppLastUpgradeDate() {
        return preferences.getLong(LAST_UPGRADE_DATE, 0);
    }
}
