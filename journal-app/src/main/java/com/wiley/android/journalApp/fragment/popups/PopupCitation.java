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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;

/**
 * Created by taraskreknin on 10.06.14.
 */
public class PopupCitation extends PopupFragment implements View.OnClickListener {

    public interface CitationListener {
        void onEmailCitation();
    }

    private Button mCopyButton;
    private CitationListener mListener;
    private ArticleMO mArticle;
    @Inject
    private AANHelper aanHelper;
    @Inject
    private ArticleService mArticleService;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (CitationListener) getParentFragment();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_citation, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Button email = findView(R.id.citation_email_button);
        email.setOnClickListener(this);
        mCopyButton = findView(R.id.citation_copy_button);
        mCopyButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.citation_copy_button) {
            copyToClipboard();
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_CITE_COPY,
                    mArticle.getDOI().getValue(),
                    0L);
            {
                aanHelper.trackActionCopyCitation(mArticle);
            }
        } else if (id == R.id.citation_email_button) {
            prepareEmail();
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_CITE_MAIL,
                    mArticle.getDOI().getValue(),
                    0L);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if (args == null) return;
        DOI doi = getArguments().getParcelable(Extras.EXTRA_DOI);
        if (doi != null) {
            showCitation(doi);
        }
    }

    private void prepareEmail() {
        if (mListener != null) {
            mListener.onEmailCitation();
        }
    }

    private void copyToClipboard() {
        final TextView text = findView(R.id.citation_text);
        ClipboardManager clipboard = (ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence cit = text.getText() == null ? new SpannedString("No citation") : text.getText();

        ClipData clip = ClipData.newPlainText("citation", cit);
        clipboard.setPrimaryClip(clip);
        setCopyButtonEnabled(false);
    }

    private void setCopyButtonEnabled(boolean enabled) {
        mCopyButton.setEnabled(enabled);
        final String text = enabled ? getString(android.R.string.copy) : getString(R.string.copied);
        mCopyButton.setText(text);
    }

    private Spanned getSpannedCitation(String citation) {
        if (TextUtils.isEmpty(citation)) {
            return null;
        }
        return Html.fromHtml(citation);
    }

    public void showCitation(DOI doi) {
        mArticle = mArticleService.getArticleQuietly(doi);
        mCopyButton.setEnabled(true);
        mCopyButton.setText(android.R.string.copy);

        final String citation = mArticleService.getArticleCitation(doi);
        final TextView text = findView(R.id.citation_text);
        text.setText(getSpannedCitation(citation));
    }
}