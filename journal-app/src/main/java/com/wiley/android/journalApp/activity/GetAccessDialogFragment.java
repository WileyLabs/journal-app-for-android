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
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.authorization.OAuthHelper;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.fragment.access.AbstractScreenFragment;
import com.wiley.android.journalApp.fragment.access.ScreenAFragment;
import com.wiley.android.journalApp.fragment.access.ScreenBFragment;
import com.wiley.android.journalApp.fragment.access.ScreenCFragment;
import com.wiley.android.journalApp.fragment.access.ScreenDFragment;
import com.wiley.android.journalApp.fragment.access.ScreenRegisterConfirmFragment;
import com.wiley.android.journalApp.fragment.access.ScreenRegisterErrorFragment;
import com.wiley.android.journalApp.fragment.access.ScreenRegisterFragment;
import com.wiley.android.journalApp.fragment.access.ScreenSubscriptionFragment;
import com.wiley.android.journalApp.fragment.access.TPSLoginFragment;
import com.wiley.android.journalApp.fragment.access.TPSSelectionFragment;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.util.List;
import java.util.Map;

import roboguice.inject.InjectExtra;

import static com.wiley.wol.client.android.data.service.AuthorizationService.LoggedInInformation;
import static com.wiley.wol.client.android.notification.EventList.AUTH_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.SOCIETY_UPDATED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SOCIETY_UPDATED_SUCCESS;
import static com.wiley.wol.client.android.notification.EventList.TPS_SITES_UPDATED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.TPS_SITES_UPDATED_SUCCESS;

/**
 * Created by taraskreknin on 26.06.14.
 */
public class GetAccessDialogFragment extends JournalFragment implements OAuthHelper.Listener, Journal {

    private static final String TAG = GetAccessDialogFragment.class.getSimpleName();

    private static final int SCREEN_A = 0;
    private static final int SCREEN_B = 1;
    private static final int SCREEN_C = 2;
    private static final int SCREEN_D = 3;
    private static final int SCREEN_TPS_SELECTION = 5;
    private static final int SCREEN_TPS_LOGIN = 6;
    private static final int SCREEN_SUBSCRIPTION = 7;
    private static final int SCREEN_REGISTER = 8;
    private static final int SCREEN_REGISTER_ERROR = 9;
    private static final int SCREEN_REGISTER_CONFIRM = 10;

    private static final String CURRENT_SCREEN_KEY = "com.wiley.android.journalApp.activity.GetAccessDialogActivity_currentScreen";
    public static final String EXTRA_WARN_MESSAGE_KEY = "com.wiley.android.journalApp.activity.GetAccessDialogActivity_warnMsg";
    private static final String CURRENT_FRAGMENT_TAG = "GetAccessDialogActivity_currentFragment";
    public static final String RESULT_CODE = "resultCode";
    public static final String REQUEST_CODE = "requestCode";

    @Inject
    private AANHelper aanHelper;
    @Inject
    private ImageLoaderHelper mImageLoader;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private AuthorizationService mAuthService;
    @Inject
    private Settings mSettings;
    @InjectExtra(value = EXTRA_WARN_MESSAGE_KEY, optional = true)
    private String mWarningMsg;

    private int mCurrentScreen = SCREEN_A;
    private int lastLoginScreen = SCREEN_A;

    private View mBackButton;
    private View mProgress;
    private TextView getAccessDialogHeader;

    private List<TPSSiteMO> mTpsSites;
    private TPSSiteMO mSelectedSite;

    private OAuthHelper mOAuthHelper;

