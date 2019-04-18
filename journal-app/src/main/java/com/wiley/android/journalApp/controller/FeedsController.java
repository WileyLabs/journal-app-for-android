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
package com.wiley.android.journalApp.controller;

import android.content.Context;
import android.content.Intent;

import com.google.inject.Inject;
import com.wiley.android.journalApp.activity.FeedItemDetailsActivity;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.utils.BundleUtils;

import java.util.List;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedsController {

    private final Context mContext;

    @Inject
    public FeedsController(final Context context) {
        mContext = context;
    }

    public void openFeedItems(List<String> itemsUids) {
        openFeedItems(itemsUids, 0);
    }

    public void openFeedItems(List<String> itemsUids, int startingIndex) {

        Intent intent = new Intent(mContext, FeedItemDetailsActivity.class);
        BundleUtils.putListToIntent(intent, Extras.EXTRA_FEED_ITEMS_LIST, itemsUids);
        intent.putExtra(Extras.EXTRA_INITIAL_FEED_ITEM_INDEX, startingIndex);

        mContext.startActivity(intent);
    }

}
