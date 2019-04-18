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
import com.wiley.wol.client.android.data.dao.TPSSiteDao;
import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.wiley.wol.client.android.notification.EventList.TPS_SITES_UPDATED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.TPS_SITES_UPDATED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.TPS_SITES_UPDATED_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;


/**
 * Created by alobachev on 7/8/14.
 */
public class TPSFeedListener implements Listener<InputStream> {
    private static final String TAG = TPSFeedListener.class.getSimpleName();

    @Inject
    private SimpleParser simpleParser;

    @Inject
    private Settings settings;

    @Inject
    private TPSSiteDao tpsSiteDao;

    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");

        final TpsConfigContainer tpsConfigContainer;
        try {
            tpsConfigContainer = simpleParser.parse(result, TpsConfigContainer.class);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }

        settings.setTPSExists(true);
        settings.setTPSUsername(tpsConfigContainer.getUsername());
        settings.setTPSPassword(tpsConfigContainer.getPassword());
        settings.setTPSTimeout(tpsConfigContainer.getTimeout());

        // tpsSites
        List<TPSSiteMO> tpsSites = tpsConfigContainer.getTpsSites();
        final Date now = new Date();
        int index = 1;

        for (final TPSSiteMO tpsSite : tpsSites) {
            tpsSite.setImportingDate(now);
            tpsSite.setSortIndex(index);

            index++;
        }

        tpsSiteDao.save(tpsSites);

        notificationCenter.sendNotification(TPS_SITES_UPDATED_SUCCESS.getEventName());
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");

        notificationCenter.sendNotification(TPS_SITES_UPDATED_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");

        if (ex.getMessage() != null && (ex.getMessage().endsWith(": 404") || ex.getMessage().endsWith(": 403"))) {
            settings.setTPSExists(false);
        }

        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(TPS_SITES_UPDATED_ERROR.getEventName(), params);
    }

    @Root(name = "tpsConfig")
    private static class TpsConfigContainer {

        @Attribute
        private String username;

        @Attribute
        private String password;

        @Attribute
        private Date lastModified;

        @Attribute
        private Integer timeout;

        @ElementList
        private List<TPSSiteMO> tpsSites;

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public List<TPSSiteMO> getTpsSites() {
            return this.tpsSites;
        }

    }
}
