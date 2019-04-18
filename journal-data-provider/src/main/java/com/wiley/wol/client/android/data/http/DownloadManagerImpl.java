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
package com.wiley.wol.client.android.data.http;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.google.inject.Inject;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.apache.http.params.HttpConnectionParams.setConnectionTimeout;
import static org.apache.http.params.HttpConnectionParams.setSoTimeout;

@TargetApi(Build.VERSION_CODES.FROYO)
public class DownloadManagerImpl implements DownloadManager {

    private static final String TAG = DownloadManagerImpl.class.getSimpleName();

    private final HttpParams httpParams;
    @Inject
    private Theme theme;

    public DownloadManagerImpl() {
        httpParams = new BasicHttpParams();
        setConnectionTimeout(httpParams, 3 * 60000);
        setSoTimeout(httpParams, 3 * 60000);
    }

    @Override
    public HttpResponse connectTo(final String url) throws IOException {
        return connectTo(url, Collections.<Header>emptyList());
    }

    @Override
    public HttpResponse connectTo(final String url, final List<Header> headers) throws IOException {
        final HttpGet request = createRequest(url, headers);
        return executeRequest(request);
    }

    @Override
    public HttpGet createRequest(String url) throws IOException {
        return createRequest(url, Collections.<Header>emptyList());
    }

    @Override
    public HttpGet createRequest(String url, List<Header> headers) throws IOException {
        final HttpGet request = getDefaultRequest();
        request.setURI(URI.create(url));
        final Header[] headerArray = headers.toArray(new Header[headers.size()]);
        request.setHeaders(headerArray);

        return request;
    }

    @Override
    public HttpResponse executeRequest(HttpGet request) throws IOException {
        return getDefaultHttpClient().execute(request);
    }

    @Override
    public void abortRequest(HttpGet request) {
        request.abort();
    }

    @Override
    public InputStream getContentFor(final HttpResponse response) throws IOException {
        return response.getEntity().getContent();
    }

    @Override
    public int getStatusCodeFor(final HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public long getContentLengthFor(final HttpResponse response) {
        return response.getEntity().getContentLength();
    }

    @Override
    public void close(final HttpResponse response) throws IOException {
        response.getEntity().consumeContent();
    }

    @Override
    public String getLastModifiedValue(final HttpResponse response) {
        final Header header = response.getFirstHeader("Last-Modified");
        return header != null ? header.getValue() : null;
    }

    @Override
    public Header createIfModifiedSinceHeader(final String date) {
        return new BasicHeader("If-Modified-Since", date);
    }

    public Header createTrueClientIpHeader(final String clientIp) {
        return new BasicHeader("True-Client-Ip", clientIp);
    }

    public Header createMobileAffiliatedHeader(final String affiliationInfo) {
        String affiliationValue = "[]";
        if (affiliationInfo != null && affiliationInfo.length() != 0) {
            final JSONArray organisations;
            try {
                organisations = new JSONObject(affiliationInfo).getJSONArray("organisations");

                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < organisations.length(); i++) {
                    JSONObject organisation = organisations.getJSONObject(i);
                    JSONObject outputJsonObject = new JSONObject();
                    outputJsonObject.put("externalId", organisation.getString("user"));
                    outputJsonObject.put("name", organisation.getString("name"));
                    jsonArray.put(outputJsonObject);
                }

                affiliationValue = jsonArray.toString();
            } catch (JSONException e) {
                Logger.s(TAG, e);
            }
        }

        return new BasicHeader("X-Mobile-Affiliated", affiliationValue);
    }

    @Override
    public synchronized Header createAuthHeaderWithIdentityAndType(String url, String identity, String type) {
        final String siteCd = String.format("JAS-%s", theme.getAppPrefix());
        final String feedCd = getFeedCd(url);
        final String query = query(url);
        final String baseSignatureString = String.format("%s%s%s%s", siteCd, feedCd, query == null ? "" : query, identity);


        final String salt = sha1(siteCd);
        final String signature = getHash(baseSignatureString, salt);
        final String headerValue;
        try {
            headerValue = String.format("identity=\"%s\", signature=\"%s\", identityType=\"%s\"", URLEncoder.encode(identity, "UTF-8"), URLEncoder.encode(signature, "UTF-8"), type);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);

        }

        Logger.d(TAG, "--- MCS-Identity signing:");
        Logger.d(TAG, "URL: " + url);
        Logger.d(TAG, "feedCd: " + feedCd);
        Logger.d(TAG, "query: " + query);
        Logger.d(TAG, "identity: " + identity);
        Logger.d(TAG, "salt: " + salt);
        Logger.d(TAG, "base signature string: " + baseSignatureString);
        Logger.d(TAG, "MCS-Identity: " + headerValue);
        Logger.d(TAG, "-------------------------------------------------------------");

