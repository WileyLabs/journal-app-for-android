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
package com.wiley.android.journalApp.base;

import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.Dimmable;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;

public abstract class ActivityWithActionBar extends JournalActivity implements Dimmable {

    @Inject
    protected Theme theme;

    private TextView actionBarTitleView = null;
    private FrameLayout actionBarCustomView = null;
    private FrameLayout actionBarContainer = null;

    private boolean useCenteredActionBarTitleView = true;

    public boolean getUseCenteredActionBarTitleView() {
        return useCenteredActionBarTitleView;
    }

    public void setUseCenteredActionBarTitleView(boolean use) {
        if (this.useCenteredActionBarTitleView != use) {
            this.useCenteredActionBarTitleView = use;
            setupActionBarTitleView();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Logger.d(getClass().getSimpleName(), "onCreate()");
        super.onCreate(savedInstanceState);
        setupActionBar();
        initContentView(savedInstanceState);
        undim();
    }

    protected void setupActionBar() {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setupActionBar(getActionBar());
    }

    protected void setupActionBar(final ActionBar actionBar) {
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        ActionBarUtils.setActionBarBackButton(actionBar, theme);

        this.actionBarContainer = ActionBarUtils.findActionBarHomeContainer(this);
        this.actionBarCustomView = new FrameLayout(this);
        actionBar.setCustomView(actionBarCustomView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT));
        actionBarCustomView.setClickable(true);
        actionBarCustomView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onActionBarTitleClick();
            }
        });

        actionBar.setBackgroundDrawable(new ColorDrawable(theme.getMainColor()));

        View contextView = ActionBarUtils.findActionContextBar(this);
        int contextColor = ColorUtils.brighterColorByPercent(theme.getMainColor(), 66.6f);
        contextView.setBackgroundDrawable(new ColorDrawable(contextColor));

        setupActionBarTitleView();
    }

    protected void setupActionBarTitleView() {
        if (this.actionBarCustomView == null || this.actionBarContainer == null)
            return;

        final FrameLayout titleContainer = useCenteredActionBarTitleView ? this.actionBarContainer : this.actionBarCustomView;

        if (this.actionBarTitleView == null) {
            this.actionBarTitleView = (TextView) getLayoutInflater().inflate(R.layout.actionbar_title, actionBarContainer, false);
            this.actionBarTitleView.setText(this.getTitle());
        } else {
            this.actionBarContainer.removeView(this.actionBarTitleView);
            this.actionBarCustomView.removeView(this.actionBarTitleView);
        }

        titleContainer.addView(actionBarTitleView,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onActionModeStarted(final ActionMode mode) {
        super.onActionModeStarted(mode);
        if (actionBarTitleView != null)
            actionBarTitleView.setVisibility(View.GONE);
    }

    @Override
    public void onActionModeFinished(final ActionMode mode) {
        super.onActionModeFinished(mode);
        if (actionBarTitleView != null)
            actionBarTitleView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onTitleChanged(CharSequence title, final int color) {
        super.onTitleChanged(title, color);
        if (actionBarTitleView != null) {
            actionBarTitleView.setText(title);
            title = "";
        }
        getActionBar().setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onHome();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isActionBarShowed() {
        final ActionBar bar = getActionBar();
        return bar != null && bar.isShowing();
    }

    public void showActionBar() {
        final ActionBar bar = getActionBar();
        if (bar != null && !bar.isShowing()) {
            onActionBarWillShow();
            bar.show();
        }
    }

    public void hideActionBar() {
        final ActionBar bar = getActionBar();
        if (bar != null && bar.isShowing()) {
            onActionBarWillHide();
            bar.hide();
        }
    }

    public void toggleActionBar() {
        final ActionBar bar = getActionBar();
        if (bar.isShowing()) {
            onActionBarWillHide();
            bar.hide();
        } else {
            onActionBarWillShow();
            bar.show();
        }
    }

    protected void onActionBarWillHide() {}

    protected void onActionBarWillShow() {}

    protected void onActionBarTitleClick() {}

    /**
     * Called on the action bar home button click.
     * Finishes the activity by default.
     */
    protected void onHome() {
        finish();
    }

    protected abstract void initContentView(Bundle savedInstanceState);

    @Override
    public void dim(final int alphaLevel) {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(alphaLevel);
        }
    }

    @Override
    public void undim() {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(0);
        }
    }

    protected FrameLayout getDimmableView() {
        return null;
    }
}