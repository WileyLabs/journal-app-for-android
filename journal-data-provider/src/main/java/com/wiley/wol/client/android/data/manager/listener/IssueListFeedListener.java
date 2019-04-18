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
package com.wiley.wol.client.android.data.manager.listener;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.dao.IssueDao;
import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.xml.IssueSimpleParser;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

public class IssueListFeedListener implements Listener<InputStream> {
    private static final String TAG = IssueListFeedListener.class.getSimpleName();
    @Inject
    private IssueSimpleParser issueParser;
    @Inject
    private IssueDao issueDao;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Settings settings;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");
        final Date now = new Date();
        final int storedCountIssues = issueDao.findAll().size();
        final List<IssueMO> issues;
        try {
            issues = issueParser.parseList(result);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }

        boolean hasSampleIssue = false;
        for (final IssueMO issue : issues) {
            if (issue.isSampleIssue())
                hasSampleIssue = true;
            final IssueMO storedIssue;
            try {
                // importingDate
                issue.setImportingDate(now);

                // sortIndex
                issue.setSortIndex(createSortIndex(issue.getPageRanges()));

                storedIssue = issueDao.findOne(issue.getDOI());
                issue.setUid(storedIssue.getUid());
                issue.setFavoritesCounter(storedIssue.getFavoritesCounter());
                issue.setSections(storedIssue.getSections());
                issue.setIsLocal(storedIssue.isLocal());
                issue.setIsNew(storedIssue.isNew());
                issue.setIsSampleIssue(issue.isSampleIssue());
            } catch (final ElementNotFoundException ignored) {
                issue.setIsLocal(false);

                if (storedCountIssues > 0) {
                    issue.setIsNew(true);
                }

                issue.setFavoritesCounter(0);
            }
            issueDao.saveRef(issue);
        }

        for (final IssueMO issue : issueDao.findAll()) {
            final Date storedDate = issue.getImportingDate();
            if (!now.equals(storedDate)) {
                issueDao.delete(issue);
            }
        }

        notificationCenter.sendNotification(ISSUE_LIST_UPDATED.getEventName());

        settings.setFullAccess(!hasSampleIssue);
    }

    private int createSortIndex(final String pageRanges) {
        int sortIndex = -1;
        final String[] s = pageRanges.split(",");
        for (final String value : s) {
            final int indexOfMinus = value.indexOf("-");
            if (indexOfMinus > 0) {
                try {
                    sortIndex = Integer.parseInt(value.substring(0, indexOfMinus));
                } catch (final NumberFormatException ignored) {
                }

                if (sortIndex >= 0)
                    break;
            }
        }
        return sortIndex;
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");
        notificationCenter.sendNotification(ISSUE_LIST_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");
        Logger.s(TAG, ex.getMessage(), ex);
        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(ISSUE_LIST_ERROR.getEventName(), params);
    }
}
