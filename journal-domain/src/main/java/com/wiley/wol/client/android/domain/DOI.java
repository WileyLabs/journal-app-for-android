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
package com.wiley.wol.client.android.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class DOI implements Parcelable {
    public static final Parcelable.Creator<DOI> CREATOR = new Creator<DOI>() {
        @Override
        public DOI createFromParcel(final Parcel source) {
            return new DOI(source);
        }

        @Override
        public DOI[] newArray(final int size) {
            return new DOI[size];
        }
    };
    private final String value;

    public DOI(final String value) {
        this.value = value;
    }

    private DOI(final Parcel source) {
        value = source.readString();
    }

    public String getValue() {
        return value;
    }

    public String getAssetCompatibleValue() {
        return value.replace('/', '_');
    }

    public String getArticleZipCompatibleValue() {
        return "article_" + value.replace("/", "%2F");
    }

    @Deprecated
    public String getIdCompatibleValue() {
        return value.replace('/', '_');
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DOI)) {
            return false;
        }

        final DOI doi = (DOI) o;
        return value.equals(doi.value);

    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "DOI{value='" + value + "']";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(value);
    }
}
