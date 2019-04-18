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
package com.wiley.android.journalApp.fragment.specialsection;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.SpecialSectionArticlesActivity;
import com.wiley.android.journalApp.base.BaseArticleComponentHostFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.SpecialSectionArticlesComponent;
import com.wiley.android.journalApp.components.popup.PopupHost;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.ArrayList;
import java.util.Map;

import static com.wiley.wol.client.android.notification.EventList.ARTICLE_FAVORITE_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTION_FEED_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.SPECIAL_SECTION_ID;

/**
 * Created by taraskreknin on 23.07.14.
 */
public class SpecialSectionArticlesFragment
        extends
        BaseArticleComponentHostFragment
        implements
        ActionBarSherlock.OnCreatePanelMenuListener,
        ActionBarSherlock.OnMenuItemSelectedListener,
        PopupHost.PopupListener {

    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ArticleService articleService;

    private SpecialSectionArticlesComponent mComponent;

    private View mProgressView;
    private ActionBarSherlockCompat mSherlock;
    private PopupHost popupHost;

    private final NotificationProcessor mOnSectionUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final String updatedSecId = (String) params.get(SPECIAL_SECTION_ID);
            if (!getSection().getUid().equals(updatedSecId)) {
                hideProgress();
                return;
            }
            final SpecialSectionMO updatedSection = specialSectionService.getSpecialSectionById(updatedSecId);
            setSection(updatedSection);
            mComponent.updateSection(getSection());
            showArticles();
        }
    };
    private final NotificationProcessor mOnSectionUpdateErrorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            errorManager.alertWithException(getActivity(), (Throwable) params.get(ERROR));
            showArticles();
        }
    };
    private final NotificationProcessor mOnSectionNotModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            showArticles();
        }
    };
    private final NotificationProcessor savedArticlesListChangedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            mSherlock.dispatchInvalidateOptionsMenu();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_special_section_articles, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationCenter.subscribeToNotification(SPECIAL_SECTION_FEED_UPDATED.getEventName(), mOnSectionUpdatedProcessor);
        CustomWebView mWebView = findView(R.id.special_section_articles_web_view);
        mProgressView = findView(R.id.special_section_articles_progress);
        mProgressView.setVisibility(View.GONE);
        mComponent = new SpecialSectionArticlesComponent(getSection(), this, mWebView);
        mComponent.onCreateHost();

        // action bar
        mSherlock = ActionBarUtils.initActionBar(getJournalActivity(), getSection().getUnescapedTitle(), this, theme);

        popupHost = findView(R.id.special_sections_popup_host);
        if (popupHost != null) {
            popupHost.setPopupListener(this);
            popupHost.setPopupContentHolderResId(R.id.special_sections_popup_content);
        }

        undim();
        updateUi();
    }

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

    protected FrameLayout getDimmableView() {
        return (FrameLayout) findView(R.id.special_sections_dimmer);
    }

    @Override
    public void onPopupShow() {
        dim(100);
    }

    @Override
    public void onPopupDismiss() {
        undim();
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(SPECIAL_SECTION_FEED_NOT_MODIFIED.getEventName(), mOnSectionNotModifiedProcessor);
        notificationCenter.subscribeToNotification(SPECIAL_SECTION_FEED_ERROR.getEventName(), mOnSectionUpdateErrorProcessor);
        if (DeviceUtils.isTablet(getActivity())) {
            notificationCenter.subscribeToNotification(ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), savedArticlesListChangedProcessor);
        }
        mComponent.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(mOnSectionNotModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(mOnSectionUpdateErrorProcessor);
        if (DeviceUtils.isTablet(getActivity())) {
            notificationCenter.unSubscribeFromNotification(savedArticlesListChangedProcessor);
        }

        mComponent.onStop();
        hideProgressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        notificationCenter.unSubscribeFromNotification(mOnSectionUpdatedProcessor);
        mComponent.onDestroyHost();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mComponent.onOrientationChanged();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mComponent.executePostponedTasks();
        }
    }

    private void updateUi() {
        if (getSection().isNew() || getSection().getArticles().isEmpty()) {
            downloadArticlesList();
        } else {
            showArticles();
        }
    }

    private void showArticles() {
        mComponent.render(new ArrayList<>(getSection().getArticles()));
    }

    private void downloadArticlesList() {
        showProgress();
        specialSectionService.downloadSpecialSectionContent(getSection().getUid());
    }

    private SpecialSectionMO getSection() {
        return ((SpecialSectionArticlesActivity) getParentFragment()).getSection();
    }

    private void setSection(SpecialSectionMO section) {
        ((SpecialSectionArticlesActivity) getParentFragment()).setSection(section);
    }

    private Handler hideProgressHandler = new Handler();

    private void showProgress() {
        hideProgressHandler.removeCallbacksAndMessages(null);
        mProgressView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
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
                        mProgressView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                fadeOut.setDuration(500);
                mProgressView.startAnimation(fadeOut);
            }
        }, 200);
    }

    @Override
    public void onRenderCompleted() {
        hideProgress();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {

        if (DeviceUtils.isPhone(this.getActivity())) {
            menu.add(getString(R.string.action_show_menu))
                    .setIcon(ActionBarUtils.getMenuIconResource(theme))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ((MainActivity) getJournalActivity()).onSideMenuButtonClicked();
                            return true;
                        }
                    });
        } else {
            ActionBarUtils.inflateIssueTocMenu(mSherlock, menu, theme);
            final long savedCount = articleService.getSavedArticleCount();
            menu.findItem(R.id.issue_toc_edit_saved).setTitle("(" + savedCount + ")");
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (DeviceUtils.isTablet(this.getActivity()) && item.getItemId() == R.id.issue_toc_edit_saved) {
            popupHost.toggleSavedArticlesEditor(getChildFragmentManager(), findView(R.id.issue_toc_edit_saved));
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            MainActivity mainActivity = (MainActivity) getJournalActivity();
            mainActivity.onBackPressed();
            return true;
        }

        return false;
    }
}
