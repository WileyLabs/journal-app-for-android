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
package com.wiley.android.journalApp.base;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.actionbarsherlock.internal.widget.ActionBarContainer;
import com.google.inject.Inject;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.wiley.android.journalApp.MainApplication;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.activity.FiguresFragment;
import com.wiley.android.journalApp.activity.GetAccessDialogFragment;
import com.wiley.android.journalApp.activity.IssueTOCFragment;
import com.wiley.android.journalApp.activity.JournalMainFragment;
import com.wiley.android.journalApp.activity.OauthAuthorizationActivity;
import com.wiley.android.journalApp.activity.SpecialSectionArticlesActivity;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.components.MainMenu;
import com.wiley.android.journalApp.controller.ConnectionController;
import com.wiley.android.journalApp.controller.DriveController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.fragment.SocietyNavigationPanel;
import com.wiley.android.journalApp.fragment.SocietyNewsFragment;
import com.wiley.android.journalApp.fragment.feeds.FeedItemsViewerFragment;
import com.wiley.android.journalApp.fragment.feeds.FeedListViewerFragment;
import com.wiley.android.journalApp.fragment.feeds.SocietyFavoritesFragment;
import com.wiley.android.journalApp.fragment.feeds.SocietyGlobalSearchFragment;
import com.wiley.android.journalApp.fragment.settings.SettingsFragment;
import com.wiley.android.journalApp.utils.BundleUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.http.UpdateManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.service.InAppBillingService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Settings;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Products;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.wiley.wol.client.android.notification.EventList.MAIN_ACTIVITY_IS_SHOWN;
import static com.wiley.wol.client.android.notification.EventList.NETWORK_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.CURRENT_TAB_ID;
import static com.wiley.wol.client.android.notification.NotificationCenter.FEED_MO;
import static com.wiley.wol.client.android.notification.NotificationCenter.IS_GLOBAL_SEARCH_SHOWN;
import static com.wiley.wol.client.android.notification.NotificationCenter.IS_HOME_SHOWN;
import static com.wiley.wol.client.android.notification.NotificationCenter.IS_SOCIETY_FAVORITES_SHOWN;
import static java.util.Collections.singletonList;
import static org.solovyev.android.checkout.ProductTypes.SUBSCRIPTION;

