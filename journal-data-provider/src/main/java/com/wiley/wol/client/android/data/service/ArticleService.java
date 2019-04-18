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
package com.wiley.wol.client.android.data.service;

import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface ArticleService {

    String ACTION_SUBSCRIBE = "subscribe";

    String ACTION_UNSUBSCRIBE = "unsubscribe";

    String ARTICLE_MO = "ArticleMo";

    ArticleMO getArticle(DOI doi);

    ArticleMO getArticleQuietly(DOI doi);

    List<ArticleMO> getArticlesForEarlyView();

    boolean hasArticlesForEarlyView();

    List<ArticleMO> getSavedArticles();

    long getSavedArticleCount();

    long getReadArticleCount();

    ArticleMO getArticleFromDao(DOI doi) throws ElementNotFoundException;

    ArticleMO getArticleFromDaoByUri(String uri) throws ElementNotFoundException;

    void addArticleRefToFavorites(ArticleMO article);

    void removeArticleRefFromFavorites(ArticleMO article);

    boolean isArticleRefFavoriteChangingInProgress(DOI doi);

    boolean isArticleRefUpdating(DOI doi);

    void markArticleAsRead(DOI doi);

    void changeArticleLocalThumbnail(DOI doi, String path);

    void updateArticleRestrictedStatus(List<String> dois, boolean status);

    void updateEarlyViewFeed();

    boolean isDownloaded(DOI doi);

    void setPropertiesFromStored(final Collection<ArticleMO> articleRefs, final Date importingDate);

    void saveRefsFromSpecialSectionFeed(final Collection<ArticleMO> articleRefs);

    void saveRefsFromEarlyViewFeed(final Collection<ArticleMO> articleRefs);

    void saveRefsFromIssueTocFeed(final SectionMO section, final Collection<ArticleMO> articleRefs);

    void deleteFromEarlyView(final ArticleMO article);

    void deleteFromIssueToc(final ArticleMO article);

    void deleteFromSpecialSection(final ArticleMO article);

    void removeOutdatedArticlesFromSection(SectionMO section);

    boolean hasArticlePdf(DOI doi);

    String getPdfSize(DOI doi);

    String getArticleCitation(DOI doi);

    boolean isArticleRestricted(DOI doi);

    boolean isArticleFavorite(DOI doi);

    List<ArticleMO> getArticlesForSection(SectionMO section);

    List<ArticleMO> getArticlesForIssueTOC(DOI issueDoi);

    long getNumOfArticlesForIssueTOC(DOI issueDoi);

    String getFullHtmlBody(ArticleMO article);

    String getKeywords(DOI doi);

    void updateArticleInfoHtmlBody(ArticleMO article);

    String loadArticleInfoHtmlBody(ArticleMO article);

    void changeKeyword(String keyword, String action);

    void updateListOfSubscribedKeywords();

    List<ArticleMO> getAllArticleDOIs();

    boolean hasArticles();

    ArticleMO getArticleByUid(Integer uid);
}
