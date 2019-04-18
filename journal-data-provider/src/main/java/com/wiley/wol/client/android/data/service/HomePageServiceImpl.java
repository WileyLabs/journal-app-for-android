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
import com.j256.ormlite.stmt.UpdateBuilder;
import com.wiley.wol.client.android.data.dao.filter.FilterFactory;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.BaseRssMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.wiley.wol.client.android.domain.entity.FeedItemMO.ADDED_TO_FAVORITES_DATE_COLUMN;
import static com.wiley.wol.client.android.domain.entity.FeedItemMO.FAVORITE_COLUMN;
import static java.lang.String.format;

/**
 * Created by taraskreknin
 * on 02.10.14.
 */
public class HomePageServiceImpl implements HomePageService {
    private static final String TAG = HomePageServiceImpl.class.getSimpleName();
    public static final String IN_FEED_DATE_COLUMN = "in_feed_date";
    private final Dao<FeedMO, String> feedDao;
    private final Dao<FeedItemMO, String> feedItemDao;

    @Inject
    private ImportManager mImportManager;
    @Inject
    private FilterFactory filterFactory;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    protected AANHelper aanHelper;

    @Inject
    public HomePageServiceImpl(final OrmLiteSqliteOpenHelper helper) {
        feedDao = new DaoProvider<>(helper, FeedMO.class, String.class).get();
        feedItemDao = new DaoProvider<>(helper, FeedItemMO.class, String.class).get();
    }

    @Override
    public Collection<FeedMO> getFeeds() {
        try {
            final QueryBuilder<FeedMO, String> feedQueryBuilder = feedDao.queryBuilder();
            feedQueryBuilder.orderBy(BaseRssMO.SORT_INDEX, true);
            return feedQueryBuilder.query();
        } catch (SQLException e) {
            Logger.s(TAG, e);
            return Collections.emptyList();
        }
    }

    @Override
    public FeedItemMO getFeedItem(String uid) {
        try {
            return feedItemDao.queryForId(uid);
        } catch (SQLException e) {
            Logger.s(TAG, format("get feedItem by uid='%s'", uid), e);
            return null;
        }
    }

    @Override
    public List<FeedItemMO> getFeedItems(String feedUid) {
        return new ArrayList<>(getFeed(feedUid).getItems());
    }

    @Override
    public void update() {
        mImportManager.updateHomePageFeed();
    }

    @Override
    public void updateFeed(FeedMO feed) {
        mImportManager.updateRssFeed(feed);
    }

    @Override
    public FeedMO getFeed(String uid) {
        try {
            if (uid == null) {
                return null;
            }

            return feedDao.queryForId(uid);
        } catch (SQLException e) {
            Logger.s(TAG, format("get feed by uid='%s' failed", uid), e);
            return null;
        }
    }

    @Override
    public void saveFeed(FeedMO feed) {
        try {
            Date currentDate = new Date();
            if (feed.getItems() != null) {
                for (FeedItemMO feedItem : feed.getItems()) {
                    feedItem.setInFeedDate(currentDate);
                    createOrUpdateFeedItem(feedItem);
                }
            }
            deleteOldFeedItems(feed, currentDate);
            feedDao.createOrUpdate(feed);
        } catch (SQLException e) {
            Logger.s(TAG, "save feed failed", e);
        }
    }

    @Override
    public void deleteOldFeeds(Date currentDate) {
        QueryBuilder<FeedMO, String> feedQueryBuilder = feedDao.queryBuilder();
        List<FeedMO> oldFeeds;
        try {
            feedQueryBuilder.where().lt(IN_FEED_DATE_COLUMN, currentDate);
            oldFeeds = feedQueryBuilder.query();
        } catch (SQLException e) {
            Logger.s(TAG, e);
            return;
        }

        for (FeedMO feedForDeleting : oldFeeds) {
            deleteFeed(feedForDeleting);
        }
    }

