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

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.os.Bundle;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EarlyViewComponent extends ArticleRefComponent {

    public EarlyViewComponent(final ArticleComponentHost host, final CustomWebView webView) {
        super(host, webView);
    }

    @Override
    protected void openArticles(List<DOI> doiList, DOI doiForOpen) {
        ((MainActivity)componentHost.getActivity()).openArticles(doiList, doiForOpen,
                context.getString(R.string.early_view_articles_title), false);
    }

    protected SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);

    protected String currentSectionHeader = null;

    @Override
    protected String headingBeforeArticle(final ArticleMO article) {
        String result = null;
        final String headingClass = "section_heading_class";
        final String publicationDate = article.getPublicationDate() != null ? dateFormat.format(article.getPublicationDate()) : "";
        if (!publicationDate.equals(currentSectionHeader)) {
            if (isDivideByDate()) {
                result = String.format("<div class=\"%s\"><p class=\"section_heading_class\">%s</p></div>", headingClass, publicationDate);
            }
            currentSectionHeader = publicationDate;
        }
        return result;
    }

    protected boolean isDivideByDate() {
        return true;
    }

    public void update() {
        currentSectionHeader = null;
        final LoaderManager lm = componentHost.getActivity().getLoaderManager();
        if (lm.getLoader(LoaderId_UpdateEVList) != null) {
            lm.restartLoader(LoaderId_UpdateEVList, null, loaderCallbacks);
        } else {
            lm.initLoader(LoaderId_UpdateEVList, null, loaderCallbacks);
        }
    }

    private static final int LoaderId_UpdateEVList = IdUtils.generateIntId();

    private LoaderManager.LoaderCallbacks<List<ArticleMO>> loaderCallbacks = new LoaderManager.LoaderCallbacks<List<ArticleMO>>() {
        @Override
        public Loader<List<ArticleMO>> onCreateLoader(int id, Bundle args) {
            return new AsyncTaskLoader<List<ArticleMO>>(componentHost.getContext()) {
                @Override
                public List<ArticleMO> loadInBackground() {
                    return articleService.getArticlesForEarlyView();
                }

                @Override
                protected void onStartLoading() {
                    super.onStartLoading();
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<ArticleMO>> loader, List<ArticleMO> data) {
            if (!data.isEmpty()) {
                render(data);
            } else {
                componentHost.onRenderCompleted();
            }
            componentHost.getActivity().getLoaderManager().destroyLoader(LoaderId_UpdateEVList);
        }

        @Override
        public void onLoaderReset(Loader<List<ArticleMO>> loader) {}
    };

}
