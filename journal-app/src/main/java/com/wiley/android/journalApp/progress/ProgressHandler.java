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
package com.wiley.android.journalApp.progress;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.wol.client.android.log.Logger;

/**
 * Created by taraskreknin on 26.09.14.
 */
public class ProgressHandler {

    // id == progress
    private View progress;
    // id == progress_info
    private TextView progressInfo;

    private final String TAG;

    private int depthLevel;

    public ProgressHandler(final Fragment fragment) {
        TAG = ((Object) fragment).getClass().getSimpleName();
        progress = fragment.getView().findViewById(R.id.progress);
        progress.setVisibility(View.GONE);
        progressInfo = (TextView) fragment.getView().findViewById(R.id.progress_info);
    }

    public ProgressHandler(final Activity activity) {
        TAG = ((Object) activity).getClass().getSimpleName();
        progress = activity.findViewById(R.id.progress);
        progress.setVisibility(View.GONE);
        progressInfo = (TextView) activity.findViewById(R.id.progress_info);
    }

    private Handler hideProgressHandler = new Handler();

    public void showProgress(String info) {
        changeProgressInfo(info);
        showProgress();
    }

    public void showProgress() {
        if (progress == null) {
            return;
        }
        depthLevel++;
        Logger.d(TAG, "showProgress, depthLevel " + depthLevel);
        hideProgressHandler.removeCallbacksAndMessages(null);
        progress.setVisibility(View.VISIBLE);
    }

    public void changeProgressInfo(String info) {
        if (progressInfo == null) {
            return;
        }
        progressInfo.setText(info);
    }

    public void hideProgress() {
        if (progress == null) {
            return;
        }
        depthLevel--;
        Logger.d(TAG, "hideProgress, depthLevel  " + depthLevel);
        hideProgressHandler.removeCallbacksAndMessages(null);
        hideProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                fadeOut.setDuration(500);
                progress.startAnimation(fadeOut);
            }
        }, 200);
    }

}
