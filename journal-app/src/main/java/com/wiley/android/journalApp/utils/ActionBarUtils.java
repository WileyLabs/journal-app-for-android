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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.wol.client.android.journalApp.theme.Theme;

public class ActionBarUtils {

    public static FrameLayout findActionBarHomeContainer(Activity activity) {
        int id = getActionBarHomeContainerId();
        return (FrameLayout) activity.findViewById(id);
    }

    public static int getActionBarHomeContainerId() {
        Resources resources = Resources.getSystem();
        return resources.getIdentifier("abs__screen_action_bar", "id",
                "com.wiley.android.journalApp");
    }

    public static int getActionBarHomeContainerId(Context context) {
        return context.getResources().getIdentifier("abs__screen_action_bar", "id", context.getPackageName());
    }

    public static ViewGroup findActionContextBar(Activity activity) {
        int id = getActionContextBarId();
        return (ViewGroup) activity.findViewById(id);
    }

    public static int getActionContextBarId() {
        Resources resources = Resources.getSystem();
        return resources.getIdentifier("action_context_bar", "id",
                "android");
    }

    public static void setActionBarBackButton(ActionBar actionBar, Theme theme) {
        final int icon_back;
        if (theme.isJournalHasDarkBackground()) {
            icon_back = R.drawable.icon_back;
        } else {
            icon_back = R.drawable.icon_back_dark;
        }

        actionBar.setIcon(icon_back);
    }

    public static int getMenuIconResource(Theme theme) {
        final int icon_menu;
        if (theme.isJournalHasDarkBackground()) {
            icon_menu = R.drawable.menu_button;
        } else {
            icon_menu = R.drawable.menu_button_dark;
        }

        return icon_menu;
    }

    public static int getLargeEnvelopIconResource(Theme theme) {
        final int icon_menu;
        if (theme.isJournalHasDarkBackground()) {
            icon_menu = R.drawable.icon_email_large;
        } else {
            icon_menu = R.drawable.icon_email_large_dark;
        }

        return icon_menu;
    }

    public static ActionBarSherlockCompat initActionBar(Activity activity, String title, JournalFragment fragment, Theme theme) {
        ActionBarSherlockCompat sherlock = new ActionBarSherlockCompat(activity, 0,
                (ViewGroup) fragment.findView(R.id.abs__screen_action_bar));
        final int icon_back;
        if (theme.isJournalHasDarkBackground()) {
            icon_back = R.drawable.icon_back;
        } else {
            icon_back = R.drawable.icon_back_dark;
        }

        sherlock.setupActionBar()
                .setIcon(icon_back)
                .setBackgroundDrawable(theme.getMainColor())
                .setTitleActionBar(title);
        sherlock.setListener(fragment);

        return sherlock;
    }

    public static ActionBarSherlockCompat initActionBarWithCloseIcon(Activity activity, String title, JournalFragment fragment, Theme theme) {
        ActionBarSherlockCompat sherlock = new ActionBarSherlockCompat(activity, 0,
                (ViewGroup) fragment.findView(R.id.abs__screen_action_bar));
        final int icon_back;
        if (theme.isJournalHasDarkBackground()) {
            icon_back = R.drawable.close_white_button_without_rect_selector;
        } else {
            icon_back = R.drawable.close_white_button_without_rect_selector_dark;
        }

        sherlock.setupActionBar()
                .setIcon(icon_back)
                .setBackgroundDrawable(theme.getMainColor())
                .setTitleActionBar(title);
        sherlock.setListener(fragment);

        return sherlock;
    }


    public static void inflateArticleViewMenu(ActionBarSherlockCompat actionBar, Menu menu, Theme theme) {
        if (theme.isJournalHasDarkBackground()) {
            actionBar.getMenuInflater().inflate(R.menu.article_view_menu, menu);
        } else {
            actionBar.getMenuInflater().inflate(R.menu.article_view_menu_dark, menu);
        }
    }

    public static void inflateIssueTocMenu(ActionBarSherlockCompat actionBar, Menu menu, Theme theme) {
        if (theme.isJournalHasDarkBackground()) {
            actionBar.getMenuInflater().inflate(R.menu.issue_toc_menu, menu);
        } else {
            actionBar.getMenuInflater().inflate(R.menu.issue_toc_menu_dark, menu);
        }
    }

    public static void inflateFeedItemMenu(ActionBarSherlockCompat actionBar, Menu menu, Theme theme) {
        actionBar.getMenuInflater().inflate(R.menu.feed_item_menu, menu);
    }
}
