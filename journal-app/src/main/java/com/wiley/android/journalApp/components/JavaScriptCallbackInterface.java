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
package com.wiley.android.journalApp.components;

import android.os.Bundle;
import android.webkit.JavascriptInterface;

import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.wol.client.android.log.Logger;

class JavaScriptCallbackInterface {
    private static final String TAG = JavaScriptCallbackInterface.class.getSimpleName();
    private CustomWebView customWebView;
    private final boolean isPhone;
    private String nextBodyInnerHtml = null;
    private Bundle extras = new Bundle();

    JavaScriptCallbackInterface(final CustomWebView customWebView) {
        this.customWebView = customWebView;
        this.isPhone = DeviceUtils.isPhone(customWebView.getContext());
    }

    @JavascriptInterface
    public void handleJavascriptErrors(final String error) {
        Logger.s(TAG, "javaScript error: " + error);
    }

    @JavascriptInterface
    public void onResult(final String handlerId, final String result) {
        customWebView.post(new Runnable() {
            @Override
            public void run() {
                customWebView.onJavaScriptResult(handlerId, result);
            }
        });
    }

    @JavascriptInterface
    public String getNextBodyInnerHtml() {
        return nextBodyInnerHtml;
    }

    void setNextBodyInnerHtml(final String html) {
        this.nextBodyInnerHtml = html;
    }

    protected Bundle getExtras() {
        return extras;
    }

    @JavascriptInterface
    public void setExtraInt(String name, int value) {
        extras.putInt(name, value);
    }

    @JavascriptInterface
    public void setExtraBool(String name, boolean value) {
        extras.putBoolean(name, value);
    }

    @JavascriptInterface
    public boolean isTablet() {
        return !isPhone;
    }

    @JavascriptInterface
    public boolean isPhone() {
        return isPhone;
    }

    @JavascriptInterface
    public String pushTempData(String data) {
        String name = String.format("tempData%d", IdUtils.generateIntId());
        setExtraString(name, data);
        return name;
    }

    @JavascriptInterface
    public String popTempData(String name) {
        String data = extras.getString(name);
        extras.remove(name);
        return data;
    }

    private void setExtraString(String name, String value) {
        extras.putString(name, value);
    }
}
