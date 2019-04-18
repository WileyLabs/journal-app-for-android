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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.wiley.android.journalApp.R;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;

public class CircleIndexIndicator extends View {
    private static final long BACKGROUND_ANIMATION_DURATION = 300;
    private static final long BACKGROUND_SHOW_ANIMATION_DELAY = 100;
    private static final double INDICATOR_SIZE_PERCENTAGES = 0.75;

    private RectF rect;
    private float defaultInnerPadding;

    public interface PopupCallback {
        void showPopup(CircleIndexIndicator indicator, PointF at, CharSequence text);

        void hidePopup(CircleIndexIndicator indicator);
    }

    public interface Listener {
        void onSelectItem(int index);
    }

    private static final int INVALID_POINTER = -1;

    private float mRadiusNormal;
    private float mRadiusSelected;
    private float mInnerPadding;
    private float mBackgroundWidth;
    private final Paint mPaintNormal = new Paint(ANTI_ALIAS_FLAG);
    private final Paint mPaintSelected = new Paint(ANTI_ALIAS_FLAG);
    private final Paint mPaintBackground = new Paint(ANTI_ALIAS_FLAG);
    private int mSelectedItem = 0;
    private int mFocusedItem = -1;
    private int mOrientation;
    private final Point size = new Point();
    private ValueAnimator animatorHideBackground = null;
    private ValueAnimator animatorShowBackground = null;

    private int mActivePointerId = INVALID_POINTER;

    private CharSequence[] mItems = new CharSequence[]{};

    private PopupCallback mPopupCallback = null;
    private List<Listener> mListeners = new ArrayList<>();
    private Context context;

    private static final long radiusAnimationDuration = 200;

    private class ItemInfo {
        public float radius;

