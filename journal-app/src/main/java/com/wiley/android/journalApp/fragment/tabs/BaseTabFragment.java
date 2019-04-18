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
package com.wiley.android.journalApp.fragment.tabs;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.JournalMainFragment;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.error.ConnectionException;
import com.wiley.wol.client.android.log.Logger;

public abstract class BaseTabFragment extends JournalFragment {

    private final String TAG = this.getClass().getSimpleName() + ".LIFE";

    private boolean mShowing = false;

    public boolean isShowing() {
        return mShowing;
    }

    public void dispatchOnShow() {
        if (!mShowing) {
            onShow();
        } else {
            onShowBack();
        }
    }

    public void dispatchOnHide() {
        if (mShowing) {
            onHide();
        }
    }

    public void onShow() {
        Logger.d(TAG, "onShow");
        mShowing = true;
    }

    public void onShowTab() {
        Logger.d(TAG, "onTabShow()");
    }

    public void onShowBack() {
        Logger.d(TAG, "onShowBack");
    }

    public void onHide() {
        Logger.d(TAG, "onHide");
        mShowing = false;
    }

    public void onHideTab() {
        Logger.d(TAG, "onTabHide()");
    }

    public void onSoftKeyboardVisibleChanged(boolean visible) {

    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        Logger.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        Logger.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Logger.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Logger.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Logger.d(TAG, "onDetach");
        super.onDetach();
    }

    @Override
    public void onResume() {
        Logger.d(TAG, "onResume");
        super.onResume();
        if (!mShowing && getTabId() == getActiveTabId())
            onShow();
    }

    @Override
    public void onPause() {
        Logger.d(TAG, "onPause");
        super.onPause();
        if (mShowing)
            onHide();
    }

    protected int getActiveTabId() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment != null && parentFragment.getClass() == JournalMainFragment.class) {
            return ((JournalMainFragment) parentFragment).getActiveTabId();
        } else {
            return getTabId();
        }
    }

    protected void showErrorMessageLayout(ErrorManager errorManager, Exception exception) {
        final ErrorMessage errorMessage;
        errorMessage = exception instanceof ConnectionException ?
                errorManager.getErrorMessageForErrorCode(getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE_FOR_FIRST_LAUNCH) :
                errorManager.getErrorMessageForErrorCode(getActivity(), AppErrorCode.SERVER_ERROR);

        final TextView titleView = findView(R.id.error_title);
        if (errorMessage.getMessage() == null) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(errorMessage.getTitle());
        }
        ((TextView) findView(R.id.error_text)).setText(errorMessage.getMessage() == null ?
                errorMessage.getTitle() : errorMessage.getMessage());

        findView(R.id.error_message_layout).setVisibility(View.VISIBLE);
    }

    public boolean onBackPressed() {
        return false;
    }

    protected abstract int getTabId();
}