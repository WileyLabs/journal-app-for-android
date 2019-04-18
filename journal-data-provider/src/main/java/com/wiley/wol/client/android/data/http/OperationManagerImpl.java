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
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.LastModifiedManager;
import com.wiley.wol.client.android.settings.Settings;

import java.util.HashMap;

public class OperationManagerImpl implements OperationManager {

    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private DownloadManager downloadManager;
    @Inject
    private Settings settings;
    @Inject
    private LastModifiedManager lastModifiedManager;
    @Inject
    private AANHelper aanHelper;

    @Override
    public DownloadOperation createDownloadOperation(final Resource resource, final HashMap<String, Object> params) {
        final DownloadOperation operation = new DownloadOperation(resource, settings, lastModifiedManager);
        operation.setParams(params);
        operation.setDownloadManager(downloadManager);
        operation.setNotificationCenter(notificationCenter);
        operation.setAANHelper(aanHelper);
        return operation;
    }
}
