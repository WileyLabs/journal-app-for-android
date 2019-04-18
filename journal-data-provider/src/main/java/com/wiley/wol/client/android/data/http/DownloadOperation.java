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

import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.error.AccessForbiddenException;
import com.wiley.wol.client.android.error.ConnectionException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.LastModifiedManager;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import static android.text.TextUtils.isEmpty;
import static com.wiley.wol.client.android.data.http.DownloadManager.ACCESS_FORBIDDEN;
import static com.wiley.wol.client.android.data.http.DownloadManager.NOT_MODIFIED_CODE;
import static com.wiley.wol.client.android.data.http.DownloadManager.OK_CODE;
import static com.wiley.wol.client.android.data.http.DownloadManager.PARTIAL_CONTENT;
import static com.wiley.wol.client.android.data.http.DownloadManager.RESET_CONTENT;
import static com.wiley.wol.client.android.data.http.DownloadManager.UNAUTHORIZED;
import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.FINISHED;
import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.INTERRUPTED;
import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.STARTED;
import static com.wiley.wol.client.android.data.manager.ResourceType.ARTICLE_ZIP;
import static com.wiley.wol.client.android.data.manager.ResourceType.ISSUE_ZIP;
import static com.wiley.wol.client.android.notification.EventList.DONE_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.ERROR_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.PROCESS_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.NotificationCenter.CANCELLED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static java.lang.String.format;
import static java.lang.Thread.sleep;

public class DownloadOperation extends AbstractThreadOperation {

    public static final String RESOURCE = "RESOURCE";
    public static final String RESOURCE_TYPE = "RESOURCE_TYPE";
    public static final String DOI = "DOI";

    private static final String TAG = DownloadOperation.class.getSimpleName();
    private static final int BUFFER_SIZE = 8192;

    private final Resource resource;
    private final Settings settings;
    private final LastModifiedManager lastModifiedManager;

    private HashMap<String, Object> params;

    private NotificationCenter notificationCenter;

    private DownloadManager downloadManager;

    private AANHelper aanHelper;

    private byte[] buffer;
    private int read;
    private HttpGet request;
    private HttpResponse response;
    private InputStream in;
    private OutputStream out;
    private boolean hasInternetConnection;

    public Resource getResource() {
        return resource;
    }

    protected DownloadOperation(final Resource resource, final Settings settings, final LastModifiedManager lastModifiedManager) {
        super();
        this.resource = resource;
        this.settings = settings;
        this.lastModifiedManager = lastModifiedManager;
    }

    @Override
    protected void taskPart() throws Exception {
        if (read < 0)
            return;

        resource.addDownloadedSize(read);
        //Logger.d(TAG, format("Write to file[%s] %d/%d bytes", resource.getFilePath(), resource.getDownloadedSize(), resource.getFileSize()));
        out.write(buffer, 0, read);
        if (resource.isNotifyProgress()) {
            params.put(RESOURCE, resource);
            notificationCenter.sendNotification(PROCESS_RESOURCE_DOWNLOADING.getEventName(), params);
        }
        read = -1;
    }

    @Override
    protected void beforeTask() throws Exception {
        Logger.d(TAG, format("Open connection for url[%s]", resource.getUrl()));
        params.put(RESOURCE, resource);
        hasInternetConnection = aanHelper.isOnline();
        final ArrayList<Header> headers = new ArrayList<>();
        if (ISSUE_ZIP == resource.getResourceType() || ARTICLE_ZIP == resource.getResourceType()) {
            if (settings.hasSubscriptionReceipt()) {
                headers.add(downloadManager.createGooglePlayHeaderWithIdentityAndType(resource.getUrl(), settings.getSubscriptionReceipt()));
            } else {
                final String accessToken = settings.getAccessToken();
                final String accessTokenSecret = settings.getAccessTokenSecret();
                if (!isEmpty(accessToken) && !isEmpty(accessTokenSecret)) {
                    headers.add(downloadManager.createAuthHeaders(resource.getUrl(), accessToken, accessTokenSecret));
                }
            }
        }

        final File file = new File(resource.getFilePath());
        final String lastModifiedString = lastModifiedManager.getLastModified(resource.getUrl());
        if (file.exists() && lastModifiedString != null) {
            headers.add(downloadManager.createIfModifiedSinceHeader(lastModifiedString));
        }
        try {
            request = downloadManager.createRequest(resource.getUrl(), headers);
            response = downloadManager.executeRequest(request);

            boolean hasSupportsRange = false;
            for (Header header : response.getAllHeaders()) {
                if ("Accept-Ranges".equals(header.getName())) {
                    hasSupportsRange = true;
                }
            }
            Logger.d(TAG, format("Server for url[%s] has 'Accept-Ranges' = %b", resource.getUrl(), hasSupportsRange));
        } catch (final IOException ex) {
            throw new ConnectionException(ex);
        }
        resource.setStatusCode(downloadManager.getStatusCodeFor(response));
        switch (resource.getStatusCode()) {
            case NOT_MODIFIED_CODE:
                Logger.d(TAG, format("File with url[%s] and path [%s] is already downloaded", resource.getUrl(), resource.getFilePath()));
                break;
            case OK_CODE:
                in = downloadManager.getContentFor(response);
                out = createOutputStream(file);
                buffer = new byte[BUFFER_SIZE];
                resource.setOperationState(STARTED);
                resource.setFileSize(downloadManager.getContentLengthFor(response));
                lastModifiedManager.removeLastModified(resource.getUrl());
                Logger.d(TAG, format("File with url[%s] and path [%s] is started downloading", resource.getUrl(), resource.getFilePath()));
                break;
            case UNAUTHORIZED:
            case ACCESS_FORBIDDEN:
                throw new AccessForbiddenException();
            default:
                throw new Exception("Wrong response code " + resource.getStatusCode());
        }
    }

