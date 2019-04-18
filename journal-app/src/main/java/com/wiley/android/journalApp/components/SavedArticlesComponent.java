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

import android.text.TextUtils;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.html.Template;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by taraskreknin on 23.06.14.
 */
public class SavedArticlesComponent extends ArticleRefComponent {

    @Inject
    private ArticleService mArticleService;
    protected SimpleDateFormat mDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
    protected String mCurrentSectionHeader = "";

    public SavedArticlesComponent(ArticleComponentHost host, CustomWebView webView) {
        super(host, webView);
    }

    @Override
    public void render(List<ArticleMO> articles) {
        mCurrentSectionHeader = "";
        super.render(articles);
    }

    @Override
    protected String htmlCodeForListHeading() {
        return super.htmlCodeForListHeading();
    }

    @Override
    protected void openArticles(List<DOI> doiList, DOI doiForOpen) {
        ((MainActivity)componentHost.getActivity()).openArticles(doiList, doiForOpen,
                context.getString(R.string.title_saved_articles), true);
    }

    @Override
    protected String htmlCodeForNoArticlesElement() {
        long saved = mArticleService.getSavedArticleCount();

        final Template noSavedArticlesTemplate = templates.useAssetsTemplate(context, "no_saved_articles");

        final String displayCode = (saved <= 0) ? "" : "display: none";

        return noSavedArticlesTemplate
                .putParam("_no_article_display_placeholder_", displayCode)
                .proceed();
    }

    @Override
    protected String headingBeforeArticle(ArticleMO article) {
        String ret = "";
        Date addedDate = article.getAddedToFavoriteDate();
        String articleDate = addedDate != null ? mDateFormat.format(addedDate) : "";

        String headingClass = "section_heading_class";

        if(!articleDate.equals(mCurrentSectionHeader))
        {
            if(isDivideByDate() || !TextUtils.isEmpty(articleDate))
            {
                ret = String.format("<div class=\"%s\"><p class=\"section_heading_class\">Saved on %s</p></div>", headingClass, articleDate);
            }
            mCurrentSectionHeader = articleDate;
        }
        return ret;
    }

    protected boolean isDivideByDate() {
        return true;
    }
}
