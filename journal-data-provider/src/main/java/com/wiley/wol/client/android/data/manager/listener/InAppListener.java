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

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.IN_APP_CONTENT_UPDATED;
import static com.wiley.wol.client.android.notification.EventList.IN_APP_ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

/**
 * Created by dfedorov
 * on 02/07/14.
 */
public class InAppListener implements Listener<InputStream> {
    private static final String TAG = InAppListener.class.getSimpleName();

    @Inject
    private SimpleParser simpleParser;
    @Inject
    private SharedPreferences preferences;
    @Inject
    private NotificationCenter notificationCenter;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");
        final InAppContentListContainer inAppContentListContainer;
        try {
            inAppContentListContainer = simpleParser.parse(result, InAppContentListContainer.class);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }

        for (final InAppContent inAppContent : inAppContentListContainer.getInAppContents()) {
            preferences.edit().putString(inAppContent.getKey(), inAppContent.getContent()).apply();
        }
        notificationCenter.sendNotification(IN_APP_CONTENT_UPDATED.getEventName());
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");
        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(IN_APP_ERROR.getEventName(), params);
    }

    @Root(name = "inAppContents")
    private static class InAppContentListContainer {
        @ElementList(inline = true, type = InAppContent.class, empty = false)
        private Collection<InAppContent> inAppContents = new ArrayList<>();

        public Collection<InAppContent> getInAppContents() {
            return inAppContents;
        }

        public void setInAppContents(final Collection<InAppContent> inAppContents) {
            this.inAppContents = inAppContents;
        }
    }

    @Root(name = "inAppContent")
    private static class InAppContent {
        @Attribute(name = "key")
        private String key;
        @Element
        private String content;

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getContent() {
            return content;
        }

        public void setContent(final String content) {
            this.content = content;
        }
    }
}
