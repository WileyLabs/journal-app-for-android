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
import com.wiley.wol.client.android.data.manager.listener.RssUpdateListener;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.notification.NotificationCenter;

import java.io.InputStream;

/**
 * Created by taraskreknin on 03.10.14.
 */
public class RssUpdateListenerProviderImpl implements RssUpdateListenerProvider {

    @Inject
    private SimpleParser mSimpleParser;
    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    private HomePageService mHomePageService;

    @Override
    public Listener<InputStream> getListener(FeedMO feed) {
        return new RssUpdateListener(feed, mNotificationCenter, mHomePageService, mSimpleParser);
    }
}
