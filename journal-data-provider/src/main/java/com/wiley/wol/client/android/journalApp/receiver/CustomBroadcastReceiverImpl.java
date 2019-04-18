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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.http.UpdateManager;

import roboguice.receiver.RoboBroadcastReceiver;

import static android.app.AlarmManager.INTERVAL_HOUR;
import static android.app.AlarmManager.RTC;
import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.getBroadcast;
import static java.lang.System.currentTimeMillis;

public class CustomBroadcastReceiverImpl extends RoboBroadcastReceiver implements CustomBroadcastReceiver {
    public static final long TEN_MINUTES = 600000L;
    @Inject
    private Application application;
    @Inject
    private AlarmManager alarmManager;
    @Inject
    private UpdateManager updateManager;

    private PendingIntent affiliationIntent;

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    public void install() {
        install(currentTimeMillis() + INTERVAL_HOUR);
    }

    @Override
    public void installAndTrigger() {
        install();
        updateFeeds();
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void install(long triggerAtMillis ) {
        final Context context = application.getApplicationContext();
        final PendingIntent pendingIntent = getBroadcast(context, 0, new Intent(context, CustomBroadcastReceiverImpl.class), FLAG_CANCEL_CURRENT);
        alarmManager.setRepeating(RTC, triggerAtMillis, INTERVAL_HOUR, pendingIntent);
    }

    @Override
    public void startUpdateAffiliationSchedulerAndTrigger() {
        final Context context = application.getApplicationContext();
        affiliationIntent = getBroadcast(context, 1, new Intent(context, AffiliationReceiver.class), FLAG_CANCEL_CURRENT);
        alarmManager.setRepeating(RTC, currentTimeMillis() + TEN_MINUTES, TEN_MINUTES, affiliationIntent);
        updateManager.updateAffiliationFeed();
    }

    @Override
    public void stopUpdateAffiliationScheduler() {
        alarmManager.cancel(affiliationIntent);
    }

    @Override
    protected void handleReceive(final Context context, final Intent intent) {
        updateFeeds();
    }

    private void updateFeeds() {
        updateManager.updateFeeds();
        updateManager.updateHomePageFeeds();
    }
}
