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
package com.wiley.wol.client.android.data.manager;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.xml.AdvertisementConfigUnit;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.settings.Settings;

public class AdvertisementManagerImpl implements AdvertisementManager {
    private static final String TAG = AdvertisementManagerImpl.class.getSimpleName();
    @Inject
    private Settings settings;

    private int counter = 0;

    @Override
    public boolean isTimeToShowAd() {
        boolean result = isTimeToShowAdWithCounter(counter);
        Logger.d(TAG, "isTimeToShowAd() counter: " + counter + ", result: " + result);
        return result;
    }

    private boolean isTimeToShowAdWithCounter(int counter) {
        final AdvertisementConfigUnit advertisementConfig = settings.getAdvertisementConfig();
        if (advertisementConfig == null || !advertisementConfig.needShowAd())
            return false;
        if (counter < advertisementConfig.getFirstAdArticleView())
            return false;
        return (counter - advertisementConfig.getFirstAdArticleView()) % (advertisementConfig.getRealOtherAdArticleView() + 1) == 0;
    }

    @Override
    public boolean isNextPageAd() {
        boolean result = isTimeToShowAdWithCounter(counter + 1);
        Logger.d(TAG, "isNextPageAd(), result: " + result);
        return result;
    }

    @Override
    public void reset() {
        counter = 0;
    }

    @Override
    public void resetIgnoringFirstAdArticleView() {
        final AdvertisementConfigUnit advertisementConfig = settings.getAdvertisementConfig();
        if (advertisementConfig == null) {
            counter = 0;
            return;
        }
        int first = advertisementConfig.getFirstAdArticleView();
        counter = counter < first ? 0 : (first + 1);
    }

    @Override
    public void increaseCounter() {
        counter++;
        Logger.d(TAG, "increaseCounter(). new value: " + counter);
    }

    @Override
    public void decreaseCount() {
        counter--;
        Logger.d(TAG, "decreaseCount(). new value: " + counter);
    }

}
