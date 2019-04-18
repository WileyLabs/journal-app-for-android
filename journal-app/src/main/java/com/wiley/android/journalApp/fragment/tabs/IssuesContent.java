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
package com.wiley.android.journalApp.fragment.tabs;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.JournalMainFragment;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.IssueView;
import com.wiley.android.journalApp.controller.ConnectionController;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.notification.IssueDownloadProgressProcessor;
import com.wiley.android.journalApp.utils.Action;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.wol.client.android.data.http.ResourceManager;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.wiley.android.journalApp.error.ErrorButton.withTitleAndListener;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_CANCEL;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_FAVORITES_COUNT_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_NEED_UPDATE;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_UPDATED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_REMOVED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.NETWORK_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.settings.Settings.DOWNLOAD_ISSUE;

public class IssuesContent extends BaseTabFragment implements IssueView.IssueViewHost {

    private static final String TAG = IssuesContent.class.getSimpleName();
    private static final long FREE_SAMPLE_HEADER_ID = "1970".hashCode();

    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ConnectionController mConnectionController;
    @Inject
    private IssueService issueService;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private Settings settings;
    @Inject
    @InjectCachePath
    private String rootPath;
    @Inject
    private Authorizer authorizer;
    @Inject
    private ImageLoaderHelper imageLoader;
    @Inject
    private LayoutInflater inflater;
    @Inject
    private ImportManager importManager;
    @Inject
    private ArticleService articleService;
    @Inject
    private Theme theme;

    private ActionMode mEditAction;

    private boolean showCovers;
    private boolean isOnline;

    private View mProgress;
    private List<IssueMO> currentIssues = null;
    private final List<ListItem> gridItems = new ArrayList<>();
    private final IssuesStickyGridAdapter gridAdapter = new IssuesStickyGridAdapter();
    private int yearSeparatorColor;

    private StickyGridHeadersGridView issuesGridView;
    private ViewGroup freeSampleOverlayView;
    private final Stack<IssueView> issueViews = new Stack<>();

    private final ActionMode.Callback mEditActionCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
            return ((JournalMainFragment)getParentFragment()).onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
            notifyEditModeChanged(true);
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            final int itemId = item.getItemId();
            if (itemId == R.id.issue_delete) {
                List<DOI> doisForRemove = new ArrayList<>();
                for (final ListItem listItem : gridItems) {
                    if (listItem.isEditable() && listItem.isSelected && !doisForRemove.contains(listItem.associatedDoi)) {
                        doisForRemove.add(listItem.associatedDoi);
                    }
                }
                issueService.removeLoadedIssues(doisForRemove);
                mode.finish();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onDestroyActionMode(final ActionMode actionMode) {
            mEditAction = null;
            notifyEditModeChanged(false);
        }
    };

    private final IssueView.EditModeListener mEditListener = new IssueView.EditModeListener() {
        @Override
        public void onToggleEditMode(final IssueView sender) {
            switchEditState();
            final ListItem item = findListItemById((Integer) sender.getTag());
            if (mEditAction != null && item != null) {
                item.isSelected = true;
                sender.setSelected(true);
            }
        }

        @Override
        public void onToggleEditModeSelected(IssueView sender) {
            if (!sender.canSelectForEdit())
                return;
            final ListItem item = findListItemById((Integer) sender.getTag());
            if (item == null) {
                return;
            }
            boolean needSelect = !item.isSelected;
            if (needSelect) {
                sender.setSelected(true);
                item.isSelected = true;
            } else {
                boolean canUnselect = false;
                for (final ListItem listItem : gridItems) {
                    if (listItem.id != item.id && listItem.isSelected) {
                        canUnselect = true;
                        break;
                    }
                }
                if (canUnselect) {
                    sender.setSelected(false);
                    item.isSelected = false;
                }
            }
        }
    };

    private final IssueView.OnDownloadCancelClickListener mIssueDownloadCancelClickListener = new IssueView.OnDownloadCancelClickListener() {
        @Override
        public void onClick(IssueView sender, DOI doi) {
            issueService.stopIssueLoading(doi);
            sender.onDownloadStopped(false);
            withListItemDo(new Action<ListItem>() {
                @Override
                public void run(ListItem object) {
                    object.resetProgress();
                }
            }, doi);
            gridAdapter.notifyDataSetChanged();
        }
    };

