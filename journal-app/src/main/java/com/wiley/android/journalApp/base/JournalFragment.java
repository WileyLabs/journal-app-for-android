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
package com.wiley.android.journalApp.base;

import android.view.View;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;

import roboguice.fragment.RoboFragment;

/**
 * Created by Andrey Rylov on 04/06/14.
 */
public class JournalFragment extends RoboFragment {

    @Inject
    protected Theme theme;
    @Inject
    private ErrorManager errorManager;

    public JournalActivity getJournalActivity() {
        return (JournalActivity) getActivity();
    }

    public <T extends View> T findView(final int id) {
        final View view = getView();
        return view != null ? (T) view.findViewById(id) : null;
    }

    protected void onSaveArticleError(final ArticleMO article) {
        final MainActivity activity = (MainActivity) getActivity();

        if (article.isRestricted()) {
            errorManager.alertWithErrorCode(getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
        } else {
            final ErrorButton cancelSaveButton = new ErrorButton(getActivity().getString(R.string.cancel_save), new ErrorButton.OnClickListener() {
                @Override
                public void onClick() {
                    activity.cancelPostSaveArticle(article);
                }
            });

            final ErrorButton okButton = new ErrorButton(getActivity().getString(R.string.OK), new ErrorButton.OnClickListener() {
                @Override
                public void onClick() {
                    activity.postSaveArticle(article);
                }
            });

            errorManager.alertWithErrorCode(getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE_TO_SAVE_ARTICLE, okButton, cancelSaveButton);
        }
    }
}
