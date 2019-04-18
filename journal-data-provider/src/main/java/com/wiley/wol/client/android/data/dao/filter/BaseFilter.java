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
package com.wiley.wol.client.android.data.dao.filter;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.wiley.wol.client.android.exception.ElementNotFoundException;

import java.sql.SQLException;

public class BaseFilter<T, ID> implements Filter<T, ID> {

    private final QueryBuilder<T, ID> queryBuilder;
    private Where<T, ID> where;

    public BaseFilter(final Dao<T, ID> dao) {
        queryBuilder = dao.queryBuilder();
        where = queryBuilder.where().reset();
    }

    @Override
    public PreparedQuery<T> getOrmSpecificRequest() throws ElementNotFoundException {
        try {
            return queryBuilder.prepare();
        } catch (final Exception e) {
            throw new ElementNotFoundException(e);
        }
    }

    @Override
    public BaseFilter<T, ID> where() {
        return this;
    }

    @Override
    public Filter<T, ID> eq(final String columnName, final Object value) throws SQLException {
        where = where.eq(columnName, value);
        return this;
    }

    @Override
    public Filter<T, ID> and() {
        where = where.and();
        return this;
    }

    @Override
    public Filter<T, ID> or() {
        where = where.or();
        return this;
    }

    @Override
    public Filter<T, ID> not() {
        where = where.not();
        return this;
    }

    @Override
    public Filter<T, ID> isNotNull(final String columnName) throws SQLException {
        where = where.isNotNull(columnName);
        return this;
    }

    @Override
    public Filter<T, ID> countOf() {
        queryBuilder.setCountOf(true);
        return this;
    }

    @Override
    public Filter<T, ID> orderBy(final String columnName, final boolean ascending) {
        queryBuilder.orderBy(columnName, ascending);
        return this;
    }
}
