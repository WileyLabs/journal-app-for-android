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
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.data.xml.SpecialSectionSimpleParser;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.SPECIAL_SECTION_ID;

public class SpecialSectionFeedListener implements Listener<InputStream> {

    private static final String TAG = SpecialSectionFeedListener.class.getSimpleName();

    @Inject
    private SpecialSectionSimpleParser parser;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");
        final Date now = new Date();
        final SpecialSectionMO specialSection;
        try {
            specialSection = parser.parseSpecialSection(result);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }

        Collection<ArticleMO> storedArticlesOld = specialSectionService.getSpecialSectionById(specialSection.getUid()).getArticles();
        final Collection<ArticleMO> articleRefs = specialSection.getArticles();
        articleService.setPropertiesFromStored(articleRefs, now);
        for (final ArticleMO articleRef : articleRefs) {
            if (null == articleRef.getUid()) {
                final ArrayList<SpecialSectionMO> specialSections = new ArrayList<>();
                specialSections.add(specialSection);
                articleRef.setSpecialSections(specialSections);
            } else {
                final ArrayList<SpecialSectionMO> specialSectionsOld = new ArrayList<>(articleRef.getSpecialSections());
                boolean found =false;
                for (SpecialSectionMO specialSectionOld : specialSectionsOld) {
                    if (specialSectionOld.getUid().equals(specialSection.getUid())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    specialSectionsOld.add(specialSection);
                articleRef.setSpecialSections(specialSectionsOld);
            }
        }
        articleService.saveRefsFromSpecialSectionFeed(articleRefs);

        if (null != storedArticlesOld && storedArticlesOld.size() > 0) {
            Collection<ArticleMO> storedArticlesNew = specialSectionService.getSpecialSectionById(specialSection.getUid()).getArticles();
            for (ArticleMO oldArticle : storedArticlesOld) {
                boolean found = false;
                for (ArticleMO newArticle : storedArticlesNew) {
                    if (oldArticle.getUid().equals(newArticle.getUid())) {
                        found =true;
                        break;
                    }
                }
                if (!found) {
                    articleService.deleteFromSpecialSection(oldArticle);
                }
            }
        }

        specialSection.setNew(false);
        specialSection.setImportingDate(now);
        specialSectionService.createOrUpdate(Collections.singletonList(specialSection));

        final HashMap<String, Object> params = new HashMap<>(1);
        params.put(SPECIAL_SECTION_ID, specialSection.getUid());
        notificationCenter.sendNotification(SPECIAL_SECTION_FEED_UPDATED.getEventName(), params);
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");
        notificationCenter.sendNotification(SPECIAL_SECTION_FEED_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");
        Logger.s(TAG, ex.getMessage(), ex);
        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(SPECIAL_SECTION_FEED_ERROR.getEventName(), params);
    }
}