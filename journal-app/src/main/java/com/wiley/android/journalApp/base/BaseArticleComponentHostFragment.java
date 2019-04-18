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

import android.content.Context;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.ArticleComponentHost;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;

import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ARTICLE;

/**
 * Created by taraskreknin on 21.07.14.
 */
public class BaseArticleComponentHostFragment extends JournalFragment implements ArticleComponentHost, StartActivityForResultHelper {
    @Inject
    protected ErrorManager errorManager;
    @Inject
    protected WebController webController;

    @Override
    public void onAccessForbiddenArticle() {
        if (!isResumed()) return;
        errorManager.alertWithErrorCode(
                getActivity(),
                ACCESS_FORBIDDEN_ARTICLE,
                ErrorButton.withTitleAndListener(getActivity().getString(android.R.string.ok), null),
                ErrorButton.withTitleAndListener(getActivity().getString(R.string.get_help), new ErrorButton.OnClickListener() {
                    @Override
                    public void onClick() {
                        webController.openUrlInternal(theme.getHelpUrl());
                    }
                })
        );
    }

    @Override
    public void onSaveArticleNoInternetConnection(final ArticleMO article) {
        if (isResumed()) {
            onSaveArticleError(article);
        }
    }

    @Override
    public StartActivityForResultHelper getStartActivityForResultHelper() {
        return this;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onRenderStarted() {}

    @Override
    public void onRenderCompleted() {}
}
