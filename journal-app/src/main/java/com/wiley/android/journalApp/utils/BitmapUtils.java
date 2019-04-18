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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;

import com.wiley.wol.client.android.journalApp.theme.ColorUtils;

public class BitmapUtils {

    public static BitmapDrawable makeDrawable(Context context, Bitmap bitmap) {
        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.setGravity(Gravity.CENTER);
        return drawable;
    }

    public static Bitmap applyTint(Bitmap source, int tintColor) {
        if (tintColor == Color.WHITE)
            return source;

        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceColor = source.getPixel(x, y);
                int resultColor = tintColor;
                float sourceLuminance = ColorUtils.getLuminance(sourceColor);
                resultColor = ColorUtils.changeLuminance(resultColor, sourceLuminance);
                resultColor = ColorUtils.changeAlpha(resultColor, Color.alpha(sourceColor));
                result.setPixel(x, y, resultColor);
            }
        }

        result.setDensity(source.getDensity());
        source.recycle();
        return result;
    }

    public static Bitmap applyAlpha(Bitmap source, int alpha) {
        if (alpha == 255)
            return source;

        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);
        Paint paint = new Paint();
        paint.setAlpha(alpha);
        canvas.drawBitmap(source, 0, 0, paint);
        result.setDensity(source.getDensity());
        source.recycle();
        return result;
    }

    public static Bitmap loadResource(Context context, int resourceId) {
        return BitmapFactory.decodeResource(context.getResources(), resourceId);
    }
}
