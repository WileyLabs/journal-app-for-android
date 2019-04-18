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

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.utils.AdViewController;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.settings.Environment;

public class ArticleViewAdvContent extends JournalFragment {

    private static final String TAG = ArticleViewAdvContent.class.getSimpleName() + ".Advertisement";

    private AdvertisementHandle adHandle;
    @Inject
    private Environment environment;

    public interface AdvertisementHandle {
        AdViewController getAdViewController();

        void onAdvCloseButtonClick();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate()");
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_article_view_advertisement, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return adHandle.getAdViewController().dispatchMotionEvent(event);
            }
        });
        View closeButtonView = findView(R.id.article_view_adv_close);
        closeButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adHandle.onAdvCloseButtonClick();
            }
        });
        if (!environment.isDebug()) {
            findView(R.id.article_view_adv_develop).setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        Logger.d(TAG, "onAttach()");
        adHandle = (AdvertisementHandle) getParentFragment();
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.d(TAG, "onResume()");
        if (environment.isDebug()) {
            ((TextView) findView(R.id.article_view_adv_develop)).setText(adHandle.getAdViewController().getDeveloperText());
        }
        adHandle.getAdViewController().resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.d(TAG, "onPause()");
        adHandle.getAdViewController().pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Logger.d(TAG, "onDestroyView()");
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Logger.d(TAG, "onDetach()");
        adHandle = null;
        super.onDetach();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        adHandle.getAdViewController().onConfigurationChanged(newConfig);
    }
}
