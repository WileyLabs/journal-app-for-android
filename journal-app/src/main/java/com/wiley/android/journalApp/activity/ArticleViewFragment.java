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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.adapter.ArticleInfoAdapter;
import com.wiley.android.journalApp.adapter.FiguresAdapter;
import com.wiley.android.journalApp.adapter.ReferencesAdapter;
import com.wiley.android.journalApp.adapter.SupportingInfoAdapter;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.ArticleComponent;
import com.wiley.android.journalApp.components.ArticleComponentHost;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.components.popup.PopupHost;
import com.wiley.android.journalApp.components.search.ArticleSearcher;
import com.wiley.android.journalApp.controller.VideoController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.fragment.articleView.ArticleViewAdvContent;
import com.wiley.android.journalApp.fragment.articleView.ArticleViewContent;
import com.wiley.android.journalApp.fragment.articleView.ArticleViewPageIndicator;
import com.wiley.android.journalApp.fragment.articleView.CenterBarArticleViewContainer;
import com.wiley.android.journalApp.fragment.articleView.LeftBarArticleView;
import com.wiley.android.journalApp.fragment.popups.PopupAuthorsInfo;
import com.wiley.android.journalApp.fragment.popups.PopupCitation;
import com.wiley.android.journalApp.layout.LeftMenu;
import com.wiley.android.journalApp.layout.LeftMenuWithTwoPanels;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.AdViewController;
import com.wiley.android.journalApp.utils.ArticleHolder;
import com.wiley.android.journalApp.utils.BundleUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.android.journalApp.utils.FiguresHandler;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.android.journalApp.widget.CustomViewPager;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.http.DocumentsDownloader;
import com.wiley.wol.client.android.data.http.Resource;
import com.wiley.wol.client.android.data.manager.AdvertisementManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wiley.android.journalApp.error.ErrorButton.OnClickListener;
import static com.wiley.android.journalApp.error.ErrorButton.withTitleAndListener;
import static com.wiley.wol.client.android.data.http.DocumentsDownloader.DOCUMENT_TYPE;
import static com.wiley.wol.client.android.data.http.DocumentsDownloader.DocType;
import static com.wiley.wol.client.android.data.http.DocumentsDownloader.DocType.PDF;
import static com.wiley.wol.client.android.data.http.DocumentsDownloader.DocType.SUPPORTING_INFO;
import static com.wiley.wol.client.android.data.http.DownloadOperation.DOI;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE;
import static com.wiley.wol.client.android.data.manager.ResourceType.ARTICLE_ZIP;
import static com.wiley.wol.client.android.data.service.ArticleService.ARTICLE_MO;
import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ARTICLE;
import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;
import static com.wiley.wol.client.android.notification.EventList.ALL_CONTENT_UPDATE_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_FAVORITE_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_UPDATE_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.DOCUMENT_DOWNLOAD_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.DOCUMENT_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.DOCUMENT_DOWNLOAD_STARTED;
import static com.wiley.wol.client.android.notification.EventList.ERROR_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.CANCELLED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.FILE_PATH;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;
import static com.wiley.wol.client.android.notification.NotificationCenter.SUCCESS;
import static com.wiley.wol.client.android.settings.Settings.SETTING_AUTH_TOKEN;

