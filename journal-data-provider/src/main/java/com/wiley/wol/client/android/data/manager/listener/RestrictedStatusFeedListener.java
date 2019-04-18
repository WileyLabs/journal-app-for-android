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
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RestrictedStatusFeedListener implements Listener<InputStream> {
    private static final String TAG = RestrictedStatusFeedListener.class.getSimpleName();
    @Inject
    private SimpleParser parser;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        final RestrictedStatusContainer resultStatuses = parseRestrictedStatusesFeed(result);

        final List<String> restrictedArticleDOIs = new LinkedList<>();
        final List<String> openArticleDOIs = new LinkedList<>();
        for (Article article : resultStatuses.getArticles()) {
            if (article.restricted) {
                restrictedArticleDOIs.add(article.doi);
            } else {
                openArticleDOIs.add(article.doi);
            }
        }

        if (!restrictedArticleDOIs.isEmpty()) {
            articleService.updateArticleRestrictedStatus(restrictedArticleDOIs, true);
        }
        if (!openArticleDOIs.isEmpty()) {
            articleService.updateArticleRestrictedStatus(openArticleDOIs, false);
        }
    }

    private RestrictedStatusContainer parseRestrictedStatusesFeed(final InputStream result) throws ParseException {
        final RestrictedStatusContainer resultStatuses;
        try {
            resultStatuses = parser.parse(result, RestrictedStatusContainer.class);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }
        return resultStatuses;
    }

    @Override
    public void onNotModified() {
    }

    @Override
    public void onError(Exception ex) {
        Logger.s(TAG, ex);
    }

    @Root(name = "restrictionStatus")
    private static class RestrictedStatusContainer {
        @ElementList(inline = true, type = Article.class, empty = false)
        private Collection<Article> articles;

        public Collection<Article> getArticles() {
            return articles;
        }

        public void setArticles(Collection<Article> articles) {
            this.articles = articles;
        }
    }

    @Root(name = "article")
    private static class Article {
        @Attribute(name = "doi")
        private String doi;
        @Attribute(name = "restricted")
        private boolean restricted;

        public String getDoi() {
            return doi;
        }

        public void setDoi(String doi) {
            this.doi = doi;
        }

        public boolean isRestricted() {
            return restricted;
        }

        public void setRestricted(boolean restricted) {
            this.restricted = restricted;
        }
    }
}
