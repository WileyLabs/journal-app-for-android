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
package com.wiley.android.journalApp.fragment.tabs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Environment;
import com.wiley.wol.client.android.utils.NetUtils;

import java.util.GregorianCalendar;
import java.util.Map;

import static com.wiley.wol.client.android.notification.EventList.IN_APP_CONTENT_UPDATED;
import static com.wiley.wol.client.android.notification.EventList.IN_APP_ERROR;
import static com.wiley.wol.client.android.notification.EventList.IN_APP_NEED_UPDATE;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

/**
 * Created by taraskreknin on 06.05.14.
 */
public class InfoFragment extends BaseTabFragment {

    private static final String COPYRIGHT_YEAR_REPLACEMENT_KEY = "@@copyright_year_placeholder@@";
    private static final String APP_VERSION_REPLACEMENT_KEY = "@@version_number@@";
    private static final String FEEDBACK_SCHEME = "feedback://";
    private static final String PRIVACY_POLICY_SCHEME = "privacypolicy://";
    private static final String GOOGLE_PLAY_REF = ".*play\\.google\\.com.*";
    private static final String ABOUT_JOURNAL_KEY = "about_journal";

    @Inject
    private AANHelper aanHelper;
    @Inject
    private Environment mEnvironment;
    @Inject
    private WebController mWebController;
    @Inject
    private EmailSender mEmailSender;
    @Inject
    private Theme mTheme;
    @Inject
    private SharedPreferences preferences;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private Templates templates;
    @Inject
    private ImportManager importManager;

    private WebView mWebView;
    private ProgressBar mProgress;

    private final Templates mTemplates = new Templates();
    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            openLink(url);
            return true;
        }
    };

    private final NotificationProcessor inAppErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (!preferences.contains(ABOUT_JOURNAL_KEY)) {
                final Exception exception = (Exception) params.get(ERROR);
                showErrorMessageLayout(errorManager, exception);
            }
        }
    };

    private final NotificationProcessor inAppContentUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            findView(R.id.error_message_layout).setVisibility(View.GONE);
            setWebViewContent(getInfoHTML());
        }
    };

    private void openLink(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (url.matches(GOOGLE_PLAY_REF)) {
            mWebController.openUrlExternalIfCan(url);
        } else if (url.startsWith(FEEDBACK_SCHEME)) {
            {
                aanHelper.trackActionOpenEmailForm("Send Feedback");
            }
            GANHelper.trackEvent(GANHelper.EVENT_INFO,
                    GANHelper.ACTION_FEEDBACK,
                    null,
                    0L);
            mEmailSender.sendFeedBack(getActivity());
        } else if (url.startsWith(PRIVACY_POLICY_SCHEME)) {
            GANHelper.trackEvent(GANHelper.EVENT_INFO,
                    GANHelper.ACTION_PRIVACY_POLICY,
                    null,
                    0L);
            {
                aanHelper.trackActionOpenWebViewerForOtherPage(mTheme.getPrivacyPolicyUrl());
            }
            // TODO track in GA
            mWebController.openUrlInternal(mTheme.getPrivacyPolicyUrl());
        } else if (url.startsWith("http")) {
            {
                aanHelper.trackActionOpenWebViewerForOtherPage(url);
            }
            mWebController.openUrlInternal(url);
        } else if (url.startsWith("legalnotice://")) {
            mWebController.openUrlInternal("file:///android_asset/NOTICE.html");
        } else {
            mWebController.openUrlExternalIfCan(url);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.info_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUi();
        setWebViewContent(getInfoHTML());
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(IN_APP_ERROR.getEventName(), inAppErrorProcessor);
        notificationCenter.subscribeToNotification(IN_APP_CONTENT_UPDATED.getEventName(), inAppContentUpdatedProcessor);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(inAppErrorProcessor);
        notificationCenter.unSubscribeFromNotification(inAppContentUpdatedProcessor);
    }

    @Override
    protected int getTabId() {
        return R.id.info_tab;
    }

    private void initUi() {
        findView(R.id.error_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetUtils.isOnline(getActivity())) {
                    errorManager.alertWithErrorCode(getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
                } else {
                    importManager.updateInAppContent();
                }
            }
        });

        mWebView = findView(R.id.info_web_view);
        mProgress = findView(R.id.info_progress);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.getSettings().setJavaScriptEnabled(true);
    }

    private void setWebViewContent(String content) {
        if (!TextUtils.isEmpty(content)) {
            final String mimeType = "text/html";
            final String encoding = "utf-8";
            mWebView.loadDataWithBaseURL("fake", content, mimeType, encoding, "");
        } else {
            mWebView.loadUrl("");
            notificationCenter.sendNotification(IN_APP_NEED_UPDATE.getEventName());
        }
    }

    private String getInfoHTML() {
        if (!isLocalInfoCopyExists()) {
            return "";
        }
        final String appVersion = mEnvironment.getAppVersion();
        final int copyrightYear = GregorianCalendar.getInstance().get(GregorianCalendar.YEAR);

        String html = mTemplates.useTemplate(getActivity(), preferences.getString(ABOUT_JOURNAL_KEY, ""))
                .putParam(APP_VERSION_REPLACEMENT_KEY, appVersion)
                .putParam(COPYRIGHT_YEAR_REPLACEMENT_KEY, copyrightYear)
                .proceed();

        // TODO: fix this hack
        // modify css on server side
        html = html.replace("\n/* iPhone, iPad portrait */\n@media only screen and (min-device-width : 320px) and (max-device-width : 568px), \n\t   only screen and (max-device-width : 640px), \n\t   only screen and (min-device-width : 768px) and (max-device-width : 1024px) and (orientation : portrait) \n",
                "@media only screen and (orientation : portrait)");
        return html;
    }

    private void onInfoLoadingStarted() {
        startProgress();
    }

    private void startProgress() {
        mProgress.setVisibility(View.VISIBLE);
    }

    private void onInfoLoadingStopped(Object error, boolean feedNotModified) {
        stopProgress();

        if (feedNotModified) {
            return; // check if the content was set
        }
        if (error == null) {
            setWebViewContent(getInfoHTML());
        } else if (isLocalInfoCopyExists()) {
            setWebViewContent(getInfoHTML());
        } else {
            showInfoLoadingError(error);
        }
    }

    private boolean isLocalInfoCopyExists() {
        return preferences.contains(ABOUT_JOURNAL_KEY);
    }

    private void showInfoLoadingError(Object error) {
        Toast.makeText(getActivity(), "Info loading error " + error.toString(), Toast.LENGTH_SHORT).show();
    }

    private void stopProgress() {
        mProgress.setVisibility(View.GONE);
    }

    @Override
    public void onShow() {
        super.onShow();
        if (getActivity() != null) {
            UIUtils.hideSoftInput(getActivity());
        }
    }
}
