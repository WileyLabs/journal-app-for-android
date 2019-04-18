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
package com.wiley.wol.client.android.settings;

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;

public class LastModifiedManager {
    private static final String TAG = LastModifiedManager.class.getSimpleName();
    private final Dao<LastModified, String> dao;

    @Inject
    public LastModifiedManager(final OrmLiteSqliteOpenHelper helper) {
        dao = new DaoProvider<>(helper, LastModified.class, String.class).get();
    }

    public synchronized void addLastModified(final String url, final String lastModifiedValue) {
        LastModified lastModified = new LastModified(url, lastModifiedValue);
        try {
            dao.createOrUpdate(lastModified);
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
    }

    public synchronized void removeLastModified(final String url) {
        try {
            dao.deleteById(url);
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
    }

    public synchronized void clearLastModified() {
        try {
            dao.deleteBuilder().delete();
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
    }

    public synchronized String getLastModified(final String url) {
        try {
            LastModified lastModified = dao.queryForId(url);
            if (lastModified == null)
                return null;
            else
                return lastModified.getLastModified();
        } catch (SQLException e) {
            Logger.s(TAG, e);
            return null;
        }
    }
}
