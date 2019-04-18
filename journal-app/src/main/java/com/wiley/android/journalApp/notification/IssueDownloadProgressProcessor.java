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
package com.wiley.android.journalApp.notification;

import com.wiley.wol.client.android.data.http.DownloadOperation;
import com.wiley.wol.client.android.data.http.Resource;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.Map;

import static com.wiley.wol.client.android.notification.NotificationCenter.IMPORTING;
import static com.wiley.wol.client.android.settings.Settings.DOWNLOAD_ISSUE;

/**
 * Created by taraskreknin on 13.08.14.
 */
public abstract class IssueDownloadProgressProcessor implements NotificationProcessor {

    @Override
    public void processNotification(Map<String, Object> params) {
        final DOI doi = (DOI) params.get(DOWNLOAD_ISSUE);

        // importing phase?
        final boolean isImporting = params.containsKey(IMPORTING) && (boolean) params.get(IMPORTING);
        if (isImporting) {
            onImportStarted(doi);
            return;
        }

        // start download?
        Boolean onZipDownloadStarted = (Boolean) params.get("onZipDownloadStarted");
        if (null == onZipDownloadStarted) {
            params.put("onZipDownloadStarted", Boolean.TRUE);
            onZipDownloadStarted(doi);
        }

        // next part
        final Resource resource = (Resource) params.get(DownloadOperation.RESOURCE);
        onZipDownloadProgress(doi, resource.getDownloadedSize(),
                resource.getFileSize());
    }

    protected abstract void onZipDownloadStarted(DOI doi);
    protected abstract void onZipDownloadProgress(DOI doi, long currentProgress, long totalProgress);
    protected abstract void onImportStarted(DOI doi);

}
