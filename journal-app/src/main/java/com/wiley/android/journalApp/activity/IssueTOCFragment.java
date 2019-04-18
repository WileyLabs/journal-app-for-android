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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.IssueView;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.components.popup.PopupHost;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.fragment.TOCContent;
import com.wiley.android.journalApp.fragment.issue.BaseIssueTocSectionsFragment;
import com.wiley.android.journalApp.fragment.issue.IssueTocSectionsFragment10inch;
import com.wiley.android.journalApp.fragment.issue.IssueTocSectionsFragment7inch;
import com.wiley.android.journalApp.notification.IssueDownloadProgressProcessor;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.error.ConnectionException;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.utils.NetUtils;

import java.util.Map;

import roboguice.inject.InjectExtra;

import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ISSUE;
import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_FAVORITE_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_STARTED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_FAVORITES_COUNT_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_REMOVED;
import static com.wiley.wol.client.android.notification.EventList.NETWORK_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.CANCELLED;
import static com.wiley.wol.client.android.notification.NotificationCenter.DOI;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.settings.Settings.DOWNLOAD_ISSUE;

public class IssueTOCFragment extends JournalFragment implements
        BaseIssueTocSectionsFragment.OnSectionSelectedListener,
        PopupHost.PopupListener,
        StartActivityForResultHelper,
        ActionBarSherlock.OnCreatePanelMenuListener,
        ActionBarSherlock.OnMenuItemSelectedListener,
        ActionBarSherlock.OnPreparePanelListener,
        IssueView.IssueViewHost,
        Journal {

    private ViewTreeObserver.OnScrollChangedListener onScrollChangedListener;
    private ViewTreeObserver observer;

    public interface OnSectionSelectedListener {
        void onSectionSelected(String section);
    }

    private final static String TAG = IssueTOCFragment.class.getSimpleName();
    private final static String TAG_LIFE = IssueTOCFragment.class.getSimpleName() + ".LIFE";
    private final static String EXTRA_SAVED_EDITOR_STATE_KEY = "com.wiley.android.journalApp.activity.IssueTOCActivity_savedEditorStateKey";

    @Inject
    private AANHelper aanHelper;
    @InjectExtra("DOI")
    private DOI doi;
    @Inject
    private IssueService mIssueService;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    private Authorizer mAuthorizer;
    @Inject
    private ErrorManager mErrorManager;
    @Inject
    private WebController mWebController;
    @Inject
    private Theme mTheme;
    @Inject
    private ImageLoaderHelper mImageLoader;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;

    private PopupHost mPopupHost;
    private IssueView mIssueView;
    private ActionBarSherlockCompat mSherlock;
    private InterfaceHelper mInterfaceHelper;

    private TOCContent tocContent;
    private ScrollView scroll;
    private boolean postponedDownloadIssue;

    private final NotificationProcessor downloadIssueProgressProcessor = new IssueDownloadProgressProcessor() {
        @Override
        protected void onZipDownloadStarted(DOI doi) {
        }

        @Override
        protected void onZipDownloadProgress(DOI doi, long currentProgress, long totalProgress) {
            if (currentProgress < 100000 && currentProgress != totalProgress) {
                return;
            }
            if (IssueTOCFragment.this.doi.equals(doi)) {
                mIssueView.onDownloadProgress(currentProgress / (1024f * 1024f), totalProgress / (1024f * 1024f));
            }
        }

        @Override
        protected void onImportStarted(DOI doi) {
            if (IssueTOCFragment.this.doi.equals(doi)) {
                mIssueView.onIssueImportingStarted();
            }
        }
    };

    private NotificationProcessor issueDownloadedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI downloadDoi = (DOI) params.get(DOWNLOAD_ISSUE);
            if (downloadDoi.equals(doi)) {
                fillIssueView();
            }
        }
    };
    private NotificationProcessor issueDownloadStartedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI downloadDoi = (DOI) params.get(DOWNLOAD_ISSUE);
            if (downloadDoi.equals(doi)) {
                mIssueView.onDownloadStarted();
            }
        }
    };
    private final NotificationProcessor networkStateChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final boolean online = (boolean) params.get("online");
            if (online && postponedDownloadIssue) {
                postponedDownloadIssue = false;
                onNeedDownloadIssue(mIssueView, doi);
            }
        }
    };
    private NotificationProcessor issueDownloadErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI downloadDoi = (DOI) params.get(DOWNLOAD_ISSUE);
            if (downloadDoi.equals(doi)) {
                if (mIssueView != null) {
                    mIssueView.setDownloadButtonEnabled(true);
                }
                if (params.containsKey(CANCELLED) && (boolean) params.get(CANCELLED)) {
                    fillIssueView();
                    return;
                }
                Object error = params.get(ERROR);
                if (error instanceof ConnectionException && !NetUtils.isOnline(getActivity())) {
                    //TODO use error manager instead
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.no_internet_connection)
                            .setMessage(R.string.issue_download_error)
                            .setCancelable(true)
                            .setNegativeButton(R.string.cancel_download, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    postponedDownloadIssue = false;
                                }
                            })
                            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    postponedDownloadIssue = true;
                                }
                            })
                            .show();

                } else if (error instanceof Throwable) {
                    showErrorWithThrowable((Throwable) error);
                } else if (error instanceof AppErrorCode) {
                    showErrorWithErrorCode((AppErrorCode) error);
                }
                fillIssueView();
            }
        }
    };
    private final NotificationProcessor issueFavCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final IssueMO issue = (IssueMO) params.get(IssueService.ISSUE_MO);
            if (issue.getDOI().equals(mIssueView.getDOI())) {
                mIssueView.fillBy(issue);
            }
        }
    };
    private final IssueView.OnDownloadStartClickListener onDownloadStartClickListener = new IssueView.OnDownloadStartClickListener() {
        @Override
        public void onClick(IssueView sender, DOI doi) {
            onNeedDownloadIssue(sender, doi);
        }
    };

    private final IssueView.OnDownloadCancelClickListener onDownloadCancelClickListener = new IssueView.OnDownloadCancelClickListener() {
        @Override
        public void onClick(IssueView sender, DOI doi) {
            onNeedCancelIssueDownload(sender, doi);
        }
    };
    private NotificationProcessor issueRemovedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI doi = (DOI) params.get(DOI);
            if (IssueTOCFragment.this.doi.equals(doi)) {
                mIssueView.onIssueRemoved();
            }
        }
    };
    private final NotificationProcessor savedArticlesListChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            Logger.d(TAG, "savedArticlesListChangedProcessor()");
            mSherlock.dispatchInvalidateOptionsMenu();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onCreateView");
        return inflater.inflate(R.layout.issue_toc, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        initContentView(savedInstanceState);
        undim();
    }

    protected void initContentView(Bundle savedInstanceState) {
        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        tocContent = (TOCContent) Fragment.instantiate(this.getActivity(), TOCContent.class.getName(), savedInstanceState);
        ft.replace(R.id.issueContent, tocContent);

        if (DeviceUtils.isTablet10Inch(getActivity())) {
            ft.replace(R.id.issue_toc_sections, new IssueTocSectionsFragment10inch());
        } else if (DeviceUtils.isTablet7Inch(getActivity())) {
            ft.replace(R.id.issue_toc_sections, new IssueTocSectionsFragment7inch());
        }

        ft.commit();

        if (DeviceUtils.isTablet(getActivity())) {
            mPopupHost = findView(R.id.issue_toc_popup_host);
            mPopupHost.setPopupContentHolderResId(R.id.issue_toc_popup_content);
            mPopupHost.setPopupListener(this);
        } else {
            mIssueView = findView(R.id.issue_toc_issue_info);
            mIssueView.setHost(this);
            mIssueView.setShowCover(mTheme.isShowCovers());
            mIssueView.setDownloadStartClickListener(onDownloadStartClickListener);
            mIssueView.setDownloadCancelClickListener(onDownloadCancelClickListener);
            fillIssueView();

            scroll = findView(R.id.scroll);

            final RelativeLayout relativeLayout = findView(R.id.sticky_heads_layout);
            tocContent.setHeadersLayout(relativeLayout);
            onScrollChangedListener = new
                    ViewTreeObserver.OnScrollChangedListener() {

                        @Override
                        public void onScrollChanged() {
                            tocContent.refreshHeads(scroll.getScrollY(), mIssueView.getHeight());
                        }
                    };

            scroll.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (observer == null) {
                        observer = scroll.getViewTreeObserver();
                        observer.addOnScrollChangedListener(onScrollChangedListener);
                    } else if (!observer.isAlive()) {
                        observer.removeOnScrollChangedListener(onScrollChangedListener);
                        observer = scroll.getViewTreeObserver();
                        observer.addOnScrollChangedListener(onScrollChangedListener);
                    }

                    return false;
                }
            });
        }

        // action bar
        mInterfaceHelper = new InterfaceHelper();
        mSherlock = ActionBarUtils.initActionBar(getJournalActivity(), getResources().getString(R.string.toc_title), this, theme);

        // feature: quick link menu
        if (DeviceUtils.isPhone(getActivity())) {
            // touch layout
            ((TouchRefreshLayout) findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    showQuickLinkMenu();
                }
            });

            quickLinkMenuComponent.initQuickLink(getActivity(), this);
        }


        { // Adobe Analytics
            final IssueMO issue;
            try {
                issue = mIssueService.getIssue(doi);
            } catch (ElementNotFoundException e) {
                Logger.d(TAG, e.getMessage(), e);
                return;
            }
            aanHelper.trackIssueTocScreen(issue);
        }
    }

    private void fillIssueView() {
        if (!DeviceUtils.isPhone(this.getActivity())) {
            return;
        }
        final IssueMO issue;
        try {
            issue = mIssueService.getIssue(doi);
        } catch (ElementNotFoundException e) {
            Logger.d(TAG, e.getMessage(), e);
            final View issueViewParent = findView(R.id.issue_toc_issue_info_parent);
            issueViewParent.setVisibility(View.GONE);
            return;
        }
        mIssueView.fillBy(issue);
    }

    /**
     * feature: quick link menu
     */
    private void showQuickLinkMenu() {
        quickLinkMenuComponent.showQuickLinkMenu();
    }

    @Override
    public void onStart() {
        Logger.d(TAG_LIFE, "onStart");
        super.onStart();
        if (!DeviceUtils.isTablet(this.getActivity())) {
            mNotificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_PROGRESS.getEventName(), downloadIssueProgressProcessor);
            mNotificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_SUCCESS.getEventName(), issueDownloadedProcessor);
            mNotificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_STARTED.getEventName(), issueDownloadStartedProcessor);
            mNotificationCenter.subscribeToNotification(ISSUE_FAVORITES_COUNT_CHANGED.getEventName(), issueFavCountChangedProcessor);
            mNotificationCenter.subscribeToNotification(ISSUE_REMOVED.getEventName(), issueRemovedProcessor);
        } else {
            mNotificationCenter.subscribeToNotification(ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), savedArticlesListChangedProcessor);
        }
        mNotificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), issueDownloadErrorProcessor);
        mNotificationCenter.subscribeToNotification(NETWORK_STATE_CHANGED.getEventName(), networkStateChangedProcessor);

        if (observer != null && onScrollChangedListener != null) {
            if (!observer.isAlive()) {
                observer.removeOnScrollChangedListener(onScrollChangedListener);
                observer = scroll.getViewTreeObserver();
            }
            observer.addOnScrollChangedListener(onScrollChangedListener);
        }
    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();
        fillIssueView();
        // feature: quick link menu
        showQuickLinkMenu();
    }

    @Override
    public void onStop() {
        Logger.d(TAG_LIFE, "onStop");
        super.onStop();
        if (!DeviceUtils.isTablet(this.getActivity())) {
            mNotificationCenter.unSubscribeFromNotification(downloadIssueProgressProcessor);
            mNotificationCenter.unSubscribeFromNotification(issueDownloadedProcessor);
            mNotificationCenter.unSubscribeFromNotification(issueDownloadStartedProcessor);
            mNotificationCenter.unSubscribeFromNotification(issueFavCountChangedProcessor);
            mNotificationCenter.unSubscribeFromNotification(issueRemovedProcessor);
        } else {
            mNotificationCenter.unSubscribeFromNotification(savedArticlesListChangedProcessor);
        }
        mNotificationCenter.unSubscribeFromNotification(issueDownloadErrorProcessor);
        mNotificationCenter.unSubscribeFromNotification(networkStateChangedProcessor);

        if (observer != null && onScrollChangedListener != null) {
            observer.removeOnScrollChangedListener(onScrollChangedListener);
        }
    }

    @Override
    public void onSectionSelected(final String section) {
        final TOCContent issueContent = (TOCContent)
                getChildFragmentManager().findFragmentById(R.id.issueContent);

        if (issueContent != null) {
            issueContent.onSectionSelected(section);
        }
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

    //    @Override
    protected FrameLayout getDimmableView() {
        return (FrameLayout) findView(R.id.issue_toc_dimmer);
    }

    public void toggleSavedArticleEditor() {
        if (!DeviceUtils.isTablet(this.getActivity())) {
            return;
        }
        mPopupHost.toggleSavedArticlesEditor(getChildFragmentManager(), findView(R.id.issue_toc_edit_saved));
    }

    @Override
    public void onPopupShow() {
        dim(100);
    }

    @Override
    public void onPopupDismiss() {
        undim();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Logger.d(TAG_LIFE, "onHiddenChanged(): hidden = " + hidden);
        super.onHiddenChanged(hidden);
        if (!hidden && tocContent != null) {
            tocContent.executePostponedTasks();

            // feature: quick link menu
            showQuickLinkMenu();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DeviceUtils.isTablet(this.getActivity())) {
            if (outState == null) {
                outState = new Bundle();
            }
            outState.putBoolean(EXTRA_SAVED_EDITOR_STATE_KEY, mPopupHost.isShowing());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            boolean showEditor = savedInstanceState.getBoolean(EXTRA_SAVED_EDITOR_STATE_KEY, false);
            if (showEditor) {
                toggleSavedArticleEditor();
            }
        }
    }

    public void onNeedCancelIssueDownload(IssueView sender, DOI doi) {
        mIssueService.stopIssueLoading(doi);
        sender.onDownloadStopped(false);
    }

    public void onNeedDownloadIssue(IssueView sender, DOI doi) {
        IssueMO issue;
        try {
            issue = mIssueService.getIssue(doi);
        } catch (ElementNotFoundException e) {
            throw new RuntimeException(TAG, e);
        }
        if (issue.isRestricted() && !mAuthorizer.isAuthorized()) {
            {
                aanHelper.setCurrentPageForGetAccess_IssueTOC();
            }
            mAuthorizer.requestAccessFromDownloadIssueAction(this, doi);
        } else {
            mIssueService.downloadIssue(doi);
            sender.onDownloadStarted();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == Authorizer.REQUEST_CODE_DOWNLOAD_ISSUE) {
            setDownloadIssueOnContentUpdate();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        getChildFragmentManager().findFragmentById(R.id.issueContent).onActivityResult(requestCode, resultCode, data);
    }

    private void setDownloadIssueOnContentUpdate() {
        TOCContent frag = (TOCContent) getChildFragmentManager().findFragmentById(R.id.issueContent);
        if (frag != null) {
            frag.setDownloadIssueOnContentUpdate(true);
        }
    }

    private void showErrorWithThrowable(Throwable throwable) {
        mErrorManager.alertWithException(this.getActivity(), throwable);
    }

    private void showErrorWithErrorCode(AppErrorCode errorCode) {
        switch (errorCode) {
            case NO_CONNECTION_AVAILABLE:
                mErrorManager.alertWithErrorCode(IssueTOCFragment.this.getActivity(), NO_CONNECTION_AVAILABLE);
                break;
            case ACCESS_FORBIDDEN_ISSUE:
                mErrorManager.alertWithErrorCode(IssueTOCFragment.this.getActivity(), ACCESS_FORBIDDEN_ISSUE, ErrorButton.withTitleAndListener(getActivity().getString(android.R.string.ok), null),
                        ErrorButton.withTitleAndListener(getActivity().getString(R.string.get_help), new ErrorButton.OnClickListener() {
                            @Override
                            public void onClick() {
                                mWebController.openUrlInternal(theme.getHelpUrl());
                            }
                        }));
        }
    }

    public void onTocLoadingStateChanged(boolean loading) {
        if (DeviceUtils.isTablet(this.getActivity())) {
            BaseIssueTocSectionsFragment frag = (BaseIssueTocSectionsFragment) getChildFragmentManager().findFragmentById(R.id.issue_toc_sections);
            if (frag != null) {
                frag.onTocLoadingStateChanged(loading);
            }
        } else {
            mIssueView.setDownloadButtonEnabled(!loading);
        }
    }

    @Override
    public ImageLoaderHelper getImageLoader() {
        return mImageLoader;
    }

    @Override
    public Theme getJournalTheme() {
        return mTheme;
    }

    @Override
    public IssueService getIssueService() {
        return mIssueService;
    }

    @Override
    public ArticleService getArticleService() {
        return articleService;
    }

    @Override
    public void alertIssueIsNotAvailableOffline() {
    }

    public void setDoi(DOI doi) {
        this.doi = doi;
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
            ActionBarUtils.inflateIssueTocMenu(mSherlock, menu, theme);
            final long savedCount = articleService.getSavedArticleCount();
            menu.findItem(R.id.issue_toc_edit_saved).setTitle("(" + savedCount + ")");
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        Logger.d(TAG, "onMenuItemSelected()");
        final int itemId = item.getItemId();
        if (DeviceUtils.isTablet(this.getActivity())) {
            if (itemId == R.id.issue_toc_edit_saved) {
                mInterfaceHelper.toggleSavedArticlesEditor();
                return true;
            } else if (itemId == android.R.id.home) {
                MainActivity mainActivity = (MainActivity) getJournalActivity();
                mainActivity.onBackPressed();
                return true;
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

    private FragmentManager childFragmentManager() {
        return getChildFragmentManager();
    }

    private class InterfaceHelper {
        private boolean isPopupInited = false;

        public InterfaceHelper() {
            mPopupHost = findView(R.id.issue_toc_popup_host);
            if (mPopupHost != null) {
                mPopupHost.setPopupListener(IssueTOCFragment.this);
                mPopupHost.setPopupContentHolderResId(R.id.issue_toc_popup_content);
                isPopupInited = true;
            }
        }

        public void toggleSavedArticlesEditor() {
            if (!isPopupInited) {
                return;
            }
            mPopupHost.toggleSavedArticlesEditor(childFragmentManager(), findView(R.id.issue_toc_edit_saved));
        }
    }
}
