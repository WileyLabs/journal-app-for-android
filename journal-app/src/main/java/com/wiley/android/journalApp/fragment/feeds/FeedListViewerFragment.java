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

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.base.Society;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.settings.Settings;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedListViewerFragment extends JournalFragment implements
        ActionBarSherlock.OnCreatePanelMenuListener,
        ActionBarSherlock.OnMenuItemSelectedListener,
        Society {

    private static final String TAG = FeedListViewerFragment.class.getSimpleName();
    private static final String TAG_LIFE = FeedListViewerFragment.class.getSimpleName() + ".LIFE";

    private ViewPager viewPager;
    private List<String> feedUidList;
    private ActionBarSherlockCompat mSherlock;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    @Inject
    private HomePageService homePageService;

    @Inject
    private Settings settings;

    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;

    @Inject
    protected AANHelper aanHelper;

    private final Map<Integer, FeedListFragment> fragmentsMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_feed_list_viewer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getJournalActivity().getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        final Intent intent = getActivity().getIntent();
        feedUidList = intent.getStringArrayListExtra(Extras.EXTRA_FEED_LIST);
        final String initialFeedUid = intent.getStringExtra(Extras.EXTRA_FEED_UID);

        viewPager = findView(R.id.feed_list_viewer_pager);
        viewPager.setAdapter(new CustomPagerAdapter(getFragmentManager()));

        int initialIndex = 0;
        for (String feedUid : feedUidList) {
            if (feedUid.equals(initialFeedUid)) {
                break;
            }
            ++initialIndex;
        }

        viewPager.setCurrentItem(initialIndex);

        final FeedMO feed = homePageService.getFeed(initialFeedUid);
        aanHelper.trackSocietyListings(feed);

        // action bar
        mSherlock = ActionBarUtils.initActionBar(getJournalActivity(), feed.getTitle(), this, theme);

        // feature: quick link menu
        if (DeviceUtils.isPhone(((MainActivity) getJournalActivity()))) {
            // touch layout
            ((TouchRefreshLayout)findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    showQuickLinkMenu();
                }
            });

            quickLinkMenuComponent.initQuickLink(getActivity(), this);
        }


        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                final CharSequence pageTitle = viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem());
                mSherlock.setTitleActionBar(pageTitle);
                ((MainActivity) getActivity()).onSocietyPageNavigated();
                if (fragmentsMap.containsKey(i)) {
                    fragmentsMap.get(i).onShow();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        showSocietyLogo();
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

    /**
     *  feature: quick link menu
     */
    private void showQuickLinkMenu() {
        quickLinkMenuComponent.showQuickLinkMenu();
    }

    public FeedMO getCurrentFeed() {
        return homePageService.getFeed(feedUidList.get(viewPager.getCurrentItem()));
    }

    private class CustomPagerAdapter extends FragmentStatePagerAdapter {
        public CustomPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return feedUidList.size();
        }

        @Override
        public Fragment getItem(int position) {
            final Bundle args = new Bundle();
            args.putString(Extras.EXTRA_FEED_UID, feedUidList.get(position));

            final FeedListFragment feedListFragment = new FeedListFragment();
            feedListFragment.setArguments(args);

            fragmentsMap.put(position, feedListFragment);

            return feedListFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return homePageService.getFeed(feedUidList.get(position)).getTitle();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            fragmentsMap.remove(position);
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {

        if (DeviceUtils.isPhone(this.getActivity())) {
            menu.add(getString(R.string.action_show_menu))
                    .setIcon(ActionBarUtils.getMenuIconResource(theme))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ((MainActivity) getJournalActivity()).onSideMenuButtonClicked();
                            return true;
                        }
                    });
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            MainActivity mainActivity = (MainActivity) getJournalActivity();
            mainActivity.onBackPressed();
            return true;
        }

        return false;
    }

    private void showSocietyLogo() {
        final String societyFooterLogoImageUrl = settings.getSocietyFooterLogoImageUrl();
        if (societyFooterLogoImageUrl != null) {
            final ImageView societyLogo = findView(R.id.society_logo);
            String logoImageUrl = Uri.fromFile(new File(societyFooterLogoImageUrl)).toString();
            imageLoader.displayImage(logoImageUrl, societyLogo);
        }
    }
}
