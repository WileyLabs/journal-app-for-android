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
package com.wiley.android.journalApp.layout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.wiley.android.journalApp.R;

public class LeftMenu extends ViewGroup {

    public interface Listener {
        void onContentStateChanged(ContentState newContentState);

        void hideInput();
    }

    protected Listener listener = null;

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    protected void onContentStateChanged(ContentState newContentState) {
        if (listener != null) {
            listener.onContentStateChanged(newContentState);
        }
    }

    public int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    protected int handleXOffsetDp = 0;

    public LeftMenu(Context context) {
        super(context);
        init();
    }

    public LeftMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LeftMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        ViewConfiguration config = ViewConfiguration.get(getContext());
        this.manualScrollingChangePageSlopPx = 2 * ViewConfigurationCompat.getScaledPagingTouchSlop(config);
        this.manualScrollingStartSlopPx = 2 * config.getScaledTouchSlop();

        sideMenuWidthPx = (int) getContext().getResources().getDimension(R.dimen.article_side_menu_width);
        centerMenuWidthPx = (int) getContext().getResources().getDimension(R.dimen.article_center_menu_content_width);
        handleXOffsetPx = dpToPx(handleXOffsetDp);
        setMinimumWidth(sideMenuWidthPx + sideMenuWidthPx);

        if (!isInEditMode()) {
            changeContentState(ContentState.Closed);
        }
    }

    protected void initChildViews() {
        content = getChildAt(0);
        centerMenu = getChildAt(1);
        handle = getChildAt(2);
        sideMenu = getChildAt(3);

        if (centerMenu != null) {
            centerMenu.setClickable(true);
        }

        if (handle != null) {
            handle.setClickable(true);
            handle.setOnClickListener(onHandleClickListener);
        }

        if (sideMenu != null) {
            sideMenu.setClickable(true);
        }

        shadow = new View(getContext());
        shadow.setBackgroundResource(R.drawable.slider_line);
        shadow.setVisibility(GONE);
        this.addView(shadow);

        content.bringToFront();
        centerMenu.bringToFront();
        shadow.bringToFront();
        handle.bringToFront();
        sideMenu.bringToFront();
    }

    private View.OnClickListener onHandleClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onHandleClick();
        }
    };

    protected View content = null;
    protected View sideMenu = null;
    protected View centerMenu = null;
    protected View handle = null;
    protected View shadow = null;
    protected boolean childViewsInitialized = false;

    protected int sideMenuWidthPx = 0;
    protected int centerMenuWidthPx = 0;
    protected int contentWidthPx = 0;
    protected int shadowWidthPx = 0;
    protected int handleXOffsetPx = 0;

    private float centerMenuFactor = 0.0f;

    private boolean resizeContent = true;

    private boolean menuHided = false;
    private float hideMenuFactor = 0.0f;
    private ValueAnimator menuHideAnimator = null;

    public void setResizeContent(boolean resizeContent) {
        if (this.resizeContent != resizeContent) {
            this.resizeContent = resizeContent;
            requestLayout();
        }
    }

    public void hideMenu() {
        if (menuHided) {
            return;
        }

        if (menuHideAnimator != null) {
            menuHideAnimator.cancel();
            menuHideAnimator = null;
        }

        menuHideAnimator = ValueAnimator.ofFloat(this.hideMenuFactor, 1.0f);
        menuHideAnimator.setDuration(getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));
        menuHideAnimator.setInterpolator(interpolator);
        menuHideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                hideMenuFactor = (Float) animation.getAnimatedValue();
                requestLayout();
                if (hideMenuFactor == 1.0f) {
                    onContentStateChanged(ContentState.Closed);
                }
            }
        });
        menuHideAnimator.start();

        menuHided = true;
    }

    public void showMenu() {
        if (!menuHided) {
            return;
        }

        if (menuHideAnimator != null) {
            menuHideAnimator.cancel();
            menuHideAnimator = null;
        }

        menuHideAnimator = ValueAnimator.ofFloat(this.hideMenuFactor, 0.0f);
        menuHideAnimator.setDuration(getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));
        menuHideAnimator.setInterpolator(interpolator);
        menuHideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                hideMenuFactor = (Float) animation.getAnimatedValue();
                requestLayout();
                if (hideMenuFactor == 0.0f) {
                    onContentStateChanged(ContentState.Open);
                }
            }
        });
        menuHideAnimator.start();

        menuHided = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!childViewsInitialized) {
            initChildViews();
            childViewsInitialized = true;
        }

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (sideMenu != null) {
            sideMenu.measure(MeasureSpec.makeMeasureSpec(sideMenuWidthPx, MeasureSpec.EXACTLY), heightMeasureSpec);
        }
        if (centerMenu != null) {
            centerMenu.measure(MeasureSpec.makeMeasureSpec(centerMenuWidthPx, MeasureSpec.EXACTLY), heightMeasureSpec);
        }

        contentWidthPx = width - (int) (sideMenuWidthPx * (1.0f - hideMenuFactor));

        if (resizeContent) {
            int centerMenuVisibleWidth = (int) (centerMenuFactor * centerMenuWidthPx * (1.0f - hideMenuFactor));
            contentWidthPx = contentWidthPx - centerMenuVisibleWidth;
        }
        if (shadowWidthPx == 0 && shadow != null) {
            shadow.measure(MeasureSpec.UNSPECIFIED, heightMeasureSpec);
            shadowWidthPx = shadow.getMeasuredWidth();
        }
        if (content != null) {
            content.measure(MeasureSpec.makeMeasureSpec(contentWidthPx, MeasureSpec.EXACTLY), heightMeasureSpec);
        }
        if (shadow != null) {
            shadow.measure(MeasureSpec.makeMeasureSpec(shadowWidthPx, MeasureSpec.EXACTLY), heightMeasureSpec);
        }
        if (handle != null) {
            handle.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int height = b - t;

        int sideMenuWidth = sideMenu == null ? 0 : sideMenu.getMeasuredWidth();
        int sideMenuLeft = -(int) (hideMenuFactor * sideMenuWidth);

        if (sideMenu != null) {
            sideMenu.layout(sideMenuLeft, 0, sideMenuLeft + sideMenuWidth, height);
        }

        int centerMenuWidth = centerMenu == null ? 0 : centerMenu.getMeasuredWidth();
        int centerMenuVisibleWidth = (int) (centerMenuFactor * centerMenuWidth * (1.0f - hideMenuFactor));
        int centerMenuLeft = sideMenuLeft + sideMenuWidth - (centerMenuWidth - centerMenuVisibleWidth);
        if (centerMenu != null) {
            centerMenu.layout(centerMenuLeft, 0, centerMenuLeft + centerMenuWidth, height);
        }

        int contentWidth = content == null ? 0 : content.getMeasuredWidth();
        int contentLeft;
        if (resizeContent) {
            contentLeft = centerMenuLeft + centerMenuWidth;
        } else {
            contentLeft = sideMenuLeft + sideMenuWidth;
        }

        if (contentLeft < 0) {
            contentLeft = 0;
        }

        if (content != null) {
            content.layout(contentLeft, 0, contentLeft + contentWidth, height);
        }

        int handleWidth = handle == null ? 0 : handle.getMeasuredWidth();
        int handleHeight = handle == null ? 0 : handle.getMeasuredHeight();
        int handleLeft = centerMenuLeft + centerMenuWidth;
        if (hideMenuFactor > 0.0f) {
            handleLeft = handleLeft - (int) (handleWidth * hideMenuFactor);
        } else if (centerMenuVisibleWidth == 0) {
            handleLeft = handleLeft - handleWidth;
        } else {
            handleLeft = handleLeft + handleXOffsetPx;
        }
        int handleTop = height / 2 - handleHeight / 2;
        if (handle != null) {
            handle.layout(handleLeft, handleTop, handleLeft + handleWidth, handleTop + handleHeight);
        }

        int shadowWidth = shadow == null ? 0 : shadow.getMeasuredWidth();
        int shadowLeft = centerMenuLeft + centerMenuWidth;
        if (hideMenuFactor > 0.0f) {
            shadowLeft = shadowLeft - (int) (shadowWidth * hideMenuFactor);
        } else if (centerMenuVisibleWidth == 0) {
            shadowLeft = shadowLeft - shadowWidth;
        }
        if (shadow != null) {
            shadow.layout(shadowLeft, 0, shadowLeft + shadowWidth, height);
        }
    }

    public enum ContentState {
        None,
        Closed,
        Open
    }

    protected ContentState contentState = ContentState.None;

    public ContentState getContentState() {
        return contentState;
    }

    public void changeContentState(ContentState newContentState) {
        if (contentState == newContentState) {
            return;
        }
        contentState = newContentState;
        switch (contentState) {
            case Closed:
                centerMenuFactor = 0.0f;
                break;
            case Open:
                centerMenuFactor = 1.0f;
                break;
            default:
                break;
        }
        requestLayout();
        finishScrolling();
    }

    public void changeContentStateAnimated(ContentState newContentState) {
        if (contentState == newContentState) {
            return;
        }
        contentState = newContentState;
        float targetCenterMenuFactor = this.centerMenuFactor;
        switch (contentState) {
            case Closed:
                targetCenterMenuFactor = 0.0f;
                break;
            case Open:
                targetCenterMenuFactor = 1.0f;
                break;
            default:
                break;
        }
        if (contentScrolling != ScrollingState.None) {
            stopScrolling();
        }
        contentScrolling = ScrollingState.Auto;
        startScrolling();
        doScroll(targetCenterMenuFactor, 0.0f);
    }

    protected Scroller centerMenuScroller = null;

    private static final Interpolator interpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    protected Runnable scrollRunnable = new Runnable() {

        @Override
        public void run() {
            if (centerMenuScroller != null) {
                if (centerMenuScroller.computeScrollOffset()) {
                    int percent = centerMenuScroller.getCurrX();
                    centerMenuFactor = (float) percent / 100.0f;
                    requestLayout();
                }
                if (centerMenuScroller.isFinished()) {
                    centerMenuScroller = null;
                }
            }

            boolean needContinue = centerMenuScroller != null;
            if (needContinue) {
                ViewCompat.postOnAnimation(LeftMenu.this, this);
            } else {
                finishScrolling();
            }
        }
    };

    protected void startScrolling() {
        centerMenu.setVisibility(View.VISIBLE);
        handle.setVisibility(View.VISIBLE);
        sideMenu.setVisibility(View.VISIBLE);
    }

    protected void finishScrolling() {
        stopScrolling();
        boolean wasCanceling = contentScrolling == ScrollingState.ManualCanceling;
        contentScrolling = ScrollingState.None;
        if (!wasCanceling) {
            onContentStateChanged(this.contentState);
        }

        int centerVisibility = this.contentState == ContentState.Open ? View.VISIBLE : View.GONE;
        if (centerMenu != null) {
            centerMenu.setVisibility(centerVisibility);
        }
        if (handle != null) {
            handle.setVisibility(centerVisibility);
        }
        if (shadow != null) {
            shadow.setVisibility(centerVisibility);
        }
    }

    protected void stopScrolling() {
        this.removeCallbacks(scrollRunnable);
    }

    protected void doScroll(float targetCenterMenuFactor, float velocity) {
        if (this.centerMenuFactor != targetCenterMenuFactor) {
            centerMenuScroller = new Scroller(getContext(), interpolator);
            int from = (int) (this.centerMenuFactor * 100.0f);
            int to = (int) (targetCenterMenuFactor * 100.0f);
            int dx = to - from;
            int duration = calculateScrollDuration(dx, 100, velocity);
            centerMenuScroller.startScroll(from, 0, dx, 0, duration);
        }
        if (this.centerMenuScroller != null) {
            ViewCompat.postOnAnimation(this, scrollRunnable);
        } else {
            finishScrolling();
        }
    }

    private static final int scrollDurationMax = 600;

    private int calculateScrollDuration(int dx, int width, float velocity) {
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth *
                distanceInfluenceForSnapDuration(distanceRatio);

        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageDelta = (float) Math.abs(dx) / width;
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, scrollDurationMax);
        return duration;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f;
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    protected void onHandleClick() {
        switch (contentState) {
            case Open:
                hideInput();
                changeContentStateAnimated(ContentState.Closed);
                break;
            default:
                break;
        }
    }

    protected enum ScrollingState {
        None,
        Auto,
        Manual,
        ManualEnding,
        ManualCanceling
    }

    private ScrollingState contentScrolling = ScrollingState.None;

    private VelocityTracker velocityTracker = null;
    private int manualScrollingPointerId = -1;
    private boolean manualScrollingWasLongDx = false;
    private int manualScrollingStartX = 0;
    private int manualScrollingStartY = 0;
    private int manualScrollingDeltaX = 0;
    private int manualScrollingStartSlopPx = 0;
    private int manualScrollingChangePageSlopPx = 0;

    protected boolean startManualScrolling(int touchX, int touchY) {
        if (contentScrolling == ScrollingState.Manual) {
            return false;
        }

        int contentX = (int) (centerMenuWidthPx * centerMenuFactor);
        int delta = touchX - contentX;

        stopScrolling();

        startScrolling();

        manualScrollingStartX = touchX;
        manualScrollingStartY = touchY;
        manualScrollingDeltaX = delta;
        manualScrollingWasLongDx = false;
        contentScrolling = ScrollingState.Manual;

        return true;
    }

    protected boolean updateManualScrolling(int touchX, int touchY) {
        if (contentScrolling != ScrollingState.Manual) {
            return false;
        }

        int contentPosition = touchX - manualScrollingDeltaX;
        if (contentPosition < 0) {
            contentPosition = 0;
        } else {
            int maxPosition = centerMenuWidthPx;
            if (contentPosition > maxPosition) {
                contentPosition = maxPosition;
            }
        }

        if (!manualScrollingWasLongDx) {
            int deltaY = Math.abs(manualScrollingStartY - touchY);
            boolean wasLongY = deltaY > dpToPx(30);
            if (wasLongY) {
                cancelManualScrolling();
                return false;
            }

            int startContentPosition = manualScrollingStartX - manualScrollingDeltaX;
            int deltaX = Math.abs(contentPosition - startContentPosition);

            if (deltaX > this.manualScrollingStartSlopPx) {
                manualScrollingWasLongDx = true;
                hideInput();
            }
        }

        if (!manualScrollingWasLongDx) {
            return false;
        }

        centerMenuFactor = (float) contentPosition / (float) centerMenuWidthPx;
        requestLayout();
        return true;
    }

    private boolean isScrollingCompleted() {
        int position = (int) (centerMenuWidthPx * centerMenuFactor);
        int target = 0;
        return position == target;
    }

    protected void cancelManualScrolling() {
        if (contentScrolling != ScrollingState.Manual) {
            return;
        }
        if (!isScrollingCompleted()) {
            contentScrolling = ScrollingState.ManualCanceling;
            velocityTracker.computeCurrentVelocity(1000);
            float velocity = VelocityTrackerCompat.getXVelocity(velocityTracker, manualScrollingPointerId);
            doScroll(contentState == ContentState.Open ? 1.0f : 0.0f, velocity);
        } else {
            contentScrolling = ScrollingState.ManualCanceling;
            finishScrolling();
        }
    }

    protected void finishManualScrolling() {
        if (contentScrolling != ScrollingState.Manual) {
            return;
        }

        int position = (int) (centerMenuWidthPx * centerMenuFactor);
        int width = centerMenuWidthPx;
        velocityTracker.computeCurrentVelocity(1000);
        float velocity = VelocityTrackerCompat.getXVelocity(velocityTracker, manualScrollingPointerId);

        if (contentState == ContentState.Closed &&
                position >= this.manualScrollingChangePageSlopPx &&
                manualScrollingWasLongDx &&
                velocity > 0.0f) {
            contentState = ContentState.Open;
            contentScrolling = ScrollingState.ManualEnding;
            doScroll(1.0f, velocity);
        } else if (contentState == ContentState.Open &&
                position <= width - this.manualScrollingChangePageSlopPx &&
                manualScrollingWasLongDx &&
                velocity < 0.0f) {
            contentState = ContentState.Closed;
            contentScrolling = ScrollingState.ManualEnding;
            doScroll(0.0f, velocity);
        } else {
            cancelManualScrolling();
        }
    }

    private void obtainVelocityTracker() {
        recycleVelocityTracker();
        velocityTracker = VelocityTracker.obtain();
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int pointerIndex = MotionEventCompat.getActionIndex(event);
        int pointerId = event.getPointerId(pointerIndex);
        int x = (int) event.getX(pointerIndex);
        int y = (int) event.getY(pointerIndex);

        switch (contentState) {
            case Open:
                if (!isViewUnder(handle, x, y) && !isViewUnder(centerMenu, x, y)) {
                    return super.onInterceptTouchEvent(event);
                }
                break;
            default:
                return super.onInterceptTouchEvent(event);
        }

        if (contentScrolling == ScrollingState.Manual) {
            if (pointerId == manualScrollingPointerId) {
                switch (MotionEventCompat.getActionMasked(event)) {
                    case MotionEvent.ACTION_MOVE: {
                        updateManualScrolling(x, y);
                        velocityTracker.addMovement(event);
                        return manualScrollingWasLongDx;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        cancelManualScrolling();
                        recycleVelocityTracker();
                        return false;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        velocityTracker.addMovement(event);
                        finishManualScrolling();
                        recycleVelocityTracker();
                        return false;
                    default:
                        break;
                }
            }
        } else {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                if (startManualScrolling(x, y)) {
                    obtainVelocityTracker();
                    velocityTracker.addMovement(event);
                    manualScrollingPointerId = pointerId;
                }
            }
        }

        return manualScrollingWasLongDx;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = MotionEventCompat.getActionIndex(event);
        int pointerId = event.getPointerId(pointerIndex);
        int x = (int) event.getX(pointerIndex);
        int y = (int) event.getY(pointerIndex);

        if (contentScrolling == ScrollingState.Manual) {
            if (pointerId == manualScrollingPointerId) {
                switch (MotionEventCompat.getActionMasked(event)) {
                    case MotionEvent.ACTION_MOVE: {
                        updateManualScrolling(x, y);
                        velocityTracker.addMovement(event);
                        return true;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        cancelManualScrolling();
                        recycleVelocityTracker();
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        velocityTracker.addMovement(event);
                        finishManualScrolling();
                        recycleVelocityTracker();
                        return true;
                    default:
                        break;
                }
            }
        } else {
            switch (contentState) {
                case Open:
                    if (!isViewUnder(handle, x, y) && !isViewUnder(centerMenu, x, y)) {
                        return super.onTouchEvent(event);
                    }
                    break;
                default:
                    return super.onTouchEvent(event);
            }
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                if (startManualScrolling(x, y)) {
                    obtainVelocityTracker();
                    velocityTracker.addMovement(event);
                    manualScrollingPointerId = pointerId;
                    return true;
                }
            }
        }
        return true;
    }

    protected boolean isViewUnder(View view, int x, int y) {
        return view != null &&
                x >= view.getLeft() &&
                x < view.getRight() &&
                y >= view.getTop() &&
                y < view.getBottom();
    }

    private void hideInput() {
        if (listener != null) {
            listener.hideInput();
        }
    }
}
