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

import android.content.Context;
import android.content.res.Resources;

import com.wiley.wol.client.android.log.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ContentIOUtils {
    private final static String TAG = ContentIOUtils.class.getSimpleName();

    public static String getRawResourceContent(final Resources resources, final int rawResId) {
        final InputStream in = resources.openRawResource(rawResId);
        try {
            return getInputStreamContent(in);
        } catch (final IOException e) {
            throw new RuntimeException("Can't read resource: " + rawResId);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Logger.s(TAG, "Error closing stream", e);
            }
        }
    }

    public static boolean hasAssetsContent(final Context context, final String filename) {
        try {
            InputStream stream = context.getAssets().open(filename);
            stream.close();
            return true;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return false;
    }

    public static String getAssetsContent(final Context context, final String filename) {
        InputStream in = null;
        try {
            in = context.getAssets().open(filename);
            return getInputStreamContent(in);
        } catch (final IOException e) {
            throw new RuntimeException("Can't read assets: " + filename);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                Logger.s(TAG, "Error closing stream", e);
            }
        }
    }

    public static String getInputStreamContent(InputStream in) throws IOException {
        final byte[] buffer = new byte[2048];
        final StringBuilder content = new StringBuilder();
        int read = 0;
        while ((read = in.read(buffer)) > 0) {
            content.append(new String(buffer, 0, read, "UTF-8"));
        }
        return content.toString();

    }
}
