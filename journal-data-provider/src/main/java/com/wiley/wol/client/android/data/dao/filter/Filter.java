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

import com.j256.ormlite.stmt.PreparedQuery;
import com.wiley.wol.client.android.exception.ElementNotFoundException;

import java.sql.SQLException;

public interface Filter<T, ID> {

    PreparedQuery<T> getOrmSpecificRequest() throws ElementNotFoundException;

    Filter<T, ID> where();

    Filter<T, ID> eq(String columnName, Object value) throws SQLException;

    Filter<T, ID> and();

    Filter<T, ID> or();

    Filter<T, ID> not();

    Filter<T, ID> isNotNull(String columnName) throws SQLException;

    Filter<T, ID> countOf();

    Filter<T, ID> orderBy(String columnName, boolean ascending);
}
