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
package com.wiley.android.journalApp.error;

public class ErrorButton {

    private final String title;

    private final OnClickListener onClickListener;

    public ErrorButton(final String title, final OnClickListener onClickListener) {
        this.title = title;
        this.onClickListener = onClickListener;
    }

    public String getTitle() {
        return title;
    }

    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    public static ErrorButton withTitleAndListener(final String title, final OnClickListener onClickListener) {
        return new ErrorButton(title, onClickListener);
    }

    public interface OnClickListener {
        void onClick();
    }
}