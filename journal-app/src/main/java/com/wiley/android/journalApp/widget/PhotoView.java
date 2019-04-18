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
import android.util.AttributeSet;

public class PhotoView extends uk.co.senab.photoview.PhotoView {
    public PhotoView(Context context) {
        super(context);
        initPhotoView();
    }

    public PhotoView(Context context, AttributeSet attr) {
        super(context, attr);
        initPhotoView();
    }

    public PhotoView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        initPhotoView();
    }

    private void initPhotoView() {
        setMinimumScale(0.1f);
    }

    protected boolean isZoomed() {
        return getScale() > 1.0;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return isZoomed();
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return isZoomed();
    }
}
