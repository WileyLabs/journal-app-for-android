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
package com.wiley.android.journalApp.fragment.issue;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.IssueTOCFragment;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.IssueView;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.notification.IssueDownloadProgressProcessor;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import roboguice.inject.InjectExtra;

import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_STARTED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_REMOVED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.SECTION_DID_BECOME_VISIBLE;
import static com.wiley.wol.client.android.settings.Settings.DOWNLOAD_ISSUE;


public abstract class BaseIssueTocSectionsFragment extends JournalFragment {

    public interface OnSectionSelectedListener {
        void onSectionSelected(String section);
    }

    @Inject
    protected IssueService issueService;
    @Inject
    protected NotificationCenter notificationCenter;
    @InjectExtra("DOI")
    protected DOI doi;
    @Inject
    protected LayoutInflater inflater;

    protected OnSectionSelectedListener sectionSelectedListener;
    protected ListView sectionsListView;
    protected IssueView issueView;
    protected String currentSelectionId = "";
    protected BaseAdapter sectionsAdapter = new SectionsAdapter();

    protected final List<SectionItem> sectionItems = new ArrayList<>();

    private final NotificationProcessor successProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            updateUi();
        }
    };

    private final NotificationProcessor sectionBecomeVisibleProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            String sectionId = (String) params.get("section_id");
            onSectionBecomeVisible(sectionId);
        }
    };

    Long previousDownloadedSize = 0L;
    private final NotificationProcessor downloadIssueProgressProcessor = new IssueDownloadProgressProcessor() {
        @Override
        protected void onZipDownloadStarted(DOI doi) {
            previousDownloadedSize = 0L;
        }

        @Override
        protected void onZipDownloadProgress(DOI doi, long currentProgress, long totalProgress) {
            if (currentProgress - previousDownloadedSize < 100000 &&
                    currentProgress != totalProgress) {
                return;
            }
            if (BaseIssueTocSectionsFragment.this.doi.equals(doi)) {
                issueView.onDownloadProgress(currentProgress / (1024f * 1024f), totalProgress / (1024f * 1024f));
            }
        }

        @Override
        protected void onImportStarted(DOI doi) {
            if (BaseIssueTocSectionsFragment.this.doi.equals(doi)) {
                Logger.d("IssueTocSections", "import started");
                issueView.onIssueImportingStarted();
            }
        }
    };

    private NotificationProcessor issueDownloadStartedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI downloadDoi = (DOI) params.get(DOWNLOAD_ISSUE);
            if (downloadDoi.equals(doi)) {
                Logger.d("IssueTocSections", "download started");
                issueView.onDownloadStarted();
            }
        }
    };

    private NotificationProcessor issueDownloadedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI downloadDoi = (DOI) params.get(DOWNLOAD_ISSUE);
            if (downloadDoi.equals(doi)) {
                Logger.d("IssueTocSections", "downloaded");
                fillIssueView();
            }
        }
    };
    private NotificationProcessor issueDownloadErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI downloadDoi = (DOI) params.get(DOWNLOAD_ISSUE);
            if (downloadDoi.equals(doi)) {
                Logger.d("IssueTocSections", "download error");
                issueView.setDownloadButtonEnabled(true);
                fillIssueView();
            }
        }
    };
    private NotificationProcessor issueRemovedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final DOI removedDoi = (DOI) params.get(NotificationCenter.DOI);
            if (doi.equals(removedDoi)) {
                issueView.onIssueRemoved();
            }
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.frag_issue_toc_sections, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUi();
        updateUi();
    }

    protected void setupIssueView(IssueView issueView) {
        issueView.setHost((IssueView.IssueViewHost) getParentFragment());
        issueView.setShowCover(theme.isShowCovers());
        issueView.setDownloadStartClickListener(new IssueView.OnDownloadStartClickListener() {
            @Override
            public void onClick(IssueView sender, DOI doi) {
                getIssueTocActivity().onNeedDownloadIssue(sender, doi);
            }
        });
        issueView.setDownloadCancelClickListener(new IssueView.OnDownloadCancelClickListener() {
            @Override
            public void onClick(IssueView sender, DOI doi) {
                getIssueTocActivity().onNeedCancelIssueDownload(sender, doi);
            }
        });
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        try {
            sectionSelectedListener = (OnSectionSelectedListener) getParentFragment();
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSectionSelectedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(ISSUE_TOC_UPDATE_SUCCESS.getEventName(), successProcessor);
        if (!theme.isJournalHasNoTocForIssues()) {
            notificationCenter.subscribeToNotification(SECTION_DID_BECOME_VISIBLE.getEventName(), sectionBecomeVisibleProcessor);
        }
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_PROGRESS.getEventName(), downloadIssueProgressProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_STARTED.getEventName(), issueDownloadStartedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_SUCCESS.getEventName(), issueDownloadedProcessor);
        notificationCenter.subscribeToNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), issueDownloadErrorProcessor);
        notificationCenter.subscribeToNotification(ISSUE_REMOVED.getEventName(), issueRemovedProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(successProcessor);
        if (!theme.isJournalHasNoTocForIssues()) {
            notificationCenter.unSubscribeFromNotification(sectionBecomeVisibleProcessor);
        }
        notificationCenter.unSubscribeFromNotification(downloadIssueProgressProcessor);
        notificationCenter.unSubscribeFromNotification(issueDownloadStartedProcessor);
        notificationCenter.unSubscribeFromNotification(issueDownloadedProcessor);
        notificationCenter.unSubscribeFromNotification(issueDownloadErrorProcessor);
        notificationCenter.unSubscribeFromNotification(issueRemovedProcessor);
    }

    @Override
    public void onResume() {
        super.onResume();
        fillIssueView();
    }

    private void updateUi() {
        fillIssueView();
        fillSections();
    }

    protected abstract void initUi();

    private void fillSections() {
        if (theme.isJournalHasNoTocForIssues()) {
            return;
        }

        final List<SectionMO> sectionsForTOC = issueService.tryGetSectionsForTOC(doi);
        sectionsAdapter.notifyDataSetInvalidated();
        sectionItems.clear();
        for (SectionMO section : sectionsForTOC) {
            sectionItems.add(new SectionItem(section));
        }
        sectionsAdapter.notifyDataSetChanged();
    }

    private void fillIssueView() {
        final IssueMO issue;
        try {
            issue = issueService.getIssue(doi);
        } catch (final ElementNotFoundException e) {
            throw new RuntimeException(e);
        }
        issueView.fillBy(issue);
    }

    private void onSectionBecomeVisible(String sectionId) {
        if (!currentSelectionId.equals(sectionId)) {
            int currentSelection = sectionsListView.getCheckedItemPosition();
            if (currentSelection != ListView.INVALID_POSITION) {
                sectionsListView.setItemChecked(sectionsListView.getCheckedItemPosition(), false);
            }
            sectionsListView.setItemChecked(findSectionPositionById(sectionId), true);

            currentSelectionId = sectionId;
        }
    }

    protected int findSectionPositionById(String sectionId) {
        for (int i = 0; i < sectionItems.size(); i++) {
            if (sectionId.equals(sectionItems.get(i).getId())) {
                return i;
            }
        }
        return 0;
    }

    private IssueTOCFragment getIssueTocActivity() {
        return (IssueTOCFragment) getParentFragment();
    }

    public void onTocLoadingStateChanged(boolean loading) {
        issueView.setDownloadButtonEnabled(!loading);
    }

    private class SectionsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return sectionItems.size();
        }

        @Override
        public Object getItem(int position) {
            return sectionItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return sectionItems.get(position).getView(convertView, parent);
        }
    }

    protected class SectionItem {

        private String name;
        private String id = "";

        public SectionItem(SectionMO section) {
            if (section != null) {
                name = HtmlUtils.stripHtml(section.getName());
                id = section.getUid();
            }
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public View getView(View convertView, ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = (TextView) inflater.inflate(R.layout.issue_toc_section_list_item, parent, false);
            }
            view.setText(name);
            view.setTag(id);
            return view;
        }
    }

}
