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
import android.os.Build;

import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.service.AnnouncementService;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.entity.AnnouncementMO;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AnnouncementFeedListener implements Listener<InputStream> {
    private static final String TAG = AnnouncementFeedListener.class.getSimpleName();
    @Inject
    private SimpleParser simpleParser;
    @Inject
    private AnnouncementService announcementService;
    @Inject
    private NotificationCenter notificationCenter;
    @com.google.inject.Inject
    @InjectCachePath
    private String rootPath;

    private boolean isPhone;

    public AnnouncementFeedListener(final boolean isPhone) {
        this.isPhone = isPhone;
    }

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        final AnnouncementsContainer announcements = simpleParser.parse(result,
                AnnouncementsContainer.class);

        for (AnnouncementMO announcement : announcements.list) {
            announcement.initImages(isPhone);
        }

        fillLocalImageUrls(announcements);

        announcementService.createOrUpdate(announcements.list);

        for (AnnouncementMO announcement : announcementService.getAnnouncements()) {
            boolean needToSendNotification = false;
            if (announcement.getImageLandscapeLocalURL() == null) {
                loadLandscapeUrl(announcement);
                needToSendNotification = true;
            }
            if (announcement.getImagePortraitLocalURL() == null) {
                loadPortraitUrl(announcement);
                needToSendNotification = true;
            }
            if (needToSendNotification) {
                notificationCenter.sendNotification(EventList.ANNOUNCEMENTS_UPDATE_SUCCESS.getEventName());
            }
        }

        notificationCenter.sendNotification(EventList.ANNOUNCEMENTS_UPDATE_SUCCESS.getEventName());
    }

    private void fillLocalImageUrls(AnnouncementsContainer announcements) {
        final List<AnnouncementMO> savedAnnouncements = announcementService.getAnnouncements();
        for (AnnouncementMO announcement : announcements.list) {
            final AnnouncementMO savedAnnouncement = findAnnouncementInList(savedAnnouncements, announcement.getUid());
            if (savedAnnouncement != null) {
                if (savedAnnouncement.getImageLandscapeURL().equals(announcement.getImageLandscapeURL())) {
                    announcement.setImageLandscapeLocalURL(savedAnnouncement.getImageLandscapeLocalURL());
                }

                if (savedAnnouncement.getImagePortraitURL().equals(announcement.getImagePortraitURL())) {
                    announcement.setImagePortraitLocalURL(savedAnnouncement.getImagePortraitLocalURL());
                }
            }
        }
    }

    private AnnouncementMO findAnnouncementInList(List<AnnouncementMO> announcements, String uid) {
        for (AnnouncementMO announcement : announcements) {
            if (announcement.getUid().equals(uid)) {
                return announcement;
            }
        }
        return null;
    }

    @Override
    public void onNotModified() {
        notificationCenter.sendNotification(EventList.ANNOUNCEMENTS_UPDATE_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        notificationCenter.sendNotification(EventList.ANNOUNCEMENTS_UPDATE_ERROR.getEventName(),
                new ParamsBuilder()
                        .succeed(false)
                        .withError(ex)
                        .get());
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void loadPortraitUrl(final AnnouncementMO currentAnnouncement) {
        final String localUrl;
        try {
            final File localFile = new File(rootPath, "announcement_portrait_" + currentAnnouncement.getUid() + ".png");
            localUrl = localFile.getAbsolutePath();
            FileUtils.copyURLToFile(new URL(currentAnnouncement.getImagePortraitURL()), localFile);
            currentAnnouncement.setImagePortraitLocalURL(localUrl);
            announcementService.createOrUpdate(currentAnnouncement);
        } catch (IOException e) {
            Logger.s(TAG, e);
        }
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void loadLandscapeUrl(final AnnouncementMO currentAnnouncement) {
        final String localUrl;
        try {
            final File localFile = new File(rootPath, "announcement_landscape_" + currentAnnouncement.getUid() + ".png");
            localUrl = localFile.getAbsolutePath();
            FileUtils.copyURLToFile(new URL(currentAnnouncement.getImageLandscapeURL()), localFile);
            currentAnnouncement.setImageLandscapeLocalURL(localUrl);
            announcementService.createOrUpdate(currentAnnouncement);
        } catch (IOException e) {
            Logger.s(TAG, e);
        }
    }

    @Root(name = "announcementConfig")
    private static class AnnouncementsContainer {
        @ElementList(entry = "announcement", inline = true, required = false)
        @Path("announcements")
        private List<AnnouncementMO> list = new ArrayList<>();
    }
}
