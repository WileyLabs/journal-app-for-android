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
package com.wiley.android.journalApp.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.popup.PopupHost;
import com.wiley.android.journalApp.fragment.specialsection.SpecialSectionArticlesFragment;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;

/**
 * Created by taraskreknin on 16.07.14.
 */
public class SpecialSectionArticlesActivity extends JournalFragment implements PopupHost.PopupListener, Journal {

    public static final String EXTRA_SECTION_ID = "com.wiley.android.journalApp.activity.SpecialSectionArticlesActivity_sectionId";

    @Inject
    private SpecialSectionService mService;
    @Inject
    private ArticleService mArticleService;

    private SpecialSectionMO mSection;

    private PopupHost mPopupHostView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_special_section_articles, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initContentView(savedInstanceState);
        undim();
    }

    protected void initContentView(Bundle savedInstanceState) {
        processIntent();
        if (DeviceUtils.isTablet(this.getActivity())) {
            mPopupHostView = findView(R.id.special_section_articles_popup_host);
            mPopupHostView.setPopupContentHolderResId(R.id.special_section_articles_popup_content);
            mPopupHostView.setPopupListener(this);
        }
        getActivity().setTitle(mSection.getUnescapedTitle());

        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        SpecialSectionArticlesFragment articlesFragment = (SpecialSectionArticlesFragment) Fragment.instantiate(this.getActivity(),
                SpecialSectionArticlesFragment.class.getName(), savedInstanceState);
        ft.add(R.id.special_section_articles_content_fragment, articlesFragment);
        ft.commit();
    }

    private void processIntent() {
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            final String sectionId = extras.getString(EXTRA_SECTION_ID, "");
            mSection = mService.getSpecialSectionById(sectionId);
        }
        assert(mSection != null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.special_section_articles_menu, menu);
        final MenuItem item = menu.findItem(R.id.special_section_articles_edit_saved);
        if (item != null) {
            final long savedCount = mArticleService.getSavedArticleCount();
            item.setTitle("(" + savedCount + ")");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.special_section_articles_edit_saved) {
            toggleSavedArticleEditor();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleSavedArticleEditor() {
        if (!DeviceUtils.isTablet(this.getActivity()))
            return;
        mPopupHostView.toggleSavedArticlesEditor(getChildFragmentManager(), findView(R.id.special_section_articles_edit_saved));
    }

    //TODO put this to common class
    public void dim(final int alphaLevel) {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(alphaLevel);
        }
    }

    public void undim() {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(0);
        }
    }

    @Override
    public void onPopupShow() {
        dim(69);
    }

    @Override
    public void onPopupDismiss() {
        undim();
    }

    protected FrameLayout getDimmableView() {
        return (FrameLayout) findView(R.id.special_section_articles_dimmer);
    }

    public SpecialSectionMO getSection() {
        return mSection;
    }

    public void setSection(SpecialSectionMO section) {
        mSection = section;
    }
}