public class ArticleViewFragment extends JournalFragment implements
        ArticleComponent.Listener,
        PopupHost.PopupListener,
        PopupCitation.CitationListener,
        ArticleHolder,
        ArticleViewContent.Host,
        FiguresHandler,
        ArticleComponentHost,
        StartActivityForResultHelper,
        ArticleViewAdvContent.AdvertisementHandle,
        ActionBarSherlock.OnCreatePanelMenuListener,
        ActionBarSherlock.OnPreparePanelListener,
        ActionBarSherlock.OnMenuItemSelectedListener,
        ActionBarSherlock.OnActionModeStartedListener,
        ActionBarSherlock.OnActionModeFinishedListener,
        Journal {

    private static final String TAG = ArticleViewFragment.class.getSimpleName() + ".Advertisement";
    private static final String TAG_LIFE = ArticleViewFragment.class.getSimpleName() + ".life";

    @Inject
    private AANHelper aanHelper;
    @Inject
    private Settings settings;
    @Inject
    private EmailSender emailSender;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Authorizer authoriser;
    @Inject
    private DocumentsDownloader documentsDownloader;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private WebController webController;
    @Inject
    private VideoController videoController;
    @Inject
    private AdvertisementManager advertisementManager;
    @Inject
    protected Theme theme;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;

    private List<DOI> doiList = new ArrayList<>();
    private int initialDoiIndex = -1;
    private ArticlePageInfo currentPageInfo = null;

    private LeftMenuWithTwoPanels leftMenuWithTwoPanels;
    private LeftMenu leftMenu;
    private CustomViewPager articlesPager;
    private TextView noArticlesTextView;

    private InterfaceHelper mInterfaceHelper;
    private boolean isPhone;
    private boolean showingSavedArticlesList;

    private FiguresAdapter figuresAdapter;
    private SupportingInfoAdapter supportingInfoAdapter;
    private ArticleInfoAdapter articleInfoAdapter;
    private ReferencesAdapter referencesAdapter;
    private ProgressDialog progressDialog;

    private boolean loadPdfOnContentReady;
    private boolean addToFavOnContentReady;

    private boolean userScrollsForward = true;

    private AdViewController adViewController;

    private ActionBarSherlockCompat mSherlock;
    private String mTitle;
    private String searchTerm;
    private CustomFragmentPagerAdapter articlesPagerAdapter;
    private MenuItem setAlertButton;
    private Integer panelIdForOpenOnAdvHidden = null;
    private boolean advShown = false;
    private Integer opennedPanelId = null;
    public boolean tryShowAdsOnNextPageScrolled = false;
    private final ArticleOnPageChangeListener onArticlePagerPageChangeListener = new ArticleOnPageChangeListener();

    private DataSetObserver pagerObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            if (getArticlesCountInList() == 0) {
                onNoArticlesInList();
            } else {
                noArticlesTextView.setVisibility(View.GONE);
            }
        }
    };

    private final NotificationProcessor articleUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final DOI doiFromParams = (DOI) params.get(DOI);
            Logger.d(TAG, "articleUpdateSuccessProcessor, DOI: " + doiFromParams);
            if (currentPageInfo != null
                    && currentPageInfo.type == ArticlePageInfo.Type.Content
                    && currentPageInfo.doi.equals(doiFromParams)) {
                ((ArticleViewContent) currentPageInfo.contentFragment).onArticleUpdated();
                initializeSliderAdapters(currentPageInfo.doi);
                getSideMenu().updateUi(currentPageInfo.doi);
            }

            if (isPhone || mInterfaceHelper.isSearcherShowed()) {
                mInterfaceHelper.doSearch();
            }
        }
    };

    private final NotificationProcessor articleUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final DOI doiFromParams = (DOI) params.get(DOI);
            if (currentPageInfo != null
                    && currentPageInfo.type == ArticlePageInfo.Type.Content
                    && currentPageInfo.doi.equals(doiFromParams)) {
                ((ArticleViewContent) currentPageInfo.contentFragment).onArticleUpdateError();
                errorManager.alertWithException(ArticleViewFragment.this.getActivity(), (Throwable) params.get(ERROR));
            }
        }
    };

    private final NotificationProcessor savedArticlesListChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            mSherlock.dispatchInvalidateOptionsMenu();
            if (!showingSavedArticlesList) {
                return;
            }
            ArticleMO article = (ArticleMO) params.get(ARTICLE_MO);
            if (article.isFavorite()) {
                addArticleToList(article.getDOI());
            } else {
                removeArticleFromList(article.getDOI());
            }
        }
    };

    private final NotificationProcessor settingsChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            Logger.d(TAG, "settingsChangedProcessor");
            final String settingName = params.get(SETTING_NAME_KEY).toString();
            if (SETTING_AUTH_TOKEN.equals(settingName)) {
                progressDialog.show();
            }
        }
    };

    private final NotificationProcessor articleDownloadErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final Resource resource = (Resource) params.get(RESOURCE);
            if (ARTICLE_ZIP == resource.getResourceType()) {
                final DOI doiFromParams = (DOI) params.get(DOI);
                Logger.d(TAG, "articleDownloadErrorProcessor, DOI: " + doiFromParams);
                if (currentPageInfo != null
                        && currentPageInfo.type == ArticlePageInfo.Type.Content
                        && currentPageInfo.doi.equals(doiFromParams)) {
                    ((ArticleViewContent) currentPageInfo.contentFragment).onArticleUpdateError();
                    errorManager.alertWithException(ArticleViewFragment.this.getActivity(), (Throwable) params.get(ERROR));
                }
            }
        }
    };

    private final NotificationProcessor feedsUpdatedSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            progressDialog.dismiss();
            if (getCurrentArticleViewContent() != null) {
                getCurrentArticleViewContent().loadAndShowArticle();
            }
        }
    };

    private final NotificationProcessor docDownloadStartedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final DocType type = (DocType) params.get(DOCUMENT_TYPE);
            if (type == PDF) {
                mInterfaceHelper.onPdfDownloadStarted();
            } else if (type == SUPPORTING_INFO) {
                mInterfaceHelper.onSupportingInfoDownloadStarted();
            }
        }
    };

    private final NotificationProcessor docDownloadFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final boolean cancelled = params.containsKey(CANCELLED) && (boolean) params.get(CANCELLED);
            final DocType type = (DocType) params.get(DOCUMENT_TYPE);
            if (cancelled) {
                trackActionByType(type);
                mInterfaceHelper.hideProgress();
                return;
            }
            final boolean success = (boolean) params.get(SUCCESS);
            if (success) {
                final String path = (String) params.get(FILE_PATH);
                mInterfaceHelper.onDocumentDownloadFinished(path, type);
            } else {
                mInterfaceHelper.onDocumentDownloadError((AppErrorCode) params.get(ERROR));
            }
        }
    };

    private void trackActionByType(DocType type) {
        if (PDF == type) {
            aanHelper.trackActionCancelPDFDownload(getCurrentArticle());
        } else if (SUPPORTING_INFO == type) {
            aanHelper.trackActionCancelSupportingInfoDownload(getCurrentArticle());
        }
    }

    private final NotificationProcessor docDownloadProgressProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            mInterfaceHelper.onDocumentDownloadProgress((Integer) params.get("progress"));
        }
    };

    private final ReferencesAdapter.Host referencesAdapterHost = new ReferencesAdapter.Host() {
        @Override
        public PopupHost getPopupHost() {
            return mInterfaceHelper.mPopupHost;
        }

        @Override
        public ListView getListView() {
            return getCenterMenu().getListFragment().getListView();
        }
    };

    public void changeFontSize(int size) {
        ArticleComponent currentArticleComponent = getCurrentArticleComponent();
        if (currentArticleComponent != null) {
            currentArticleComponent.changeFontSize(size);
        }
    }

    public void hidePopup() {
        mInterfaceHelper.hidePopup(false);
    }

    @Override
    public void onArticleViewContentReady(final ArticleViewContent sender) {
        Logger.d(TAG, "onArticleViewContentReady()");
        for (final ArticlePageInfo pageInfo : articlePageInfos) {
            if (pageInfo.contentFragment == sender) {
                if (pageInfo.doi.equals(getCurrentDoi())) {
                    reInitViewsForNewArticlePage(pageInfo);
                }
                break;
            }
        }

        if (DeviceUtils.isPhone(getContext())) {
            ArticleViewContent articleViewContent = getCurrentArticleViewContent();
            if (null != articleViewContent) {
                int visibility = needToHideSetAlertButton() ? View.GONE : View.VISIBLE;
                findView(R.id.alert_quick_link_menu_button).setVisibility(visibility);

                final CustomWebView webView = articleViewContent.getWebView();
                final CustomWebView.OnScrollListener onScrollListener = webView.getOnScrollListener();
                webView.setOnScrollListener(new CustomWebView.OnScrollListener() {
                    @Override
                    public void onScrollStarted(int l, int t) {
                        onScrollListener.onScrollStarted(l, t);
                        if (mSherlock.getActionBar().isShowing()) {
                            showQuickLinkMenu();
                        }
                    }

                    @Override
                    public void onScrollChanged(int l, int t, int oldl, int oldt) {
                        onScrollListener.onScrollChanged(l, t, oldl, oldt);
                        ArticleViewContent articleViewContent = getCurrentArticleViewContent();
                        if (null != articleViewContent) {
                            articleViewContent.markArticleAsReadIfNeeded();
                        }
                    }

                    @Override
                    public void onScrollEnded(int l, int t) {
                        onScrollListener.onScrollEnded(l, t);
                    }
                });
            }
        }
    }

    private boolean isAdvShown() {
        return advShown;
    }

    private void onAdvShown() {
        Logger.d(TAG, "onAdvShown()");
        advShown = true;
        if (!isPhone) {
            articlesPager.setTouchesEnabled(false);
        }
        getActivity().getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        panelIdForOpenOnAdvHidden = opennedPanelId;
        hideLeftTabMenu();
        quickLinkMenuComponent.hideQuickLinkMenu();
        if (!isPhone) {
            ((MainActivity) getActivity()).hideSocietyNavigationPanel();
            getActivity().setRequestedOrientation(DeviceUtils.getRotation(getContext()));
        }
        updateActionBarVisible();
        updatePageIndicatorVisible();
        adViewController.updateLayout();
        advertisementManager.resetIgnoringFirstAdArticleView();
        mInterfaceHelper.onAdvShown();
    }

    private void onAdvHidden() {
        Logger.d(TAG, "onAdvHidden()");
        advShown = false;
        if (!isPhone) {
            articlesPager.setTouchesEnabled(false);
        }
        getActivity().getWindow().getDecorView().setBackgroundColor(Color.WHITE);
        opennedPanelId = panelIdForOpenOnAdvHidden;
        showLeftTabMenu();
        if (!isPhone) {
            ((MainActivity) getActivity()).showSocietyNavigationPanel();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        updateActionBarVisible();
        updatePageIndicatorVisible();
        mInterfaceHelper.onAdvHidden();
    }

    @Override
    public AdViewController getAdViewController() {
        return adViewController;
    }

    @Override
    public void onAdvCloseButtonClick() {
        int nextPos = articlesPager.getCurrentItem() + 1;
        int prevPos = nextPos - 2;
        int newPos = userScrollsForward ? nextPos : prevPos;
        newPos = newPos == articlesPagerAdapter.getCount() ? prevPos : newPos;
        newPos = newPos < 0 ? nextPos : newPos;
        articlesPager.setCurrentItem(newPos, true);
    }

    private static class ArticlePageInfo {
        private enum Type {
            Ad,
            Content
        }

        private final int id;
        private final Type type;
        private final DOI doi;
        private Fragment contentFragment = null;

        private ArticlePageInfo(final Type type, final DOI doi) {
            this.id = IdUtils.generateIntId();
            this.type = type;
            this.doi = doi;
        }

        private static ArticlePageInfo ad() {
            return new ArticlePageInfo(Type.Ad, null);
        }

        private static ArticlePageInfo content(final DOI doi) {
            return new ArticlePageInfo(ArticlePageInfo.Type.Content, doi);
        }
    }

    private final List<ArticlePageInfo> articlePageInfos = new ArrayList<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG_LIFE, "onCreate");

        notificationCenter.subscribeToNotification(SETTINGS_CHANGED.getEventName(), settingsChangedProcessor);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.article_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        articlesPagerAdapter = new CustomFragmentPagerAdapter(getChildFragmentManager());
        initContentView(savedInstanceState);
    }

    protected void initContentView(final Bundle savedInstanceState) {
        getActivity().getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        isPhone = DeviceUtils.isPhone(this.getActivity());
        this.leftMenuWithTwoPanels = findView(R.id.menu_two_panels);
        this.leftMenu = findView(R.id.menu);

        progressDialog = new ProgressDialog(this.getActivity());
        progressDialog.setMessage(getString(R.string.refreshing_content));
        progressDialog.setCancelable(false);

        noArticlesTextView = findView(R.id.article_view_empty_list_placeholder);
        noArticlesTextView.setVisibility(View.GONE);

        processIntent();

        if (savedInstanceState == null) {
            final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.add(R.id.menu_side, new LeftBarArticleView());
            ft.add(R.id.menu_center, new CenterBarArticleViewContainer());
            if (findView(R.id.page_indicator_host) != null && doiList.size() >= 3) {
                final ArticleViewPageIndicator pageIndicator = new ArticleViewPageIndicator();
                final Bundle args = new Bundle();
                args.putInt(ArticleViewPageIndicator.Extra_ViewPagerId, R.id.article_content_host);
                args.putInt(ArticleViewPageIndicator.Extra_PopupHostId, R.id.article_popup_host);
                BundleUtils.putParcelableListToBundle(args, ArticleViewPageIndicator.Extra_DoiList, this.doiList);
                pageIndicator.setArguments(args);
                ft.add(R.id.page_indicator_host, pageIndicator);
            }
            ft.commit();
            getChildFragmentManager().executePendingTransactions();
        }

        articlesPager = findView(R.id.article_content_host);
        articlesPager.setAdapter(articlesPagerAdapter);
        if (getPageIndicator() != null) {
            getPageIndicator().setOnIndicatorChangeListener(onArticlePagerIndicatorChangeListener);
        }
        articlesPager.setOnPageChangeListener(onArticlePagerPageChangeListener);

        if (leftMenuWithTwoPanels != null) {
            leftMenuWithTwoPanels.setListener(leftMenuWithTwoPanelsListener);
        }

        if (leftMenu != null) {
            leftMenu.setListener(leftMenuListener);
            leftMenu.setResizeContent(DeviceUtils.isLandscape(this.getActivity()));
        }

        mInterfaceHelper = new InterfaceHelper();
        figuresAdapter = new FiguresAdapter(this.getActivity());
        supportingInfoAdapter = new SupportingInfoAdapter(this.getActivity());
        referencesAdapter = new ReferencesAdapter(this.getJournalActivity(), referencesAdapterHost);

        doiIndexForShowOnStart = this.initialDoiIndex;

        adViewController = new AdViewController(this.getJournalActivity(), settings.getAdvertisementConfig().getAdUnitId());

        // action bar
        mSherlock = ActionBarUtils.initActionBar(getJournalActivity(), mTitle, this, theme);

        // feature: quick link menu
        if (isPhone) {
            // touch layout
            ((TouchRefreshLayout) findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                }
            });

            quickLinkMenuComponent.initQuickLink(getActivity(), this);
            quickLinkMenuComponent.setAlertButtonOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final View button = findView(R.id.articleBarInfoButton);
                    onBarButtonClick(button);
                }
            });
        }
    }

    private int doiIndexForShowOnStart = 0;

    @Override
    public void onStart() {
        Logger.d(TAG_LIFE, "onStart");
        super.onStart();
        mInterfaceHelper.onActivityStart();

        notificationCenter.subscribeToNotification(ARTICLE_UPDATE_SUCCESS.getEventName(), articleUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(ARTICLE_UPDATE_ERROR.getEventName(), articleUpdateErrorProcessor);
        notificationCenter.subscribeToNotification(ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), savedArticlesListChangedProcessor);
        notificationCenter.subscribeToNotification(ERROR_RESOURCE_DOWNLOADING.getEventName(), articleDownloadErrorProcessor);
        notificationCenter.subscribeToNotification(ALL_CONTENT_UPDATE_FINISHED.getEventName(), feedsUpdatedSuccessProcessor);
        notificationCenter.subscribeToNotification(DOCUMENT_DOWNLOAD_STARTED.getEventName(), docDownloadStartedProcessor);
        notificationCenter.subscribeToNotification(DOCUMENT_DOWNLOAD_FINISHED.getEventName(), docDownloadFinishedProcessor);
        notificationCenter.subscribeToNotification(DOCUMENT_DOWNLOAD_PROGRESS.getEventName(), docDownloadProgressProcessor);

        if (doiIndexForShowOnStart < 0) {
            doiIndexForShowOnStart = 0;
        }
        if (doiIndexForShowOnStart > articlePageInfos.size() - 1) {
            doiIndexForShowOnStart = articlePageInfos.size() - 1;
        }

        if (advertisementManager.isTimeToShowAd() && adViewController.isReadyToShowAd()) {
            articlesPagerAdapter.setAdvertisementAt(doiIndexForShowOnStart, false);
        }
        articlesPager.setCurrentItem(doiIndexForShowOnStart, false);

        onArticlePageChanged(doiIndexForShowOnStart);
        tryShowAdsOnNextPageScrolled = true;

        articlesPagerAdapter.registerDataSetObserver(pagerObserver);
    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();
        if (mSherlock.getActionBar().isShowing() && !mInterfaceHelper.isAuthorsInfoOpened()) {
            showQuickLinkMenu();
        }

        if (mInterfaceHelper.isSearcherHasFocus()) {
            mInterfaceHelper.showSearcherKeyboard();
        }
    }

    @Override
    public void onStop() {
        Logger.d(TAG_LIFE, "onStop");
        mInterfaceHelper.onActivityStop();
        notificationCenter.unSubscribeFromNotification(articleUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(articleUpdateErrorProcessor);
        notificationCenter.unSubscribeFromNotification(savedArticlesListChangedProcessor);
        notificationCenter.unSubscribeFromNotification(articleDownloadErrorProcessor);
        notificationCenter.unSubscribeFromNotification(feedsUpdatedSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(docDownloadStartedProcessor);
        notificationCenter.unSubscribeFromNotification(docDownloadFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(docDownloadProgressProcessor);
        articlesPagerAdapter.unregisterDataSetObserver(pagerObserver);
        if (null != articleInfoAdapter) {
            articleInfoAdapter.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG_LIFE, "onDestroy");
        notificationCenter.unSubscribeFromNotification(settingsChangedProcessor);
        adViewController.destroy();
        super.onDestroy();
    }

    public CenterBarArticleViewContainer getCenterMenu() {
        return (CenterBarArticleViewContainer) getChildFragmentManager().findFragmentById(R.id.menu_center);
    }

    public LeftBarArticleView getSideMenu() {
        return (LeftBarArticleView) getChildFragmentManager().findFragmentById(R.id.menu_side);
    }

    public ArticleViewPageIndicator getPageIndicator() {
        return (ArticleViewPageIndicator) getChildFragmentManager().findFragmentById(R.id.page_indicator_host);
    }

    public DOI getCurrentDoi() {
        if (currentPageInfo == null) {
            return null;
        }
        return currentPageInfo.doi;
    }

    /**
     * feature: quick link menu
     */
    private void showQuickLinkMenu() {
        if (isPhone && !advShown) {
            quickLinkMenuComponent.showQuickLinkMenu();
        }
    }

    private void processIntent() {
        this.doiList = BundleUtils.getParcelableListFromBundle(getActivity().getIntent().getExtras(), Extras.EXTRA_DOI_LIST);
        this.initialDoiIndex = getActivity().getIntent().getIntExtra(Extras.EXTRA_INITIAL_DOI_INDEX, 0);
        this.showingSavedArticlesList = getActivity().getIntent().getBooleanExtra(Extras.EXTRA_SAVED_ARTICLES, false);
        this.searchTerm = getActivity().getIntent().getStringExtra(Extras.EXTRA_SEARCH_TERM);

        this.articlePageInfos.clear();
        for (DOI doi : doiList) {
            this.articlePageInfos.add(ArticlePageInfo.content(doi));
        }

        mTitle = getActivity().getIntent().getStringExtra(Extras.EXTRA_TITLE);
    }

    public boolean isShowingSavedArticlesList() {
        return showingSavedArticlesList;
    }

    private void onNoArticlesInList() {
        adViewController.destroy();
        hidePanels();
        mInterfaceHelper.hideAuthorsInfo();
        mInterfaceHelper.hidePopup(false);
        leftMenuWithTwoPanels = null;
        getActivity().onBackPressed();
    }

    private void removeArticleFromList(DOI doi) {
        this.doiList = BundleUtils.getParcelableListFromBundle(getActivity().getIntent().getExtras(), Extras.EXTRA_DOI_LIST);
        if (doiList.contains(doi)) {
            doiList.remove(doi);
            BundleUtils.putParcelableListToIntent(getActivity().getIntent(), Extras.EXTRA_DOI_LIST, doiList);
            updatePageIndicatorDoiList(doiList);
        }
        ArticlePageInfo removeInfo = null;
        for (ArticlePageInfo info : articlePageInfos) {
            if (info.type == ArticlePageInfo.Type.Content && info.doi.equals(doi)) {
                removeInfo = info;
                break;
            }
        }
        if (removeInfo == null) {
            return;
        }
        if (currentPageInfo == removeInfo) {
            int nextIndex = findNextItemIndex(articlesPager.getCurrentItem());
            currentPageInfo = null;
            articlesPagerAdapter.notifyDataSetChanged();
            articlesPager.setCurrentItem(nextIndex, false);
        }

        articlePageInfos.remove(removeInfo);
        updatePageIndicatorVisible();
        articlesPagerAdapter.notifyDataSetChanged();
    }

    private int findNextItemIndex(int currentIndex) {
        int nextIndex = currentIndex + 1;
        if (nextIndex < articlePageInfos.size()) {
            if (articlePageInfos.get(nextIndex).type == ArticlePageInfo.Type.Content) {
                return nextIndex;
            } else {
                articlePageInfos.remove(nextIndex);
                articlesPagerAdapter.advertisementPosition = -1;
                return findNextItemIndex(currentIndex);
            }
        } else if (currentIndex > 0) {
            nextIndex = currentIndex - 1;
            if (articlePageInfos.get(nextIndex).type == ArticlePageInfo.Type.Content) {
                return nextIndex;
            } else {
                articlesPagerAdapter.advertisementPosition = -1;
                articlePageInfos.remove(nextIndex);
                return findNextItemIndex(currentIndex - 1);
            }
        } else {
            return 0;
        }
    }

    private void addArticleToList(DOI doi) {
        final List<DOI> doiList = BundleUtils.getParcelableListFromBundle(getActivity().getIntent().getExtras(), Extras.EXTRA_DOI_LIST);
        if (!doiList.contains(doi)) {
            doiList.add(doi);
            BundleUtils.putParcelableListToIntent(getActivity().getIntent(), Extras.EXTRA_DOI_LIST, doiList);
            updatePageIndicatorDoiList(doiList);
        }
        for (ArticlePageInfo info : articlePageInfos) {
            if (info.type == ArticlePageInfo.Type.Content && info.doi.equals(doi)) {
                return;
            }
        }
        articlePageInfos.add(ArticlePageInfo.content(doi));
        if (getArticlesCountInList() == 3) {
            showPageIndicator();
        }
        articlesPagerAdapter.notifyDataSetChanged();
    }

    private int getArticlesCountInList() {
        int count = 0;
        for (ArticlePageInfo info : articlePageInfos) {
            if (info.type == ArticlePageInfo.Type.Content) {
                count++;
            }
        }
        return count;
    }

    private class CustomFragmentPagerAdapter extends FragmentPagerAdapter {

        private int advertisementPosition = -1;

        public CustomFragmentPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            final ArticlePageInfo pageInfo = articlePageInfos.get(position);
            final Fragment fragment;
            switch (pageInfo.type) {
                case Ad:
                    Logger.d(TAG, "getItem(): type = Ad; position: " + position);
                    fragment = new ArticleViewAdvContent();
                    String adUnitId = settings.getAdvertisementConfig().getAdUnitId();
                    setArgumentToFragment(fragment, Extras.EXTRA_AD_UNIT_ID, adUnitId);
                    break;
                case Content:
                    Logger.d(TAG, "getItem(): type = Content; position: " + position);
                    fragment = new ArticleViewContent();
                    getSideMenu().updateUi(pageInfo.doi);
                    setArgumentToFragment(fragment, Extras.EXTRA_DOI, pageInfo.doi);
                    break;
                default:
                    fragment = null;
                    break;
            }

            return fragment;
        }

        private void setArgumentToFragment(Fragment fragment, String key, String value) {
            final Bundle args = new Bundle();
            args.putString(key, value);
            fragment.setArguments(args);
        }

        private void setArgumentToFragment(Fragment fragment, String key, Parcelable value) {
            final Bundle args = new Bundle();
            args.putParcelable(key, value);
            fragment.setArguments(args);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object result = super.instantiateItem(container, position);
            articlePageInfos.get(position).contentFragment = (Fragment) result;
            return result;
        }

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            Logger.d(TAG, "destroyItem() position: " + position);
            super.destroyItem(container, position, object);
            for (ArticlePageInfo info : articlePageInfos) {
                if (info.contentFragment == object) {
                    if (info.contentFragment instanceof ArticleViewContent) {
                        ((ArticleViewContent) info.contentFragment).onRemovedFromPager();
                    }
                    info.contentFragment = null;
                    return;
                }
            }
        }

        @Override
        public int getCount() {
            return articlePageInfos.size();
        }

        @Override
        public int getItemPosition(final Object object) {
            Logger.d(TAG, "getItemPosition() object: " + object);
            Fragment fragment = (Fragment) object;
            for (int i = 0; i < articlePageInfos.size(); i++) {
                if (articlePageInfos.get(i).contentFragment == fragment) {
                    return i;
                }
            }
            return POSITION_NONE;
        }

        @Override
        public long getItemId(int position) {
            return articlePageInfos.get(position).id;
        }

        public boolean isAdvertisementShown() {
            return this.advertisementPosition == articlesPager.getCurrentItem();
        }

        public void setAdvertisementAt(final int newAdvertisementPosition, boolean movePositionIfNeed) {
            Logger.d(TAG, "setAdvertisementAt() position: " + newAdvertisementPosition);
            if (isAdvertisementShown()) {
                return;
            }

            if (this.advertisementPosition != newAdvertisementPosition) {
                if (this.advertisementPosition >= 0) {
                    removeAdvertisement();
                }
                this.advertisementPosition = newAdvertisementPosition;
                articlePageInfos.add(advertisementPosition, ArticlePageInfo.ad());
                notifyDataSetChanged();
                if (movePositionIfNeed && advertisementPosition == articlesPager.getCurrentItem()) {
                    articlesPager.setCurrentItem(advertisementPosition + 1, false);
                }
            }
        }

        public void resetAdvertisement() {
            Logger.d(TAG, "resetAdvertisement()");
            if ((advertisementPosition >= 0) && Math.abs(articlesPager.getCurrentItem() - advertisementPosition) > 1) {
                removeAdvertisement();
            }
        }

        private void removeAdvertisement() {
            Logger.d(TAG, "removeAdvertisement()");
            articlePageInfos.get(this.advertisementPosition).contentFragment = null;
            articlePageInfos.remove(this.advertisementPosition);
            this.advertisementPosition = -1;
            notifyDataSetChanged();
        }
    }

    private final ArticleViewPageIndicator.OnIndicatorChangeListener onArticlePagerIndicatorChangeListener = new ArticleViewPageIndicator.OnIndicatorChangeListener() {
        @Override
        public void onNeedScrollToDoi(DOI doi) {
            int pagePosition = -1;
            for (int i = 0; i < articlePageInfos.size(); i++) {
                if (doi.equals(articlePageInfos.get(i).doi)) {
                    pagePosition = i;
                    break;
                }
            }

            if (pagePosition < 0) {
                return;
            }

            if (advertisementManager.isTimeToShowAd()) {
                if (articlesPagerAdapter.isAdvertisementShown()) {
                    advertisementManager.increaseCounter();
                } else {
                    if (adViewController.isReadyToShowAd()) {
                        articlesPagerAdapter.setAdvertisementAt(pagePosition, true);
                        articlesPager.setCurrentItem(pagePosition, false);
                        return;
                    } else {
                        // wait for the ad to load
                        advertisementManager.decreaseCount();
                    }
                }
            }

            tryShowAdsOnNextPageScrolled = false;
            articlesPager.setCurrentItem(pagePosition, true);
        }
    };

    private final class ArticleOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private boolean scrolling = false;
        private boolean pageChanged = false;
        private int prevPosition = -1;

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            if (mInterfaceHelper.isAuthorsInfoOpened()) {
                mInterfaceHelper.hideAuthorsInfo();
            }
            if (articlesPagerAdapter.advertisementPosition == position
                    || articlesPagerAdapter.advertisementPosition == position + 1) {
                // if we are moving the adPage or the page before the adPage
                // then move the adView to make it follow viewPager's movement
                adViewController.moveAdView(articlesPager.getMeasuredWidth(), positionOffsetPixels, articlesPagerAdapter.advertisementPosition == position);
            }
            if (tryShowAdsOnNextPageScrolled && positionOffsetPixels > 0) {
                Logger.d(TAG, "onPageScrolled() position: " + position);
                tryShowAdsOnNextPageScrolled = false;

                int advertisementPosition = position + 1;
                if (advertisementManager.isTimeToShowAd()) {
                    if (articlesPagerAdapter.isAdvertisementShown()) {
                        advertisementManager.increaseCounter();
                    } else if (adViewController.isReadyToShowAd()) {
                        articlesPagerAdapter.setAdvertisementAt(advertisementPosition, true);
                    } else {
                        // wait for the ad to load
                        advertisementManager.decreaseCount();
                    }
                }
            }
        }

        @Override
        public void onPageSelected(final int position) {
            Logger.d(TAG, "onPageSelected() position: " + position + " scrolling " + scrolling);

            userScrollsForward = prevPosition < position;
            prevPosition = position;

            if (scrolling) {
                pageChanged = true;
            } else {
                onArticlePageChanged(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            Logger.d(TAG, "onPageScrollStateChanged() state: " + state + ", position: " + articlesPager.getCurrentItem() + ", scrolling: " + scrolling + ", pageChanged: " + pageChanged);
            switch (state) {
                case ViewPager.SCROLL_STATE_DRAGGING:
                case ViewPager.SCROLL_STATE_SETTLING:
                    scrolling = true;
                    break;
                case ViewPager.SCROLL_STATE_IDLE:
                    if (scrolling) {
                        scrolling = false;
                        if (pageChanged) {
                            pageChanged = false;
                            final int currentItem = articlesPager.getCurrentItem();
                            onArticlePageChanged(currentItem);
                        }
                    }
                    tryShowAdsOnNextPageScrolled = true;
                    break;
                default:
                    break;
            }
        }
    }

    private void onArticlePageChanged(final int newIndex) {
        Logger.d(TAG, "onArticlePageChanged() index: " + newIndex);
        if (newIndex < 0) {
            return;
        }

        doiIndexForShowOnStart = newIndex;
        final ArticlePageInfo pageInfo = articlePageInfos.get(newIndex);

        if (this.currentPageInfo != pageInfo) {
            final boolean wasAd = this.currentPageInfo != null && this.currentPageInfo.type == ArticlePageInfo.Type.Ad;
            this.currentPageInfo = pageInfo;
            final boolean isAd = pageInfo.type == ArticlePageInfo.Type.Ad;
            final boolean isContent = pageInfo.type == ArticlePageInfo.Type.Content;

            if (wasAd && !isAd) {
                onAdvHidden();
            }

            if (pageInfo.contentFragment != null && isContent) {
                getCenterMenu().resetAdapter();
                reInitViewsForNewArticlePage(pageInfo);
            }
            loadPdfOnContentReady = false;
            addToFavOnContentReady = false;

            articlesPagerAdapter.resetAdvertisement();
            if (isContent) {
                if (advertisementManager.isTimeToShowAd() || advertisementManager.isNextPageAd()) {
                    adViewController.loadAdForArticle(pageInfo.doi, articleService.getKeywords(pageInfo.doi));
                }
                advertisementManager.increaseCounter();
            }

            if (isAd) {
                adViewController.onAdShow();
            }

            if (pageInfo.doi != null && getPageIndicator() != null) {
                getPageIndicator().setCurrentDoi(pageInfo.doi);
            }

            if (isAd && !wasAd) {
                onAdvShown();
            } else if (setAlertButton != null) {
                if (getCurrentArticle().getKeywords() == null || getCurrentArticle().getKeywords().isEmpty()) {
                    setAlertButton.setVisible(false);
                } else {
                    setAlertButton.setVisible(true);
                }
            }
        }
    }

    private void reInitViewsForNewArticlePage(final ArticlePageInfo pageInfo) {
        assert pageInfo.doi != null;
        Logger.d(TAG, "reInitViewsForNewArticlePage(), pageInfo: " + pageInfo);
        initializeSliderAdapters(pageInfo.doi);
        ArticleViewContent content = (ArticleViewContent) pageInfo.contentFragment;
        content.onNeedArticle();
        content.getArticleComponent().setListener(ArticleViewFragment.this);
        content.getArticleComponent().applyJsNotRespondingFix();
        hideInput();
        if (DeviceUtils.isTablet(this.getActivity()) && opennedPanelId != null) {
            openPanel(opennedPanelId);
        } else if (isPhone) {
            mInterfaceHelper.doSearch();
        }
        getSideMenu().updateUi(pageInfo.doi);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adViewController.onConfigurationChanged(newConfig);
        if (leftMenu != null) {
            leftMenu.setResizeContent(DeviceUtils.isLandscape(this.getActivity()));
        }
        ArticleComponent ac = getCurrentArticleComponent();
        if (ac != null) {
            ac.onConfigurationChanged();
        }
    }

    public ArticleViewContent getCurrentArticleViewContent() {
        Logger.d(TAG, "getCurrentArticleViewContent()");
        if (articlesPager == null) {
            return null;
        }
        if (articlePageInfos.isEmpty()) {
            return null;
        }
        final int currentArticleIndex = articlesPager.getCurrentItem();
        if (currentArticleIndex < 0) {
            return null;
        }
        final ArticlePageInfo pageInfo = articlePageInfos.get(currentArticleIndex);
        if (pageInfo == null) {
            return null;
        }
        if (pageInfo.type != ArticlePageInfo.Type.Content) {
            return null;
        }
        return (ArticleViewContent) pageInfo.contentFragment;
    }

    @Override
    public ArticleComponent getCurrentArticleComponent() {
        final ArticleViewContent content = getCurrentArticleViewContent();
        if (content == null) {
            return null;
        }
        return content.getArticleComponent();
    }

    public void onBarButtonClick(final View view) {
        if (getArticlesCountInList() == 0) {
            return;
        }

        if (onArticlePagerPageChangeListener.scrolling) {
            return;
        }

        final Checkable button = (Checkable) view;
        button.toggle();
        if (button.isChecked()) {
            openPanel(view.getId());
        } else {
            hidePanels();
        }
    }

    private void openPanel(int id) {
        opennedPanelId = id;
        final DOI currentDoi = getCurrentDoi();
        if (currentDoi == null) {
            return;
        }

        final View view = findView(id);
        final Checkable button = (Checkable) view;
        uncheckAllButtonsExcept(button, (ViewGroup) view.getParent());
        button.setChecked(true);
        if (id == R.id.articleBarFiguresButton) {
            if (!articleService.isArticleRestricted(currentDoi)) {
                getCenterMenu().showArticleFiguresContent(figuresAdapter);
            } else {
                getCenterMenu().showPlaceHolderWithMessage(getString(R.string.figures_are_not_available_message));
            }
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_FEATURES,
                    GANHelper.LABEL_FIGURES,
                    0L);
            {
                aanHelper.trackActionSelectArticleSidebar("Figures", getCurrentArticle());
            }
        } else if (id == R.id.articleBarInfoButton) {
            getCenterMenu().showArticleInfoContent(articleInfoAdapter);
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_FEATURES,
                    GANHelper.LABEL_INFO,
                    0L);
            {
                aanHelper.trackActionSelectArticleSidebar("Info", getCurrentArticle());
            }
        } else if (id == R.id.articleBarSearchButton) {
            {
                aanHelper.trackActionSelectArticleSidebar("Find", getCurrentArticle());
            }
            mInterfaceHelper.showSearcher();
            if (isPhone) {
                button.setChecked(false);
                opennedPanelId = null;
                return;
            }
        } else if (id == R.id.articleBarReferencesButton) {
            if (!articleService.isArticleRestricted(currentDoi)) {
                getCenterMenu().showReferencesContent(referencesAdapter);
            } else {
                getCenterMenu().showPlaceHolderWithMessage(getString(R.string.references_are_not_available_message));
            }
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_FEATURES,
                    GANHelper.LABEL_REFERENCES,
                    0L);
            {
                aanHelper.trackActionSelectArticleSidebar("References", getCurrentArticle());
            }
        } else if (id == R.id.articleBarCitationButton) {
            getCenterMenu().showCitation(currentDoi);
            {
                aanHelper.trackActionSelectArticleSidebar("Citation", getCurrentArticle());
            }
        } else if (id == R.id.articleBarEmailButton) {
            {
                aanHelper.trackActionOpenEmailForm(getCurrentArticle().isRestricted() ? "Abstract" : "Fulltext");
            }
            if (isPhone) {
                getCenterMenu().clear();
            }
            mInterfaceHelper.onOpenEmailSender();
            emailSender.sendArticle(currentDoi, this.getActivity());
            button.setChecked(false);
            opennedPanelId = null;
            return;
        } else if (id == R.id.articleBarPdfButton) {
            {
                aanHelper.trackActionSelectArticleSidebar("Get PDF", getCurrentArticle());
            }
            if (isPhone) {
                getCenterMenu().clear();
            }
            final boolean handled = mInterfaceHelper.openPdf();
            if (handled) {
                opennedPanelId = null;
                return;
            }
        } else if (id == R.id.articleBarSupportingInformationButton) {
            getCenterMenu().showArticleSupportingInfoContent(supportingInfoAdapter);
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_FEATURES,
                    GANHelper.LABEL_SUPPORTING_INFO,
                    0L);
            {
                aanHelper.trackActionSelectArticleSidebar("Supporting Information", getCurrentArticle());
            }
        } else {
            getCenterMenu().showPlaceHolderWithMessage(getString(R.string.references_are_not_available_message));
        }

        if (leftMenuWithTwoPanels != null) {
            leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.OpenCenter);
        }
        if (leftMenu != null) {
            leftMenu.changeContentStateAnimated(LeftMenu.ContentState.Open);
        }
    }

    private void hidePanels() {
        hideInput();

        if (leftMenuWithTwoPanels != null &&
                leftMenuWithTwoPanels.getContentState() == LeftMenuWithTwoPanels.ContentState.OpenCenter) {
            leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.OpenSide);
        }
        if (leftMenu != null) {
            leftMenu.changeContentStateAnimated(LeftMenu.ContentState.Closed);
        }
        uncheckAllTabs();
        opennedPanelId = null;
    }

    private void uncheckAllButtonsExcept(final Checkable button, final ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            final Checkable childAt = (Checkable) parent.getChildAt(i);
            if (!childAt.equals(button)) {
                childAt.setChecked(false);
            }
        }
    }

    private void uncheckAllTabs() {
        final ViewGroup tabHolder = findView(R.id.tab_holder);
        if (tabHolder != null) {
            for (int i = 0; i < tabHolder.getChildCount(); i++) {
                ((Checkable) tabHolder.getChildAt(i)).setChecked(false);
            }
        }
    }

    private final ArticleSearcher.Listener searcherListener = new ArticleSearcher.BaseListener() {
        @Override
        public void onShowSearcher() {
            if (leftMenuWithTwoPanels != null) {
                leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.Closed);
            }
        }
    };

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        // TODO: fix this hack
        // No call for super(). Bug on API Level > 11. http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-h
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
        return (FrameLayout) findView(R.id.article_view_dimmer);
    }

    private void initializeSliderAdapters(final DOI doi) {
        ArticleMO article = null;

        if (doi != null) {
            try {
                article = articleService.getArticleFromDao(doi);
                figuresAdapter.setFigures(article.getFiguresSorted());
                supportingInfoAdapter.setItems(article.getSupportingInfoRefs());
                referencesAdapter.setReferences(article.getReferences());
                if (articleInfoAdapter == null) {
                    articleInfoAdapter = new ArticleInfoAdapter(this.getActivity(), article);
                } else {
                    articleInfoAdapter.setArticle(article);
                }
            } catch (ElementNotFoundException e) {
                Logger.d(TAG, "Unable to find article by doi=" + doi.getValue());
            }
        }

        boolean articleIsNotNull = article != null;
        boolean articleIsNotRestricted = articleIsNotNull && !articleService.isArticleRestricted(doi);

        boolean figuresEnabled = articleIsNotRestricted && !article.getFigures().isEmpty();
        boolean referencesEnabled = articleIsNotRestricted && !article.getReferences().isEmpty();
        boolean supportingInfoEnabled = articleIsNotRestricted && !article.getSupportingInfoRefs().isEmpty();
        boolean pdfEnabled = articleIsNotRestricted && article.hasPdf();

        findView(R.id.article_bar_info_img).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_info_text).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_figures_img).setEnabled(figuresEnabled);
        findView(R.id.article_bar_figures_text).setEnabled(figuresEnabled);
        findView(R.id.article_bar_refs_img).setEnabled(referencesEnabled);
        findView(R.id.article_bar_refs_text).setEnabled(referencesEnabled);
        findView(R.id.article_bar_support_info_img).setEnabled(supportingInfoEnabled);
        findView(R.id.article_bar_support_info_text).setEnabled(supportingInfoEnabled);
        findView(R.id.article_bar_find_img).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_find_text).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_citation_img).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_citation_text).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_email_img).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_email_text).setEnabled(articleIsNotNull);
        findView(R.id.article_bar_pdf_img).setEnabled(pdfEnabled);
        findView(R.id.article_bar_pdf_text).setEnabled(pdfEnabled);
    }

    protected LeftMenuWithTwoPanels.Listener leftMenuWithTwoPanelsListener = new LeftMenuWithTwoPanels.Listener() {

        @Override
        public void onContentStateStartChanging(final LeftMenuWithTwoPanels.ContentState oldContentState, final LeftMenuWithTwoPanels.ContentState newContentState) {
            if (oldContentState == LeftMenuWithTwoPanels.ContentState.Closed
                    && newContentState == LeftMenuWithTwoPanels.ContentState.OpenSide) {
                mInterfaceHelper.hideSearcher(true, false);
            }
        }

        @Override
        public void onContentStateChanged(final LeftMenuWithTwoPanels.ContentState newContentState) {
            if (newContentState == LeftMenuWithTwoPanels.ContentState.OpenSide) {
                uncheckAllTabs();
                opennedPanelId = null;
            }
        }

        @Override
        public void hideInput() {
            ArticleViewFragment.this.hideInput();
        }
    };

    protected LeftMenu.Listener leftMenuListener = new LeftMenu.Listener() {
        @Override
        public void onContentStateChanged(final LeftMenu.ContentState newContentState) {
            if (!isPhone) {
                articlesPager.setTouchesEnabled(true);
            }
            if (newContentState == LeftMenu.ContentState.Closed && getArticlesCountInList() > 0) {
                uncheckAllTabs();
                opennedPanelId = null;
            }
        }

        @Override
        public void hideInput() {
            ArticleViewFragment.this.hideInput();
        }
    };

    protected void resetMenusOnContentTouch() {
        if (leftMenuWithTwoPanels != null &&
                leftMenuWithTwoPanels.getContentState() == LeftMenuWithTwoPanels.ContentState.OpenSide) {
            leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.Closed);
        }
    }

    public void hideSideMenu() {
        if (leftMenuWithTwoPanels != null) {
            if (leftMenuWithTwoPanels.getContentState() == LeftMenuWithTwoPanels.ContentState.OpenCenter) {
                leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.OpenSide);
                uncheckAllTabs();
                opennedPanelId = null;
            }
            if (leftMenuWithTwoPanels.getContentState() == LeftMenuWithTwoPanels.ContentState.OpenSide) {
                leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.Closed);
            }
        }
    }

    protected void hideInput() {
        UIUtils.hideSoftInput(this.getActivity());
    }

    private void updateActionBarVisible() {
        if (!isFullscreenMode() && !isAdvShown()) {
            mSherlock.getActionBar().show();
        } else {
            mSherlock.getActionBar().hide();
        }
    }

    private void updatePageIndicatorDoiList(List<DOI> doiList) {
        if (getPageIndicator() != null) {
            getPageIndicator().updateDoiList(doiList);
        }
    }

    private boolean needPageIndicatorVisible() {
        return getPageIndicator() != null &&
                !isFullscreenMode() &&
                getArticlesCountInList() > 2 &&
                !isAdvShown() &&
                !getJournalActivity().isSoftKeyboardVisible();
    }

    private void updatePageIndicatorVisible() {
        if (needPageIndicatorVisible()) {
            showPageIndicator();
        } else {
            hidePageIndicator();
        }
    }

    private void hidePageIndicator() {
        Logger.d(TAG, "hidePageIndicator()");
        final ArticleViewPageIndicator pageIndicator = getPageIndicator();
        if (pageIndicator == null) {
            return;
        }

        if (!pageIndicator.isHidden()) {
            getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                    .hide(pageIndicator)
                    .commit();
        }
    }

    private void showPageIndicator() {
        Logger.d(TAG, "showPageIndicator()");
        final ArticleViewPageIndicator pageIndicator = getPageIndicator();
        if (pageIndicator == null) {
            return;
        }

        if (pageIndicator.isHidden()) {
            getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                    .show(pageIndicator)
                    .commit();
        }
    }

    private void showLeftTabMenu() {
        Logger.d(TAG, "showLeftTabMenu()");

        if (leftMenu != null) {
            leftMenu.showMenu();
        }

        if (leftMenuWithTwoPanels != null) {
            leftMenuWithTwoPanels.showMenu();
        }
    }

    private void hideLeftTabMenu() {
        Logger.d(TAG, "hideLeftTabMenu()");

        if (leftMenu != null) {
            leftMenu.hideMenu();
        }

        if (leftMenuWithTwoPanels != null) {
            leftMenuWithTwoPanels.hideMenu();
        }
    }

    public boolean handleBackPress() {
        return mInterfaceHelper.handleBackPress();
    }

    @Override
    public void scrollToFigure(final FigureMO figure) {
        if (isPhone) {
            hideSideMenu();
        }
        getCurrentArticleViewContent().scrollToFigure(figure);
    }

    @Override
    public void showFigure(final FigureMO figure) {
        onOpenFigure(figure.getShortCaption());
    }

    private boolean fullscreenMode = false;

    private boolean isFullscreenMode() {
        return fullscreenMode;
    }

    // --> Article component callbacks
    @Override
    public void onBodyTouched(final String url) {
        if (mInterfaceHelper.isAuthorsInfoOpened()) {
            return;
        }
        fullscreenMode = !fullscreenMode;
        updateActionBarVisible();
        updatePageIndicatorVisible();
    }

    @Override
    public void onWebViewTouched() {
        resetMenusOnContentTouch();
        UIUtils.hideSoftInput(this.getActivity());
    }

    @Override
    public void onAccessRequested() {
        if (DeviceUtils.isInternetConnectionAvailable(getContext())) {
            if (authoriser.isAuthorized()) {
                aanHelper.trackActionOpenWebViewerForOtherPage(theme.getHelpUrl());
                webController.openUrlInternal(theme.getHelpUrl());
            } else {
                aanHelper.trackActionLaunchGetAccessDialogue("Get Access Click");
                aanHelper.setCurrentPageForGetAccess_ArticleViewer();
                ((MainActivity) getActivity()).openGetAccessDialog();
            }
        } else {
            errorManager.alertWithErrorCode(this.getActivity(), NO_CONNECTION_AVAILABLE);
        }
    }

    @Override
    public void onOpenCustomScheme(final String url) {
        if (url.startsWith("http")) {
            webController.openUrlInternal(url);
        } else if (url.startsWith("openvideo")) {
            {
                aanHelper.trackActionPlayVideo(url);
            }
            videoController.openVideoUrl(url);
        } else if (webController.canOpenUrlExternal(url)) {
            webController.openUrlExternal(url);
        } else if (url.startsWith("openbib")) {
            Uri uri = Uri.parse(url);
            String bib = uri.getHost();
            openPanel(R.id.articleBarReferencesButton);
            referencesAdapter.scrollToBib(bib);
            {
                aanHelper.trackActionJumpToReference(getCurrentArticle());
            }
        } else if (url.startsWith("refresh")) {
            getCurrentArticleViewContent().loadAndShowArticle();
        } else {
            UIUtils.showLongToast(getActivity().getApplicationContext(), "Not implemented action: " + url);
        }
    }

    @Override
    public void onOpenFigure(final String figShortCaption) {
        final ArticleMO currentArticle = getCurrentArticle();
        FigureMO figureForOpen = null;
        for (final FigureMO figure : currentArticle.getFigures()) {
            if (figure.getShortCaption().equals(figShortCaption)) {
                figureForOpen = figure;
                break;
            }
        }
        if (figureForOpen == null) {
            return;
        }
        ((MainActivity) getActivity()).openFigures(currentArticle.getDOI(), figureForOpen.getUid());
        {
            aanHelper.trackActionJumpToFigure(figureForOpen.getTitle(), getCurrentArticle());
        }
    }

    @Override
    public void onArticleViewHasLoadedContent(ArticleComponent sender, ArticleMO article) {
        showQuickLinkMenu();
    }

    @Override
    public void onArticleViewStartLoadContent(ArticleComponent sender, final ArticleMO article) {
        if (addToFavOnContentReady) {
            sender.setShowArticleSaving(true);
            articleService.addArticleRefToFavorites(article);
        }
        if (loadPdfOnContentReady) {
            documentsDownloader.getArticlePdf(article);
        }
        loadPdfOnContentReady = false;
        addToFavOnContentReady = false;
    }

    @Override
    public void onOpenAuthors(final Point at, final Rect rect) {
        if (isPhone) {
            leftMenuWithTwoPanels.changeContentState(LeftMenuWithTwoPanels.ContentState.Closed);
        }
        mInterfaceHelper.showAuthorsInfo(at, rect);
    }

    @Override
    public void onPopupShow() {
        dim(63);
    }

    @Override
    public void onPopupDismiss() {
        undim();
    }

    public void onNeedScrollToBioSection() {
        ArticleComponent articleComponent = getCurrentArticleComponent();
        if (articleComponent != null) {
            mInterfaceHelper.hideAuthorsInfo();
            articleComponent.scrollToBioSection();
        }
    }

    @Override
    public void onEmailCitation() {
        {
            aanHelper.trackActionOpenEmailForm("Citation");
        }
        mInterfaceHelper.onOpenEmailSender();
        try {
            final ArticleMO article = articleService.getArticleFromDao(getCurrentDoi());
            emailSender.emailCitation(article, this.getActivity());
        } catch (final ElementNotFoundException e) {
            Logger.s(TAG, e);
        }
    }

    @Override
    public ArticleMO getCurrentArticle() {
        final DOI doi = getCurrentDoi();
        if (null == doi) {
            return null;
        }

        try {
            return articleService.getArticleFromDao(doi);
        } catch (final ElementNotFoundException | NullPointerException e) {
            Logger.s(TAG, e);
            return null;
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && authoriser.isAuthorized()) {
            switch (requestCode) {
                case Authorizer.REQUEST_CODE_GET_PDF:
                    loadPdfOnContentReady = true;
                    break;
                case Authorizer.REQUEST_CODE_ADD_TO_FAVORITES:
                    addToFavOnContentReady = true;
                    break;
            }
        }
    }


    @Override
    public void onAccessForbiddenArticle() {
        errorManager.alertWithErrorCode(
                getActivity(),
                ACCESS_FORBIDDEN_ARTICLE,
                ErrorButton.withTitleAndListener(getActivity().getString(android.R.string.ok), null),
                ErrorButton.withTitleAndListener(getActivity().getString(R.string.get_help), new ErrorButton.OnClickListener() {
                    @Override
                    public void onClick() {
                        webController.openUrlInternal(theme.getHelpUrl());
                    }
                })
        );
    }

    @Override
    public void onSaveArticleNoInternetConnection(ArticleMO articleMO) {
        onSaveArticleError(articleMO);
    }

    @Override
    public StartActivityForResultHelper getStartActivityForResultHelper() {
        return this;
    }

    @Override
    public Context getContext() {
        return this.getActivity();
    }

    @Override
    public void onRenderStarted() {
    }

    @Override
    public void onRenderCompleted() {
        if (searchTerm != null) {
            openPanel(R.id.articleBarSearchButton);
            mInterfaceHelper.setSearcherText(searchTerm);
            mInterfaceHelper.hideSearcherInput();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && DeviceUtils.isPhone(getActivity())) {
            changeFontSize(settings.getArticleFontSize());
        }
    }

    private class InterfaceHelper {

        private boolean isPopupInited = false;
        private PopupHost mPopupHost;
        private PopupAuthorsInfo mAuthorsInfo;
        private ArticleSearcher mSearcher;
        private final ViewGroup mProgressViewParent;
        private boolean isShownSearcherBeforeAd = false;

        public InterfaceHelper() {
            mPopupHost = findView(R.id.article_popup_host);
            if (mPopupHost != null) {
                mPopupHost.setPopupListener(ArticleViewFragment.this);
                mPopupHost.setPopupContentHolderResId(R.id.article_popup_content);
                isPopupInited = true;
            }
            mProgressViewParent = findView(R.id.article_progress);
            mProgressViewParent.setVisibility(View.GONE);
            mProgressViewParent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    return true;
                }
            });
            mProgressViewParent.findViewById(R.id.article_progress_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    documentsDownloader.cancel();
                }
            });

            if (isPhone) {
                FragmentManager fragmentManager = getChildFragmentManager();
                final FragmentTransaction ft = fragmentManager.beginTransaction();
                mAuthorsInfo = new PopupAuthorsInfo();
                ft.add(R.id.article_authors_sliding_fragment, mAuthorsInfo);
                ft.commit();
                fragmentManager.executePendingTransactions();
            }
        }

        public void toggleFontSizePicker() {
            if (!isPopupInited) {
                return;
            }
            mPopupHost.toggleFontSizePicker(getChildFragmentManager(), findView(R.id.article_view_change_text_size));
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_FONT_SIZE,
                    getCurrentDoi().getValue(),
                    0L);
        }

        public void toggleCitation() {
            if (!isPopupInited) {
                return;
            }
            mPopupHost.toggleCitation(getChildFragmentManager(), findView(R.id.article_view_citation), getCurrentDoi());
        }

        public void toggleSavedArticlesEditor() {
            if (!isPopupInited) {
                return;
            }
            mPopupHost.toggleSavedArticlesEditor(getChildFragmentManager(), findView(R.id.article_view_edit_saved));
        }

        public void showAuthorsInfo(final Point at, final Rect rectDp) {
            {
                aanHelper.trackActionAuthorInformation(getCurrentArticle());
            }

            if (!isPhone && isPopupInited) {
                mPopupHost.showAuthorsInfo(getChildFragmentManager(), at);
            } else if (isPhone) {
                final CustomWebView articleWebView = getCurrentArticleViewContent().getWebView();
                final Rect rectPx = UIUtils.dpToPxRect(getActivity(), rectDp);
                mAuthorsInfo.show(true, articleWebView, rectPx);
            }
        }

        public boolean isPopupShowing() {
            return isPopupInited && mPopupHost.isShowing();
        }

        public void hidePopup() {
            if (isPopupShowing()) {
                mPopupHost.hide();
            }
        }

        public void hidePopup(final boolean animated) {
            if (isPopupShowing()) {
                mPopupHost.hide(animated);
            }
        }

        public void onOpenEmailSender() {
            hidePopup();
            if (mAuthorsInfo != null && mAuthorsInfo.isOpened()) {
                mAuthorsInfo.hide(false);
            }
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_SHARE_EMAIL,
                    getCurrentDoi().getValue(),
                    0L);
        }

        public boolean handleBackPress() {
            if (isAdvShown()) {
                ArticleViewFragment.this.onAdvHidden();
                return false;
            }

            if (documentsDownloader.isDownloadInProgress()) {
                documentsDownloader.cancel();
                return true;
            }
            if (isPhone && isSearcherShowed()) {
                hideSearcher(true, true);
                return true;
            }
            if (mPopupHost != null && mPopupHost.isShowing()) {
                mPopupHost.hide();
                return true;
            }
            if (mAuthorsInfo != null && mAuthorsInfo.isOpened()) {
                mAuthorsInfo.hide(true);
                return true;
            }
            if (leftMenuWithTwoPanels != null) {
                if (leftMenuWithTwoPanels.getContentState() == LeftMenuWithTwoPanels.ContentState.OpenCenter) {
                    leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.OpenSide);
                    return true;
                }
                if (leftMenuWithTwoPanels.getContentState() == LeftMenuWithTwoPanels.ContentState.OpenSide) {
                    leftMenuWithTwoPanels.changeContentStateAnimated(LeftMenuWithTwoPanels.ContentState.Closed);
                    return true;
                }
            }
            if (leftMenu != null && leftMenu.getContentState() == LeftMenu.ContentState.Open) {
                leftMenu.changeContentStateAnimated(LeftMenu.ContentState.Closed);
                return true;
            }
            return false;
        }

        public boolean isAuthorsInfoOpened() {
            return mAuthorsInfo != null && mAuthorsInfo.isOpened();
        }

        public void hideAuthorsInfo() {
            if (isPhone && isAuthorsInfoOpened()) {
                mAuthorsInfo.hide(true);
            } else if (!isPhone) {
                hidePopup();
            }
        }

        public void onActivityStart() {
            if (mSearcher != null) {
                mSearcher.addListener(searcherListener);
            }
            if (documentsDownloader.isDownloadInProgress()) {
                showProgress(getString(R.string.downloading_article_content));
                final int progress = documentsDownloader.getProgress();
                if (progress != -1) {
                    onDocumentDownloadProgress(progress);
                }
            } else {
                hideProgress();
            }
        }

        public void onActivityStop() {
//            initializeAuthorsInfoFrag();
            if (mSearcher != null) {
                mSearcher.removeListener(searcherListener);
            }
        }

        public void onAdvShown() {
            if (null != mSearcher) {
                isShownSearcherBeforeAd = mSearcher.isShowed();
                if (isShownSearcherBeforeAd) {
                    mInterfaceHelper.hideSearcher(true, false);
                }
            }
        }

        public void onAdvHidden() {
            if (isShownSearcherBeforeAd) {
                mInterfaceHelper.showSearcher(true);
            }
        }

        private void initializeSearcher() {
            if (mSearcher == null) {
                mSearcher = new ArticleSearcher();
                FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
                fragmentTransaction.add(R.id.article_searcher_fragment, mSearcher).commit();
                getChildFragmentManager().executePendingTransactions();
                mSearcher.hideSearcher(true);
                mSearcher.addListener(searcherListener);
            }
        }

        public void showSearcher() {
            if (isPhone) {
                showSearcher(true);
            } else {
                final ArticleViewContent content = getCurrentArticleViewContent();
                if (mSearcher == null) {
                    getCenterMenu().getListFragment();
                    initializeSearcher();
                }
                if (content != null) {
                    getCenterMenu().showSearcher(content.getWebView());
                }
            }
        }

        private void showSearcher(final boolean animated) {
            if (mSearcher == null) {
                initializeSearcher();
            }
            if (mSearcher != null && getCurrentArticleViewContent() != null) {
                if (animated) {
                    mSearcher.showSearcherAnimated(getCurrentArticleViewContent().getWebView());
                } else {
                    mSearcher.showSearcher(getCurrentArticleViewContent().getWebView());
                }
            }
        }

        public void doSearch() {
            if (mSearcher != null) {
                mSearcher.doSearch(getCurrentArticleViewContent().getWebView());
            }
        }

        public boolean isSearcherHasFocus() {
            return mSearcher != null && mSearcher.hasFocus();
        }

        public void showSearcherKeyboard() {
            if (mSearcher != null) {
                mSearcher.showKeyboard();
            }
        }

        public void hideSearcher(final boolean animated, final boolean clearResults) {
            if (mSearcher == null) {
                return;
            }
            if (animated) {
                mSearcher.hideSearcherAnimated(clearResults);
            } else {
                mSearcher.hideSearcher(clearResults);
            }
        }

        public void setSearcherText(String text) {
            mSearcher.setText(text);
        }

        public void hideSearcherInput() {
            mSearcher.hideSoftInput();
        }

        private boolean isSearcherShowed() {
            return mSearcher != null && mSearcher.isShowed();
        }

        public boolean openPdf() {
            loadPdfOnContentReady = false;
            if (!DeviceUtils.isInternetConnectionAvailable(getContext())) {
                GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_INTERNET_CONNECTION,
                        GANHelper.ACTION_ERROR,
                        GANHelper.LABEL_INTERNET_ERROR_8,
                        -1L);
                final ErrorButton okButton = withTitleAndListener(getString(android.R.string.ok), new OnClickListener() {
                    @Override
                    public void onClick() {
                        hidePanels();
                    }
                });
                errorManager.alertWithErrorCode(ArticleViewFragment.this.getActivity(), NO_CONNECTION_AVAILABLE, okButton);

            } else {
                ArticleComponent articleComponent = getCurrentArticleComponent();
                if (articleComponent == null) {
                    return false;
                }
                final ArticleMO article = articleComponent.getLoadedArticle();
                if (article == null || !article.hasPdf()) {
                    getCenterMenu().showPlaceHolderWithMessage(getString(R.string.no_pdf_version));
                    return false;
                } else if (article.isRestricted() && !authoriser.isAuthorized()) {
                    {
                        aanHelper.setCurrentPageForGetAccess_ArticleViewer();
                    }
                    authoriser.requestAccessFromGetPdfAction(getStartActivityForResultHelper(), article.getDOI());
                    uncheckAllTabs();
                } else {
                    documentsDownloader.getArticlePdf(article);
                    uncheckAllTabs();
                }

            }
            return true;
        }

        public void onPdfDownloadStarted() {
            this.showProgress(getString(R.string.downloading_article_pdf));
        }

        public void onSupportingInfoDownloadStarted() {
            this.showProgress(getString(R.string.downloading_article_sup_info));
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_LINK,
                    null,
                    0L);
        }

        public void onDocumentDownloadFinished(final String path, final DocType type) {
            this.hideProgress();
            uncheckAllTabs();
            switch (type) {
                case PDF: {
                    ArticleComponent articleComponent = getCurrentArticleComponent();
                    if (null != articleComponent) {
                        final ArticleMO article = articleComponent.getLoadedArticle();
                        aanHelper.trackArticleFulltextPDF(article);
                    }
                }
                settings.incCountDownloadedArticlePdf();
                startActivity(PdfViewActivity.getStartingIntent(ArticleViewFragment.this.getActivity(), path));
                break;
                case SUPPORTING_INFO: {
                    ArticleComponent articleComponent = getCurrentArticleComponent();
                    if (null != articleComponent) {
                        final ArticleMO article = articleComponent.getLoadedArticle();
                        int index = path.lastIndexOf("/");
                        final String fileName = index < 0 ? path : path.substring(index + 1);
                        aanHelper.trackArticleViewSupportingInfoFile(article, fileName);
                    }
                }
                String mimeType = URLConnection.guessContentTypeFromName(path);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(path)), mimeType);
                startActivity(Intent.createChooser(intent, "Open:"));
                break;
            }
        }

        public void onDocumentDownloadError(final AppErrorCode code) {
            hideProgress();
            GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_PDF_LOAD_ERROR,
                    GANHelper.ACTION_ERROR,
                    GANHelper.LABEL_PDF_LOAD_ERROR_27,
                    -1L);
            errorManager.alertWithErrorCode(ArticleViewFragment.this.getActivity(), code);
        }

        public void onDocumentDownloadProgress(final int progressPercents) {
            mProgressViewParent.findViewById(R.id.article_progress_circle).setVisibility(View.GONE);
            mProgressViewParent.findViewById(R.id.article_progress_horizontal_parent).setVisibility(View.VISIBLE);
            ((ProgressBar) mProgressViewParent.findViewById(R.id.article_progress_horizontal)).setProgress(progressPercents);
        }

        public void showProgress(final String message) {
            mProgressViewParent.setVisibility(View.VISIBLE);
            mProgressViewParent.findViewById(R.id.article_progress_circle).setVisibility(View.VISIBLE);
            mProgressViewParent.findViewById(R.id.article_progress_horizontal_parent).setVisibility(View.GONE);
            final TextView progressMsg = (TextView) mProgressViewParent.findViewById(R.id.article_progress_text);
            progressMsg.setText(message);
            dim(100);
        }

        public void hideProgress() {
            mProgressViewParent.setVisibility(View.GONE);
            undim();
            if (isPopupShowing()) {
                onPopupShow();
            }
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        Logger.d(TAG, "onActionModeFinished()");
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        Logger.d(TAG, "onActionModeStarted()");
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        Logger.d(TAG, "onCreatePanelMenu()");

        if (DeviceUtils.isPhone(this.getActivity())) {
            menu.add(getString(R.string.action_show_menu))
                    .setIcon(ActionBarUtils.getMenuIconResource(theme))
                    .setShowAsActionFlags(com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                            ((MainActivity) getJournalActivity()).onSideMenuButtonClicked();
                            return true;
                        }
                    });
        } else {
            ActionBarUtils.inflateArticleViewMenu(mSherlock, menu, theme);
            final long savedCount = articleService.getSavedArticleCount();
            menu.findItem(R.id.article_view_edit_saved).setTitle("(" + savedCount + ")");

            setAlertButton = menu.findItem(R.id.article_view_set_alert);
            if (needToHideSetAlertButton()) {
                setAlertButton.setVisible(false);
            }
        }

        return true;
    }

    private boolean needToHideSetAlertButton() {
        return getCurrentArticle() != null &&
                (getCurrentArticle().getKeywords() == null || getCurrentArticle().getKeywords().isEmpty());
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        Logger.d(TAG, "onMenuItemSelected()");
        final int itemId = item.getItemId();
        if (DeviceUtils.isTablet(this.getActivity())) {
            if (itemId == R.id.article_view_change_text_size) {
                mInterfaceHelper.toggleFontSizePicker();
                return true;
            } else if (itemId == R.id.article_view_edit_saved) {
                mInterfaceHelper.toggleSavedArticlesEditor();
                return true;
            } else if (itemId == R.id.article_view_citation) {
                mInterfaceHelper.toggleCitation();
                return true;
            } else if (itemId == R.id.article_view_send_email) {
                {
                    aanHelper.trackActionOpenEmailForm(getCurrentArticle().isRestricted() ? "Abstract" : "Fulltext");
                }
                mInterfaceHelper.onOpenEmailSender();
                emailSender.sendArticle(getCurrentDoi(), getJournalActivity());
                return true;
            } else if (itemId == android.R.id.home) {
                MainActivity mainActivity = (MainActivity) getJournalActivity();
                mainActivity.onBackPressed();
                return true;
            } else if (itemId == R.id.article_view_set_alert) {
                final View button = findView(R.id.articleBarInfoButton);
                onBarButtonClick(button);
            }
        } else {
            if (itemId == android.R.id.home) {
                MainActivity mainActivity = (MainActivity) getJournalActivity();
                mainActivity.onBackPressed();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, com.actionbarsherlock.view.Menu menu) {
        Logger.d(TAG, "onPreparePanel()");
        return true;
    }

}