    @Override
    protected boolean afterTask() {
        closeConnection();

        if (RESET_CONTENT == resource.getStatusCode()) {
            Logger.d(TAG, format("File with url[%s] should be to reload", resource.getUrl()));
            return true;
        }

        Logger.d(TAG, format("File with url[%s] is downloaded", resource.getUrl()));
        resource.setOperationState(FINISHED);
        lastModifiedManager.addLastModified(resource.getUrl(), downloadManager.getLastModifiedValue(response));
        notificationCenter.sendNotification(DONE_RESOURCE_DOWNLOADING.getEventName(), params);
        return false;
    }

    @Override
    protected boolean condition() throws Exception {
        if (!hasInternetConnection) {
            sleep(1000);
            if (aanHelper.isOnline()) {
                Logger.d(TAG, "Status internet connection: YES");
                hasInternetConnection = true;
                try {
                    sleep(3000);
                    request.setHeader("Range", "bytes=" + resource.getDownloadedSize() + "-");
                    response = downloadManager.executeRequest(request);
                    resource.setStatusCode(downloadManager.getStatusCodeFor(response));
                    switch (resource.getStatusCode()) {
                        case OK_CODE:
                        case RESET_CONTENT:
                            resource.setStatusCode(RESET_CONTENT);
                            return false;
                        case PARTIAL_CONTENT:
                            resource.setStatusCode(OK_CODE);
                            in = downloadManager.getContentFor(response);
                            break;
                        default:
                            throw new Exception("Wrong response code after restore internet connection: " + resource.getStatusCode());
                    }
                } catch (final IOException ex) {
                    throw new ConnectionException(ex);
                }
            }

            return true;
        }

        try {
            return resource.getStatusCode() == OK_CODE && (read = in.read(buffer)) != -1;
        } catch (Exception exception) {
            if (exception instanceof java.net.SocketException || exception instanceof SocketTimeoutException) {
                Logger.d(TAG, "Status internet connection: NO");
                hasInternetConnection = false;
                read = -1;
            } else {
                throw exception;
            }
        }

        return true;
    }

    @Override
    protected void whenError(final Exception ex) {
        resource.setError(ex);
        resource.setOperationState(INTERRUPTED);
        final boolean cancelled = ex instanceof InterruptedException;
        params.put(CANCELLED, cancelled);
        params.put(ERROR, resource.getError());
        notificationCenter.sendNotification(ERROR_RESOURCE_DOWNLOADING.getEventName(), params);
        if (cancelled)
            downloadManager.abortRequest(request);
        closeConnection();
    }

    private void closeConnection() {
        Logger.d(TAG, format("Close connection for url[%s]", resource.getUrl()));
        if (resource.getStatusCode() == OK_CODE) {
            IOUtils.closeQuietly(out);
            try {
                downloadManager.close(response);
            } catch (final IOException ignored) {
            }
        }
    }

    private OutputStream createOutputStream(final File file) throws FileNotFoundException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        return new FileOutputStream(file);
    }

    protected void setNotificationCenter(final NotificationCenter notificationCenter) {
        this.notificationCenter = notificationCenter;
    }

    protected void setDownloadManager(final DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    protected void setAANHelper(final AANHelper aanHelper) {
        this.aanHelper = aanHelper;
    }
    public HashMap<String, Object> getParams() {
        return params;
    }

    public void setParams(final HashMap<String, Object> params) {
        this.params = params;
    }
}
