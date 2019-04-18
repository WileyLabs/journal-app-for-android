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

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.wiley.wol.client.android.data.provider.DaoProvider;

public class FilterFactoryImpl implements FilterFactory {

    private final OrmLiteSqliteOpenHelper helper;

    @Inject
    public FilterFactoryImpl(final OrmLiteSqliteOpenHelper helper) {
        this.helper = helper;
    }

    @Override
    public <T> BaseFilter<T, Integer> getFor(final Class<T> clazz) {
        final Dao<T, Integer> nativeDao = new DaoProvider<T, Integer>(helper, clazz, Integer.class).get();
        return new BaseFilter<T, Integer>(nativeDao);
    }
}
