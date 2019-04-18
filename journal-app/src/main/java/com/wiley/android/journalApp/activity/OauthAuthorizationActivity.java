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
package com.wiley.android.journalApp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.authorization.OAuthHelper;
import com.wiley.android.journalApp.base.ActivityWithActionBar;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.util.Map;

/**
 * @author Sergey Rybakov
 */
public class OauthAuthorizationActivity extends ActivityWithActionBar implements OAuthHelper.Listener {

    private static final String TAG = OauthAuthorizationActivity.class.getSimpleName();

    public static Intent getStartingIntent(Context context) {
        return getStartingIntent(context, false);
    }

    public static Intent getStartingIntent(Context context, boolean afterUserActivation) {
        Intent intent = new Intent(context, OauthAuthorizationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("afterUserActivation", afterUserActivation);
        return intent;
    }

    private CustomWebView webView;
    @Inject
    private AANHelper aanHelper;
    @Inject
    private Settings settings;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Theme theme;
    @Inject
    private ErrorManager errorManager;


    private OAuthHelper oAuthHelper;
    private View progress;
    private TextView progressText;

    private NotificationProcessor authSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            boolean afterActivation = params.containsKey(NotificationCenter.LOGGED_IN_AFTER_USER_ACTIVATION)
                    && (boolean) params.get(NotificationCenter.LOGGED_IN_AFTER_USER_ACTIVATION);
            if (afterActivation) {
                settings.setShowSponsoredPromo(true);
            }
            OauthAuthorizationActivity.this.finish();
        }
    };

    private NotificationProcessor authErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            ErrorButton finish = ErrorButton.withTitleAndListener(OauthAuthorizationActivity.this.getString(android.R.string.ok),
                    new ErrorButton.OnClickListener() {
                        @Override
                        public void onClick() {
                            OauthAuthorizationActivity.this.finish();
                        }
                    });
            errorManager.alertWithErrorCode(OauthAuthorizationActivity.this, AppErrorCode.AUTHORIZATION_FAILED, finish);
        }
    };

    @Override
    protected void initContentView(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_oauth_authorization);
        webView = (CustomWebView) findViewById(R.id.authorization_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        oAuthHelper = new OAuthHelper(webView, this);

        progress = findView(R.id.oauth_progress);
        progress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // consume all clicks
            }
        });
        progressText = findView(R.id.oauth_progress_text);
        hideProgress();
        setTitle("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(EventList.AUTH_SUCCESS.getEventName(), authSuccessProcessor);
        notificationCenter.subscribeToNotification(EventList.AUTH_ERROR.getEventName(), authErrorProcessor);
    }

    @Override
    protected void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(authSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(authErrorProcessor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (oAuthHelper.isInProgress()) {
            return;
        }
        if (getIntent().getBooleanExtra("afterUserActivation", false)) {
            oAuthHelper.authorizeAfterUserActivation();
        } else {
            oAuthHelper.authorize();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            oAuthHelper.stopAuthorisation();
        }
    }

    @Override
    protected void onHome() {
        {
            aanHelper.trackActionAbandonLoginToWOL();
        }
        super.onHome();
    }

    @Override
    public void onAuthProgress(String message) {
        showProgress(message);
    }

    @Override
    public void onWaitForUserAction() {
        hideProgress();
    }

    private void showProgress(String message) {
        if (TextUtils.isEmpty(message)) {
            progressText.setVisibility(View.GONE);
        } else {
            progressText.setText(message);
        }
        progress.setVisibility(View.VISIBLE);
    }

    private void showProgress() {
        showProgress(null);
    }

    private void hideProgress() {
        progress.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (oAuthHelper.isInProgress()) {
            hideProgress();
            oAuthHelper.stopAuthorisation();
        } else {
            super.onBackPressed();
        }
    }
}