    private final NotificationProcessor issueTocUpdated = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            new AsyncTask<Void, Void, List<IssueMO>>() {
                @Override
                protected List<IssueMO> doInBackground(Void... params) {
                    return issueService.getIssues();
                }

                @Override
                protected void onPostExecute(List<IssueMO> issues) {
                    super.onPostExecute(issues);
                    currentIssues = issues;
                    updateUi(false);
                }
            }.execute();
        }
    };

    private final NotificationProcessor issueListUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "issueListUpdateSuccessProcessor");
            currentIssues = issueService.getIssues();
            updateUi(false);
            hideProgress();
            findView(R.id.error_message_layout).setVisibility(View.GONE);
        }
    };

    private final NotificationProcessor issueListNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "issueListNotModifiedProcessor");
            hideProgress();
        }
    };

    private final NotificationProcessor issueListUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            Logger.d(TAG, "issueListUpdateErrorProcessor");
            hideProgress();

            if (currentIssues == null || currentIssues.isEmpty()) {
                final Exception exception = (Exception) params.get(ERROR);
                showErrorMessageLayout(errorManager, exception);
            }
        }
    };

    private final NotificationProcessor issueFavCountChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            Logger.d(TAG, "issueFavCountChangedProcessor");
            final IssueMO issue = (IssueMO) params.get(IssueService.ISSUE_MO);
            updateIssue(issue);
        }
    };

    private final NotificationProcessor networkStateChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            Logger.d(TAG, "networkStateChangedProcessor");
            final boolean online = (boolean) params.get("online");
            onNetworkStateChanged(online);
        }
    };

    private final NotificationProcessor downloadIssueProgressProcessor = new IssueDownloadProgressProcessor() {
        @Override
        protected void onZipDownloadStarted(DOI doi) {
            for (ListItem listItem : gridItems) {
                if (listItem.getDoi().getValue().equals(doi.getValue())) {
                    listItem.setDownloadedSize(0L);
                    return;
                }
            }
        }

        @Override
        protected void onZipDownloadProgress(DOI doi, long currentProgress, long totalProgress) {
            for (ListItem listItem : gridItems) {
                if (listItem.getDoi().getValue().equals(doi.getValue())) {
                    if (currentProgress - listItem.getDownloadedSize() < 100000 &&
                            currentProgress != totalProgress) {
                        return;
                    }
                    listItem.setDownloadedSize(currentProgress);
                    sendProgressToItem(doi, currentProgress / (1024f * 1024f), totalProgress / (1024f * 1024f));
                    return;
                }
            }
        }

        @Override
        protected void onImportStarted(DOI doi) {
            onImportingStarted(doi);
        }
    };
    private NotificationProcessor cancelIssueProgressProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI doi = (DOI) params.get(DOWNLOAD_ISSUE);
            withListItemDo(new Action<ListItem>() {
                @Override
                public void run(ListItem object) {
                    object.resetProgress();
                }
            }, doi);
        }
    };
    private NotificationProcessor issueDownloadedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final IssueMO issue = (IssueMO) params.get(IssueService.ISSUE_MO);
            onIssueDownloadCompleted(issue);
        }
    };
    private NotificationProcessor issueDownloadErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            gridAdapter.notifyDataSetChanged();
        }
    };
    private NotificationProcessor issueRemovedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final IssueMO issue = (IssueMO) params.get(IssueService.ISSUE_MO);
            onIssueRemoved(issue);
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        boolean journalHasDarkBackground = theme.isJournalHasDarkBackground();
        if (journalHasDarkBackground) {
            yearSeparatorColor = ColorUtils.brighterColorByPercent(theme.getMainColor(), 75);
        } else {
            yearSeparatorColor = theme.getMainColor();
        }
        showCovers = theme.isShowCovers();
    }

    @Override
    public void onStart() {
        super.onStart();
        isOnline = mConnectionController.isOnline();
        currentIssues = issueService.getIssues();
        updateUi(true);
        notificationCenter.subscribeToNotification(ISSUE_LIST_UPDATED.getEventName(), issueListUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(ISSUE_LIST_NOT_MODIFIED.getEventName(), issueListNotModifiedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_LIST_ERROR.getEventName(), issueListUpdateErrorProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_PROGRESS.getEventName(), downloadIssueProgressProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_CANCEL.getEventName(), cancelIssueProgressProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_SUCCESS.getEventName(), issueDownloadedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), issueDownloadErrorProcessor);
        notificationCenter.subscribeToNotification(ISSUE_REMOVED.getEventName(), issueRemovedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_FAVORITES_COUNT_CHANGED.getEventName(), issueFavCountChangedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_SUCCESS.getEventName(), issueTocUpdated);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(issueListUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(issueListNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(issueListUpdateErrorProcessor);
        notificationCenter.unSubscribeFromNotification(downloadIssueProgressProcessor);
        notificationCenter.unSubscribeFromNotification(cancelIssueProgressProcessor);
        notificationCenter.unSubscribeFromNotification(issueDownloadedProcessor);
        notificationCenter.unSubscribeFromNotification(issueDownloadErrorProcessor);
        notificationCenter.unSubscribeFromNotification(issueRemovedProcessor);
        notificationCenter.unSubscribeFromNotification(issueFavCountChangedProcessor);
        notificationCenter.unSubscribeFromNotification(issueTocUpdated);
    }

    @Override
    protected int getTabId() {
        return R.id.issues_tab;
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container,
                                   final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.issues_content, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        issuesGridView = findView(R.id.issues_grid_view);

        prepareIssueViews();

        mProgress = findView(R.id.progress);
        mProgress.setVisibility(View.GONE);
        freeSampleOverlayView = findView(R.id.free_sample_container);
        freeSampleOverlayView.setTag(freeSampleOverlayView.findViewById(R.id.free_sample_issue_view));

        findView(R.id.error_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetUtils.isOnline(getActivity())) {
                    errorManager.alertWithErrorCode(getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
                } else {
                    importManager.updateIssueList();
                }
            }
        });

        setupGridView();
    }

    private void prepareIssueViews() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long issueCount = issueService.countOf();

                for (int i = 0; i < issueCount; i++) {
                    IssueView issueView = (IssueView) inflater.inflate(showCovers ? R.layout.issue : R.layout.issue_no_cover, issuesGridView, false);
                    issueViews.push(issueView);
                }
            }
        }).start();
    }

    private void setupGridView() {
        final boolean showCover = theme.isShowCovers();
        final int numOfIssuesPerLine = getResources().getInteger(showCover
                ? R.integer.num_of_issues_per_line
                : R.integer.num_of_issues_per_line_no_cover);
        issuesGridView.setNumColumns(numOfIssuesPerLine);
        issuesGridView.setAdapter(gridAdapter);
        issuesGridView.setOnScrollListener(new PauseOnScrollListener(imageLoader.getLoader(), false, true, new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                updateFreeSampleViewPosition();
            }
        }));
    }

    /*
    * This moves free sample view along with fake transparent view in the gridview
    * if the fake view is scrolled out of the screen, the free sample view should disappear too.
    */
    private void updateFreeSampleViewPosition() {
        ViewGroup cont = findFakeViewContainer();

        if (cont == null) {
            // the zero child is not a fake view container
            freeSampleOverlayView.setTop(-9999);
            freeSampleOverlayView.setVisibility(View.INVISIBLE);
            return;
        }

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) freeSampleOverlayView.getLayoutParams();
        lp.setMargins(lp.leftMargin, cont.getTop(), lp.rightMargin, lp.bottomMargin);
        freeSampleOverlayView.setLayoutParams(lp);

        freeSampleOverlayView.setVisibility(View.VISIBLE);
    }

    private ViewGroup findFakeViewContainer() {
        View view = issuesGridView.getChildAt(0);
        if (view == null || !(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view; // a container for the fake view container
        view = viewGroup.getChildAt(0); // fake view container itself

        if (view == null) {
            return null;
        }

        // the fake view container holds the fake view as a tag
        view = view.getTag() instanceof View ? (View) view.getTag() : null;

        if (view == null || view.getId() != R.id.issue_fake_list_item) {
            // the zero child is not the fake view container
            return null;
        }

        return viewGroup;
    }

    private void onNetworkStateChanged(final boolean available) {
        isOnline = available;
        gridAdapter.notifyDataSetChanged();
    }

    @Override
    public void onShow() {
        if (notificationCenter != null) {
            super.onShow();
            notificationCenter.subscribeToNotification(NETWORK_STATE_CHANGED.getEventName(), networkStateChangedProcessor);
            if (isOnline != NetUtils.isOnline(getActivity())) {
                onNetworkStateChanged(NetUtils.isOnline(getActivity()));
            }
            GANHelper.trackPageView("/home/past-issues", true);
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        notificationCenter.unSubscribeFromNotification(networkStateChangedProcessor);
        hideEditAction();
    }

    private void sendProgressToItem(final DOI doi, final float curr, final float total) {
        withListItemDo(new Action<ListItem>() {
            @Override
            public void run(ListItem object) {
                object.totalProgress = total;
                object.currentProgress = curr;
            }
        }, doi);

        withIssueViewDo(new Action<IssueView>() {
            @Override
            public void run(IssueView object) {
                object.onDownloadProgress(curr, total);
            }
        }, doi);
    }

    private void onIssueDownloadCompleted(final IssueMO issue) {
        updateIssue(issue);
    }

    private void onIssueRemoved(final IssueMO issue) {
        updateIssue(issue);
        gridAdapter.notifyDataSetChanged();
    }

    private void onImportingStarted(final DOI doi) {
        withListItemDo(new Action<ListItem>() {
            @Override
            public void run(ListItem object) {
                object.resetProgress();
            }
        }, doi);
        gridAdapter.notifyDataSetChanged();
    }

    private void switchEditState() {
        if (mEditAction != null) {
            hideEditAction();
            return;
        }

        boolean hasEditableIssues = false;
        for (final ListItem item : gridItems) {
            if (item.isEditable()) {
                hasEditableIssues = true;
                break;
            }
        }
        if (hasEditableIssues) {
            showEditAction();
        }

    }

    private void hideEditAction() {
        if (mEditAction != null) mEditAction.finish();
    }

    private void showEditAction() {
        if (mEditAction == null) {
            mEditAction = ((JournalMainFragment)getParentFragment()).startActionMode(mEditActionCallback);
        }
    }

    private void notifyEditModeChanged(final boolean isEditMode) {
        if (!isEditMode) {
            withListItemDo(new Action<ListItem>() {
                @Override
                public void run(ListItem object) {
                    object.isSelected = false;
                }
            });
        }
        gridAdapter.notifyDataSetChanged();
    }

    protected void updateUi(final boolean showProgressIfNeed) {
        Logger.d(TAG, "updating UI");

        gridItems.clear();

        if (currentIssues == null || currentIssues.isEmpty()) {
            if (showProgressIfNeed) {
                showProgress();
            }
            gridAdapter.notifyDataSetChanged();
            notificationCenter.sendNotification(ISSUE_LIST_NEED_UPDATE.getEventName());
            return;
        }

        final IssueMO freeSample = findFreeSample(currentIssues);
        final ListItem fakeItem = freeSample == null ? null : new DummyListItem(freeSample);
        if (fakeItem != null) {
            gridItems.add(fakeItem);
        }

        updateFreeSample(fakeItem);

        for (final IssueMO each : currentIssues) {
            gridItems.add(new IssueListItem(each));
        }

        gridAdapter.notifyDataSetChanged();
    }

    private void  updateFreeSample(ListItem fakeItem) {
        if (fakeItem == null || fakeItem.issue == null) {
            freeSampleOverlayView.setVisibility(View.INVISIBLE);
        } else {
            final IssueView issueView = (IssueView) freeSampleOverlayView.getTag();

            issueView.setTag(fakeItem.id);
            issueView.setHost(IssuesContent.this);
            issueView.fillBy(fakeItem.issue);
            issueView.setDownloadCancelClickListener(mIssueDownloadCancelClickListener);
            issueView.setEditListener(mEditListener);

            if (fakeItem.currentProgress != -1) {
                issueView.onDownloadProgress(fakeItem.currentProgress, fakeItem.totalProgress);
            }

            issueView.onNetworkStateChanged(isOnline);
            issueView.updateUiState();

            updateFreeSampleViewPosition();
        }
    }

    private void updateIssue(IssueMO issue) {
        if (issue == null) {
            return;
        }
        int pos;
        boolean found = false;
        for (pos = 0; pos < currentIssues.size(); pos++) {
            if (currentIssues.get(pos).getDOI().equals(issue.getDOI())) {
                found = true;
                break;
            }
        }
        if (found) {
            currentIssues.remove(pos);
            currentIssues.add(pos, issue);
            updateUi(false);
        }
    }

    private IssueMO findFreeSample(final List<IssueMO> issues) {
        if (issues == null)
            return null;
        for (final IssueMO each : issues) {
            if (each.isSampleIssue()) {
                return each;
            }
        }
        return null;
    }

    private Handler hideProgressHandler = new Handler();

    private void showProgress() {
        hideProgressHandler.removeCallbacksAndMessages(null);
        mProgress.clearAnimation();
        mProgress.setAlpha(1.0f);
        mProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        hideProgressHandler.removeCallbacksAndMessages(null);
        hideProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                fadeOut.setDuration(500);
                mProgress.startAnimation(fadeOut);
            }
        }, 200);
    }

    @Override
    public ImageLoaderHelper getImageLoader() {
        return imageLoader;
    }

    @Override
    public Theme getJournalTheme() {
        return theme;
    }

    @Override
    public IssueService getIssueService() {
        return issueService;
    }

    @Override
    public ArticleService getArticleService() {
        return articleService;
    }

    @Override
    public void alertIssueIsNotAvailableOffline() {
        final ErrorButton closeButton = withTitleAndListener(getString(R.string.close), null);
        final ErrorButton goToSavedArticlesButton = withTitleAndListener(getString(R.string.go_to_saved_articles), new ErrorButton.OnClickListener() {
            @Override
            public void onClick() {
                ((MainActivity) getActivity()).navigateToJournalSavedArticles();
            }
        });
        errorManager.alertWithErrorCode(getActivity(), AppErrorCode.ISSUE_IS_NOT_AVAILABLE_OFFLINE, closeButton, goToSavedArticlesButton);
    }

    private ListItem findListItemById(int id) {
        for (final ListItem item : gridItems) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    private void withListItemDo(final Action<ListItem> action) {
        for (final ListItem item : gridItems) {
            action.run(item);
        }
    }

    private void withListItemDo(final Action<ListItem> action, final DOI doi) {
        for (final ListItem item : gridItems) {
            if (item.associatedDoi.equals(doi)) {
                action.run(item);
            }
        }
    }

    private void withIssueViewDo(final Action<IssueView> action, final DOI doi) {
        if (null != findFreeSample(currentIssues)) {
            final IssueView freeIssueView = (IssueView) freeSampleOverlayView.getTag();
            if (freeIssueView.getDOI().equals(doi)) {
                action.run(freeIssueView);
            }
        }

        int childCount = issuesGridView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final IssueView iv = findIssueViewAt(issuesGridView.getChildAt(i));
            if (iv != null && iv.getDOI().equals(doi)) {
                action.run(iv);
            }
        }
    }

    private IssueView findIssueViewAt(View view) {
        if (view instanceof IssueView) {
            return (IssueView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            IssueView iv = findIssueViewAt(viewGroup.getChildAt(i));
            if (iv != null) {
                return iv;
            }
        }
        return null;
    }

    private class IssuesStickyGridAdapter extends BaseAdapter implements StickyGridHeadersBaseAdapter {

        private List<HeaderData> headers = new ArrayList<>();

        public IssuesStickyGridAdapter() {
            headers = generateHeaderList();
        }

        @Override
        public int getCountForHeader(int header) {
            return headers.get(header).getCount();
        }

        @Override
        public int getNumHeaders() {
            return headers.size();
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {

            ListItem item = gridItems.get(headers.get(position).getRefPosition());

            if (item instanceof DummyListItem) {
                return getFreeSampleHeaderView((DummyListItem) item, convertView instanceof LinearLayout ? convertView : null, parent);
            } else if (item instanceof IssueListItem) {
                return getPubYearHeaderView((IssueListItem) item, convertView instanceof TextView ? convertView : null, parent);
            } else {
                throw new RuntimeException("Unable to create header view. Unknown list item type " + item);
            }
        }

        @Override
        public int getCount() {
            return gridItems.size();
        }

        @Override
        public Object getItem(int position) {
            return gridItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return gridItems.get(position).getView(convertView, parent);
        }

        @Override
        public int getItemViewType(int position) {
            return gridItems.get(position).getViewType();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        private View getFreeSampleHeaderView(DummyListItem item, View convertView, ViewGroup parent) {
            ViewGroup view;

            if (convertView != null && convertView.getId() == R.id.issue_fake_list_item) {
                view = (ViewGroup) convertView;
            } else {
                view = (ViewGroup) inflater.inflate(R.layout.issues_fake_list_item, parent, false);
                View stubView = view.getChildAt(0);
                ViewGroup.LayoutParams lp = stubView.getLayoutParams();
                lp.height = freeSampleOverlayView.getMeasuredHeight();
                stubView.setLayoutParams(lp);
            }

            updateFreeSample(item);
            return view;
        }

        private View getPubYearHeaderView(IssueListItem item, View convertView, ViewGroup parent) {
            TextView yearView = (TextView) convertView;
            if (yearView == null) {
                yearView = (TextView) inflater.inflate(R.layout.issues_year_list_item, parent, false);
            }

            yearView.setBackgroundColor(yearSeparatorColor);
            yearView.setText(item.publicationYear);

            final TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.list_article_header_title_color, typedValue, true);
            int color = typedValue.data;
            yearView.setTextColor(color);

            return yearView;
        }

        private List<HeaderData> generateHeaderList() {
            Map<Long, HeaderData> mapping = new HashMap<>();
            List<HeaderData> headers = new ArrayList<>();

            for (int i = 0; i < gridItems.size(); i++) {
                long headerId = gridItems.get(i).getHeaderId();
                HeaderData headerData = mapping.get(headerId);
                if (headerData == null) {
                    headerData = new HeaderData(i);
                    headers.add(headerData);
                }
                headerData.incrementCount();
                mapping.put(headerId, headerData);
            }

            return headers;
        }

        @Override
        public void notifyDataSetChanged() {
            headers = generateHeaderList();
            super.notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetInvalidated() {
            headers = generateHeaderList();
            super.notifyDataSetInvalidated();
        }

        private class HeaderData {
            private int mCount;
            private int mRefPosition;

            public HeaderData(int refPosition) {
                mRefPosition = refPosition;
                mCount = 0;
            }

            public int getCount() {
                return mCount;
            }

            public int getRefPosition() {
                return mRefPosition;
            }

            public void incrementCount() {
                mCount++;
            }
        }

    }

    private abstract class ListItem {

        IssueMO issue;
        DOI associatedDoi;
        float currentProgress = -1, totalProgress = -1;
        boolean isSelected;
        int id;
        long downloadedSize;

        ListItem(IssueMO issue) {
            this.issue = issue;
            this.associatedDoi = issue.getDOI();
            this.id = IdUtils.generateIntId();
        }

        abstract View getView(View convertView, ViewGroup parent);
        abstract long getHeaderId();
        abstract int getViewType();
        abstract boolean isEditable();
        long getDownloadedSize() { return downloadedSize; }
        void setDownloadedSize(long downloadedSize) { this.downloadedSize = downloadedSize; }
        DOI getDoi() { return issue.getDOI(); }

        void resetProgress() {
            totalProgress = currentProgress = -1;
        }
    }

    private class IssueListItem extends ListItem {

        String publicationYear;

        public IssueListItem(IssueMO issue) {
            super(issue);
            this.publicationYear = issue.getCoverYear();
        }

        @Override
        public View getView(View convertView, ViewGroup parent) {
            if (!(convertView instanceof IssueView)) {
                convertView = null;
            }

            IssueView issueView = (IssueView) convertView;

            if (issueView == null) {
                try {
                    issueView = issueViews.pop();
                } catch (EmptyStackException e) {
                    issueView = (IssueView) inflater.inflate(showCovers ? R.layout.issue : R.layout.issue_no_cover, parent, false);
                }
            }

            issueView.setTag(id);
            issueView.setHost(IssuesContent.this);
            issueView.fillBy(issue);
            issueView.setDownloadCancelClickListener(mIssueDownloadCancelClickListener);
            issueView.setEditListener(mEditListener);

            if (currentProgress != -1) {
                issueView.onDownloadProgress(currentProgress, totalProgress);
            }

            issueView.onNetworkStateChanged(isOnline);
            issueView.setEditMode(mEditAction != null);
            issueView.setSelected(isSelected);

            issueView.updateUiState();

            return issueView;
        }

        @Override
        public long getHeaderId() {
            return publicationYear.hashCode();
        }

        @Override
        public int getViewType() {
            return 0;
        }

        @Override
        boolean isEditable() {
            return issue.isLocal();
        }
    }

    private class DummyListItem extends ListItem {

        public DummyListItem(IssueMO freeSample) {
            super(freeSample);
        }

        @Override
        public View getView(View convertView, ViewGroup parent) {
            return convertView == null ? new View(getActivity()) : convertView;
        }

        @Override
        public long getHeaderId() {
            return FREE_SAMPLE_HEADER_ID;
        }

        @Override
        public int getViewType() {
            return 1;
        }

        @Override
        boolean isEditable() {
            return false;
        }
    }
}
