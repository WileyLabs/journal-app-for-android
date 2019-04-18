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

public class ScreenRegisterErrorFragment extends AbstractScreenFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_register_error, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findView(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccessDialogFragment().goBack();
            }
        });
        findView(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNeedWolLogIn();
            }
        });
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    protected String getGANHelperEvent() {
        return null;
    }

    @Override
    protected void openPreviousScreen() {
        final Bundle args = getArguments();
        getAccessDialogFragment().openRegisterScreen(args);
    }

    @Override
    public void onStart() {
        super.onStart();
        getAccessDialogFragment().setDialogHeader(getActivity().getResources().getString(R.string.registration_error_label));
    }

    @Override
    public void onStop() {
        super.onStop();
        getAccessDialogFragment().setDialogHeader(getActivity().getResources().getString(R.string.get_access_label));
    }
}
