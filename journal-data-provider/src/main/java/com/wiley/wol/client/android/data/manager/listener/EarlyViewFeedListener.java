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

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.dao.ArticleDao;
import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.xml.ArticleRefSimpleParser;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static java.lang.String.format;

public class EarlyViewFeedListener implements Listener<InputStream> {

    private static final String TAG = EarlyViewFeedListener.class.getSimpleName();

    @Inject
    private ArticleRefSimpleParser articleRefParser;
    @Inject
    private ArticleDao articleDao;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Settings settings;

    private String feed;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");

        final List<ArticleMO> articleRefs;

        final String feed = IOUtils.toString(result);
        if (feed.equals("<earlyViewArticleRefs>.</earlyViewArticleRefs>")) {
            articleRefs = new ArrayList<>();
        } else if (!feed.contains("<earlyViewArticleRefs")) {
            throw new ParseException(format("Unable to parse feed: '%s'", feed));
        } else {
            InputStream inputStream = null;
            try {
                inputStream = IOUtils.toInputStream(feed);
                articleRefs = articleRefParser.parseEarlyView(inputStream);
            } catch (final Exception ex) {
                throw new ParseException(feed, ex);
            } finally {
                if (null != inputStream)
                    IOUtils.closeQuietly(inputStream);
            }

        }

        final Date now = new Date();
        articleService.setPropertiesFromStored(articleRefs, now);
        articleService.saveRefsFromEarlyViewFeed(articleRefs);

        for (final ArticleMO storedArticle : articleService.getArticlesForEarlyView()) {
            if (!now.equals(storedArticle.getImportingDate())) {
                articleService.deleteFromEarlyView(storedArticle);
            }
        }

        notificationCenter.sendNotification(EARLY_VIEW_FEED_UPDATED.getEventName());
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");
        notificationCenter.sendNotification(EARLY_VIEW_FEED_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");
        Logger.s(TAG, ex.getMessage(), ex);
        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(EARLY_VIEW_FEED_ERROR.getEventName(), params);
    }
}