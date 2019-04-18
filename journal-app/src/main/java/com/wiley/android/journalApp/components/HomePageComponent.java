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
package com.wiley.android.journalApp.components;

import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.controller.FeedsController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import roboguice.RoboGuice;

/**
 * Created by taraskreknin on 02.10.14.
 */
public class HomePageComponent {

    private static final Templates sTemplates = new Templates();

    @Inject
    protected HomePageService mHomePageService;
    @Inject
    private FeedsController mFeedsController;
    @Inject
    private Settings mSettings;
    @Inject
    private Theme mTheme;
    @Inject
    protected WebController webController;
    @Inject
    private ErrorManager errorManager;
    @Inject
    protected AANHelper aanHelper;

    protected CustomWebView mWebView;
    protected HomePageHost mHost;

    public HomePageComponent(HomePageHost host, CustomWebView webView) {
        mHost = host;
        mWebView = webView;
        setupWebView();
        RoboGuice.injectMembers(mHost.getActivity(), this);
    }

    private void setupWebView() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                return onWebViewShouldOverrideUrlLoading(url);
            }

        });
    }

    private boolean onWebViewShouldOverrideUrlLoading(String url) {
        if (url.startsWith("http")) {
            if (!NetUtils.isOnline(mHost.getContext())) {
                errorManager.alertWithErrorCode(mHost.getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
            } else {
                webController.openUrlInternal(url);
            }
            return true;
        }

        if (url.startsWith("www.")) {
            if (!NetUtils.isOnline(mHost.getContext())) {
                errorManager.alertWithErrorCode(mHost.getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
            } else {
                webController.openUrlInternal("http://" + url);
            }
            return true;
        }

        if (url.startsWith("openfeed")) {
            ((MainActivity)mHost.getActivity()).openRssFeed(url.replace("openfeed://", ""));
            return true;
        }

        if (url.startsWith("details")) {
            if (!NetUtils.isOnline(mHost.getContext())) {
                errorManager.alertWithErrorCode(mHost.getActivity(), AppErrorCode.NO_CONNECTION_AVAILABLE);
                return true;
            }

            final String itemUid = url.replace("details://", "");
            final FeedItemMO item = mHomePageService.getFeedItem(itemUid);

            if (item == null) {
                return true;
            }

            aanHelper.trackActionOpenSocietyContentItem(item.getFeed());

            final List<FeedItemMO> items = new ArrayList<>(item.getFeed().getItems());
            final List<String> itemsUids = new ArrayList<>(items.size());
            for (final FeedItemMO anItem : items) {
                itemsUids.add(anItem.getUid());
            }

            int index = itemsUids.indexOf(item.getUid());

            ((MainActivity)mHost.getActivity()).openFeedItems(itemsUids, index == -1 ? 0 : index);

        }

        return true;
    }

    public void render() {
        String data = generateHomeScreenHtml(homeScreenGenerateHtmlListener);
        if (data == null) {
            mHost.showNoContentAvailableMessage();
        } else {
            mWebView.loadData(data, "text/html", "UTF-8");
        }
    }

    interface HomeScreenGenerateHtmlListener {
        String getSectionsList();
        String getSectionTitleColor();
    }

    HomeScreenGenerateHtmlListener homeScreenGenerateHtmlListener = new HomeScreenGenerateHtmlListener() {

        @Override
        public String getSectionsList() {
            String sectionsList;
            final Collection<FeedMO> feeds = mHomePageService.getFeeds();

            if (feeds.isEmpty()) {
                return null;
            }

            if (DeviceUtils.isTablet(mHost.getContext()) && feeds.size() > 2) {
                final StringBuilder feedsMain = new StringBuilder();
                final StringBuilder feedsRight = new StringBuilder();
                int feedCounter = 0;

                final StringBuilder feedsFirst = new StringBuilder();
                for (FeedMO feed : feeds) {
                    String feedHTML = getHtmlForFeed(feed);
                    if (feedHTML.equals("")) {
                        continue;
                    }

                    if (feedCounter == 0) {
                        feedsFirst.append(feedHTML);
                    }

                    if (feedCounter % 2 == 0) {
                        feedsMain.append(feedHTML);
                    } else {
                        feedsRight.append(feedHTML);
                    }

                    feedCounter++;
                }

                final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "home_screen_sections_ipad");
                sectionsList =  template
                        .reset()
                        .putParam("first_item_of_first_section", feedsFirst.toString())
                        .putParam("sections_main", feedsMain.toString())
                        .putParam("sections_right", feedsRight.toString())
                        .proceed();

            } else {
                final StringBuilder fullHtml = new StringBuilder();
                for (FeedMO feed : feeds) {
                    String feedHTML = getHtmlForFeed(feed);
                    fullHtml.append(feedHTML);
                }

                final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "home_screen_sections_iphone");
                sectionsList = template
                        .reset()
                        .putParam("sections", fullHtml.toString())
                        .proceed();
            }

            return sectionsList;
        }

        @Override
        public String getSectionTitleColor() {
            final String color = mSettings.getSocietyScreenSectionTitleColor();
            if (null != color && !"".equals(color))
                return color;

            return String.format("#%s", mTheme.getMainColorHEX());
        }
    };

    private String generateHomeScreenHtml(final HomeScreenGenerateHtmlListener listener) {
        final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "home_screen");

        final String sectionsList = listener.getSectionsList();
        if (sectionsList == null) {
            return null;
        }
        return template
                .reset()
                .putParam("device_type", DeviceUtils.isPhone(mHost.getContext()) ? "iPhone" : "iPad")
                .putParam("sections_list", sectionsList)
                .putParam("section_title_color", listener.getSectionTitleColor())
                .proceed();
    }

    private String getHtmlForFeed(FeedMO feed) {

        if (null == feed || 0 == feed.getItems().size()) {
            return "";
        }

        final StringBuilder feedItemsHtmlSb = new StringBuilder();
        final String feedTitle = feed.getTitle();
        final List<FeedItemMO> items = new ArrayList<>(feed.getItems());
        Collections.sort(items, new Comparator<FeedItemMO>() {
                    @Override
                    public int compare(FeedItemMO one, FeedItemMO another) {
                        return another.getSortIndex() - one.getSortIndex();
                    }
                }
        );

        final boolean isTablet = DeviceUtils.isTablet(mHost.getContext());
        final int itemsNumber = items.size() > feed.getNumberOfItemsOnHomeScreen(isTablet) ?
                feed.getNumberOfItemsOnHomeScreen(isTablet) : items.size();

        final String feedColor = feed.getFeedColor().length() > 0 ?
                feed.getFeedColor() : feed.getDefaultColorForIndex(feed.getSortIndex());

        for (int i = 0; i < itemsNumber; i++) {
            final FeedItemMO feedItem = items.get(i);
            if (null != feedItem) {
                final String feedItemHTML = getHtmlForFeedItem(feedItem);
                feedItemsHtmlSb.append(feedItemHTML);
            }
        }

        final Template sectionTemplate = sTemplates.useAssetsTemplate(mHost.getContext(), "home_screen_section");

        return sectionTemplate
                .reset()
                .putParam("feed_items", feedItemsHtmlSb.toString())
                .putParam("section_title", feedTitle)
                .putParam("feed_id", feed.getUid())
                .putParam("section_border_left_color", feedColor)
                .putParam("title_border_bottom_color", feedColor)
                .proceed();
    }

    private String getHtmlForFeedItem(FeedItemMO feedItem) {

        final String thumbnail = TextUtils.isEmpty(feedItem.getImageLink()) ? "" : String.format("<div class=\"feed-thumbnail\"><img src=\"%s\" /></div>", feedItem.getImageLink());
        final String description = HtmlUtils.stripHtml(feedItem.getDescr());
        final String announce = description.length() > 500 ? description.substring(0, 500) + " ..." : description;

        final Template itemTemplate = sTemplates.useAssetsTemplate(mHost.getContext(), "home_screen_section_item");

        return itemTemplate
                .reset()
                .putParam("id", feedItem.getUid())
                .putParam("title", feedItem.getTitle())
                .putParam("thumbnail", thumbnail)
                .putParam("description", announce)
                .proceed();
    }

    public void updateFeedContent(FeedMO feed) {
        final String feedHtml = CustomWebView.makeJsSafeString(getHtmlForFeed(feed));

        String js = String.format("$('div#%s').replaceWith('%s');"
                        + "addTouchEvents('feed-body', onBodyClick);",
                feed.getUid(), feedHtml);
        mWebView.executeJavaScript(js.replaceAll("\u2028|\u2029", ""));
    }

    private void loadImagesForFeeds() {
        // todo: save images for feeds
    }
}
