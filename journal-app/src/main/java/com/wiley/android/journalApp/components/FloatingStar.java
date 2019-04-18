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
package com.wiley.android.journalApp.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.wiley.android.journalApp.R;

/**
 * Created by taraskreknin on 30.05.14.
 */
public class FloatingStar extends FrameLayout {

    public static enum State {
        Activated,
        Deactivated,
        Spinning
    }

    private static final int ACTIVE_IMAGE_PATH = R.drawable.favorite_highlighted_selector;
    private static final int NOT_ACTIVE_IMAGE_PATH = R.drawable.favorite_normal_selector;

    private final Animation mFadeInAnimation = new AlphaAnimation(0, 1);

    private State mState = State.Deactivated;

    private ImageView mStarView;
    private ProgressBar mProgress;

    private Animation.AnimationListener mAnimListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
    };

    public FloatingStar(Context context) {
        super(context);
        init();
    }

    public FloatingStar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingStar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mState = State.Deactivated;

        mStarView = new ImageView(getContext());
        mStarView.setImageResource(NOT_ACTIVE_IMAGE_PATH);
        addView(mStarView);

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        mProgress = (ProgressBar) inflater.inflate(R.layout.circular_progress, this, false);
        mProgress.setVisibility(View.GONE);
        addView(mProgress);

        mFadeInAnimation.setInterpolator(new AccelerateInterpolator());
        mFadeInAnimation.setDuration(500);
        mFadeInAnimation.setAnimationListener(mAnimListener);
    }

    public void hide() {
        setVisibility(View.INVISIBLE);
    }

    public void show(boolean animated) {
        if (!animated) {
            setVisibility(View.VISIBLE);
        } else {
            showAnimated();
        }
    }

    private void showAnimated() {
        startAnimation(mFadeInAnimation);
    }

    public State getState() {
        return mState;
    }

    public void setState(State newState) {
        mState = newState;
        switch (mState) {
            case Activated:
                mStarView.setImageResource(ACTIVE_IMAGE_PATH);
                mStarView.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.GONE);
                break;
            case Deactivated:
                mStarView.setImageResource(NOT_ACTIVE_IMAGE_PATH);
                mStarView.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.GONE);
                break;
            case Spinning:
                mStarView.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);
                break;
        }
    }
}
