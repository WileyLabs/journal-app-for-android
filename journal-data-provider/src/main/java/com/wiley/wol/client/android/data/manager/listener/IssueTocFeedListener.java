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
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.error.ParseException;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_TOC_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

public class IssueTocFeedListener implements Listener<InputStream> {
    private static final String TAG = IssueTocFeedListener.class.getSimpleName();

    @Inject
    private IssueDao issueDao;
    @Inject
    private IssueSimpleParser issueParser;
    @Inject
    private NotificationCenter notificationCenter;

    @Override
    public void onComplete(final InputStream result, final Object... additionalData) throws Exception {
        Logger.d(TAG, "onComplete");
        final IssueMO issueFeed;
        try {
            issueFeed = issueParser.parseIssue(result);
        } catch (final Exception ex) {
            throw new ParseException(ex);
        }

        IssueMO issueStored;
        try {
            issueStored = issueDao.findOne(issueFeed.getDOI());
            issueFeed.setUid(issueStored.getUid());
            issueFeed.setIsLocal(issueStored.isLocal());
            issueFeed.setIsSampleIssue(issueStored.isSampleIssue());
        } catch (ElementNotFoundException ignored) {
        }

        issueFeed.setTocImportingDate(new Date());
        issueFeed.setRestricted(hasRestrictedArticles(issueFeed));
        issueFeed.setIsNew(false);
        issueDao.save(issueFeed);
        notificationCenter.sendNotification(ISSUE_TOC_UPDATE_SUCCESS.getEventName());
    }

    private boolean hasRestrictedArticles(IssueMO issueFeed) {
        for (SectionMO sec : issueFeed.getSections()) {
            if (sec.hasRestrictedArticles()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNotModified() {
        Logger.d(TAG, "onNotModified");
        notificationCenter.sendNotification(ISSUE_TOC_UPDATE_NOT_MODIFIED.getEventName());
    }

    @Override
    public void onError(final Exception ex) {
        Logger.d(TAG, "onError");
        Logger.s(TAG, ex.getMessage(), ex);
        final HashMap<String, Object> params = new HashMap<>();
        params.put(ERROR, ex);
        notificationCenter.sendNotification(ISSUE_TOC_UPDATE_ERROR.getEventName(), params);
    }
}
