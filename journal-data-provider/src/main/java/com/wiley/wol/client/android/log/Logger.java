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
package com.wiley.wol.client.android.log;

public final class Logger {
    private Logger() {
    }

    public static void i(final String tag, final String msg) {
//        Log.i(tag, msg);
    }

    public static void d(final String tag, final String message) {
//        Log.d(tag, message);
    }

    public static void d(final String tag, final String msg, final Throwable tr) {
//        Log.d(tag, msg, tr);
    }

    public static void s(final String tag, final Throwable throwable) {
//        Log.e(tag, throwable.getMessage(), throwable);
    }

    public static void s(final String tag, final String message) {
//        Log.e(tag, message);
    }

    public static void s(final String tag, final String message, final Throwable throwable) {
//        Log.e(tag, message, throwable);
    }

    public static void v(final java.lang.String tag, final java.lang.String msg) {
//        Log.v(tag, msg);
    }

    public static void w(final String tag, final String msg) {
//        Log.w(tag, msg);
    }
}
