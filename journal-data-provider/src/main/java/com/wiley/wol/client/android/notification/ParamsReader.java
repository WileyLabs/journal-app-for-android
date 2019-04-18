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

import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.error.AppErrorCode;

import java.util.List;
import java.util.Map;

/**
 * Created by taraskreknin on 03.10.14.
 */
public class ParamsReader {

    private final Map<String, Object> mParams;

    public ParamsReader(Map<String, Object> params) {
        mParams = params;
    }

    public boolean succeed() {
        return hasParam(NotificationCenter.SUCCESS)
                && (boolean) mParams.get(NotificationCenter.SUCCESS);
    }

    public boolean hasParam(String key) {
        return mParams != null && mParams.containsKey(key);
    }

    public <T> T getParam(String key) {
        if (hasParam(key)) {
            return (T) mParams.get(key);
        } else {
            return null;
        }
    }

    public String getUid() {
        return getParam(NotificationCenter.UID);
    }

    public FeedMO getFeedMO() {
        return getParam(NotificationCenter.FEED_MO);
    }

    public boolean ignoreLastModified() {
        final Boolean ignoreLastModified = getParam(NotificationCenter.IGNORE_LAST_MODIFIED);
        return ignoreLastModified != null && ignoreLastModified;
    }

    public String getFeedItemContent() {
        return getParam(NotificationCenter.FEED_ITEM_CONTENT);
    }

    public String getFeedItemUrl() {
        return getParam(NotificationCenter.FEED_ITEM_URL);
    }

    public int getError() {
        return getParam(NotificationCenter.ERROR);
    }

    public AppErrorCode getAppErrorCode() {
        return getParam(NotificationCenter.APP_ERROR_CODE);
    }

    public String getMode() {
        return getParam(NotificationCenter.MODE);
    }

    public String getErrorMessage() {
        return getParam(NotificationCenter.ERROR_MESSAGE);
    }

    public String getTitleList() {
        return getParam(NotificationCenter.TITLE_LIST);
    }

    public List<DOI> getDoiList() {
        return getParam(NotificationCenter.DOI_LIST);
    }

    public DOI getArticleDoi() {
        return getParam(NotificationCenter.ARTICLE_DOI);
    }

    public DOI getIssueDoi() {
        return getParam(NotificationCenter.ISSUE_DOI);
    }

    public String getSpecialSectionId() {
        return getParam(NotificationCenter.SPECIAL_SECTION_ID);
    }
}
