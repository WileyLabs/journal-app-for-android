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

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.http.DownloadManager;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.wiley.wol.client.android.error.AppErrorCode.TPS_NO_ACCESS;
import static com.wiley.wol.client.android.error.AppErrorCode.TPS_NO_RESPONSE;
import static com.wiley.wol.client.android.error.AppErrorCode.TPS_UNKNOWN_RESPONSE;
import static com.wiley.wol.client.android.error.AppErrorCode.TPS_WRONG_PASSWORD;
import static com.wiley.wol.client.android.utils.EncryptionUtils.encryptKey;
import static java.lang.String.format;

/**
 * Created by taraskreknin on 04.08.14.
 */
public class TPSLoginFragment extends AbstractScreenFragment implements View.OnClickListener {

    private static final String RESULT_WAITING = "1";
    private static final String RESULT_OK = "0";
    private static final String RESULT_NO_ACCESS = "2";
    private static final String RESULT_INVALID_FORM = "-1";
    private static final String RESULT_INVALID_LOGIN_PASS = "-2";
    private static final String RESULT_UNKNOWN_ERROR = "-10";
    private static final int NO_RESPONSE_ERROR_CODE = -1;

    private enum LoginState {
        LoadingForm,
        LoggingIn,
        None
    }

    private static final String TAG = TPSLoginFragment.class.getSimpleName();

    private EditText mLoginFieldView, mPasswordFieldView;
    private Button mLoginButtonView;
    private CustomWebView mHiddenWebView;

    private boolean mIsSelectedNonTPSSociety;
    private LoginState mState = LoginState.None;
    private String mUrl;
    private int pageLoadingState;
    private boolean timeoutExpired;

    @Inject
    private DownloadManager downloadManager;

