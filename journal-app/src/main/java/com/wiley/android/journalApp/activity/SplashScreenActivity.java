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
package com.wiley.android.journalApp.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.journalApp.receiver.CustomBroadcastReceiver;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.util.Map;

public class SplashScreenActivity extends JournalActivity {
    @Inject
    private Settings settings;
    @Inject
    private Theme theme;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private CustomBroadcastReceiver broadcastReceiver;

    private long startDate;
    private boolean waitForAffiliationFeedUpdate;
    private boolean homeFeedUpdated;
    private boolean issueListUpdated;
    private static final Object MONITOR = new Object();

    private NotificationProcessor homeFeedUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            synchronized (MONITOR) {
                homeFeedUpdated = true;
                MONITOR.notifyAll();
            }
        }
    };

    private NotificationProcessor issueListFeedUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            synchronized (MONITOR) {
                issueListUpdated = true;
                MONITOR.notifyAll();
            }

        }
    };

    final NotificationProcessor affiliationInfoUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            if (waitForAffiliationFeedUpdate) {
                waitForAffiliationFeedUpdate = false;
                broadcastReceiver.installAndTrigger();
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_SUCCESS.getEventName(), homeFeedUpdatedProcessor);
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_ERROR.getEventName(), homeFeedUpdatedProcessor);
        notificationCenter.subscribeToNotification(EventList.HOME_FEED_UPDATED_NOT_MODIFIED.getEventName(), homeFeedUpdatedProcessor);

        notificationCenter.subscribeToNotification(EventList.ISSUE_LIST_UPDATED.getEventName(), issueListFeedUpdatedProcessor);
        notificationCenter.subscribeToNotification(EventList.ISSUE_LIST_NOT_MODIFIED.getEventName(), issueListFeedUpdatedProcessor);
        notificationCenter.subscribeToNotification(EventList.ISSUE_LIST_ERROR.getEventName(), issueListFeedUpdatedProcessor);


        notificationCenter.subscribeToNotification(EventList.AFFILIATION_INFO_UPDATE_FINISHED.getEventName(), affiliationInfoUpdateFinishedProcessor);

        if (settings.isAuthorized()) {
            waitForAffiliationFeedUpdate = true;
            broadcastReceiver.startUpdateAffiliationSchedulerAndTrigger();
        } else {
            broadcastReceiver.installAndTrigger();
        }

        if (theme.customSplashScreen()) {
            setContentView(R.layout.splash_layout);

            final ImageView iView = (ImageView) findViewById(R.id.iv_background);
            iView.setVisibility(View.VISIBLE);
            if (DeviceUtils.isLandscape(this)) {
                iView.setImageResource(R.drawable.splash_landscape);
            } else {
                iView.setImageResource(R.drawable.splash_default);
            }
        }

        boolean needShowGetAccessScreenOnStart = !settings.hasSubscriptionReceipt() && !theme.isOpenAccessJournal();
        settings.setNeedShowGetAccessScreenOnStart(needShowGetAccessScreenOnStart);
        settings.setShowSponsoredPromo(true);

        startDate = System.currentTimeMillis();

        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!theme.customSplashScreen()) {
                    setContentView(R.layout.splash_layout);
                }
                final View progress = findViewById(R.id.progress);
                progress.setVisibility(View.VISIBLE);
            }
        }, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                synchronized (MONITOR) {
                    while (!(homeFeedUpdated && issueListUpdated)) {
                        try {
                            MONITOR.wait();
                        } catch (InterruptedException ignored) {
                            break;
                        }
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Void aVoid) {
                super.onPostExecute(aVoid);
                startMainActivity();
            }
        }.execute();
    }

    private void startMainActivity() {
        final int delayInMillis = getResources().getInteger(R.integer.splash_screen_timeout_in_millis);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                finish();
                GANHelper.trackEvent(GANHelper.EVENT_APP,
                        GANHelper.ACTION_LAUNCH,
                        GANHelper.LABEL_CLOSED,
                        0L);
            }
        }, delayInMillis - (System.currentTimeMillis() - startDate));
    }

    @Override
    protected void onDestroy() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(homeFeedUpdatedProcessor);
        notificationCenter.unSubscribeFromNotification(affiliationInfoUpdateFinishedProcessor);
        notificationCenter.unSubscribeFromNotification(issueListFeedUpdatedProcessor);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (theme.customSplashScreen()) {
            final ImageView iView = (ImageView) findViewById(R.id.iv_background);
            if (DeviceUtils.isLandscape(this)) {
                iView.setImageResource(R.drawable.splash_landscape);
            } else {
                iView.setImageResource(R.drawable.splash_default);
            }
        }
    }
}
