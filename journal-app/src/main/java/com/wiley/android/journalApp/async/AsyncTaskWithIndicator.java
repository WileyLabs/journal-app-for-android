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
package com.wiley.android.journalApp.async;

import android.app.ProgressDialog;
import android.content.Context;
import com.wiley.android.journalApp.R;
import roboguice.util.RoboAsyncTask;

public abstract class AsyncTaskWithIndicator<Result> extends RoboAsyncTask<Result> {
    private final ProgressDialog progressDialog;

    protected AsyncTaskWithIndicator(final Context context) {
        this(context, R.string.progressMessage);
    }

    protected AsyncTaskWithIndicator(final Context context, final int msgResId) {
        super(context);
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(msgResId));
    }

    @Override
    protected void onPreExecute() throws Exception {
        super.onPreExecute();
        progressDialog.show();
    }

    @Override
    protected void onSuccess(final Result result) throws Exception {
        super.onSuccess(result);
        progressDialog.dismiss();
    }
}
