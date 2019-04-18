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
package com.wiley.wol.client.android.domain.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

/**
 * Created by taraskreknin
 * on 02.10.14.
 */
public class BaseRssMO {
    public static final String SORT_INDEX = "sort_index";

    @DatabaseField(columnName = "uid", id = true)
    protected String uid;
    @DatabaseField(columnName = "in_feed_date", dataType = DataType.DATE)
    protected Date inFeedDate;
    @DatabaseField(columnName = SORT_INDEX)
    protected int sortIndex;
    @DatabaseField
    protected String title;

    public String getUid() {
        return uid;
    }

    public Date getInFeedDate() {
        return inFeedDate;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public String getTitle() {
        return title;
    }

    public void setUid(String uID) {
        this.uid = uID;
    }

    public void setInFeedDate(Date inFeedDate) {
        this.inFeedDate = inFeedDate;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
