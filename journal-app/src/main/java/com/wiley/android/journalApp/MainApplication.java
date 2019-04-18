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
package com.wiley.android.journalApp;

import android.app.Application;
import android.content.res.Configuration;

import com.bugsense.trace.BugSenseHandler;
import com.urbanairship.UAirship;
import com.wiley.android.journalApp.receiver.JasPushNotificationBuilder;
import com.wiley.wol.client.android.data.utils.GANHelper;

public class MainApplication extends Application {
    private int mOrientation;
    private static boolean mMainActivityVisible;

    @Override
    public void onCreate() {
        super.onCreate();

        mMainActivityVisible = false;

        // Google Analytics
        GANHelper.init(getApplicationContext());
        mOrientation = getApplicationContext().getResources().getConfiguration().orientation;

        // feature: Urban Airship
        UAirship.takeOff(this, new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship airship) {
                airship.getPushManager().setNotificationFactory(new JasPushNotificationBuilder(getApplicationContext()));
            }
        });

        // Bugsense
        BugSenseHandler.initAndStartSession(this, getResources().getString(R.string.bugsense_key));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mOrientation != newConfig.orientation) {
            mOrientation = newConfig.orientation;
            GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_APP,
                    GANHelper.ACTION_CHANGE_ORIENTATION,
                    getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "portrait" : "landscape",
                    0L);
        }
    }

    public static boolean isMainActivityVisible() {
        return mMainActivityVisible;
    }

    public static void mainActivityResumed() {
        mMainActivityVisible = true;
    }

    public static void mainActivityPaused() {
        mMainActivityVisible = false;
    }
}