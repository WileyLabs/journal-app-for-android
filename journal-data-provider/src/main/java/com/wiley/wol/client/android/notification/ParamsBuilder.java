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
package com.wiley.wol.client.android.notification;

import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.error.AppErrorCode;

import java.util.HashMap;
import java.util.List;

import static com.wiley.wol.client.android.notification.NotificationCenter.APP_ERROR_CODE;
import static com.wiley.wol.client.android.notification.NotificationCenter.ARTICLE_DOI;
import static com.wiley.wol.client.android.notification.NotificationCenter.DOI;
import static com.wiley.wol.client.android.notification.NotificationCenter.DOI_LIST;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR_MESSAGE;
import static com.wiley.wol.client.android.notification.NotificationCenter.ISSUE_DOI;
import static com.wiley.wol.client.android.notification.NotificationCenter.MODE;
import static com.wiley.wol.client.android.notification.NotificationCenter.SPECIAL_SECTION_ID;
import static com.wiley.wol.client.android.notification.NotificationCenter.TITLE_LIST;

/**
 * Created by taraskreknin on 18.08.14.
 */
public class ParamsBuilder {

    private final HashMap<String, Object> params = new HashMap<>();

    public ParamsBuilder withDoi(DOI doi) {
        return withParam(DOI, doi);
    }

    public ParamsBuilder withDoiList(final List<DOI> dois) {
        return withParam(DOI_LIST, dois);
    }

    public ParamsBuilder withArticleDoi(final DOI doi) {
        return withParam(ARTICLE_DOI, doi);
    }

    public ParamsBuilder withIssueDoi(final DOI doi) {
        return withParam(ISSUE_DOI, doi);
    }

    public ParamsBuilder withTitleList(final String title) {
        return withParam(TITLE_LIST, title);
    }

    public ParamsBuilder withSpecialSectionId(final String specialSectionId) {
        return withParam(SPECIAL_SECTION_ID, specialSectionId);
    }

    public ParamsBuilder withError(Object error) {
        return withParam(ERROR, error);
    }

    public ParamsBuilder withAppErrorCode(AppErrorCode error) {
        return withParam(APP_ERROR_CODE, error);
    }

    public ParamsBuilder withMode(final String mode) {
        return withParam(MODE, mode);
    }

    public ParamsBuilder withErrorMessage(final String message) {
        return withParam(ERROR_MESSAGE, message);
    }

    public ParamsBuilder withIssue(IssueMO issue) {
        return withParam(IssueService.ISSUE_MO, issue);
    }

    public ParamsBuilder withFeedItemContent(final String feedItemContent) {
        return withParam(NotificationCenter.FEED_ITEM_CONTENT, feedItemContent);
    }

    public ParamsBuilder withFeedItemUrl(final String feedItemUrl) {
        return withParam(NotificationCenter.FEED_ITEM_URL, feedItemUrl);
    }

    public ParamsBuilder withParam(String key, Object value) {
        params.put(key, value);
        return this;
    }

    public ParamsBuilder succeed(boolean success) {
        return withParam(NotificationCenter.SUCCESS, success);
    }

    public ParamsBuilder notModified(boolean notModified) {
        return withParam(NotificationCenter.NOT_MODIFIED, notModified);
    }

    public ParamsBuilder withFeedMO(FeedMO feed) {
        return withParam(NotificationCenter.FEED_MO, feed);
    }

    public ParamsBuilder withUid(String uid) {
        return withParam(NotificationCenter.UID, uid);
    }

    public HashMap<String, Object> get() {
        return params;
    }
}
