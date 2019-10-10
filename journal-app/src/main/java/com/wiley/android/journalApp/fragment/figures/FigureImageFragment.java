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
package com.wiley.android.journalApp.fragment.figures;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.controller.FigureViewerImageLoaderHelper;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.AssetsUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.ArticleHtmlUtils;
import com.wiley.wol.client.android.journalApp.theme.Theme;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class FigureImageFragment extends FigureFragment {
    private final static String TAG = FigureImageFragment.class.getSimpleName();

    @Inject
    private AANHelper aanHelper;
    @Inject
    protected FigureViewerImageLoaderHelper imageLoaderHelper;
    @Inject
    protected Theme theme;
    @Inject
    protected WebController webController;
    @Inject
    protected EmailSender emailSender;

    protected final Templates templates = new Templates();

    public static FigureImageFragment newInstance(int figureIndex) {
        FigureImageFragment fragment = new FigureImageFragment();
        Bundle args = new Bundle();
        args.putInt(Argument_FigureIndex, figureIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_figure_image, container, false);
    }

    private PhotoView photoView;
    private CustomWebView captionView;
    private ImageView logoView;
    private boolean hasCaption = false;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageLoaderHelper.setDrawBorder(true);
        imageLoaderHelper.setInitialScale(0.75f);
        imageLoaderHelper.clearCache();

        photoView = findView(R.id.photo_view);
        photoView.setOnViewTapListener(photoViewOnTapListener);
        String url = "file:/" + figure.getOriginalLocal();
        imageLoaderHelper.displayImage(url, photoView);

        captionView = findView(R.id.caption_view);
        captionView.setWebViewClient(captionViewClient);

        String caption = figure.getCaption();
        if (TextUtils.isEmpty(caption)) {
            hasCaption = false;
            captionView.setVisibility(View.GONE);
        } else {
            hasCaption = true;
            captionView.setVisibility(getHost_().isFullscreen() ? View.GONE : View.VISIBLE);
            caption = ArticleHtmlUtils.expandLinksInFigureCaption(caption, figure, theme.isJournalHasNoReferenceNumbers());
            String html = templates.useAssetsTemplate(getActivity(), DeviceUtils.isTablet(getActivity()) ? "figure_caption_ipad" : "figure_caption_iphone")
                    .putParam("caption_body", caption)
                    .proceed();
            captionView.loadData(html);
        }

        logoView = findView(R.id.logo);
        if (DeviceUtils.isPhone(getActivity())) {
            logoView.setVisibility(View.VISIBLE);
            String filename = HtmlUtils.getAssetsHtmlFilename("Images/ArticleLogo/article_top_logo.png");
            AssetsUtils.showBitmap(logoView, filename, DisplayMetrics.DENSITY_XHIGH);
        } else {
            logoView.setVisibility(View.GONE);
        }
    }

    private WebViewClient captionViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http")) {
                webController.openUrlInternal(url);
                return true;
            } else if (url.startsWith("openfig")) {
                Uri uri = Uri.parse(url);
                String figureShortCaption = uri.getHost();
                getHost_().openFigureByShortCaption(figureShortCaption);
                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }
    };

    private PhotoViewAttacher.OnViewTapListener photoViewOnTapListener = new PhotoViewAttacher.OnViewTapListener() {
        @Override
        public void onViewTap(View view, float x, float y) {
            getHost_().toggleUiFullscreen();
        }
    };

    @Override
    public void onUiFullscreenChanged(boolean fullscreen) {
        super.onUiFullscreenChanged(fullscreen);

        if (!hasCaption) {
            captionView.setVisibility(View.GONE);
        } else {
            if (fullscreen) {
                Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_down);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        captionView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                captionView.startAnimation(animation);
            } else {
                Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_up);
                captionView.setVisibility(View.VISIBLE);
                captionView.startAnimation(animation);
            }
        }
    }
}
