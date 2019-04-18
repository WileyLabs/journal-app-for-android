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
package com.wiley.android.journalApp.base;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.inject.Inject;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.utils.AANHelper;

import roboguice.activity.RoboFragmentActivity;

import static java.lang.String.format;

/**
 * Created by Andrey Rylov on 04/06/14.
 */
public abstract class JournalActivity extends RoboFragmentActivity {

    @Inject
    private AANHelper aanHelper;

    private View rootView = null;
    private Boolean softKeyboardVisible = null;

    public <T extends View> T findView(final int id) {
        return (T) findViewById(id);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Point displaySizePx = UIUtils.getDisplaySizePx(this);
        aanHelper.takeOff(DeviceUtils.isPhone(this), format("%dx%d", displaySizePx.x, displaySizePx.y));

        if (!canRotateToLandscape()) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (rootView != null) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(onRootViewGlobalLayoutListener);
        }
    }

    protected boolean canRotateToLandscape() {
        return DeviceUtils.isTablet(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        aanHelper.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        aanHelper.onResume();
    }

    private ViewTreeObserver.OnGlobalLayoutListener onRootViewGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        private final Rect rect = new Rect();

        // Workaround for soft keyboard visibility events.
        // http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        @Override
        public void onGlobalLayout() {
            rootView.getWindowVisibleDisplayFrame(rect);
            int heightDiff = rootView.getRootView().getHeight() - (rect.bottom - rect.top);
            boolean visible = heightDiff > 100;
            if (softKeyboardVisible == null || softKeyboardVisible != visible) {
                softKeyboardVisible = visible;
                onSoftKeyboardVisibleChanged(visible);
            }
        }
    };

    public boolean isSoftKeyboardVisible() {
        return softKeyboardVisible != null && softKeyboardVisible;
    }

    protected void onSoftKeyboardVisibleChanged(boolean visible) {
    }
}