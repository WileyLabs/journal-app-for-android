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
package com.wiley.wol.client.android.data.manager;

import android.os.Parcel;
import android.os.Parcelable;

public enum ResourceType implements Parcelable {
    ARTICLE_ZIP,
    ARTICLE_DIR,
    THUMBNAIL,
    ISSUE_ZIP,
    UNKNOWN;

    public static final Parcelable.Creator<ResourceType> CREATOR = new Creator<ResourceType>() {
        @Override
        public ResourceType createFromParcel(final Parcel source) {
            return valueOf(source.readString());
        }

        @Override
        public ResourceType[] newArray(final int size) {
            throw new RuntimeException("not supported");
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int i) {
        parcel.writeString(this.name());
    }
}
