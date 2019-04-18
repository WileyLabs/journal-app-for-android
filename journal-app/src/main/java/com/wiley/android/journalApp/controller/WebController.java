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
package com.wiley.android.journalApp.controller;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.View;

import com.google.inject.Inject;
import com.wiley.android.journalApp.activity.WebBrowserActivity;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.log.Logger;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Andrey Rylov on 06/05/14.
 */
public class WebController {
    private final static String TAG = WebController.class.getSimpleName();
    protected final Context context;

    @Inject
    private AANHelper aanHelper;
    @Inject
    public WebController(Context context) {
        this.context = context.getApplicationContext();
    }

    public Spanned assignSpannableUrlInternalHandlers(Spanned spanned) {
        SpannableStringBuilder spannable = SpannableStringBuilder.valueOf(spanned);
        URLSpan[] urlSpans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan urlSpan : urlSpans) {
            int spanStart = spannable.getSpanStart(urlSpan);
            int spanEnd = spannable.getSpanEnd(urlSpan);
            int spanFlags = spannable.getSpanFlags(urlSpan);
            String url = urlSpan.getURL();
            spannable.removeSpan(urlSpan);
            spannable.setSpan(new URLSpanInternal(url), spanStart, spanEnd, spanFlags);
        }
        return spannable;
    }

    private class URLSpanInternal extends URLSpan {
        public URLSpanInternal(String url) {
            super(url);
        }

        public URLSpanInternal(Parcel src) {
            super(src);
        }

        @Override
        public void onClick(View widget) {
            {
                aanHelper.trackActionOpenWebViewerForOtherPage(getURL());
            }
            openUrlInternal(getURL());
        }
    }

    public void openUrlInternal(String url) {
        Intent intent = WebBrowserActivity.getStartingIntent(context, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void openUrlExternal(String url) {
        Intent intent = makeUrlExternalIntent(url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public boolean openUrlExternalIfCan(String url) {
        if (!canOpenUrlExternal(url))
            return false;
        try {
            openUrlExternal(url);
            return true;
        } catch (ActivityNotFoundException e) {
            Logger.d(TAG, e.getMessage(), e);
            return false;
        }
    }

    private HashMap<String, Boolean> checkedShemas = new HashMap<String, Boolean>();

    public boolean canOpenUrlExternal(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (checkedShemas.containsKey(scheme))
            return checkedShemas.get(scheme).booleanValue();

        Intent intent = makeUrlExternalIntent(url);
        PackageManager pm =context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        boolean can = activities != null && !activities.isEmpty();

        checkedShemas.put(scheme, can);
        return can;
    }

    public Intent makeUrlExternalIntent(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        return intent;
    }
}
