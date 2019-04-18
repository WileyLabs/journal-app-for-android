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
package com.wiley.wol.client.android.data.provider;

import com.google.inject.Provider;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;

public class DaoProvider<T, D> implements Provider<Dao<T, D>> {
    private static final String TAG = DaoProvider.class.getSimpleName();

    protected OrmLiteSqliteOpenHelper helper;
    protected Class<T> moClazz;
    protected Class<D> idClazz;

    public DaoProvider(final OrmLiteSqliteOpenHelper helper, final Class<T> moClazz, final Class<D> idClazz) {
        this.helper = helper;
        this.moClazz = moClazz;
        this.idClazz = idClazz;
    }

    @Override
    public Dao<T, D> get() {
        Dao<T, D> dao;
        try {
            dao = DaoManager.createDao(helper.getConnectionSource(), moClazz);
        } catch (final SQLException e) {
            Logger.s(TAG, "Failed to get dao for " + moClazz.getSimpleName());
            dao = null;
        }

        return dao;
    }
}
