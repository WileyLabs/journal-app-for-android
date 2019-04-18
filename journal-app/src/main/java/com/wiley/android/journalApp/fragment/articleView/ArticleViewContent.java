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
package com.wiley.android.journalApp.fragment.articleView;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.ArticleComponent;
import com.wiley.android.journalApp.components.ArticleComponentHost;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.FloatingStar;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.widget.CircleIndexIndicator;
import com.wiley.android.journalApp.widget.IndexPopupHost;
import com.wiley.wol.client.android.data.manager.AdvertisementManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.log.Logger;

public class ArticleViewContent extends JournalFragment {

    private static final String TAG = ArticleViewContent.class.getSimpleName();

    public interface Host {

        void onArticleViewContentReady(ArticleViewContent sender);
    }

    @Inject
    private AANHelper aanHelper;
    @Inject
    private ArticleService articleService;
    @Inject
    private Authorizer authorizer;
    @Inject
    private AdvertisementManager advertisementManager;

    private CustomWebView webView;
    private CircleIndexIndicator indexIndicator;
    private IndexPopupHost indexPopup;
    private ArticleComponent articleComponent;
    private View progressBar;
    private FloatingStar floatingStar;

    private DOI doi = null;
    private boolean articleShowed = false;
    private boolean articleLoading = false;
    private ArticleComponentHost articleComponentHost;

    public CustomWebView getWebView() {
        return this.webView;
    }

    public CircleIndexIndicator getIndexIndicator() {
        return indexIndicator;
    }

    public IndexPopupHost getIndexPopupHost() {
        return indexPopup;
    }

    public ArticleComponent getArticleComponent() {
        return articleComponent;
    }

    public boolean isArticleShowed() {
        return articleShowed;
    }

    public boolean isArticleLoading() {
        return articleLoading;
    }

    public DOI getDoi() {
        return doi;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.doi = getArguments().getParcelable(Extras.EXTRA_DOI);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_article_view_content, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.webView = findView(R.id.article_content);
        this.indexIndicator = findView(R.id.index_indicator);
        this.indexPopup = findView(R.id.index_popup);
        this.progressBar = findView(R.id.progress);
        if (DeviceUtils.isPhone(getActivity())) {
            this.floatingStar = findView(R.id.article_floating_star);
            this.floatingStar.setVisibility(View.VISIBLE);
        }
        indexIndicator.setPopupCallback(indexPopup);

        articleComponentHost = (ArticleComponentHost) getParentFragment();
        this.articleComponent = new ArticleComponent(articleComponentHost,
                webView, indexIndicator, floatingStar, progressBar);
    }

    @Override
    public void onStart() {
        Logger.d(TAG, "onStart()");
        super.onStart();
        if (!isArticleShowed() || isArticleLoading())
            showProgress();

        getHost().onArticleViewContentReady(this);
        articleComponent.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        articleComponent.onStop();
    }

    public void loadAndShowArticle() {
        Logger.d(TAG, "loadAndShowArticle()");
        if (this.doi == null) {
            articleComponent.showArticle(null, false);
            return;
        }

        final ArticleMO article = articleService.getArticle(doi);
        hideProgress();

        articleLoading = false;

        if (!article.isRestricted() && !articleService.isDownloaded(doi)) {
            showProgress();
            articleLoading = true;
        }

        articleComponent.showArticle(article, articleLoading);
        articleShowed = true;

        if (!article.isRestricted()) {
            String page;
            if (null != article.getSection()) {
                page = "article/v" + article.getSection().getIssue().getVolumeNumber()
                        + "." + article.getSection().getIssue().getIssueNumber()
                        + "/" + article.getDOI().getValue();
            } else {
                page = "/article/early-view/" + article.getDOI().getValue();
            }
            GANHelper.trackPageView(page, true);
        }
        {
            aanHelper.trackArticleView(article);
        }
    }

    protected Handler hideProgressHandler = new Handler();

    protected void showProgress() {
        hideProgressHandler.removeCallbacksAndMessages(null);
        progressBar.clearAnimation();
        progressBar.setAlpha(1.0f);
        progressBar.setVisibility(View.VISIBLE);
    }

    protected void hideProgress() {
        hideProgressHandler.removeCallbacksAndMessages(null);
        hideProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                fadeOut.setDuration(500);
                progressBar.startAnimation(fadeOut);
            }
        }, 200);
    }

    protected Host getHost() {
        return (Host) articleComponentHost;
    }

    public void onArticleUpdated() {
        hideProgress();
        loadAndShowArticle();
    }

    public void onArticleUpdateError() {
        hideProgress();
    }

    public void onNeedArticle() {
        if (!isArticleShowed() || isArticleLoading())
            loadAndShowArticle();
    }

    public void scrollToFigure(FigureMO figure) {
        articleComponent.scrollToFigure(figure);
    }

    public void onRemovedFromPager() {
        articleShowed = false;
        articleLoading = false;
    }

    public void markArticleAsReadIfNeeded() {
        this.articleComponent.markArticleAsReadIfNeeded();
    }
}
