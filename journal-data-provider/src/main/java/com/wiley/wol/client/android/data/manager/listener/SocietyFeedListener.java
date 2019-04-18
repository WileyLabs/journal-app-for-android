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
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.SOCIETY_UPDATED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SOCIETY_UPDATED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.SOCIETY_UPDATED_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

/**
 * Created by alobachev
 * on 07/07/14.
 */
public class SocietyFeedListener implements Listener<InputStream> {
    private static final String TAG = SocietyFeedListener.class.getSimpleName();

    @Inject
    private SimpleParser simpleParser;

    @Inject
    private Settings settings;

    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");
        final InAppContentListContainer inAppContentListContainer;
        try {
            inAppContentListContainer = simpleParser.parse(result, InAppContentListContainer.class);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }

        String societyLoginInstructions = null;
        String societyUrl = null;
        String societyInformation = null;

        for (final InAppContent inAppContent : inAppContentListContainer.getInAppContents()) {
            String key = inAppContent.getKey();
            if (key.equalsIgnoreCase(Settings.SOCIETY_LOGIN_INSTRUCTIONS))
                societyLoginInstructions = inAppContent.getContent();
            else if (key.equalsIgnoreCase(Settings.SOCIETY_URL))
                societyUrl = inAppContent.getContent();
            else if (key.equalsIgnoreCase(Settings.SOCIETY_INFORMATION))
                societyInformation = inAppContent.getContent();
        }

        if (null != societyLoginInstructions) {
            settings.setSocietyExists(true);
            settings.setSocietyLoginInstructions(societyLoginInstructions);
            settings.setSocietyUrl(societyUrl);
            settings.setSocietyInformation(societyInformation);

        } else {
            settings.setSocietyExists(false);
        }

        notificationCenter.sendNotification(SOCIETY_UPDATED_SUCCESS.getEventName());
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");

        notificationCenter.sendNotification(SOCIETY_UPDATED_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");

        if (ex.getMessage() != null && (ex.getMessage().endsWith(": 404") || ex.getMessage().endsWith(": 403"))) {
            settings.setSocietyExists(false);
        }

        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(SOCIETY_UPDATED_ERROR.getEventName(), params);
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
        @Element(name = "content", required = false)
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
