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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.JournalMainFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.IssueView;
import com.wiley.android.journalApp.controller.ConnectionController;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.notification.IssueDownloadProgressProcessor;
import com.wiley.android.journalApp.utils.Action;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.utils.NetUtils;

import java.util.*;

import static com.wiley.android.journalApp.error.ErrorButton.withTitleAndListener;
import static com.wiley.wol.client.android.notification.EventList.*;
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
    private List<IssueMO> currentIssues = null;
    private final List<ListItem> gridItems = new ArrayList<>();
    private List<GridSectionData> gridData = new ArrayList<>();
    private final IssuesStickyGridAdapter gridAdapter = new IssuesStickyGridAdapter();
    private int yearSeparatorColor;
    private int numOfIssuesPerLine = 4;
    private RecyclerView issuesGridView;
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

        final View mProgress = findView(R.id.progress);
        mProgress.setVisibility(View.GONE);

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
        numOfIssuesPerLine = getResources().getInteger(showCover
                ? R.integer.num_of_issues_per_line
                : R.integer.num_of_issues_per_line_no_cover);

        setGridLayoutManager();
        issuesGridView.setAdapter(gridAdapter);
    }

    private void setGridLayoutManager() {
        final StickyHeaderGridLayoutManager layoutManager = new StickyHeaderGridLayoutManager(numOfIssuesPerLine);
        layoutManager.setHeaderBottomOverlapMargin(getResources().getDimensionPixelSize(R.dimen.header_shadow_size));
        layoutManager.setSpanSizeLookup(new StickyHeaderGridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int section, int position) {
                if (section == 0 && position == 0 && findFreeSample(currentIssues) != null) {
                    return numOfIssuesPerLine;
                } else {
                    return 1;
                }
            }
        });

        issuesGridView.setLayoutManager(layoutManager);
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

        for (final IssueMO each : currentIssues) {
            gridItems.add(new IssueListItem(each));
        }

        gridData = getGridData();

        gridAdapter.notifyAllSectionsDataSetChanged();
    }

    private List<GridSectionData> getGridData() {
        final LongSparseArray<GridSectionData> mapping = new LongSparseArray<>();
        final List<GridSectionData> headers = new ArrayList<>();

        for (final ListItem item : gridItems) {
            long headerId = item.getHeaderId();
            GridSectionData gridSectionData = mapping.get(headerId);
            if (gridSectionData == null) {
                gridSectionData = new GridSectionData();
                headers.add(gridSectionData);
            }
            gridSectionData.addItem(item);
            gridSectionData.incrementCount();
            mapping.put(headerId, gridSectionData);
        }

        return headers;
    }

    private View updateFreeSample(ListItem listItem) {
        final View view = inflater.inflate(R.layout.issues_free_sample_list_item, issuesGridView, false);
        final IssueView issueView = (IssueView) view.findViewById(R.id.free_sample_issue_view);

        issueView.setTag(listItem.id);
        issueView.setHost(IssuesContent.this);
        issueView.fillBy(listItem.issue);
        issueView.setDownloadCancelClickListener(mIssueDownloadCancelClickListener);
        issueView.setEditListener(mEditListener);

        if (listItem.currentProgress != -1) {
            issueView.onDownloadProgress(listItem.currentProgress, listItem.totalProgress);
        }

        issueView.onNetworkStateChanged(isOnline);
        issueView.updateUiState();

        return view;
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

    private void showProgress() {
    }

    private void hideProgress() {
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

    private class IssuesStickyGridAdapter extends StickyHeaderGridAdapter {

        @Override
        public int getSectionCount() {
            return gridData.size();
        }

        @Override
        public int getSectionItemCount(int section) {
            return gridData.get(section).count;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public HeaderViewHolder onCreateHeaderViewHolder(final ViewGroup parent, final int headerType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.issue_list_header_container, parent, false);
            return new IssuesHeaderViewHolder(view);
        }

        @Override
        public ItemViewHolder onCreateItemViewHolder(final ViewGroup parent, final int itemType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.issue_list_item_container, parent, false);
            return new IssuesItemViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(final HeaderViewHolder viewHolder, final int section) {

            ListItem item = gridData.get(section).items.get(0);

            final View headerView;
            final IssuesHeaderViewHolder issueHeaderViewHolder = (IssuesHeaderViewHolder) viewHolder;
            if (item instanceof DummyListItem) {
                 headerView = getFreeSampleHeaderView(issueHeaderViewHolder.layout);
            } else if (item instanceof IssueListItem) {
                headerView = getPubYearHeaderView((IssueListItem) item, issueHeaderViewHolder.layout);
            } else {
                throw new RuntimeException("Unable to create header view. Unknown list item type " + item);
            }

            issueHeaderViewHolder.layout.removeAllViews();
            issueHeaderViewHolder.layout.addView(headerView);
        }

        @Override
        public void onBindItemViewHolder(final ItemViewHolder viewHolder, final int section, final int offset) {
            final IssuesItemViewHolder issuesItemViewHolder = (IssuesItemViewHolder) viewHolder;
            final ListItem listItem = gridData.get(section).items.get(offset);

            final View itemView;
            if (section == 0 && offset == 0 && listItem.issue.isSampleIssue()) {
                itemView = updateFreeSample(listItem);
                final ViewGroup.LayoutParams layoutParams = issuesItemViewHolder.layout.getLayoutParams();
                layoutParams.width = RecyclerView.LayoutParams.MATCH_PARENT;
            } else {
                itemView = listItem.getView(issuesItemViewHolder.layout);
            }
            issuesItemViewHolder.layout.removeAllViews();
            issuesItemViewHolder.layout.addView(itemView);
        }

        private View getFreeSampleHeaderView(ViewGroup parent) {
            return inflater.inflate(R.layout.issues_fake_list_item, parent, false);
        }

        private View getPubYearHeaderView(IssueListItem item, ViewGroup parent) {
            TextView yearView = (TextView) inflater.inflate(R.layout.issues_year_list_item, parent, false);
            yearView.setBackgroundColor(yearSeparatorColor);
            yearView.setText(item.publicationYear);
            return yearView;
        }
    }

    public static class IssuesHeaderViewHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
        FrameLayout layout;

        IssuesHeaderViewHolder(View itemView) {
            super(itemView);
            layout = (FrameLayout) itemView.findViewById(R.id.item_container);
        }
    }

    public static class IssuesItemViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
        FrameLayout layout;

        IssuesItemViewHolder(View itemView) {
            super(itemView);
            layout = (FrameLayout) itemView.findViewById(R.id.item_container);
        }
    }

    private abstract static class ListItem {

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

        abstract View getView(ViewGroup parent);
        abstract long getHeaderId();
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

        IssueListItem(IssueMO issue) {
            super(issue);
            this.publicationYear = issue.getCoverYear();
        }

        @Override
        public View getView(ViewGroup parent) {
            IssueView issueView;
            try {
                issueView = issueViews.pop();
            } catch (EmptyStackException e) {
                issueView = (IssueView) inflater.inflate(showCovers ? R.layout.issue : R.layout.issue_no_cover, parent, false);
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
        boolean isEditable() {
            return issue.isLocal();
        }
    }

    private class DummyListItem extends ListItem {

        DummyListItem(IssueMO freeSample) {
            super(freeSample);
        }

        @Override
        public View getView(ViewGroup parent) {
            return new View(getActivity());
        }

        @Override
        public long getHeaderId() {
            return FREE_SAMPLE_HEADER_ID;
        }

        @Override
        boolean isEditable() {
            return false;
        }
    }

    private class GridSectionData {
        private int count;
        private List<ListItem> items;

        GridSectionData() {
            items = new ArrayList<>();
            count = 0;
        }

        void addItem(ListItem item) {
            items.add(item);
        }

        void incrementCount() {
            count++;
        }
    }
}
