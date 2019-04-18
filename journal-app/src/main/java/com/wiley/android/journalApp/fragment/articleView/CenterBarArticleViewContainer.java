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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.fragment.popups.PopupCitation;
import com.wiley.wol.client.android.domain.DOI;

public class CenterBarArticleViewContainer extends JournalFragment {

    private static final String CENTER_FRAG_TAG = "CenterBarListFragment_TAG";
    private boolean mNotifyAboutOnRootClick = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_center_bar_article_container, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ViewGroup root = findView(R.id.center_bar_container);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRootClick();
            }
        });
    }

    private void onRootClick() {
        if (mNotifyAboutOnRootClick) {
            ((ArticleViewFragment) getParentFragment()).hideSideMenu();
        }
    }

    public void showArticleInfoContent(BaseAdapter infoAdapter) {
        mNotifyAboutOnRootClick = false;
        getListFragment().showArticleInfoContent(infoAdapter);
    }

    public void showPlaceHolderWithMessage(String msg) {
        mNotifyAboutOnRootClick = false;
        getListFragment().showPlaceholderWithMessage(msg);
    }

    public void showArticleFiguresContent(BaseAdapter figuresAdapter) {
        mNotifyAboutOnRootClick = false;
        getListFragment().showArticleFiguresContent(figuresAdapter);
    }

    public void showReferencesContent(BaseAdapter referencesAdapter) {
        mNotifyAboutOnRootClick = false;
        getListFragment().showReferencesContent(referencesAdapter);
    }

    public void showArticleSupportingInfoContent(BaseAdapter supportingInfoAdapter) {
        mNotifyAboutOnRootClick = false;
        getListFragment().showArticleSupportingInfoContent(supportingInfoAdapter);
    }

    public void showCitation(DOI doi) {
        mNotifyAboutOnRootClick = true;
        getCitationFragment().showCitation(doi);
    }

    public void showSearcher(CustomWebView webView) {
        mNotifyAboutOnRootClick = false;
        getListFragment().showSearcherContent(webView);
    }

    private PopupCitation getCitationFragment() {
        checkFragmentIsActive(PopupCitation.class);
        return (PopupCitation) getFragmentManager().findFragmentByTag(CENTER_FRAG_TAG);
    }

    public CenterBarListFragment getListFragment() {
        checkFragmentIsActive(CenterBarListFragment.class);
        return (CenterBarListFragment) getFragmentManager().findFragmentByTag(CENTER_FRAG_TAG);
    }

    private void checkFragmentIsActive(Class<?> neededClass) {
        Fragment frag = getFragmentManager().findFragmentByTag(CENTER_FRAG_TAG);
        if (frag != null && neededClass.equals(frag.getClass())) {
            return;
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.center_bar_container, createFragment(neededClass), CENTER_FRAG_TAG)
                .commit();
        getFragmentManager().executePendingTransactions();
    }

    private Fragment createFragment(Class<?> neededClass) {
        if (neededClass.equals(CenterBarListFragment.class)) {
            return new CenterBarListFragment();
        }
        if (neededClass.equals(PopupCitation.class)) {
            return new PopupCitation();
        }
        return null;
    }

    public void clear() {
        Fragment f = getFragmentManager().findFragmentByTag(CENTER_FRAG_TAG);
        if (f == null) return;
        getFragmentManager().beginTransaction()
                .remove(f)
                .commit();
        getFragmentManager().executePendingTransactions();
    }

    public void resetAdapter() {
        getListFragment().resetAdapter();
    }
}
