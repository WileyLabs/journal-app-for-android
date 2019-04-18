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
package com.wiley.android.journalApp.components;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ExpandableListView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.adapter.MainMenuExpandableListAdapter;
import com.wiley.android.journalApp.adapter.MainMenuGroup;
import com.wiley.android.journalApp.adapter.MainMenuItem;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.popup.PopupWindows;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wiley.wol.client.android.notification.NotificationCenter.FEED_MO;
import static com.wiley.wol.client.android.notification.NotificationCenter.IS_GLOBAL_SEARCH_SHOWN;
import static com.wiley.wol.client.android.notification.NotificationCenter.IS_HOME_SHOWN;
import static com.wiley.wol.client.android.notification.NotificationCenter.IS_SOCIETY_FAVORITES_SHOWN;

public class MainMenu {
    private static final String EARLY_VIEW_ITEM_NAME = "Early View";
    private static final String SPECIAL_SECTIONS_ITEM_NAME = "Special Sections";

    private static final int SOCIETY_GROUP_POSITION = 0;

    public static final String GLOBAL_SEARCH = "Search";
    public static final String SAVED_ARTICLES = "Saved Articles";
    public static final String SOCIETY_FAVORITES = "Society Favorites";
    public static final String SOCIETY_HOME = "Home";

    private Context context;
    private MainMenuExpandableListAdapter adapter;
    private ExpandableListView menuListView;
    private View rootView;
    private int lastExpandedPosition = 0;
    private PopupWindows window;
    private NotificationCenter notificationCenter;

    private int highlightedGroupPosition = -1;
    private int highlightedChildPosition = -1;

    private MainMenuGroup societyGroup;

    private Map<Integer, Integer> journalTabIdToImageResourceIdMap = new HashMap<>();
    private Map<Integer, Integer> infoTabIdToImageResourceIdMap = new HashMap<>();

    {
        journalTabIdToImageResourceIdMap.put(R.id.early_view_tab, R.drawable.early_view_small);
        journalTabIdToImageResourceIdMap.put(R.id.issues_tab, R.drawable.issues_small);
        journalTabIdToImageResourceIdMap.put(R.id.special_sections_tab, R.drawable.special_sections_small);
        journalTabIdToImageResourceIdMap.put(R.id.saved_articles_tab, R.drawable.saved_article_small);
        journalTabIdToImageResourceIdMap.put(R.id.global_search_tab, R.drawable.global_search_small);

        infoTabIdToImageResourceIdMap.put(R.id.settings_tab, R.drawable.settings_small);
        infoTabIdToImageResourceIdMap.put(R.id.info_tab, R.drawable.about_small);
    }

