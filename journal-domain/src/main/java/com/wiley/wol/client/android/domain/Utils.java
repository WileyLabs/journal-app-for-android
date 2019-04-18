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

/**
 * Created by Andrey Rylov on 19/05/14.
 */
public final class Utils {
    private Utils() {
    }

    public static String oneLetterHash(String text) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        char h = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            h = (char) (h ^ c);
        }
        return Character.toString(alphabet.charAt(h % alphabet.length()));
    }

    public static long getContentExpireTimeInterval() {
        return 1000L * 60 * 60 * 24 * 180; // half-year
    }
}
