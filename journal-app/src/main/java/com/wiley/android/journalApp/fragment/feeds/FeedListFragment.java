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
package com.wiley.android.journalApp.fragment.feeds;

import android.content.Context;
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
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.ArticleComponentHost;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.FeedListComponent;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.log.Logger;

import java.util.ArrayList;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedListFragment
        extends
        JournalFragment
        implements
        ArticleComponentHost {

    private static final String TAG_LIFE = FeedListFragment.class.getSimpleName() + ".LIFE";

    @Inject
    private HomePageService mHomePageService;
    private CustomWebView webView;
    private View progress;
    private FeedListComponent mComponent;
    private Handler progressHideHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onCreateView()");
        return inflater.inflate(R.layout.frag_feed_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        webView = findView(R.id.feed_list_web_view);
        mComponent = new FeedListComponent(this, webView);

        final String mFeedUid = getArguments().getString(Extras.EXTRA_FEED_UID);

        progress = findView(R.id.progress);
        progress.setVisibility(View.GONE);

        final FeedMO feed = mHomePageService.getFeed(mFeedUid);

        mComponent.render(new ArrayList<>(feed.getItems()));
    }

    @Override
         public void onStart() {
        super.onStart();
        mComponent.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mComponent.onStop();
    }

    @Override
    public void onAccessForbiddenArticle() {

    }

    @Override
    public void onSaveArticleNoInternetConnection(ArticleMO article) {
    }

    @Override
    public StartActivityForResultHelper getStartActivityForResultHelper() {
        return null;
    }

    @Override
    public Context getContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void onRenderStarted() {
        progressHideHandler.removeCallbacksAndMessages(null);
        progress.clearAnimation();
        progress.setAlpha(1.0f);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRenderCompleted() {
        progressHideHandler.removeCallbacksAndMessages(null);
        progressHideHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                fadeOut.setDuration(500);
                progress.startAnimation(fadeOut);
            }
        }, 200);

    }

    public void onShow() {
        UIUtils.refreshWebView(webView, getContext());
    }
}
