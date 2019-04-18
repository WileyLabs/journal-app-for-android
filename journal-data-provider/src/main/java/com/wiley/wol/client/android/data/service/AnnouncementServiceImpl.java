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
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.domain.entity.AnnouncementMO;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AnnouncementServiceImpl implements AnnouncementService {
    private static final String TAG = AnnouncementServiceImpl.class.getSimpleName();
    private final Dao<AnnouncementMO, String> announcementDao;

    @Inject
    public AnnouncementServiceImpl(final OrmLiteSqliteOpenHelper helper) {
        announcementDao = new DaoProvider<>(helper, AnnouncementMO.class, String.class).get();
    }

    @Override
    public List<AnnouncementMO> getActualAnnouncements() {
        final Date currentDate = new Date();
        final QueryBuilder<AnnouncementMO, String> announcementQueryBuilder = announcementDao.queryBuilder();
        try {
            announcementQueryBuilder.where()
                    .le(AnnouncementMO.START_DATE, currentDate)
                    .and()
                    .gt(AnnouncementMO.END_DATE, currentDate)
                    .and()
                    .isNotNull(AnnouncementMO.IMAGE_LANDSCAPE_LOCAL_URL)
                    .and()
                    .isNotNull(AnnouncementMO.IMAGE_PORTRAIT_LOCAL_URL);
            return announcementQueryBuilder.query();
        } catch (SQLException e) {
            Logger.s(TAG, e);
            return Collections.emptyList();
        }
    }

    @Override
    public long getActualAnnouncementsCount() {
        final Date currentDate = new Date();
        final QueryBuilder<AnnouncementMO, String> announcementQueryBuilder = announcementDao.queryBuilder();
        try {
            announcementQueryBuilder.where()
                    .le(AnnouncementMO.START_DATE, currentDate)
                    .and()
                    .gt(AnnouncementMO.END_DATE, currentDate)
                    .and()
                    .isNotNull(AnnouncementMO.IMAGE_LANDSCAPE_LOCAL_URL)
                    .and()
                    .isNotNull(AnnouncementMO.IMAGE_PORTRAIT_LOCAL_URL);
            return announcementQueryBuilder.countOf();
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
        return 0;
    }

    @Override
    public void createOrUpdate(AnnouncementMO announcement) {
        try {
            announcementDao.createOrUpdate(announcement);
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
    }

    @Override
    public void createOrUpdate(List<AnnouncementMO> announcements) {
        try {
            final Date currentDate = new Date();
            for (AnnouncementMO announcement : announcements) {
                announcement.setInFeedDate(currentDate);
                announcementDao.createOrUpdate(announcement);
            }

            final DeleteBuilder<AnnouncementMO, String> announcementDeleteBuilder = announcementDao.deleteBuilder();
            announcementDeleteBuilder.where().lt("in_feed_date", currentDate);
            announcementDeleteBuilder.delete();
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
    }

    @Override
    public List<AnnouncementMO> getAnnouncements() {
        try {
            return announcementDao.queryForAll();
        } catch (SQLException e) {
            Logger.s(TAG, e);
            return Collections.emptyList();
        }
    }
}
