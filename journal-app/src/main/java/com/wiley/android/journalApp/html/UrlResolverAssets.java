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
package com.wiley.android.journalApp.html;

import android.content.Context;

import com.wiley.android.journalApp.utils.AssetsUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;

/**
 * Created by Andrey Rylov on 12/05/14.
 */
public class UrlResolverAssets implements UrlResolver {

    private final Context context;

    public UrlResolverAssets(Context context) {
        this.context = context;
    }

    @Override
    public String resolve(String key) {
        key = proceedMacrosInKey(key);
        return HtmlUtils.getAssetsHtmlUrl(key);
    }

    private String proceedMacrosInKey(String key) {
        if (key.contains("!device!")) {
            String deviceTypeAsString = DeviceUtils.getDeviceTypeAsString(context);
            String tryKey = key.replace("!device!", deviceTypeAsString);
            if (HtmlUtils.hasAssetsHtmlUrl(context, tryKey))
                return tryKey;
            if (DeviceUtils.isPhone(context)) {
                tryKey = key.replace("!device!", "phone");
                if (HtmlUtils.hasAssetsHtmlUrl(context, tryKey))
                    return tryKey;
            }
            if (DeviceUtils.isTablet(context)) {
                tryKey = key.replace("!device!", "tablet");
                if (HtmlUtils.hasAssetsHtmlUrl(context, tryKey))
                    return tryKey;
            }
        }
        return key;
    }
}
