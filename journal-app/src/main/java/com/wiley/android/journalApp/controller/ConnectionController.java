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
package com.wiley.android.journalApp.controller;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.inject.Inject;
import com.wiley.android.journalApp.utils.NetworkStateReceiver;
import com.wiley.wol.client.android.notification.NotificationCenter;

import java.util.HashMap;

import static com.wiley.wol.client.android.notification.EventList.NETWORK_STATE_CHANGED;

/**
 * Created by taraskreknin on 13.05.14.
 */
public class ConnectionController {

    @Inject
    private NotificationCenter mNotificationCenter;

    private final Context mContext;
    private final NetworkStateReceiver mStateReceiver = new NetworkStateReceiver() {
        @Override
        protected void onNetworkStateChanged(boolean available) {
            fireStateChanged(available);
        }
    };

    @Inject
    public ConnectionController(Context c) {
        mContext = c;
    }

    private void fireStateChanged(boolean available) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("online", available);
        mNotificationCenter.sendNotification(NETWORK_STATE_CHANGED.getEventName(), params);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }

    public boolean isOnline() {
        // TODO service check
        return isNetworkAvailable();
    }

    public void start() {
        mStateReceiver.register(mContext);
    }

    public void stop() {
        mStateReceiver.unregister();
    }

}
