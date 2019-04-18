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
package com.wiley.wol.client.android.data.service;

import android.annotation.SuppressLint;
import android.os.Looper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.wiley.wol.client.android.data.dao.TPSSiteDao;
import com.wiley.wol.client.android.data.http.DownloadManager;
import com.wiley.wol.client.android.data.manager.FeedsInfo;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.AuthToken;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.text.TextUtils.isEmpty;
import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;
import static com.wiley.wol.client.android.settings.Settings.SETTING_AUTH_TOKEN;

/**
 * Created by alobachev on 7/10/14.
 */

public class AuthorizationServiceImpl implements AuthorizationService {
    private static final String TAG = AuthorizationServiceImpl.class.getSimpleName();

    @Inject
    private Settings settings;

    @Inject
    private TPSSiteDao tpsSiteDao;

    @Inject
    private DownloadManager downloadManager;

    private FeedsInfo feedsInfo;

    @Inject
    protected AANHelper aanHelper;

    @Inject
    private Theme theme;
    private boolean isUpdateContentAfterAuthorisationExpected;

    @Inject
    public AuthorizationServiceImpl(final NotificationCenter notificationCenter,
                                    final Provider<FeedsInfo> feedsInfoProvider) {
        feedsInfo = feedsInfoProvider.get();
        notificationCenter.subscribeToNotification(SETTINGS_CHANGED.getEventName(), new NotificationProcessor() {
            @Override
            public void processNotification(final Map<String, Object> params) {
                final String settingName = params.get(SETTING_NAME_KEY).toString();
                if ("current_server".equals(settingName)) {
                    feedsInfo = feedsInfoProvider.get();
                } else if (SETTING_AUTH_TOKEN.equals(settingName) && settings.isAuthorized()) {
                    isUpdateContentAfterAuthorisationExpected = true;
                }
            }
        });
    }

    @Override
    public boolean hasSociety() {
        return settings.getSocietyExists();
    }

    @Override
    public String getSocietyLoginInstructions() {
        return settings.getSocietyLoginInstructions();
    }

    @Override
    public String getSocietyUrl() {
        return settings.getSocietyUrl();
    }

    @Override
    public String getSocietyInformation() {
        return settings.getSocietyInformation();
    }

    @Override
    public boolean hasTPS() {
        return settings.getTPSExists();
    }

    @Override
    public String getTPSUsername() {
        return settings.getTPSUsername();
    }

    @Override
    public String getTPSPassword() {
        return settings.getTPSPassword();
    }

    @Override
    public int getTPSTimeout() {
        return settings.getTPSTimeout();
    }

    @Override
    public List<TPSSiteMO> getAllTPSSites() {
        return tpsSiteDao.findAll();
    }

    @Override
    public void saveLastLoginInformation(LoggedInInformation info) {
        if (info == null) {
            settings.setHasLastLoginInfo(false);
        } else {
            settings.setHasLastLoginInfo(true);
            settings.setLoggedInViaTps(info.viaTps);
            settings.setTpsLoginSite(info.tpsSiteName);
            settings.setLoginScreen(info.lastSuccessfulLoginScreen);
        }
    }

    @Override
    public LoggedInInformation getLastLoginInformation() {
        if (!settings.hasLastLoginInfo()) {
            return null;
        }
        if (settings.getLoggedInViaTps()) {
            return new LoggedInInformation(settings.getTpsLoginSite(), settings.getLoginScreen());
        } else {
            return new LoggedInInformation(settings.getLoginScreen());
        }
    }

    @Override
    public void saveAuthToken(AuthToken token) {
        settings.setAuthToken(token);
    }

    @Override
    public void clearAuthToken() {
        settings.resetAuthToken();
    }

    @Override
    public AccessCodeInformation getAccessCodeInformation() {
        if (!hasAccessCode()) {
            return null;
        }
        String accessCode = settings.getAccessCode();
        Date expirationDate = settings.getAccessCodeExpirationDate();
        return new AccessCodeInformation(accessCode, expirationDate);
    }

    @Override
    public boolean hasAccessCode() {
        return settings.hasAccessCode();
    }

    @Override
    @SuppressLint("NewApi")
    public boolean checkAccessCode(String code) {
        assert (Thread.currentThread() != Looper.getMainLooper().getThread());

        String url = feedsInfo.getLicenseFeed();

        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("code", code);
            requestJson.put("action", "validate");
        } catch (JSONException e) {
            Logger.s("AuthorizationService", e);
            throw new RuntimeException(e);
        }

