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

import android.graphics.Point;

/**
 * Created by Andrey Rylov on 13/05/14.
 */
public class MathUtils {

    public static Point inflateSize(Point originalSize, Point maxSize) {
        float hRatio = 1.0f;
        if (maxSize.y > 0 && originalSize.y > 0)
            hRatio = ((float) maxSize.y) / ((float) originalSize.y);

        float wRatio = 1.0f;
        if (maxSize.x > 0 && originalSize.x > 0)
            wRatio = ((float) maxSize.x) / ((float) originalSize.x);

        float newWidth = Math.round(Math.min(originalSize.x * Math.min(wRatio, hRatio), originalSize.x));
        float newHeight = Math.round(Math.min(originalSize.y * Math.min(wRatio, hRatio), originalSize.y));

        return new Point((int) newWidth, (int) newHeight);
    }
}
