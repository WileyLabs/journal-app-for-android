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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface DownloadManager {

    String TPS = "tps";

    int OK_CODE = 200;
    int CREATED = 201;
    int RESET_CONTENT = 205;
    int PARTIAL_CONTENT = 206;
    int NOT_MODIFIED_CODE = 304;
    int UNAUTHORIZED = 401;
    int ACCESS_FORBIDDEN = 403;

    HttpResponse connectTo(String url) throws IOException;

    HttpResponse connectTo(String url, final List<Header> headers) throws IOException;

    HttpGet createRequest(String url) throws IOException;

    HttpGet createRequest(String url, final List<Header> headers) throws IOException;

    HttpResponse executeRequest(HttpGet request) throws IOException;

    void abortRequest(HttpGet request);

    InputStream getContentFor(HttpResponse response) throws IOException;

    int getStatusCodeFor(HttpResponse response);

    long getContentLengthFor(HttpResponse response);

    void close(HttpResponse response) throws IOException;

    String getLastModifiedValue(HttpResponse response);

    Header createIfModifiedSinceHeader(String date);

    Header createTrueClientIpHeader(String clientIp);

    Header createMobileAffiliatedHeader(String affiliationInfo);

    Header createAuthHeaderWithIdentityAndType(String url, final String identity, final String type);

    Header createAuthHeaders(String url, final String accessToken, final String accessTokenSecret);

    Header createAuthTpsFeedHeader(String url);

    Header createGooglePlayHeaderWithIdentityAndType(String url, String subscriptionReceipt);

    HttpResponse executePostRequest(final String url, final String contentType, final String body) throws IOException;

    HttpResponse executePostRequest(final String url, final String contentType, final String body, final List<Header> headers) throws IOException;

    class JsonResponse {
        public final JSONObject json;
        public final int statusCode;
        public final Exception exception;
        public final String errorMessage;

        public boolean isValid() {
            return exception == null && statusCode == OK_CODE;
        }

        public JsonResponse(final int statusCode, final JSONObject json, final String errorMessage) {
            this.json = json;
            this.statusCode = statusCode;
            this.errorMessage = errorMessage;
            exception = null;
        }

        public JsonResponse(Exception exception) {
            this.json = null;
            this.statusCode = 0;
            this.exception = exception;
            errorMessage = exception.getMessage();
        }
    }

    JsonResponse executeJsonRequest(String url, JSONObject json);

    JsonResponse executeJsonRequest(String url, JSONObject json, final List<Header> headers);
}