        protected ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                radius = (Float) animation.getAnimatedValue();
                invalidate();
            }
        };

        protected ValueAnimator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };

        protected ValueAnimator animator = null;

        protected float animatorTarget = 0.0f;

        public void animateRadiusTo(float newValue) {
            if (this.animator != null) {
                if (animatorTarget == newValue) {
                    return;
                }

                this.animator.cancel();
                this.animator = null;
            }
            this.animatorTarget = newValue;
            this.animator = ValueAnimator.ofFloat(radius, newValue);
            this.animator.setDuration(radiusAnimationDuration);
            this.animator.addUpdateListener(animatorUpdateListener);
            this.animator.addListener(animatorListener);
            this.animator.start();
        }
    }

    private ItemInfo[] mItemInfos = new ItemInfo[]{};

    private void recreateItemInfos() {
        mItemInfos = new ItemInfo[mItems.length];
        for (int i = 0; i < mItems.length; i++) {
            ItemInfo info = new ItemInfo();
            if (i == mSelectedItem) {
                info.radius = mRadiusSelected;
            } else {
                info.radius = mRadiusNormal;
            }
            mItemInfos[i] = info;
        }
    }

    private float mBackgroundFactor = 0.0f;
    private int mColorBackground;

    public CircleIndexIndicator(Context context) {
        this(context, null);
    }

    public CircleIndexIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleIndexIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        if (isInEditMode()) {
            return;
        }

        //Load defaults from resources
        final Resources res = getResources();
        final int defaultColorNormal = res.getColor(R.color.default_circle_indicator_color_normal);
        final int defaultColorSelected = res.getColor(R.color.default_circle_indicator_color_selected);
        final int defaultColorBackground = res.getColor(R.color.default_circle_indicator_color_background);
        final int defaultOrientation = res.getInteger(R.integer.default_circle_indicator_orientation);
        final float defaultRadiusNormal = res.getDimension(R.dimen.default_circle_indicator_radius_normal);
        final float defaultRadiusSelected = res.getDimension(R.dimen.default_circle_indicator_radius_selected);
        defaultInnerPadding = res.getDimension(R.dimen.default_circle_indicator_inner_padding);
        final float defaultBackgroundWidth = res.getDimension(R.dimen.default_circle_indicator_background_width);
        final CharSequence[] defaultItems = res.getTextArray(R.array.default_circle_indicator_items);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleIndexIndicator, defStyle, 0);

        mOrientation = a.getInt(R.styleable.CircleIndexIndicator_android_orientation, defaultOrientation);
        mPaintNormal.setStyle(Style.FILL);
        mPaintNormal.setColor(a.getColor(R.styleable.CircleIndexIndicator_colorNormal, defaultColorNormal));
        mPaintSelected.setStyle(Style.FILL);
        mPaintSelected.setColor(a.getColor(R.styleable.CircleIndexIndicator_colorSelected, defaultColorSelected));
        mPaintBackground.setStyle(Style.FILL);
        mColorBackground = a.getColor(R.styleable.CircleIndexIndicator_colorBackground, defaultColorBackground);
        mPaintBackground.setColor(mColorBackground);
        mRadiusNormal = a.getDimension(R.styleable.CircleIndexIndicator_radiusNormal, defaultRadiusNormal);
        mRadiusSelected = a.getDimension(R.styleable.CircleIndexIndicator_radiusSelected, defaultRadiusSelected);
        mInnerPadding = a.getDimension(R.styleable.CircleIndexIndicator_innerPadding, defaultInnerPadding);
        mBackgroundWidth = a.getDimension(R.styleable.CircleIndexIndicator_backgroundWidth, defaultBackgroundWidth);

        CharSequence[] items = a.getTextArray(R.styleable.CircleIndexIndicator_items);
        if (items == null) {
            items = defaultItems;
        }
        mItems = items;
        recreateItemInfos();

        a.recycle();

        rect = new RectF(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
    }

    public int getColorNormal() {
        return mPaintNormal.getColor();
    }

    public void setColorNormal(int color) {
        mPaintNormal.setColor(color);
        invalidate();
    }

    public int getColorSelected() {
        return mPaintSelected.getColor();
    }

    public void setColorSelected(int color) {
        mPaintSelected.setColor(color);
        invalidate();
    }

    public int getColorBackground() {
        return mPaintBackground.getColor();
    }

    public void setColorBackground(int color) {
        mColorBackground = color;
        mPaintBackground.setColor(color);
        invalidate();
    }

    public void setOrientation(int orientation) {
        switch (orientation) {
            case HORIZONTAL:
            case VERTICAL:
                mOrientation = orientation;
                requestLayout();
                break;

            default:
                throw new IllegalArgumentException("Orientation must be either HORIZONTAL or VERTICAL.");
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setRadiusNormal(float radius) {
        mRadiusNormal = radius;
        invalidate();
    }

    public float getRadiusNormal() {
        return mRadiusNormal;
    }

    public void setRadiusSelected(float radius) {
        mRadiusSelected = radius;
        invalidate();
    }

    public float getRadiusSelected() {
        return mRadiusSelected;
    }

    public float getInnerPadding() {
        return mInnerPadding;
    }

    public void setBackgroundWidth(float width) {
        mBackgroundWidth = width;
        invalidate();
    }

    public float getBackgroundWidth() {
        return mBackgroundWidth;
    }

    public CharSequence[] getItems() {
        return mItems;
    }

    public void setItems(CharSequence[] items) {
        mItems = items;
        recreateItemInfos();
        requestLayout();
        invalidate();
    }

    public void clearItems() {
        CharSequence[] empty = new CharSequence[0];
        setItems(empty);
    }

    public void setPopupCallback(PopupCallback newPopupCallback) {
        mPopupCallback = newPopupCallback;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int count = mItems.length;
        if (count == 0) {
            return;
        }

        int longPaddingBefore;
        int shortPaddingBefore;
        if (mOrientation == HORIZONTAL) {
            longPaddingBefore = getPaddingLeft();
            shortPaddingBefore = getPaddingTop();
        } else {
            longPaddingBefore = getPaddingTop();
            shortPaddingBefore = getPaddingLeft();
        }

        android.view.Display display = ((android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getSize(size);

        final float sizeOfPlaceForOneCircle = (float) (size.y * INDICATOR_SIZE_PERCENTAGES / count);
        mInnerPadding = sizeOfPlaceForOneCircle > mRadiusNormal * 2 ? sizeOfPlaceForOneCircle - mRadiusNormal * 2 : sizeOfPlaceForOneCircle * 0.75f;
        if (mInnerPadding > defaultInnerPadding) {
            mInnerPadding = defaultInnerPadding;
        }

        final float shortOffset = mBackgroundWidth / 2 + shortPaddingBefore;
        final float longOffset = mBackgroundWidth / 2 + longPaddingBefore;

        drawCircles(canvas, count, shortOffset, longOffset);

        if (mBackgroundFactor > 0.0f) {
            drawBackground(canvas, count, longOffset);
        }
    }

    private void drawBackground(final Canvas canvas, final int count, final float longOffset) {
        mPaintBackground.setColor(mColorBackground);
        mPaintBackground.setAlpha((int) (Color.alpha(mColorBackground) * mBackgroundFactor));
        final float bottom = mOrientation == HORIZONTAL ? getHeight() - getPaddingBottom() :
                longOffset + ((count - 1) * mInnerPadding) + mRadiusNormal * 2;
        final float right = mOrientation == HORIZONTAL ?
                longOffset + ((count - 1) * mInnerPadding) + mRadiusNormal * 2  :
                getWidth() - getPaddingRight();

        rect.set(getPaddingLeft(), getPaddingTop(), right, bottom);
        canvas.drawRoundRect(
                rect,
                mBackgroundWidth / 2, mBackgroundWidth / 2, mPaintBackground);
    }

    private void drawCircles(final Canvas canvas, final int count, final float shortOffset, final float longOffset) {
        int itemForHighlight = mFocusedItem;
        if (itemForHighlight < 0) {
            itemForHighlight = mSelectedItem;
        }

        float dX;
        float dY;
        for (int i = 0; i < count; i++) {
            float drawLong = longOffset + (i * mInnerPadding);
            if (mOrientation == HORIZONTAL) {
                dX = drawLong;
                dY = shortOffset;
            } else {
                dX = shortOffset;
                dY = drawLong;
            }
            ItemInfo itemInfo = mItemInfos[i];
            canvas.drawCircle(dX, dY, itemInfo.radius, i == itemForHighlight ? mPaintSelected : mPaintNormal);
        }
    }

    @Override
    public int getPaddingTop() {
        if (mOrientation == HORIZONTAL) {
            return super.getPaddingTop();
        } else {
            android.view.Display display = ((android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            display.getSize(size);

            return (int) (size.y * (1. - INDICATOR_SIZE_PERCENTAGES) / 2);
        }
    }

    @Override
    public int getPaddingBottom() {
        if (mOrientation == HORIZONTAL) {
            return super.getPaddingBottom();
        } else {
            android.view.Display display = ((android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            display.getSize(size);

            return (int) (size.y * (1. - INDICATOR_SIZE_PERCENTAGES) / 2);
        }
    }

    @Override
    public int getPaddingRight() {
        if (mOrientation != HORIZONTAL) {
            return super.getPaddingRight();
        } else {
            android.view.Display display = ((android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            display.getSize(size);

            return (int) (size.x * (1. - INDICATOR_SIZE_PERCENTAGES) / 2);
        }
    }

    @Override
    public int getPaddingLeft() {
        if (mOrientation != HORIZONTAL) {
            return super.getPaddingLeft();
        } else {
            android.view.Display display = ((android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            display.getSize(size);
            return (int) (size.x * (1. - INDICATOR_SIZE_PERCENTAGES) / 2);
        }
    }

    private final ValueAnimator.AnimatorUpdateListener backgroundAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mBackgroundFactor = (Float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private final Animator.AnimatorListener backgroundAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (animation == animatorShowBackground) {
                animatorShowBackground = null;
            }
            if (animation == animatorHideBackground) {
                animatorHideBackground = null;
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            if (animation == animatorShowBackground) {
                animatorShowBackground = null;
            }
            if (animation == animatorHideBackground) {
                animatorHideBackground = null;
            }
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    };

    protected void showBackground() {
        if (animatorShowBackground != null) {
            return;
        }

        stopHideBackground();

        animatorShowBackground = ValueAnimator.ofFloat(mBackgroundFactor, 1.0f);
        animatorShowBackground.setDuration(BACKGROUND_ANIMATION_DURATION);
        animatorShowBackground.setStartDelay(BACKGROUND_SHOW_ANIMATION_DELAY);
        animatorShowBackground.addUpdateListener(backgroundAnimatorUpdateListener);
        animatorShowBackground.addListener(backgroundAnimatorListener);
        animatorShowBackground.start();
    }

    protected void stopShowBackground() {
        if (animatorShowBackground != null) {
            animatorShowBackground.cancel();
            animatorShowBackground = null;
        }
    }

    protected void hideBackground() {
        if (animatorHideBackground != null) {
            return;
        }

        stopShowBackground();

        animatorHideBackground = ValueAnimator.ofFloat(mBackgroundFactor, 0.0f);
        animatorHideBackground.setDuration(BACKGROUND_ANIMATION_DURATION);
        animatorHideBackground.addUpdateListener(backgroundAnimatorUpdateListener);
        animatorHideBackground.addListener(backgroundAnimatorListener);
        animatorHideBackground.start();
    }

    protected void stopHideBackground() {
        if (animatorHideBackground != null) {
            animatorHideBackground.cancel();
            animatorHideBackground = null;
        }
    }

    protected void focusItem(int index) {
        if (mFocusedItem != index) {
            mFocusedItem = index;
            updateItemRadiuses();
        }
    }

    private void updateItemRadiuses() {
        boolean hasFocus = mFocusedItem >= 0;
        for (int i = 0; i < mItemInfos.length; i++) {
            ItemInfo itemInfo = mItemInfos[i];
            if (hasFocus) {
                if (i == mSelectedItem) {
                    itemInfo.animateRadiusTo(mRadiusNormal);
                } else if (i == mFocusedItem) {
                    itemInfo.animateRadiusTo(mRadiusSelected);
                } else {
                    itemInfo.animateRadiusTo(mRadiusNormal);
                }
            } else {
                if (i == mSelectedItem) {
                    itemInfo.animateRadiusTo(mRadiusSelected);
                } else {
                    itemInfo.animateRadiusTo(mRadiusNormal);
                }
            }
        }
    }

    private void showPopup(PointF at, CharSequence text) {
        if (mPopupCallback != null) {
            mPopupCallback.showPopup(this, at, text);
        }
    }

    private void hidePopup() {
        if (mPopupCallback != null) {
            mPopupCallback.hidePopup(this);
        }
    }

    public boolean onTouchEvent(android.view.MotionEvent ev) {
        if (super.onTouchEvent(ev)) {
            return true;
        }

        if (mItems.length == 0) {
            return false;
        }

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                showBackground();
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float x = MotionEventCompat.getX(ev, activePointerIndex);
                final float y = MotionEventCompat.getY(ev, activePointerIndex);
                int hoverItem;
                if (mOrientation == HORIZONTAL) {
                    hoverItem = findItemByX(x - getPaddingLeft() - mBackgroundWidth / 2);
                } else {
                    hoverItem = findItemByX(y - getPaddingTop() - mBackgroundWidth / 2);
                }
                focusItem(hoverItem);
                if (mOrientation == VERTICAL) {
                    PointF popupAt = new PointF();
                    popupAt.x = 0;
                    float popupY = y;
                    if (popupY < 0.0f) {
                        popupY = 0.0f;
                    }
                    if (popupY > this.getMeasuredHeight()) {
                        popupY = this.getMeasuredHeight();
                    }
                    popupAt.y = popupY;
                    CharSequence popupText = mItems[hoverItem];
                    showPopup(popupAt, popupText);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float x = MotionEventCompat.getX(ev, activePointerIndex);
                final float y = MotionEventCompat.getY(ev, activePointerIndex);
                int hoverItem;
                if (mOrientation == HORIZONTAL) {
                    hoverItem = findItemByX(x - getPaddingLeft() - mBackgroundWidth / 2);
                } else {
                    hoverItem = findItemByX(y - getPaddingTop() - mBackgroundWidth / 2);
                }
                focusItem(hoverItem);
                if (mOrientation == VERTICAL) {
                    PointF popupAt = new PointF();
                    popupAt.x = 0;
                    float popupY = y;
                    if (popupY < 0.0f) {
                        popupY = 0.0f;
                    }
                    if (popupY > this.getMeasuredHeight()) {
                        popupY = this.getMeasuredHeight();
                    }
                    popupAt.y = popupY;
                    CharSequence popupText = mItems[hoverItem];
                    showPopup(popupAt, popupText);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                focusItem(-1);
                mActivePointerId = INVALID_POINTER;
                hideBackground();
                hidePopup();
                break;
            }
            case MotionEvent.ACTION_UP: {
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float x = MotionEventCompat.getX(ev, activePointerIndex);
                final float y = MotionEventCompat.getY(ev, activePointerIndex);
                int hoverItem;
                if (mOrientation == HORIZONTAL) {
                    hoverItem = findItemByX(x - getPaddingLeft() - mBackgroundWidth / 2);
                } else {
                    hoverItem = findItemByX(y - getPaddingTop() - mBackgroundWidth / 2);
                }
                changeSelected(hoverItem);
                focusItem(-1);
                mActivePointerId = INVALID_POINTER;
                hideBackground();
                hidePopup();
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                break;
        }

        return true;
    }

    private int findItemByX(float x) {
        if (mItems.length == 0) {
            return -1;
        }
        if (x < mInnerPadding * 0.5f) {
            return 0;
        }
        for (int i = 1; i < mItems.length; i++) {
            if (x >= mInnerPadding * (i - 0.5f) && x <= mInnerPadding * (i + 0.5f)) {
                return i;
            }
        }
        return mItems.length - 1;
    }

    public void setSelectedItem(int item) {
        mSelectedItem = item;
        updateItemRadiuses();
        invalidate();
    }

    protected void changeSelected(int newSelected) {
        if (mSelectedItem != newSelected) {
            mSelectedItem = newSelected;
            updateItemRadiuses();
            invalidate();
            for (Listener listener : mListeners)
                listener.onSelectItem(newSelected);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mOrientation == HORIZONTAL) {
            setMeasuredDimension(
                    measureLong(widthMeasureSpec, getPaddingLeft() + getPaddingRight()),
                    measureShort(heightMeasureSpec, getPaddingTop() + getPaddingBottom()));
        } else {
            setMeasuredDimension(
                    measureShort(widthMeasureSpec, getPaddingLeft() + getPaddingRight()),
                    measureLong(heightMeasureSpec, getPaddingTop() + getPaddingBottom()));
        }
    }

    private int measureLong(int measureSpec, float paddings) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            final int count = mItems.length;
            if (count == 0) {
                result = (int) (mBackgroundWidth + paddings + 1);
            } else {
                result = (int) (mBackgroundWidth + paddings + mInnerPadding * (count - 1) + 1);
            }
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureShort(int measureSpec, float paddings) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = (int) (mBackgroundWidth + paddings + 1);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
}