public class MainActivity extends JournalActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final String DOI_KEY = "DOI";
    private static final String RESTORING = "restoring";
    private static final String JOURNAL_IS_ACTIVE = "journalIsActive";
    private static final String EXTRAS = "extras";

    private Bundle savedInstanceState;
    private final Deque<Fragment> journalFragmentsStack = new ArrayDeque<>();
    private final Deque<Fragment> societyNewsFragmentsStack = new ArrayDeque<>();

    protected View mainMenu;
    protected SlidingMenu slidingMenu;
    private MainMenu phoneExpandableMainMenu;
    private JournalMainFragment journal;

    private View.OnClickListener listenerGlobalSearchSortMenu;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private Settings settings;
    @Inject
    private Theme theme;

    @Inject
    private NotificationCenter notificationCenter;

    @Inject
    private HomePageService homePageService;

    @Inject
    private DriveController driveController;

    @Inject
    private Billing billing;
    @Inject
    private InAppBillingService inAppBillingService;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private WebController webController;
    @Inject
    protected ConnectionController connectionController;
    @Inject
    private UpdateManager updateManager;
    @Inject
    private ArticleService articleService;
    @Inject
    protected AANHelper aanHelper;
    private boolean firstStart = true;

    protected ActivityCheckout checkout;
    private final Set<View> disabledView = new HashSet<>();
    private final Map<Integer, ArticleMO> articlesToSave = new HashMap<>();
    private boolean restoringActivity = false;

    private NotificationProcessor pushNotificationOpenIssueArticleProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);

            navigateToJournalIssues();
            openIssue(pr.getIssueDoi());
            openArticles(pr.getDoiList(), pr.getArticleDoi(), pr.getTitleList(), false);
        }
    };
    private NotificationProcessor menuItemCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (!societyNewsFragmentsStack.peek().isVisible()) {
                onJournalPageNavigated();
            } else {
                onSocietyPageNavigated();
            }
        }
    };
    private NotificationProcessor homeFeedUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {

            if (DeviceUtils.isTablet(MainActivity.this)) {
                final SocietyNavigationPanel societyNavigationPanel = (SocietyNavigationPanel) getSupportFragmentManager()
                        .findFragmentById(R.id.society_navigation_panel);

                setTabletSocietyNewsTitle(societyNavigationPanel);

                if (settings.isSocietyContentEnabled()) {
                    if (!societyNavigationPanel.isVisible()) {
                        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.show(societyNavigationPanel);
                        ft.commitAllowingStateLoss();
                    }
                } else {
                    if (societyNavigationPanel.isVisible()) {
                        societyNavigationPanel.navigateToJournal();
                        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.hide(societyNavigationPanel);
                        ft.commit();
                    }
                }
            } else {
                updatePhoneSocietyMenu();
            }
        }
    };

    private NotificationProcessor pushNotificationOpenEarlyViewArticleProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);

            navigateToJournalEarlyView();
            openArticles(pr.getDoiList(), pr.getArticleDoi(), pr.getTitleList(), false);
        }
    };
    private NotificationProcessor pushNotificationOpenIssueProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);
            navigateToSpecialSection();
            openSpecialSectionsArticle(pr.getSpecialSectionId());
            openArticles(pr.getDoiList(), pr.getArticleDoi(), pr.getTitleList(), false);
        }
    };
    private NotificationProcessor pushNotificationOpenIssue = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);

            navigateToJournalIssues();
            openIssue(pr.getIssueDoi());
        }
    };
    private NotificationProcessor allContentUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (settings.isAuthorized() && !settings.isFullAccess() && authorizationService.isUpdateContentAfterAuthorisationExpected()) {
                authorizationService.setUpdateContentAfterAuthorisationExpected(false);
                errorManager.alertWithErrorCode(MainActivity.this, AppErrorCode.ACCESS_FORBIDDEN_APP,
                        ErrorButton.withTitleAndListener(MainActivity.this.getString(R.string.get_help), new ErrorButton.OnClickListener() {
                            @Override
                            public void onClick() {
                                webController.openUrlInternal(theme.getHelpUrl());
                            }
                        }),
                        ErrorButton.withTitleAndListener(MainActivity.this.getString(android.R.string.ok), null));
            }
        }
    };
    private final NotificationProcessor networkStateChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            if (firstStart) {
                firstStart = false;
                return;
            }

            final boolean online = (boolean) params.get("online");
            if (online) {
                updateManager.updateFeeds(!articleService.hasArticlesForEarlyView());
                updateManager.updateHomePageFeeds();

                for (ArticleMO articleMO : articlesToSave.values()) {
                    articleService.addArticleRefToFavorites(articleMO);
                }
                articlesToSave.clear();
            }
        }
    };

    public void hideSocietyNavigationPanel() {
        final SocietyNavigationPanel societyNavigationPanel = (SocietyNavigationPanel) getSupportFragmentManager()
                .findFragmentById(R.id.society_navigation_panel);
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.hide(societyNavigationPanel);
        ft.commit();
    }

    public void showSocietyNavigationPanel() {
        if (settings.isSocietyContentEnabled()) {
            final SocietyNavigationPanel societyNavigationPanel = (SocietyNavigationPanel) getSupportFragmentManager()
                    .findFragmentById(R.id.society_navigation_panel);
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.show(societyNavigationPanel);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Intent intent = getIntent();
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRAS) && intent != null) {
            intent.putExtras(savedInstanceState.getBundle(EXTRAS));
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(RESTORING)) {
            restoringActivity = true;
        }

        super.onCreate(savedInstanceState);
        { // feature: in-app purchase
            checkout = Checkout.forActivity(this, billing, Products.create().add(SUBSCRIPTION, singletonList(theme.getNameOfPaidSubscription())));
            checkout.start();
            inAppBillingService.setCheckout(checkout);
        }

        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main_new);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        if (DeviceUtils.isPhone(this)) {
            this.slidingMenu = new SlidingMenu(this);
            this.slidingMenu.setMode(SlidingMenu.RIGHT);
            this.slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
            this.slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_MARGIN);
            if (DeviceUtils.isSmallPhone(this)) {
                this.slidingMenu.setBehindOffsetRes(R.dimen.right_bar_menu_offset);
            } else {
                this.slidingMenu.setBehindWidthRes(R.dimen.right_bar_menu_width);
            }
            this.slidingMenu.setBehindScrollScale(0.0f);
            this.slidingMenu.setShadowDrawable(R.drawable.shadowright);
            this.slidingMenu.setShadowWidthRes(R.dimen.right_bar_menu_shadow_width);
            this.slidingMenu.setFadeEnabled(true);
            this.slidingMenu.setFadeDegree(0.66f);
            this.slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW, false);

            this.slidingMenu.setMenu(R.layout.journal_main_menu);
            this.mainMenu = this.slidingMenu.getMenu();

            this.slidingMenu.setSecondaryOnOpenListner(new SlidingMenu.OnOpenListener() {
                @Override
                public void onOpen() {
                    onSideMenuOpen();
                }
            });
            this.slidingMenu.setOnClosedListener(new SlidingMenu.OnClosedListener() {
                @Override
                public void onClosed() {
                    onSideMenuClosed();
                }
            });

            disableSliding();

            phoneExpandableMainMenu = new MainMenu(this, mainMenu, notificationCenter);
        } else if (!settings.isArticleShowAbstractInitialized()) {
            settings.changeArticleShowAbstract(theme.isShowAbstaractByDefault());
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Fragment societyNavigationPanel = getSupportFragmentManager().findFragmentById(R.id.society_navigation_panel);
        if (DeviceUtils.isTablet(this) && !settings.isSocietyContentEnabled() && societyNavigationPanel != null) {
            ft.hide(societyNavigationPanel);
        }
        ft.commit();

        if (!restoringActivity) {
            ft = getSupportFragmentManager().beginTransaction();
            journal = (JournalMainFragment) Fragment.instantiate(this, JournalMainFragment.class.getName(), savedInstanceState);

            ft.add(R.id.content_container, journal);
            journalFragmentsStack.push(journal);

            Fragment societyNews = Fragment.instantiate(this, SocietyNewsFragment.class.getName(), savedInstanceState);
            ft.add(R.id.content_container, societyNews);
            societyNewsFragmentsStack.push(societyNews);

            ft.commit();

            boolean showSocietyContent = settings.isSocietyPageByDefault() && settings.isSocietyContentEnabled();
            if (showSocietyContent) {
                if (societyNavigationPanel != null) {
                    ((SocietyNavigationPanel) societyNavigationPanel).navigateToSocietyNews();
                }
                showFragment(societyNews, journal);
                onSocietyPageNavigated();
            } else {
                showFragment(journal, societyNews);
            }
            getSupportFragmentManager().executePendingTransactions();

            if (intent != null) {
                processIntent(intent);
            }

            if (DeviceUtils.isPhone(this)) {
                updatePhoneSocietyMenu();
            } else {
                setTabletSocietyNewsTitle((SocietyNavigationPanel) societyNavigationPanel);
            }
        }
        undim();

        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction())) {
            if (intent.hasExtra("fromNotification")) {
                Logger.d(TAG, "onCreate(): PushNotification.fromNotification !!!!!!!");
            }
        }

        Logger.d(TAG, "PushNotification: device token = " + settings.getDeviceToken());

        connectionController.start();

        { // feature: google drive
            driveController.onCreate(this);
        }

        { // feature: in-app purchase
            if (settings.hasSubscriptionReceipt()) {
                inAppBillingService.checkMcsSubscription();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        { // feature: google drive
            driveController.onStart();
        }
        notificationCenter.subscribeToNotification(EventList.MENU_ITEM_COUNT_CHANGED.getEventName(), menuItemCountChangedProcessor);
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_SUCCESS.getEventName(), homeFeedUpdatedProcessor);
        notificationCenter.subscribeToNotification(EventList.PUSH_NOTIFICATION_OPEN_ISSUE_ARTICLE.getEventName(), pushNotificationOpenIssueArticleProcessor);
        notificationCenter.subscribeToNotification(EventList.PUSH_NOTIFICATION_OPEN_EARLY_VIEW_ARTICLE.getEventName(), pushNotificationOpenEarlyViewArticleProcessor);
        notificationCenter.subscribeToNotification(EventList.PUSH_NOTIFICATION_OPEN_SPECIAL_SECTION_ARTICLE.getEventName(), pushNotificationOpenIssueProcessor);
        notificationCenter.subscribeToNotification(EventList.PUSH_NOTIFICATION_OPEN_ISSUE.getEventName(), pushNotificationOpenIssue);
        notificationCenter.subscribeToNotification(EventList.ALL_CONTENT_UPDATE_FINISHED.getEventName(), allContentUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(NETWORK_STATE_CHANGED.getEventName(), networkStateChangedProcessor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationCenter.sendNotification(MAIN_ACTIVITY_IS_SHOWN.getEventName());
        MainApplication.mainActivityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.mainActivityPaused();
    }

    @Override
    protected void onStop() {
        super.onStop();
        { // feature: google drive
            driveController.onStop();
        }
        notificationCenter.unSubscribeFromNotification(menuItemCountChangedProcessor);
        notificationCenter.unSubscribeFromNotification(homeFeedUpdatedProcessor);
        notificationCenter.unSubscribeFromNotification(pushNotificationOpenIssueArticleProcessor);
        notificationCenter.unSubscribeFromNotification(pushNotificationOpenEarlyViewArticleProcessor);
        notificationCenter.unSubscribeFromNotification(pushNotificationOpenIssueProcessor);
        notificationCenter.unSubscribeFromNotification(pushNotificationOpenIssue);
        notificationCenter.unSubscribeFromNotification(allContentUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(networkStateChangedProcessor);
    }

    @Override
    protected void onDestroy() {
        checkout.stop();
        super.onDestroy();
        connectionController.stop();
        { // feature: google drive
            driveController.onDestroy();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            { // feature: google drive
                driveController.reconnect();
            }
        } else if (requestCode == checkout.getDefaultRequestCode()) {
            checkout.onActivityResult(requestCode, resultCode, data);
        }
    }

    public MainMenu getPhoneExpandableMainMenu() {
        return phoneExpandableMainMenu;
    }

    public View getMainMenu() {
        return mainMenu;
    }

    protected void onSideMenuOpen() {
        changeAllWebViewsToSoftware();

        final ViewGroup viewGroup = getActiveViewGroup();
        disableViewGroup(viewGroup);

        final Deque<Fragment> stack = journalFragmentsStack.peek().isVisible() ? journalFragmentsStack : societyNewsFragmentsStack;
        stack.peek().onHiddenChanged(true);
    }

    protected void onSideMenuClosed() {
        changeAllWebViewsToHardware();

        enableViewGroup();

        if (DeviceUtils.isPhone(this)) {
            final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_container);
            if (currentFragment != null && currentFragment.getClass() == SettingsFragment.class) {
                return;
            }
        }

        final Deque<Fragment> stack = journalFragmentsStack.peek().isVisible() ? journalFragmentsStack : societyNewsFragmentsStack;
        stack.peek().onHiddenChanged(false);
    }

    private void setTabletSocietyNewsTitle(final SocietyNavigationPanel societyNavigationPanel) {
        final String societyAlternateTabLabel = settings.getSocietyAlternateTabLabelForSocietyFeedPage();
        final String societyNewsTitle = !TextUtils.isEmpty(societyAlternateTabLabel) ? societyAlternateTabLabel :
                MainActivity.this.getString(R.string.society_news);
        societyNavigationPanel.setSocietyNewsTitle(societyNewsTitle);
    }

    private void updatePhoneSocietyMenu() {
        if (settings.isSocietyContentEnabled()) {
            changeMenuSocietyTitle();
            phoneExpandableMainMenu.showSocietyGroup();
        } else {
            if (!journalFragmentsStack.peek().isVisible()) {
                showFragment(journalFragmentsStack.peek(), societyNewsFragmentsStack.peek());
            }
            phoneExpandableMainMenu.hideSocietyGroup();
        }
    }

    private void changeMenuSocietyTitle() {
        final String societyAlternateTabLabel = settings.getSocietyAlternateTabLabelForSocietyFeedPage();
        if (!TextUtils.isEmpty(societyAlternateTabLabel)) {
            phoneExpandableMainMenu.setSocietyTitle(societyAlternateTabLabel);
        } else {
            phoneExpandableMainMenu.setSocietyTitle(MainActivity.this.getString(R.string.society_news));
        }
    }

    private ViewGroup getActiveViewGroup() {
        final Deque<Fragment> stack = journalFragmentsStack.peek().isVisible() ? journalFragmentsStack : societyNewsFragmentsStack;
        return (ViewGroup) stack.peek().getView();
    }

    private void disableViewGroup(ViewGroup viewGroup) {
        final int childCount = viewGroup.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View view = viewGroup.getChildAt(i);
            if (view.getClass() == ActionBarContainer.class) {
                continue;
            }

            if (!view.isEnabled()) {
                continue;
            }

            setEnabledForView(view, false);

            disabledView.add(view);

            if (view instanceof ViewGroup) {
                disableViewGroup((ViewGroup) view);
            }
        }
    }

    private void enableViewGroup() {
        for (View view : disabledView) {
            setEnabledForView(view, true);
        }

        disabledView.clear();
    }

    private void setEnabledForView(View view, final boolean enabled) {
        if (view instanceof WebView || view instanceof ScrollView) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return !enabled;
                }
            });
        } else {
            view.setEnabled(enabled);
        }
    }

    //R.id.articleRefContentWebView: issueToc only work correctly with layerType = LAYER_TYPE_NONE because webView in scrollView
    //R.id.saved_articles_content_view: empty list of favorites work correctly only when layerType = LAYER_TYPE_SOFTWARE
    //R.id.slider_panel_info_web_view: references list in article view
    private final List<Integer> webViewWithPermanentLayerTypeIds = Arrays.asList(R.id.articleRefContentWebView,
            R.id.saved_articles_content_view, R.id.slider_panel_info_web_view);

    // Bugfix: fix for WebView rendering bug. see http://stackoverflow.com/questions/14466035/android-webview-rendering-when-used-together-with-menudrawer
    protected void changeAllWebViewsToSoftware() {
        List<WebView> webViews = UIUtils.getAllChildren(this.getWindow().getDecorView(), WebView.class);
        for (WebView webView : webViews) {
            if (!webViewWithPermanentLayerTypeIds.contains(webView.getId())) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
    }

    protected void changeAllWebViewsToHardware() {
        List<WebView> webViews = UIUtils.getAllChildren(this.getWindow().getDecorView(), WebView.class);
        for (WebView webView : webViews) {
            if (!webViewWithPermanentLayerTypeIds.contains(webView.getId())) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }
    }

    private void disableSliding() {
        if (slidingMenu != null) {
            slidingMenu.setSlidingEnabled(false);
        }
    }

    public void showContent() {
        if (slidingMenu != null) {
            slidingMenu.showContent(true);
        }
    }

    public void showMenu() {
        if (slidingMenu != null) {
            slidingMenu.showMenu(true);
        }
    }

    public void onSideMenuButtonClicked() {
        if (slidingMenu != null) {
            UIUtils.hideSoftInput(this);
            if (slidingMenu.isMenuShowing()) {
                showContent();
            } else {
                showMenu();
            }
        }
    }

    public boolean isShowingSlidingMenu() {
        return slidingMenu != null && slidingMenu.isMenuShowing();
    }

    public void showSocietyNews() {
        if (!societyNewsFragmentsStack.isEmpty()) {
            showFragment(societyNewsFragmentsStack.peek(), journalFragmentsStack.peek(),
                    R.anim.society_appear, R.anim.journal_disappear);
            onSocietyPageNavigated();
        }
    }

    public void showJournal() {
        if (!journalFragmentsStack.isEmpty()) {
            showFragment(journalFragmentsStack.peek(), societyNewsFragmentsStack.peek(),
                    R.anim.journal_appear, R.anim.society_disappear);
            onJournalPageNavigated();
        }
    }

    @SuppressWarnings("unused")
    public void onBarButtonClick(final View view) throws Exception {
        ((ArticleViewFragment) journalFragmentsStack.peek()).onBarButtonClick(view);
    }

    public void setListenerGlobalSearchSortMenu(View.OnClickListener listener) {
        this.listenerGlobalSearchSortMenu = listener;
    }

    @SuppressWarnings("unused")
    public void onGlobalSearchBarButtonClick(final View view) {
        if (null != listenerGlobalSearchSortMenu) {
            listenerGlobalSearchSortMenu.onClick(view);
        }
    }

    private void showFragment(Fragment fragmentToShow, Fragment currentFragment) {
        showFragment(fragmentToShow, currentFragment, FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_NONE);
    }

    private void showFragment(Fragment fragmentToShow, Fragment currentFragment, int animAppear, int animDisappear) {
        if (fragmentToShow == null) {
            return;
        }

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (animAppear != FragmentTransaction.TRANSIT_NONE) {
            ft.setCustomAnimations(animAppear, animDisappear);
        }
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }

        hideOrShowFragmentUnderGetAccessDialog(fragmentToShow, currentFragment, ft);

        ft
                .show(fragmentToShow)
                .commitAllowingStateLoss();
        final View view = fragmentToShow.getView();
        if (view != null) {
            view.bringToFront();
        }
        getSupportFragmentManager().executePendingTransactions();
    }

    private void hideOrShowFragmentUnderGetAccessDialog(Fragment fragmentToShow, Fragment currentFragment, FragmentTransaction ft) {
        if (currentFragment != null && currentFragment.getClass() == GetAccessDialogFragment.class) {
            Fragment topFragment = journalFragmentsStack.pop();
            Fragment fragment = journalFragmentsStack.peek();
            journalFragmentsStack.push(topFragment);
            ft.hide(fragment);
        } else if (fragmentToShow.getClass() == GetAccessDialogFragment.class && journalFragmentsStack.size() > 1) {
            Fragment topFragment = journalFragmentsStack.pop();
            Fragment fragment = journalFragmentsStack.peek();
            journalFragmentsStack.push(topFragment);
            ft.show(fragment);
        }
    }

    public void openArticles(List<DOI> doiList, DOI doiForOpen, String title, boolean savedArticles) {
        openArticles(doiList, doiForOpen, title, savedArticles, null);
    }

    public void openArticles(List<DOI> doiList, DOI doiForOpen, String title, boolean savedArticles, String term) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        final Fragment currentFragment = journalFragmentsStack.peek();
        if (currentFragment.getClass() == ArticleViewFragment.class) {
            ft.remove(currentFragment);
            journalFragmentsStack.pop();
        }

        final ArticleViewFragment articles = (ArticleViewFragment) Fragment.instantiate(this,
                ArticleViewFragment.class.getName(), savedInstanceState);

        final Intent intent = getIntent();
        BundleUtils.putParcelableListToIntent(intent, Extras.EXTRA_DOI_LIST, doiList);
        intent.putExtra(Extras.EXTRA_SAVED_ARTICLES, savedArticles);
        intent.putExtra(Extras.EXTRA_INITIAL_DOI_INDEX, doiList.indexOf(doiForOpen));
        intent.putExtra(Extras.EXTRA_TITLE, title);
        intent.putExtra(Extras.EXTRA_SEARCH_TERM, term);

        ft.add(R.id.content_container, articles);
        ft.commit();
        showFragment(articles, journalFragmentsStack.peek());
        journalFragmentsStack.push(articles);
    }

    @Override
    public void onBackPressed() {
        if (DeviceUtils.isPhone(this)) {
            final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_container);
            if (currentFragment != null && currentFragment.getClass() == SettingsFragment.class) {
                ((SettingsFragment) currentFragment).onBackPressed();
                return;
            } else if (currentFragment != null && currentFragment.getClass() == GetAccessDialogFragment.class) {
                if (journalFragmentsStack.peek() == currentFragment) {
                    journalFragmentsStack.pop();
                }
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.remove(currentFragment).commitAllowingStateLoss();
                return;
            }
        }

        if (isJournalShown()) {
            onBackPressedJournal();
            onJournalPageNavigated();
        } else {
            onBackPressedSocietyNews();
            onSocietyPageNavigated();
        }
    }

    private void onBackPressedJournal() {
        if (journalFragmentsStack.size() > 1) {
            Fragment fragment = journalFragmentsStack.peek();
            if (fragment.getClass() == ArticleViewFragment.class && ((ArticleViewFragment) fragment).handleBackPress()) {
                return;
            }

            journalFragmentsStack.pop();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment).commitAllowingStateLoss();
            showFragment(journalFragmentsStack.peek(), null);
            journalFragmentsStack.peek().onHiddenChanged(false);
        } else {
            super.onBackPressed();
        }
    }

    private void onBackPressedSocietyNews() {
        if (societyNewsFragmentsStack.size() > 1) {
            Fragment fragment = societyNewsFragmentsStack.peek();
            if (fragment.getClass() == ArticleViewFragment.class && ((ArticleViewFragment) fragment).handleBackPress()) {
                return;
            }

            societyNewsFragmentsStack.pop();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment).commit();
            showFragment(societyNewsFragmentsStack.peek(), null);
        } else if (journalFragmentsStack.peek().getClass() == GetAccessDialogFragment.class) {
            Fragment getAccessFragment = journalFragmentsStack.pop();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(getAccessFragment).commitAllowingStateLoss();
            showFragment(societyNewsFragmentsStack.peek(), null);
        } else {
            super.onBackPressed();
        }
    }

    public void openIssue(DOI doi) {
        getIntent().putExtra(DOI_KEY, doi);
        addAndShowJournalFragment(IssueTOCFragment.class);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState == null) {
            outState = new Bundle();
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            outState.putBundle(EXTRAS, extras);
        }

        outState.putBoolean(RESTORING, true);

        if (journalFragmentsStack.peek().isVisible()) {
            outState.putBoolean(JOURNAL_IS_ACTIVE, true);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoringActivity = false;

        hideFragments(societyNewsFragmentsStack);
        hideFragments(journalFragmentsStack);
        if (savedInstanceState != null && savedInstanceState.containsKey(JOURNAL_IS_ACTIVE)) {
            showFragment(journalFragmentsStack.peek(), null);
        } else {
            showFragment(societyNewsFragmentsStack.peek(), null);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (restoringActivity) {
            if (fragment instanceof Journal) {
                if (fragment.getClass() == JournalMainFragment.class) {
                    journal = (JournalMainFragment) fragment;
                }
                journalFragmentsStack.push(fragment);
            } else if (fragment instanceof Society) {
                societyNewsFragmentsStack.push(fragment);
            }
        }
    }

    private void hideFragments(Deque<Fragment> fragments) {
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : fragments) {
            fragmentTransaction.hide(fragment);
        }
        fragmentTransaction.commit();
    }

    public void openRssFeed(final String rssFeedUid) {
        prepareIntentForOpeningFeed(rssFeedUid);

        addAndShowSocietyNewsFragment(FeedListViewerFragment.class);
        onSocietyPageNavigated();
    }

    public void openFeedItems(List<String> itemsUids, int startingIndex) {
        BundleUtils.putListToIntent(getIntent(), Extras.EXTRA_FEED_ITEMS_LIST, itemsUids);
        getIntent().putExtra(Extras.EXTRA_INITIAL_FEED_ITEM_INDEX, startingIndex);

        addAndShowSocietyNewsFragment(FeedItemsViewerFragment.class);
        onSocietyPageNavigated();
    }

    public void openSpecialSectionsArticle(String sectionId) {
        getIntent().putExtra(SpecialSectionArticlesActivity.EXTRA_SECTION_ID, sectionId);
        addAndShowJournalFragment(SpecialSectionArticlesActivity.class);
    }

    public void openGetAccessDialog() {
        getIntent().putExtra(GetAccessDialogFragment.EXTRA_WARN_MESSAGE_KEY, "");
        getIntent().removeExtra(GetAccessDialogFragment.REQUEST_CODE);

        Fragment fragment = getFragmentByClass(GetAccessDialogFragment.class);
        if (DeviceUtils.isPhone(this) || isJournalShown()) {
            showFragment(fragment, null);
        }
        journalFragmentsStack.push(fragment);
    }

    private boolean isJournalShown() {
        if (phoneExpandableMainMenu != null) {
            return phoneExpandableMainMenu.isJournalShown();
        }

        final Fragment societyNavigationPanel = getSupportFragmentManager().findFragmentById(R.id.society_navigation_panel);
        return !(DeviceUtils.isTablet(this) && societyNavigationPanel != null) ||
                !((SocietyNavigationPanel) societyNavigationPanel).isSocietyNewsTabActive();

    }

    public void openGetAccessDialog(int requestCode, String warnMessage, DOI doi) {
        getIntent().putExtra(GetAccessDialogFragment.REQUEST_CODE, requestCode);
        getIntent().putExtra(GetAccessDialogFragment.EXTRA_WARN_MESSAGE_KEY, warnMessage);
        getIntent().putExtra(Extras.EXTRA_DOI, doi);

        Fragment fragment = getFragmentByClass(GetAccessDialogFragment.class);
        showFragment(fragment, null);
        journalFragmentsStack.push(fragment);
    }

    public void openFigures(DOI articleDoi, Integer figureId) {
        final Intent intent = getIntent();
        intent.putExtra(Extras.EXTRA_ARTICLE_DOI, articleDoi);
        intent.putExtra(Extras.EXTRA_FIGURE_ID, figureId);
        Fragment fragment = getFragmentByClass(FiguresFragment.class);
        showFragment(fragment, journalFragmentsStack.peek());
        journalFragmentsStack.push(fragment);
    }

    public void finishAccessDialog() {
        closeAccessDialog();
    }

    public boolean isFragmentOnTop(Fragment fragment) {
        final Fragment parentFragment = fragment.getParentFragment();
        final Fragment journalTopFragment = journalFragmentsStack.peek();
        final Fragment societyTopFragment = societyNewsFragmentsStack.peek();
        return journalTopFragment == fragment ||
                societyTopFragment == fragment ||
                journalTopFragment == parentFragment ||
                societyTopFragment == parentFragment;
    }

    private void closeAccessDialog() {
        onBackPressed();
        int requestCode = getIntent().getIntExtra(GetAccessDialogFragment.REQUEST_CODE, 0);
        if (requestCode > 0) {
            journalFragmentsStack.peek().onActivityResult(requestCode, getIntent()
                    .getIntExtra(GetAccessDialogFragment.RESULT_CODE, Activity.RESULT_CANCELED), getIntent());
        }
    }

    private void addAndShowJournalFragment(Class fragmentClass) {
        Fragment fragment = getFragmentByClass(fragmentClass);
        showFragment(fragment, journalFragmentsStack.peek());
        journalFragmentsStack.push(fragment);
    }

    private void addAndShowSocietyNewsFragment(Class fragmentClass) {
        Fragment fragment = getFragmentByClass(fragmentClass);
        showFragment(fragment, societyNewsFragmentsStack.peek());
        societyNewsFragmentsStack.push(fragment);
    }

    private Fragment getFragmentByClass(Class fragmentClass) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment = Fragment.instantiate(this, fragmentClass.getName(), savedInstanceState);

        ft.add(R.id.content_container, fragment);
        ft.hide(fragment);
        ft.commit();
        return fragment;
    }

    @SuppressWarnings("unused")
    public void doNothing(final View view) throws Exception {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (processIntent(intent)) {
            return;
        }
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            if (intent.hasExtra("fromNotification")) {
                Logger.d(TAG, "onNewIntent(): PushNotification.fromNotification !!!!!!!");
            }
        }
        super.onNewIntent(intent);
    }

    private boolean processIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final String url = intent.getDataString();
            Logger.d(TAG, "Got user activation url: " + url);
            intent.setAction(Intent.ACTION_DEFAULT);
            intent.setData(null);
            setIntent(intent);
            settings.setNeedShowGetAccessScreenOnStart(false);
            new ActivateUserTask().execute(url);
            return true;
        }
        return false;
    }

    public void dim(final int alphaLevel) {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(alphaLevel);
        }
    }

    public void undim() {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(0);
        }
    }

    public FrameLayout getDimmableView() {
        return (FrameLayout) findView(R.id.view_dimmer);
    }

    public void navigateToJournalIssues() {
        openJournalTab(R.id.issues_tab);
    }

    public void navigateToJournalSavedArticles() {
        openJournalTab(R.id.saved_articles_tab);
    }

    public void navigateToJournalGlobalSearch() {
        openJournalTab(R.id.global_search_tab);
    }

    public void navigateToJournalEarlyView() {
        openJournalTab(R.id.early_view_tab);
    }

    public void navigateToJournalSettings() {
        if (DeviceUtils.isTablet(this)) {
            openJournalTab(R.id.settings_tab);
        } else {
            Fragment fragment = getFragmentByClass(SettingsFragment.class);
            showFragment(fragment, null);
            slidingMenu.showContent();
            notificationCenter.sendNotification(EventList.SETTINGS_WINDOW_IS_SHOWN.getEventName());
        }
    }

    public void navigateToJournalAbout() {
        openJournalTab(R.id.info_tab);
    }

    public void navigateToSpecialSection() {
        openJournalTab(R.id.special_sections_tab);
    }

    private void openJournalTab(int tabId) {
        removeAdditionalJournalFragments();
        showCurrentJournalFragment();

        final Fragment fragment = journalFragmentsStack.peek();
        if (fragment.getClass() == JournalMainFragment.class) {
            ((JournalMainFragment) fragment).changeCurrentTab(tabId);
        } else {
            journalFragmentsStack.pop();
            Fragment journalMainFragment = journalFragmentsStack.peek();
            journalFragmentsStack.push(fragment);
            ((JournalMainFragment) journalMainFragment).changeCurrentTab(tabId);
        }
    }

    public void navigateToSocietyHome() {
        aanHelper.trackSocietyHome();
        removeAdditionalSocietyFragments();
        showCurrentSocietyFragment(null);
    }

    public void navigateToSocietyFavorites() {
        aanHelper.trackSocietyFavorites();
        removeAdditionalSocietyFragments();
        final Fragment currentSocietyFragment = societyNewsFragmentsStack.peek();
        societyNewsFragmentsStack.push(getFragmentByClass(SocietyFavoritesFragment.class));
        showCurrentSocietyFragment(currentSocietyFragment);
    }

    public void navigateToSocietyFeed(FeedMO feed) {
        removeAdditionalSocietyFragments();
        getIntent().putExtra(Extras.EXTRA_FEED_UID, feed.getUid());
        final Fragment currentSocietyFragment = societyNewsFragmentsStack.peek();

        prepareIntentForOpeningFeed(feed.getUid());

        societyNewsFragmentsStack.push(getFragmentByClass(FeedListViewerFragment.class));
        showCurrentSocietyFragment(currentSocietyFragment);
    }

    public void navigateToSocietyGlobalSearch() {
        aanHelper.trackSocietyContentSearch();
        removeAdditionalSocietyFragments();
        final Fragment currentSocietyFragment = societyNewsFragmentsStack.peek();
        societyNewsFragmentsStack.push(getFragmentByClass(SocietyGlobalSearchFragment.class));
        showCurrentSocietyFragment(currentSocietyFragment);
    }

    private void prepareIntentForOpeningFeed(String rssFeedUid) {
        final Collection<FeedMO> feeds = homePageService.getFeeds();

        final ArrayList<String> feedUidList = new ArrayList<>(feeds.size());
        for (FeedMO storedFeed : feeds) {
            if (storedFeed.getTitle() != null && !storedFeed.getTitle().isEmpty()) {
                feedUidList.add(storedFeed.getUid());
            }
        }

        Intent intent = getIntent();
        intent.putExtra(Extras.EXTRA_FEED_UID, rssFeedUid);

        intent.putStringArrayListExtra(Extras.EXTRA_FEED_LIST, feedUidList);
    }

    private void showCurrentJournalFragment() {
        final SocietyNavigationPanel navigationPanel = (SocietyNavigationPanel) getSupportFragmentManager()
                .findFragmentById(R.id.society_navigation_panel);
        if (navigationPanel != null && navigationPanel.isSocietyNewsTabActive()) {
            navigationPanel.navigateToJournal();
        } else {
            showFragment(journalFragmentsStack.peek(), societyNewsFragmentsStack.peek().isVisible() ?
                    societyNewsFragmentsStack.peek() : null);
        }
    }

    private void showCurrentSocietyFragment(Fragment currentSocietyFragment) {
        final SocietyNavigationPanel navigationPanel = (SocietyNavigationPanel) getSupportFragmentManager()
                .findFragmentById(R.id.society_navigation_panel);
        if (navigationPanel != null && !navigationPanel.isSocietyNewsTabActive()) {
            navigationPanel.navigateToSocietyNews();
        } else {
            showFragment(societyNewsFragmentsStack.peek(), journalFragmentsStack.peek().isVisible() ?
                    journalFragmentsStack.peek() : currentSocietyFragment);
            if (DeviceUtils.isPhone(this)) {
                if (slidingMenu != null) {
                    slidingMenu.showContent(true);
                }
            }
        }
        onSocietyPageNavigated();
    }

    public void onSettingWindowClosed() {
        if (journalFragmentsStack.peek().isVisible()) {
            journalFragmentsStack.peek().onHiddenChanged(false);
            onJournalPageNavigated();
        } else if (societyNewsFragmentsStack.peek().isVisible()) {
            societyNewsFragmentsStack.peek().onHiddenChanged(false);
            onSocietyPageNavigated();
        }
    }

    public void onSocietyPageNavigated() {
        settings.setSocietyPageByDefault(true);
        final HashMap<String, Object> params = new HashMap<>();
        if (societyNewsFragmentsStack.isEmpty()) {
            return;
        }

        if (societyNewsFragmentsStack.size() == 1) {
            params.put(IS_HOME_SHOWN, true);
        } else {
            if (societyNewsFragmentsStack.peek().getClass() == SocietyFavoritesFragment.class) {
                params.put(IS_SOCIETY_FAVORITES_SHOWN, true);
            } else if (societyNewsFragmentsStack.peek().getClass() == SocietyGlobalSearchFragment.class) {
                params.put(IS_GLOBAL_SEARCH_SHOWN, true);
            } else if (societyNewsFragmentsStack.peek().getClass() == FeedListViewerFragment.class) {
                FeedMO feed = ((FeedListViewerFragment) societyNewsFragmentsStack.peek()).getCurrentFeed();
                params.put(FEED_MO, feed);
            } else if (societyNewsFragmentsStack.peek().getClass() == FeedItemsViewerFragment.class) {
                List<String> itemsList = BundleUtils.getListFromBundle(getIntent().getExtras(), Extras.EXTRA_FEED_ITEMS_LIST);
                if (itemsList.size() > 0) {
                    FeedItemMO feedItem = homePageService.getFeedItem(itemsList.get(0));
                    if (feedItem != null) {
                        params.put(FEED_MO, feedItem.getFeed());
                    }
                }
            }
        }

        notificationCenter.sendNotification(EventList.SOCIETY_PAGE_NAVIGATED.getEventName(), params);
    }

    public void onJournalPageNavigated() {
        final HashMap<String, Object> params = new HashMap<>();
        params.put(CURRENT_TAB_ID, journal.getActiveTabId());
        settings.setSocietyPageByDefault(false);
        notificationCenter.sendNotification(EventList.JOURNAL_PAGE_NAVIGATED.getEventName(), params);
    }

    private void removeAdditionalJournalFragments() {
        while (journalFragmentsStack.size() > 1) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (journalFragmentsStack.peek().getClass() == GetAccessDialogFragment.class) {
                return;
            }
            ft.remove(journalFragmentsStack.pop()).commit();
        }
    }

    private void removeAdditionalSocietyFragments() {
        while (societyNewsFragmentsStack.size() > 1) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(societyNewsFragmentsStack.pop()).commit();
        }
    }

    public void highlightSocietyFavoriteMenuItem() {
        highlightMenuItem(new Runnable() {
            @Override
            public void run() {
                phoneExpandableMainMenu.highlightSocietyFavoriteMenuItem();
            }
        });
    }

    public void highlightJournalFavoriteMenuItem() {
        highlightMenuItem(new Runnable() {
            @Override
            public void run() {
                phoneExpandableMainMenu.highlightJournalFavoriteMenuItem();
            }
        });
    }

    private void highlightMenuItem(Runnable menuItemHighlighter) {
        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onSideMenuButtonClicked();
            }
        }, 200);

        handler.postDelayed(menuItemHighlighter, 700);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onSideMenuButtonClicked();
                phoneExpandableMainMenu.highlightCurrentMenuItem();
            }
        }, 1200);
    }

    @Override
    protected void onSoftKeyboardVisibleChanged(boolean visible) {
        if (journalFragmentsStack.peek().isVisible()) {
            for (Fragment fragment : journalFragmentsStack) {
                if (fragment.getClass() == JournalMainFragment.class) {
                    ((JournalMainFragment) fragment).onSoftKeyboardVisibleChanged(visible);
                    break;
                }
            }
        }
    }

    public void postSaveArticle(ArticleMO article) {
        articlesToSave.put(article.getUid(), article);
    }

    public void cancelPostSaveArticle(ArticleMO article) {
        articlesToSave.remove(article.getUid());
    }

    private final class ActivateUserTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            final String query = params[0];
            return authorizationService.activateUser(query);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (isFinishing()) {
                return;
            }
            hideProgress();
            if (success && !settings.isAuthorized()) {
                final String accessCode = settings.getAccessCode();
                if (accessCode != null) {
                    startActivity(OauthAuthorizationActivity.getStartingIntent(getApplicationContext(), true));
                } else {
                    startActivity(OauthAuthorizationActivity.getStartingIntent(getApplicationContext(), false));
                }
            }
        }

        private void showProgress() {
            final Fragment currentFragment = journalFragmentsStack.peek();
            if (currentFragment.getClass() == JournalMainFragment.class) {
                ((JournalMainFragment) currentFragment).showProgress("Activating user...");
            }
        }

        private void hideProgress() {
            final Fragment currentFragment = journalFragmentsStack.peek();
            if (currentFragment.getClass() == JournalMainFragment.class) {
                ((JournalMainFragment) currentFragment).hideProgress();
            }
        }
    }
}
