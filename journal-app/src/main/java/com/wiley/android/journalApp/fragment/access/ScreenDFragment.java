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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.journalApp.theme.Theme;

public class ScreenDFragment extends BaseCDScreenFragment {

    @Inject
    private Theme mTheme;
    @Inject
    protected AANHelper aanHelper;
    private static final Templates sTemplates = new Templates();

    @Override
    public void onViewCreated(View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        aanHelper.trackGetAccessDialogueInstitutionInfo();

        final Button logInButton = findView(R.id.get_access_log_in_to_wol);
        final Button registerButton = findView(R.id.register_in_wol);
        final View getAccessButtonsSpacer = findView(R.id.get_access_buttons_spacer);

        registerButton.setVisibility(View.VISIBLE);
        getAccessButtonsSpacer.setVisibility(View.VISIBLE);

        final LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) logInButton.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.weight = 1f;
        logInButton.setLayoutParams(layoutParams);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aanHelper.trackRegistrationForm();
                final Bundle args = new Bundle(1);
                args.putBoolean(ScreenRegisterFragment.MOBILE_AFFILIATION, true);
                getAccessDialogFragment().openRegisterScreen(args);
            }
        });
    }
    @Override
    protected String getMessageHtml() {
        Template template = sTemplates.useAssetsTemplate(getActivity(), "login_descr_items");
        return template
                .putParam("login_steps_placeholder", mTheme.getLoginSteps())
                .proceed();
    }

    @Override
    protected boolean needMoreInfoBlock() {
        return true;
    }

    @Override
    protected boolean needDescriptionMessage() {
        return true;
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_D;
    }
}
