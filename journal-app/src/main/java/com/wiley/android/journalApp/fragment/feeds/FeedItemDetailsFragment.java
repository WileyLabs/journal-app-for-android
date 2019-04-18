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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.FeedItemDetailsComponent;
import com.wiley.android.journalApp.components.HomePageHost;
import com.wiley.android.journalApp.progress.ProgressHandler;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;

import java.util.Map;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedItemDetailsFragment extends JournalFragment implements HomePageHost {

    private static final String TAG_LIFE = FeedItemDetailsFragment.class.getSimpleName() + ".LIFE";

    @Inject
    private HomePageService mHomePageService;
    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    protected AANHelper aanHelper;

    private ProgressHandler mProgress;
    private FeedItemDetailsComponent mComponent;
    private CustomWebView mWebView;

    private final NotificationProcessor mFeedItemContentStartedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            mProgress.showProgress();
        }
    };

    private final NotificationProcessor mFeedItemContentFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);
            if (!pr.succeed()) {
                mProgress.hideProgress();
                return;
            }

            final String url = pr.getFeedItemUrl();
            final FeedItemMO feedItem = mHomePageService.getFeedItem(mItemUid);
            if (null == url || !url.equals(feedItem.getUrl())) {
                mProgress.hideProgress();
                return;
            }
            if (pr.getFeedItemContent().equals("")) {
                mComponent.loadOriginalContent(pr.getFeedItemUrl());
            } else {
                mComponent.render(pr.getFeedItemContent());
            }

            mProgress.hideProgress();
            aanHelper.trackSocietyContentPage(feedItem);
        }
    };

    public static FeedItemDetailsFragment newInstance(String feedItemUid) {

        FeedItemDetailsFragment frag = new FeedItemDetailsFragment();

        Bundle args = new Bundle();
        args.putString("feedItemUid", feedItemUid);

        frag.setArguments(args);

        return frag;
    }

    private String mItemUid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemUid = getArguments().getString("feedItemUid");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onCreateView()");
        return inflater.inflate(R.layout.frag_feed_item_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG_LIFE, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);

        mProgress = new ProgressHandler(this);

        mWebView = findView(R.id.frag_feed_item_content_view);
        mComponent = new FeedItemDetailsComponent(this, mWebView);
    }

    @Override
    public void onStart() {
        Logger.d(TAG_LIFE, "onStart()");
        super.onStart();
        mNotificationCenter.subscribeToNotification(EventList.FEED_ITEM_CONTENT_STARTED.getEventName(), mFeedItemContentStartedProcessor);
        mNotificationCenter.subscribeToNotification(EventList.FEED_ITEM_CONTENT_SUCCESS.getEventName(), mFeedItemContentFinishedProcessor);
        mNotificationCenter.subscribeToNotification(EventList.FEED_ITEM_CONTENT_NOT_MODIFIED.getEventName(), mFeedItemContentFinishedProcessor);
        mNotificationCenter.subscribeToNotification(EventList.FEED_ITEM_CONTENT_ERROR.getEventName(), mFeedItemContentFinishedProcessor);

        mHomePageService.updateFeedItemContent(mHomePageService.getFeedItem(mItemUid));
    }

    @Override
    public void onResume() {
        Logger.d(TAG_LIFE, "onResume()");
        super.onResume();
        mWebView.onResume();
    }

    @Override
    public void onPause() {
        Logger.d(TAG_LIFE, "onPause()");
        super.onPause();
        mWebView.onPause();
    }

    @Override
    public void onStop() {
        Logger.d(TAG_LIFE, "onStop()");
        super.onStop();
        mNotificationCenter.unSubscribeFromNotification(mFeedItemContentStartedProcessor);
        mNotificationCenter.unSubscribeFromNotification(mFeedItemContentFinishedProcessor);
    }

    @Override
    public void onDestroyView() {
        Logger.d(TAG_LIFE, "onDestroyView()");
        super.onDestroyView();
    }

    @Override
    public Context getContext() {
        return getJournalActivity().getApplicationContext();
    }

    @Override
    public void showNoContentAvailableMessage() {
    }
}
