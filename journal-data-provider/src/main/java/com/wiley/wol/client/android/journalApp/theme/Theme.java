package com.wiley.wol.client.android.journalApp.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.TextUtils;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.google.inject.Inject;
import com.wiley.wol.client.android.log.Logger;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import static java.lang.String.format;

public class Theme {
    private static final String TAG = Theme.class.getSimpleName();
    private static final String FEEDBACK_EMAIL_KEY = "feedback_email";

    private NSDictionary settings;
    private Properties projectState;

    private Context context;
    private SharedPreferences preferences;

    private static Boolean journalHasDarkBackground;

    @Inject
    public Theme(Context context, SharedPreferences preferences) throws IOException, ParserConfigurationException,
            ParseException, SAXException, PropertyListFormatException {
        this.context = context;
        this.preferences = preferences;
        readSettings();
        readProjectState();
    }

    // TODO: implement
    public int getDefaultArticleFontSize() {
        return 18;
    }

    public String getHelpUrl() {
        return getStringValue("getHelpURL");
    }

    public String getJournalName() {
        return getStringValue("journalName");
    }

    public String getForgottenPasswordUrl() {
        return "http://onlinelibrary.wiley.com/user/forgottenpassword";
    }

    public String getServerUrlOnServer(String serverUrl) {
        return format("http://%s/JAS-%s/%s", serverUrl, getAppPrefix().toUpperCase(),
                getMainURLPart());
    }

    public String getInAppContentFeedOnServer(String serverUrl) {
        return format("http://%s/JAS-%s/%s/inAppContent.feed?contentKey=about_journal&" +
                        "contentKey=%s", serverUrl, getAppPrefix().toUpperCase(),
                getMainURLPart(), FEEDBACK_EMAIL_KEY);
    }

