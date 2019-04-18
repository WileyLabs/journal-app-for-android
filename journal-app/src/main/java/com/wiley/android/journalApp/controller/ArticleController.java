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
import android.content.Intent;
import android.os.Bundle;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.activity.FiguresFragment;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.utils.BundleUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;

import java.util.ArrayList;
import java.util.List;

public class ArticleController {
    private static final String TAG = ArticleController.class.getSimpleName();

    private final Context context;

    @Inject
    public ArticleController(final Context context) {
        this.context = context;
    }

    public void openSingleArticle(final DOI doi) {
        List<DOI> doiList = new ArrayList<DOI>();
        doiList.add(doi);
        openMultiArticle(doiList, 0, context.getString(R.string.title_article));
    }

    public void openMultiArticle(final List<DOI> doiList, final DOI initialDoi, String title) {
        final int initialDoiIndex = doiList.indexOf(initialDoi);
        openMultiArticle(doiList, initialDoiIndex, title);
    }

    public void openMultiArticle(final List<DOI> doiList, final DOI initialDoi, String title, Bundle additionalExtras) {
        final int initialDoiIndex = doiList.indexOf(initialDoi);
        openMultiArticle(doiList, initialDoiIndex, title, additionalExtras);
    }

    public void openMultiArticle(final List<DOI> doiList, final int initialDoiIndex, String title) {
        openMultiArticle(doiList, initialDoiIndex, title, null);
    }

    public void openMultiArticle(final List<DOI> doiList, final int initialDoiIndex, String title, Bundle additionalExtras) {
        final Intent intent = new Intent(context, ArticleViewFragment.class);
        BundleUtils.putParcelableListToIntent(intent, Extras.EXTRA_DOI_LIST, doiList);
        intent.putExtra(Extras.EXTRA_INITIAL_DOI_INDEX, initialDoiIndex);
        intent.putExtra(Extras.EXTRA_TITLE, title);
        if (additionalExtras != null) {
            intent.putExtras(additionalExtras);
        }
        context.startActivity(intent);
    }

    public void openSingleArticleNewTask(final DOI doi, String title) {
        final Intent intent = new Intent(context, ArticleViewFragment.class);
        List<DOI> doiList = new ArrayList<>();
        doiList.add(doi);
        BundleUtils.putParcelableListToIntent(intent, Extras.EXTRA_DOI_LIST, doiList);
        intent.putExtra(Extras.EXTRA_INITIAL_DOI_INDEX, 0);
        intent.putExtra(Extras.EXTRA_TITLE, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void openSavedArticles(final List<DOI> doiList, final DOI initialDoi) {
        Bundle extras = new Bundle();
        extras.putBoolean(Extras.EXTRA_SAVED_ARTICLES, true);
        openMultiArticle(doiList, initialDoi, context.getString(R.string.title_saved_articles), extras);
    }

    public void openEarlyViewArticles(final List<DOI> doiList, final DOI initialDoi) {
        openMultiArticle(doiList, initialDoi, context.getString(R.string.early_view_articles_title));
    }

    public void openIssueArticles(final List<DOI> doiList, final DOI initialDoi, final IssueMO issue) {
        final String title = String.format(context.getString(R.string.issue_volume_title), issue.getVolumeNumber(), issue.getIssueNumber());
        openMultiArticle(doiList, initialDoi, title);
    }

    public void openSpecialSectionArticles(final List<DOI> doiList, final DOI initialDoi, final SpecialSectionMO specialSection) {
        final String title = specialSection.getUnescapedTitle();
        openMultiArticle(doiList, initialDoi, title);
    }

    public void openFigures(DOI articleDoi, int figureId) {
        final Intent intent = new Intent(context, FiguresFragment.class);
        intent.putExtra(Extras.EXTRA_DOI, articleDoi);
        intent.putExtra(Extras.EXTRA_FIGURE_ID, figureId);
        context.startActivity(intent);
    }
}
