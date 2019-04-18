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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.android.journalApp.controller.WebController;

public class AlertDialogActivity extends JournalActivity {

    public static final int FLAG_SHOW_CLOSE_BUTTON = 1;
    public static final int FLAG_SHOW_MORE_INFO = 2;

    private static final String PARAM_TITLE = "title";
    private static final String PARAM_TEXT = "text";
    private static final String PARAM_FLAGS = "flags";

    @Inject
    private WebController webController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        String title = getIntent().getStringExtra(PARAM_TITLE);
        String text = getIntent().getStringExtra(PARAM_TEXT);
        int flags = getIntent().getIntExtra(PARAM_FLAGS, 0);

        boolean showClose = (flags & FLAG_SHOW_CLOSE_BUTTON) == FLAG_SHOW_CLOSE_BUTTON;
        boolean showMoreInfo = (flags & FLAG_SHOW_MORE_INFO) == FLAG_SHOW_MORE_INFO;

        View buttonClose = findView(R.id.close);
        buttonClose.setVisibility(showClose ? View.VISIBLE : View.GONE);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findView(R.id.alert_more_label).setVisibility(showMoreInfo ? View.VISIBLE : View.GONE);
        View moreLink = findView(R.id.alert_more_link);
        moreLink.setVisibility(showMoreInfo ? View.VISIBLE : View.GONE);
        moreLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webController.openUrlInternal("http://olabout.wiley.com/go/apps");
            }
        });

        findView(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });

        TextView titleTextView = findView(R.id.title);
        titleTextView.setText(title);

        TextView textTextView = findView(R.id.text);
        textTextView.setText(linkify(text));
        textTextView.setLinksClickable(true);
        textTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private CharSequence linkify(String html) {
        Spanned spanned = Html.fromHtml(html.replace("\n", "<br/>"));
        Spannable spannable = SpannableString.valueOf(spanned);
        URLSpan[] urlSpans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan urlSpan : urlSpans) {
            int spanStart = spannable.getSpanStart(urlSpan);
            int spanEnd = spannable.getSpanEnd(urlSpan);
            int spanFlags = spannable.getSpanFlags(urlSpan);
            URLSpanEx urlSpanEx = new URLSpanEx(urlSpan.getURL());
            spannable.removeSpan(urlSpan);
            spannable.setSpan(urlSpanEx, spanStart, spanEnd, spanFlags);
        }
        return spannable;
    }

    private class URLSpanEx extends URLSpan {

        public URLSpanEx(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            webController.openUrlInternal(getURL());
        }
    }

    private static Intent createIntentForShow(Context context, String title, String text, int flags) {
        Intent intent = new Intent(context, AlertDialogActivity.class);
        intent.putExtra(PARAM_TITLE, title);
        intent.putExtra(PARAM_TEXT, text);
        intent.putExtra(PARAM_FLAGS, flags);
        return intent;
    }

    public static void show(Activity from, String title, String text, int flags) {
        Intent intent = createIntentForShow(from, title, text, flags);
        from.startActivity(intent);
    }

    public static void show(Activity from, String title, String text) {
        show(from, title, text, 0);
    }

    public static void showForResult(Activity from, int requestCode, String title, String text, int flags) {
        Intent intent = createIntentForShow(from, title, text, flags);
        from.startActivityForResult(intent, requestCode);
    }

    public static void showForResult(Activity from, int requestCode, String title, String text) {
        showForResult(from, requestCode, title, text, 0);
    }
}
