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
package com.wiley.android.journalApp.fragment.feeds;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.base.Society;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.BundleUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.log.Logger;

import java.util.List;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedItemsViewerFragment
        extends
        JournalFragment
        implements
        ActionBarSherlock.OnCreatePanelMenuListener,
        ActionBarSherlock.OnMenuItemSelectedListener,
        Society {

    @Inject
    private WebController webController;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;

    private static final String TAG_LIFE = FeedItemsViewerFragment.class.getSimpleName() + ".LIFE";

    public static FeedItemDetailsFragment newInstance(List<String> itemsUids, int startingIndex) {

        FeedItemDetailsFragment frag = new FeedItemDetailsFragment();

        Bundle args = new Bundle();
        BundleUtils.putListToBundle(args, "uids_list", itemsUids);
        args.putInt("startingItemUid", startingIndex);

        frag.setArguments(args);

        return frag;
    }

    @Inject
    private HomePageService homePageService;

    private List<String> mItemsUids;
    private int mInitialIndex;

    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;

    private ActionBarSherlockCompat mSherlock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItemsUids = BundleUtils.getListFromBundle(getJournalActivity().getIntent().getExtras(), Extras.EXTRA_FEED_ITEMS_LIST);

        if (mItemsUids == null || mItemsUids.isEmpty()) {
            throw new RuntimeException("FeedItemsViewerFragment: item list is empty!");
        }

        mInitialIndex = getJournalActivity().getIntent().getIntExtra(Extras.EXTRA_INITIAL_FEED_ITEM_INDEX, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onCreateView()");
        return inflater.inflate(R.layout.frag_feed_items_viewer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        initContentView(savedInstanceState);
    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();

        // feature: quick link menu
        showQuickLinkMenu();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Logger.d(TAG_LIFE, "onHiddenChanged(): hidden = " + hidden);
        super.onHiddenChanged(hidden);

        // feature: quick link menu
        if (!hidden) {
            showQuickLinkMenu();
        }
    }

    @Override
    public void onDestroyView() {
        Logger.d(TAG_LIFE, "onDestroyView()");
        super.onDestroyView();
    }

    private void initContentView(Bundle savedInstanceState) {
        getJournalActivity().getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        mViewPager = findView(R.id.feed_items_viewer_pager);
        mPagerAdapter = new CustomPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(pagerListener);

        // action bar
        mSherlock = ActionBarUtils.initActionBar(getJournalActivity(), "", this, theme);

        // feature: quick link menu
        if (DeviceUtils.isPhone((getJournalActivity()))) {
            // touch layout
            ((TouchRefreshLayout) findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    showQuickLinkMenu();
                }
            });

            quickLinkMenuComponent.initQuickLink(getActivity(), this);
        }


        mViewPager.setCurrentItem(mInitialIndex);

        updateUi();
    }

    private void updateUi() {

        // actionBar: title
        CharSequence pageTitle = mViewPager.getAdapter().getPageTitle(mViewPager.getCurrentItem());
        mSherlock.setTitleActionBar(pageTitle);
    }

    /**
     * feature: quick link menu
     */
    private void showQuickLinkMenu() {
        quickLinkMenuComponent.showQuickLinkMenu();
    }

    private class CustomPagerAdapter extends FragmentStatePagerAdapter {

        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return FeedItemDetailsFragment.newInstance(mItemsUids.get(position));
        }

        @Override
        public int getCount() {
            return mItemsUids.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return homePageService.getFeedItem(mItemsUids.get(position)).getTitle();//figures.get(position).getTitle();
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        ActionBarUtils.inflateFeedItemMenu(mSherlock, menu, theme);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            MainActivity mainActivity = (MainActivity) getJournalActivity();
            mainActivity.onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.feed_item_open_in_browser) {
            webController.openUrlExternal(homePageService.getFeedItem(mItemsUids.get(mViewPager.getCurrentItem())).getUrl());
        }

        return false;
    }

    private ViewPager.OnPageChangeListener pagerListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            updateUi();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };
}
