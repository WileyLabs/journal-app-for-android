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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.GetAccessDialogFragment;
import com.wiley.android.journalApp.activity.OauthAuthorizationActivity;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.service.InAppBillingService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

/**
 * Created by taraskreknin on 26.06.14.
 */
public abstract class AbstractScreenFragment extends JournalFragment {

    @Inject
    protected AANHelper aanHelper;
    @Inject
    protected WebController mWebController;
    @Inject
    protected Settings mSettings;
    @Inject
    protected Theme mTheme;
    @Inject
    protected AuthorizationService mAuthService;
    @Inject
    protected Authorizer mAuthorizer;
    @Inject
    protected InAppBillingService mInAppBillingService;
    @Inject
    protected ErrorManager mErrorManager;
    @Inject
    protected NotificationCenter mNotificationCenter;

    private final View.OnClickListener mOnMoreInfoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            {
                aanHelper.trackActionOpenWebViewerForOtherPage("http://olabout.wiley.com/go/apps");
            }
            GANHelper.trackEventWithoutOrientation(getGANHelperEvent(), GANHelper.ACTION_LINK, GANHelper.LABEL_HELP, -1L);
            mWebController.openUrlInternal("http://olabout.wiley.com/go/apps");
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRetainInstance(true);//IllegalStateException: Can't retain fragements that are nested in other fragments
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        linkifyMoreInfoBlock();
    }

    private void linkifyMoreInfoBlock() {
        final TextView moreInfoLink = findView(R.id.get_access_more_info_link);
        if (moreInfoLink != null) {
            moreInfoLink.setOnClickListener(mOnMoreInfoClickListener);
        }
    }

    public abstract boolean canGoBack();

    protected abstract String getGANHelperEvent();

    protected GetAccessDialogFragment getAccessDialogFragment() {
        return (GetAccessDialogFragment) getParentFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((GetAccessDialogFragment) getParentFragment()).setBackButtonEnabled(canGoBack());
    }

    protected void onNeedWolLogIn() {
        final Intent startNewActivityIntent = new Intent(getActivity().getApplicationContext(),
                OauthAuthorizationActivity.class);
        startNewActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().getApplicationContext().startActivity(startNewActivityIntent);
    }

    protected String addColor(String text) {
        int textColorInt = getResources().getColor(R.color.get_access_text);
        String textColorStr = '#' + Integer.toHexString(textColorInt).toLowerCase().substring(2);
        return String.format("<font color=\"" + textColorStr + "\" >%s</font>", text);
    }

    public void updateUi() {}

    public void goBack() {
        FragmentManager fm = getChildFragmentManager();
        boolean popped = fm.popBackStackImmediate();
        if (!popped) {
            openPreviousScreen();
        }
    }

    protected abstract void openPreviousScreen();
}
