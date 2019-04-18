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
package com.wiley.wol.client.android.data.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.dao.IssueDao;
import com.wiley.wol.client.android.data.dao.filter.FilterFactory;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.xml.IssueSimpleParser;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE_TYPE;
import static com.wiley.wol.client.android.data.manager.ResourceType.ISSUE_ZIP;
import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ISSUE;
import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_CANCEL;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_STARTED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_REMOVED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static java.util.Collections.sort;

public class IssueServiceImpl implements IssueService {

    private static final String TAG = IssueServiceImpl.class.getSimpleName();

    @Inject
    private AANHelper aanHelper;
    @Inject
    private IssueSimpleParser issueSimpleParser;
    @Inject
    private IssueDao issueDao;
    @Inject
    private ImportManager importManager;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Settings settings;
    @Inject
    private Context context;
    @Inject
    private FilterFactory filterFactory;

    private final Set<DOI> updatingIssuesSet = new HashSet<>();
    private final Set<DOI> removingIssuesSet = new HashSet<>();

    @Override
    public IssueMO getIssue(final DOI doi) throws ElementNotFoundException {
        return issueDao.findOne(doi);
    }

    @Override
    public boolean isIssueLoading(DOI doi) {
        return importManager.isIssueLoading(doi);
    }

    @Override
    public boolean isIssueUpdating(DOI doi) {
        synchronized (updatingIssuesSet) {
            return updatingIssuesSet.contains(doi);
        }
    }

    @Override
    public boolean isIssueRemoving(DOI doi) {
        synchronized (removingIssuesSet) {
            return removingIssuesSet.contains(doi);
        }
    }

    @Override
    public void setIssueUpdating(DOI doi, boolean updating) {
        synchronized (updatingIssuesSet) {
            if (updating) {
                updatingIssuesSet.add(doi);
            } else if (updatingIssuesSet.contains(doi)) {
                updatingIssuesSet.remove(doi);
            }
        }
    }

    @Override
    public void downloadIssue(DOI doi) {
        final IssueMO issue;
        try {
            issue = getIssue(doi);
        } catch (ElementNotFoundException e) {
            throw new RuntimeException(e);
        }
        final HashMap<String, Object> params = new HashMap<>();
        params.put(Settings.DOWNLOAD_ISSUE, doi);
        params.put(RESOURCE_TYPE, ISSUE_ZIP);
        if (issue.isRestricted()) {
            if (!NetUtils.isOnline(context)) {
                params.put(ERROR, NO_CONNECTION_AVAILABLE);
                notificationCenter.sendNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), params);
            } else if (settings.isAuthorized()) {
                params.put(ERROR, ACCESS_FORBIDDEN_ISSUE);
                notificationCenter.sendNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), params);
            }
            return;
        }
        notificationCenter.sendNotification(ISSUE_DOWNLOAD_STARTED.getEventName(), params);
        importManager.loadIssue(doi);
    }

    @Override
    public void stopIssueLoading(DOI doi) {
        {
            aanHelper.trackActionCancelIssueDownload(doi.getValue());
        }

        // send notification
        final HashMap<String, Object> params = new HashMap<>();
        params.put(Settings.DOWNLOAD_ISSUE, doi);
        params.put(RESOURCE_TYPE, ISSUE_ZIP);
        notificationCenter.sendNotification(ISSUE_DOWNLOAD_CANCEL.getEventName(), params);

        // stop loading task
        importManager.stopIssueLoading(doi);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    public void removeLoadedIssues(final List<DOI> dois) {
        synchronized (removingIssuesSet) {
            removingIssuesSet.addAll(dois);
        }
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }

            @Override
            protected Void doInBackground(Void... params) {
                for (DOI doi : dois) {
                    try {
                        IssueMO issue = issueDao.findOne(doi);
                        issue.setIsLocal(false);
                        issue.setImportingDate(null);
                        issue.setTocImportingDate(null);
                        issue.setSections(Collections.<SectionMO>emptyList());
                        issueDao.save(issue);
                        importManager.removeLoadedIssue(doi);
                        synchronized (removingIssuesSet) {
                            removingIssuesSet.remove(doi);
                        }
                        notificationCenter.sendNotification(ISSUE_REMOVED.getEventName(), new ParamsBuilder().withDoi(doi).withIssue(issue).get());
                        {
                            aanHelper.trackActionDeleteIssue(doi.getValue());
                        }
                    } catch (ElementNotFoundException ignore) {
                    }
                }
                return null;
            }

        };
        task.execute();
    }

    @Override
    public List<IssueMO> getIssues() {
        Logger.d(TAG, "Getting issues");

        final List<IssueMO> issues = issueDao.findAll();

        sort(issues, new Comparator<IssueMO>() {
            @Override
            public int compare(final IssueMO lhs, final IssueMO rhs) {
                return rhs.getPublicationDate().compareTo(lhs.getPublicationDate());
            }
        });

        return issues;
    }

    @Override
    public List<SectionMO> getSectionsAndRequestUpdateForTOC(final DOI doi) {
        Logger.d(TAG, "getSectionsAndRequestUpdateForTOC");
        final List<SectionMO> sections;
        try {
            sections = issueDao.getSectionsForTOC(doi);
            importManager.updateIssuesTOC(doi);
        } catch (final ElementNotFoundException e) {
            throw new RuntimeException(e);
        }

        return sections;
    }

    @Override
    public List<SectionMO> tryGetSectionsForTOC(final DOI doi) {
        Logger.d(TAG, "tryGetSectionsForTOC");
        try {
            return issueDao.getSectionsForTOC(doi);
        } catch (final ElementNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getNumOfSavedArticles(final DOI doi) {
        try {
            return issueDao.getNumOfSavedArticles(doi);
        } catch (ElementNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countOf() {
        return issueDao.countOf();
    }

    @Override
    public long getOpenedIssuesCount() {
        try {
            return issueDao.getCount(filterFactory.getFor(IssueMO.class)
                    .where()
                    .isNotNull(IssueMO.TOC_IMPORTING_DATE)
                    .or()
                    .eq(IssueMO.LOCAL, true));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getDownloadedIssuesCount() {
        try {
            return issueDao.getCount(filterFactory.getFor(IssueMO.class)
                    .where()
                    .eq(IssueMO.LOCAL, true));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveRefs(final List<IssueMO> issues) {
        for (IssueMO issue : issues) {
            issueDao.saveRef(issue);
        }
    }

    @Override
    public void updateIssueList() {
        importManager.updateIssueList();
    }
}
