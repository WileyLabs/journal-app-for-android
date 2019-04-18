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
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTIONS_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTIONS_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTIONS_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

/**
 * Created by dfedorov
 * on 09/07/14.
 */
public class SpecialSectionsFeedListener implements Listener<InputStream> {
    private static final String TAG = SpecialSectionsFeedListener.class.getSimpleName();
    @Inject
    private SimpleParser simpleParser;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private NotificationCenter notificationCenter;

    private String feed;

    @Override
    public void onComplete(InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");

        final Date now = new Date();

        final Collection<SpecialSectionMO> specialSections;
        final String feed = IOUtils.toString(result).replaceAll("\r", "");
        if ( feed.equals("<special-sections>\n</special-sections>") || feed.equals("<special-sections/>")) {
            specialSections = new ArrayList<>();
        } else {
            InputStream inputStream = null;
            try {
                inputStream = IOUtils.toInputStream(feed);
                specialSections = simpleParser.parse(inputStream, SpecialSectionsContainer.class).specialSections;
            } catch (final Exception ex) {
                throw new ParseException(feed, ex);
            } finally {
                if (null != inputStream)
                    IOUtils.closeQuietly(inputStream);
            }
        }

        for (SpecialSectionMO specialSection : specialSections) {
            SpecialSectionMO storedSpecialSection = specialSectionService
                    .getSpecialSectionById(specialSection.getUid());
            if (storedSpecialSection == null) {
                specialSection.setNew(true);
                specialSection.setNeedToCheck(false);
            } else {
                specialSection.setNew(false);
                specialSection.setArticles(storedSpecialSection.getArticles());
            }

            specialSection.setInFeedDate(now);
        }
        specialSectionService.createOrUpdate(specialSections);

        final Collection<SpecialSectionMO> specialSectionsMO = specialSectionService.getSpecialSections();
        for (final SpecialSectionMO specialSectionMO : specialSectionsMO) {
            final Date storedDate = specialSectionMO.getInFeedDate();
            if (!now.equals(storedDate)) {
                specialSectionService.delete(specialSectionMO);
            }
        }

        notificationCenter.sendNotification(SPECIAL_SECTIONS_UPDATED.getEventName());
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");

        notificationCenter.sendNotification(SPECIAL_SECTIONS_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(Exception ex) {
        Logger.d(TAG, "onError");
        Logger.s(TAG, ex.getMessage(), ex);
        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(SPECIAL_SECTIONS_ERROR.getEventName(), params);
    }

    @Root(name = "special-sections")
    private static final class SpecialSectionsContainer {
        @ElementList(inline = true, type = SpecialSectionMO.class, empty = false)
        private Collection<SpecialSectionMO> specialSections = new ArrayList<>();
    }
}
