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

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.android.journalApp.utils.AdViewController;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.settings.Settings;

/**
 * Created by taraskreknin on 30.09.14.
 */
public class SponsoredPromoActivity extends JournalActivity implements View.OnClickListener {
    @Inject
    private Settings settings;
    private AdViewController adViewController;
    private View progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sponsored_promo);
        findViewById(R.id.promo_close).setOnClickListener(this);
        progress = findView(R.id.article_progress);
        progress.setVisibility(View.VISIBLE);

        adViewController = new AdViewController(this,
                settings.getAdvertisementConfig().getAdUnitId() + "-sub", new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                progress.setVisibility(View.GONE);
                adViewController.updateLayout();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);
                close();
            }
        });

        adViewController.loadAd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!DeviceUtils.isPhone(this)) {
            if (DeviceUtils.isLandscape(this)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.promo_close) {
            close();
        }
    }

    @Override
    protected void onDestroy() {
        adViewController.destroy();
        super.onDestroy();
    }

    private void close() {
        progress.setVisibility(View.GONE);
        if (!DeviceUtils.isPhone(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        finish();
    }
}