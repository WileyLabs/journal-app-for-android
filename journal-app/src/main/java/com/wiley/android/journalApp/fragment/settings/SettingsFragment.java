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
package com.wiley.android.journalApp.fragment.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.AlertDialogActivity;
import com.wiley.android.journalApp.activity.UpdateOperationActivity;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.dialog.SelectStringDialog;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.fragment.tabs.BaseTabFragment;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.http.DownloadManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.service.InAppBillingService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Environment;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;
import static com.wiley.wol.client.android.notification.EventList.AFFILIATION_INFO_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.ALL_CONTENT_UPDATE_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.EARLY_VIEW_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_LIST_UPDATED;
import static com.wiley.wol.client.android.notification.EventList.KEYWORDS_DEVICE_REGISTERED_ON_MCS;
import static com.wiley.wol.client.android.notification.EventList.KEYWORD_UPDATE_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.KEYWORD_UPDATE_STARTED;
import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;
import static com.wiley.wol.client.android.settings.Settings.SETTING_ACCESS_CODE;
import static com.wiley.wol.client.android.settings.Settings.SETTING_ARTICLE_FONT_SIZE;
import static com.wiley.wol.client.android.settings.Settings.SETTING_AUTH_TOKEN;
import static com.wiley.wol.client.android.settings.Settings.SETTING_CURRENT_SERVER;
import static com.wiley.wol.client.android.settings.Settings.SETTING_GOOGLE_DRIVE;
import static java.lang.String.format;

