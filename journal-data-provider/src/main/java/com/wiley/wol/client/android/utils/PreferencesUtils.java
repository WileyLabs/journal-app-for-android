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

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Date;

public class PreferencesUtils {

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void putDate(SharedPreferences preferences, String key, Date date) {
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        putDate(preferencesEditor, key, date);
        preferencesEditor.apply();
    }

    public static void putDate(SharedPreferences.Editor preferencesEditor, String key, Date date) {
        long milliseconds = date == null ? 0L : date.getTime();
        preferencesEditor.putLong(key, milliseconds);
    }

    public static Date getDate(SharedPreferences preferences, String key) {
        long milliseconds = preferences.getLong(key, 0L);
        if (milliseconds == 0L)
            return null;
        return new Date(milliseconds);
    }

}
