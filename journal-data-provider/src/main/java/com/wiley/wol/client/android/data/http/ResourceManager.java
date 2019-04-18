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

import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.domain.DOI;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;

public interface ResourceManager {

    void addSmallTask(String url, String localFileName, boolean notifyProgress);

    void cancelTask(String url);

    boolean hasRunningTask(String url);

    boolean hasRunningListenerTask(String url);

    boolean hasRunningListenerTask();

    void addSmallTask(String url, com.wiley.wol.client.android.data.manager.Listener<InputStream> listener);

    void addSmallTask(String url, com.wiley.wol.client.android.data.manager.Listener<InputStream> listener, HashMap<String, Object> params);

    void addBigTask(String url, String localFileName, HashMap<String, Object> params);

    void addSmallTask(String url, String localFileName, boolean notifyProgress, HashMap<String, Object> params);

    void addSmallTask(String url, String localDirectoryPath,
                      String localFileName, boolean notifyProgress,
                      HashMap<String, Object> params);

    void removeLoadedFile(String url, String localFileName);

    String getArticleLocalPath(DOI doi);

    void updateFeedItemContent(String url, String params, Listener<InputStream> listener, String urlConverterServer);

    void updateArticleInfoHtmlBody(String url, Listener<InputStream> listener);

    void changeKeyword(String url, String keyword, String action, String deviceToken, Listener<JSONObject> listener);

    void updateListOfSubscribedKeywords(String url, Listener<JSONObject> listener);

    void executeJsonRequest(String url, JSONObject json, Listener<InputStream> listener);
}