    private void deleteOldFeedItems(FeedMO feed, Date currentDate) throws SQLException {
        DeleteBuilder<FeedItemMO, String> feedItemDeleteBuilder = feedItemDao.deleteBuilder();
        feedItemDeleteBuilder.where()
                .lt(IN_FEED_DATE_COLUMN, currentDate)
                .and()
                .eq(FAVORITE_COLUMN, false)
                .and()
                .eq("feed_uid", feed.getUid());
        feedItemDeleteBuilder.delete();
    }

    private void createOrUpdateFeedItem(FeedItemMO feedItem) throws SQLException {
        QueryBuilder<FeedItemMO, String> feedItemQueryBuilder = feedItemDao.queryBuilder();
        feedItemQueryBuilder.selectColumns(FAVORITE_COLUMN, ADDED_TO_FAVORITES_DATE_COLUMN).where().eq("uid", feedItem.getUid());
        FeedItemMO existingFeedItem = feedItemQueryBuilder.queryForFirst();
        if (existingFeedItem != null) {
            feedItem.setFavorite(existingFeedItem.isFavorite());
            feedItem.setAddedToFavoritesDate(existingFeedItem.getAddedToFavoritesDate());
            feedItemDao.update(feedItem);
        } else {
            feedItemDao.create(feedItem);
        }
    }

    @Override
    public void deleteFeed(FeedMO feed) {
        try {
            deleteFeedItemsForFeed(feed);
            feedDao.delete(feed);
        } catch (SQLException e) {
            Logger.s(TAG, "delete feed failed", e);
        }
    }

    private void deleteFeedItemsForFeed(FeedMO feed) throws SQLException {
        DeleteBuilder<FeedItemMO, String> feedItemDeleteBuilder = feedItemDao.deleteBuilder();
        feedItemDeleteBuilder.where().eq("feed_uid", feed.getUid());
        feedItemDeleteBuilder.delete();
    }

    @Override
    public void updateFeedItemContent(FeedItemMO feedItem) {
        mImportManager.updateFeedItemContent(feedItem);
    }

    @Override
    public void updateItem(FeedItemMO item) {
        try {
            feedItemDao.update(item);
        } catch (SQLException e) {
            Logger.s(TAG, "update feedItem failed", e);
        }
    }

    @Override
    public long getFavoriteItemsCount() {
        try {
            return feedItemDao.countOf(filterFactory.getFor(FeedItemMO.class)
                    .where()
                    .eq(FAVORITE_COLUMN, true)
                    .countOf()
                    .getOrmSpecificRequest());
        } catch (SQLException | ElementNotFoundException e) {
            Logger.s(TAG, "get favorite items count failed", e);
            return 0;
        }
    }

    @Override
    public List<FeedItemMO> getFavoriteItems() {
        try {
            return feedItemDao.queryForEq(FAVORITE_COLUMN, true);
        } catch (SQLException e) {
            Logger.s(TAG, "get favorite items count failed", e);
            return Collections.emptyList();
        }

    }

    @Override
    public void updateFavoriteState(String uid, boolean favoriteState) {
        UpdateBuilder<FeedItemMO, String> feedItemUpdateBuilder = feedItemDao.updateBuilder();
        try {
            feedItemUpdateBuilder
                    .where()
                    .eq("uid", uid);

            feedItemUpdateBuilder.updateColumnValue(FAVORITE_COLUMN, favoriteState);
            feedItemUpdateBuilder.updateColumnValue(ADDED_TO_FAVORITES_DATE_COLUMN, new Date());

            feedItemUpdateBuilder.update();

            notificationCenter.sendNotification(EventList.SOCIETY_FAVORITES_COUNT_CHANGED.getEventName(), new ParamsBuilder().withUid(uid).withParam("favoriteState", favoriteState).get());

            final FeedItemMO feedItem = getFeedItem(uid);
            if (favoriteState) {
                aanHelper.trackActionSaveFeedItem(feedItem);
            } else {
                aanHelper.trackActionRemoveFeedItem(feedItem);
            }
        } catch (SQLException e) {
            Logger.s(TAG, "Unable to update favorite state", e);
        }
    }
}
