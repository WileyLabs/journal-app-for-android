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
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.utils.UIUtils;

public class LeftMenuWithTwoPanels extends ViewGroup {

    public interface Listener {
        void onContentStateStartChanging(ContentState oldContentState, ContentState newContentState);

        void onContentStateChanged(ContentState newContentState);

        void hideInput();
    }

    protected enum ScrollingState {
        None,
        Auto,
        Manual,
        ManualEnding,
        ManualCanceling
    }

    protected Listener listener = null;

    protected View sideMenu = null;
    protected View centerMenu = null;
    protected View handle = null;
    protected View cover = null;
    protected boolean childViewsInitialized = false;

    protected int sideMenuWidthPx = 0;
    protected int centerMenuWidthPx = 0;
    protected int coverFullColor = 0;

    private float sideMenuFactor = 0.0f;
    private float centerMenuFactor = 0.0f;

    private boolean menuHided = false;
    private float hideMenuFactor = 0.0f;
    private ValueAnimator menuHideAnimator = null;

    private ScrollingState contentScrolling = ScrollingState.None;

    private VelocityTracker velocityTracker = null;
    private int manualScrollingPointerId = -1;
    private boolean manualScrollingWasLongDx = false;
    private int manualScrollingStartX = 0;
    private int manualScrollingStartY = 0;
    private int manualScrollingDeltaX = 0;
    private int manualScrollingStartSlopPx = 0;
    private int manualScrollingChangePageSlopPx = 0;
    protected Scroller sideMenuScroller = null;
    protected Scroller centerMenuScroller = null;

