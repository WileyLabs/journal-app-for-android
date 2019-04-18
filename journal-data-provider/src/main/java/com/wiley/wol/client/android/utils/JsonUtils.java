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
package com.wiley.wol.client.android.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public final class JsonUtils {
    private JsonUtils() {
    }

    public static boolean jsonObjectEquals(JSONObject o1, JSONObject o2) throws JSONException {
        if (o1 == o2) {
            return true;
        }

        if (o1 == null || o2 == null) {
            return false;
        }

        if (o1.length() != o2.length()) {
            return false;
        }

        final Iterator it = o1.keys();
        while (it.hasNext()) {
            final String key = (String) it.next();
            if (!o2.has(key)) {
                return false;
            }

            Object value1 = o1.get(key);
            Object value2 = o2.get(key);

            if (value1.getClass() != value2.getClass()) {
                return false;
            }

            if (value1.getClass() == JSONObject.class) {
                if (!jsonObjectEquals((JSONObject) value1, (JSONObject) value2)) {
                    return false;
                }
            } else {
                if (!value1.equals(value2)) {
                    return false;
                }
            }
        }

        return true;
    }
}
