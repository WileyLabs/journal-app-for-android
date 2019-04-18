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

import com.wiley.wol.client.android.data.manager.ResourceType;

import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.NOT_STARTED;
import static com.wiley.wol.client.android.data.manager.ResourceType.UNKNOWN;

public class Resource {

    private String filePath;
    private String url;
    private boolean notifyProgress;
    private long downloadedSize;
    private long fileSize;
    private int statusCode;
    private Exception error;
    private ThreadOperation.OperationState operationState = NOT_STARTED;
    private ResourceType resourceType = UNKNOWN;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public boolean isNotifyProgress() {
        return notifyProgress;
    }

    public void setNotifyProgress(final boolean notifyProgress) {
        this.notifyProgress = notifyProgress;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void addDownloadedSize(final long downloadedSize) {
        this.downloadedSize += downloadedSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public Exception getError() {
        return error;
    }

    public void setError(final Exception error) {
        this.error = error;
    }

    public ThreadOperation.OperationState getOperationState() {
        return operationState;
    }

    public void setOperationState(final ThreadOperation.OperationState operationState) {
        this.operationState = operationState;
    }

    public void setResourceType(final ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}