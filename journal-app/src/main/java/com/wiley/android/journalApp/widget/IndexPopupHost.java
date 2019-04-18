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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.wiley.android.journalApp.utils.UIUtils;

/**
 * Created by Andrey Rylov on 02/06/14.
 */
public class IndexPopupHost extends ViewGroup implements CircleIndexIndicator.PopupCallback {

    public IndexPopupHost(Context context) {
        super(context);
    }

    public IndexPopupHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IndexPopupHost(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Point popupPosition = null;
    private View popup = null;
    private CharSequence popupText = null;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (popup == null) {
            popup = getChildAt(0);
            updatePopupText();
            popup.setVisibility(View.GONE);
            popup.setAlpha(0.0f);
        }

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (popup != null) {
            popup.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.AT_MOST, width), MeasureSpec.makeMeasureSpec(MeasureSpec.AT_MOST, height));
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (popup != null) {
            Point rightCenter = popupPosition;
            if (rightCenter == null)
                rightCenter = new Point(r, t + (b - t) / 2);
            int popupWidth = popup.getMeasuredWidth();
            int popupHeight = popup.getMeasuredHeight();

            popup.layout(
                    rightCenter.x - popupWidth,
                    rightCenter.y - popupHeight / 2,
                    rightCenter.x,
                    rightCenter.y + popupHeight / 2);
        }
    }

    private void updatePopupText() {
        if (popup != null && popup instanceof TextView)
            ((TextView) popup).setText(popupText);
    }

    private ValueAnimator showAnimator = null;
    private Animator.AnimatorListener showAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            showAnimator = null;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            showAnimator = null;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {}
    };

    private ValueAnimator.AnimatorUpdateListener showAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float value = (Float) animation.getAnimatedValue();
            if (popup != null)
                popup.setAlpha(value);
        }
    };

    private static final long popupAnimationDuration = 300;
    private static final long popupShowAnimationDelay = 100;

    @Override
    public void showPopup(CircleIndexIndicator indicator, PointF at, CharSequence text) {
        if (popup != null)
            popup.setVisibility(View.VISIBLE);
        if (showAnimator == null) {
            showAnimator = ValueAnimator.ofFloat(popup.getAlpha(), 1.0f);
            showAnimator.setDuration(popupAnimationDuration);
            showAnimator.setStartDelay(popupShowAnimationDelay);
            showAnimator.addListener(showAnimatorListener);
            showAnimator.addUpdateListener(showAnimatorUpdateListener);
            showAnimator.start();
        }
        popupPosition = UIUtils.convertBetweenViews(new Point((int) at.x, (int) at.y), indicator, this);
        popupText = text;
        updatePopupText();
        requestLayout();
    }

    @Override
    public void hidePopup(CircleIndexIndicator indicator) {
        if (popup != null) {
            popup.setVisibility(View.GONE);
            popup.setAlpha(0.0f);
        }
        if (showAnimator != null) {
            showAnimator.cancel();
            showAnimator = null;
        }
    }
}