public class SettingsFragment extends BaseTabFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Inject
    private AANHelper aanHelper;
    @Inject
    protected Environment environment;
    @Inject
    protected Theme theme;
    @Inject
    protected Settings settings;
    @Inject
    protected Authorizer authorizer;
    @Inject
    protected WebController webController;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private ArticleService articleService;
    @Inject
    private InAppBillingService inAppBillingService;
    @Inject
    private ErrorManager errorManager;

    protected View progress;
    protected TextView progressInfo;

    // layout 'Account'
    private View layoutAccount;
    protected Button buttonGetAccess;
    protected Button buttonLogout;
    protected TextView labelFullAccess1;
    protected TextView labelFullAccess2;
    protected TextView labelNotFullAccess;
    protected Button buttonRefreshAccount;
    private Button buttonRefreshAffiliationInfo;
    protected View accessCodeGroup;
    protected EditText accessCodeText;
    protected ImageView accessCodeStatus;
    protected ProgressBar accessCodeProgress;
    protected Button accessCodeSubmit;
    protected TextView affiliationInfoView;
    protected View affiliationInfoDivider;
    protected TextView subscriptionInfo;
    protected View subscriptionInfoDivider;
    private View closeButton;

    // layout 'App Preferences'
    private View layoutAppPreferences;
    protected View showAbstractLine;
    protected Switch showAbstractSwitch;
    protected SeekBar fontSizeSeekBar;
    protected WebView fontSizeWebView;

    // layout 'Restore Purchases'
    private View layoutRestorePurchases;
    protected View groupRestorePurchases;
    protected Button buttonRestorePurchases;

    // layout 'Message Center'
    private View layoutMessageCenter;
    protected View emptyMessageCenter;
    protected LinearLayout keywordsMessageCenter;
    protected View progressMessageCenter;

    // layout 'Developer'
    private View layoutDeveloper;
    protected View headerDeveloper;
    protected View groupDeveloper;
    protected Button buttonChangeServer;
    protected Button buttonChangeOAuth;
    protected EditText debugIpAddress;
    // feature: google drive
    private String connectionGoogleDrive = "";
    private String savedArticlesFileGoogleDrive = "";
    private String readArticlesFileGoogleDrive = "";
    private String keywordsFileGoogleDrive = "";
    private String savedFeedItemsFileGoogleDrive = "";
    private String uaDeviceToken = "";

    protected AccessCodeChecker accessCodeChecker;
    private ViewTreeObserver observer;

    private final NotificationProcessor settingsChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            if (null == getActivity() || getActivity().isFinishing()) {
                return;
            }

            final String settingName = params.get(SETTING_NAME_KEY).toString();
            if (SETTING_ARTICLE_FONT_SIZE.equals(settingName)) {
                return;
            }
            { // feature: google drive
                if (SETTING_GOOGLE_DRIVE.equals(settingName)) {
                    if (params.containsKey("connection")) {
                        connectionGoogleDrive = params.get("connection").toString();
                    }

                    if (params.containsKey("saved_articles_file")) {
                        savedArticlesFileGoogleDrive = params.get("saved_articles_file").toString();
                    }

                    if (params.containsKey("read_articles_file")) {
                        readArticlesFileGoogleDrive = params.get("read_articles_file").toString();
                    }

                    if (params.containsKey("keywords_file")) {
                        keywordsFileGoogleDrive = params.get("keywords_file").toString();
                    }

                    if (params.containsKey("saved_feed_items_file")) {
                        savedFeedItemsFileGoogleDrive = params.get("saved_feed_items_file").toString();
                    }

                    final String deviceToken = settings.getDeviceToken();
                    uaDeviceToken = (null == deviceToken || deviceToken.isEmpty()) ? "undefined" : deviceToken;

                    showDeveloperInfo();
                    return;
                }
            }

            updateUi();
            if (SETTING_AUTH_TOKEN.equals(settingName) ||
                    SETTING_ACCESS_CODE.equals(settingName) ||
                    SETTING_CURRENT_SERVER.equals(settingName)) {
                Logger.d(TAG, "received settingChanged for " + settingName);
                showProgress(getString(R.string.refreshing_content));
            }
        }
    };

    private final NotificationProcessor feedsUpdateProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            if (null == getActivity() || getActivity().isFinishing()) {
                return;
            }

            Logger.d(TAG, "feedsUpdateProcessor");
            hideProgress();
        }
    };

    /**
     *       feature: google drive
     */

    private final NotificationProcessor keywordsDeviceRegisteredOnMcsProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            articleService.updateListOfSubscribedKeywords();
        }
    };

    private final NotificationProcessor keywordUpdateStartedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (null != progressMessageCenter) {
                progressMessageCenter.setVisibility(View.VISIBLE);
            }
        }
    };

    private final NotificationProcessor keywordUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            progressMessageCenter.setVisibility(View.GONE);

            updateUi();
        }
    };

    private final NotificationProcessor affiliationInfoUpdateProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            showAffiliationInfo();
        }
    };

    private NotificationProcessor issuesListUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            updateUi();
        }
    };

    private ViewTreeObserver.OnScrollChangedListener onScrollChangedListener;

    private void showSubscriptionCodeInfo() {
        AuthorizationService.AccessCodeInformation accessCodeInformation = authorizationService.getAccessCodeInformation();
        if (!settings.isAuthorized() || !authorizationService.hasAccessCode() || accessCodeInformation.expirationDate == null) {
            subscriptionInfo.setVisibility(View.GONE);
            subscriptionInfoDivider.setVisibility(View.GONE);
        } else {
            final SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

            stringBuilder.append("\nSubscription Code\n\n");
            stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, stringBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


            final String text;
            final ForegroundColorSpan expirationDateColor;
            if (accessCodeInformation.isExpired()) {
                text = "Your Subscription code access expired on ";
                expirationDateColor = new ForegroundColorSpan(Color.RED);
            } else {
                text = "Your Subscription code access will expire on ";
                expirationDateColor = new ForegroundColorSpan(Color.BLUE);
            }

            int oldPosition = stringBuilder.length();
            final SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            stringBuilder.append(text);
            stringBuilder.setSpan(new LeadingMarginSpan.Standard(40), oldPosition, stringBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            oldPosition = stringBuilder.length();
            stringBuilder.append(outputFormat.format(accessCodeInformation.expirationDate));
            stringBuilder.setSpan(expirationDateColor, oldPosition,
                    stringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            stringBuilder.append("\n");

            subscriptionInfo.setVisibility(View.VISIBLE);
            subscriptionInfoDivider.setVisibility(View.VISIBLE);
            subscriptionInfo.setText(stringBuilder);
        }
    }

    private void showAffiliationInfo() {
        final String affiliationInfo = settings.getAffiliationInfo();
        if (!settings.isAuthorized() || affiliationInfo == null || affiliationInfo.isEmpty()) {
            affiliationInfoView.setVisibility(View.GONE);
            affiliationInfoDivider.setVisibility(View.GONE);
            return;
        }

        try {
            final JSONArray organisations = new JSONObject(affiliationInfo).getJSONArray("organisations");
            if (organisations.length() == 0) {
                affiliationInfoView.setVisibility(View.GONE);
                affiliationInfoDivider.setVisibility(View.GONE);
                return;
            }

            final SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

            stringBuilder.append("\nMobile Affiliation\n\n");
            stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, stringBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new LeadingMarginSpan.Standard(8), 0, stringBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new RelativeSizeSpan(1.5f), 0, stringBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            for (int i = 0; i < organisations.length(); i++) {
                JSONObject organisation = organisations.getJSONObject(i);

                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

                final Date expirationDate = format.parse(organisation.getString("validTo"));

                final String text;
                final int expirationDatePosition;
                final ForegroundColorSpan expirationDateColor;
                if (new Date().before(expirationDate)) {
                    text = "Your institutional access will expire on the %s, please connect to your " +
                            "institutions network before this date to ensure continuity of your access.";
                    expirationDatePosition = 45;
                    expirationDateColor = new ForegroundColorSpan(Color.BLUE);
                } else {
                    text = "Your institutional access expired on the %s, " +
                            "please connect to your institutions network to renew your access.";
                    expirationDatePosition = 41;
                    expirationDateColor = new ForegroundColorSpan(Color.RED);
                }

                int oldPosition = stringBuilder.length();
                stringBuilder.append(organisation.getString("name")).append("\n");
                stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), oldPosition, stringBuilder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new RelativeSizeSpan(1.2f), oldPosition, stringBuilder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new LeadingMarginSpan.Standard(40), oldPosition, stringBuilder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                oldPosition = stringBuilder.length();
                final SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                stringBuilder.append(format(text, outputFormat.format(expirationDate)));
                stringBuilder.setSpan(new LeadingMarginSpan.Standard(40), oldPosition, stringBuilder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(expirationDateColor, oldPosition + expirationDatePosition,
                        oldPosition + expirationDatePosition + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            stringBuilder.append("\n");
            affiliationInfoView.setVisibility(View.VISIBLE);
            affiliationInfoDivider.setVisibility(View.VISIBLE);
            affiliationInfoView.setText(stringBuilder);
        } catch (JSONException | ParseException e) {
            Logger.s(TAG, "Unable to update affiliation info", e);
        }
    }

    private void showDeveloperInfo() {

        if (environment.isDebug()) {
            // feature: google drive
            ((TextView) findView(R.id.text_google_drive_connection)).setText("Google drive: " + connectionGoogleDrive);
            ((TextView) findView(R.id.text_google_drive_saved_articles_file)).setText("SA: " + savedArticlesFileGoogleDrive);
            ((TextView) findView(R.id.text_google_drive_read_articles_file)).setText("RA: " + readArticlesFileGoogleDrive);
            ((TextView) findView(R.id.text_google_drive_keywords_file)).setText("KW: " + keywordsFileGoogleDrive);
            ((TextView) findView(R.id.text_google_drive_saved_society_news_file)).setText("SN: " + savedFeedItemsFileGoogleDrive);
            ((TextView) findView(R.id.text_google_drive_ua_device_token)).setText("UA: " + uaDeviceToken);
        }
    }

    protected AccessCodeChecker.Listener accessCodeCheckerListener = new AccessCodeChecker.Listener() {
        @Override
        public void onStateChanged() {
            updateUi();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accessCodeChecker = new AccessCodeChecker(getActivity());
        accessCodeChecker.addListener(accessCodeCheckerListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessCodeChecker.removeListener(accessCodeCheckerListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationCenter.subscribeToNotification(SETTINGS_CHANGED.getEventName(), settingsChangedProcessor);
        notificationCenter.subscribeToNotification(ALL_CONTENT_UPDATE_FINISHED.getEventName(), feedsUpdateProcessor);
        notificationCenter.subscribeToNotification(EARLY_VIEW_FEED_ERROR.getEventName(), feedsUpdateProcessor);
        notificationCenter.subscribeToNotification(KEYWORDS_DEVICE_REGISTERED_ON_MCS.getEventName(), keywordsDeviceRegisteredOnMcsProcessor);
        notificationCenter.subscribeToNotification(KEYWORD_UPDATE_STARTED.getEventName(), keywordUpdateStartedProcessor);
        notificationCenter.subscribeToNotification(KEYWORD_UPDATE_FINISHED.getEventName(), keywordUpdateFinishedProcessor);
        notificationCenter.subscribeToNotification(AFFILIATION_INFO_CHANGED.getEventName(), affiliationInfoUpdateProcessor);
        notificationCenter.subscribeToNotification(ISSUE_LIST_UPDATED.getEventName(), issuesListUpdatedProcessor);

        initUi();
    }

    @Override
    public void onShow() {
        super.onShow();
        if (null != articleService) {
            articleService.updateListOfSubscribedKeywords();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        updateUi();
    }

    @Override
    public void onDestroyView() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(settingsChangedProcessor);
        notificationCenter.unSubscribeFromNotification(feedsUpdateProcessor);
        notificationCenter.unSubscribeFromNotification(keywordsDeviceRegisteredOnMcsProcessor);
        notificationCenter.unSubscribeFromNotification(keywordUpdateStartedProcessor);
        notificationCenter.unSubscribeFromNotification(keywordUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(affiliationInfoUpdateProcessor);
        notificationCenter.unSubscribeFromNotification(issuesListUpdatedProcessor);
    }

    @Override
    protected int getTabId() {
        return R.id.settings_tab;
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void initUi() {
        progress = findView(R.id.progress);
        progressInfo = findView(R.id.progress_info);
        progressInfo.setText(R.string.refreshing_content);
        progress.setVisibility(View.GONE);

        closeButton = findView(R.id.settings_close);
        if (closeButton != null) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeSettingWindow();
                }
            });
        }

        // layout 'Account'
        this.layoutAccount = findView(R.id.settings_layout_account);
        this.buttonGetAccess = findView(R.id.button_get_access);
        this.buttonLogout = findView(R.id.button_logout);
        this.labelFullAccess1 = findView(R.id.label_full_access_1);
        this.labelFullAccess2 = findView(R.id.label_full_access_2);
        this.labelNotFullAccess = findView(R.id.label_not_full_access);
        this.buttonRefreshAccount = findView(R.id.button_refresh_account);
        buttonRefreshAffiliationInfo = findView(R.id.button_refresh_affiliation_info);
        this.accessCodeGroup = findView(R.id.access_code_group);
        this.accessCodeText = findView(R.id.access_code_text);
        this.accessCodeStatus = findView(R.id.access_code_status);
        this.accessCodeProgress = findView(R.id.access_code_progress);
        this.accessCodeSubmit = findView(R.id.access_code_submit);
        affiliationInfoView = findView(R.id.mobile_affiliation);
        affiliationInfoDivider = findView(R.id.affiliation_info_divider);
        subscriptionInfo = findView(R.id.subscription_code_expipation);
        subscriptionInfoDivider = findView(R.id.subscription_code_expipation_divider);

        // layout 'App Preferences'
        this.layoutAppPreferences = findView(R.id.settings_layout_preferences);
        this.showAbstractLine = findView(R.id.show_abstract_line);
        this.showAbstractSwitch = findView(R.id.show_abstract_switch);
        this.fontSizeSeekBar = findView(R.id.font_size_seek_bar);
        this.fontSizeWebView = findView(R.id.font_size_web_view);

        // layout 'Restore Purchases'
        { // feature: in-app purchase
            this.layoutRestorePurchases = findView(R.id.settings_layout_restore_purchases);
            this.groupRestorePurchases = findView(R.id.settings_group_restore_purchases);
            this.buttonRestorePurchases = findView(R.id.button_restore_purchases);
        }

        // layout 'Message Center'
        this.layoutMessageCenter = findView(R.id.settings_layout_message_center);
        this.emptyMessageCenter = findView(R.id.settings_empty_message_center);
        this.keywordsMessageCenter = findView(R.id.settings_keywords_message_center);
        this.progressMessageCenter = findView(R.id.progress_message_center);

        // layout 'Developer'
        this.layoutDeveloper = findView(R.id.settings_layout_developer);
        this.headerDeveloper = findView(R.id.settings_header_developer);
        this.groupDeveloper = findView(R.id.settings_group_developer);
        this.buttonChangeServer = findView(R.id.button_change_server);
        this.buttonChangeOAuth = findView(R.id.button_change_oauth);
        debugIpAddress = findView(R.id.debug_ip);

        // account
        String fullAccess2Html = getString(R.string.settings_access_full_text_2, theme.getHelpUrl());
        labelFullAccess2.setText(webController.assignSpannableUrlInternalHandlers(Html.fromHtml(fullAccess2Html)));
        labelFullAccess2.setMovementMethod(LinkMovementMethod.getInstance());

        String notFullAccessHtml = getString(R.string.settings_access_not_full_text, theme.getJournalName(), theme.getHelpUrl());
        labelNotFullAccess.setText(webController.assignSpannableUrlInternalHandlers(Html.fromHtml(notFullAccessHtml)));
        labelNotFullAccess.setMovementMethod(LinkMovementMethod.getInstance());

        if (showAbstractSwitch != null) {
            showAbstractSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    settings.changeArticleShowAbstract(b);
                }
            });
        }

        initFontSizeWebView();

        // account
        buttonGetAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (NetUtils.isOnline(getActivity())) {
                    authorizer.requestAccess(getActivity());
                } else {
                    errorManager.alertWithErrorCode(getActivity(), NO_CONNECTION_AVAILABLE);
                }
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                {
                    aanHelper.trackActionLogout();
                }
                authorizationService.clearAuthToken();
            }
        });

        buttonRefreshAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                {
                    aanHelper.trackActionRefreshAccount();
                }
                authorizer.refresh();
            }
        });

        buttonRefreshAffiliationInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aanHelper.trackActionRefreshInstitutionalAccess();
                notificationCenter.sendNotification(EventList.AFFILIATION_INFO_NEED_UPDATE.getEventName());
            }
        });

        // 'Restore All Purchases' button

        { // feature: in-app purchase
            buttonRestorePurchases.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!DeviceUtils.isInternetConnectionAvailable(getActivity())) {
                        errorManager.alertWithErrorCode(getActivity(), NO_CONNECTION_AVAILABLE);
                    } else {
                        aanHelper.trackActionRestoreAllPurchases();
                        UpdateOperationActivity.startRestoreAllPurchases(getJournalActivity());
                    }
                }
            });
        }

        setChangeServerButtonTitle();
        setChangeOAuthButtonTitle();

        // developer
        buttonChangeServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeDebugServer();
            }
        });

        buttonChangeOAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeDebugOAuthLink();
            }
        });

        accessCodeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!authorizationService.hasAccessCode()) {
                    accessCodeChecker.requestAccessCodeCheck(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        accessCodeSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAccessCode(accessCodeText.getText().toString());
            }
        });

        debugIpAddress.setText(settings.getDebugIp());
        debugIpAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                settings.setDebugIp(s.toString());
            }
        });
    }

    private void initFontSizeWebView() {
        fontSizeWebView.getSettings().setJavaScriptEnabled(true);
        fontSizeWebView.getSettings().setSupportZoom(false);

        final String articleTitleText;
        final String articleBodyText;
        if (DeviceUtils.isPhone(getActivity())) {
            articleTitleText = "Article Title";
            articleBodyText = "Article body text";
        } else {
            articleTitleText = "Article Title Looks Like This";
            articleBodyText = "Article body text looks like this";
        }

        String html = format(
                "<html><head><meta name=\"viewport\" content=\"width=100%%; initial-scale=1.00; maximum-scale=1.00;" +
                        " user-scalable=0;\" />" +
                        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
                        "<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" /><style type=\"text/css\"> " +
                        "body { " +
                        "word-wrap:break-word; font-family:\"Helvetica Neue Light\", \"Helvetica Neue\", Helvetica, " +
                        "Arial, sans-serif; font-size:%dpx; color:#333; }</style></head><body id=\"font_size_web_view\">" +
                        "<div style=\"font-size:%s; font-weight:bold;\" align=\"center\">%s" +
                        "</div>" +
                        "<div style=\"font-family:Times New Roman;\" align=\"center\">" +
                        "%s" +
                        "</div></body></html>",
                Settings.ARTICLE_FONT_SIZE_MIN, "120%", articleTitleText, articleBodyText);
        fontSizeWebView.loadData(html, "text/html", "UTF-8");
        fontSizeWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateUi();
                UIUtils.refreshWebView(fontSizeWebView, getActivity());
            }
        });

        fontSizeWebView.addJavascriptInterface(new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                UIUtils.refreshWebView(fontSizeWebView, getActivity());
            }
        }, "hostCallbacks");


        if (DeviceUtils.isPhone(getActivity())) {
            final ScrollView scrollView = findView(R.id.settingsScrollView);

            onScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {

                @Override
                public void onScrollChanged() {
                    UIUtils.refreshWebView(fontSizeWebView, getActivity());
                }
            };

            scrollView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (observer == null) {
                        observer = scrollView.getViewTreeObserver();
                        observer.addOnScrollChangedListener(onScrollChangedListener);
                    } else if (!observer.isAlive()) {
                        observer.removeOnScrollChangedListener(onScrollChangedListener);
                        observer = scrollView.getViewTreeObserver();
                        observer.addOnScrollChangedListener(onScrollChangedListener);
                    }

                    return false;
                }
            });
        }

        fontSizeSeekBar.setMax(Settings.ARTICLE_FONT_SIZE_MAX - Settings.ARTICLE_FONT_SIZE_MIN);
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int newFontSize = Settings.ARTICLE_FONT_SIZE_MIN + i;
                settings.changeArticleFontSize(newFontSize);
                changeFontSizeSeekBarValue(newFontSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        if (DeviceUtils.isPhone(getActivity())) {
            closeSettingWindow();
            return true;
        }

        return super.onBackPressed();
    }

    private void closeSettingWindow() {
        MainActivity activity = (MainActivity) getActivity();
        if (observer != null && onScrollChangedListener != null) {
            observer.removeOnScrollChangedListener(onScrollChangedListener);
        }
        FragmentManager supportFragmentManager = getActivity().getSupportFragmentManager();
        supportFragmentManager.beginTransaction().remove(this).commit();
        supportFragmentManager.executePendingTransactions();
        activity.onSettingWindowClosed();
    }

    private void setChangeOAuthButtonTitle() {
        final String debugOAuthLink = settings.getDebugOAuthLink();
        buttonChangeOAuth.setText(format("OAuth link: %s", debugOAuthLink));
    }

    private void setChangeServerButtonTitle() {
        final String currentServer = settings.getCurrentServer();
        buttonChangeServer.setText(format("Current server: %s", currentServer));
    }

    protected void updateUi() {
        final boolean isAuthorized = settings.isAuthorized();
        final boolean isFullAccess = settings.isFullAccess();
        // layout 'Account'
        if (isAuthorized && isFullAccess) {
            labelFullAccess1.setVisibility(View.VISIBLE);
            labelFullAccess2.setVisibility(View.VISIBLE);
        } else {
            labelFullAccess1.setVisibility(View.GONE);
            labelFullAccess2.setVisibility(View.GONE);
        }

        showSubscriptionCodeInfo();
        showAffiliationInfo();
        showDeveloperInfo();

        buttonGetAccess.setVisibility(isAuthorized ? View.GONE : View.VISIBLE);
        buttonLogout.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);
        buttonRefreshAffiliationInfo.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);

        labelNotFullAccess.setVisibility(isAuthorized && !isFullAccess ? View.VISIBLE : View.GONE);
        buttonRefreshAccount.setVisibility(isAuthorized && !isFullAccess ? View.VISIBLE : View.GONE);

        if (!isAuthorized) {
            accessCodeGroup.setVisibility(View.GONE);
        } else {
            accessCodeGroup.setVisibility(View.VISIBLE);
            if (authorizationService.hasAccessCode()) {
                accessCodeText.setText(authorizationService.getAccessCodeInformation().code);
                accessCodeText.setBackgroundResource(R.drawable.grey_edit_text_selector);
                accessCodeProgress.setVisibility(View.GONE);
                accessCodeStatus.setVisibility(View.VISIBLE);
                accessCodeStatus.setImageResource(R.drawable.status_checkmark);
            } else {
                boolean isValid = accessCodeChecker.getState() == AccessCodeChecker.State.Valid;
                boolean isProgress = accessCodeChecker.getState() == AccessCodeChecker.State.Progress;
                boolean isNone = accessCodeChecker.getState() == AccessCodeChecker.State.None;
                accessCodeText.setBackgroundResource(isValid || isNone ? R.drawable.grey_edit_text_selector : R.drawable.red_edit_text_selector);
                accessCodeText.setAlpha(1.0f);
                accessCodeText.setEnabled(true);
                accessCodeProgress.setVisibility(isProgress ? View.VISIBLE : View.GONE);
                if (isNone) {
                    accessCodeStatus.setVisibility(View.INVISIBLE);
                } else {
                    accessCodeStatus.setVisibility(isProgress ? View.GONE : View.VISIBLE);
                    accessCodeStatus.setImageResource(isValid ? R.drawable.status_checkmark : R.drawable.status_crossmark);
                }
                accessCodeSubmit.setVisibility(View.VISIBLE);
                accessCodeSubmit.setEnabled(isValid);
            }
        }

        if (settings.hasSubscriptionReceipt() || theme.isOpenAccessJournal()) {
            layoutAccount.setVisibility(View.GONE);
        } else {
            layoutAccount.setVisibility(View.VISIBLE);
        }

        updateAppPreferencesLayout();
        updateRestorePurchasesLayout(isAuthorized, isFullAccess);
        updateMessageCenterLayout();
        updateDeveloperLayout();
    }

    private void updateDeveloperLayout() {
        headerDeveloper.setVisibility(environment.isDebug() ? View.VISIBLE : View.GONE);
        groupDeveloper.setVisibility(environment.isDebug() ? View.VISIBLE : View.GONE);
    }

    private void updateAppPreferencesLayout() {
        if (DeviceUtils.isTablet(getActivity())) {
            showAbstractLine.setVisibility(theme.isJournalHasNoTextAbstracts() ? View.GONE : View.VISIBLE);
            showAbstractSwitch.setChecked(settings.getArticleShowAbstract());
        }

        final int fontSize;
        if (settings.hasArticleFontSize()) {
            fontSize = settings.getArticleFontSize();
        } else {
            fontSize = theme.getDefaultArticleFontSize();
        }
        changeFontSizeSeekBarValue(fontSize);
    }

    private void updateRestorePurchasesLayout(boolean isAuthorized, boolean isFullAccess) {
        layoutRestorePurchases.setVisibility(!theme.isEnableSubscription() || (isAuthorized && isFullAccess) ? View.GONE : View.VISIBLE);
    }

    private void updateMessageCenterLayout() {
        if (settings.getKeywords().size() > 0) {
            emptyMessageCenter.setVisibility(View.GONE);
            setKeywordListView(this.getActivity(), keywordsMessageCenter);
            keywordsMessageCenter.setVisibility(View.VISIBLE);
            findView(R.id.message_center_divider).setVisibility(View.VISIBLE);
            findView(R.id.message_center_alert).setVisibility(View.VISIBLE);
        } else {
            emptyMessageCenter.setVisibility(View.VISIBLE);
            keywordsMessageCenter.setVisibility(View.GONE);
            findView(R.id.message_center_divider).setVisibility(View.GONE);
            findView(R.id.message_center_alert).setVisibility(View.GONE);
        }
    }

    protected void changeFontSizeSeekBarValue(final int value) {
        fontSizeSeekBar.setProgress(value - Settings.ARTICLE_FONT_SIZE_MIN);
        fontSizeWebView.loadUrl(format("javascript:(function() {window.document.getElementById('font_size_web_view').style.fontSize='%dpx';hostCallbacks.onJavaScriptResult('');})()", value));
    }

    protected void changeDebugServer() {
        assert (environment.isDebug());
        final String selectedServer = settings.getCurrentServer();

        SelectStringDialog.show(getActivity(),
                getString(R.string.settings_button_change_server),
                environment.getDebugServers(), selectedServer,
                new SelectStringDialog.Listener() {
                    @Override
                    public void onDialogStringSelected(String result) {
                        settings.changeCurrentServer(result);
                        setChangeServerButtonTitle();
                    }
                });
    }

    protected void changeDebugOAuthLink() {
        assert (environment.isDebug());

        String selectedOAuth = settings.getDebugOAuthLink();
        if (TextUtils.isEmpty(selectedOAuth)) {
            selectedOAuth = environment.getDebugOAuthLinks()[0];
        }

        SelectStringDialog.show(getActivity(),
                getString(R.string.settings_button_change_oauth),
                environment.getDebugOAuthLinks(), selectedOAuth,
                new SelectStringDialog.Listener() {
                    @Override
                    public void onDialogStringSelected(String result) {
                        settings.changeDebugOAuthLink(result);
                        setChangeOAuthButtonTitle();
                    }
                });
    }

    protected void submitAccessCode(final String accessCode) {
        showProgress(getString(R.string.progress_submit_code));
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                DownloadManager.JsonResponse jsonResponse = authorizationService.useAccessCode(accessCode);
                return jsonResponse.errorMessage;
            }

            @Override
            protected void onPostExecute(String errorMessage) {
                super.onPostExecute(errorMessage);

                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }

                updateUi();

                if (!TextUtils.isEmpty(errorMessage)) {
                    AlertDialogActivity.show(getActivity(),
                            getActivity().getString(R.string.submit_code_error_title),
                            errorMessage);
                }

                hideProgress();
            }
        };
        task.execute();
    }

    private Handler hideProgressHandler = new Handler();

    protected void showProgress(String info) {
        changeProgressInfo(info);
        showProgress();
    }

    protected void showProgress() {
        if (progress == null) {
            return;
        }
        Logger.d(SettingsFragment.class.getSimpleName(), "showProgress");
        hideProgressHandler.removeCallbacksAndMessages(null);
        progress.setVisibility(View.VISIBLE);
    }

    protected void changeProgressInfo(String info) {
        if (progressInfo == null) {
            return;
        }
        progressInfo.setText(info);
    }

    protected void hideProgress() {
        if (progress == null) {
            return;
        }
        Logger.d(SettingsFragment.class.getSimpleName(), "hideProgress");
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
                        progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                fadeOut.setDuration(500);
                progress.startAnimation(fadeOut);
            }
        }, 200);
    }

    private void setKeywordListView(final Activity activity, final LinearLayout layout) {

        layout.removeAllViews();

        List<String> keywords = settings.getKeywords();
        int index = 0;
        for (String keyword : keywords) {

            // set keyword
            Switch aSwitch = new Switch(activity);
            aSwitch.setChecked(true);
            aSwitch.setText(keyword);
            aSwitch.setTextOff("   ");
            aSwitch.setTextOn("   ");
            aSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Switch view = (Switch) v;
                    final String keyword = view.getText().toString();
                    if (!view.isChecked()) {
                        new AlertDialog.Builder(activity)
                                .setTitle(activity.getString(R.string.article_info_delete_keyword_title))
                                .setMessage(format(activity.getString(R.string.article_info_delete_keyword_text), keyword))
                                .setCancelable(true)
                                .setNegativeButton(R.string.alert_button_no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        view.setChecked(true);
                                    }
                                })
                                .setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        aanHelper.trackActionUnsubscribeFromKeyword(keyword, null);
                                        articleService.changeKeyword(keyword, ArticleService.ACTION_UNSUBSCRIBE);
                                    }
                                })
                                .show();
                    }
                }
            });

            aSwitch.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            layout.addView(aSwitch);

            // set divider
            if (index < keywords.size() - 1) {
                View divider = new View(activity);
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.GRAY);
                layout.addView(divider);
            }
            index++;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            final int fontSize;
            if (settings.hasArticleFontSize()) {
                fontSize = settings.getArticleFontSize();
            } else {
                fontSize = theme.getDefaultArticleFontSize();
            }
            changeFontSizeSeekBarValue(fontSize);
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        Activity activity = getActivity();
        if (null != activity) {
            UIUtils.hideSoftInput(activity);
        }
    }
}
