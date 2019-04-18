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

import java.io.File;

public class FileUtils {

    public static File joinPathToFile(String part1, String part2, String ... otherParts) {
        File file = new File(part1, part2);
        for (String part : otherParts) {
            if (part != null) {
                file = new File(file, part);
            }
        }
        return file;
    }

    public static String joinPath(String part1, String part2, String ... otherParts) {
        File file = joinPathToFile(part1, part2, otherParts);
        return file.getAbsolutePath();
    }
}