        return new BasicHeader("MCS-Identity", headerValue);
    }

    @Override
    public synchronized Header createAuthHeaders(final String url, final String accessToken, final String accessTokenSecret) {
        final String identity = new String(Base64.encode(String.format("%s|%s", accessToken, accessTokenSecret).getBytes(), Base64.NO_WRAP));
        final String type = "wol";
        Logger.d(TAG, "--- CREATING AUTH HEADER WITH TOKENS:");
        Logger.d(TAG, "access token: " + accessToken);
        Logger.d(TAG, "access token secret: " + accessTokenSecret);
        return createAuthHeaderWithIdentityAndType(url, identity, type);
    }

    @Override
    public Header createAuthTpsFeedHeader(String url) {
        String type = "tps";
        String identity = String.format("Tps request %s", new Date().toString());
        Logger.d(TAG, "--- CREATING AUTH HEADER FOR TPS FEED:");
        return createAuthHeaderWithIdentityAndType(url, identity, type);
    }

    @Override
    public Header createGooglePlayHeaderWithIdentityAndType(String url, String subscriptionReceipt) {
        try {
            final JSONObject json = new JSONObject(subscriptionReceipt);
            final String productId = json.getString("productId");
            final String packageName = json.optString("packageName");
            final int purchaseState = json.optInt("purchaseState", 0);
            final String token = json.optString("token", json.optString("purchaseToken"));

            final String identity = String.format("%s,%s,%s", packageName, productId,token);
            Logger.d(TAG + ".InAppBilling", "getHttpResponseForIdentityOnGooglePlay(): url = " + url
                    + "; purchaseState = " + purchaseState
                    + "; identity = " + identity);
            final String identityBase64 = new String(Base64.encode(String.format("%s", identity).getBytes(), Base64.NO_WRAP));
            return createAuthHeaderWithIdentityAndType(url, identityBase64, "google-play");
        } catch (JSONException ignored) {
        }

        return createAuthHeaderWithIdentityAndType(url, "", "");
    }

    @Override
    public HttpResponse executePostRequest(String url, final String contentType, String body) throws IOException {
        return executePostRequest(url, contentType, body, Collections.<Header>emptyList());
    }

    @Override
    public HttpResponse executePostRequest(String url, final String contentType, String body, List<Header> headers) throws IOException {
        final HttpPost request = new HttpPost();
        request.setURI(URI.create(url));
        final Header[] headerArray = headers.toArray(new Header[headers.size()]);
        request.setHeaders(headerArray);
        StringEntity entity = new StringEntity(body, "UTF-8");
        entity.setContentType(contentType);
        request.setEntity(entity);
        final HttpClient defaultHttpClient = getDefaultHttpClient();
        final HttpParams httpParams = defaultHttpClient.getParams();
        setConnectionTimeout(httpParams, 60000);
        setSoTimeout(httpParams, 60000);
        return defaultHttpClient.execute(request);
    }

    @Override
    public JsonResponse executeJsonRequest(String url, JSONObject json) {
        return executeJsonRequest(url, json, Collections.<Header>emptyList());
    }

    @Override
    public JsonResponse executeJsonRequest(String url, JSONObject json, List<Header> headers) {
        String content = json.toString();
        Logger.d("JsonRequest", "start request to " + url + "; content: " + content);
        HttpResponse httpResponse = null;
        try {
            try {
                httpResponse = executePostRequest(url, "application/json; charset=utf-8", content, headers);
                JSONObject responseJson = null;
                String errorMessage = "";
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                Logger.d("JsonRequest", "statusCode: " + statusCode);
                if (statusCode == OK_CODE || statusCode == CREATED) {
                    String responseString = EntityUtils.toString(httpResponse.getEntity());
                    Logger.d("JsonRequest", "response is ok. content: " + responseString);
                    if (!TextUtils.isEmpty(responseString)) {
                        try {
                            responseJson = new JSONObject(responseString);
                        }  catch (JSONException ex) {
                            JSONArray jsonArray = new JSONArray(responseString);
                            responseJson = new JSONObject().put("arr",jsonArray);
                        }
                    }
                } else {
                     errorMessage = IOUtils.toString(httpResponse.getEntity().getContent());
                }
                return new JsonResponse(statusCode, responseJson, errorMessage);
            } finally {
                if (httpResponse != null) {
                    close(httpResponse);
                }
            }
        } catch (IOException | JSONException e) {
            Logger.d("JsonRequest", "request exception", e);
            return new JsonResponse(e);
        }
    }

    protected HttpGet getDefaultRequest() {
        return new HttpGet();
    }

    protected HttpClient getDefaultHttpClient() {
        return new DefaultHttpClient(httpParams);
    }

    private String query(final String urlPath) {
        try {
            final URI uri = new URI(urlPath);
            return uri.getQuery();
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String sha1(final String text) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(text.getBytes());
            return new String(Hex.encodeHex(messageDigest.digest()));
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHash(final String clearText, final String secret) {
        try {
            final String type = "HmacSHA1";
            final Mac mac = Mac.getInstance(type);
            final SecretKeySpec key = new SecretKeySpec(secret.getBytes(), type);
            mac.init(key);
            final byte[] hmacEncoded = mac.doFinal(clearText.getBytes());
            return Base64.encodeToString(hmacEncoded, Base64.NO_WRAP);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFeedCd(final String urlPath) {
        try {
            final URI uri = new URI(urlPath);
            final String[] paths = uri.getPath().split("/");
            final String path = paths[paths.length - 1];
            int index = path.indexOf(".");
            if (index < 0) {
                return "";
            }
            return path.substring(0, index);
        } catch (final URISyntaxException e) {
            return "";
        }
    }
}