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

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.utils.BitmapUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.journalApp.theme.ColorUtils;


public class LeftBarArticleView extends JournalFragment {

    @Inject
    private ArticleService mArticleService;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.article_view_bar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyImageViewDisabledState(R.id.article_bar_info_img, R.drawable.article_bar_info);
        applyImageViewDisabledState(R.id.article_bar_figures_img, R.drawable.article_bar_figures);
        applyImageViewDisabledState(R.id.article_bar_refs_img, R.drawable.article_bar_references);
        applyImageViewDisabledState(R.id.article_bar_support_info_img, R.drawable.article_bar_supporting_information);
        applyImageViewDisabledState(R.id.article_bar_find_img, R.drawable.article_bar_search);
        applyImageViewDisabledState(R.id.article_bar_citation_img, R.drawable.article_bar_citation);
        applyImageViewDisabledState(R.id.article_bar_email_img, R.drawable.article_bar_email);
        applyImageViewDisabledState(R.id.article_bar_pdf_img, R.drawable.article_bar_get_pdf);

        applyTextViewDisabledState(R.id.article_bar_info_text);
        applyTextViewDisabledState(R.id.article_bar_figures_text);
        applyTextViewDisabledState(R.id.article_bar_refs_text);
        applyTextViewDisabledState(R.id.article_bar_support_info_text);
        applyTextViewDisabledState(R.id.article_bar_find_text);
        applyTextViewDisabledState(R.id.article_bar_citation_text);
        applyTextViewDisabledState(R.id.article_bar_email_text);
        applyTextViewDisabledState(R.id.article_bar_pdf_text);

        if (DeviceUtils.isTablet(getActivity())) {
            findView(R.id.articleBarCitationButton).setVisibility(View.GONE);
            findView(R.id.articleBarEmailButton).setVisibility(View.GONE);
        }
    }

    public void updateUi(final DOI doi) {
        final TextView pdfView = findView(R.id.article_bar_pdf_text);
        boolean hasPdf = mArticleService.hasArticlePdf(doi);
        if (!hasPdf) {
            pdfView.setText(R.string.get_PDF);
            // setEnabled - false
        } else {
            pdfView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (null != getActivity()) {
                        pdfView.setText(getString(R.string.get_PDF) + "\n" + "(" + mArticleService.getPdfSize(doi) + "MB)");
                    }
                }
            }, 100);
        }
    }

    private void applyImageViewDisabledState(int id, int imageId) {
        ImageView imageView = findView(id);
        if (imageView == null)
            return;
        applyImageViewDisabledState(imageView, imageId);
    }

    private void applyImageViewDisabledState(ImageView imageView, int imageId) {
        float disabledAlphaFactor = DeviceUtils.isTablet(getActivity()) ? 0.35f : 0.5f;
        int disabledAlpha = (int) (disabledAlphaFactor * 255.0f);

        Bitmap normalBitmap = BitmapUtils.loadResource(getActivity(), imageId);
        BitmapDrawable normalDrawable = BitmapUtils.makeDrawable(getActivity(), normalBitmap);

        Bitmap disabledBitmap = BitmapUtils.loadResource(getActivity(), imageId);
        disabledBitmap = BitmapUtils.applyAlpha(disabledBitmap, disabledAlpha);
        BitmapDrawable disabledDrawable = BitmapUtils.makeDrawable(getActivity(), disabledBitmap);

        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[] {-android.R.attr.state_enabled}, disabledDrawable);
        drawable.addState(new int[] {}, normalDrawable);

        imageView.setImageDrawable(drawable);
    }

    private void applyTextViewDisabledState(int id) {
        TextView textView = findView(id);
        if (textView == null)
            return;
        applyTextViewDisabledState(textView);
    }

    private void applyTextViewDisabledState(TextView textView) {
        float disabledAlphaFactor = DeviceUtils.isTablet(getActivity()) ? 0.35f : 0.5f;
        int disabledAlpha = (int) (disabledAlphaFactor * 255.0f);

        int normalColor = Color.WHITE;
        int disabledColor = ColorUtils.changeAlpha(normalColor, disabledAlpha);

        ColorStateList colors = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_enabled},
                        new int[] {}
                },
                new int[] {
                        disabledColor,
                        normalColor
                }
        );
        textView.setTextColor(colors);
    }
}
