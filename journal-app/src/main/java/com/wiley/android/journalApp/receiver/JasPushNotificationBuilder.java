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
package com.wiley.android.journalApp.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.urbanairship.UAirship;
import com.urbanairship.push.PushMessage;
import com.urbanairship.push.notifications.DefaultNotificationFactory;
import com.urbanairship.util.NotificationIDGenerator;
import com.wiley.android.journalApp.R;


/**
 * Created by alobachev on 9/15/14.
 */

public class JasPushNotificationBuilder extends DefaultNotificationFactory {

    private static final String TAG = JasPushNotificationBuilder.class.getSimpleName() + ".PushNotification";
    private static final int INBOX_NOTIFICATION_ID = 9000000;
    private static final long ONE_MINUTE = 60000L;

    private String lastAlert = "";
    private long timeBetweenAlert = 0L;

    public JasPushNotificationBuilder(Context context) {
        super(context);
    }

    @Override
    public Notification createNotification(PushMessage message, int notificationId) {
        final String alert = message.getAlert();
        long currentTimeMillis = System.currentTimeMillis();
        if (lastAlert.equals(alert) && (currentTimeMillis - timeBetweenAlert < ONE_MINUTE)) {
            Log.d(TAG, "buildNotification() SKIPPED: alert = '" + alert + "'");
            return null;
        }
        lastAlert = alert;
        timeBetweenAlert = currentTimeMillis;
        Log.d(TAG, "buildNotification() CREATED: alert = '" + alert + "'");

        Resources res = UAirship.shared().getApplicationContext().getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(res, R.drawable.icon_app_target);

        return new NotificationCompat.Builder(UAirship.shared().getApplicationContext())
                .setContentTitle(UAirship.getAppName())
                .setContentText(message.getAlert())
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.icon_app_target)
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .build();
    }

    @Override
    public int getNextId(PushMessage pushMessage) {
        return NotificationIDGenerator.nextID();
    }


    /**
     * Dismisses the inbox style notification if it exists
     */
    public static void dismissInboxNotification() {
        NotificationManager manager = (NotificationManager) UAirship.shared().
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        manager.cancel(INBOX_NOTIFICATION_ID);
    }
}