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
package com.wiley.wol.client.android.data.xml.loader.http;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.wiley.wol.client.android.data.xml.loader.IssueListFeedLoader;
import com.wiley.wol.client.android.data.xml.loader.IssueLoader;

public final class HtppBasedFeedLoaderModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IssueListFeedLoader.class).to(HtppBasedIssueListFeedLoaderImpl.class).in(Singleton.class);
        bind(IssueLoader.class).to(HtppBasedIssueLoaderImpl.class).in(Singleton.class);
    }
}
