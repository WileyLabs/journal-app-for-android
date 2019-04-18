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
package com.wiley.wol.client.android.data.manager.listener;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Map;

import roboguice.util.Strings;

import static com.wiley.wol.client.android.notification.EventList.AFFILIATION_INFO_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.AFFILIATION_INFO_UPDATE_FINISHED;

public class AffiliationFeedListener implements Listener<InputStream> {
    private static final String TAG = EarlyViewFeedListener.class.getSimpleName();

    @Inject
    private Settings settings;
    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(InputStream result, Object... additionalData) throws Exception {
        if (additionalData != null && additionalData.length > 0) {
            final String affiliationInfo = (String) ((Map) additionalData[0]).get("X-Mobile-Affiliation");
            if (affiliationInfo != null && !affiliationInfo.equals(settings.getAffiliationInfo())) {
                if(isAffiliationInfoChanged(affiliationInfo)) {
                    settings.setAffiliationInfo(affiliationInfo);
                    notificationCenter.sendNotification(AFFILIATION_INFO_CHANGED.getEventName());
                }
            }
        }
        notificationCenter.sendNotification(AFFILIATION_INFO_UPDATE_FINISHED.getEventName());
    }

    private boolean isAffiliationInfoChanged(String affiliationInfo) {
        try {
            final JSONObject newAffiliationInfoJson = Strings.isEmpty(affiliationInfo) ? new JSONObject() :
                    new JSONObject(affiliationInfo);
            JSONObject existedAffiliationInfoJson = Strings.isEmpty(settings.getAffiliationInfo()) ? new JSONObject() :
                    new JSONObject(settings.getAffiliationInfo());
            return !JsonUtils.jsonObjectEquals(newAffiliationInfoJson, existedAffiliationInfoJson);
        } catch (JSONException e) {
            Logger.s(TAG, e);
        }
        return false;
    }

    @Override
    public void onNotModified() {
        Logger.i(TAG, "Affiliation feed does not modified");
        notificationCenter.sendNotification(AFFILIATION_INFO_UPDATE_FINISHED.getEventName());
    }

    @Override
    public void onError(Exception ex) {
        Logger.s(TAG, ex);
        notificationCenter.sendNotification(AFFILIATION_INFO_UPDATE_FINISHED.getEventName());
    }
}