    private NotificationProcessor mAuthFailedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_OAUTH_FAILED);
            getAccessDialogFragment().hideProgress();
        }
    };

    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Logger.d(TAG, "onPageStarted " + url);
            synchronized (this) {
                ++pageLoadingState;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Logger.d(TAG, "onPageFinished " + url);

            synchronized (this) {
                ++pageLoadingState;
            }

            TPSLoginFragment.this.onPageFinished(url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Logger.d(TAG, "onPageError " + failingUrl);
            mErrorManager.alertWithErrorCode(getActivity(), TPS_NO_RESPONSE);
            stopAndClearWebView();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_tps_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoginFieldView = findView(R.id.get_access_tps_login_field);
        mPasswordFieldView = findView(R.id.get_access_tps_password_field);
        mLoginButtonView = findView(R.id.get_access_tps_log_in);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        mHiddenWebView = findView(R.id.get_access_hidden_web_view);
        mHiddenWebView.getSettings().setSaveFormData(false);
        mHiddenWebView.getSettings().setSavePassword(false);
        mHiddenWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mHiddenWebView.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_3) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65");
        mHiddenWebView.setWebViewClient(mWebViewClient);
        updateUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mState != LoginState.None) {
            getAccessDialogFragment().showProgress();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mNotificationCenter.subscribeToNotification(EventList.AUTH_ERROR.getEventName(), mAuthFailedProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        mNotificationCenter.unSubscribeFromNotification(mAuthFailedProcessor);
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    public void updateUi() {
        final TPSSiteMO site = getAccessDialogFragment().getSelectedSite();

        /*
         *   Ios implementation in TPSAuthLoginViewController updateStateForSite
         *       if (_currTPSSite.formUrl == nil || _currTPSSite.loginScript == nil || _currTPSSite.responseScript == nil) {
         *           _isSelectedNonTPSSociety = YES;
         *       }
         */
        mIsSelectedNonTPSSociety = TextUtils.isEmpty(site.getFormUrl())
                || site.getLoginScript() == null
                || site.getResponseScript() == null;

        final TextView siteName = findView(R.id.get_access_tps_site_name);
        siteName.setText(site.getTPSName());
        final TextView siteAddr = findView(R.id.get_access_tps_site_address);
        siteAddr.setText(site.getTPSSiteUrl());
        if (!TextUtils.isEmpty(site.getInstructions())) {
            final TextView instructions = findView(R.id.get_access_tps_instructions);
            instructions.setText(Html.fromHtml(site.getInstructions()));
        }

        int fieldsVisibility = mIsSelectedNonTPSSociety ? View.GONE : View.VISIBLE;

        mLoginFieldView.setHint(TextUtils.isEmpty(site.getUsernameLabel()) ? getString(R.string.username) : site.getUsernameLabel());
        mLoginFieldView.setVisibility(fieldsVisibility);

        mPasswordFieldView.setHint(TextUtils.isEmpty(site.getPasswordLabel()) ? getString(R.string.password) : site.getPasswordLabel());
        mPasswordFieldView.setVisibility(fieldsVisibility);

        mLoginButtonView.setOnClickListener(this);

        clearWebView();
    }

    private void clearWebView() {
        pageLoadingState = 0;
        mHiddenWebView.clearCache(true);
        mHiddenWebView.clearHistory();
    }

    @Override
    public void onClick(View v) {
        if (mIsSelectedNonTPSSociety) {
            onNeedWolLogIn();
        } else {
            onNeedTpsLogin();
        }
    }

    private void onNeedTpsLogin() {
        UIUtils.hideSoftInput(getActivity());
        boolean loginIsEmpty = TextUtils.isEmpty(mLoginFieldView.getText().toString());
        boolean passIsEmpty = TextUtils.isEmpty(mPasswordFieldView.getText().toString());
        if (loginIsEmpty || passIsEmpty) {
            mErrorManager.alertWithErrorMessage(getActivity(), ErrorMessage.withTitleAndMessage(
                    getString(R.string.please_enter_login_and_pass),
                    getString(R.string.login_and_pass_cannot_be_empty)));
            return;
        }
        Logger.d(TAG, "Start TPS login process");
        GANHelper.trackEvent(GANHelper.EVENT_GET_ACCESS_C, GANHelper.ACTION_BUTTON, GANHelper.LABEL_LOGIN, 0L);
        loadLoginForm();
    }

//    login: 30130
//    passw: Diamond
//    Просьба быть поаккуратнее с этим паролем, нам его сообщили в виде исключения, по большому секрету :)
    private void loadLoginForm() {
        Logger.d(TAG, "loadLoginForm...");

        final TPSSiteMO site = getAccessDialogFragment().getSelectedSite();
        String loginScript = site.getLoginScript().trim();
        if (loginScript.contains("function(")) {
            Logger.d(TAG, "TPS Login: using JScript");
            loginUsingJScripts(site);
        } else {
            if (site.getResponseScript() == null || site.getResponseScript().trim().length() == 0) {
                Logger.d(TAG, "TPS Login: using Basic Authorization");
                loginUsingBasicAuthorization(site);
            } else {
                Logger.d(TAG, "TPS Login: using REST API");
                loginUsingRESTWebService(site);
            }
        }
    }

    private void loginUsingBasicAuthorization(final TPSSiteMO site) {
        final String encodedAuthInfo = new String(Base64.encodeBase64(format("%s:%s",
                mLoginFieldView.getText().toString(), mPasswordFieldView.getText().toString()).getBytes()));

        final List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Authorization", format("Basic %s", encodedAuthInfo)));

        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                getAccessDialogFragment().showProgress();
            }

            @Override
            protected Integer doInBackground(final Void... params) {
                try {
                    final HttpResponse httpResponse = downloadManager.connectTo(site.getFormUrl(), headers);
                    return downloadManager.getStatusCodeFor(httpResponse);
                } catch (IOException e) {
                    Logger.s(TAG, e);
                    return NO_RESPONSE_ERROR_CODE;
                }
            }

            @Override
            protected void onPostExecute(final Integer statusCode) {
                super.onPostExecute(statusCode);
                getAccessDialogFragment().hideProgress();
                if (statusCode == DownloadManager.OK_CODE) {
                    getAccessDialogFragment().doOAuthLoginWithTpsCredentials();
                } else if (statusCode == DownloadManager.UNAUTHORIZED) {
                    mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_WRONG_PASSWORD);
                } else if (statusCode == NO_RESPONSE_ERROR_CODE) {
                    mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_NO_RESPONSE);
                } else {
                    mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_UNKNOWN_RESPONSE);
                }
            }
        }.execute();

    }

    private void loginUsingRESTWebService(TPSSiteMO site) {
        List<Header> headers = new ArrayList<>();

        String url = site.getFormUrl();
        if (url.toLowerCase().contains("http://spa-as-") || url.toLowerCase().contains("http://contentserver.")) {
            url = url.replaceAll("\\{appPrefix\\}", theme.getAppPrefix());
            final String encryptedLoginPass = encryptKey(format("%s::%s", mLoginFieldView.getText().toString(), mPasswordFieldView.getText().toString()), getString(R.string.recipe));
            url = format("%s?signature=%s&society=%s", url, encryptedLoginPass, site.getTPSShortName());
        } else {
            url = url.replaceAll("\\{username\\}", mLoginFieldView.getText().toString())
                    .replaceAll("\\{password\\}", mPasswordFieldView.getText().toString())
                    .replaceAll("&amp;", "&");
        }

        String body = "";
        String contentType = "application/x-www-form-urlencoded";
        String loginScript = site.getLoginScript().trim();
        if (!loginScript.isEmpty()) {
            JSONObject loginDataJson;
            try {
                loginDataJson = new JSONObject(loginScript);
            } catch (JSONException e) {
                loginDataJson = new JSONObject();
            }

            JSONObject loginJson = getJsonObject(loginDataJson, "json");
            JSONObject loginBasic = getJsonObject(loginDataJson, "basic");

            if (loginJson != null) {
                contentType = "application/json";
                replaceLoginAndPasswordPlaceholders(loginJson);
                body = loginJson.toString();
            } else if (loginBasic != null) {
                String login = getStringFromJson(loginBasic, "Login");
                String password = getStringFromJson(loginBasic, "Password");
                if (!TextUtils.isEmpty(login) && !TextUtils.isEmpty(password)) {
                    String encodedAuthInfo = new String(Base64.encodeBase64(format("%s:%s", login, password).getBytes()));
                    headers.add(new BasicHeader("Authorization", format("Basic %s", encodedAuthInfo)));
                }

                String loginBasicContentType = getStringFromJson(loginBasic, "ContentType");
                if (!TextUtils.isEmpty(loginBasicContentType)) {
                    contentType = loginBasicContentType;
                }

                String data = getStringFromJson(loginBasic, "Data");
                if (!TextUtils.isEmpty(data)) {
                    body = data.replaceAll("\\{username\\}", mLoginFieldView.getText().toString())
                            .replaceAll("\\{password\\}", mPasswordFieldView.getText().toString());
                }
            } else {
                final StringBuilder params = new StringBuilder();
                Iterator keys = loginDataJson.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    params.append(format("%s%s=%s", params.length() > 0 ? "&" : "", key, getStringFromJson(loginDataJson, key)));
                }

                body = params.toString().replaceAll("\\{username\\}", mLoginFieldView.getText().toString())
                        .replaceAll("\\{password\\}", mPasswordFieldView.getText().toString());
            }
        }

        executePostRequest(site, url, contentType, body, headers);
    }

    private void executePostRequest(final TPSSiteMO site, final String url, final String contentType, final String body, final List<Header> headers) {
        new AsyncTask<Void, Void, HttpURLConnection>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                getAccessDialogFragment().showProgress();
            }

            @Override
            protected HttpURLConnection doInBackground(final Void... params) {
                try {
                    return executePostRequest(url, contentType, body, headers);
                } catch (Exception e) {
                    Logger.s(TAG, e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final HttpURLConnection connection) {
                super.onPostExecute(connection);
                getAccessDialogFragment().hideProgress();
                if (connection == null) {
                    mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_NO_RESPONSE);
                } else {
                    try {
                        int statusCode = connection.getResponseCode();
                        if (statusCode == DownloadManager.OK_CODE) {
                            processRestWebServiceResponse(connection.getInputStream(), site);
                        } else {
                            mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_UNKNOWN_RESPONSE);
                        }
                    } catch (IOException e) {
                        Logger.s(TAG, e);
                        mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_UNKNOWN_RESPONSE);
                    }
                }
            }
        }.execute();
    }

    private void processRestWebServiceResponse(final InputStream response, final TPSSiteMO site) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(final Void... params) {
                try {
                    return IOUtils.toString(response);
                } catch (IOException e) {
                    Logger.s(TAG, e);
                    return "";
                }
            }

            @Override
            protected void onPostExecute(final String responseString) {
                super.onPostExecute(responseString);
                if (responseString.isEmpty()) {
                    mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_UNKNOWN_RESPONSE);
                } else {
                    JSONObject checkLogin = null;
                    try {
                        checkLogin = new JSONObject(site.getResponseScript());
                    } catch (Exception e) {
                        Logger.s(TAG, e);
                    }

                    if (checkLogin != null) {
                        String checkLoginResult = getStringFromJson(checkLogin, responseString);
                        if ("No_Content_Access".equals(checkLoginResult)) {
                            mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_NO_ACCESS);
                        } else if ("Full_Content_Access".equals(checkLoginResult)) {
                            getAccessDialogFragment().doOAuthLoginWithTpsCredentials();
                        } else if ("Wrong_Login_Pass".equals(checkLoginResult)) {
                            mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_WRONG_PASSWORD);
                        } else {
                            mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_UNKNOWN_RESPONSE);
                        }
                    } else {
                        mErrorManager.alertWithErrorCode(getActivity(), AppErrorCode.TPS_UNKNOWN_RESPONSE);
                    }
                }
            }
        }.execute();
    }

    private HttpURLConnection executePostRequest(final String url, String contentType, final String body, final List<Header> headers) throws IOException {
        HttpURLConnection connection= (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", Integer.toString(body.length()));

        for (Header header : headers) {
            connection.setRequestProperty(header.getName(), header.getValue());
        }

        connection.setUseCaches(false);

        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream( connection.getOutputStream());
            wr.write(body.getBytes());
            wr.flush();
        } finally {
            IOUtils.closeQuietly(wr);
        }

        connection.getResponseCode();
        return connection;
    }

    private String getStringFromJson(final JSONObject jsonObject, final String key) {
        String result = null;
        try {
            result = jsonObject.getString(key);
        } catch (JSONException e) {
            Logger.s(TAG, e);
        }
        return result;
    }

    private JSONObject getJsonObject(final JSONObject jsonObject, final String key) {
        JSONObject result = null;
        try {
            result = jsonObject.getJSONObject(key);
        } catch (JSONException e) {
            Logger.s(TAG, e);
        }
        return result;
    }

    private void replaceLoginAndPasswordPlaceholders(final JSONObject loginDataJson) {
        try {
            Iterator keysIterator = loginDataJson.keys();
            String userNameKey = null;
            String passwordKey = null;
            while (keysIterator.hasNext()) {
                String key = (String) keysIterator.next();
                if (loginDataJson.get(key).equals("{username}")) {
                    userNameKey = key;
                } else if (loginDataJson.get(key).equals("{password}")) {
                    passwordKey = key;
                }
            }
            if (userNameKey != null) {
                loginDataJson.put(userNameKey, mLoginFieldView.getText().toString());
            }
            if (passwordKey != null) {
                loginDataJson.put(passwordKey, mPasswordFieldView.getText().toString());
            }
        } catch (JSONException e) {
            Logger.s(TAG, e);
        }
    }

    private void loginUsingJScripts(final TPSSiteMO site) {
        mState = LoginState.LoadingForm;
        getAccessDialogFragment().showProgress();
        clearWebView();
        mHiddenWebView.loadUrl(site.getFormUrl());
    }

    private void doTpsLogin() {
        Logger.d(TAG, "trying to log in...");

        mState = LoginState.LoggingIn;

        final TPSSiteMO site = getAccessDialogFragment().getSelectedSite();
        final String login = mLoginFieldView.getText().toString();
        final String password = mPasswordFieldView.getText().toString();
        final String jsInjectLoginScript = "var func = " + site.getLoginScript() + ";";
        final String jsExecLoginScript = format("return func('%s', '%s')", login, password);

        timeoutExpired = false;
        final Handler errorTimeoutHandler = new Handler();
        final Runnable errorTimeoutCallback = new Runnable() {
            @Override
            public void run() {
                timeoutExpired = true;
                onJsResult(RESULT_UNKNOWN_ERROR);
            }
        };
        errorTimeoutHandler.postDelayed(errorTimeoutCallback, 3 * 60 * 1000);

        final int state = pageLoadingState;
        mHiddenWebView.executeJavaScriptAndGetResult(jsInjectLoginScript, jsExecLoginScript, new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                if (!timeoutExpired) {
                    errorTimeoutHandler.removeCallbacks(errorTimeoutCallback);
                    if (RESULT_OK.equals(result)) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Cancel waiting for result - most likely wrong login and/or password
                                if (state == pageLoadingState) {
                                    onJsResult(RESULT_INVALID_LOGIN_PASS);
                                }
                            }
                        }, 15000);
                    } else if (RESULT_INVALID_FORM.equalsIgnoreCase(result)) {

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (state == pageLoadingState) {
                                    onJsResult(RESULT_INVALID_FORM);
                                }
                            }
                        }, 2000);
                    } else {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (state == pageLoadingState) {
                                    onJsResult(RESULT_UNKNOWN_ERROR);
                                }
                            }
                        }, 2000);
                    }
                }
            }
        });

    }

    private void doCheckTpsLogin() {
        Logger.d(TAG, "checking login...");

        final TPSSiteMO site = getAccessDialogFragment().getSelectedSite();
        final String jsInjectCheckScript = "var func = " + site.getResponseScript() + ";";
        final String jsExecCheckScript = "return func();";

        timeoutExpired = false;
        final Handler errorTimeoutHandler = new Handler();
        final Runnable errorTimeoutCallback = new Runnable() {
            @Override
            public void run() {
                timeoutExpired = true;
                onJsResult(RESULT_UNKNOWN_ERROR);
            }
        };
        errorTimeoutHandler.postDelayed(errorTimeoutCallback, 3 * 60 * 1000);

        mHiddenWebView.executeJavaScriptAndGetResult(jsInjectCheckScript, jsExecCheckScript, new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                if (!timeoutExpired) {
                    errorTimeoutHandler.removeCallbacks(errorTimeoutCallback);
                    Logger.d(TAG, "check result " + result);
                    onJsResult(result);
                }
            }
        });
    }

    private void onPageFinished(String url) {
        Logger.d(TAG, "state " + mState.name());
        mUrl = url;
        switch (mState) {
            case LoadingForm:
                doTpsLogin();
                break;
            case LoggingIn:
                doCheckTpsLogin();
                break;
        }
    }

    private void onJsResult(String resultCode) {
        switch (resultCode) {
            case RESULT_NO_ACCESS:
                mErrorManager.alertWithErrorCode(getActivity(), TPS_NO_ACCESS);
                stopAndClearWebView();
                break;
            case RESULT_INVALID_LOGIN_PASS:
                GANHelper.trackEventWithTPSUrl(GANHelper.EVENT_GET_ACCESS_TPS_LOGIN_FAIL, GANHelper.ACTION_UI_BUTTON, GANHelper.LABEL__CLOSE, 0L, mUrl);
                mErrorManager.alertWithErrorCode(getActivity(), TPS_WRONG_PASSWORD);
                stopAndClearWebView();
                break;
            case RESULT_OK:
                mState = LoginState.None;
                getAccessDialogFragment().hideProgress();
                getAccessDialogFragment().doOAuthLoginWithTpsCredentials();
                break;
            case RESULT_WAITING:
                break;
            case RESULT_INVALID_FORM:
            case RESULT_UNKNOWN_ERROR:
            default:
                final int state = pageLoadingState;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (state == pageLoadingState) {
                            GANHelper.trackEventWithTPSUrl(GANHelper.EVENT_GET_ACCESS_TPS_SOCIETY_UNKNOWN_ERROR, GANHelper.ACTION_UI_BUTTON, GANHelper.LABEL__CLOSE, 0L, mUrl);
                            mErrorManager.alertWithErrorCode(getActivity(), TPS_UNKNOWN_RESPONSE);
                            stopAndClearWebView();
                        }
                    }
                }, 2000);
                break;
        }
    }

    private void stopAndClearWebView() {
        mState = LoginState.None;
        mHiddenWebView.stopLoading();
        clearWebView();
        getAccessDialogFragment().hideProgress();
    }

    @Override
    public void goBack() {
        if (mState != LoginState.None) {
            stopAndClearWebView();
        } else {
            super.goBack();
        }
    }

    @Override
    protected void openPreviousScreen() {
        getAccessDialogFragment().backToTpsSelectionScreen();
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_TPS_LOGIN;
    }
}
