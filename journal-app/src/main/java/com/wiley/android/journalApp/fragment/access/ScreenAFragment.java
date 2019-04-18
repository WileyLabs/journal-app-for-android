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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.UpdateOperationActivity;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;

import org.solovyev.android.checkout.ResponseCodes;

import java.util.Map;

import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;

/**
 * Created by taraskreknin on 26.06.14.
 */
public class ScreenAFragment extends AbstractScreenFragment {
    /**
     *  feature: in-app purchase
    */

    private NotificationProcessor mInAppBillingPurchaseSubsSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            UpdateOperationActivity.startRestoreAllPurchases(getJournalActivity());
        }
    };

    private NotificationProcessor mInAppBillingPurchaseSubsErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            ParamsReader paramsReader = new ParamsReader(params);
            final String title = "Buy a subscription";
            String  message = null;
            int errorCode = paramsReader.getError();
            switch(errorCode) {
                case 1:
                case 5:
                case 6: // 'User in not eligible for this purchase' message
                case ResponseCodes.ITEM_ALREADY_OWNED:
                case 10001:
                    break;
                case -1:
                    message = "Subscription is not available";
                    break;
                default:
                    message = "error code = " + errorCode;

            }
            if (null != message) {
                mErrorManager.alertWithErrorMessage(getActivity(), new ErrorMessage(title, message));
            }
        }
    };

    private final NotificationProcessor inAppBillingCheckMcsPurchasesCompletedProcessor = new NotificationProcessor()
    {
        @Override
        public void processNotification(final Map<String, Object> params) {
            if (null == getActivity() || getActivity().isFinishing()) {
                return;
            }

            ParamsReader paramsReader = new ParamsReader(params);
            if (paramsReader.succeed()) {
                getAccessDialogFragment().finish();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_a, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findView(R.id.get_access_browse_free_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                {
                    aanHelper.trackActionBrowseFreeContentOnly();
                }
                GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_GET_ACCESS_A, GANHelper.ACTION_BUTTON, GANHelper.LABEL_BROWSE_FREE, -1L);
                getAccessDialogFragment().finish();
            }
        });
        findView(R.id.get_access_i_have_access).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccessDialogFragment().openScreenB();
                GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_GET_ACCESS_A, GANHelper.ACTION_BUTTON, GANHelper.LABEL_YES_I_HAVE_ACCESS, -1L);
            }
        });
        findView(R.id.get_access_i_have_a_subscription_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccessDialogFragment().openSubscriptionScreen();
            }
        });

        { // feature: in-app purchase
            if (mTheme.isEnableSubscription()) {
                findView(R.id.get_access_i_want_buy_subscription_from_store).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!DeviceUtils.isInternetConnectionAvailable(getActivity())) {
                            mErrorManager.alertWithErrorCode(getActivity(), NO_CONNECTION_AVAILABLE);
                        } else {
                            aanHelper.trackActionIWantToBuyASubscription();
                            mInAppBillingService.buyJournalSubscription();
                        }
                    }
                });

                findView(R.id.get_access_i_want_buy_subscription_from_store).setVisibility(View.VISIBLE);
            } else {
                findView(R.id.get_access_i_want_buy_subscription_from_store).setVisibility(View.GONE);
            }
        }

        final TextView warn = findView(R.id.get_access_warn_message);
        final String warnMsg = getAccessDialogFragment().getWarningMessage();
        int warnVisibility = TextUtils.isEmpty(warnMsg) ? View.GONE: View.VISIBLE;
        warn.setVisibility(warnVisibility);
        warn.setText(warnMsg);

        {
            aanHelper.trackGetAccessDialogueDoYouHaveAccess();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        { // feature: in-app purchase
            mNotificationCenter.subscribeToNotification(EventList.IN_APP_BILLING_PURCHASE_SUBS_SUCCESS.getEventName(), mInAppBillingPurchaseSubsSuccessProcessor);
            mNotificationCenter.subscribeToNotification(EventList.IN_APP_BILLING_PURCHASE_SUBS_ERROR.getEventName(), mInAppBillingPurchaseSubsErrorProcessor);
            mNotificationCenter.subscribeToNotification(EventList.IN_APP_BILLING_CHECK_MCS_PURCHASES_COMPLETED.getEventName(), inAppBillingCheckMcsPurchasesCompletedProcessor);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        { // feature: in-app purchase
            mNotificationCenter.unSubscribeFromNotification(mInAppBillingPurchaseSubsSuccessProcessor);
            mNotificationCenter.unSubscribeFromNotification(mInAppBillingPurchaseSubsErrorProcessor);
            mNotificationCenter.unSubscribeFromNotification(inAppBillingCheckMcsPurchasesCompletedProcessor);

        }
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public void updateUi() {
    }

    @Override
    protected void openPreviousScreen() {
        getAccessDialogFragment().finish();
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_A;
    }
}
