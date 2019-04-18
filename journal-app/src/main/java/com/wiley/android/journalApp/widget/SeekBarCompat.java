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
package com.wiley.android.journalApp.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * Created by Andrey Rylov on 30/06/14.
 */
public class SeekBarCompat extends SeekBar {
    public SeekBarCompat(Context context) {
        super(context);
    }

    public SeekBarCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarCompat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Drawable thumbCompatHolder;

    @Override
    public void setThumb(Drawable thumb) {
        this.thumbCompatHolder = thumb;
        super.setThumb(thumb);
    }

    public Drawable getThumbCompat() {
        if (Build.VERSION.SDK_INT < 16)
            return thumbCompatHolder;
        else
            return this.getThumb();
    }
}
