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
package com.wiley.wol.client.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.wiley.wol.client.android.domain.entity.AnnouncementMO;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleSpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.CitationMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.ReferenceMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.SupportingInfoMO;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.inject.InjectDatabasePath;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.settings.LastModified;

import java.sql.SQLException;

import static com.j256.ormlite.table.TableUtils.createTable;

public class JournalAppDatabase extends OrmLiteSqliteOpenHelper {

    private static final String TAG = JournalAppDatabase.class.getSimpleName();

    private static final int DB_INITIAL_VERSION = 2;
    private static final int DB_VERSION = DB_INITIAL_VERSION;

    @Inject
    public JournalAppDatabase(final Context context, final @InjectDatabasePath String databasePath) {
        super(context, databasePath, null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase sqLiteDatabase, final ConnectionSource connectionSource) {
        try {
            createTable(connectionSource, IssueMO.class);
            createTable(connectionSource, SectionMO.class);
            createTable(connectionSource, ArticleMO.class);
            createTable(connectionSource, FigureMO.class);
            createTable(connectionSource, ReferenceMO.class);
            createTable(connectionSource, CitationMO.class);
            createTable(connectionSource, SupportingInfoMO.class);
            createTable(connectionSource, SpecialSectionMO.class);
            createTable(connectionSource, TPSSiteMO.class);
            createTable(connectionSource, ArticleSpecialSectionMO.class);
            createTable(connectionSource, LastModified.class);

            createTable(connectionSource, FeedMO.class);
            createTable(connectionSource, FeedItemMO.class);
            createTable(connectionSource, AnnouncementMO.class);
        } catch (final SQLException e) {
            Logger.s(TAG, "Error creating DB");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final ConnectionSource connectionSource,
                          final int oldVersion, final int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            try {
                createTable(connectionSource, FeedMO.class);
                createTable(connectionSource, FeedItemMO.class);
                createTable(connectionSource, AnnouncementMO.class);
            } catch (SQLException e) {
                Logger.s(TAG, "Error updating DB");
                throw new RuntimeException(e);
            }
        }
    }
}
