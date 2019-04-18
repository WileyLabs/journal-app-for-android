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
package com.wiley.android.journalApp.authorization;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Message;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.AuthToken;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.EncryptionUtils;

import org.apache.commons.ssl.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import roboguice.RoboGuice;

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

/**
 * Created by taraskreknin on 08.08.14.
 */
public class OAuthHelper {

    public static interface Listener {
        void onAuthProgress(String message);

        void onWaitForUserAction();

        void onBackPressed();
    }

    private static final String TAG = OAuthHelper.class.getSimpleName();

    private static final int CHUNK_LENGTH = 6;
    private static final int RADIX = 16;
    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final int SERVER_PORT = 443;

    private static final String TOKEN_PATH = "/3/token";
    private static final String REQUEST_PATH = "/3/initiate";
    private static final String AUTHORIZE_PATH = "/authorize";

    private static final String DUMMY_CALLBACK = "http://localhost:8082/ws/callback";
    private static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String ENCODING_UTF8 = "UTF-8";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private Context context;
    private CustomWebView webView;
    private boolean accessTokenRequestInProgress = false;
    private boolean isSilent, isInProgress, isLoggingIn, isConfirming, afterUserActivation;
    private String oauthToken;
    private String oauthTokenSecret;
    private String login;
    private String password;
    private Listener listener;
    private LoadAuthenticationPage loadPageTask;
    private GetAccessTokens getTokensTask;

    @Inject
    private Theme theme;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private Settings settings;

    private void setUserLogin(final String userLogin) {
        if (userLogin != null && !"null".equals(userLogin)) {
            final String login = userLogin.replaceAll("^\"|\"$", "").trim();
            if (!isEmpty(login)) {
                settings.setUserLogin(login);
            }
        }
    }

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(final WebView view, final String url) {
            super.onPageFinished(view, url);
            listener.onWaitForUserAction();

            Logger.d(TAG, "on page finished " + url);
            if (accessTokenRequestInProgress) {
                return;
            }
            if (!isSilent && listener != null) {
                listener.onWaitForUserAction();
            } else if (isSilent) {
                processPageFinished(url);
            }
        }

        @Override
        public void onLoadResource(final WebView view, final String url) {
            if (url != null && url.contains("authorize")) {
                final String javaScriptUrl = "%s(function() { var username = " +
                        "document.getElementById('username').value; return %s;})()";

                webView.loadUrl(format(javaScriptUrl, "javascript:", "userInfo.saveUserLogin(username)"));
            }

            super.onLoadResource(view, url);
        }

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            listener.onAuthProgress(null);
            Logger.d(TAG, "on page started " + url);

//            "Cancel" button click processing.
//            There no way to get http request body here. Objective C realisation:
//            NSString * httpBody = [[NSString alloc] initWithData:request.HTTPBody encoding:NSUTF8StringEncoding];
//            if ([httpBody rangeOfString:@"cancel" options:NSCaseInsensitiveSearch].location != NSNotFound) {
//                [self cancel];
//                return NO;
//            }
            if (url != null && url.contains("permission=denied")) {
                authorizationService.saveLastLoginInformation(null);
                stopAuthorisation();
                listener.onBackPressed();
                return;
            }

