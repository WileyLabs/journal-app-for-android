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

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.WebBrowserActivity;
import com.wiley.wol.client.android.data.utils.GANHelper;

public class ScreenSubscriptionFragment extends AbstractScreenFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_subscription, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findView(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GANHelper.trackEventWithoutOrientation(getGANHelperEvent(), GANHelper.ACTION_LINK, GANHelper.LABEL_WOL_LOGIN, -1L);
                onNeedWolLogIn();
            }
        });

        final TextView forgottenPassword = findView(R.id.forgotten_password);
        final SpannableString spannableString = new SpannableString(forgottenPassword.getText());
        forgottenPassword.setText(spannableString);

        forgottenPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = WebBrowserActivity.getStartingIntent(getActivity(), theme.getForgottenPasswordUrl());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
            }
        });

        findView(R.id.create_an_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccessDialogFragment().openRegisterScreen();
            }
        });

        {
            aanHelper.trackGetAccessDialogueSponsoredSubscriptionInfo();
        }
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_SUBSCRIPTION;
    }

    @Override
    protected void openPreviousScreen() {
        getAccessDialogFragment().backToScreenA();
    }
}