    private final NotificationProcessor authSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            Logger.d(TAG, "authSuccessProcessor");
            getActivity().getIntent().putExtra(RESULT_CODE, Activity.RESULT_OK);
            if (lastLoginScreen == SCREEN_TPS_LOGIN) {
                mAuthService.saveLastLoginInformation(
                        new LoggedInInformation(
                                mSelectedSite.getTPSName(),
                                SCREEN_TPS_LOGIN));
            } else {
                mAuthService.saveLastLoginInformation(
                        new LoggedInInformation(lastLoginScreen));
            }
            finish();
        }
    };

    public void finish() {
        ((MainActivity) getActivity()).finishAccessDialog();
    }

    private NotificationProcessor mSocietyUpdateSuccessProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateUi();
        }
    };
    private NotificationProcessor mSocietyUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateUi();
        }
    };
    private NotificationProcessor mTpsFeedUpdateProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            mTpsSites = null;
            updateUi();
        }
    };private NotificationProcessor mTpsFeedUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateUi();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.get_access, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate()");
        super.onViewCreated(view, savedInstanceState);

        notificationCenter.subscribeToNotification(AUTH_SUCCESS.getEventName(), authSuccessProcessor);

        getView().findViewById(R.id.get_access_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                {
                    aanHelper.trackActionAbandonGetAccessDialogue();
                }
                GANHelper.trackEventWithoutOrientation(getGANHelperEvent(), GANHelper.ACTION_UI_BUTTON, GANHelper.LABEL_CLOSE, -1L);
                getActivity().getIntent().putExtra("resultCode", Activity.RESULT_CANCELED);
                finish();
            }
        });
        getAccessDialogHeader = findView(R.id.get_access_dialog_header);
        mBackButton = getView().findViewById(R.id.get_access_go_back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GANHelper.trackEventWithoutOrientation(getGANHelperEvent(), GANHelper.ACTION_UI_BUTTON, GANHelper.LABEL_BACK, -1L);
                goBack();
            }
        });
        mProgress = getView().findViewById(R.id.get_access_progress);
        mProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // consume all clicks while showing progress
            }
        });
        hideProgress();
        if (DeviceUtils.isTablet(this.getActivity())) {
            final ImageView logoBanner = (ImageView) getView().findViewById(R.id.get_access_banner);
            logoBanner.setVisibility(View.VISIBLE);
            mImageLoader.displayImage("assets://html/Images/About/logoBanner@2x.png", logoBanner);
        }
        restoreCurrentScreenNumber(savedInstanceState);
        if (getChildFragmentManager().findFragmentByTag(CURRENT_FRAGMENT_TAG) == null) {
            openInitialScreen();
        }
    }

    private void openInitialScreen() {
        LoggedInInformation lastLoginInfo = mSettings.hasLastLoginInfo() ? mAuthService.getLastLoginInformation() : null;
        if (lastLoginInfo == null) {
            openScreen(SCREEN_A, false, null);
        } else if (lastLoginInfo.viaTps) {
            mSelectedSite = findTpsSiteByName(lastLoginInfo.tpsSiteName);
            openScreen(mSelectedSite != null ? SCREEN_TPS_LOGIN : SCREEN_TPS_SELECTION, false, null);
        } else {
            openScreen(lastLoginInfo.lastSuccessfulLoginScreen, false, null);
        }
    }

    private TPSSiteMO findTpsSiteByName(String siteName) {
        for (TPSSiteMO site : getTpsSites()) {
            if (site.getTPSName().equals(siteName)) {
                return site;
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOAuthHelper != null && !mOAuthHelper.isInProgress()) {
            hideProgress();
            mOAuthHelper = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(SOCIETY_UPDATED_SUCCESS.getEventName(), mSocietyUpdateSuccessProcessor);
        notificationCenter.subscribeToNotification(SOCIETY_UPDATED_ERROR.getEventName(), mSocietyUpdateErrorProcessor);
        notificationCenter.subscribeToNotification(TPS_SITES_UPDATED_SUCCESS.getEventName(), mTpsFeedUpdateProcessor);
        notificationCenter.subscribeToNotification(TPS_SITES_UPDATED_ERROR.getEventName(), mTpsFeedUpdateErrorProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(mSocietyUpdateSuccessProcessor);
        notificationCenter.unSubscribeFromNotification(mSocietyUpdateErrorProcessor);
        notificationCenter.unSubscribeFromNotification(mTpsFeedUpdateProcessor);
        notificationCenter.unSubscribeFromNotification(mTpsFeedUpdateErrorProcessor);
    }

    public String getWarningMessage() {
        return mWarningMsg;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState = outState == null ? new Bundle() : outState;
        outState.putInt(CURRENT_SCREEN_KEY, mCurrentScreen);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        restoreCurrentScreenNumber(savedInstanceState);
    }

    private void restoreCurrentScreenNumber(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentScreen = savedInstanceState.getInt(CURRENT_SCREEN_KEY, SCREEN_A);
        }
    }

    public void backToScreenA() {
        openScreen(SCREEN_A, true, false, null);
    }

    public void backToScreenB() {
        openScreen(SCREEN_B, true, false, null);
    }

    public void backToTpsSelectionScreen() {
        if (mTpsSites.size() == 1) {
            openScreen(SCREEN_B, true, false, null);
        } else {
            openScreen(SCREEN_TPS_SELECTION, true, false, null);
        }
    }

    public void openScreenD() {
        openScreen(SCREEN_D);
    }

    public void openScreenC() {
        openScreen(SCREEN_C);
    }

    public void openScreenB() {
        openScreen(SCREEN_B);
    }

    public void openTpsSelectionScreen() {
        openScreen(SCREEN_TPS_SELECTION);
    }

    public void openTpsLoginScreen() {
        openScreen(SCREEN_TPS_LOGIN, true, null);
    }

    public void openSubscriptionScreen() {
        openScreen(SCREEN_SUBSCRIPTION);
    }

    public void openRegisterScreen() {
        openScreen(SCREEN_REGISTER);
    }

    public void openRegisterScreen(Bundle args) {
        openScreen(SCREEN_REGISTER, args);
    }

    public void openRegisterErrorScreen() {
        openScreen(SCREEN_REGISTER_ERROR);
    }

    public void openRegisterErrorScreen(Bundle args) {
        openScreen(SCREEN_REGISTER_ERROR, args);
    }

    public void openRegisterConfirmScreen() {
        openScreen(SCREEN_REGISTER_CONFIRM);
    }

    private void openScreen(int screen) {
        openScreen(screen, true, true, null);
    }

    private void openScreen(int screen, Bundle args) {
        openScreen(screen, true, true, args);
    }

    private void openScreen(int screen, boolean animate, Bundle args) {
        openScreen(screen, animate, false, args);
    }

    private void openScreen(int screen, boolean animate, boolean addToStack, Bundle args) {
        if (screen != SCREEN_REGISTER_CONFIRM && screen != SCREEN_REGISTER && screen != SCREEN_REGISTER_ERROR) {
            lastLoginScreen = screen;
        }
        mCurrentScreen = screen;
        AbstractScreenFragment frag = getScreenFragment(screen);
        frag.setArguments(args);
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (animate) {
            ft.setCustomAnimations(android.R.anim.fade_in, 0, android.R.anim.fade_in, 0);
        }
        if (addToStack) {
            ft.addToBackStack(null);
        }
        ft.replace(R.id.get_access_screen_container, frag, CURRENT_FRAGMENT_TAG).commit();
    }

    public void goBack() {
        AbstractScreenFragment fragment = (AbstractScreenFragment) getChildFragmentManager().findFragmentByTag(CURRENT_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.goBack();
        }
    }

    private void updateUi() {
        AbstractScreenFragment fragment = (AbstractScreenFragment) getChildFragmentManager().findFragmentByTag(CURRENT_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.updateUi();
        }
    }

    public void setBackButtonEnabled(boolean enabled) {
        int backBtnVisibility = enabled ? View.VISIBLE : View.INVISIBLE;
        mBackButton.setVisibility(backBtnVisibility);
    }

    private AbstractScreenFragment getScreenFragment(int screen) {
        if (screen == SCREEN_A) {
            return new ScreenAFragment();
        } else if (screen == SCREEN_B) {
            return new ScreenBFragment();
        } else if (screen == SCREEN_C) {
            return new ScreenCFragment();
        } else if (screen == SCREEN_D) {
            return new ScreenDFragment();
        } else if (screen == SCREEN_TPS_SELECTION) {
            return new TPSSelectionFragment();
        } else if (screen == SCREEN_TPS_LOGIN) {
            return new TPSLoginFragment();
        } else if (screen == SCREEN_SUBSCRIPTION) {
            return new ScreenSubscriptionFragment();
        } else if (screen == SCREEN_REGISTER) {
            return new ScreenRegisterFragment();
        } else if (screen == SCREEN_REGISTER_ERROR) {
            aanHelper.trackRegistrationErrorDuplicateEmailOverlay();
            return new ScreenRegisterErrorFragment();
        } else if (screen == SCREEN_REGISTER_CONFIRM) {
            aanHelper.trackRegistrationConfirmedOverlay();
            return new ScreenRegisterConfirmFragment();
        }
        return null;
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "onDestroy()");
        notificationCenter.unSubscribeFromNotification(authSuccessProcessor);
        super.onDestroy();
    }

    public void onNeedTpsAccess() {
        mTpsSites = getTpsSites();
        if (mTpsSites.size() == 1) {
            mSelectedSite = mTpsSites.get(0);
            openTpsLoginScreen();
        } else if (mTpsSites.size() > 1) {
            openTpsSelectionScreen();
        } else {
            throw new RuntimeException("no tps sites found");
        }
    }

    public List<TPSSiteMO> getTpsSites() {
        if (mTpsSites == null) {
            mTpsSites = mAuthService.getAllTPSSites();
        }
        return mTpsSites;
    }

    public TPSSiteMO getSelectedSite() {
        return mSelectedSite;
    }

    public void setSelectedSite(final TPSSiteMO site) {
        mSelectedSite = site;
    }

    public void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        mProgress.setVisibility(View.GONE);
    }

    public void doOAuthLoginWithTpsCredentials() {
        final CustomWebView webView = (CustomWebView) getView().findViewById(R.id.get_access_hidden_web_view);
        mOAuthHelper = new OAuthHelper(webView, this);
        mOAuthHelper.authorize(true, mAuthService.getTPSUsername(), mAuthService.getTPSPassword());
    }

    @Override
    public void onAuthProgress(String message) {
        // todo use message
        showProgress();
    }

    @Override
    public void onWaitForUserAction() {}

    @Override
    public void onBackPressed() {
    }

    private String getGANHelperEvent() {
        String eventGANHelper = "";
        switch (mCurrentScreen) {
            case SCREEN_A: eventGANHelper = GANHelper.EVENT_GET_ACCESS_A; break;
            case SCREEN_B: eventGANHelper = GANHelper.EVENT_GET_ACCESS_B; break;
            case SCREEN_C: eventGANHelper = GANHelper.EVENT_GET_ACCESS_C; break;
            case SCREEN_D: eventGANHelper = GANHelper.EVENT_GET_ACCESS_D; break;
            case SCREEN_TPS_SELECTION: eventGANHelper = GANHelper.EVENT_GET_ACCESS_TPS_SELECTION; break;
            case SCREEN_TPS_LOGIN: eventGANHelper = GANHelper.EVENT_GET_ACCESS_TPS_LOGIN; break;
        }

        return eventGANHelper;
    }

    public void setDialogHeader(final String text) {
        getAccessDialogHeader.setText(text);
    }
}
