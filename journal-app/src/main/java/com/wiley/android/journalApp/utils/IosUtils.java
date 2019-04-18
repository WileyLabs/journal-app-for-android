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
package com.wiley.android.journalApp.utils;

import android.graphics.Rect;

/**
 * Created by Andrey Rylov on 23/07/14.
 */
public class IosUtils {

    public static Rect parseIosRectFromString(String string) {
        String s = string.replaceAll("(\\{|\\}|,)", "");
        Rect rect = Rect.unflattenFromString(s);
        if (rect != null) {
            rect.right += rect.left;
            rect.bottom += rect.top;
        }
        return rect;
    }
}
