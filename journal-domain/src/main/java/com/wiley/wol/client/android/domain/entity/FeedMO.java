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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by taraskreknin
 * on 02.10.14.
 */
public class FeedMO extends BaseRssMO {
    private static final String HS_ITEMS_TABLET = "hsitemstablet";
    private static final String HS_ITEMS_PHONE = "hsitemsphone";
    private static final String FEED_COLOR = "feedcolor";
    private static final List<String> DEFAULT_COLORS = Arrays.asList("#ff0000", "#990033", "#0000ff");

    @DatabaseField
    private String url;
    @DatabaseField
    private String params;
    @ForeignCollectionField(eager = true)
    private Collection<FeedItemMO> items;

    public Collection<FeedItemMO> getItems() {
        return items;
    }

    public void setItems(Collection<FeedItemMO> items) {
        this.items = items;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getParams() {
        return params;
    }

    public void setParams(final String params) {
        this.params = params;
    }

    public int getNumberOfItemsOnHomeScreen(final boolean isTablet) {
        final String numberString;
        final int defaultValue;
        if (isTablet) {
            defaultValue = 3;
            numberString = getParameter(HS_ITEMS_TABLET);
        } else {
            defaultValue = 1;
            numberString = getParameter(HS_ITEMS_PHONE);
        }

        int result = 0;
        try {
            result = Integer.valueOf(numberString);
        } catch (NumberFormatException ignore) {
        }

        if (result == 0) {
            result = defaultValue;
        }

        return result;
    }

    public String getFeedColor() {
        final String feedColor = getParameter(FEED_COLOR);
        return feedColor != null ? feedColor : "";
    }

    public String getDefaultColorForIndex(int index) {
        return DEFAULT_COLORS.get(index % DEFAULT_COLORS.size());
    }

    private String getParameter(String name) {
        final String[] parameters = params.split(";");
        for (String parameter : parameters) {
            final String[] keyValue = parameter.split("=");
            if (keyValue[0].equals(name)) {
                return keyValue[1];
            }
        }

        return null;
    }
}
