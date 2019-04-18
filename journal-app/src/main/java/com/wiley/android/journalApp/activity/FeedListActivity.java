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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.base.ActivityWithActionBar;
import com.wiley.android.journalApp.fragment.feeds.FeedListFragment;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.domain.entity.FeedMO;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedListActivity extends ActivityWithActionBar {

    private static final String EXTRA_FEED_UID = "com.wiley.android.journalApp.activity.FeedListActivity_feed_uid";

    public static Intent getStartingIntent(Context ctx, String feedUid) {
        Intent intent = new Intent(ctx, FeedListActivity.class);
        intent.putExtra(EXTRA_FEED_UID, feedUid);
        return intent;
    }

    @Inject
    private HomePageService mHomePageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || intent.getStringExtra(EXTRA_FEED_UID) == null) {
            finish();
        }
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {

        final String feedUid = getIntent().getStringExtra(EXTRA_FEED_UID);
        final FeedMO feed = mHomePageService.getFeed(feedUid);
        if (feed == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_container);
        setTitle(feed.getTitle() + " - " + feed.getTitle());
        if (getSupportFragmentManager().findFragmentByTag("feed_list_fragment") == null) {
            Bundle args = new Bundle();
            args.putString(Extras.EXTRA_FEED_UID, feedUid);

            Fragment frag = new FeedListFragment();
            frag.setArguments(args);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, frag, "feed_list_fragment").commit();
            getSupportFragmentManager().executePendingTransactions();
        }
    }
}
