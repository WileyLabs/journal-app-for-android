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
package com.wiley.android.journalApp.components.popup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.fragment.popups.PopupAuthorsInfo;
import com.wiley.android.journalApp.fragment.popups.PopupCitation;
import com.wiley.android.journalApp.fragment.popups.PopupFontPicker;
import com.wiley.android.journalApp.fragment.popups.PopupFragment;
import com.wiley.android.journalApp.fragment.popups.PopupSavedArticlesEditor;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.log.Logger;

/**
 * Created by taraskreknin on 05.06.14.
 */
public class PopupHost extends ViewGroup {
    private final static String TAG = PopupHost.class.getSimpleName();
    private static final String TAG_POPUP = "PopupHost_fragment";

    public static interface PopupListener {
        void onPopupShow();

        void onPopupDismiss();
    }

    public enum Orientation {
        Vertical,
        Horizontal
    }

    private static final String LOG_TAG = PopupHost.class.getSimpleName();
    private View mPopup;
    private ImageView mArrowVerticalView;
    private ImageView mArrowHorizontalView;
    private Point mFixedShowPoint;
    private Rect mTmpRect = new Rect();
    private Rect mTmpLayoutRect = new Rect();
    private int[] mTmpLocation = new int[2];
    private PopupListener mListener;
    private AnimationHandler mAnimator = new AnimationHandler(300, 0);
    private int mPrevOrientation;
    private View mAnchorView;

    private boolean mDimOnShow = false;
    private int mArrowVerticalResId = R.drawable.popup_top_arrow;
    private int mArrowHorizontalResId = R.drawable.popup_left_arrow;
    private int mPopupContentHolderId;
    private Fragment currentFragment;

    private Orientation orientation = Orientation.Vertical;

    public PopupHost(Context context) {
        this(context, null);
    }

    public PopupHost(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PopupHost(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mPrevOrientation = DeviceUtils.isLandscape(getContext()) ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;

        setVisibility(View.GONE);
        setAlpha(0.0f);

        mArrowVerticalView = new ImageView(getContext());
        mArrowVerticalView.setImageResource(mArrowVerticalResId);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(mArrowVerticalView, 0, lp);

        mArrowHorizontalView = new ImageView(getContext());
        mArrowHorizontalView.setImageResource(mArrowHorizontalResId);
        mArrowHorizontalView.setVisibility(View.GONE);
        LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(mArrowHorizontalView, 0, lp2);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation newOrientation) {
        this.orientation = newOrientation;
        if (newOrientation == Orientation.Horizontal) {
            mArrowHorizontalView.setVisibility(View.VISIBLE);
            mArrowVerticalView.setVisibility(View.GONE);
        } else {
            mArrowHorizontalView.setVisibility(View.GONE);
            mArrowVerticalView.setVisibility(View.VISIBLE);
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mPopup == null) {
            if (getChildCount() == 3) {
                mPopup = getChildAt(2);
            } else {
                throw new IllegalStateException("Popup host supports only 1 child!");
            }
        }

        if (orientation == Orientation.Vertical) {
            onMeasureVertical(width, height);
        } else {
            onMeasureHorizontal(width, height);
        }

        setMeasuredDimension(width, height);
    }

    private void onMeasureVertical(int width, int height) {
        int arrowHeight = 0;
        if (mArrowVerticalView != null) {
            mArrowVerticalView.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.AT_MOST, width), MeasureSpec.makeMeasureSpec(MeasureSpec.AT_MOST, height));
            arrowHeight = mArrowVerticalView.getMeasuredHeight();
        }

        final LayoutParams lp = mPopup.getLayoutParams();

        int childWidthSpec;
        if (lp.width == LayoutParams.WRAP_CONTENT) {
            childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
        } else if (lp.width == LayoutParams.MATCH_PARENT) {
            childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
            childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        }

