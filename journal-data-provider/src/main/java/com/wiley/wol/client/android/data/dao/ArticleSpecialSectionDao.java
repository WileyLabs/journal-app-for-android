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

import com.j256.ormlite.stmt.QueryBuilder;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleSpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;

import java.util.Collection;

/**
 * Created by dfedorov
 * on 15/07/14.
 */
public interface ArticleSpecialSectionDao {
    void create(ArticleSpecialSectionMO articleSpecialSection);

    void delete(ArticleSpecialSectionMO articleSpecialSection);

    void delete(Collection<ArticleSpecialSectionMO> articleSpecialSections);

    Collection<ArticleSpecialSectionMO> queryForSpecialSection(SpecialSectionMO specialSection);

    Collection<ArticleSpecialSectionMO> queryForArticle(ArticleMO articleMO);

    QueryBuilder<ArticleSpecialSectionMO, Integer> queryBuilder();
}
