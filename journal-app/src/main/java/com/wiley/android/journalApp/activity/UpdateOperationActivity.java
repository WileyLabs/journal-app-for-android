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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.wol.client.android.data.http.UpdateManager;
import com.wiley.wol.client.android.data.service.InAppBillingService;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Settings;

import java.util.Map;

import static com.wiley.wol.client.android.notification.EventList.ALL_CONTENT_UPDATE_FINISHED;

/**
 * Created by alobachev on 03/04/15.
 */

public class UpdateOperationActivity
        extends
            JournalActivity
        implements
            View.OnClickListener {

    private View mProgressView;
    private TextView mProgressInfo;
    private boolean isActiveErrorDialog = false;

    @Inject
    protected Settings mSettings;
    @Inject
    protected UpdateManager mUpdateManager;
    @Inject
    protected InAppBillingService mInAppBillingService;
    @Inject
    protected ErrorManager mErrorManager;
    @Inject
    protected NotificationCenter mNotificationCenter;

    /**
     *  feature: in-app purchase
    */

    final NotificationProcessor inAppBillingLoadPurchasesCompletedProcessor = new NotificationProcessor()
    {
        @Override
        public void processNotification(final Map<String, Object> params) {
            ParamsReader paramsReader = new ParamsReader(params);
            if (paramsReader.succeed()) {
                mInAppBillingService.checkMcsSubscription();
            } else {
                hideProgressWithText();

                final String title = "Restore all purchases";
                String  message;
                AppErrorCode appErrorCode = paramsReader.getAppErrorCode();
                if (appErrorCode == AppErrorCode.NO_PREVIOUS_SUBSCRIPTION) {
                    message = "You don't have purchased a subscription";
                } else {
                    message = "Load Purchases: Undefined error";
                }

                isActiveErrorDialog = true;
                mErrorManager.alertWithErrorMessage(getActivity()
                        , new ErrorMessage(title, message)
                        , ErrorButton.withTitleAndListener(getActivity().getString(R.string.close), new ErrorButton.OnClickListener()
                {
                    @Override
                    public void onClick() {
                        finish();
                    }
                }));
            }

        }
    };

    final NotificationProcessor inAppBillingCheckMcsPurchasesCompletedProcessor = new NotificationProcessor()
    {
        @Override
        public void processNotification(final Map<String, Object> params) {
            ParamsReader paramsReader = new ParamsReader(params);
            if (paramsReader.succeed()) {
                showProgressWithText(getString(R.string.refreshing_content));
                mUpdateManager.updateFeedsForce();
            } else {
                hideProgressWithText();

                final String title = "Restore all purchases";
                String  message;
                AppErrorCode appErrorCode = paramsReader.getAppErrorCode();
                if (appErrorCode == AppErrorCode.CONTENT_LOCKED_FOR_ANDROID) {
                    message = "Content server is locked for you";
                } else if (appErrorCode == AppErrorCode.IO_EXCEPTION) {
                    message = paramsReader.getErrorMessage();
                } else if (appErrorCode == AppErrorCode.NO_CONNECTION_AVAILABLE) {
                    message = "ERROR: status code = " + paramsReader.getErrorMessage();
                } else {
                    message = "Check Mcs: Undefined error";
                }

                isActiveErrorDialog = true;
                mErrorManager.alertWithErrorMessage(getActivity()
                        , new ErrorMessage(title, message)
                        , ErrorButton.withTitleAndListener(getActivity().getString(R.string.close), new ErrorButton.OnClickListener() {
                    @Override
                    public void onClick() {
                        finish();
                    }
                }));
            }
        }
    };

    private final NotificationProcessor feedsUpdateProcessor = new NotificationProcessor()
    {
        @Override
        public void processNotification(final Map<String, Object> params) {
            ParamsReader paramsReader = new ParamsReader(params);
            if (paramsReader.getMode().equals(UpdateManager.UPDATE_TASK_FORCE)) {
                hideProgressWithText();
                if (!isActiveErrorDialog) {
                    finish();
                }
            }
        }
    };

    private Activity getActivity() {
        return this;
    }

    public static void startRestoreAllPurchases(Activity context) {
        Intent intent = new Intent(context, UpdateOperationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_operation);

        mProgressView = findView(R.id.progress_view);
        mProgressInfo = findView(R.id.progress_info);
    }

    @Override
    public void onStart() {
        super.onStart();
        { // feature: in-app purchase
            mNotificationCenter.subscribeToNotification(EventList.IN_APP_BILLING_LOAD_PURCHASES_COMPLETED.getEventName(), inAppBillingLoadPurchasesCompletedProcessor);
            mNotificationCenter.subscribeToNotification(EventList.IN_APP_BILLING_CHECK_MCS_PURCHASES_COMPLETED.getEventName(), inAppBillingCheckMcsPurchasesCompletedProcessor);
            mNotificationCenter.subscribeToNotification(ALL_CONTENT_UPDATE_FINISHED.getEventName(), feedsUpdateProcessor);
        }

        showProgressWithText(getString(R.string.loading_purchases));
        mInAppBillingService.loadPurchases();
    }

    @Override
    public void onStop() {
        super.onStop();
        { // feature: in-app purchase
            mNotificationCenter.unSubscribeFromNotification(inAppBillingLoadPurchasesCompletedProcessor);
            mNotificationCenter.unSubscribeFromNotification(inAppBillingCheckMcsPurchasesCompletedProcessor);
            mNotificationCenter.unSubscribeFromNotification(feedsUpdateProcessor);
        }
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void showProgressWithText(String info) {
        if (mProgressInfo == null) {
            return;
        }

        mProgressInfo.setText(info);
        showProgressWithText();
    }

    private Handler hideProgressHandler = new Handler();
    public void showProgressWithText() {
        if (mProgressView == null) {
            return;
        }
        hideProgressHandler.removeCallbacksAndMessages(null);
        mProgressView.setVisibility(View.VISIBLE);
    }

    public void hideProgressWithText() {
        if (mProgressView == null) {
            return;
        }
        hideProgressHandler.removeCallbacksAndMessages(null);
        hideProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mProgressView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                fadeOut.setDuration(500);
                mProgressView.startAnimation(fadeOut);
            }
        }, 200);
    }


}