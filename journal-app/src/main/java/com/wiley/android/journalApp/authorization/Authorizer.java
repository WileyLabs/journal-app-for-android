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
package com.wiley.android.journalApp.authorization;

import android.app.Activity;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;


public class Authorizer {

    public static final int REQUEST_CODE_ADD_TO_FAVORITES = 2358;
    public static final int REQUEST_CODE_GET_PDF = 2359;
    public static final int REQUEST_CODE_DOWNLOAD_ISSUE = 2360;

    @Inject
    private AANHelper aanHelper;
    @Inject
    private Settings settings;
    @Inject
    private ErrorManager errorManager;

    public boolean isAuthorized() {
        return settings.isAuthorized();
    }

    public void requestAccess(Activity activity) {
        {
            aanHelper.trackActionLaunchGetAccessDialogue("Get Access Click");
        }
        ((MainActivity) activity).openGetAccessDialog();
    }

    public void requestAccessFromFavoriteAction(StartActivityForResultHelper helper, DOI doi) {
        assert(helper != null);
        {
            aanHelper.trackActionLaunchGetAccessDialogue("Article Save Denied");
        }

        if (!NetUtils.isOnline(helper.getActivity())) {
            errorManager.alertWithErrorCode(helper.getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
            return;
        }

        ((MainActivity) helper.getActivity()).openGetAccessDialog(REQUEST_CODE_ADD_TO_FAVORITES,
                helper.getActivity().getString(R.string.a_subscription_is_required_article), doi);
    }

    public void requestAccessFromGetPdfAction(StartActivityForResultHelper helper, DOI doi) {
        assert(helper != null);
        {
            aanHelper.trackActionLaunchGetAccessDialogue("Get PDF Denied");
        }
        ((MainActivity) helper.getActivity()).openGetAccessDialog(REQUEST_CODE_GET_PDF,
                helper.getActivity().getString(R.string.a_subscription_is_required_pdf), doi);
    }

    public void requestAccessFromDownloadIssueAction(StartActivityForResultHelper helper, DOI doi) {
        assert(helper != null);
        {
            aanHelper.trackActionLaunchGetAccessDialogue("Issue Download Denied");
        }
        ((MainActivity) helper.getActivity()).openGetAccessDialog(REQUEST_CODE_DOWNLOAD_ISSUE,
                helper.getActivity().getString(R.string.a_subscription_is_required_issue_download), doi);
    }

    public void refresh() {
        settings.refreshAccount();
    }
}
