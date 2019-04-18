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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.wiley.android.journalApp.html.Templates;
import com.wiley.wol.client.android.log.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoBrowserActivity extends WebBrowserActivity {
    private static final String TAG = VideoBrowserActivity.class.getSimpleName();
    protected static final String EXTRA_NEED_MODIFY_HTML = "param_need_modify_html";

    public static Intent getStartingIntent(Context context, String videoUrl, boolean needModifyHtml) {
        Intent intent = new Intent(context, VideoBrowserActivity.class);
        intent.putExtra(EXTRA_START_URL, videoUrl);
        intent.putExtra(EXTRA_SHOW_OPEN_IN_BROWSER, false);
        intent.putExtra(EXTRA_NEED_MODIFY_HTML, needModifyHtml);
        return intent;
    }

    protected Templates mTemplates = new Templates();

    protected boolean mNeedModifyHtml = false;
    protected String mModifiedHtml = null;

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        super.initContentView(savedInstanceState);
        mBottomBar.setVisibility(View.GONE);
    }

    @Override
    protected void processIntent() {
        super.processIntent();
        mNeedModifyHtml = getIntent().getBooleanExtra(EXTRA_NEED_MODIFY_HTML, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("modifiedHtml", mModifiedHtml);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mModifiedHtml = savedInstanceState.getString("modifiedHtml");
    }

    @Override
    public void onPause() {
        super.onPause();
        // workaround for video sound bug.
        try {
            Class.forName("android.webkit.WebView")
                    .getMethod("onPause", (Class[]) null)
                    .invoke(mWebView, (Object[]) null);
        } catch(ClassNotFoundException e) {
        } catch(NoSuchMethodException e) {
        } catch(InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }
    }

    @Override
    protected void load() {
        if (mNeedModifyHtml) {
            if (TextUtils.isEmpty(mModifiedHtml)) {
                downloadHtmlAndModify();
            } else {
                loadHtml(mModifiedHtml);
            }
        } else {
            loadUrl();
        }
    }

    protected void loadHtml(String html) {
        mWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    protected void loadUrl() {
        mWebView.loadUrl(mStartingUrl);
    }

    protected void downloadHtmlAndModify() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mCircleProgress.setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... params) {
                final String videoContentPage;
                try {
                    final DefaultHttpClient httpClient = new DefaultHttpClient();
                    final HttpGet httpGet = new HttpGet(mStartingUrl);
                    final HttpResponse httpResponse = httpClient.execute(httpGet);
                    if (httpResponse.getStatusLine().getStatusCode() != 200) {
                        return null;
                    }
                    videoContentPage = EntityUtils.toString(httpResponse.getEntity());
                } catch (IOException e) {
                    Logger.s(TAG, e);
                    return null;
                }
                if (videoContentPage == null)
                    return null;

                Pattern regex = Pattern.compile("<param name=\"flashVars\" value=\"@videoPlayer=([0-9]+)&amp;playerID=([0-9]+)&amp;playerKey=(.+?)&amp;", Pattern.MULTILINE);
                Pattern regexWidth = Pattern.compile("<object.+?width=\"([0-9]+)\"", Pattern.MULTILINE);
                Pattern regexHeight = Pattern.compile("<object.+?height=\"([0-9]+)\"", Pattern.MULTILINE);

                Matcher matches = regex.matcher(videoContentPage);
                Matcher widthMatches = regexWidth.matcher(videoContentPage);
                Matcher heightMatches = regexHeight.matcher(videoContentPage);

                int videoWidth = 320;
                int videoHeight = 240;

                if (widthMatches.find())
                    videoWidth = Integer.parseInt(widthMatches.group(1));
                if (heightMatches.find())
                    videoHeight = Integer.parseInt(heightMatches.group(1));

                if (matches.find()) {
                    String videoPlayer = matches.group(1);
                    String playerID = matches.group(2);
                    String playerKey = matches.group(3);
                    mModifiedHtml = mTemplates.useAssetsTemplate(VideoBrowserActivity.this, "video")
                            .putParam("videoPlayer", videoPlayer)
                            .putParam("playerID", playerID)
                            .putParam("playerKey", playerKey)
                            .putParam("width", videoWidth)
                            .putParam("height", videoHeight)
                            .putParam("margin-left", videoWidth / 2)
                            .putParam("margin-top", videoHeight / 2)
                            .proceed();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                mCircleProgress.setVisibility(View.GONE);
                if (TextUtils.isEmpty(mModifiedHtml))
                    loadUrl();
                else
                    loadHtml(mModifiedHtml);
            }
        };
        task.execute();
    }
}
