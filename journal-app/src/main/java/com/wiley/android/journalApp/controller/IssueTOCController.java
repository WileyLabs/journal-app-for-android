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

import android.content.Context;

import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.log.Logger;

public class IssueTOCController {
    private static final String TAG = IssueTOCController.class.getSimpleName();
    private final Context context;

    public IssueTOCController(final Context context) {
        this.context = context;
    }

    public void open(final DOI doi) {
        Logger.d(TAG, "Opening issue with " + doi);
        ((MainActivity) context).openIssue(doi);
    }

    public void openNewTask(final DOI doi) {
        Logger.d(TAG, "Opening (NewTask) issue with " + doi);
/*
        final Intent intent = new Intent(context, IssueTOCActivity.class);
        intent.putExtra("DOI", doi);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
*/

        try {
            ((MainActivity) context).openIssue(doi);
        } catch (ClassCastException e) {
            Logger.s(TAG, e);
        }
    }
}
