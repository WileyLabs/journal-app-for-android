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
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alobachev on 7/9/14.
 */
public class TPSSiteDaoImpl implements TPSSiteDao {
    private static final String TAG = TPSSiteDaoImpl.class.getSimpleName();

    private final OrmLiteSqliteOpenHelper helper;
    private final Dao<TPSSiteMO, Integer> tpsSiteDao;

    @Inject
    public TPSSiteDaoImpl(final OrmLiteSqliteOpenHelper helper) {
        this.helper = helper;
        tpsSiteDao = new DaoProvider<>(this.helper, TPSSiteMO.class, Integer.class).get();
    }

    @Override
    public List<TPSSiteMO> findAll() {
        try {
            return tpsSiteDao.queryForAll();
        } catch (final SQLException e) {
            Logger.s(TAG, "findAll() failed");
        }

        return new ArrayList<>();
    }

    @Override
    public void save(final List<TPSSiteMO> tpsSites) {

        List<TPSSiteMO> tpsSitesStored = findAll();
        for (TPSSiteMO tpsSiteStored : tpsSitesStored) {
            delete(tpsSiteStored);
        }

        for (final TPSSiteMO tpsSite : tpsSites) {
            create(tpsSite);
        }
    }

    private void delete(final TPSSiteMO tpsSiteMO) {
        try {
            tpsSiteDao.delete(tpsSiteMO);
        } catch (final SQLException ignored) {
            Logger.s(TAG, "delete() failed");
        }
    }

    private void create(final TPSSiteMO tpsSiteMO) {
        try {
            tpsSiteDao.create(tpsSiteMO);
        } catch (final SQLException ignored) {
            Logger.s(TAG, "create() failed");
        }
    }
}
