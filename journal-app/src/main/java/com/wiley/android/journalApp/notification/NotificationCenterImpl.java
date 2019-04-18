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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.inject.Inject;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

public class NotificationCenterImpl implements NotificationCenter {
    private static final String TAG = NotificationCenterImpl.class.getSimpleName();
    private static final String PARAMS = "PARAMS";

    private final LocalBroadcastManager manager;
    private final HashMap<String, IntentFilter> filters = new HashMap<String, IntentFilter>();
    private final Map<NotificationProcessor, BroadcastReceiver> receivers = synchronizedMap(new HashMap<NotificationProcessor, BroadcastReceiver>());

    @Inject
    public NotificationCenterImpl(final Context context) {
        manager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void subscribeToNotification(final String eventName, final NotificationProcessor processor) {
        manager.registerReceiver(getBroadcastReceiverFor(processor), getFilter(eventName));
    }

    @Override
    public void unSubscribeFromNotification(final NotificationProcessor processor) {
        manager.unregisterReceiver(getBroadcastReceiverFor(processor));
        receivers.remove(processor);
    }

    @Override
    public void sendNotification(final String eventName, final Map<String, Object> params) {
        manager.sendBroadcast(createIntent(eventName, params));
    }

    @Override
    public void sendNotification(final String eventName) {
        sendNotification(eventName, new HashMap<String, Object>());
    }

    private BroadcastReceiver getBroadcastReceiverFor(final NotificationProcessor processor) {
        BroadcastReceiver receiver = receivers.get(processor);
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final Map<String, Object> params = (Map<String, Object>) intent.getSerializableExtra(PARAMS);
                    try {
                        processor.processNotification(params);
                    } catch (Exception e) {
                        Logger.s(TAG, e);
                    }
                }
            };
            receivers.put(processor, receiver);
        }
        return receiver;
    }

    private IntentFilter getFilter(final String eventName) {
        IntentFilter filter = filters.get(eventName);
        if (filter == null) {
            filter = new IntentFilter(eventName);
            filters.put(eventName, filter);
        }
        return filter;
    }

    private Intent createIntent(final String eventName, final Map<String, Object> params) {
        final Intent intent = new Intent(eventName);
        intent.putExtra(PARAMS, (Serializable) params);
        return intent;
    }
}
