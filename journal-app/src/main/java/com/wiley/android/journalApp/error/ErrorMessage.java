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

import android.text.Spannable;
import android.text.TextUtils;

public class ErrorMessage {

    private final String title;
    private final String message;
    private final Spannable spannableMessage;

    public ErrorMessage(final String title) {
        this(title, (String) null);
    }

    public ErrorMessage(final String title, final String message) {
        this.title = title;
        this.message = message;
        this.spannableMessage = null;
    }

    public ErrorMessage(final String title, final Spannable spannableMessage) {
        this.title = title;
        this.message = null;
        this.spannableMessage = spannableMessage;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(title);
    }

    public String getMessage() {
        return message;
    }

    public Spannable getSpannableMessage() {
        return spannableMessage;
    }

    public static ErrorMessage withMessage(final String msg) {
        return new ErrorMessage(null, msg);
    }

    public static ErrorMessage withTitle(final String title) {
        return new ErrorMessage(title);
    }

    public static ErrorMessage withTitleAndMessage(final String title, final String message) {
        return new ErrorMessage(title, message);
    }

    public static ErrorMessage withTitleAndSpannableMessage(final String title, final Spannable spannableMessage) {
        return new ErrorMessage(title, spannableMessage);
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ErrorMessage that = (ErrorMessage) o;

        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }
        return !(message != null ? !message.equals(that.message) : that.message != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}