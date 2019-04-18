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
package com.wiley.android.journalApp.fragment.popups;

import android.animation.ValueAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.ArticleComponent;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.journalApp.theme.Theme;

import javax.inject.Inject;

public class PopupAuthorsInfo extends JournalFragment {

    private ContentState mContentState = ContentState.None;

    public enum ContentState {
        None,
        Closed,
        Open
    }

    private static final String SCHEME_OPEN_BIO = "showbiography://";
    private static final int ANIMATION_DURATION_MSC = 300;

    private static final Interpolator sInterpolatorAcc = new AccelerateInterpolator();
    private static final Interpolator sInterpolatorDec = new DecelerateInterpolator();

    private final Templates mTemplates = new Templates();
    private WebView mWebView;
    private CustomWebView mArticleWebView;
    private ViewGroup mRootView;
    private ViewGroup mSliderRootView;

    private ValueAnimator mAnimator;

    @Inject
    private WebController mWebController;
    @Inject
    private Theme mTheme;
    @Inject
    private EmailSender emailSender;
    @Inject
    protected AANHelper aanHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_authors_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWebView = findView(R.id.authors_info_web_view);
        mRootView = findView(R.id.authors_info_root);
        mSliderRootView = findView(R.id.authors_info_slider_root);
        final ImageView hide = findView(R.id.article_authors_close_button);
        if (hide != null) {
            hide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hide(true);
                }
            });
            int hideImgRes = mTheme.isJournalHasDarkBackground() ? R.drawable.close_button_light_selector : R.drawable.close_button_dark_selector;
            hide.setImageResource(hideImgRes);
        }
        final ViewGroup titleBack = findView(R.id.authors_info_title_back);
        if (titleBack != null) {
            titleBack.setBackgroundColor(mTheme.getMainColor());
        }
        setupWebView();
        loadContent();

        if (DeviceUtils.isPhone(getActivity())) {
            hide(false);
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            ArticleComponent articleComponent = getArticleComponent();
            if (articleComponent != null) {
                lp.width = articleComponent.getAuthorsWebElementWidth();
                view.setLayoutParams(lp);

                articleComponent.scrollAuthorsInformationWebElement();
            }
        }
        GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                GANHelper.ACTION_AUTHORS,
                "",
                0L);
    }

    private void loadContent() {
        final ArticleComponent ac = getArticleComponent();
        if (ac == null) {
            return;
        }
        final ArticleMO article = getArticle();
        if (article == null) {
            return;
        }
        final String mimeType = "text/html";
        final String encoding = "utf-8";
        String affiliationBlock = article.getAffiliationBlock();
        if (TextUtils.isEmpty(affiliationBlock)) {
            affiliationBlock = "No data available"; // should not happen, usually
        }

        final String html = HtmlUtils.detectLinks(mTemplates.useAssetsTemplate(getActivity(), "affiliation_block")
                .putParam("affiliation_block", affiliationBlock)
                .proceed());

        getArticleComponent().checkHasBiographySection(new Runnable() {
            @Override
            public void run() {
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        String bioHtml = html;
                        bioHtml = bioHtml.replace("</body>", "<a href='showBiography://'>View Author Biographies</a></body>");
                        mWebView.loadDataWithBaseURL(null, bioHtml + "", mimeType, encoding, null);
                    }
                });
            }
        }, new Runnable() {
            @Override
            public void run() {
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.loadDataWithBaseURL(null, html, mimeType, encoding, null);
                    }
                });
            }
        });
    }

    private void setupWebView() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (TextUtils.isEmpty(url)) {
                    return true;
                }
                if (url.startsWith(SCHEME_OPEN_BIO)) {
                    getArticleFragment().onNeedScrollToBioSection();
                    return true;
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    mWebController.openUrlInternal(url);
                    return true;
                }
                if (url.startsWith("mailto:")) {
                    aanHelper.trackActionOpenEmailClient();
                    emailSender.sendEmailText(getActivity(), "", "", "", "", url.replace("mailto:", ""));
                }
                return true;
            }
        });
    }

    public int getUsefulContentHeight() {
        return mSliderRootView.getMeasuredHeight() != 0 ? mSliderRootView.getMeasuredHeight() : getResources().getDimensionPixelSize(R.dimen.article_authors_info_view_phone_height);
    }

    private ArticleMO getArticle() {
        return getArticleComponent().getLoadedArticle();
    }

    private ArticleComponent getArticleComponent() {
        return ((ArticleViewFragment) ((Fragment) this).getParentFragment()).getCurrentArticleComponent();
    }

    private ArticleViewFragment getArticleFragment() {
        return (ArticleViewFragment) ((Fragment) this).getParentFragment();
    }

    public void show(boolean animated) {
        show(animated, null, null);
    }

    public void show(boolean animated, CustomWebView webView, Rect authorsContentRectPx) {
        if (mContentState == ContentState.Open) {
            return;
        }
        mArticleWebView = webView;
        if (mArticleWebView != null) {
            int newContentHeightDp = UIUtils.pxToDp(getActivity(), authorsContentRectPx.top + authorsContentRectPx.height() + getUsefulContentHeight());
            mArticleWebView.setContentHeightLimitDp(newContentHeightDp);
        }

        loadContent();
        if (animated) {
            mContentState = ContentState.Open;
            mRootView.setVisibility(View.VISIBLE);
            doScroll(getTotalHeightPx(), getYForOpenedStatePx());
            return;
        }
        setPanelYPosition(getYForOpenedStatePx());
        mRootView.setVisibility(View.VISIBLE);
    }

    public void hide(boolean animated) {
        if (mContentState == ContentState.Closed) {
            return;
        }
        if (mArticleWebView != null) {
            mArticleWebView.resetContentHeightLimit();
            mArticleWebView = null;
        }
        mContentState = ContentState.Closed;
        if (animated) {
            doScroll(getYForOpenedStatePx(), getTotalHeightPx());
            return;
        }
        setPanelYPosition(getTotalHeightPx());
        mRootView.setVisibility(View.INVISIBLE);
    }

    private int getTotalHeightPx() {
        int h = getView().getMeasuredHeight();
        if (h == 0) {
            Point dispSizePx = UIUtils.getDisplaySizePx(getActivity());
            h = dispSizePx.y;
        }
        return h;
    }

    private int getYForOpenedStatePx() {
        return getTotalHeightPx() - getUsefulContentHeight();
    }

    private void doScroll(final int from, final int to) {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }

        final boolean isHiding = from < to;

        mAnimator = ValueAnimator.ofInt(from, to);
        mAnimator.setDuration(ANIMATION_DURATION_MSC);
        mAnimator.setInterpolator(isHiding ? sInterpolatorAcc : sInterpolatorDec);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int val = (Integer) animation.getAnimatedValue();
                setPanelYPosition(val);
                if (val == to && isHiding) {
                    mRootView.setVisibility(View.INVISIBLE);
                }
            }
        });
        mAnimator.start();
    }

    private void setPanelYPosition(int yPosPx) {
        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) mSliderRootView.getLayoutParams();
        lp.y = yPosPx;
        mSliderRootView.setLayoutParams(lp);
    }

    public boolean isOpened() {
        return mContentState == ContentState.Open;
    }

}
