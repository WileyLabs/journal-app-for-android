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
import com.wiley.wol.client.android.data.xml.AdvertisementConfigContainer;
import com.wiley.wol.client.android.data.xml.AdvertisementConfigUnit;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.ADV_FEED_UPDATE_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ADV_FEED_UPDATE_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ADV_FEED_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

public class AdvertisementFeedListener implements Listener<InputStream> {

    private static final String TAG = AdvertisementFeedListener.class.getSimpleName();

    @Inject
    private SimpleParser simpleParser;
    @Inject
    private Settings settings;
    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete()");

        final AdvertisementConfigUnit configUnit;
        final String feed = IOUtils.toString(result);
        InputStream inputStream = null;
        try {
            inputStream = IOUtils.toInputStream(feed);
            configUnit = simpleParser.parse(inputStream, AdvertisementConfigContainer.class).getConfigUnit();
        } catch (final Exception e) {
            Logger.d(TAG, "onComplete(): invalid content feed = " + feed);
            throw new ParseException(e);
        } finally {
            if (null != inputStream)
                IOUtils.closeQuietly(inputStream);
        }

        if (null != configUnit) {
            settings.setAdvertisementConfig(configUnit);
        }

        notificationCenter.sendNotification(ADV_FEED_UPDATE_SUCCESS.getEventName());
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified()");

        notificationCenter.sendNotification(ADV_FEED_UPDATE_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError()");
        Logger.s(TAG, ex.getMessage(), ex);

        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(ADV_FEED_UPDATE_ERROR.getEventName(), params);
    }
}
