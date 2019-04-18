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

import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by taraskreknin on 02.10.14.
 */
public interface HomePageService {

    Collection<FeedMO> getFeeds();

    FeedItemMO getFeedItem(String uid);

    List<FeedItemMO> getFeedItems(String feedUid);

    void update();

    void updateFeed(FeedMO feed);

    FeedMO getFeed(String uid);

    void saveFeed(FeedMO feed);

    void deleteOldFeeds(Date currentDate);

    void deleteFeed(FeedMO feed);

    void updateFeedItemContent(FeedItemMO feedItem);

    void updateItem(FeedItemMO item);

    long getFavoriteItemsCount();

    List<FeedItemMO> getFavoriteItems();

    void updateFavoriteState(String uid, boolean favoriteState);
}