    private View.OnClickListener onHandleClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (contentScrolling == ScrollingState.None) {
                onHandleClick();
            }
        }
    };


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

    protected void onContentStateStartChanging(ContentState oldContentState, ContentState newContentState) {
        if (listener != null) {
            listener.onContentStateStartChanging(oldContentState, newContentState);
        }
    }

    public LeftMenuWithTwoPanels(Context context) {
        super(context);
        init();
    }

    public LeftMenuWithTwoPanels(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LeftMenuWithTwoPanels(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        ViewConfiguration config = ViewConfiguration.get(getContext());
        this.manualScrollingChangePageSlopPx = 2 * ViewConfigurationCompat.getScaledPagingTouchSlop(config);
        this.manualScrollingStartSlopPx = 2 * config.getScaledTouchSlop();

        sideMenuWidthPx = (int) getContext().getResources().getDimension(R.dimen.article_side_menu_width);
        setMinimumWidth(2 * sideMenuWidthPx);

        if (!isInEditMode()) {
            changeContentState(ContentState.Closed);
        }
    }

    protected void initChildViews() {
        centerMenu = getChildAt(0);
        sideMenu = getChildAt(1);
        handle = getChildAt(2);

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

        coverFullColor = getContext().getResources().getColor(R.color.article_view_content_cover_background);
        cover = new View(getContext());
        cover.setBackgroundColor(Color.TRANSPARENT);
        this.addView(cover);

        cover.bringToFront();
        centerMenu.bringToFront();
        sideMenu.bringToFront();
        handle.bringToFront();
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

        if (cover != null) {
            cover.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
        if (sideMenu != null) {
            sideMenu.measure(MeasureSpec.makeMeasureSpec(sideMenuWidthPx, MeasureSpec.EXACTLY), heightMeasureSpec);
        }
        centerMenuWidthPx = width - sideMenuWidthPx;
        if (centerMenu != null) {
            centerMenu.measure(MeasureSpec.makeMeasureSpec(centerMenuWidthPx, MeasureSpec.EXACTLY), heightMeasureSpec);
        }
        if (handle != null) {
            handle.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;

        if (cover != null) {
            int coverAlpha = (int) (centerMenuFactor * (float) Color.alpha(coverFullColor));
            int coverColor = Color.argb(coverAlpha, Color.red(coverFullColor), Color.green(coverFullColor), Color.blue(coverFullColor));
            cover.setBackgroundColor(coverColor);
            cover.layout(0, 0, width, height);
        }

        int handleWidth = handle == null ? 0 : handle.getMeasuredWidth();
        int handleHeight = handle == null ? 0 : handle.getMeasuredHeight();

        int sideMenuWidth = sideMenu == null ? 0 : sideMenu.getMeasuredWidth();
        int sideMenuVisibleWidth = (int) (sideMenuFactor * sideMenuWidth);
        int sideMenuLeft = 0 - (sideMenuWidth - sideMenuVisibleWidth) - (int) ((sideMenuVisibleWidth + handleWidth) * hideMenuFactor);
        if (sideMenu != null) {
            sideMenu.layout(sideMenuLeft, 0, sideMenuLeft + sideMenuWidth, height);
        }

        int centerMenuWidth = centerMenu == null ? 0 : centerMenu.getMeasuredWidth();
        int centerMenuVisibleWidth = (int) (centerMenuFactor * centerMenuWidth * (1.0f - hideMenuFactor));
        int centerMenuLeft = sideMenuLeft + sideMenuWidth - (centerMenuWidth - centerMenuVisibleWidth);
        if (centerMenu != null) {
            centerMenu.layout(centerMenuLeft, 0, centerMenuLeft + centerMenuWidth, height);
        }

        boolean flipHandle = false;
        int handleLeft = contentState == ContentState.OpenCenter
                ? centerMenuLeft + centerMenuWidth - handleWidth
                : centerMenuLeft + centerMenuWidth;
        if (contentState == ContentState.OpenCenter) {
            flipHandle = true;
        }
        if (handleLeft + handleWidth > width) {
            handleLeft = width - handleWidth;
            flipHandle = true;
        }
        if (handleLeft < sideMenuLeft + sideMenuWidth) {
            handleLeft = sideMenuLeft + sideMenuWidth;
        }
        int handleTop = height - handleHeight;
        if (handle != null) {
            handle.setScaleX(flipHandle ? -1.0f : 1.0f);
            handle.layout(handleLeft, handleTop, handleLeft + handleWidth, handleTop + handleHeight);
        }
    }

    public enum ContentState {
        None,
        Closed,
        OpenSide,
        OpenCenter
    }

    protected ContentState contentState = ContentState.None;

    public ContentState getContentState() {
        return contentState;
    }

    public void changeContentState(ContentState newContentState) {
        onContentStateStartChanging(this.contentState, newContentState);
        contentState = newContentState;
        switch (contentState) {
            case Closed:
                sideMenuFactor = 0.0f;
                centerMenuFactor = 0.0f;
                break;
            case OpenSide:
                sideMenuFactor = 1.0f;
                centerMenuFactor = 0.0f;
                break;
            case OpenCenter:
                sideMenuFactor = 1.0f;
                centerMenuFactor = 1.0f;
                break;
            default:
                break;
        }
        requestLayout();
        finishScrolling();
    }

    public void changeContentStateAnimated(final ContentState newContentState) {
        if (contentState == newContentState) {
            return;
        }
        onContentStateStartChanging(this.contentState, newContentState);
        contentState = newContentState;
        final float targetSideMenuFactor;
        final float targetCenterMenuFactor;
        switch (contentState) {
            case Closed:
                targetSideMenuFactor = 0.0f;
                targetCenterMenuFactor = 0.0f;
                break;
            case OpenSide:
                targetSideMenuFactor = 1.0f;
                targetCenterMenuFactor = 0.0f;
                break;
            case OpenCenter:
                targetSideMenuFactor = 1.0f;
                targetCenterMenuFactor = 1.0f;
                break;
            default:
                targetSideMenuFactor = this.sideMenuFactor;
                targetCenterMenuFactor = this.centerMenuFactor;
                break;
        }

        if (contentScrolling != ScrollingState.None) {
            stopScrolling();
        }

        contentScrolling = ScrollingState.Auto;
        startScrolling();
        doScroll(targetSideMenuFactor, targetCenterMenuFactor, 0.0f);
    }

    private static final Interpolator interpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    protected Runnable scrollRunnable = new Runnable() {

        @Override
        public void run() {
            if (sideMenuScroller != null) {
                if (sideMenuScroller.computeScrollOffset()) {
                    int percent = sideMenuScroller.getCurrX();
                    sideMenuFactor = (float) percent / 100.0f;
                    requestLayout();
                }
                if (sideMenuScroller.isFinished()) {
                    sideMenuScroller = null;
                }
            }

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

            boolean needContinue = sideMenuScroller != null || centerMenuScroller != null;
            if (needContinue) {
                ViewCompat.postOnAnimation(LeftMenuWithTwoPanels.this, this);
            } else {
                finishScrolling();
            }
        }
    };

    protected void startScrolling() {
        centerMenu.setVisibility(View.VISIBLE);
        cover.setVisibility(View.VISIBLE);
        sideMenu.setVisibility(View.VISIBLE);
    }

    protected void finishScrolling() {
        stopScrolling();

        boolean wasCanceling = contentScrolling == ScrollingState.ManualCanceling;
        contentScrolling = ScrollingState.None;
        if (!wasCanceling) {
            onContentStateChanged(this.contentState);
        }

        int centerVisibility = this.contentState == ContentState.OpenCenter ? View.VISIBLE : View.GONE;
        int sideVisibility = this.contentState == ContentState.OpenCenter || this.contentState == ContentState.OpenSide ? View.VISIBLE : View.GONE;
        if (centerMenu != null) {
            centerMenu.setVisibility(centerVisibility);
        }
        if (cover != null) {
            cover.setVisibility(centerVisibility);
        }
        if (sideMenu != null) {
            sideMenu.setVisibility(sideVisibility);
        }
    }

    protected void stopScrolling() {
        this.removeCallbacks(scrollRunnable);
    }

    protected void doScroll(float targetSideMenuFactor, float targetCenterMenuFactor, float velocity) {
        if (this.sideMenuFactor != targetSideMenuFactor) {
            sideMenuScroller = new Scroller(getContext(), interpolator);
            int from = (int) (this.sideMenuFactor * 100.0f);
            int to = (int) (targetSideMenuFactor * 100.0f);
            int dx = to - from;
            int duration = calculateScrollDuration(dx, 100, velocity);
            sideMenuScroller.startScroll(from, 0, dx, 0, duration);
        }
        if (this.centerMenuFactor != targetCenterMenuFactor) {
            centerMenuScroller = new Scroller(getContext(), interpolator);
            int from = (int) (this.centerMenuFactor * 100.0f);
            int to = (int) (targetCenterMenuFactor * 100.0f);
            int dx = to - from;
            int duration = calculateScrollDuration(dx, 100, velocity);
            centerMenuScroller.startScroll(from, 0, dx, 0, duration);
        }
        if (this.sideMenuScroller != null || this.centerMenuScroller != null) {
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
            case Closed:
                changeContentStateAnimated(ContentState.OpenSide);
                break;
            case OpenSide:
                changeContentStateAnimated(ContentState.Closed);
                break;
            case OpenCenter:
                changeContentStateAnimated(ContentState.OpenSide);
                break;
            default:
                break;
        }
    }

    protected boolean startManualScrolling(int touchX, int touchY) {
        if (contentScrolling == ScrollingState.Manual) {
            return false;
        }

        int contentX = contentState == ContentState.OpenCenter ? (int) (centerMenuWidthPx * centerMenuFactor) : (int) (sideMenuWidthPx * sideMenuFactor);
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
            int maxPosition = contentState == ContentState.OpenCenter ? centerMenuWidthPx : sideMenuWidthPx;
            if (contentPosition > maxPosition) {
                contentPosition = maxPosition;
            }
        }

        if (!manualScrollingWasLongDx) {
            int deltaY = Math.abs(manualScrollingStartY - touchY);
            boolean wasLongY = deltaY > UIUtils.dpToPx(getContext(), 30);
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

        if (contentState == ContentState.OpenCenter) {
            centerMenuFactor = (float) contentPosition / (float) centerMenuWidthPx;
        } else {
            sideMenuFactor = (float) contentPosition / (float) sideMenuWidthPx;
        }
        requestLayout();
        return true;
    }

    private boolean isScrollingCompleted() {
        if (contentState == ContentState.OpenSide || contentState == ContentState.Closed) {
            int position = (int) (sideMenuWidthPx * sideMenuFactor);
            int target = contentState == ContentState.OpenSide ? sideMenuWidthPx : 0;
            return position == target;
        } else if (contentState == ContentState.OpenCenter) {
            int position = (int) (centerMenuWidthPx * centerMenuFactor);
            int target = 0;
            return position == target;
        } else {
            return true;
        }
    }

    protected void cancelManualScrolling() {
        if (contentScrolling != ScrollingState.Manual) {
            return;
        }
        if (!isScrollingCompleted()) {
            contentScrolling = ScrollingState.ManualCanceling;
            velocityTracker.computeCurrentVelocity(1000);
            float velocity = VelocityTrackerCompat.getXVelocity(velocityTracker, manualScrollingPointerId);
            doScroll(contentState == ContentState.OpenSide || contentState == ContentState.OpenCenter ? 1.0f : 0.0f, contentState == ContentState.OpenCenter ? 1.0f : 0.0f, velocity);
        } else {
            contentScrolling = ScrollingState.ManualCanceling;
            finishScrolling();
        }
    }

    protected void finishManualScrolling() {
        if (contentScrolling != ScrollingState.Manual) {
            return;
        }

        int position = contentState == ContentState.OpenCenter ? (int) (centerMenuWidthPx * centerMenuFactor) : (int) (sideMenuWidthPx * sideMenuFactor);
        int width = contentState == ContentState.OpenCenter ? centerMenuWidthPx : sideMenuWidthPx;
        velocityTracker.computeCurrentVelocity(1000);
        float velocity = VelocityTrackerCompat.getXVelocity(velocityTracker, manualScrollingPointerId);

        if (contentState == ContentState.Closed &&
                position >= this.manualScrollingChangePageSlopPx &&
                manualScrollingWasLongDx &&
                velocity > 0.0f) {
            onContentStateStartChanging(this.contentState, ContentState.OpenSide);
            contentState = ContentState.OpenSide;
            contentScrolling = ScrollingState.ManualEnding;
            doScroll(1.0f, 0.0f, velocity);
        } else if (contentState == ContentState.OpenSide &&
                position <= width - this.manualScrollingChangePageSlopPx &&
                manualScrollingWasLongDx &&
                velocity < 0.0f) {
            onContentStateStartChanging(this.contentState, ContentState.Closed);
            contentState = ContentState.Closed;
            contentScrolling = ScrollingState.ManualEnding;
            doScroll(0.0f, 0.0f, velocity);
        } else if (contentState == ContentState.OpenCenter &&
                position <= width - this.manualScrollingChangePageSlopPx &&
                manualScrollingWasLongDx &&
                velocity < 0.0f) {
            onContentStateStartChanging(this.contentState, ContentState.OpenSide);
            contentState = ContentState.OpenSide;
            contentScrolling = ScrollingState.ManualEnding;
            doScroll(1.0f, 0.0f, velocity);
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
            case Closed:
                if (!isViewUnder(handle, x, y)) {
                    return super.onInterceptTouchEvent(event);
                }
                break;
            case OpenSide:
                if (!isViewUnder(handle, x, y) && !isViewUnder(sideMenu, x, y)) {
                    return super.onInterceptTouchEvent(event);
                }
                break;
            case OpenCenter:
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
                case Closed:
                    if (!isViewUnder(handle, x, y)) {
                        return super.onTouchEvent(event);
                    }
                    break;
                case OpenSide:
                    if (!isViewUnder(handle, x, y) && !isViewUnder(sideMenu, x, y)) {
                        return super.onTouchEvent(event);
                    }
                    break;
                case OpenCenter:
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
