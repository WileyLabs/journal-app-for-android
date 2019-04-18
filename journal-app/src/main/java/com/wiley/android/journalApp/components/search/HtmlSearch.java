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
package com.wiley.android.journalApp.components.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrey Rylov on 26/05/14.
 */
public class HtmlSearch {

    public static class Result {
        public List<String> items = new ArrayList<String>();
        public String highlightedHtml;
    };

    public boolean addAnchor = false;
    public String anchorOpenTag = "";
    public String anchorCloseTag = "";
    public String highlightOpenTag = "<span class=\"highlighted_text\">";
    public String highlightCloseTag = "</span>";
    public int highlightingStartIndex = 0;

    public Result find(String html, String pattern) {
        Callback callback = new Callback();
        nativeFind(html, pattern,
                addAnchor, anchorOpenTag, anchorCloseTag,
                highlightOpenTag, highlightCloseTag, highlightingStartIndex,
                callback);
        return callback.currentResult;
    };

    protected static class Callback {
        public Result currentResult = new Result();

        public void clearResult() {
            currentResult = new Result();
        }

        public void addItem(String item) {
            currentResult.items.add(item);
        }

        public void assignHtml(String html) {
            currentResult.highlightedHtml = html;
        }
    }

    protected static native void nativeFind(String html, String pattern,
                                            boolean addAnchor, String anchorOpenTag, String anchorCloseTag,
                                            String highlightOpenTag, String highlightCloseTag, int highlightingStartIndex,
                                            Callback callback);

    static {
        System.loadLibrary("journal-native-search");
    }
}
