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
import com.wiley.android.journalApp.activity.UpdateOperationActivity;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;

import java.util.Map;

import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;

/**
 * Created by taraskreknin on 26.06.14.
 */
public class ScreenBFragment extends AbstractScreenFragment {
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
        return inflater.inflate(R.layout.frag_access_screen_b, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final View accessThroughSocietyAffiliationButton = findView(R.id.get_access_through_society_affiliation);
        if (!mAuthService.hasSociety() && !mAuthService.hasTPS()) {
            accessThroughSocietyAffiliationButton.setVisibility(View.GONE);
        } else {
            accessThroughSocietyAffiliationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_GET_ACCESS_B, GANHelper.ACTION_LINK_TO_GET_ACCESS_C, GANHelper.LABEL_SOCIETY, -1L);
                    if (mAuthService.hasTPS()) {
                        getAccessDialogFragment().onNeedTpsAccess();
                    } else {
                        getAccessDialogFragment().openScreenC();
                    }
                }
            });
        }
        findView(R.id.get_access_via_network).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_GET_ACCESS_B, GANHelper.ACTION_LINK_TO_GET_ACCESS_D, GANHelper.LABEL_INSTITUTIONAL, -1L);
                getAccessDialogFragment().openScreenD();
            }
        });
        findView(R.id.get_access_log_in_to_wol).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                {
                    aanHelper.trackActionLoginToWOL();
                }
                GANHelper.trackEventWithoutOrientation(GANHelper.EVENT_GET_ACCESS_B, GANHelper.ACTION_LINK, GANHelper.LABEL_WOL_LOGIN, -1L);
                onNeedWolLogIn();
            }
        });

        { // feature: in-app purchase
            if (mTheme.isEnableSubscription()) {
                findView(R.id.get_access_restore_purchases).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!DeviceUtils.isInternetConnectionAvailable(getActivity())) {
                            mErrorManager.alertWithErrorCode(getActivity(), NO_CONNECTION_AVAILABLE);
                        } else {
                            aanHelper.trackActionRestoreAllPurchases();
                            UpdateOperationActivity.startRestoreAllPurchases(getJournalActivity());
                        }
                    }
                });

                findView(R.id.get_access_group_restore_purchases).setVisibility(View.VISIBLE);
            } else {
                findView(R.id.get_access_group_restore_purchases).setVisibility(View.GONE);
            }
        }

        { // feature: analytics
            aanHelper.trackGetAccessDialogueAccessType();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mNotificationCenter.subscribeToNotification(EventList.IN_APP_BILLING_CHECK_MCS_PURCHASES_COMPLETED.getEventName(), inAppBillingCheckMcsPurchasesCompletedProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        mNotificationCenter.unSubscribeFromNotification(inAppBillingCheckMcsPurchasesCompletedProcessor);
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_B;
    }

    @Override
    protected void openPreviousScreen() {
        getAccessDialogFragment().backToScreenA();
    }
}
