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
import android.content.res.Configuration;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.settings.Environment;

import roboguice.RoboGuice;

/**
 * Created by alobachev on 7/25/14.
 */
public class GANHelper  {

    public static final String EVENT_APP = "app";
    public static final String EVENT_ARTICLE = "article";
    public static final String EVENT_FAVORITES = "favorites";
    public static final String EVENT_INTERNET_CONNECTION = "Internet Connection";
    public static final String EVENT_PDF_LOAD_ERROR = "PDF load error";
    public static final String EVENT_INFO = "info";
    public static final String EVENT_SEARCH_ARTICLE = "SearchArticle";
    public static final String EVENT_GET_ACCESS_A = "getAccessA";
    public static final String EVENT_GET_ACCESS_B = "getAccessB";
    public static final String EVENT_GET_ACCESS_C = "getAccessC";
    public static final String EVENT_GET_ACCESS_D = "getAccessD";
    public static final String EVENT_GET_ACCESS_E = "getAccessE";
    public static final String EVENT_GET_ACCESS_TPS_LOGIN = "TPS Login";
    public static final String EVENT_GET_ACCESS_TPS_LOGIN_FAIL = "Pop up TPS login fail";
    public static final String EVENT_GET_ACCESS_TPS_SOCIETY_UNKNOWN_ERROR = "Pop up TPS society unknown error";
    public static final String EVENT_GET_ACCESS_TPS_SELECTION = "TPS Selection";
    public static final String EVENT_GET_ACCESS_SUBSCRIPTION = "getAccessSubscription";
    public static final String ACTION_LAUNCH = "launch";
    public static final String ACTION_REFRESH = "refresh";
    public static final String ACTION_CHANGE_ORIENTATION = "change-orientation";
    public static final String ACTION_NOTIFICATIONS = "notifications";
    public static final String ACTION_FEATURES = "features";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_FONT_SIZE = "font-size";
    public static final String ACTION_CITE_MAIL = "cite-mail";
    public static final String ACTION_CITE_COPY = "cite-copy";
    public static final String ACTION_SHARE_EMAIL = "share-email";
    public static final String ACTION_REFERENCE = "reference";
    public static final String ACTION_AUTHORS = "authors";
    public static final String ACTION_ERROR = "Error";
    public static final String ACTION_FEEDBACK = "feedback";
    public static final String ACTION_PRIVACY_POLICY = "privacy_policy";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_LINK = "link";
    public static final String ACTION_SEARCH_FOR_TERM = "SearchForTerm";
    public static final String ACTION_BUTTON = "button";
    public static final String ACTION_UI_BUTTON = "UI button";
    public static final String ACTION_LINK_TO_GET_ACCESS_C = "link to getAccess C";
    public static final String ACTION_LINK_TO_GET_ACCESS_D = "link to getAccess D";
    public static final String LABEL_CLOSED = "closed";
    public static final String LABEL_USER_GENERATED = "user-generated";
    public static final String LABEL_AUTOMATIC = "automatic";
    public static final String LABEL_INFO = "info";
    public static final String LABEL_FIGURES = "figures";
    public static final String LABEL_REFERENCES = "references";
    public static final String LABEL_SUPPORTING_INFO = "supporting-info";
    public static final String LABEL_WOL = "wol";
    public static final String LABEL_LINK = "link";
    public static final String LABEL_INTERNET_ERROR_8 = "Internet Error (#8)";
    public static final String LABEL_PDF_LOAD_ERROR_27 = "PDF Load Error (#27)";
    public static final String LABEL_MENU = "menu";
    public static final String LABEL_YES = "yes";
    public static final String LABEL_YES_I_HAVE_ACCESS = "Yes I have access";
    public static final String LABEL_BROWSE_FREE = "browse free";
    public static final String LABEL_SOCIETY = "society";
    public static final String LABEL_SOCIETY_WEBSITE = "Society Website";
    public static final String LABEL_INSTITUTIONAL = "institutional";
    public static final String LABEL_WOL_LOGIN = "WOL login";
    public static final String LABEL_BACK = "back";
    public static final String LABEL_CLOSE = "close";
    public static final String LABEL__CLOSE = "Close";
    public static final String LABEL_HELP = "help";
    public static final String LABEL_LOGIN = "login";

    private static Context mContext;
    private static Tracker tracker;
    private static boolean bInit = false;

    /*
     * Google Analytics configuration values.
     */
    // Placeholder property ID.
    private static final String GA_PROPERTY_ID = "UA-39674335-1";
    //private static final String GA_PROPERTY_ID = "UA-50990045-1";

    // Dispatch period in seconds.
    private static final int GA_DISPATCH_PERIOD = 10;

    // Prevent hits from being sent to reports, i.e. during testing.
    private static final boolean GA_IS_DRY_RUN = false;

    // GA Logger verbosity.
    private static final Logger.LogLevel GA_LOG_VERBOSITY = Logger.LogLevel.VERBOSE;

    // Key used to store a user's tracking preferences in SharedPreferences.
    private static final String TRACKING_PREF_KEY = "trackingPreference";


    public static void init(final Context context) {
        mContext = context;

        GoogleAnalytics mGa = GoogleAnalytics.getInstance(context);
        tracker = GoogleAnalytics.getInstance(context).getTracker(GA_PROPERTY_ID);

        // Set dispatch period.
        GAServiceManager.getInstance().setLocalDispatchPeriod(GA_DISPATCH_PERIOD);

        // Set dryRun flag.
        mGa.setDryRun(GA_IS_DRY_RUN);

        // Set Logger verbosity.
        mGa.getLogger().setLogLevel(GA_LOG_VERBOSITY);

    }

    synchronized static Tracker getTracker() {
        if (!bInit) {
            Theme theme = RoboGuice.getInjector(mContext).getInstance(Theme.class);
            tracker.set("&cd3", theme.getAppPrefix());

            Environment environment = RoboGuice.getInjector(mContext).getInstance(Environment.class);
            tracker.set("&cd1", environment.getAppVersion());

            bInit = true;
        }

        return tracker;
    }

    public static void setOrientationVariable() {
        tracker.set("&cd2", mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "portrait" : "landscape");
    }

    public static void setVariableAtSlot(int index, String value) {
        if (index > 1 && index <=  20) {
            tracker.set("&cd" + index, value);
        }
    }

    public static void trackPageView(final String pageName, boolean sendOrientation) {
        if (sendOrientation) {
            setOrientationVariable();
        }

        getTracker().send(MapBuilder
                .createAppView()
                .set(Fields.SCREEN_NAME, pageName)
                .build());

        tracker.set("&cd2", null);

    }

    public static void trackEvent(final String event, final String action, final String label, Long value) {
        setOrientationVariable();
        getTracker().send(MapBuilder
                .createEvent(event, action, label, value)
                .build());
        tracker.set("&cd2", null);
    }

    public static void trackEventWithoutOrientation(final String event, final String action, final String label, Long value) {
        getTracker().send(MapBuilder
                .createEvent(event, action, label, value)
                .build());
    }

    public static void trackEventWithTPSUrl(final String event, final String action, final String label, Long value, String formUrl) {
        tracker.set("&cd4", formUrl);
        getTracker().send(MapBuilder
                .createEvent(event, action, label, value)
                .build());
        tracker.set("&cd4", null);
    }
}
