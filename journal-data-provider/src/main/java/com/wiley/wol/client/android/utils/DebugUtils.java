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

import android.os.Looper;

import com.wiley.wol.client.android.log.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by taraskreknin on 05.09.14.
 */
public class DebugUtils {

    private static final Map<String, Long> sPerformanceTestsNames = new HashMap<>();
    private static final String TAG = "DebugUtils";

    public static void startPerformanceTest(final String name) {
        sPerformanceTestsNames.put(name, System.currentTimeMillis());
    }

    public static long stopPerformanceTest(final String name) {
        long stopTimeMsc = System.currentTimeMillis();
        if (sPerformanceTestsNames.containsKey(name)) {
            long execTimeMsc = stopTimeMsc - sPerformanceTestsNames.get(name);
            sPerformanceTestsNames.remove(name);
            Logger.i(TAG, name + " ended in " + execTimeMsc);
            boolean isUiThread = Thread.currentThread() == Looper.getMainLooper().getThread();
            if (execTimeMsc >= 300 && isUiThread) {
                Logger.w(TAG, name + " executed too long! " + execTimeMsc);
            }
            return stopTimeMsc;
        }
        return -1;
    }

}
