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
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.EncryptionUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Created by taraskreknin on 02.10.14.
 */
public class HomePageFeedListener implements Listener<InputStream> {

    private static final String TAG = HomePageFeedListener.class.getSimpleName();

    @Inject
    private SimpleParser mSimpleParser;
    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    private Settings mSettings;
    @Inject
    private HomePageService mHomePageService;
    @Inject
    @InjectCachePath
    private String rootPath;

    @Override
    public void onComplete(InputStream result, final Object... additionalData) throws Exception {

        final String feedString = IOUtils.toString(result);
        //Logger.d(TAG, "FEED: " + feedString);

        SectionsListContainer listContainer = null;
        InputStream inputStream = null;
        try {
            inputStream = IOUtils.toInputStream(feedString);
            listContainer = mSimpleParser.parse(inputStream, SectionsListContainer.class);
        } catch (Exception ex) {
            throw new ParseException(feedString, ex);
        } finally {
            if (null != inputStream)
                IOUtils.closeQuietly(inputStream);
        }

        String urlsString = null;
        for (Entry entry : listContainer.getList()) {
            if ("SOCIETY_NEWS_FEEDS".equalsIgnoreCase(entry.getKey())) {
                urlsString = entry.getValue();
            } else if ("SECTION_TITLE_LINKS_SOCIETY".equalsIgnoreCase(entry.getKey())) {
                mSettings.setSocietyScreenSectionTitleColor(entry.getValue());
                Logger.d(TAG, "SECTION_TITLE_LINKS_SOCIETY: " + mSettings.getSocietyScreenSectionTitleColor());
            } else if ("SPONSORED_AD_UNIT_ID".equalsIgnoreCase(entry.getKey())) {
                mSettings.setSocietySponsoredSubscriptionAdUnitId(entry.getValue().replaceAll("\n", "").replaceAll("\r", ""));
                Logger.d(TAG, "SPONSORED_AD_UNIT_ID: " + mSettings.getSocietySponsoredSubscriptionAdUnitId());
            } else if ("ALTERNATE_TAB_LABEL_SOCIETY_FEED".equalsIgnoreCase(entry.getKey())) {
                mSettings.setSocietyAlternateTabLabelForSocietyFeedPage(entry.getValue());
                Logger.d(TAG, "ALTERNATE_TAB_LABEL_SOCIETY_FEED: " + mSettings.getSocietyAlternateTabLabelForSocietyFeedPage());
            } else if ("ENABLE_SOCIETY_CONTENT_FEEDS".equalsIgnoreCase(entry.getKey())) {
                mSettings.setSocietyIsSocietyContentEnabled(entry.getValue());
                Logger.d(TAG, "ENABLE_SOCIETY_CONTENT_FEEDS: " + mSettings.isSocietyContentEnabled());
            } else if ("SOCIETY_LOGO_FOR_SOCIETY_FOOTER".equalsIgnoreCase(entry.getKey())) {
                final String url = entry.getValue();
                if (null != url) {
                    final File localFile = new File(rootPath, "societyLogo" + ".png");
                    final String localUrl = localFile.getAbsolutePath();
                    try {
                        FileUtils.copyURLToFile(new URL(url), localFile);
                        mSettings.setSocietyFooterLogoImageUrl(localUrl);
                    } catch (IOException e) {
                        Logger.s(TAG, e);
                    }
                    Logger.d(TAG, "SOCIETY_LOGO_FOR_SOCIETY_FOOTER: " + mSettings.getSocietyFooterLogoImageUrl());
                } else {
                    Logger.d(TAG, "SOCIETY_LOGO_FOR_SOCIETY_FOOTER: is empty");
                }
            } else if (!mSettings.isSocietyPageByDefaultInitialised() && "SET_DEFAULT_HOME_JOURNAL".equalsIgnoreCase(entry.getKey())) {
                mSettings.setSocietyPageByDefault(!"Y".equalsIgnoreCase(entry.getValue()));
            }
        }

        final Date now = new Date();
        if (null != urlsString && !urlsString.equals("")) {
            String[] urls = urlsString.split("\n");
            int sortIndex = 1;
            for (String url_ : urls) {
                String url = url_.replaceAll("\n", "").replaceAll("\r", "");
                Logger.d(TAG, "url = '" + url + "'");
                if (!"".equals(url)) {
                    String params = "";

                    final String uid = EncryptionUtils.md5Hash(url);

                    int index = url.indexOf("|");
                    if (index >= 0) {
                        params = url.substring(index + 1);
                        url = url.substring(0, index);
                    }

                    FeedMO feedStored = mHomePageService.getFeed(uid);
                    if (null != feedStored) {
                        feedStored.setInFeedDate(now);
                    } else {
                        feedStored = new FeedMO();
                        feedStored.setUid(uid);
                        feedStored.setInFeedDate(now);
                        feedStored.setUrl(url);
                        feedStored.setParams(params);
                    }

                    feedStored.setSortIndex(sortIndex++);
                    mHomePageService.saveFeed(feedStored);
                }
            }
        }

        mHomePageService.deleteOldFeeds(now);

        mNotificationCenter.sendNotification(EventList.HOME_FEED_UPDATED_SUCCESS.getEventName(),
                new ParamsBuilder()
                        .succeed(true)
                        .notModified(false)
                        .get());
    }

    @Override
    public void onNotModified() {
        mNotificationCenter.sendNotification(EventList.HOME_FEED_UPDATED_NOT_MODIFIED.getEventName(),
                new ParamsBuilder()
                        .succeed(true)
                        .notModified(true)
                        .get());
    }

    @Override
    public void onError(Exception ex) {
        Logger.s(TAG, ex.getMessage(), ex);
        mNotificationCenter.sendNotification(EventList.HOME_FEED_UPDATED_ERROR.getEventName(),
                new ParamsBuilder()
                        .succeed(false)
                        .withError(ex)
                        .get());
    }

    @Root(name = "inAppContents")
    private static class SectionsListContainer {

        @ElementList(entry="inAppContent", inline = true, required = false)
        private List<Entry> list;

        public List<Entry> getList() {
            return list;
        }
    }

    @Root
    private static  class Entry {

        @Attribute
        private String key;

        @Element(name="content", required = false)
        private String content;

        public  String getKey() {
            return key;
        }
        public String getValue() {
            return content;
        }
    }

}
