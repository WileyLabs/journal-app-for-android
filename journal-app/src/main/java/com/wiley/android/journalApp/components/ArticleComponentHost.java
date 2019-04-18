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

import android.app.Activity;
import android.content.Context;

import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;

/**
 * Created by taraskreknin on 21.07.14.
 */
public interface ArticleComponentHost {
    void onAccessForbiddenArticle();

    void onSaveArticleNoInternetConnection(ArticleMO article);

    StartActivityForResultHelper getStartActivityForResultHelper();

    Activity getActivity();

    Context getContext();

    void onRenderStarted();

    void onRenderCompleted();

    boolean isVisible();
}
