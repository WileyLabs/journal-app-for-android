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
package com.wiley.wol.client.android.data.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HttpClientUtils {
    public static final String FEED_LINK_TEMPLATE = "http://%s/JAS-%s/%s/";
    public static final String MAIN_URL_PART = "journalApp";
    public static final String SERVER_URL = "contentserver.wiley.com";
    // journal name should be in settings.xml for the journal
    public static final String TARGET_NAME = "JGRD";
    public static final String EARLY_VIEW_FEED =
            String.format(FEED_LINK_TEMPLATE + "earlyViewArticleRefs.feed",
                    SERVER_URL, TARGET_NAME, MAIN_URL_PART);
    public static final String ISSUES_FEED =
            String.format(FEED_LINK_TEMPLATE + "issueList.feed",
                    SERVER_URL, TARGET_NAME, MAIN_URL_PART);

    public static InputStream getEarlyViewFeed() throws IOException {
        return httpGet(EARLY_VIEW_FEED);
    }

    public static InputStream getIssuesFeed() throws IOException {
        return httpGet(ISSUES_FEED);
    }

    public static ZipInputStream getArticleZip(String doi) throws IOException {
        final String articleZipUrl = String.format(FEED_LINK_TEMPLATE + "article.zip?doi=" + doi,
                SERVER_URL, TARGET_NAME, MAIN_URL_PART);
        return new ZipInputStream(new BufferedInputStream(httpGet(articleZipUrl)));
    }

    public static InputStream getArticle(String doi) throws IOException {
        ZipInputStream zis = getArticleZip(doi);
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains("article.xml")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    return new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray()));
                }
            }
        } finally {
            zis.close();
        }
        return null;
    }

    public static InputStream httpGet(String url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        return new BufferedInputStream(urlConnection.getInputStream());
    }

    public static InputStream httpGet(String url, Map<String, String> properties) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            urlConnection.setRequestProperty(property.getKey(), property.getValue());
        }
        return new BufferedInputStream(urlConnection.getInputStream());
    }
}