        int childHeightSpec;
        if (lp.height == LayoutParams.WRAP_CONTENT) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(height - arrowHeight, MeasureSpec.AT_MOST);
        } else if (lp.height == LayoutParams.MATCH_PARENT) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(height - arrowHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }

        mPopup.measure(childWidthSpec, childHeightSpec);
    }

    private void onMeasureHorizontal(int width, int height) {
        int arrowWidth = 0;
        if (mArrowHorizontalView != null) {
            mArrowHorizontalView.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.AT_MOST, width), MeasureSpec.makeMeasureSpec(MeasureSpec.AT_MOST, height));
            arrowWidth = mArrowHorizontalView.getMeasuredWidth();
        }

        final LayoutParams lp = mPopup.getLayoutParams();

        int childWidthSpec;
        if (lp.width == LayoutParams.WRAP_CONTENT) {
            childWidthSpec = MeasureSpec.makeMeasureSpec(width - arrowWidth, MeasureSpec.AT_MOST);
        } else if (lp.width == LayoutParams.MATCH_PARENT) {
            childWidthSpec = MeasureSpec.makeMeasureSpec(width - arrowWidth, MeasureSpec.EXACTLY);
        } else {
            childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        }

        int childHeightSpec;
        if (lp.height == LayoutParams.WRAP_CONTENT) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        } else if (lp.height == LayoutParams.MATCH_PARENT) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }

        mPopup.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mPopup == null) {
            return;
        }

        mTmpLayoutRect.left = l;
        mTmpLayoutRect.top = t;
        mTmpLayoutRect.right = r;
        mTmpLayoutRect.bottom = b;

        Point localShowPoint = getLocalShowPoint(getShowPoint(), mTmpLayoutRect);

        if (orientation == Orientation.Vertical) {
            onLayoutVertical(l, t, r, b, localShowPoint);
        } else {
            onLayoutHorizontal(l, t, r, b, localShowPoint);
        }
    }

    private void onLayoutVertical(int l, int t, int r, int b, Point localShowPoint) {
        int arrowWidth = mArrowVerticalView.getMeasuredWidth();
        int arrowHeight = mArrowVerticalView.getMeasuredHeight();
        int popupWidth = mPopup.getMeasuredWidth();
        int popupHeight = mPopup.getMeasuredHeight();

        int popl = localShowPoint.x - popupWidth / 2;
        int popr = localShowPoint.x + popupWidth / 2;
        int popt = localShowPoint.y + arrowHeight;
        int popb = popt + popupHeight;

        boolean invertVertical = false;

        if (popb > b) {
            int invertedPopB = localShowPoint.y - arrowHeight;
            int invertedPopT = invertedPopB - popupHeight;

            if (invertedPopT >= t) {
                invertVertical = true;
            } else {
                if ((t - invertedPopT) < (popb - b)) {
                    invertVertical = true;
                    invertedPopT = t;
                } else {
                    invertVertical = false;
                    popb = b;
                }
            }

            if (invertVertical) {
                popb = invertedPopB;
                popt = invertedPopT;
            }
        }

        Rect popRect = getRectFitToSize(popl, popt, popr, popb, mTmpLayoutRect);

        mPopup.layout(popRect.left, popRect.top, popRect.right, popRect.bottom);

        int arrl = localShowPoint.x - arrowWidth / 2;
        int arrr = localShowPoint.x + arrowWidth / 2;
        int arrt = localShowPoint.y;
        int arrb = arrt + arrowHeight;
        if (invertVertical) {
            arrb = localShowPoint.y;
            arrt = arrb - arrowHeight;
        }

        Rect arrRect = getRectFitToSize(arrl, arrt, arrr, arrb, mTmpLayoutRect);

        mArrowVerticalView.layout(arrRect.left, arrRect.top, arrRect.right, arrRect.bottom);

        if (popRect.height() == 0 || popRect.width() == 0) {
            setVisibility(View.GONE);
        }

        if (invertVertical) {
            mArrowVerticalView.setScaleY(-1.0f);
        } else {
            mArrowVerticalView.setScaleY(1.0f);
        }
    }

    private void onLayoutHorizontal(int l, int t, int r, int b, Point localShowPoint) {
        int arrowWidth = mArrowHorizontalView.getMeasuredWidth();
        int arrowHeight = mArrowHorizontalView.getMeasuredHeight();
        int popupWidth = mPopup.getMeasuredWidth();
        int popupHeight = mPopup.getMeasuredHeight();

        int popl = localShowPoint.x + arrowWidth;
        int popr = popl + popupWidth;
        int popt = localShowPoint.y - popupHeight / 2;
        int popb = localShowPoint.y + popupHeight / 2;

        boolean invertHorizontal = false;

        if (popr > r) {
            int invertedPopR = localShowPoint.x - arrowWidth;
            int invertedPopL = invertedPopR - popupWidth;

            if (invertedPopL >= l) {
                invertHorizontal = true;
            } else {
                if ((l - invertedPopL) < (popr - r)) {
                    invertHorizontal = true;
                    invertedPopL = l;
                } else {
                    invertHorizontal = false;
                    popr = r;
                }
            }

            if (invertHorizontal) {
                popr = invertedPopR;
                popl = invertedPopL;
            }
        }

        Rect popRect = getRectFitToSize(popl, popt, popr, popb, mTmpLayoutRect);

        mPopup.layout(popRect.left, popRect.top, popRect.right, popRect.bottom);

        int arrl = localShowPoint.x;
        int arrr = localShowPoint.x + arrowWidth;
        int arrt = localShowPoint.y - arrowHeight / 2;
        int arrb = localShowPoint.y + arrowHeight / 2;
        if (invertHorizontal) {
            arrr = localShowPoint.x;
            arrl = arrr - arrowWidth;
        }

        Rect arrRect = getRectFitToSize(arrl, arrt, arrr, arrb, mTmpLayoutRect);

        mArrowHorizontalView.layout(arrRect.left, arrRect.top, arrRect.right, arrRect.bottom);

        if (popRect.height() == 0 || popRect.width() == 0) {
            setVisibility(View.GONE);
        }

        if (invertHorizontal) {
            mArrowHorizontalView.setScaleX(-1.0f);
        } else {
            mArrowHorizontalView.setScaleX(1.0f);
        }
    }

    private Point getShowPoint() {
        if (mAnchorView != null) {
            mAnchorView.getLocationOnScreen(mTmpLocation);
            return new Point(mTmpLocation[0] + mAnchorView.getWidth() / 2, mTmpLocation[1] + mAnchorView.getHeight());
        }
        return mFixedShowPoint;
    }

    private Point getLocalShowPoint(Point globalPoint, Rect layoutRect) {
        if (globalPoint == null) {
            return new Point(layoutRect.centerX(), layoutRect.centerY());
        }

        int[] layoutScreenLoc = new int[2];
        getLocationInWindow(layoutScreenLoc);

        int localX = globalPoint.x - layoutScreenLoc[0];
        localX = localX > 0 ? localX : 0;
        int localY = globalPoint.y - layoutScreenLoc[1];
        localY = localY > 0 ? localY : 0;

        return new Point(localX, localY);
    }

    private Rect getRectFitToSize(int l, int t, int r, int b, Rect screen) {
        mTmpRect.left = l;
        mTmpRect.top = t;
        mTmpRect.right = r;
        mTmpRect.bottom = b;

        int offsetX = 0;
        int offsetY = 0;

        if (screen.width() < mTmpRect.width() || screen.height() < mTmpRect.height()) {
            Logger.w(LOG_TAG, "Bad! Popup is bigger than its parent! Fix it!");
        }

        if (mTmpRect.left < screen.left) {
            offsetX = screen.left - mTmpRect.left;
        }
        if (mTmpRect.top < screen.top) {
            offsetY = screen.top - mTmpRect.top;
        }
        if (mTmpRect.right > screen.right) {
            offsetX = screen.right - mTmpRect.right;
        }
        if (mTmpRect.bottom > screen.bottom) {
            offsetY = screen.bottom - mTmpRect.bottom;
        }
        mTmpRect.offset(offsetX, offsetY);
        return mTmpRect;
    }

    private void show(View anchor) {
        anchor.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                requestLayout();
            }
        });
        mAnchorView = anchor;
        mFixedShowPoint = null;
        internalShow();
    }

    private void show(Point at) {
        mFixedShowPoint = at;
        mAnchorView = null;
        internalShow();
    }

    public void toggleFontSizePicker(FragmentManager manager, View anchor) {
        toggleFragmentAtAnchor(PopupFontPicker.class, manager, anchor, Orientation.Vertical);
    }

    public void toggleCitation(FragmentManager manager, View anchor, DOI doi) {
        Bundle args = new Bundle();
        args.putParcelable(Extras.EXTRA_DOI, doi);
        toggleFragmentAtAnchor(PopupCitation.class, manager, anchor, args, Orientation.Vertical);
    }

    public void toggleSavedArticlesEditor(FragmentManager manager, View anchor) {
        toggleFragmentAtAnchor(PopupSavedArticlesEditor.class, manager, anchor, Orientation.Vertical);
    }

    public void showAuthorsInfo(FragmentManager manager, Point at) {
        showFragmentAtPoint(PopupAuthorsInfo.class, manager, at, Orientation.Vertical);
        mArrowVerticalView.setVisibility(View.GONE);
    }

    public <T extends Fragment> void toggleFragmentAtAnchor(Class<T> fragmentClass, FragmentManager manager, View anchor, Orientation orientation) {
        toggleFragmentAtAnchor(fragmentClass, manager, anchor, null, orientation);
    }

    public <T extends Fragment> void toggleFragmentAtAnchor(Class<T> fragmentClass, FragmentManager manager, View anchor, Bundle arguments, Orientation orientation) {
        toggleFragment(fragmentClass, manager, new AnchorShowHelper(anchor), arguments, orientation);
    }

    public <T extends Fragment> void toggleFragmentAtPoint(Class<T> fragmentClass, FragmentManager manager, Point at, Orientation orientation) {
        toggleFragmentAtPoint(fragmentClass, manager, at, null, orientation);
    }

    public <T extends Fragment> void toggleFragmentAtPoint(Class<T> fragmentClass, FragmentManager manager, Point at, Bundle arguments, Orientation orientation) {
        toggleFragment(fragmentClass, manager, new PointShowHelper(at), arguments, orientation);
    }

    private <T extends Fragment> void toggleFragment(Class<T> fragmentClass, FragmentManager manager, ShowHelper helper, Bundle arguments, Orientation orientation) {
        if (isAlreadyShowing(fragmentClass, manager)) {
            hide();
        } else {
            showFragment(fragmentClass, manager, helper, arguments, orientation);
        }
    }

    public <T extends Fragment> void showFragmentAtAnchor(Class<T> fragmentClass, FragmentManager manager, View anchor, Orientation orientation) {
        showFragmentAtAnchor(fragmentClass, manager, anchor, null, orientation);
    }

    public <T extends Fragment> void showFragmentAtAnchor(Class<T> fragmentClass, FragmentManager manager, View anchor, Bundle arguments, Orientation orientation) {
        showFragment(fragmentClass, manager, new AnchorShowHelper(anchor), arguments, orientation);
    }

    public <T extends Fragment> void showFragmentAtPoint(Class<T> fragmentClass, FragmentManager manager, Point at, Orientation orientation) {
        showFragmentAtPoint(fragmentClass, manager, at, null, orientation);
    }

    public <T extends Fragment> void showFragmentAtPoint(Class<T> fragmentClass, FragmentManager manager, Point at, Bundle arguments, Orientation orientation) {
        showFragment(fragmentClass, manager, new PointShowHelper(at), arguments, orientation);
    }

    private <T extends Fragment> void showFragment(Class<T> fragmentClass, FragmentManager manager, ShowHelper helper, Bundle arguments, Orientation orientation) {
        Fragment newFragment = getFragmentInstance(fragmentClass);
        if (newFragment != null) {
            if (arguments == null) {
                arguments = new Bundle();
            }
            arguments.putInt(PopupFragment.Param_HostId, this.getId());
            newFragment.setArguments(arguments);
        }
        FragmentTransaction fragmentTransaction = manager.beginTransaction();

        if (currentFragment != null) {
            fragmentTransaction.detach(currentFragment);
        }

        currentFragment = newFragment;
        fragmentTransaction.replace(mPopupContentHolderId, newFragment, TAG_POPUP).commit();
        manager.executePendingTransactions();
        setOrientation(orientation);
        helper.show();
    }

    private <T extends Fragment> Fragment getFragmentInstance(Class<T> clazz) {
        Fragment frag = null;
        try {
            frag = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Logger.s(TAG, e);
        }
        return frag;
    }

    private <T extends Fragment> boolean isAlreadyShowing(Class<T> aClass, FragmentManager manager) {
        if (!isShowing()) {
            return false;
        }
        Fragment frag = manager.findFragmentByTag(TAG_POPUP);
        return frag != null && frag.getClass().equals(aClass);
    }

    private void internalShow() {
        setVisibility(View.VISIBLE);
        requestLayout();
        if (mListener != null && isPopupVisible()) {
            mListener.onPopupShow();
        }
        if (isPopupVisible()) {
            mAnimator.fadeIn();
        }
    }

    public void hide() {
        mFixedShowPoint = null;
        mAnchorView = null;
        mAnimator.fadeOut(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onPopupDismiss();
                }
            }
        });
    }

    public void hide(final boolean animated) {
        if (animated) {
            hide();
        } else {
            setVisibility(View.GONE);
            mListener.onPopupDismiss();
        }
    }

    private boolean isPopupVisible() {
        return getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void setDimOnShow(boolean dim) {
        //TODO
        mDimOnShow = dim;
    }

    public void setPopupContentHolderResId(int resId) {
        mPopupContentHolderId = resId;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPrevOrientation != newConfig.orientation) {
            mPrevOrientation = newConfig.orientation;
            if (mFixedShowPoint != null) {
                hide();
            }
        }
    }

    public void setPopupListener(PopupListener l) {
        mListener = l;
    }

    public boolean isShowing() {
        return isPopupVisible();
    }

    private class AnimationHandler {
        private boolean mFadingOut = false;
        private int mDuration;
        private int mDelay;
        private Runnable mOnAnimationEndCallback;
        private ValueAnimator mShowAnimator = null;
        private Animator.AnimatorListener mShowAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                endAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                endAnimation();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };

        private void endAnimation() {
            mShowAnimator = null;
            if (mFadingOut) {
                setVisibility(View.GONE);
                if (mOnAnimationEndCallback != null) {
                    mOnAnimationEndCallback.run();
                }
            }
        }

        private ValueAnimator.AnimatorUpdateListener mShowAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                setAlpha(value);
            }
        };

        public AnimationHandler(int duration, int delay) {
            mDuration = duration;
            mDelay = delay;
        }

        public void fadeIn() {
            fadeIn(null);
        }

        public void fadeIn(Runnable callback) {
            if (mShowAnimator != null && !mFadingOut) {
                return;
            }

            mFadingOut = false;
            mOnAnimationEndCallback = callback;
            stopAnimation();
            mShowAnimator = ValueAnimator.ofFloat(getAlpha(), 1.0f);
            setupAndStart();
        }

        public void fadeOut(Runnable callback) {
            if (mShowAnimator != null && mFadingOut) {
                return;
            }

            mFadingOut = true;
            mOnAnimationEndCallback = callback;
            stopAnimation();
            mShowAnimator = ValueAnimator.ofFloat(getAlpha(), 0.0f);
            setupAndStart();
        }

        private void setupAndStart() {
            mShowAnimator.setDuration(mDuration);
            mShowAnimator.setStartDelay(mDelay);
            mShowAnimator.addListener(mShowAnimatorListener);
            mShowAnimator.addUpdateListener(mShowAnimatorUpdateListener);
            mShowAnimator.start();
        }

        public void stopAnimation() {
            if (mShowAnimator != null) {
                mShowAnimator.cancel();
                mShowAnimator = null;
            }
        }
    }

    private interface ShowHelper {
        void show();
    }

    private class AnchorShowHelper implements ShowHelper {

        private View mAnchor;

        public AnchorShowHelper(View anchor) {
            mAnchor = anchor;
        }

        @Override
        public void show() {
            PopupHost.this.show(mAnchor);
        }
    }

    private class PointShowHelper implements ShowHelper {

        private Point mPoint;

        public PointShowHelper(Point point) {
            mPoint = point;
        }

        @Override
        public void show() {
            PopupHost.this.show(mPoint);
        }
    }
}
