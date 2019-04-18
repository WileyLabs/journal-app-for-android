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
package com.wiley.android.journalApp.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.popup.MainMenuWindow;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

public class SocietyNavigationPanel extends JournalFragment {
    private ToggleButton societyNewsButton;
    private ToggleButton journalButton;
    private MainMenuWindow mainMenuWindow;
    private boolean isSocietyFeedsUpdating = false;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ArticleService articleService;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private HomePageService homePageService;
    @Inject
    private Settings settings;

    private NotificationProcessor earlyViewChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateMenuItems();
        }
    };

    private NotificationProcessor specialSectionsChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateMenuItems();
        }
    };

    private final NotificationProcessor homeFeedUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            isSocietyFeedsUpdating = false;
            updateSocietyFeeds();
        }
    };
    private final NotificationProcessor rssFeedUpdateCompletedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (!isSocietyFeedsUpdating) {
                isSocietyFeedsUpdating = true;
                updateSocietyFeeds();
            }
        }
    };
    private final NotificationProcessor menuItemCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            isSocietyFeedsUpdating = false;
        }
    };

    private NotificationProcessor savedArticlesListChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateSavedArticlesButtonText();
        }
    };

    private NotificationProcessor societyFavoritesCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateSocietyFavoritesMenuItem();
        }
    };

    public void setSocietyNewsTitle(final String title) {
        societyNewsButton.setText(title);
        societyNewsButton.setTextOn(title);
        societyNewsButton.setTextOff(title);
        mainMenuWindow.setSocietyTitle(title);
    }

    private void updateSocietyFavoritesMenuItem() {
        final long favoritesCount = homePageService.getFavoriteItemsCount();
        final String title = String.format(getString(R.string.society_favorites_menu_item_format), favoritesCount);
        mainMenuWindow.setSocietyFavoritesItemTitle(title);
    }

    private void updateSavedArticlesButtonText() {
        final long savedCount = articleService.getSavedArticleCount();
        final String savedCountStr = String.format(getString(R.string.saved_articles_tab_label_format), savedCount);
        mainMenuWindow.setJournalSavedArticlesTitle(savedCountStr);
    }

    private void updateSocietyFeeds() {
        Collection<FeedMO> feeds = homePageService.getFeeds();
        mainMenuWindow.addSocietyFeeds(feeds);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.society_journal_panel, container, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mainMenuWindow.dismiss();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button menuButton = findView(R.id.menu_button);
        societyNewsButton = findView(R.id.society_button);
        journalButton = findView(R.id.journal_button);

        journalButton.setChecked(true);
        journalButton.setClickable(false);
        societyNewsButton.setChecked(false);

        mainMenuWindow = new MainMenuWindow(this.getActivity(), notificationCenter);
        mainMenuWindow.setOnDismissListener(new MainMenuWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                ((MainActivity) getActivity()).undim();
            }
        });
        updateSavedArticlesButtonText();
        updateSocietyFavoritesMenuItem();
        updateSocietyFeeds();

        societyNewsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    journalButton.setChecked(false);
                    societyNewsButton.setClickable(false);
                } else {
                    societyNewsButton.setClickable(true);
                    return;
                }

                MainActivity activity = (MainActivity) getActivity();
                activity.showSocietyNews();
            }
        });

        journalButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    societyNewsButton.setChecked(false);
                    journalButton.setClickable(false);
                } else {
                    journalButton.setClickable(true);
                    return;
                }
                MainActivity activity = (MainActivity) getActivity();
                activity.showJournal();
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationCenter.sendNotification(EventList.MENU_BUTTON_IS_SHOWN.getEventName());
                ((MainActivity) getActivity()).dim(100);
                mainMenuWindow.show(v);
            }
        });

        updateMenuItems();
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(EventList.EARLY_VIEW_FEED_UPDATED.getEventName(), earlyViewChanged);
        notificationCenter.subscribeToNotification(EventList.SPECIAL_SECTIONS_UPDATED.getEventName(), specialSectionsChanged);
        notificationCenter.subscribeToNotification(EventList.HOME_PAGE_FEEDS_UPDATE_FINISHED.getEventName(), homeFeedUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(), rssFeedUpdateCompletedProcessor);
        notificationCenter.subscribeToNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName(), menuItemCountChangedProcessor);
        notificationCenter.subscribeToNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), savedArticlesListChanged);
        notificationCenter.subscribeToNotification(EventList.SOCIETY_FAVORITES_COUNT_CHANGED.getEventName(), societyFavoritesCountChangedProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(earlyViewChanged);
        notificationCenter.unSubscribeFromNotification(specialSectionsChanged);
        notificationCenter.unSubscribeFromNotification(homeFeedUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(rssFeedUpdateCompletedProcessor);
        notificationCenter.unSubscribeFromNotification(menuItemCountChangedProcessor);
        notificationCenter.unSubscribeFromNotification(savedArticlesListChanged);
        notificationCenter.unSubscribeFromNotification(societyFavoritesCountChangedProcessor);
    }

    private void updateMenuItems() {
        boolean needShowEarlyView = articleService.hasArticlesForEarlyView();
        if (needShowEarlyView) {
            mainMenuWindow.addEarlyViewItem();
        } else {
            mainMenuWindow.removeEarlyViewItem();
        }

        boolean needShowSpecialSections = specialSectionService.hasSpecialSections();
        if (needShowSpecialSections) {
            mainMenuWindow.addSpecialSectionsItem();
        } else {
            mainMenuWindow.removeSpecialSectionsItem();
        }
    }

    public boolean isSocietyNewsTabActive() {
        return societyNewsButton.isChecked();
    }

    public void navigateToJournal() {
        journalButton.performClick();
    }

    public void navigateToSocietyNews() {
        societyNewsButton.performClick();
    }
}
