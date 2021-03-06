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
package com.wiley.android.journalApp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by taraskreknin on 13.05.14.
 */

public abstract class NetworkStateReceiver extends BroadcastReceiver {

    protected abstract void onNetworkStateChanged(boolean available);

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        handleConnectivityChange(intent);
    }

    private void handleConnectivityChange(Intent intent) {

        if (isInternetConnectionAvailable(context)) {
            onNetworkStateChanged(true);
            return;
        }

        if (intent.getExtras().getBoolean(
                ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            onNetworkStateChanged(false);
        }
    }

    private boolean isInternetConnectionAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }

    public void register(Context context) {
        if (context != null) {
            this.context = context;
            this.context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    public void unregister() {
        if (context != null) {
            context.unregisterReceiver(this);
            context = null;
        }
    }
}

