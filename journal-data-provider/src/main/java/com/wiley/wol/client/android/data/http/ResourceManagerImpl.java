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

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.manager.ResourceType;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.error.AccessForbiddenException;
import com.wiley.wol.client.android.error.ConnectionException;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Environment;
import com.wiley.wol.client.android.settings.LastModifiedManager;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static android.text.TextUtils.isEmpty;
import static com.wiley.wol.client.android.data.http.DownloadManager.ACCESS_FORBIDDEN;
import static com.wiley.wol.client.android.data.http.DownloadManager.NOT_MODIFIED_CODE;
import static com.wiley.wol.client.android.data.http.DownloadManager.OK_CODE;
import static com.wiley.wol.client.android.data.http.DownloadManager.TPS;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE_TYPE;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class ResourceManagerImpl implements ResourceManager {
    private static final String TAG = ResourceManagerImpl.class.getSimpleName();

    private final ExecutorService executor = newFixedThreadPool(10);
    private final ExecutorService listenerExecutor = newFixedThreadPool(5);
    private final Map<String, DownloadOperation> tasks = new HashMap<>();
    private final Set<String> listenerTasks = new HashSet<>();

    @Inject
    private DownloadManager downloadManager;
    @Inject
    private OperationManager operationManager;
    @Inject
    @InjectCachePath
    private String rootPath;
    @Inject
    private Settings settings;
    @Inject
    private Theme theme;
    @Inject
    private LastModifiedManager lastModifiedManager;
    @Inject
    private Environment environment;

    @Override
    public void addSmallTask(final String url, final String localFileName, final boolean notifyProgress) {
        addTask(url, getAbsolutePath(localFileName), notifyProgress);
    }

    @Override
    public void addBigTask(final String url, final String localFileName, final HashMap<String, Object> params) {
        addTask(url, getAbsolutePath(localFileName), false, params);
    }

    @Override
    public void addSmallTask(final String url, final String localFileName, final boolean notifyProgress, final HashMap<String, Object> params) {
        addTask(url, getAbsolutePath(localFileName), notifyProgress, params);
    }

    @Override
    public void addSmallTask(final String url, final String localDirectoryPath,
                             final String localFileName, final boolean notifyProgress,
                             final HashMap<String, Object> params) {
        File fileToDownload = new File(rootPath + File.separator + localDirectoryPath, localFileName);
        addTask(url, fileToDownload.getAbsolutePath(), notifyProgress, params);
    }

    @Override
    public void removeLoadedFile(String url, String localFileName) {
        lastModifiedManager.removeLastModified(url);
        File absolutePath = getAbsolutePathAsFile(localFileName);
        if (!absolutePath.delete()) {
            Logger.s(TAG, format("Unable to delete file '%s'", absolutePath.getAbsolutePath()));
        }
    }

    @Override
    public String getArticleLocalPath(final DOI doi) {
        return getAbsolutePath(doi.getArticleZipCompatibleValue());
    }

    private synchronized void addTask(final String url, final String absolutePath,
                                      final boolean notify,
                                      final HashMap<String, Object> params) {
        final DownloadOperation previousTask = tasks.get(url);
        if (previousTask == null || !previousTask.getResource().getFilePath().equals(absolutePath)) {
            submitTask(url, absolutePath, notify, params);
            Logger.i(TAG, format("Added new task with url[%s] path[%s] notify[%b]", url, absolutePath, notify));
        } else {
            Logger.i(TAG, format("Set high priority for task with url[%s]", url));
        }
    }

    @Override
    public synchronized void cancelTask(final String url) {
        final ThreadOperation task = tasks.get(url);
        if (task != null) {
            Logger.i(TAG, format("Canceling task with url[%s]", url));
            tasks.remove(url);
            task.interrupt();
        } else {
            Logger.i(TAG, format("Can not find task with url[%s]", url));
        }
    }

    @Override
    public synchronized boolean hasRunningTask(String url) {
        final ThreadOperation task = tasks.get(url);
        return task != null && task.getState() == ThreadOperation.OperationState.STARTED;
    }

    @Override
    public synchronized boolean hasRunningListenerTask(String url) {
        boolean has;
        synchronized (listenerTasks) {
            has = listenerTasks.contains(url);
        }
        return has;
    }

    @Override
    public synchronized boolean hasRunningListenerTask() {
        boolean has;
        synchronized (listenerTasks) {
            has = listenerTasks.size() > 0;
        }
        return has;
    }

    @Override
    public void addSmallTask(final String url, final Listener<InputStream> listener) {
        addSmallTask(url, listener, null);
    }

    @Override
    public void addSmallTask(final String url, final Listener<InputStream> listener, final HashMap<String, Object> params) {
        synchronized (listenerTasks) {
            if (listenerTasks.add(url)) {
                listenerExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String currentLastModified = lastModifiedManager.getLastModified(url);
                            final List<Header> headers = getRequestHeaders(url, currentLastModified, params);

                            // request
                            final HttpResponse response;
                            try {
                                response = downloadManager.connectTo(url, headers);
                            } catch (final IOException ex) {
                                Logger.d(TAG, "smallTask url: " + url + ", IOException : " + ex.getMessage());
                                throw new ConnectionException(ex);
                            }

                            final int statusCode = downloadManager.getStatusCodeFor(response);
                            Logger.d(TAG, "smallTask url: " + url + ", statusCode: " + statusCode + ", last modified " + currentLastModified);
                            switch (statusCode) {
                                case OK_CODE:
                                    listener.onComplete(downloadManager.getContentFor(response), headersToMap(response.getAllHeaders()));
                                    lastModifiedManager.addLastModified(url, downloadManager.getLastModifiedValue(response));
                                    break;
                                case NOT_MODIFIED_CODE:
                                    listener.onNotModified();
                                    break;
                                case ACCESS_FORBIDDEN:
                                    throw new AccessForbiddenException();
                                default:
                                    throw new Exception("Wrong status code: " + statusCode);
                            }
                        } catch (final Exception ex) {
                            listener.onError(ex);
                        } finally {
                            synchronized (listenerTasks) {
                                listenerTasks.remove(url);
                            }
                        }
                    }

                    private Map<String, String> headersToMap(final Header[] headers) {
                        final Map<String, String> result = new HashMap<>(headers.length);
                        for (Header header : headers) {
                            result.put(header.getName(), header.getValue());
                        }
                        return result;
                    }
                });
            } else {
                Logger.d(TAG, "smallTask url: " + url + " IGNORED");
            }
        }
    }

    private List<Header> getRequestHeaders(final String url, final String currentLastModified, final HashMap<String, Object> params) {
        final ParamsReader pr = new ParamsReader(params);
        List<Header> headers = createDeviceSpecificHeaders();
        if (currentLastModified != null && !pr.ignoreLastModified()) {
            headers.add(downloadManager.createIfModifiedSinceHeader(currentLastModified));
        }

        if (settings.hasSubscriptionReceipt()) {
            headers.add(downloadManager.createGooglePlayHeaderWithIdentityAndType(url, settings.getSubscriptionReceipt()));
        } else {
            final String accessToken = settings.getAccessToken();
            final String accessTokenSecret = settings.getAccessTokenSecret();
            if (!isEmpty(accessToken) && !isEmpty(accessTokenSecret)) {
                if (environment.isDebug()) {
                    final String debugIp = settings.getDebugIp();
                    if (debugIp != null && debugIp.length() > 0) {
                        headers.add(downloadManager.createTrueClientIpHeader(debugIp));
                    }
                }

                headers.add(downloadManager.createMobileAffiliatedHeader(settings.getAffiliationInfo()));
                headers.add(downloadManager.createAuthHeaders(url, accessToken, accessTokenSecret));
            }

            if (pr.hasParam(TPS) && (Boolean) pr.getParam(TPS)) {
                headers.add(downloadManager.createAuthTpsFeedHeader(url));
            }
        }
        return headers;
    }

    private String getAbsolutePath(final String localPath) {
        return getAbsolutePathAsFile(localPath).getAbsolutePath();
    }

    private File getAbsolutePathAsFile(final String localPath) {
        return new File(rootPath, localPath);
    }

    private void addTask(final String url, final String absolutePath, final boolean notify) {
        addTask(url, absolutePath, notify, new HashMap<String, Object>());
    }

    private void submitTask(final String url, final String absolutePath, final boolean notify, final HashMap<String, Object> params) {
        tasks.remove(url);
        final Resource resource = new Resource();
        resource.setUrl(url);
        resource.setFilePath(absolutePath);
        resource.setNotifyProgress(notify);
        resource.setResourceType((ResourceType) params.get(RESOURCE_TYPE));
        final DownloadOperation task = operationManager.createDownloadOperation(resource, params);
        tasks.put(url, task);
        final Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } finally {
                    tasks.remove(url);
                }
            }
        };
        executor.submit(taskWrapper);
    }

    private List<Header> createDeviceSpecificHeaders() {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("X-OS-Type", "Android"));
        headers.add(new BasicHeader("X-App-Info", theme.getAppInfoString()));
        headers.add(new BasicHeader("JAS-User-Agent", theme.getJasUserAgent()));

        return headers;
    }

    @Override
    public void updateFeedItemContent(final String url, final String params, final Listener<InputStream> listener, final String urlConverterServer) {
        synchronized (listenerTasks) {
            if (listenerTasks.add(url)) {
                listenerExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // download original feedItemContent
                            String feedItemContentOriginal;
                            HttpResponse response;
                            int statusCode;
                            List<Header> headers = createDeviceSpecificHeaders();
                            headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2; en-us; SCH-I800 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1"));
                            try {
                                response = downloadManager.connectTo(url, headers);
                            } catch (final IOException ex) {
                                throw new ConnectionException(ex);
                            }

                            statusCode = downloadManager.getStatusCodeFor(response);
                            Logger.d(TAG, "convertFeedItemContent.original url: " + url + ", statusCode: " + statusCode);
                            if (OK_CODE == statusCode) {
                                feedItemContentOriginal = IOUtils.toString(downloadManager.getContentFor(response));
                                Logger.d(TAG, "convertFeedItemContent.original body.length: " + feedItemContentOriginal.length());
                            } else {
                                throw new Exception("Wrong status code: " + statusCode);
                            }

                            // send original and download converted feedItemContent
                            headers = createDeviceSpecificHeaders();
                            headers.add(new BasicHeader("X-Site-Url", url));
                            headers.add(new BasicHeader("X-Feed-Params", null == params ? "" : params));

                            try {
                                response = downloadManager.executePostRequest(urlConverterServer,
                                        "text/html; charset=utf-8", feedItemContentOriginal, headers);
                            } catch (final IOException ex) {
                                response = null;
                            }

                            if (null == response) {
                                Logger.d(TAG, "convertFeedItemContent.converted url: " + urlConverterServer + ", IOException !!! body.length: " + feedItemContentOriginal.length());
                                InputStream inputStream = null;
                                try {
                                    inputStream = IOUtils.toInputStream(feedItemContentOriginal);
                                    listener.onComplete(inputStream, headersToMap(new Header[]{}));
                                } finally {
                                    if (null != inputStream)
                                        IOUtils.closeQuietly(inputStream);
                                }

                            }
                            else {
                                statusCode = downloadManager.getStatusCodeFor(response);
                                Logger.d(TAG, "convertFeedItemContent.converted url: " + urlConverterServer + ", statusCode: " + statusCode );
                                switch (statusCode) {
                                    case OK_CODE:
                                        listener.onComplete(downloadManager.getContentFor(response), headersToMap(response.getAllHeaders()));
                                        //lastModifiedManager.addLastModified(url, downloadManager.getLastModifiedValue(response));
                                        break;
                                    case NOT_MODIFIED_CODE:
                                        InputStream inputStream = null;
                                        try {
                                            inputStream = IOUtils.toInputStream("");
                                            listener.onComplete(inputStream, headersToMap(new Header[]{}));
                                        } finally {
                                            if (null != inputStream)
                                                IOUtils.closeQuietly(inputStream);
                                        }
                                        break;
                                    case ACCESS_FORBIDDEN:
                                        throw new AccessForbiddenException();
                                    default:
                                        throw new Exception("Wrong status code: " + statusCode);
                                }
                            }
                        } catch (final Exception ex) {
                            listener.onError(ex);
                        } finally {
                            synchronized (listenerTasks) {
                                listenerTasks.remove(url);
                            }
                        }
                    }

                    private Map<String, String> headersToMap(final Header[] headers) {
                        final Map<String, String> result = new HashMap<>(headers.length);
                        for (Header header : headers) {
                            result.put(header.getName(), header.getValue());
                        }
                        result.put("feedItemContentUrl", url);
                        return result;
                    }
                });
            }
        }
    }

    @Override
    public void updateArticleInfoHtmlBody(final String url, final Listener<InputStream> listener) {
        synchronized (listenerTasks) {
            if (listenerTasks.add(url)) {
                listenerExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Header> headers = createDeviceSpecificHeaders();
                            final String currentLastModified = lastModifiedManager.getLastModified(url);
                            if (currentLastModified != null) {
                                headers.add(downloadManager.createIfModifiedSinceHeader(currentLastModified));
                            }

                            HttpResponse response;
                            try {
                                response = downloadManager.connectTo(url, headers);
                            } catch (final IOException ex) {
                                throw new ConnectionException(ex);
                            }

                            int statusCode = downloadManager.getStatusCodeFor(response);
                            switch (statusCode) {
                                case OK_CODE:
                                    listener.onComplete(downloadManager.getContentFor(response), headersToMap(response.getAllHeaders()));
                                    lastModifiedManager.addLastModified(url, downloadManager.getLastModifiedValue(response));
                                    break;
                                case NOT_MODIFIED_CODE:
                                    listener.onNotModified();
                                    break;
                                case ACCESS_FORBIDDEN:
                                    throw new AccessForbiddenException();
                                default:
                                    throw new Exception("Wrong status code: " + statusCode);
                            }
                        } catch (final Exception ex) {
                            listener.onError(ex);
                        } finally {
                            synchronized (listenerTasks) {
                                listenerTasks.remove(url);
                            }
                        }
                    }

                    private Map<String, String> headersToMap(final Header[] headers) {
                        final Map<String, String> result = new HashMap<>(headers.length);
                        for (Header header : headers) {
                            result.put(header.getName(), header.getValue());
                        }
                        return result;
                    }
                });
            }
        }
    }

    @Override
    public void changeKeyword(final String url, final String keyword, final String action, final String deviceToken, final Listener<JSONObject> listener) {
        synchronized (listenerTasks) {
            if (listenerTasks.add(url + keyword + action)) {
                listenerExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Header> headers = createDeviceSpecificHeaders();
                            JSONObject json = new JSONObject();
                            json.put("action", action)
                                    .put("keyword", keyword)
                                    .put("devtoken", deviceToken);

                            DownloadManager.JsonResponse response = downloadManager.executeJsonRequest(url, json, headers);


                            switch (response.statusCode) {
                                case OK_CODE:
                                    listener.onComplete(response.json);
                                    break;
                                case ACCESS_FORBIDDEN:
                                    throw new AccessForbiddenException();
                                default:
                                    throw new Exception("Wrong status code: " + response.statusCode);
                            }
                        } catch (final Exception ex) {
                            listener.onError(ex);
                        } finally {
                            synchronized (listenerTasks) {
                                listenerTasks.remove(url + keyword + action);
                            }
                        }
                    }
                });
            }
        }

    }

    @Override
    public void updateListOfSubscribedKeywords(final String url, final Listener<JSONObject> listener) {
        synchronized (listenerTasks) {
            if (listenerTasks.add(url)) {
                listenerExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Header> headers = createDeviceSpecificHeaders();
                            DownloadManager.JsonResponse response = downloadManager.executeJsonRequest(url, new JSONObject(), headers);

                            switch (response.statusCode) {
                                case OK_CODE:
                                    listener.onComplete(response.json);
                                    break;
                                case ACCESS_FORBIDDEN:
                                    throw new AccessForbiddenException();
                                default:
                                    throw new Exception("Wrong status code: " + response.statusCode);
                            }
                        } catch (final Exception ex) {
                            listener.onError(ex);
                        } finally {
                            synchronized (listenerTasks) {
                                listenerTasks.remove(url);
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void executeJsonRequest(final String url, JSONObject json,  final Listener<InputStream> listener) {
        try {
            final List<Header> headers = getRequestHeaders(url, null, new HashMap<String, Object>());
            final HttpResponse httpResponse = downloadManager.executePostRequest(url, "application/json; charset=utf-8",
                    json.toString(), headers);

            int statusCode = downloadManager.getStatusCodeFor(httpResponse);
            switch (statusCode) {
                case OK_CODE:
                    listener.onComplete(downloadManager.getContentFor(httpResponse));
                    break;
                case NOT_MODIFIED_CODE:
                    listener.onNotModified();
                    break;
                case ACCESS_FORBIDDEN:
                    throw new AccessForbiddenException();
                default:
                    throw new Exception("Wrong status code: " + statusCode);
            }
        } catch (final Exception ex) {
            listener.onError(ex);
        }
    }
}