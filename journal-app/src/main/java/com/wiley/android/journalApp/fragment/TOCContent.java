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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.IssueTOCFragment;
import com.wiley.android.journalApp.base.BaseArticleComponentHostFragment;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.TocComponent;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.List;
import java.util.Map;

import roboguice.inject.InjectExtra;

import static com.wiley.wol.client.android.notification.EventList.ALL_CONTENT_UPDATE_STARTED;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_FAVORITE_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_UPDATED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

public class TOCContent extends BaseArticleComponentHostFragment implements IssueTOCFragment.OnSectionSelectedListener {

    @Inject
    private IssueService issueService;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private ImportManager importManager;

    private final NotificationProcessor successProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            showContent();
            if (downloadIssueOnContentUpdate) {
                downloadIssueOnContentUpdate = false;
                issueService.downloadIssue(doi);
            }
        }
    };

    private final NotificationProcessor notModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            if (needRender)  {
                showContent();
            } else {
                onContentLoadingStopped();
            }
        }
    };

    private final NotificationProcessor errorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            onContentLoadingStopped();
            errorManager.alertWithException(TOCContent.this.getActivity(), (Throwable) params.get(ERROR));
        }
    };

    private final NotificationProcessor savedArticlesListChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            getActivity().invalidateOptionsMenu();
        }
    };

    private final NotificationProcessor allContentUpdateStartedProcessor= new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            onContentLoadingStarted();
        }
    };

    private final NotificationProcessor issuesListUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            updateSections();
        }
    };

    private final NotificationProcessor issuesListNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            onContentLoadingStopped();
        }
    };

    private final NotificationProcessor issuesListErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            onContentLoadingStopped();
        }
    };

    @InjectExtra("DOI")
    private DOI doi;

    private boolean downloadIssueOnContentUpdate;

    private TocComponent tocComponent;
    private View progress;

    private final InterfaceManager mInterfaceManager = new InterfaceManager();
    private boolean needRender = true;
    private RelativeLayout headersLayout;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.article_ref_content, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final CustomWebView articleRefContent = findView(R.id.articleRefContentWebView);
        final IssueMO issue;
        try {
            issue = issueService.getIssue(doi);
        } catch (final ElementNotFoundException e) {
            throw new RuntimeException(e);
        }

        progress = findView(R.id.progress);
        progress.setVisibility(View.GONE);

        tocComponent = new TocComponent(this, articleRefContent, issue);
        tocComponent.setHeadersLayout(headersLayout);
        tocComponent.onCreateHost();

        GANHelper.trackPageView("/home/issue-toc/v"
                + issue.getVolumeNumber() + "." + issue.getIssueNumber(), true);
    }

    @Override
    public void onDestroyView() {
        tocComponent.onDestroyHost();
        super.onDestroyView();
    }

    private void updateSections() {
        onContentLoadingStarted();
        importManager.updateIssuesTOC(doi);
        showContent();
    }

     private Handler progressHideHandler = new Handler();

    private void onContentLoadingStarted() {
        progressHideHandler.removeCallbacksAndMessages(null);
        progress.clearAnimation();
        progress.setAlpha(1.0f);
        progress.setVisibility(View.VISIBLE);
        IssueTOCFragment issueTOCActivity = mInterfaceManager.getIssueTocActivity();
        if (null == issueTOCActivity)
            return;
        issueTOCActivity.onTocLoadingStateChanged(true);
    }

    private void onContentLoadingStopped() {
        progressHideHandler.removeCallbacksAndMessages(null);
        progressHideHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                fadeOut.setDuration(500);
                progress.startAnimation(fadeOut);
            }
        }, 200);
        if (isAdded()) {
            mInterfaceManager.getIssueTocActivity().onTocLoadingStateChanged(false);
        }
    }

    private static final int LoaderId_UpdateIssueTOCList = IdUtils.generateIntId();
    private void showContent() {
        final LoaderManager lm = getActivity().getLoaderManager();
        if (lm.getLoader(LoaderId_UpdateIssueTOCList) != null) {
            lm.restartLoader(LoaderId_UpdateIssueTOCList, null, loaderCallbacks);
        } else {
            lm.initLoader(LoaderId_UpdateIssueTOCList, null, loaderCallbacks);
        }
    }

    private final LoaderManager.LoaderCallbacks<List<ArticleMO>> loaderCallbacks = new LoaderManager.LoaderCallbacks<List<ArticleMO>>() {
        @Override
        public Loader<List<ArticleMO>> onCreateLoader(int id, Bundle args) {
            return new AsyncTaskLoader<List<ArticleMO>>(getContext()) {
                @Override
                public List<ArticleMO> loadInBackground() {
                    return articleService.getArticlesForIssueTOC(doi);
                }

                @Override
                protected void onStartLoading() {
                    super.onStartLoading();
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<ArticleMO>> loader, List<ArticleMO> data) {
            if (!data.isEmpty()) {
                tocComponent.render(data);
                needRender = false;
            } else {
                needRender = true;
            }

            final Activity activity = getActivity();
            if (activity != null) {
                final LoaderManager loaderManager = activity.getLoaderManager();
                if (loaderManager != null) {
                    loaderManager.destroyLoader(LoaderId_UpdateIssueTOCList);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ArticleMO>> loader) {
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        tocComponent.onStart();
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_SUCCESS.getEventName(), successProcessor);
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_NOT_MODIFIED.getEventName(), notModifiedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_ERROR.getEventName(), errorProcessor);
        notificationCenter.subscribeToNotification(ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), savedArticlesListChangedProcessor);
        notificationCenter.subscribeToNotification(ALL_CONTENT_UPDATE_STARTED.getEventName(), allContentUpdateStartedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_LIST_UPDATED.getEventName(), issuesListUpdatedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_LIST_NOT_MODIFIED.getEventName(), issuesListNotModifiedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_LIST_ERROR.getEventName(), issuesListErrorProcessor);
        updateSections();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        tocComponent.onOrientationChanged();
    }

    @Override
    public void onStop() {
        notificationCenter.unSubscribeFromNotification(successProcessor);
        notificationCenter.unSubscribeFromNotification(notModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(errorProcessor);
        notificationCenter.unSubscribeFromNotification(savedArticlesListChangedProcessor);
        notificationCenter.unSubscribeFromNotification(allContentUpdateStartedProcessor);
        notificationCenter.unSubscribeFromNotification(issuesListUpdatedProcessor);
        notificationCenter.unSubscribeFromNotification(issuesListNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(issuesListErrorProcessor);
        tocComponent.onStop();
        progressHideHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    @Override
    public void onSectionSelected(final String sectionIdentifier) {
        tocComponent.selectSection(sectionIdentifier);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.issue_toc_menu, menu);
        final MenuItem item = menu.findItem(R.id.issue_toc_edit_saved);
        if (item != null) {
            final long savedCount = articleService.getSavedArticleCount();
            item.setTitle("(" + savedCount + ")");
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.issue_toc_edit_saved) {
            mInterfaceManager.toggleSavedArticleEditor();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final boolean handled = tocComponent.onActivityResult(requestCode, resultCode, data);
        if (!handled) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRenderCompleted() {
        onContentLoadingStopped();
    }

    public void setDownloadIssueOnContentUpdate(boolean downloadIssueOnContentUpdate) {
        this.downloadIssueOnContentUpdate = downloadIssueOnContentUpdate;
    }

    public void executePostponedTasks() {
        if (tocComponent != null) {
            tocComponent.executePostponedTasks();
        }
    }

    public void refreshHeads(int scrollY, int offset) {
        tocComponent.onScrollViewScrolled(scrollY, offset);
    }

    public void setHeadersLayout(RelativeLayout headersLayout) {
        this.headersLayout = headersLayout;
    }

    private class InterfaceManager {

        public void toggleSavedArticleEditor() {
            getIssueTocActivity().toggleSavedArticleEditor();
        }

        private IssueTOCFragment getIssueTocActivity() {
            return (IssueTOCFragment) getParentFragment();
        }

    }
}
