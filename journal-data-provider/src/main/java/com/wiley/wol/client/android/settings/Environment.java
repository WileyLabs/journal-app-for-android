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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.google.inject.Inject;
import com.wiley.wol.client.android.log.Logger;

/**
 * Created by taraskreknin on 06.05.14.
 */
public class Environment {

    private String mAppVersion;

    @Inject
    public Environment(Context context) {
        retrieveAppVersion(context);
    }

    private void retrieveAppVersion(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Logger.d("Environment", e.getMessage(), e);
        }
        mAppVersion = pInfo == null ? "" : pInfo.versionName;
    }

    /**
     * Returns string representing current application version.
     */
    public String getAppVersion() {
        return mAppVersion;
    }

    public String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public String getDeviceName() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    public boolean isDebug() {
        return false;
    }

    public String[] getDebugServers() {
        assert (isDebug());
        return new String[]{"contentserver.wiley.com", "spa-as-qa.wiley.com",
                "spa-as-dev.wiley.com", "demo.wiley.ru/mobile", "spa-as-prod4.wiley.com"};
    }

    public String[] getDebugOAuthLinks() {
        assert (isDebug());
        return new String[]{
                "https://oauth.wiley.com/oauth",
                "https://qae.oauth.wiley.com",
                "https://qaf.oauth.wiley.com",
                "http://qa01.eal.ol.wiley.com:9092"
        };
    }
}
