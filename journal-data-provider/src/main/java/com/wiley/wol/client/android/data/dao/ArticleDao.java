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
package com.wiley.wol.client.android.data.dao;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.wiley.wol.client.android.data.dao.filter.Filter;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public interface ArticleDao {

    void delete(ArticleMO article);

    Object executeBatch(Callable task);

    void save(ArticleMO article);

    String getFullHtmlBody(ArticleMO article);

    void updateSpecialSectionsForArticle(ArticleMO article);

    void save(List<ArticleMO> articles);

    ArticleMO findOneQuietly(DOI doi);

    ArticleMO findOne(DOI doi) throws ElementNotFoundException;

    ArticleMO findOne(String uri) throws ElementNotFoundException;

    List<ArticleMO> find(Filter<ArticleMO, Integer> filter);

    long getCount(Filter<ArticleMO, Integer> filter);

    QueryBuilder<ArticleMO, Integer> queryBuilder();

    List<ArticleMO> query(PreparedQuery<ArticleMO> preparedQuery) throws SQLException;

    List<ArticleMO> getArticlesToRemoveFromSection(SectionMO section);

    void saveRef(ArticleMO article);

    void updateSupportingInfo(ArticleMO article);

    void updateArticleRestrictedStatus(List<String> dois, boolean status);

    void makeArticleAsRead(DOI doi);

    void changeArticleLocalThumbnail(DOI doi, String thumbnailLocal);

    List<ArticleMO> getArticlesForSection(SectionMO section);

    List<ArticleMO> getArticlesForIssueTOC(DOI issueDoi);

    long getNumOfArticlesForIssueTOC(DOI issueDoi);

    List<ArticleMO> getAllArticleDOIs();

    boolean hasArticles();

    ArticleMO findByUid(Integer uid);
}
