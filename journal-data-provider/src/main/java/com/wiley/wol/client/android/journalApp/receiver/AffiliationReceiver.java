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
package com.wiley.wol.client.android.journalApp.receiver;

import android.content.Context;
import android.content.Intent;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.http.UpdateManager;

import roboguice.receiver.RoboBroadcastReceiver;

public class AffiliationReceiver extends RoboBroadcastReceiver {
    @Inject
    private UpdateManager updateManager;

    @Override
    protected void handleReceive(Context context, Intent intent) {
        updateManager.updateAffiliationFeed();
    }
}