    public String getSocietyFeedOnServer(String serverUrl) {
        return format("http://%s/JAS-%s/%s/inAppContent.feed?contentKey=all_society_content", serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getSpecialSectionsListFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/specialSections.feed", serverUrl,
                getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getTPSFeedOnServer(String serverUrl) {
        return format("http://%s/JAS-%s/%s/tpsSites.feed",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getAdvertisementFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/inAppContent.feed?contentKey=all_ad_content",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getRegisterNewUserFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/userReg.feed",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getActivateNewUserFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/userActivate.feed", serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getLicenseFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/license.feed",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getAnnouncementsFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/announcements.feed", serverUrl,
                getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getRssContentFeedOnServer(final String serverUrl) {
        // http://spa-as-dev.wiley.com/JAS-ACIE/journalApp/rssContent.feed
        return format("http://%s/JAS-%s/%s/rssContent.feed",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getAffiliationFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/affiliation.feed", serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getMainURLPart() {
        return "journalApp";
    }

    public String getOAuthLink() {
        return "http://stub.oauth.server.com";
    }

    public String getAppFeedBackEmail() {
        return preferences.getString(FEEDBACK_EMAIL_KEY, getStringValue("appFeedbackEmail"));
    }

    public String getAppFeedBackSubject() {
        return getStringValue("appFeedbackSubject");
    }

    public String getAppPrefix() {
        return getStringValue("appPrefix");
    }

    public String getAuthorizeCallbackUrl() {
        return getStringValue("authorizeCallbackURL");
    }

    public String getConsumerKey() {
        return getStringValue("consumerKey");
    }

    public String getSecretKey() {
        return getStringValue("secretKey");
    }

    public boolean isEnableContentUpdatesSubscription() {
        return getBooleanValue("enableContentUpdatesSubscription");
    }

    public boolean isEnableSubscription() {
        return getBooleanValue("enableSubscription");
    }

    public boolean isOpenAccessJournal() {
        return getBooleanValue("isOpenAccessJournal");
    }

    public String getFreeSubscriptionNameAppStore() {
        return getStringValue("freeSubscriptionNameAppStore");
    }

    public String getSubscriptionNameDev() {
        return getStringValue("freeSubscriptionNameDev");
    }

    public String getJournalCopyright() {
        return getStringValue("journalCopyright");
    }

    public String getJournalDoi() {
        String journalDOI = getStringValue("journalDOI");
        return journalDOI.split(" ")[0];
    }

    public boolean isJournalHasNoReferenceNumbers() {
        return getBooleanValue("journalHasNoReferenceNumbers");
    }

    public boolean isJournalHasNoTocForIssues() {
        return getBooleanValue("journalHasNoTOCForIssues");
    }

    public boolean isJournalHasNoTextAbstracts() {
        return getBooleanValue("journalHasNoTextAbstracts");
    }

    /*
    feature: in-app purchase
 */
    public String getMcsIdentityFeedOnServer(final String serverUrl) {
        // http://spa-as-dev.wiley.com/JAS-CAAC/journalApp/mcsIdentity.feed
        return format("http://%s/JAS-%s/%s/mcsIdentity.feed",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getPublicKey() {
        return getStringValue("pubKey");
    }

    public String getNameOfPaidSubscription() {
        // todo real property
        //return "fake.journal";
        if (context.getPackageName().contains(".dev"))
            return getStringValue("paidSubscriptionNameDev").toLowerCase();
        return getStringValue("paidSubscriptionNameAppSotre").toLowerCase();
    }

    public boolean isJournalHasDarkBackground() {
        if (journalHasDarkBackground != null) {
            return journalHasDarkBackground;
        }

        if (getIntegerValue("journalHasDarkBackground") == 1) {
            journalHasDarkBackground = false;
            return false;
        }

        if (getIntegerValue("journalHasDarkBackground") == -1) {
            journalHasDarkBackground = true;
            return true;
        }

        journalHasDarkBackground = isJournalHasDarkBackgroundForColor(getMainColor());
        return journalHasDarkBackground;
    }

    public boolean isJournalHasDarkBackgroundForResource(final int resId) {
        return getImageLightness(resId) < 70;
    }

    public boolean isJournalHasDarkBackgroundForColor(final int color) {
        return getLightnessForPixel(color) < 70;
    }

    private double getImageLightness(final int resId) {
        BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(resId);

        final int x = 74;
        final int minY = 5;
        final int maxY = 430;

        final int pixel1 = drawable.getBitmap().getPixel(x, minY);
        final int pixel2 = drawable.getBitmap().getPixel(x, maxY);

        final double lightness1 = getLightnessForPixel(pixel1);
        final double lightness2 = getLightnessForPixel(pixel2);

        return (lightness1 + lightness2) / 2;
    }

    private double getLightnessForPixel(int pixel) {
        double r = ((pixel >> 16) & 0xff) / 255.0f;
        double g = ((pixel >> 8) & 0xff) / 255.0f;
        double b = (pixel & 0xff) / 255.0f;

        //RGB2XYZ
        if (r > 0.04045) {
            r = Math.pow((r + 0.055) / 1.055, 2.4);
        } else {
            r = r / 12.92;
        }

        if (g > 0.04045) {
            g = Math.pow((g + 0.055) / 1.055, 2.4);
        } else {
            g = g / 12.92;
        }

        if (b > 0.04045) {
            b = Math.pow((b + 0.055) / 1.055, 2.4);
        } else {
            b = b / 12.92;
        }

        r = r * 100;
        g = g * 100;
        b = b * 100;

        double outY = r * 0.2126 + g * 0.7152 + b * 0.0722;

        //XYZ2LAB
        double refY = 100.000;

        outY = outY / refY;

        if (outY > 0.008856) {
            outY = Math.pow(outY, 1.0 / 3);
        } else {
            outY = (7.787 * outY) + (16 / 116);
        }

        return (116 * outY) - 16;
    }

    public boolean isShowAbstaractByDefault() {
        return getBooleanValue("isShowAbstractsByDefault");
    }

    public boolean isShowGraphicalAbstract() {
        return getBooleanValue("isShowGraphicalAbstract");
    }

    public String getLoginSteps() {
        return getStringValue("loginSteps");
    }

    public String getMainColorHEX() {
        return getStringValue("mainColor");
    }

    public int getMainColor() {
        return ColorUtils.parseHexColor(getStringValue("mainColor"));
    }

    public boolean isShowCovers() {
        String value = getStringValue("showCovers");
        if (TextUtils.isEmpty(value))
            return true;
        else
            return Boolean.parseBoolean(value);
    }

    public String getBuildDate() {
        return projectState.getProperty("buildDate");
    }

    public String getPrivacyPolicyUrl() {
        return "http://olabout.wiley.com/go/privacy_policy";
    }

    private void readProjectState() {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open("project_state.properties");
            projectState = new Properties();
            projectState.load(inputStream);
        } catch (IOException e) {
            Logger.s(TAG, e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void readSettings() throws IOException, PropertyListFormatException, ParseException, ParserConfigurationException, SAXException {
        final InputStream inputStream = context.getAssets().open("TargetSettings.plist");
        try {
            settings = (NSDictionary) PropertyListParser.parse(inputStream);
        } finally {
            inputStream.close();
        }
    }

    private boolean getBooleanValue(String key) {
        return Boolean.parseBoolean(getStringValue(key));
    }

    private String getStringValue(String key) {
        NSObject value = settings.get(key);
        return value != null ? value.toString() : "";
    }

    private int getIntegerValue(String key) {
        NSObject value = settings.get(key);
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    public boolean customSplashScreen() {
        return getBooleanValue("customSplashScreen");
    }

    public int getHeaderTextColor() {
        return getSectionBarTextColor();
    }

    private int getSectionBarTextColor() {
        return Color.rgb(66, 66, 66);
    }

    public int getHeaderBackColor(boolean journalHasDarkBackground) {
        return getSectionBarColor(journalHasDarkBackground);
    }

    private int getSectionBarColor(boolean journalHasDarkBackground) {
        int headerColor;
        if (journalHasDarkBackground) {
            headerColor = ColorUtils.brighterColorByPercent(getMainColor(), 10.0f);
        } else {
            headerColor = getMainColor();
        }
        return headerColor;
    }

    public int getColorForNewIssue() {
        return this.getMainColor();
    }

    public int getColorForDownloadedIssue() {
        return ColorUtils.parseHexColor("999999");
    }

    public int getColorForNoCoverIssue() {
        String colorHex = getStringValue("substrateColorForNoCoverIssue");
        if (TextUtils.isEmpty(colorHex))
            colorHex = "6D7073";
        return ColorUtils.parseHexColor(colorHex);
    }

    public int getColorForSelectedIssue() {
        return ColorUtils.changeAlpha(this.getMainColor(), 127);
    }

    public boolean needModifyHtmlForVideo() {
        return getAppPrefix().equals("CLD");
    }

    public int getDaysForShowAccessCodeExpiration() {
        return 7;
    }

    public String getHomeScreenFeedOnServer(final String serverUrl) {
        return format("http://%s/JAS-%s/%s/inAppContent.feed?" +
                        "contentKey=all_society_content&contentKey=feedback_email&contentKey=set_default_home_journal",
                serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getRestrictedStatusFeed(String serverUrl) {
        return format("http://%s/JAS-%s/%s/doiRestriction.feed", serverUrl, getAppPrefix().toUpperCase(), getMainURLPart());
    }

    public String getAppInfoString() {
        return String.format("Device: %s; Android: %s; App Version: %s; Project: %s;",
                getDeviceName(),
                Build.VERSION.RELEASE,
                getApplicationVersion(),
                getEngineVersion());
    }

    // todo: get real application version. in ios from "App-Info.plist"
    public String getApplicationVersion() {
        return "3.2.0";
    }

    // todo: get real engine version. in ios from "ProjectState.plist"
    public String getEngineVersion() {
        return "2128";
    }

    public String getJasUserAgent() {
        return String.format("appVersion=\"%s\"", getEngineVersion());
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
