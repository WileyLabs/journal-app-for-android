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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.MainMenu;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.fragment.settings.SettingsFragment;
import com.wiley.android.journalApp.fragment.tabs.BaseTabFragment;
import com.wiley.android.journalApp.fragment.tabs.EarlyViewContent;
import com.wiley.android.journalApp.fragment.tabs.GlobalSearchFragment;
import com.wiley.android.journalApp.fragment.tabs.InfoFragment;
import com.wiley.android.journalApp.fragment.tabs.IssuesContent;
import com.wiley.android.journalApp.fragment.tabs.SavedArticlesFragment;
import com.wiley.android.journalApp.fragment.tabs.SpecialSectionsFragment;
import com.wiley.android.journalApp.progress.ProgressHandler;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.BitmapUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.http.UpdateManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JournalMainFragment
        extends
        JournalFragment
        implements
        ActionBarSherlock.OnCreatePanelMenuListener, Journal {
    private final static String TAG = JournalMainFragment.class.getSimpleName();
    private final static String TAG_LIFE = JournalMainFragment.class.getSimpleName() + ".life";

    private final static String State_CurrentTab = "currentTab";
    private final static int RequestCode_ShowCodeExpired = 800;

    private View earlyViewTab, issuesTab, specialSectionsTab, savedArticlesTab, globalSearchTab, settingsTab, infoTab;
    private final ArrayList<View> tabButtons = new ArrayList<View>();

    protected View mainMenu;

    private ProgressHandler progress;

    private final Map<Integer, TabInfo> tabInfoMap = new HashMap<>();
    private TabInfo currentTab;

    private ActionBarSherlockCompat mSherlock;
    private MainMenu phoneExpandableMainMenu;
    private boolean isSocietyFeedsUpdating = false;

    @Inject
    private AANHelper aanHelper;
    @Inject
    private Settings settings;
    @Inject
    private Authorizer authorizer;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private UpdateManager updateManager;
    @Inject
    private ArticleService articleService;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private HomePageService homePageService;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;

    private NotificationProcessor savedArticlesListChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateSavedArticlesButtonText();
        }
    };

    private NotificationProcessor earlyViewChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateTabVisibility();
        }
    };

    private NotificationProcessor specialSectionsChanged = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateTabVisibility();
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

    private NotificationProcessor societyFavoritesCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateSocietyFavoritesMenuItem();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.journal_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initContentView(savedInstanceState);
    }

    public void initContentView(Bundle savedInstanceState) {
        progress = new ProgressHandler(this);

        if (DeviceUtils.isTablet(this.getActivity())) {
            mainMenu = findView(R.id.main_menu);
            mainMenu.setBackgroundColor(theme.getMainColor());
        } else {
            phoneExpandableMainMenu = ((MainActivity) getJournalActivity()).getPhoneExpandableMainMenu();
            mainMenu = ((MainActivity) getJournalActivity()).getMainMenu();
        }

        initializeTabHost(savedInstanceState);

        if (DeviceUtils.isPhone(this.getActivity())) {
            int mainColor = ColorUtils.changeAlpha(theme.getMainColor(), 255);

            int normalTopColor = ColorUtils.modifyHsv(mainColor, 0.03f, 0.98f);
            int normalBottomColor = ColorUtils.modifyHsv(mainColor, 0.05f, 0.95f);
            int pressedTopColor = ColorUtils.modifyHsv(mainColor, 0.2f, 0.85f);
            int pressedBottomColor = ColorUtils.modifyHsv(mainColor, 0.25f, 0.9f);

            GradientDrawable normalGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{normalTopColor, normalBottomColor});
            GradientDrawable pressedGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{pressedTopColor, pressedBottomColor});

            for (View tabButton : tabButtons) {
                StateListDrawable background = makeStateListDrawable(normalGradient, pressedGradient);
                tabButton.setBackgroundDrawable(background);
            }
        }

        // action bar
        mSherlock = new ActionBarSherlockCompat(getJournalActivity(), 0,
                (ViewGroup) findView(R.id.abs__screen_action_bar));
        mSherlock.setupActionBar().setBackgroundDrawable(theme.getMainColor());
        mSherlock.setListener(this);
        if (DeviceUtils.isPhone(this.getActivity())) {
            mSherlock.getActionBar().show();
        } else {
            mSherlock.getActionBar().hide();
        }
        tabIdForSelectOnStart = selectStartingTab(savedInstanceState);

        // feature: quick link menu
        if (DeviceUtils.isPhone(this.getActivity())) {
            // touch layout
            ((TouchRefreshLayout) findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    showQuickLinkMenu();
                }
            });

            quickLinkMenuComponent.initQuickLink(getActivity(), this);
        }
    }

    private void initializeTabHost(final Bundle savedInstanceState) {
//        homeDebugViewTab = mainMenu.findViewById(R.id.home_tab);
        earlyViewTab = mainMenu.findViewById(R.id.early_view_tab);
        issuesTab = mainMenu.findViewById(R.id.issues_tab);
        specialSectionsTab = mainMenu.findViewById(R.id.special_sections_tab);
        savedArticlesTab = mainMenu.findViewById(R.id.saved_articles_tab);
        globalSearchTab = mainMenu.findViewById(R.id.global_search_tab);
        settingsTab = mainMenu.findViewById(R.id.settings_tab);
        infoTab = mainMenu.findViewById(R.id.info_tab);

        boolean isTablet = DeviceUtils.isTablet(getActivity());
//        setupTab(homeDebugViewTab, new FragmentTabInfo(HomePageFragment.class, savedInstanceState, "Home debug"));
        setupTab(earlyViewTab, new FragmentTabInfo(EarlyViewContent.class, savedInstanceState, getString(R.string.early_view_tab_label)));
        setupTab(issuesTab, new FragmentTabInfo(IssuesContent.class, savedInstanceState, getString(R.string.issues_tab_label)));
        setupTab(specialSectionsTab, new FragmentTabInfo(SpecialSectionsFragment.class, savedInstanceState, getString(R.string.special_sections_tab_label)));
        setupTab(savedArticlesTab, new FragmentTabInfo(SavedArticlesFragment.class, savedInstanceState, getString(R.string.saved_articles_tab_label)));
        setupTab(globalSearchTab, new FragmentTabInfo(GlobalSearchFragment.class, savedInstanceState, getString(R.string.global_search_tab_label)));
        if (isTablet) {
            setupTab(settingsTab, new FragmentTabInfo(SettingsFragment.class, savedInstanceState, getString(R.string.settings_tab_label)));
        }
        setupTab(infoTab, new FragmentTabInfo(InfoFragment.class, savedInstanceState, getString(R.string.info_tab_label)));

        if (isTablet) {
//            assignTintDrawableToButton(homeDebugViewTab, R.drawable.home_on, R.drawable.home_off, false);
            assignTintDrawableToButton(earlyViewTab, R.drawable.early_view_on, R.drawable.early_view_off, false);
            assignTintDrawableToButton(issuesTab, R.drawable.issues_on, R.drawable.issues_off, false);
            assignTintDrawableToButton(specialSectionsTab, R.drawable.special_sections_on, R.drawable.special_sections_off, false);
            assignTintDrawableToButton(savedArticlesTab, R.drawable.saved_articles_on, R.drawable.saved_articles_off, false);
            assignTintDrawableToButton(globalSearchTab, R.drawable.global_search_on, R.drawable.global_search_off, false);
            assignTintDrawableToButton(settingsTab, R.drawable.settings_on, R.drawable.settings_off, true);
            assignTintDrawableToButton(infoTab, R.drawable.about_on, R.drawable.about_off, true);
        }
    }

    /**
     * feature: quick link menu
     */
    private void showQuickLinkMenu() {
        if (currentTab.getTabId() != R.id.info_tab) {
            quickLinkMenuComponent.showQuickLinkMenu();
        }
    }

    private Boolean darkBackground = null;

    private boolean isDarkBackground() {
        if (darkBackground == null) {
            darkBackground = theme.isJournalHasDarkBackground();
        }
        return darkBackground;
    }

    private void assignTintDrawableToButton(View view, int onBitmapId, int offBitmapId, boolean isSmallButton) {
        int mainColor = theme.getMainColor();
        mainColor = ColorUtils.changeAlpha(mainColor, 255);

        int normalBitmapId = isDarkBackground() ? onBitmapId : offBitmapId;
        Bitmap normalBitmap = BitmapUtils.loadResource(this.getActivity(), normalBitmapId);
        normalBitmap = BitmapUtils.applyTint(normalBitmap, mainColor);
        normalBitmap = BitmapUtils.applyAlpha(normalBitmap, isDarkBackground() ? 255 : 179);
        BitmapDrawable normalDrawable = BitmapUtils.makeDrawable(this.getActivity(), normalBitmap);

        int activeBitmapId = isDarkBackground() ? onBitmapId : offBitmapId;
        Bitmap activeBitmap = BitmapUtils.loadResource(this.getActivity(), activeBitmapId);
        int activeTintColor = ColorUtils.modifyColor(mainColor, isDarkBackground() ? 0.6f : 0.8f);
        activeBitmap = BitmapUtils.applyTint(activeBitmap, activeTintColor);
        if (isSmallButton) {
            activeBitmap = BitmapUtils.applyAlpha(activeBitmap, isDarkBackground() ? 127 : 255);
        } else {
            activeBitmap = BitmapUtils.applyAlpha(activeBitmap, isDarkBackground() ? 179 : 255);
        }
        BitmapDrawable activeDrawable = BitmapUtils.makeDrawable(this.getActivity(), activeBitmap);

        StateListDrawable drawable = makeStateListDrawable(normalDrawable, activeDrawable);

        if (isSmallButton) {
            ((ImageButton) view).setImageDrawable(drawable);
        } else {
            Button button = (Button) view;

            button.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);

            int normalTextColor = isDarkBackground()
                    ? Color.WHITE
                    : ColorUtils.modifyColor(mainColor, 0.5f);
            int activeTextColor = isDarkBackground()
                    ? ColorUtils.changeAlpha(Color.WHITE, 179)
                    : ColorUtils.modifyColor(mainColor, 0.4f);
            ColorStateList textColors = makeStateListColors(normalTextColor, activeTextColor);
            button.setTextColor(textColors);

            int shadowColor = isDarkBackground()
                    ? Color.BLACK
                    : Color.WHITE;
            shadowColor = ColorUtils.changeAlpha(shadowColor, 127);
            button.setShadowLayer(1.0f, 0.0f, 1.0f, shadowColor);

            int dividerOffset = getResources().getDimensionPixelOffset(R.dimen.left_bar_menu_button_divider);
            LayerDrawable normalBackgroundDrawable = new LayerDrawable(new Drawable[]{getResources().getDrawable(R.drawable.left_menu_item_bg_shape)});
            normalBackgroundDrawable.setLayerInset(0, 0, dividerOffset, 0, 0);

            int activeBackgroundColor = isDarkBackground()
                    ? ColorUtils.modifyColor(mainColor, 0.6f)
                    : ColorUtils.modifyColor(mainColor, 0.8f);

            LayerDrawable pressedBackgroundDrawable = new LayerDrawable(new Drawable[]{
                    new ColorDrawable(activeBackgroundColor),
                    getResources().getDrawable(R.drawable.left_menu_item_bg_shape)});
            pressedBackgroundDrawable.setLayerInset(1, 0, dividerOffset, 0, 0);

            BitmapDrawable selectedBackgroundTriangle = (BitmapDrawable) getResources().getDrawable(R.drawable.triangle);
            selectedBackgroundTriangle.setBounds(0, 0, selectedBackgroundTriangle.getIntrinsicWidth(), selectedBackgroundTriangle.getIntrinsicHeight());
            selectedBackgroundTriangle.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            LayerDrawable selectedBackgroundDrawable = new LayerDrawable(new Drawable[]{
                    new ColorDrawable(activeBackgroundColor),
                    getResources().getDrawable(R.drawable.left_menu_item_bg_shape),
                    selectedBackgroundTriangle});
            selectedBackgroundDrawable.setLayerInset(1, 0, dividerOffset, 0, 0);

            int paddingLeft = button.getPaddingLeft();
            int paddingTop = button.getPaddingTop();
            int paddingRight = button.getPaddingRight();
            int paddingBottom = button.getPaddingBottom();

            button.setBackgroundDrawable(makeStateListDrawable(normalBackgroundDrawable, pressedBackgroundDrawable, selectedBackgroundDrawable));

            // Workaround. On android 4.0 and 4.1 paddings are set to zero when changing background
            button.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }
    }

    private StateListDrawable makeStateListDrawable(Drawable normalDrawable, Drawable activeDrawable) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, activeDrawable);
        drawable.addState(new int[]{android.R.attr.state_focused}, activeDrawable);
        drawable.addState(new int[]{android.R.attr.state_selected}, activeDrawable);
        drawable.addState(new int[]{}, normalDrawable);
        return drawable;
    }

    private StateListDrawable makeStateListDrawable(Drawable normalDrawable, Drawable pressedDrawable, Drawable selectedDrawable) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_selected}, selectedDrawable);
        drawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        drawable.addState(new int[]{android.R.attr.state_focused}, pressedDrawable);
        drawable.addState(new int[]{}, normalDrawable);
        return drawable;
    }

    private ColorStateList makeStateListColors(int normalColor, int activeColor) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_selected},
                        new int[]{}
                },
                new int[]{
                        activeColor,
                        activeColor,
                        activeColor,
                        normalColor
                }
        );
    }

    private void setupTab(final View tabButton, final TabInfo tabInfo) {
        tabButton.setClickable(true);
        tabButton.setOnClickListener(onTabButtonClickedListener);

        int tabId = tabButton.getId();
        tabInfo.setUp(tabId);

        tabButtons.add(tabButton);
        tabInfoMap.put(tabId, tabInfo);
    }

    private int selectStartingTab(final Bundle savedInstanceState) {
        int tabId = R.id.early_view_tab;

        final Intent intent = getActivity().getIntent();
        if (intent != null) {
            tabId = intent.getIntExtra(Extras.EXTRA_STARTING_TAB, tabId);
        }

        if (savedInstanceState != null) {
            tabId = savedInstanceState.getInt(State_CurrentTab, tabId);
        }

        return tabId;
    }

    private void updateTabVisibility() {
        boolean needShowEarlyView = articleService.hasArticlesForEarlyView();
        if (needShowEarlyView) {
            if (phoneExpandableMainMenu != null) {
                phoneExpandableMainMenu.addEarlyViewItem();
            } else {
                earlyViewTab.setVisibility(View.VISIBLE);
            }
        } else {
            if (phoneExpandableMainMenu != null) {
                phoneExpandableMainMenu.removeEarlyViewItem();
            } else {
                earlyViewTab.setVisibility(View.GONE);
            }
            if (currentTab == tabInfoMap.get(earlyViewTab.getId())) {
                changeCurrentTab(issuesTab.getId());
            }
        }

        boolean needShowSpecialSections = specialSectionService.hasSpecialSections();
        if (needShowSpecialSections) {
            if (phoneExpandableMainMenu != null) {
                phoneExpandableMainMenu.addSpecialSectionsItem();
            } else {
                specialSectionsTab.setVisibility(View.VISIBLE);
            }
        } else {
            if (phoneExpandableMainMenu != null) {
                phoneExpandableMainMenu.removeSpecialSectionsItem();
            } else {
                specialSectionsTab.setVisibility(View.GONE);
            }
            if (currentTab == tabInfoMap.get(specialSectionsTab.getId())) {
                changeCurrentTab(issuesTab.getId());
            }
        }
    }

    private void updateSocietyFavoritesMenuItem() {
        if (phoneExpandableMainMenu != null) {
            final long favoritesCount = homePageService.getFavoriteItemsCount();
            final String title = String.format(getString(R.string.society_favorites_menu_item_format), favoritesCount);
            phoneExpandableMainMenu.setSocietyFavoritesItemTitle(title);
        }
    }

    private void updateSocietyFeeds() {
        if (phoneExpandableMainMenu != null) {
            Collection<FeedMO> feeds = homePageService.getFeeds();
            phoneExpandableMainMenu.addSocietyFeeds(feeds);
        }
    }

    @SuppressLint("StringFormatMatches")
    private void updateSavedArticlesButtonText() {
        long savedCount = articleService.getSavedArticleCount();
        String savedCountStr = String.format(getString(R.string.saved_articles_tab_label_format), savedCount);
        if (DeviceUtils.isTablet(getActivity())) {
            TextView button = (TextView) getActivity().findViewById(R.id.saved_articles_tab);
            button.setText(savedCountStr);
        } else {
            phoneExpandableMainMenu.setJournalSavedArticlesTitle(savedCountStr);
        }
    }

    protected View.OnClickListener onTabButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View target) {
            onTabButtonClicked(target);
        }
    };

    protected void onTabButtonClicked(final View target) {
        int tabId = target.getId();
        changeCurrentTab(tabId);
    }

    public void changeCurrentTab(final int tabId) {
        changeCurrentTab(tabId, ((MainActivity) getJournalActivity()).isShowingSlidingMenu());
    }

    public void changeCurrentTab(final int tabId, boolean needCloseSlidingMenu) {
        final TabInfo newTab = tabInfoMap.get(tabId);

        if (currentTab != newTab) {
            if (currentTab != null) {
                currentTab.hide();
                currentTab.onHideTab();
            }

            currentTab = newTab;

            if (newTab != null) {
                newTab.show();
                newTab.onShowTab();
            }

            for (View tabButton : tabButtons) {
                if (tabButton.getId() == tabId) {
                    tabButton.setSelected(true);
                } else {
                    tabButton.setSelected(false);
                }
            }

            if (DeviceUtils.isPhone(this.getActivity())) {
                mSherlock.setTitleActionBar(newTab != null ? newTab.getTitle() : "",
                        (int) getActivity().getResources().getDimension(R.dimen.action_bar_title_padding_left));
            }

            {  // Adobe Analytics
                final String tabTitle = newTab != null ? newTab.getTitle() : "";
                switch (tabTitle) {
                    case AANHelper.TAB_TITLE_EARLY_VIEW:
                        aanHelper.trackEarlyViewScreen();
                        break;
                    case AANHelper.TAB_TITLE_ISSUES:
                        aanHelper.trackIssuesListScreen();
                        break;
                    case AANHelper.TAB_TITLE_SPECIAL_SECTIONS:
                        aanHelper.trackSpecialSectionsScreen();
                        break;
                    case AANHelper.TAB_TITLE_SAVED_ARTICLES:
                        aanHelper.trackSavedArticlesScreen();
                        break;
                    case AANHelper.TAB_TITLE_SETTINGS:
                        aanHelper.trackSettingsScreen();
                        break;
                    case AANHelper.TAB_TITLE_ABOUT:
                        aanHelper.trackJournalInfoScreen();
                        break;
                    case AANHelper.TAB_TITLE_SEARCH:
                        aanHelper.trackGlobalSearchScreen();
                        break;
                }
            }
        } else {
            currentTab.showBack();
        }
        if (needCloseSlidingMenu) {
            ((MainActivity) getJournalActivity()).showContent();
        }

        if (isVisible()) {
            ((MainActivity) getJournalActivity()).onJournalPageNavigated();
        }
    }

    public void showProgress(final String progressText) {
        if (progress != null) {
            progress.showProgress(progressText);
        }
    }

    public void hideProgress() {
        progress.hideProgress();
    }

    abstract class TabInfo {
        protected final Bundle args;
        private int tabId;

        protected TabInfo(final Bundle args) {
            this.args = args;
        }

        public abstract String getTitle();

        public int getTabId() {
            return tabId;
        }

        public void setUp(int tabId) {
            this.tabId = tabId;
        }

        public abstract void hide();

        public abstract void show();

        public abstract void onShowTab();

        public abstract void onHideTab();

        public abstract void onSoftKeyboardVisibleChanged(boolean visible);

        public abstract void showBack();

        public abstract boolean onBackPressed();
    }

    class FragmentTabInfo extends TabInfo {
        private final Class<? extends BaseTabFragment> clazz;
        private BaseTabFragment fragment;
        private String title;
        private String tag;

        public FragmentTabInfo(final Class<? extends BaseTabFragment> clazz, final Bundle args, final String title) {
            super(args);
            this.clazz = clazz;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public void setUp(int tabId) {
            super.setUp(tabId);
            this.tag = String.format("tab_%d", tabId);
            fragment = (BaseTabFragment) getChildFragmentManager().findFragmentByTag(tag);
            if (fragment != null) {
                fragment.dispatchOnHide();
                getChildFragmentManager().beginTransaction()
                        .hide(fragment)
                        .commit();
                getChildFragmentManager().executePendingTransactions();
            }
        }

        @Override
        public void hide() {
            if (fragment != null) {
                fragment.dispatchOnHide();
                getChildFragmentManager().beginTransaction().hide(fragment).commit();
                getChildFragmentManager().executePendingTransactions();
            }
        }

        @Override
        public void show() {
            final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            if (fragment == null) {
                Logger.d(TAG, "Getting new instance of " + tag);
                fragment = (BaseTabFragment) Fragment.instantiate(JournalMainFragment.this.getActivity(), clazz.getName(), args);
                ft.add(R.id.tab_content, fragment, tag);
            }
            ft
                    .show(fragment)
                    .commit();
            fragment.dispatchOnShow();

            // feature: quick link menu
            showQuickLinkMenu();
        }

        @Override
        public void onShowTab() {
            fragment.onShowTab();
        }

        @Override
        public void onHideTab() {
            fragment.onHideTab();
        }

        @Override
        public void onSoftKeyboardVisibleChanged(boolean visible) {
            fragment.onSoftKeyboardVisibleChanged(visible);
        }

        @Override
        public boolean onBackPressed() {
            return fragment != null && fragment.isShowing() && fragment.onBackPressed();
        }

        @Override
        public void showBack() {
            fragment.dispatchOnShow();

            // feature: quick link menu
            showQuickLinkMenu();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationCenter.subscribeToNotification(EventList.EARLY_VIEW_FEED_UPDATED.getEventName(), earlyViewChanged);
        notificationCenter.subscribeToNotification(EventList.SPECIAL_SECTIONS_UPDATED.getEventName(), specialSectionsChanged);
        notificationCenter.subscribeToNotification(EventList.HOME_PAGE_FEEDS_UPDATE_FINISHED.getEventName(), homeFeedUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(), rssFeedUpdateCompletedProcessor);
        notificationCenter.subscribeToNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName(), menuItemCountChangedProcessor);
        notificationCenter.subscribeToNotification(EventList.SOCIETY_FAVORITES_COUNT_CHANGED.getEventName(), societyFavoritesCountChangedProcessor);
    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();
        updateSavedArticlesButtonText();
        updateSocietyFavoritesMenuItem();
        updateSocietyFeeds();

        if (settings.isAuthorized() && settings.getNeedShowSponsoredPromo()) {
            final String accessCode = settings.getAccessCode();
            if (accessCode != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        authorizationService.useAccessCode(accessCode);
                    }
                }).start();
            }
            settings.setShowSponsoredPromo(false);
            startActivity(new Intent(this.getActivity(), SponsoredPromoActivity.class));
            return;
        }

        if (settings.getNeedShowGetAccessScreenOnStart()) {
            settings.setNeedShowGetAccessScreenOnStart(false);
            if (!settings.isAuthorized() && NetUtils.isOnline(getActivity())) {
                authorizer.requestAccess(this.getActivity());
            } else {
                showSubscriptionCodeExpirationMessage();
                showAffiliationExpirationMessage();
            }
        }
    }

    private void showAffiliationExpirationMessage() {
        try {
            final String affiliationInfo = settings.getAffiliationInfo();
            if (affiliationInfo == null || affiliationInfo.isEmpty()) {
                return;
            }
            final JSONArray organisations = new JSONObject(affiliationInfo).getJSONArray("organisations");
            if (organisations.length() == 0) {
                return;
            }

            final StringBuilder message = new StringBuilder();
            for (int i = 0; i < organisations.length(); i++) {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                final JSONObject organisation = organisations.getJSONObject(i);
                final Date expirationDate = format.parse(organisation.getString("validTo"));
                final long interval = expirationDate.getTime() - System.currentTimeMillis();

                final int numberOfDays = (int) (interval / (24 * 60 * 60 * 1000)) + 1;

                if (isItTimeToWarn(numberOfDays)) {
                    if (message.length() > 0) {
                        message.append("\n");
                    }
                    message.append(String.format(getString(R.string.alert_text_affiliation_expiration_days), organisation.getString("name"), numberOfDays));
                }

            }

            if (message.length() > 0) {
                AlertDialogActivity.show(this.getActivity(),
                        getString(R.string.warning),
                        message.toString(),
                        AlertDialogActivity.FLAG_SHOW_CLOSE_BUTTON | AlertDialogActivity.FLAG_SHOW_MORE_INFO);
            }
        } catch (JSONException | ParseException e) {
            Logger.s(TAG, e);
        }
    }

    private void showSubscriptionCodeExpirationMessage() {
        if (authorizationService.hasAccessCode()) {
            final AuthorizationService.AccessCodeInformation accessCodeInformation = authorizationService.getAccessCodeInformation();

            if (accessCodeInformation.hasExpiration() && !accessCodeInformation.isExpired()) {
                final int days = accessCodeInformation.daysToExpiration();

                if (isItTimeToWarn(days)) {
                    aanHelper.trackSubscriptionExpirationWarning(days);
                    AlertDialogActivity.show(this.getActivity(),
                            getString(R.string.alert_title_susbscription_expiration),
                            String.format(getString(R.string.alert_text_susbscription_expiration), days),
                            AlertDialogActivity.FLAG_SHOW_CLOSE_BUTTON | AlertDialogActivity.FLAG_SHOW_MORE_INFO);
                }
            }
        }
    }

    private boolean isItTimeToWarn(int daysToExpiration) {
        final Set<Integer> days = new HashSet<>(Arrays.asList(29, 10, 5, 3, 2));
        return days.contains(daysToExpiration);
    }


    private Integer tabIdForSelectOnStart;

    @Override
    public void onHiddenChanged(boolean hidden) {
        Logger.d(TAG_LIFE, "onHiddenChanged(): hidden = " + hidden);
        super.onHiddenChanged(hidden);
        if (currentTab != null) {
            ((FragmentTabInfo) currentTab).fragment.onHiddenChanged(hidden);
        }

        // feature: quick link menu
        if (!hidden) {
            showQuickLinkMenu();
        }
    }

    @Override
    public void onStart() {
        Logger.d(TAG_LIFE, "onStart()");
        super.onStart();
        notificationCenter.subscribeToNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), savedArticlesListChanged);

        if (currentTab != null) {
            changeCurrentTab(currentTab.getTabId(), false);
        } else {
            changeCurrentTab(tabIdForSelectOnStart);
        }
        updateTabVisibility();
    }

    @Override
    public void onStop() {
        Logger.d(TAG_LIFE, "onStop()");
        notificationCenter.unSubscribeFromNotification(savedArticlesListChanged);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG_LIFE, "onDestroy()");
        super.onDestroy();
        notificationCenter.unSubscribeFromNotification(earlyViewChanged);
        notificationCenter.unSubscribeFromNotification(specialSectionsChanged);
        notificationCenter.unSubscribeFromNotification(homeFeedUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(rssFeedUpdateCompletedProcessor);
        notificationCenter.unSubscribeFromNotification(menuItemCountChangedProcessor);
        notificationCenter.unSubscribeFromNotification(societyFavoritesCountChangedProcessor);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Logger.d(TAG_LIFE, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        if (currentTab != null) {
            outState.putInt(State_CurrentTab, currentTab.getTabId());
        }
    }

    public void onSoftKeyboardVisibleChanged(boolean visible) {
        if (currentTab != null) {
            currentTab.onSoftKeyboardVisibleChanged(visible);
        }
    }

    public int getActiveTabId() {
        return currentTab.tabId;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCode_ShowCodeExpired) {
            if (resultCode == Activity.RESULT_OK) {
                changeCurrentTab(R.id.settings_tab);
            }
        }
        ((FragmentTabInfo) currentTab).fragment.onActivityResult(requestCode, resultCode, data);
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

    public ActionMode startActionMode(ActionMode.Callback callback) {
        final ActionMode actionMode = mSherlock.startActionMode(callback);
        mSherlock.getActionBar().setActionModeBackground(new ColorDrawable(theme.getMainColor()));
        return actionMode;
    }

    public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.issues_edit, menu);

        return true;
    }
}