            if (url != null && url.contains(DUMMY_CALLBACK) && !accessTokenRequestInProgress) {
                try {
                    accessTokenRequestInProgress = true;
                    final String oauthVerifier = getParametersMapFromResponse(url).get("oauth_verifier");
                    new GetAccessTokens().execute(oauthToken, oauthTokenSecret, oauthVerifier);
                } catch (final Exception e) {
                    //TODO: process it
                    onCompleted(false);
                }

            }
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            super.onFormResubmission(view, dontResend, resend);
        }

        @Override
        public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
            if (errorCode == -2) {
                onCompleted(false);
            } else {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }
    };

    public OAuthHelper(CustomWebView webView, Listener listener) {
        this.webView = webView;
        this.webView.setWebViewClient(webViewClient);

        this.webView.addJavascriptInterface(new UserInfoJavaScriptInterface(), "userInfo");

        this.listener = listener;
        this.context = webView.getContext();
        if (context != null) {
            RoboGuice.getInjector(context).injectMembersWithoutViews(this);
        }
    }

    private void onCompleted(boolean success) {
        if (isInProgress) {
            isInProgress = false;
            final ParamsBuilder paramsBuilder = new ParamsBuilder();
            final String eventName = success ? EventList.AUTH_SUCCESS.getEventName() : EventList.AUTH_ERROR.getEventName();
            if (!success) {
                authorizationService.saveLastLoginInformation(null);
            } else if (afterUserActivation) {
                paramsBuilder.withParam(NotificationCenter.LOGGED_IN_AFTER_USER_ACTIVATION, true);
            }
            notificationCenter.sendNotification(eventName, paramsBuilder.get());
        }
    }

    public void authorizeAfterUserActivation() {
        authorize(false, null, null, true);
    }

    public void authorize() {
        authorize(false, null, null);
    }

    public void authorize(boolean silent, String login, String pass, boolean afterUserActivation) {
        this.isSilent = silent;
        this.isInProgress = true;
        this.login = login;
        this.password = pass;
        this.afterUserActivation = afterUserActivation;
        this.loadPageTask = new LoadAuthenticationPage();
        this.loadPageTask.execute();
    }

    public void authorize(boolean silent, String login, String pass) {
        authorize(silent, login, pass, false);
    }

    public void stopAuthorisation() {
        this.isInProgress = false;
        this.password = null;
        this.login = null;
        this.afterUserActivation = false;
        if (this.loadPageTask != null) {
            this.loadPageTask.cancel(true);
            this.loadPageTask = null;
        }
        if (this.getTokensTask != null) {
            this.getTokensTask.cancel(true);
            this.getTokensTask = null;
        }
    }

    public boolean isInProgress() {
        return isInProgress;
    }

    private void processPageFinished(String url) {
        if (!isLoggingIn && !isConfirming) {
            webView.removeCallbacks(doJsLoginRunnable);
            webView.postDelayed(doJsLoginRunnable, 1000);
            return;
        }

        if (isLoggingIn && !isConfirming) {
            webView.removeCallbacks(doJsConfirmRunnable);
            webView.postDelayed(doJsConfirmRunnable, 1000);
        }

    }

    private final Runnable doJsLoginRunnable = new Runnable() {
        @Override
        public void run() {
            doJsLogin();
        }
    };

    private void doJsLogin() {
        Logger.d(TAG, "doJsLogin");
        isLoggingIn = true;
        final String script = format("$('#username').val('%s');$('#password').val('%s');document.authorizationForm.submit();",
                EncryptionUtils.decryptKey(login, context.getString(R.string.recipe)),
                EncryptionUtils.decryptKey(password, context.getString(R.string.recipe)));
        webView.executeJavaScriptAndGetResult(script, new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                Logger.d(TAG, "doJsLogin finished");
            }
        });
    }

    private final Runnable doJsConfirmRunnable = new Runnable() {
        @Override
        public void run() {
            doJsConfirm();
        }
    };

    private void doJsConfirm() {
        isLoggingIn = false;
        isConfirming = true;
        Logger.d(TAG, "doJsConfirm");
        // check login is correct
        final String checkScript = "return $('.login-error').length == 0;";
        webView.executeJavaScriptAndGetResult(checkScript, new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                if ("true".equals(result)) {
                    // confirm login
                    // TODO click will not work on Android less than 4.4
                    webView.executeJavaScriptAndGetResult("$('input[type=submit][value=Confirm]').click();", new CustomWebView.JavaScriptExecutionCallback() {
                        @Override
                        public void onJavaScriptResult(String result) {
                            Logger.d(TAG, "doJsConfirm clicked on confirm");
                        }
                    });
                } else {
                    Logger.d(TAG, "doJsConfirm wrong pass " + result);
                    onCompleted(false);
                }
            }
        });
    }

    private static Map<String, String> getParametersMapFromResponse(final String serverResponse) {
        final Map<String, String> result = new HashMap<>();
        final StringTokenizer paramsTokenizer = new StringTokenizer(serverResponse, "&");
        while (paramsTokenizer.hasMoreTokens()) {
            final String paramNameAndValue = paramsTokenizer.nextToken();
            final StringTokenizer valuesTokenizer = new StringTokenizer(paramNameAndValue, "=");
            result.put(valuesTokenizer.nextToken(), valuesTokenizer.nextToken());
        }
        return result;
    }

    private static String sendPostAndGetResponse(final String url, final String authorizationHeader) throws Exception {

        String serverProtocol = new URL(url).getProtocol();

        final SocketFactory factory;

        if ("https".equals(serverProtocol)) {
            factory = new SimpleSSLSocketFactory(null);
            ((SimpleSSLSocketFactory) factory)
                    .setHostnameVerifier(SSLSocketFactory.
                            ALLOW_ALL_HOSTNAME_VERIFIER);
        } else {
            factory = PlainSocketFactory.getSocketFactory();
        }

        final HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme(serverProtocol, factory, SERVER_PORT));
        final ClientConnectionManager ccm = new ThreadSafeClientConnManager(
                params, registry);
        final DefaultHttpClient httpClient = new DefaultHttpClient(ccm, params);
        final HttpPost httpPost = new HttpPost(url);

        httpPost.addHeader(AUTHORIZATION_HEADER, authorizationHeader);
        final HttpResponse httpResponse = httpClient.execute(httpPost);
        final HttpEntity httpEntity = httpResponse.getEntity();
        return EntityUtils.toString(httpEntity);
    }

    private String getTokenRequestHeader(final String consumerKey) throws NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException {
        final String oauthTimeStamp = getSecondsSince1970();
        final String oauthNonce = UUID.randomUUID().toString().toUpperCase();

        final StringBuilder headerBuilder = new StringBuilder();
        addHeaderParam(headerBuilder, "OAuth realm", "wiley", false);
        addHeaderParam(headerBuilder, "oauth_consumer_key", consumerKey, false);
        addHeaderParam(headerBuilder, "oauth_signature_method", OAUTH_SIGNATURE_METHOD, false);
        final String oAuthSignature = getOAuthSignature(oauthNonce + oauthTimeStamp, getDecryptSecretKey());
        addHeaderParam(headerBuilder, "oauth_signature", oAuthSignature, false);
        addHeaderParam(headerBuilder, "oauth_timestamp", oauthTimeStamp, false);
        addHeaderParam(headerBuilder, "oauth_nonce", oauthNonce, false);
        addHeaderParam(headerBuilder, "oauth_callback", DUMMY_CALLBACK, true);

        return headerBuilder.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    String getDecryptSecretKey() {
        final byte[] data = hexStringToByteArray(theme.getSecretKey());
        final byte[] saltData = hexStringToByteArray(context.getString(R.string.recipe));
        final byte[] resultData = new byte[data.length];

        for (int i = 0; i < data.length; ++i) {
            byte b = (byte) (data[i] ^ saltData[i % saltData.length]);
            resultData[i] = b;
        }

        return new String(resultData, Charset.forName("UTF-8"));
    }

    private static String getOAuthSignature(final String signatureString, final String secretKey)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return URLEncoder.encode(sha1(signatureString, secretKey), ENCODING_UTF8);
    }

    private static String sha1(final String baseString, final String keyString) throws InvalidKeyException, NoSuchAlgorithmException {
        final SecretKey secretKey;
        final byte[] keyBytes = keyString.getBytes();
        secretKey = new SecretKeySpec(keyBytes, HMAC_SHA1);
        final Mac mac = Mac.getInstance(HMAC_SHA1);
        mac.init(secretKey);
        final byte[] text = baseString.getBytes();
        return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
    }

    private static List<String> getChunksList(final String chunksString) {
        final List<String> chunks = new ArrayList<>();
        final char[] characters = chunksString.toCharArray();

        final int numberOfFullChunks = characters.length / CHUNK_LENGTH;
        final int numberOfRemainCharactersInLastChunk = characters.length - (numberOfFullChunks * CHUNK_LENGTH);

        for (int chunkNumber = 0; chunkNumber < numberOfFullChunks; chunkNumber++) {
            chunks.add(String.copyValueOf(characters, chunkNumber * CHUNK_LENGTH, CHUNK_LENGTH));
        }
        chunks.add(String.copyValueOf(characters, characters.length - numberOfRemainCharactersInLastChunk,
                numberOfRemainCharactersInLastChunk));
        return chunks;
    }

    private String getAccessTokenRequestHeader(final String oauthTokenReceived, final String oauthTokenSecretReceived,
                                               final String oauthVerifierReceived, final String consumerKey) throws NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException {
        final String oauthTimeStamp = getSecondsSince1970();
        final String oauthNonce = UUID.randomUUID().toString().toUpperCase();

        final StringBuilder headerBuilder = new StringBuilder();
        addHeaderParam(headerBuilder, "OAuth realm", "wiley", false);
        addHeaderParam(headerBuilder, "oauth_consumer_key", consumerKey, false);
        addHeaderParam(headerBuilder, "oauth_token", oauthTokenReceived, false);
        addHeaderParam(headerBuilder, "oauth_signature_method", OAUTH_SIGNATURE_METHOD, false);
        addHeaderParam(headerBuilder, "oauth_timestamp", oauthTimeStamp, false);
        addHeaderParam(headerBuilder, "oauth_verifier", oauthVerifierReceived, false);
        addHeaderParam(headerBuilder, "oauth_signature", getOAuthSignature(oauthNonce + oauthTimeStamp,
                getDecryptSecretKey() + "&" + oauthTokenSecretReceived), false);
        addHeaderParam(headerBuilder, "oauth_nonce", oauthNonce, true);

        return headerBuilder.toString();
    }

    private static void addHeaderParam(final StringBuilder sb, final String paramName, final String paramValue, final boolean isLastParam) {
        sb.append(paramName).append("=\"").append(paramValue).append("\"");
        if (!isLastParam) {
            sb.append(",");
        }
    }

    private static String getSecondsSince1970() {
        return String.valueOf(new Date().getTime() / MILLISECONDS_IN_SECOND);
    }

    private class GetAccessTokens extends AsyncTask<String, Void, AuthToken> {
        @Override
        protected void onPreExecute() {
            if (listener != null) {
                listener.onAuthProgress(context.getString(R.string.authenticating));
            }
        }

        @Override
        protected AuthToken doInBackground(final String... params) {
            Logger.d(TAG, "GetAccessTokens started");
            final String oauthToken = params[0];
            final String oauthTokenSecret = params[1];
            final String oauthVerifier = params[2];
            try {
                if (!isSilent) {
                    webView.post(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadData("", "", "");
                        }
                    });
                }

                final String accessTokenUrl = settings.getDebugOAuthLink() + TOKEN_PATH;

                final Map<String, String> accessTokensParam = getParametersMapFromResponse(
                        sendPostAndGetResponse(
                                accessTokenUrl,
                                getAccessTokenRequestHeader(oauthToken, oauthTokenSecret, oauthVerifier, theme.getConsumerKey())));

                if (accessTokensParam.size() == 1 && accessTokensParam.keySet().iterator().next().contains("Error report")) {
                    return null;
                } else {
                    return new AuthToken(accessTokensParam.get("oauth_token"), accessTokensParam.get("oauth_token_secret"));
                }
            } catch (final Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final AuthToken authToken) {
            Logger.d(TAG, "GetAccessTokens finished. " + (authToken == null ? "Failed" : "Succeeded"));
            super.onPostExecute(authToken);
            if (authToken != null) {
                authorizationService.saveAuthToken(authToken);
            } else {
                authorizationService.clearAuthToken();
            }
            accessTokenRequestInProgress = false;
            onCompleted(authToken != null);
        }
    }

    private class LoadAuthenticationPage extends AsyncTask<Void, Void, Map<String, String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (listener != null) {
                listener.onAuthProgress(context.getString(R.string.progressMessage));
            }
        }

        @Override
        protected Map<String, String> doInBackground(final Void... args) {
            final Map<String, String> params = new HashMap<>();
            try {
                final String requestTokenUrl = settings.getDebugOAuthLink() + REQUEST_PATH;
                params.putAll(
                        getParametersMapFromResponse(
                                sendPostAndGetResponse(
                                        requestTokenUrl,
                                        getTokenRequestHeader(theme.getConsumerKey())
                                )
                        )
                );
            } catch (final Exception e) {
                Logger.s(TAG, e);
                return null;
            }
            return params;
        }

        @Override
        protected void onPostExecute(final Map<String, String> params) {
            if (params == null) {
                onCompleted(false);
                return;
            }
            super.onPostExecute(params);
            oauthToken = params.get("oauth_token");
            oauthTokenSecret = params.get("oauth_token_secret");
            final String authorizationUrl = settings.getDebugOAuthLink().replaceFirst("(?s)(.*)/oauth", "$1" + "") + AUTHORIZE_PATH;
            webView.loadUrl(authorizationUrl + "?oauth_token=" + oauthToken);
        }
    }

    void setTheme(Theme theme) {
        this.theme = theme;
    }

    final class UserInfoJavaScriptInterface {
        UserInfoJavaScriptInterface() {
        }

        public void saveUserLogin(String userLogin) {
            setUserLogin(userLogin);
        }
    }
}
