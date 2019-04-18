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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Toast;

import com.wiley.android.journalApp.R;

import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * @author Sergey Rybakov
 */
public final class UIUtils {

    private static Toast toast;

    private static void makeToast(final Context context, final String text, final int duration) {
        toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public static void showShortToast(final Context context, final String text) {
        hideToast();
        makeToast(context, text, LENGTH_SHORT);
    }

    public static void showLongToast(final Context context, final String text) {
        hideToast();
        makeToast(context, text, Toast.LENGTH_LONG);
    }

    public static void hideToast() {
        if (toast != null) {
            toast.cancel();
        }
    }

    public static void showNotImplemented(final Context ctx) {
        makeToast(ctx, "Not implemented yet", LENGTH_SHORT);
    }

    public static void showCancelRemove(final Context context, String title, String text, final Runnable onYesListener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(text)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (onYesListener != null)
                            onYesListener.run();
                    }
                })
                .show();
    }

    public static void showSoftInput(final View focusView) {
        final Context context = focusView.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(focusView, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideSoftInput(final View focusView) {
        if (focusView != null) {
            final Context context = focusView.getContext();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void hideSoftInput(final Activity activity) {
        View focusView = activity.getCurrentFocus();
        if (focusView != null)
            hideSoftInput(focusView);
    }

    public static <T extends View> List<T> getAllChildren(View root, Class<? extends T> viewClass) {
        List<T> result = new ArrayList<T>();
        doGetAllChildren(result, root, viewClass);
        return result;
    }

    protected static <T extends View> void doGetAllChildren(List<T> result, View current, Class<? extends T> viewClass) {
        if (viewClass.isAssignableFrom(current.getClass()))
            result.add((T) current);
        if (current instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) current;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                doGetAllChildren(result, child, viewClass);
            }
        }
    }

    public static Point convertBetweenViews(Point point, View from, View to) {
        int[] fromInWindow = new int[2];
        from.getLocationInWindow(fromInWindow);
        int[] toInWindow = new int[2];
        to.getLocationInWindow(toInWindow);

        Point pointInWindow = new Point(fromInWindow[0] + point.x, fromInWindow[1] + point.y);
        Point result = new Point(pointInWindow.x - toInWindow[0], pointInWindow.y - toInWindow[1]);

        return result;
    }

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(Context context, int px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    public static Point dpToPxPoint(Context context, Point dpPoint) {
        return new Point(dpToPx(context, dpPoint.x), dpToPx(context, dpPoint.y));
    }

    public static Point pxToDpPoint(Context context, Point pxPoint) {
        return new Point(pxToDp(context, pxPoint.x), pxToDp(context, pxPoint.y));
    }

    public static Rect dpToPxRect(Context context, Rect dpRect) {
        Rect newRect = new Rect();
        newRect.left = dpToPx(context, dpRect.left);
        newRect.top = dpToPx(context, dpRect.top);
        newRect.right = dpToPx(context, dpRect.right);
        newRect.bottom = dpToPx(context, dpRect.bottom);
        return newRect;
    }

    public static Rect pxToDpRect(Context context, Rect pxRect) {
        Rect newRect = new Rect();
        newRect.left = pxToDp(context, pxRect.left);
        newRect.top = pxToDp(context, pxRect.top);
        newRect.right = pxToDp(context, pxRect.right);
        newRect.bottom = pxToDp(context, pxRect.bottom);
        return newRect;
    }

    public static Point getDisplaySizePx(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getDisplaySizeDp(Context context) {
        Point sizePx = getDisplaySizePx(context);
        Point sizeDp = pxToDpPoint(context, sizePx);
        return sizeDp;
    }

    public static Point getRawDisplaySizeDp(Context context) {
        return pxToDpPoint(context, getRawDisplaySize(context));
    }

    public static Point getRawDisplaySize(Context context) {
        WindowManager w = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;

        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        }

        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                widthPixels = realSize.x;
                heightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        }

        return new Point(widthPixels, heightPixels);
    }

    public static void refreshWebView(WebView webView, Context context) {
        if (DeviceUtils.isPhone(context)) {
            int scrollY = webView.getScrollY();
            webView.scrollTo(webView.getScrollX(), scrollY + 1);
            webView.scrollTo(webView.getScrollX(), scrollY);
        }
    }
}
