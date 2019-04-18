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
package com.wiley.wol.client.android.data.utils;

import com.wiley.wol.client.android.error.AccessForbiddenException;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.error.ConnectionException;
import com.wiley.wol.client.android.error.ParseException;

import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN;
import static com.wiley.wol.client.android.error.AppErrorCode.XML_PARSE_ERROR;

public final class AppErrorUtils {
    private AppErrorUtils() {
    }

    public static AppErrorCode getAppErrorCode(final Throwable error) {
        if (error.getClass() == AccessForbiddenException.class) {
            return ACCESS_FORBIDDEN;
        }

        if (error.getClass() == ParseException.class) {
            return XML_PARSE_ERROR;
        }

        if (error instanceof ConnectionException) {
            return AppErrorCode.NO_CONNECTION_AVAILABLE;
        }

        return AppErrorCode.UNDEFINED;
    }
}
