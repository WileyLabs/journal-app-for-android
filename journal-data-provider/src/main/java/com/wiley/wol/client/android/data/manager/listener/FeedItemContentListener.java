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
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by admin on 18/12/14.
 */
public class FeedItemContentListener implements Listener<InputStream> {
    private static final String TAG = FeedItemContentListener.class.getSimpleName();

    @Inject
    private NotificationCenter mNotificationCenter;

    @Override
    public void onComplete(InputStream result, final Object... additionalData) throws Exception {

        String feedItemContentUrl = "";
        final String feedItemString = IOUtils.toString(result);
        //Logger.d(TAG, "FeedItemContent: " + feedItemString);

        if (additionalData != null && additionalData.length > 0) {
            feedItemContentUrl = (String) ((Map) additionalData[0]).get("feedItemContentUrl");
        }

        mNotificationCenter.sendNotification(EventList.FEED_ITEM_CONTENT_SUCCESS.getEventName(),
                new ParamsBuilder()
                        .succeed(true)
                        .withFeedItemUrl(feedItemContentUrl)
                        .withFeedItemContent(feedItemString)
                        .get());
    }
    @Override
    public void onNotModified() {
        mNotificationCenter.sendNotification(EventList.FEED_ITEM_CONTENT_NOT_MODIFIED.getEventName(),
                new ParamsBuilder()
                        .succeed(false)
                        .get());
    }

    @Override
    public void onError(Exception ex) {
        Logger.s(TAG, ex.getMessage(), ex);
        mNotificationCenter.sendNotification(EventList.FEED_ITEM_CONTENT_ERROR.getEventName(),
                new ParamsBuilder()
                        .succeed(false)
                        .withError(ex)
                        .get());
    }
}
