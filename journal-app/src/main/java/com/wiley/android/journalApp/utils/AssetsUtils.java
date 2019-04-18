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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.wiley.wol.client.android.log.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AssetsUtils {
    private final static String TAG = AssetsUtils.class.getSimpleName();
    public static Bitmap loadBitmap(Context context, String filename, int density) {
        try {
            InputStream stream = context.getAssets().open(filename);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            bitmap.setDensity(density);
            return bitmap;
        } catch (IOException e) {
            Logger.d(TAG, e.getMessage(), e);
            return null;
        }
    }

    public static void showBitmap(ImageView imageView, String filename, int density) {
        Context context = imageView.getContext();
        Bitmap bitmap = loadBitmap(context, filename, density);
        imageView.setImageBitmap(bitmap);
    }

    private static Map<String, Boolean> checkedFilesInAssets = new HashMap<>();

    public static boolean existsFileInAssets(Context context, String filename) {
        if (checkedFilesInAssets.containsKey(filename))
            return checkedFilesInAssets.get(filename).booleanValue();

        boolean exists = false;
        try {
            InputStream stream = context.getAssets().open(filename);
            stream.close();
            exists = true;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        checkedFilesInAssets.put(filename, exists);

        return exists;
    }
}
