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
package com.wiley.android.journalApp.fragment.access;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wiley.android.journalApp.R;

public class ScreenRegisterConfirmFragment extends AbstractScreenFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_register_confirm, container, false);
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    protected String getGANHelperEvent() {
        return null;
    }

    @Override
    protected void openPreviousScreen() {
    }

    @Override
    public void onStart() {
        super.onStart();
        getAccessDialogFragment().setDialogHeader(getActivity().getResources().getString(R.string.registration_confirmation));
    }

    @Override
    public void onStop() {
        super.onStop();
        getAccessDialogFragment().setDialogHeader(getActivity().getResources().getString(R.string.get_access_label));
    }
}
