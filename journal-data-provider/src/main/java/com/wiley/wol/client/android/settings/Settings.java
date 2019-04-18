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

import android.content.SharedPreferences;

import com.wiley.wol.client.android.data.xml.AdvertisementConfigUnit;

import java.util.Date;
import java.util.List;

public interface Settings {

    public static final String TPS_EXISTS = "tps_exists";
    public static final String TPS_USERNAME = "tps_username";
    public static final String TPS_PASSWORD = "tps_password";
    public static final String TPS_TIMEOUT = "tps_timeout";

    public static final String SOCIETY_EXISTS = "society_exists";
    public static final String SOCIETY_LOGIN_INSTRUCTIONS = "society_login_instructions";
    public static final String SOCIETY_URL = "society_url";
    public static final String SOCIETY_INFORMATION = "society_information";

    public static final String DOWNLOAD_ISSUE = "download_issue";

    public static final String AFFILIATION_INFO = "affiliation_info";

    public static final String SETTING_ARTICLE_FONT_SIZE = "article_font_size";
    public static final String SETTING_ARTICLE_SHOW_ABSTRACT = "article_show_abstract";
    public static final String SETTING_CURRENT_SERVER = "current_server";
    public static final String SETTING_DEBUG_OAUTH_LINK = "debug_oauth_link";
    public static final String SETTING_AUTH_TOKEN = "auth_token";
    public static final String SETTING_ACCESS_CODE = "access_code_value";
    public static final String SETTING_IN_APP_PURCHASE = "in_app_purchase";
    public static final String SETTING_GOOGLE_DRIVE = "google_drive";

    public static final String SETTING_IS_FULL_ACCESS = "is_full_access";

    public static final int ARTICLE_FONT_SIZE_MIN = 13;
    public static final int ARTICLE_FONT_SIZE_MAX = 25;

    SharedPreferences getPreferences();

    int getArticleFontSize();

    boolean hasArticleFontSize();

    void changeArticleFontSize(int newSize);

    void setStoredAppVersion(String storedAppVersion);

    String getStoredAppVersion();

    boolean getArticleShowAbstract();

    boolean isArticleShowAbstractInitialized();

    void changeArticleShowAbstract(final boolean newShow);

    String getCurrentServer();

    boolean hasDebugServer();

    void changeCurrentServer(final String newServer);

    String getDebugOAuthLink();

    boolean hasDebugOAuthLink();

    void changeDebugOAuthLink(final String newLink);

    String getAccessToken();

    String getAccessTokenSecret();

    void refreshAccount();

    void resetAuthToken();

    boolean isAuthorized();

    void setAuthToken(final AuthToken authToken);

    boolean getSocietyExists();

    void setSocietyExists(final boolean exists);

    String getSocietyLoginInstructions();

    void setSocietyLoginInstructions(final String societyLoginInstructions);

    String getSocietyUrl();

    void setSocietyUrl(final String url);

    String getSocietyInformation();

    void setSocietyInformation(final String information);

    boolean getTPSExists();

    void setTPSExists(final boolean exists);

    String getTPSUsername();

    void setTPSUsername(final String username);

    String getTPSPassword();

    void setTPSPassword(final String password);

    int getTPSTimeout();

    void setTPSTimeout(final int timeout);

    AdvertisementConfigUnit getAdvertisementConfig();

    void setAdvertisementConfig(final AdvertisementConfigUnit configUnit);

    void setNeedShowGetAccessScreenOnStart(boolean show);

    boolean getNeedShowGetAccessScreenOnStart();

    void setHasLastLoginInfo(boolean hasInfo);

    boolean hasLastLoginInfo();

    void setLoggedInViaTps(boolean viaTps);

    boolean getLoggedInViaTps();

    void setTpsLoginSite(String siteName);

    String getTpsLoginSite();

    void setLoginScreen(int screen);

    int getLoginScreen();

    boolean hasAccessCode();

    void setAccessCode(String code);

    void setAccessCode(String code, String userLogin);

    String getAccessCode();

    void setAccessCodeExpirationDate(Date date);

    Date getAccessCodeExpirationDate();

    void setShowSponsoredPromo(boolean show);

    boolean getNeedShowSponsoredPromo();

    void setAffiliationInfo(String affiliationInfo);

    String getAffiliationInfo();

    void setDebugIp(String affiliationInfo);

    String getDebugIp();

    int getCountDownloadedArticlePdf();

    void incCountDownloadedArticlePdf();

    void setSocietyScreenSectionTitleColor(final String value);

    String getSocietyScreenSectionTitleColor();

    void setSocietySponsoredSubscriptionAdUnitId(final String value);

    String getSocietySponsoredSubscriptionAdUnitId();

    void setSocietyAlternateTabLabelForSocietyFeedPage(final String value);

    String getSocietyAlternateTabLabelForSocietyFeedPage();

    void setSocietyIsSocietyContentEnabled(final String value);

    boolean isSocietyContentEnabled();

    void setSocietyFooterLogoImageUrl(final String value);

    String getSocietyFooterLogoImageUrl();

    void setDeviceToken(String value);

    String getDeviceToken();

    void setAnnouncementPanelOpen(boolean panelOpen);

    boolean isAnnouncementPanelOpen();

    void setSocietyPageByDefault(boolean societyPageByDefault);

    boolean isSocietyPageByDefault();

    boolean isSocietyPageByDefaultInitialised();

    void addKeyword(String keyword);

    void removeKeyword(String keyword);

    void updateKeywords(List<String> keywords);

    List<String> getKeywords();

    void saveICloudKeywords(String keywords);

    String getICloudKeywords();

    boolean isFullAccess();

    void setFullAccess(boolean value);

    boolean isFirstLaunch();

    void setFirstLaunch(final boolean value);

    /**
    *       feature: in-app purchase
    */

    void setSubscriptionReceipt(String receipt);

    String getSubscriptionReceipt();

    boolean hasSubscriptionReceipt();

    void setUserLogin(String userLogin);

    /**
     *       feature: google drive
     */

    void saveLocalSavedArticlesToLocalFile(final String content);

    void saveDriveSavedArticlesToLocalFile(final String content);

    String loadLocalSavedArticlesFromLocalFile();

    String loadDriveSavedArticlesFromLocalFile();

    void saveLocalReadArticlesToLocalFile(final String content);

    void saveDriveReadArticlesToLocalFile(final String content);

    String loadLocalReadArticlesFromLocalFile();

    String loadDriveReadArticlesFromLocalFile();

    void saveLocalKeywordsToLocalFile(final String content);

    void saveDriveKeywordsToLocalFile(final String content);

    String loadLocalKeywordsFromLocalFile();

    String loadDriveKeywordsFromLocalFile();

    void saveLocalSavedFeedItemsToLocalFile(final String content);

    void saveDriveSavedFeedItemsToLocalFile(final String content);

    String loadLocalSavedFeedItemsFromLocalFile();

    String loadDriveSavedFeedItemsFromLocalFile();

    /**
    *    feature: keywords
    */

    boolean isRegisteredDeviceOnMCS();

    void  setRegisteredDeviceOnMCS(boolean value);

    void setAppLastUpgradeDate(Date date);

    long getAppLastUpgradeDate();
}
