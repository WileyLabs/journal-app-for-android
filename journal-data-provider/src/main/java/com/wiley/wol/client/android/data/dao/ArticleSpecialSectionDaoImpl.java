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

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleSpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by dfedorov
 * on 15/07/14.
 */
public class ArticleSpecialSectionDaoImpl implements ArticleSpecialSectionDao {
    private static final String TAG = ArticleSpecialSectionDao.class.getSimpleName();

    private final Dao<ArticleSpecialSectionMO, Integer> articleSpecialSectionDao;

    @Inject
    public ArticleSpecialSectionDaoImpl(final OrmLiteSqliteOpenHelper helper) throws SQLException {
        articleSpecialSectionDao = new DaoProvider<>(helper, ArticleSpecialSectionMO.class,
                Integer.class).get();
    }

    @Override
    public void create(ArticleSpecialSectionMO articleSpecialSection) {
        try {
            articleSpecialSectionDao.create(articleSpecialSection);
        } catch (SQLException e) {
            Logger.d(TAG, "create field for articleSpecialSection: " + e.getMessage());
        }
    }

    @Override
    public void delete(ArticleSpecialSectionMO articleSpecialSection) {
        try {
            if (articleSpecialSection.getUid() != null) {
                articleSpecialSectionDao.delete(articleSpecialSection);
            } else {
                final List<ArticleSpecialSectionMO> articleSpecialSectionList = articleSpecialSectionDao
                        .queryForMatching(articleSpecialSection);
                if (!articleSpecialSectionList.isEmpty()) {
                    articleSpecialSectionDao.delete(articleSpecialSectionList.get(0));
                }
            }
        } catch (SQLException e) {
            Logger.s(TAG, "delete() failed for articleSpecialSection", e);
        }
    }

    @Override
    public void delete(Collection<ArticleSpecialSectionMO> articleSpecialSections) {
        try {
            articleSpecialSectionDao.delete(articleSpecialSections);
        } catch (SQLException e) {
            Logger.s(TAG, "delete() failed for articleSpecialSections", e);
        }
    }

    @Override
    public Collection<ArticleSpecialSectionMO> queryForSpecialSection(SpecialSectionMO specialSection) {
        try {
            return articleSpecialSectionDao
                    .queryForMatching(new ArticleSpecialSectionMO(null, specialSection));
        } catch (SQLException e) {
            Logger.s(TAG, "queryForSpecialSection() failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<ArticleSpecialSectionMO> queryForArticle(ArticleMO article) {
        try {
            return articleSpecialSectionDao
                    .queryForMatching(new ArticleSpecialSectionMO(article, null));
        } catch (SQLException e) {
            Logger.s(TAG, "queryForSpecialSection() failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public QueryBuilder<ArticleSpecialSectionMO, Integer> queryBuilder() {
        return articleSpecialSectionDao.queryBuilder();
    }
}
