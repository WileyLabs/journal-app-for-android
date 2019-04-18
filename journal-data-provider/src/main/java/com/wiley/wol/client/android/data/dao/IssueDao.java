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

import com.wiley.wol.client.android.data.dao.filter.Filter;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;

import java.util.List;

public interface IssueDao {

    List<IssueMO> findAll();

    long countOf();

    List<SectionMO> getSectionsForTOC(DOI doi) throws ElementNotFoundException;

    int getNumOfSavedArticles(DOI doi) throws ElementNotFoundException;

    IssueMO findOne(DOI doi) throws ElementNotFoundException;

    void save(IssueMO issue);

    void saveRef(IssueMO issueRef);

    void save(List<IssueMO> issues);

    void delete(IssueMO issue);

    void updateIssueFavoriteArticlesCount(IssueMO issue);

    long getCount(Filter<IssueMO, Integer> filter);

    @Deprecated
    void clear();
}
