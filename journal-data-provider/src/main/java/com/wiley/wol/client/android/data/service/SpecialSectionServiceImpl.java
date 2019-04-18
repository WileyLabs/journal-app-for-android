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

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.wiley.wol.client.android.data.dao.ArticleDao;
import com.wiley.wol.client.android.data.dao.ArticleSpecialSectionDao;
import com.wiley.wol.client.android.data.dao.filter.Filter;
import com.wiley.wol.client.android.data.dao.filter.FilterFactory;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleSpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Created by dfedorov
 * on 09/07/14.
 */
public class SpecialSectionServiceImpl implements SpecialSectionService {
    private static final String TAG = SpecialSectionServiceImpl.class.getSimpleName();
    @Inject
    private ArticleDao articleDao;
    @Inject
    private ArticleService articleService;
    private final Dao<SpecialSectionMO, String> specialSectionDao;
    @Inject
    private ArticleSpecialSectionDao articleSpecialSectionDao;
    @Inject
    private ImportManager importManager;
    @Inject
    private FilterFactory filterFactory;
    private PreparedQuery<ArticleMO> articlesForSpecialSectionPreparedQuery;

    @Inject
    public SpecialSectionServiceImpl(final OrmLiteSqliteOpenHelper helper) {
        specialSectionDao = new DaoProvider<>(helper, SpecialSectionMO.class, String.class).get();
    }

    @Override
    public SpecialSectionMO getSpecialSectionById(String id) {
        try {
            final SpecialSectionMO specialSection = specialSectionDao.queryForId(id);
            if (specialSection != null) {
                specialSection.setArticles(getArticlesForSpecialSection(specialSection));
            }
            return specialSection;
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void downloadSpecialSectionContent(String id) {
        importManager.updateSpecialSection(id);
    }

    @Override
    public List<SpecialSectionMO> getSpecialSections() {
        try {
            final List<SpecialSectionMO> specialSections = specialSectionDao.queryForAll();
            for (SpecialSectionMO specialSection : specialSections) {
                specialSection.setArticles(getArticlesForSpecialSection(specialSection));
            }
            return specialSections;
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
            return emptyList();
        }
    }

    @Override
    public boolean hasSpecialSections() {
        try {
            long count = specialSectionDao.countOf();
            return count > 0;
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void create(SpecialSectionMO specialSection) {
        try {
            specialSectionDao.create(specialSection);
            for (ArticleMO article : specialSection.getArticles()) {
                articleSpecialSectionDao.create(new ArticleSpecialSectionMO(article, specialSection));
            }
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void delete(SpecialSectionMO specialSection) {
        try {
            articleSpecialSectionDao.delete(articleSpecialSectionDao.queryForSpecialSection(specialSection));
            specialSectionDao.delete(specialSection);
            deleteArticlesFromSpecialSection(specialSection);
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
        }
    }

    @Override
    public long getOpenedSpecialSectionsCount() {
        try {
            return getCount(filterFactory.getFor(SpecialSectionMO.class)
                    .where()
                    .isNotNull(SpecialSectionMO.IMPORTING_DATE));
        } catch (final SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long count() {
        try {
            return specialSectionDao.countOf();
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
            return 0;
        }
    }

    private void deleteArticlesFromSpecialSection(final SpecialSectionMO specialSection) {
        for (final ArticleMO storedArticle : specialSection.getArticles()) {
            articleService.deleteFromSpecialSection(storedArticle);
        }
    }

    @Override
    public void createOrUpdate(Collection<SpecialSectionMO> specialSections) {
        for (SpecialSectionMO specialSection : specialSections) {
            if (specialSection.isNew()) {
                create(specialSection);
            } else {
                update(specialSection);
            }
        }
    }

    private void update(SpecialSectionMO specialSection) {
        try {
            specialSectionDao.update(specialSection);

            for (ArticleMO article : getArticlesForSpecialSection(specialSection)) {
                if (!isSpecialSectionContainsArticle(specialSection, article)) {
                    articleSpecialSectionDao.delete(new ArticleSpecialSectionMO(article, specialSection));
                    articleService.deleteFromSpecialSection(article);
                }
            }
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
        }
    }

    private boolean isSpecialSectionContainsArticle(SpecialSectionMO specialSection, ArticleMO article) {
        for (ArticleMO specialSectionsArticle : specialSection.getArticles()) {
            if (specialSectionsArticle.getDOI().equals(article.getDOI())) {
                return true;
            }
        }
        return false;
    }

    private Collection<ArticleMO> getArticlesForSpecialSection(final SpecialSectionMO specialSection)
            throws SQLException {
        if (articlesForSpecialSectionPreparedQuery == null) {
            articlesForSpecialSectionPreparedQuery = makeArticleForSpecialSectionQuery();
        }
        articlesForSpecialSectionPreparedQuery.setArgumentHolderValue(0, specialSection);
        return articleDao.query(articlesForSpecialSectionPreparedQuery);
    }

    private PreparedQuery<ArticleMO> makeArticleForSpecialSectionQuery() throws SQLException {
        final QueryBuilder<ArticleSpecialSectionMO, Integer> articleSpecialSectionQb =
                articleSpecialSectionDao.queryBuilder();

        articleSpecialSectionQb.selectColumns("article_uid");
        SelectArg articleSelectArg = new SelectArg();
        articleSpecialSectionQb.where().eq("special_section_uid", articleSelectArg);
        QueryBuilder<ArticleMO, Integer> articleQb = articleDao.queryBuilder();
        articleQb.where().in("uid", articleSpecialSectionQb);

        return articleQb.prepare();
    }

    private long getCount(Filter<SpecialSectionMO, Integer> filter) throws SQLException {
        try {
            return specialSectionDao.countOf(filter.countOf().getOrmSpecificRequest());
        } catch (final ElementNotFoundException e) {
            Logger.s(TAG, e.getMessage(), e);
            return 0;
        }
    }
}
