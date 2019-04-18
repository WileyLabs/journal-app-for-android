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

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.MainMenu;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.notification.NotificationCenter;

import java.util.Collection;

public class MainMenuWindow extends PopupWindows implements OnDismissListener {
    private View rootView;
    private ImageView arrow;
    private LayoutInflater inflater;
    private View scroller;
    private OnDismissListener dismissListener;
    private boolean mDidAction;
    private int rootWidth = 0;
    private MainMenu mainMenu;
    private NotificationCenter notificationCenter;

    public MainMenuWindow(Context context, NotificationCenter notificationCenter) {
        super(context);
        this.notificationCenter = notificationCenter;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setRootViewId(R.layout.main_menu);
    }

    private void setRootViewId(int id) {
        rootView = inflater.inflate(id, null);

        arrow = (ImageView) rootView.findViewById(R.id.arrow_up);

        scroller = rootView.findViewById(R.id.scroller);
        rootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mainMenu = new MainMenu(context, rootView, this, notificationCenter);
        setContentView(rootView);
    }

    /**
     * Show quickaction popup. Popup is automatically positioned, on top or bottom of anchor view.
     */
    public void show(View anchor) {
        preShow();

        int xPos, yPos, arrowPos;

        mDidAction = false;

        int[] location = new int[2];

        anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1]
                + anchor.getHeight());

        //rootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        rootView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootHeight = rootView.getMeasuredHeight();

        if (rootWidth == 0) {
            rootWidth = rootView.getMeasuredWidth();
        }

        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();

        //automatically get X coord of popup (top left)
        if ((anchorRect.left + rootWidth) > screenWidth) {
            xPos = anchorRect.left - (rootWidth - anchor.getWidth() - 10);
            xPos = (xPos < 0) ? 0 : xPos;

            arrowPos = anchorRect.centerX() - xPos;

        } else {
            if (anchor.getWidth() > rootWidth) {
                xPos = anchorRect.centerX() - (rootWidth / 2);
            } else {
                xPos = anchorRect.left;
            }

            arrowPos = anchorRect.centerX() - xPos;
        }

        int dyTop = anchorRect.top;
        int dyBottom = screenHeight - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom);

        if (onTop) {
            if (rootHeight > dyTop) {
                yPos = 15;
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;
        }

        showArrow(arrowPos);
        window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

        mainMenu.expandHighlightedGroup();
    }

    private void showArrow(int requestedX) {
        final int arrowWidth = arrow.getMeasuredWidth();
        arrow.setVisibility(View.VISIBLE);
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) arrow.getLayoutParams();
        param.leftMargin = requestedX - arrowWidth / 2;
    }

    /**
     * Set listener for window dismissed. This listener will only be fired if the quicakction dialog is dismissed
     * by clicking outside the dialog or clicking on sticky item.
     */
    public void setOnDismissListener(MainMenuWindow.OnDismissListener listener) {
        setOnDismissListener(this);

        dismissListener = listener;
    }

    @Override
    public void onDismiss() {
        if (!mDidAction && dismissListener != null) {
            dismissListener.onDismiss();
        }
    }

    public void addEarlyViewItem() {
        mainMenu.addEarlyViewItem();
    }

    public void removeEarlyViewItem() {
        mainMenu.removeEarlyViewItem();
    }

    public void addSpecialSectionsItem() {
        mainMenu.addSpecialSectionsItem();
    }

    public void removeSpecialSectionsItem() {
        mainMenu.removeSpecialSectionsItem();
    }

    public void addSocietyFeeds(Collection<FeedMO> feeds) {
        mainMenu.addSocietyFeeds(feeds);
    }

    public void setJournalSavedArticlesTitle(String title) {
        mainMenu.setJournalSavedArticlesTitle(title);
    }

    public void setSocietyFavoritesItemTitle(String title) {
        mainMenu.setSocietyFavoritesItemTitle(title);
    }

    public void setSocietyTitle(final String title) {
        mainMenu.setSocietyTitle(title);
    }

    /**
     * Listener for item click
     */
    public interface OnActionItemClickListener {
        public abstract void onItemClick(MainMenuWindow source, int pos, int actionId);
    }

    /**
     * Listener for window dismiss
     */
    public interface OnDismissListener {
        public abstract void onDismiss();
    }
}