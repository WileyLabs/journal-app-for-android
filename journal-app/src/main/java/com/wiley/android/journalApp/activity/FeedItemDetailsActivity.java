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
package com.wiley.android.journalApp.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.base.ActivityWithActionBar;
import com.wiley.android.journalApp.fragment.feeds.FeedItemsViewerFragment;
import com.wiley.android.journalApp.utils.BundleUtils;

import java.util.List;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedItemDetailsActivity extends ActivityWithActionBar {

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        List<String> itemsList = BundleUtils.getListFromBundle(getIntent().getExtras(), Extras.EXTRA_FEED_ITEMS_LIST);

        if (itemsList == null || itemsList.isEmpty()) {
            finish();
            return;
        }

        int initialIndex = getIntent().getIntExtra(Extras.EXTRA_INITIAL_FEED_ITEM_INDEX, 0);

        setContentView(R.layout.activity_container);

        if (getSupportFragmentManager().findFragmentByTag("feed_items_viewer_fragment") == null) {

            Fragment frag = FeedItemsViewerFragment.newInstance(itemsList, initialIndex);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, frag, "feed_list_fragment").commit();
            getSupportFragmentManager().executePendingTransactions();
        }

    }

}
