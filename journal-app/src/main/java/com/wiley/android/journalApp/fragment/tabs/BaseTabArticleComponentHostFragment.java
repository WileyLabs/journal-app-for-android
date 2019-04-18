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
package com.wiley.android.journalApp.fragment.tabs;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.ArticleComponentHost;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.log.Logger;

import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ARTICLE;

/**
 * Created by taraskreknin on 21.07.14.
 */
public abstract class BaseTabArticleComponentHostFragment extends BaseTabFragment implements ArticleComponentHost, StartActivityForResultHelper {

    @Inject
    private ErrorManager errorManager;
    @Inject
    protected WebController webController;

    private View progress;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = findView(R.id.progress);
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAccessForbiddenArticle() {
        if (!isShowing()) return;
        errorManager.alertWithErrorCode(
                getActivity(),
                ACCESS_FORBIDDEN_ARTICLE,
                ErrorButton.withTitleAndListener(getActivity().getString(android.R.string.ok), null),
                ErrorButton.withTitleAndListener(getActivity().getString(R.string.get_help), new ErrorButton.OnClickListener() {
                    @Override
                    public void onClick() {
                        webController.openUrlInternal(theme.getHelpUrl());
                    }
                })
        );
    }

    @Override
    public void onSaveArticleNoInternetConnection(ArticleMO article) {
        if (isShowing()) {
            onSaveArticleError(article);
        }
    }

    @Override
    public StartActivityForResultHelper getStartActivityForResultHelper() {
        return this;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    private Handler hideProgressHandler = new Handler();

    protected void showProgress() {
        if (progress == null) {
            return;
        }
        Logger.d(getClass().getSimpleName(), "showProgress");
        hideProgressHandler.removeCallbacksAndMessages(null);
        progress.setVisibility(View.VISIBLE);
    }

    protected void hideProgress() {
        if (progress == null || View.VISIBLE != progress.getVisibility()) {
            return;
        }
        Logger.d(getClass().getSimpleName(), "hideProgress");
        hideProgressHandler.removeCallbacksAndMessages(null);
        hideProgressHandler.postDelayed(new Runnable() {
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

}