        DownloadManager.JsonResponse jsonResponse = prepareAndExecuteJsonRequest(url, requestJson);
        if (jsonResponse.isValid()) {
            Logger.d("AuthorizationService", "Code is valid");
            return true;
        } else {
            Logger.d("AuthorizationService", "Code is invalid");
            if (jsonResponse.exception != null) {
                Logger.d("AuthorizationService", "Exception on Json response", jsonResponse.exception);
            }
            if (jsonResponse.statusCode != 0) {
                Logger.d("AuthorizationService", "Status code from Json response: " + jsonResponse.statusCode);
            }
            return false;
        }
    }

    @Override
    @SuppressLint("NewApi")
    public DownloadManager.JsonResponse useAccessCode(String code) {
        assert (Thread.currentThread() != Looper.getMainLooper().getThread());

        String url = feedsInfo.getLicenseFeed();

        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("code", code);
            requestJson.put("action", "create");
        } catch (JSONException e) {
            Logger.s("AuthorizationService", e);
            throw new RuntimeException(e);
        }

        DownloadManager.JsonResponse jsonResponse = prepareAndExecuteJsonRequestWithAuth(url, requestJson);
        if (jsonResponse.exception == null && jsonResponse.statusCode == 201) {
            Logger.d("AuthorizationService", "Code is valid");
            JSONObject responseJson = jsonResponse.json;

            Date expirationDate = null;

            if (responseJson != null && responseJson.length() > 0) {
                try {
                    JSONObject root = responseJson.getJSONObject(responseJson.names().getString(0));
                    String expirationDateString = root.getString("validTo");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    expirationDate = dateFormat.parse(expirationDateString);

                } catch (JSONException | ParseException e) {
                    Logger.s(TAG, e.getMessage(), e);
                }
            }

            aanHelper.trackSubscriptionCodeAddedOverlay();
            settings.setAccessCode(code);
            settings.setAccessCodeExpirationDate(expirationDate);
        } else {
            aanHelper.trackSubscriptionCodeInvalidOverlay();
        }

        return jsonResponse;
    }

    @Override
    @SuppressLint("NewApi")
    public RegisterNewUserResult registerNewUser(NewUserData data) {
        assert (data != null);
        assert (Thread.currentThread() != Looper.getMainLooper().getThread());

        String url = feedsInfo.getRegisterNewUserFeed();

        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("firstName", data.firstName);
            requestJson.put("lastName", data.lastName);
            requestJson.put("email", data.email);
            requestJson.put("password", data.password);
            requestJson.put("workflowType", (data.accessCode == null || data.accessCode.length() == 0 ?
                    "MobileAffiliation" : "SponsoredSubscription"));
//            requestJson.put("code", data.accessCode);
        } catch (JSONException e) {
            Logger.s("AuthorizationService", e);
            throw new RuntimeException(e);
        }

        DownloadManager.JsonResponse jsonResponse = prepareAndExecuteJsonRequest(url, requestJson);

        if (jsonResponse.exception != null) {
            Logger.s("AuthorizationService", "Register user error", jsonResponse.exception);
            return RegisterNewUserResult.NetworkError;
        } else {
            if (jsonResponse.statusCode == 201) {
                if (data.accessCode != null && data.accessCode.length() > 0) {
                    settings.setAccessCode(data.accessCode, data.email);
                }
                return RegisterNewUserResult.Success;
            } else if (jsonResponse.statusCode == 409) {
                return RegisterNewUserResult.EmailError;
            } else {
                return RegisterNewUserResult.NetworkError;
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean activateUser(String query) {
        assert (Thread.currentThread() != Looper.getMainLooper().getThread());

        final String url = theme.getActivateNewUserFeedOnServer(settings.getCurrentServer());
        final String contentType = "application/x-www-form-urlencoded; charset=utf-8";
        try {
            // TODO process query
            final String requestBody = query.substring(query.indexOf('?') + 1);
            final HttpResponse response = downloadManager.executePostRequest(url, contentType, requestBody);
            int code = downloadManager.getStatusCodeFor(response);
            if (code == DownloadManager.OK_CODE || code == DownloadManager.NOT_MODIFIED_CODE) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    @Override
    public boolean isUpdateContentAfterAuthorisationExpected() {
        return isUpdateContentAfterAuthorisationExpected;
    }

    @Override
    public void setUpdateContentAfterAuthorisationExpected(boolean isUpdateContentAfterAuthorisationExpected) {
        this.isUpdateContentAfterAuthorisationExpected = isUpdateContentAfterAuthorisationExpected;
    }

    @SuppressLint("NewApi")
    private DownloadManager.JsonResponse prepareAndExecuteJsonRequest(String url, JSONObject requestJson) {
        assert (Thread.currentThread() != Looper.getMainLooper().getThread());
        return downloadManager.executeJsonRequest(url, requestJson);
    }

    @SuppressLint("NewApi")
    private DownloadManager.JsonResponse prepareAndExecuteJsonRequestWithAuth(String url, JSONObject requestJson) {
        assert (Thread.currentThread() != Looper.getMainLooper().getThread());

        List<Header> headers = new ArrayList<>();

        if (settings.hasSubscriptionReceipt()) {
            headers.add(downloadManager.createGooglePlayHeaderWithIdentityAndType(url, settings.getSubscriptionReceipt()));
        } else {
            final String accessToken = settings.getAccessToken();
            final String accessTokenSecret = settings.getAccessTokenSecret();
            if (!isEmpty(accessToken) && !isEmpty(accessTokenSecret)) {
                headers.add(downloadManager.createAuthHeaders(url, accessToken, accessTokenSecret));
            }
        }

        return downloadManager.executeJsonRequest(url, requestJson, headers);
    }

}