    private NotificationProcessor journalPageNavigatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final int currentTabId = (int) params.get(NotificationCenter.CURRENT_TAB_ID);
            if (journalTabIdToImageResourceIdMap.containsKey(currentTabId)) {
                MainMenuGroup group = (MainMenuGroup) adapter.getGroup(getJournalGroupPosition());
                int resourceId = journalTabIdToImageResourceIdMap.get(currentTabId);
                int childPosition = findChildItemBuResourceId(group, resourceId);
                expandGroup(getJournalGroupPosition());
                setChildItemChecked(getJournalGroupPosition(), childPosition);
            } else {
                MainMenuGroup group = (MainMenuGroup) adapter.getGroup(getInfoGroupPosition());
                int resourceId = infoTabIdToImageResourceIdMap.get(currentTabId);
                int childPosition = findChildItemBuResourceId(group, resourceId);
                expandGroup(getInfoGroupPosition());
                setChildItemChecked(getInfoGroupPosition(), childPosition);
            }
        }
    };

    private NotificationProcessor societyPageNavigatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            boolean isHomeShown = params.containsKey(IS_HOME_SHOWN) && (boolean) params.get(IS_HOME_SHOWN);
            boolean isSocietyFavoritesShown = params.containsKey(IS_SOCIETY_FAVORITES_SHOWN)
                    && (boolean) params.get(IS_SOCIETY_FAVORITES_SHOWN);
            boolean isGlobalSearchShown = params.containsKey(IS_GLOBAL_SEARCH_SHOWN)
                    && (boolean) params.get(IS_GLOBAL_SEARCH_SHOWN);

            if (isHomeShown) {
                expandGroup(SOCIETY_GROUP_POSITION);
                setChildItemChecked(SOCIETY_GROUP_POSITION, 0);
            } else if (isSocietyFavoritesShown) {
                int childPosition = getChildPosition(SOCIETY_FAVORITES);
                expandGroup(SOCIETY_GROUP_POSITION);
                setChildItemChecked(SOCIETY_GROUP_POSITION, childPosition);
            } else if (isGlobalSearchShown) {
                int childPosition = getChildPosition(GLOBAL_SEARCH);
                expandGroup(SOCIETY_GROUP_POSITION);
                setChildItemChecked(SOCIETY_GROUP_POSITION, childPosition);
            } else {
                FeedMO feed = (FeedMO) params.get(FEED_MO);
                if (feed == null) {
                    return;
                }

                MainMenuGroup group = (MainMenuGroup) adapter.getGroup(SOCIETY_GROUP_POSITION);
                int childPosition = 0;
                for (MainMenuItem menuItem : group.getItems()) {
                    if (feed.getUid().equals(menuItem.getUniqueId())) {
                        break;
                    }
                    childPosition++;
                }
                expandGroup(SOCIETY_GROUP_POSITION);
                setChildItemChecked(SOCIETY_GROUP_POSITION, childPosition);
            }
        }

        private int getChildPosition(String title) {
            MainMenuGroup group = (MainMenuGroup) adapter.getGroup(SOCIETY_GROUP_POSITION);
            int childPosition = 0;
            for (MainMenuItem menuItem : group.getItems()) {
                if (menuItem.getTitle().contains(title)) {
                    break;
                }
                childPosition++;
            }
            return childPosition;
        }
    };

    public MainMenu(Context context, View rootView, NotificationCenter notificationCenter) {
        this.context = context;
        this.rootView = rootView;
        this.notificationCenter = notificationCenter;
        initializeMenu();
    }

    public MainMenu(Context context, View rootView, PopupWindows window, NotificationCenter notificationCenter) {
        this.context = context;
        this.rootView = rootView;
        this.window = window;
        this.notificationCenter = notificationCenter;
        initializeMenu();
    }

    private void initializeMenu() {
        adapter = new MainMenuExpandableListAdapter(context, new HighlightMenuHelper());

        societyGroup = new MainMenuGroup();
        societyGroup.setTitle(context.getString(R.string.society_news));
        societyGroup.addMenuItem(0, new MainMenuItem(SOCIETY_HOME, R.drawable.home_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToSocietyHome();
            }
        }));
        societyGroup.addMenuItem(1, new MainMenuItem(SOCIETY_FAVORITES, R.drawable.saved_article_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToSocietyFavorites();
            }
        }));
        societyGroup.addMenuItem(2, new MainMenuItem(GLOBAL_SEARCH, R.drawable.global_search_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToSocietyGlobalSearch();
            }
        }));

        MainMenuGroup journalGroup = new MainMenuGroup();
        journalGroup.setTitle("Journal");
        journalGroup.addMenuItem(0, new MainMenuItem("Issues", R.drawable.issues_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToJournalIssues();
            }
        }));
        journalGroup.addMenuItem(1, new MainMenuItem(SAVED_ARTICLES, R.drawable.saved_article_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToJournalSavedArticles();
            }
        }));
        journalGroup.addMenuItem(2, new MainMenuItem(GLOBAL_SEARCH, R.drawable.global_search_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToJournalGlobalSearch();
            }
        }));

        MainMenuGroup settingGroup = new MainMenuGroup();
        settingGroup.setTitle("Settings and About");
        settingGroup.addMenuItem(0, new MainMenuItem("Settings", R.drawable.settings_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToJournalSettings();
            }
        }));
        settingGroup.addMenuItem(1, new MainMenuItem("About", R.drawable.about_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToJournalAbout();
            }
        }));

        adapter.addGroup(0, societyGroup);
        adapter.addGroup(1, journalGroup);
        adapter.addGroup(2, settingGroup);
        menuListView = (ExpandableListView) rootView.findViewById(R.id.main_menu_list);
        menuListView.setAdapter(adapter);

        menuListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (groupPosition != lastExpandedPosition) {
                    menuListView.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;

                if (groupPosition == highlightedGroupPosition) {
                    setChildItemChecked(highlightedGroupPosition, highlightedChildPosition);
                }
            }
        });

        menuListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                ((MainMenuItem) adapter.getChild(groupPosition, childPosition))
                        .getOnClickAction()
                        .onItemClick();

                if (window != null) {
                    window.dismiss();
                }

                setChildItemChecked(groupPosition, childPosition);

                return true;
            }
        });

        menuListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (menuListView.isGroupExpanded(highlightedGroupPosition)) {
                                setChildItemChecked(highlightedGroupPosition, highlightedChildPosition);
                            }
                        }
                    }, 200);
                }

                return false;
            }
        });

        menuListView.expandGroup(lastExpandedPosition);

        notificationCenter.subscribeToNotification(EventList.JOURNAL_PAGE_NAVIGATED.getEventName(), journalPageNavigatedProcessor);
        notificationCenter.subscribeToNotification(EventList.SOCIETY_PAGE_NAVIGATED.getEventName(), societyPageNavigatedProcessor);
    }

    public void setSocietyTitle(final String title) {
        societyGroup.setTitle(title);
        adapter.notifyDataSetChanged();
    }

    public void highlightCurrentMenuItem() {
        highlightItem(highlightedGroupPosition, highlightedChildPosition);
    }

    private void setChildItemChecked(int groupPosition, int childPosition) {
        highlightedGroupPosition = groupPosition;
        highlightedChildPosition = childPosition;
        highlightItem(groupPosition, childPosition);
    }

    private void highlightItem(int groupPosition, int childPosition) {
        try {
            int index = menuListView.getFlatListPosition(ExpandableListView
                    .getPackedPositionForChild(groupPosition, childPosition));
            menuListView.setItemChecked(index, true);
        } catch (Exception e) {
            Logger.s("MainMenu", e);
        }
    }

    public void addEarlyViewItem() {
        final MainMenuGroup journalGroup = getJournalGroup();

        journalGroup.addMenuItem(0, new MainMenuItem(EARLY_VIEW_ITEM_NAME, R.drawable.early_view_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToJournalEarlyView();
            }
        }));

        adapter.notifyDataSetChanged();
        notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
    }

    public void removeEarlyViewItem() {
        final MainMenuGroup journalGroup = getJournalGroup();
        journalGroup.removeItem(EARLY_VIEW_ITEM_NAME);
        adapter.notifyDataSetChanged();
        notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
    }

    public void hideSocietyGroup() {
        adapter.removeGroup(societyGroup);
        adapter.notifyDataSetChanged();
        notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
    }

    public void showSocietyGroup() {
        if (!adapter.isGroupExists(societyGroup)) {
            adapter.addGroup(SOCIETY_GROUP_POSITION, societyGroup);
            adapter.notifyDataSetChanged();
            notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
        }
    }

    public void addSpecialSectionsItem() {
        int position = 2;
        final MainMenuGroup journalGroup = getJournalGroup();
        List<MainMenuItem> items = journalGroup.getItems();
        if (!EARLY_VIEW_ITEM_NAME.equals(items.get(0).getTitle())) {
            position = 1;
        }

        journalGroup.addMenuItem(position, new MainMenuItem(SPECIAL_SECTIONS_ITEM_NAME, R.drawable.special_sections_small, new MainMenuItem.OnClickAction() {
            @Override
            public void onItemClick() {
                ((MainActivity) context).navigateToSpecialSection();
            }
        }));
        adapter.notifyDataSetChanged();
        notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
    }

    public void removeSpecialSectionsItem() {
        final MainMenuGroup journalGroup = getJournalGroup();
        journalGroup.removeItem(SPECIAL_SECTIONS_ITEM_NAME);
        adapter.notifyDataSetChanged();
        notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
    }

    public void addSocietyFeeds(Collection<FeedMO> feeds) {
        int itemPosition = 1;
        final MainMenuGroup societyGroup = getSocietyGroup();
        societyGroup.clearFeedItems();
        for (final FeedMO feed : feeds) {
            boolean added = societyGroup.addMenuItem(itemPosition, new MainMenuItem(getFeedTitle(feed), feed.getUid(),
                    null, new MainMenuItem.OnClickAction() {
                @Override
                public void onItemClick() {
                    ((MainActivity) context).navigateToSocietyFeed(feed);
                }
            }));
            if (added) {
                itemPosition++;
            }
        }
        adapter.notifyDataSetChanged();
        notificationCenter.sendNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName());
    }

    public void setJournalSavedArticlesTitle(final String title) {
        changeMenuItemTitle(title, SAVED_ARTICLES, getJournalGroupPosition());
    }

    public void setSocietyFavoritesItemTitle(String title) {
        changeMenuItemTitle(title, SOCIETY_FAVORITES, SOCIETY_GROUP_POSITION);
    }

    public void changeMenuItemTitle(String title, String oldTitleStartsWith, int groupPosition) {
        final MainMenuGroup journalGroup = (MainMenuGroup) adapter.getGroup(groupPosition);
        for (MainMenuItem menuItem : journalGroup.getItems()) {
            if (menuItem.getTitle().startsWith(oldTitleStartsWith)) {
                menuItem.setTitle(title);
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    private String getFeedTitle(FeedMO feed) {
        if (feed.getTitle() != null && feed.getTitle().length() > 20) {
            return feed.getTitle().substring(0, 20) + "...";
        }
        return feed.getTitle() != null ? feed.getTitle() : "";
    }

    private MainMenuGroup getJournalGroup() {
        return (MainMenuGroup) adapter.getGroup(getJournalGroupPosition());
    }

    private MainMenuGroup getSocietyGroup() {
        return societyGroup;
    }

    private void expandGroup(int groupPosition) {
        menuListView.collapseGroup(lastExpandedPosition);
        menuListView.expandGroup(groupPosition);
        lastExpandedPosition = groupPosition;
    }

    private int findChildItemBuResourceId(MainMenuGroup group, int resourceId) {
        int childPosition = 0;
        for (MainMenuItem item : group.getItems()) {
            if (item.getImageResourceId() == resourceId) {
                break;
            }
            childPosition++;
        }
        return childPosition;
    }

    public void expandHighlightedGroup() {
        if (highlightedGroupPosition >= 0) {
            expandGroup(highlightedGroupPosition);
        }
    }

    public boolean isJournalShown() {
        return getJournalGroupPosition() <= highlightedGroupPosition;
    }

    private int getJournalGroupPosition() {
        if (adapter.isGroupExists(societyGroup)) {
            return 1;
        } else {
            return 0;
        }
    }

    private int getInfoGroupPosition() {
        if (adapter.isGroupExists(societyGroup)) {
            return 2;
        } else {
            return 1;
        }
    }

    public void highlightSocietyFavoriteMenuItem() {
        int societyFavoriteItemPosition = getItemPosition(SOCIETY_GROUP_POSITION, SOCIETY_FAVORITES);
        highlightItem(SOCIETY_GROUP_POSITION, societyFavoriteItemPosition);
    }

    public void highlightJournalFavoriteMenuItem() {
        final int journalGroupPosition = getJournalGroupPosition();
        int savedArticlesItemPosition = getItemPosition(journalGroupPosition, SAVED_ARTICLES);
        highlightItem(journalGroupPosition, savedArticlesItemPosition);
    }

    private int getItemPosition(int groupPosition, String itemTitle) {
        int societyFavoriteItemPosition = 0;
        final MainMenuGroup journalGroup = (MainMenuGroup) adapter.getGroup(groupPosition);
        for (MainMenuItem menuItem : journalGroup.getItems()) {
            if (menuItem.getTitle().startsWith(itemTitle)) {
                break;
            }
            ++societyFavoriteItemPosition;
        }
        return societyFavoriteItemPosition;
    }

    public final class HighlightMenuHelper {
        public void highlightMenuItem(int groupPosition, int itemPosition) {
            if (groupPosition == highlightedGroupPosition) {
                highlightItem(groupPosition, itemPosition);
            }
        }
    }
}
