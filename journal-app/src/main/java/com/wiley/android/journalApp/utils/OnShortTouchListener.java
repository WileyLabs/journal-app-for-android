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

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by taraskreknin on 16.05.14.
 */
public abstract class OnShortTouchListener implements View.OnTouchListener {
    public float sliderDownXValue;

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                sliderDownXValue = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                final float upXValue = event.getX();
                if (sliderDownXValue == upXValue && (event.getEventTime() - event.getDownTime() < 100)) {
                    onShortTouch();
                }
        }

        return false;
    }

    public abstract void onShortTouch();

}
