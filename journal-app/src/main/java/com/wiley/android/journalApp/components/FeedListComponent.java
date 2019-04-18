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

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

/**
 * Created by taraskreknin on 07.10.14.
 */
public class FeedListComponent {

    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
    private static final Templates sTemplates = new Templates();

    @Inject
    private Theme theme;
    @Inject
    private Settings mSettings;
    @Inject
    private HomePageService mHomePageService;
    @Inject
    private WebController mWebController;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    protected WebController webController;
    @Inject
    protected AANHelper aanHelper;

    private CustomWebView mWebView;
    private ArticleComponentHost mHost;
    private String mCurrentHeading = null;
    private boolean isFavorites = false;
    private StickyHeaderComponent stickyHeaderComponent;
    private final int LOADER_ID_GENERATE_HTML = IdUtils.generateIntId();

    private final NotificationProcessor feedItemFavoriteStateUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final String uid = (String) params.get(NotificationCenter.UID);
            final boolean favoriteState = (boolean) params.get("favoriteState");
            FeedItemMO feedItem = new FeedItemMO();
            feedItem.setUid(uid);
            updateWebUiFavoriteState(feedItem, favoriteState);
        }
    };

    protected CustomWebView.OnScrollListener onWebViewScrollListener = new CustomWebView.OnScrollListenerBase() {
        @Override
        public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
            stickyHeaderComponent.refreshStickyHeaders(mHost.getContext(), t);
        }

        @Override
        public void onScrollEnded(int l, int t) {
        }
    };

    public FeedListComponent(ArticleComponentHost host, CustomWebView webView) {
        this(host, webView, false);
    }

    public FeedListComponent(ArticleComponentHost host, CustomWebView webView, boolean isFavorites) {
        mWebView = webView;
        setupWebView(mWebView);
        mHost = host;
        RoboGuice.getInjector(mHost.getContext()).injectMembersWithoutViews(this);

        stickyHeaderComponent = new StickyHeaderComponent(theme);
        this.isFavorites = isFavorites;
    }

    public void onStart() {
        notificationCenter.subscribeToNotification(EventList.SOCIETY_FAVORITES_COUNT_CHANGED.getEventName(), feedItemFavoriteStateUpdatedProcessor);
    }

    public void onStop() {
        notificationCenter.unSubscribeFromNotification(feedItemFavoriteStateUpdatedProcessor);
    }

    private void setupWebView(CustomWebView webView) {
        webView.setOnScrollListener(onWebViewScrollListener);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("openarticle")) {
                    final String itemUid = url.replace("openarticle://", "");
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

                    return true;
                }

                if (url.startsWith("favoriteaction")) {
                    final String itemUid = url.replace("favoriteaction://", "");
                    final FeedItemMO item = mHomePageService.getFeedItem(itemUid);
                    if (item == null) {
                        return true;
                    }

                    toggleFavoriteState(item);
                    return true;
                }

                if (url.startsWith("onready")) {
                    return true;
                }

                if (url.startsWith("onload")) {
                    UIUtils.refreshWebView(mWebView, mHost.getContext());
                    return true;
                }

                if (url.startsWith("openvideo")) {
                    return true;
                }

                if (url.startsWith("http")) {
                    webController.openUrlInternal(url);
                    return true;
                }

                if (url.startsWith("www.")) {
                    webController.openUrlInternal("http://" + url);
                    return true;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                stickyHeaderComponent.initStickyHeaders(mHost, mWebView);

                UIUtils.refreshWebView(mWebView, mHost.getContext());

                mHost.onRenderCompleted();
            }
        });
    }

    private void toggleFavoriteState(final FeedItemMO item) {
        if (item.isFavorite()) {
            UIUtils.showCancelRemove(mHost.getActivity(),
                    mHost.getActivity().getString(R.string.favorite_delete_article_title),
                    mHost.getActivity().getString(R.string.favorite_delete_feed_item_text),
                    new Runnable() {
                        @Override
                        public void run() {
                            item.setFavorite(false);
                            mHomePageService.updateFavoriteState(item.getUid(), item.isFavorite());
                            updateWebUiFavoriteState(item, false);
                        }
                    }
            );
        } else {
            item.setFavorite(true);
            mHomePageService.updateFavoriteState(item.getUid(), item.isFavorite());
            updateWebUiFavoriteState(item, true);
            if (DeviceUtils.isPhone(mHost.getContext())) {
                ((MainActivity) mHost.getActivity()).highlightSocietyFavoriteMenuItem();
            }
        }
    }

    protected void updateWebUiFavoriteState(final FeedItemMO item, final boolean favorite) {
        mWebView.executeJavaScript(format("document.getElementById(\"%s\").getElementsByTagName(\"img\")[0].src=\"%s\";",
                "article_item_element_id_" + item.getUid(), favorite ? HtmlUtils.getAssetsImgUrl("ArticleList/favorite_hilighted@2x.png") :
                        HtmlUtils.getAssetsImgUrl("ArticleList/favorite_normal@2x.png")));
    }

    public void render(final List<FeedItemMO> feedItems) {
        final LoaderManager.LoaderCallbacks<String> loaderCallbacksGenerateHtml = getGenerateHtmlLoaderCallbacks(feedItems);

        final LoaderManager lm = mHost.getActivity().getLoaderManager();
        if (lm.getLoader(LOADER_ID_GENERATE_HTML) != null) {
            lm.restartLoader(LOADER_ID_GENERATE_HTML, null, loaderCallbacksGenerateHtml);
        } else {
            lm.initLoader(LOADER_ID_GENERATE_HTML, null, loaderCallbacksGenerateHtml);
        }
    }

    private LoaderManager.LoaderCallbacks<String> getGenerateHtmlLoaderCallbacks(final List<FeedItemMO> feedItems) {
        return new LoaderManager.LoaderCallbacks<String>() {

                @Override
                public Loader<String> onCreateLoader(int id, Bundle args) {
                    mHost.onRenderStarted();
                    return new AsyncTaskLoader<String>(mHost.getContext()) {
                        @Override
                        public String loadInBackground() {
                            Collections.sort(feedItems, new Comparator<FeedItemMO>() {
                                @Override
                                public int compare(FeedItemMO one, FeedItemMO another) {
                                    Date oneDate = isFavorites ? one.getAddedToFavoritesDate() : one.getPubDate();
                                    Date anotherDate = isFavorites ? another.getAddedToFavoritesDate() : another.getPubDate();
                                    return anotherDate.compareTo(oneDate);
                                }
                            });

                            return generateItemListHtml(feedItems, itemListGenerateHtmlListener);
                        }

                        @Override
                        protected void onStartLoading() {
                            super.onStartLoading();
                            forceLoad();
                        }
                    };
                }

                @Override
                public void onLoadFinished(Loader<String> loader, String data) {
                    mWebView.loadData(data, "text/html", "UTF-8");
                    mHost.getActivity().getLoaderManager().destroyLoader(LOADER_ID_GENERATE_HTML);
                }

                @Override
                public void onLoaderReset(Loader<String> loader) {
                }
            };
    }

    private ItemListGenerateHtmlListener itemListGenerateHtmlListener = new ItemListGenerateHtmlListener() {

        @Override
        public String getAbstracts() {
            return "";
        }

        @Override
        public String getMainColor() {
            return theme.getMainColorHEX();
        }

        @Override
        public String getThemeClass() {
            return theme.isJournalHasDarkBackground() ? "darkTheme" : "";
        }

        @Override
        public String getCustomCSS() {
            if (!DeviceUtils.isPhone(mHost.getContext()) ) {
                return ".article_class {"
                + "padding-top: 15px;"
                + "max-width: none;"
                + "margin-top: 0;"
                + "border: 6px solid #F2F2F2;"
                + "}"
                + ".article_item_element_class + .article_item_element_class .article_class { border-top: 0 !important; }"
                + ".main_table_class { max-width: 800px; margin: 0 auto !important; }"
                + "hr { display: none !important; }";
            }

            return "";
        }

        @Override
        public String getNoInternetConnection() {
            final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "no_internet_connection");

            return template
                    .reset()
                    .putParam("offline_div_display", "none")
                    .proceed();
        }

        @Override
        public String getFontSize() {
            return mSettings.getArticleFontSize() + "";
        }

        @Override
        public String getNoArticlesElement() {
            final Template noSavedArticlesTemplate = new Templates().useAssetsTemplate(mHost.getContext(), "no_society_favorites");

            return noSavedArticlesTemplate
                    .proceed();
        }

        @Override
        public String getHtmlCodeForLoadMoreWithIndex(int lastIndex) {
            final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "LoadMoreArticlesTemplate");

            return template
                    .reset()
                    .putParam("_atricle_index_", "" + lastIndex)
                    .proceed();
        }

        @Override
        public String getHtmlCodeForListHeading(Collection collection) {
            return "";
        }

        @Override
        public String getLoadingElementString() {
            return "<p>Loading...</p>";
        }

        @Override
        public String getHtmlArticlesList(Collection collection) {

            final Template feedItemTemplate = sTemplates.useAssetsTemplate(mHost.getContext(), "ArticleListItemTemplate");

            final List<String> htmlList = new ArrayList<>();
            int capacity = 0;

            mCurrentHeading = null;

            int i = 0;
            for (FeedItemMO feedItem : (Collection<FeedItemMO>)collection) {
                String feedItemHtml = "";
                String additionalClass = (++i % 2 == 0) ? "odd" : "even";

                feedItemHtml = getHTMLForFeedItem(feedItem);
                feedItemTemplate
                        .reset()
                        .putParam("_id_placeholder_", feedItem.getUid())
                        .putParam("_additional_class_", additionalClass)
                        .putParam("_article_item_placeholder_", feedItemHtml)
                        .putParam("_article_item_opacity_placeholder_", "1");

                final String feedItemHtmlAsString = feedItemTemplate.proceed();

                final String heading = headingBeforeItem(feedItem);
                if (!isEmpty(heading)) {
                    htmlList.add(heading);
                }

                htmlList.add(feedItemHtmlAsString);
                capacity += feedItemHtmlAsString.length();
            }

            final StringBuilder fullHtml = new StringBuilder(capacity);
            for (String s : htmlList) {
                fullHtml.append(s);
            }

            return fullHtml.toString();
        }
    };

    interface ItemListGenerateHtmlListener {
        String getAbstracts();
        String getMainColor();
        String getThemeClass();
        String getCustomCSS();
        String getNoInternetConnection();
        String getFontSize();
        String getNoArticlesElement();
        String getHtmlCodeForLoadMoreWithIndex(int listIndex);
        String getHtmlCodeForListHeading(Collection collection);
        String getLoadingElementString();
        String getHtmlArticlesList(Collection collection);
    }

    private String generateItemListHtml(Collection collection, final ItemListGenerateHtmlListener listener) {
        final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "ArticleListTemplate");

        return template
                .reset()
                .putParam("_abstracts_", listener.getAbstracts())
                .putParam("main_color", listener.getMainColor())
                .putParam("_theme_class_", listener.getThemeClass())
                .putParam("_custom_css_classes_", listener.getCustomCSS())
                .putParam("_no_internet_connection_placeholder_", listener.getNoInternetConnection())
                .putParam("_list_font_size_", listener.getFontSize())
                .putParam("_no_article_placeholder_", collection.size() == 0 ? listener.getNoArticlesElement() : "")
                .putParam("_load_more_element_", listener.getHtmlCodeForLoadMoreWithIndex(0))
                .putParam("_list_heading_placeholder_", listener.getHtmlCodeForListHeading(collection))
                .putParam("_loading_top_placeholder_", listener.getLoadingElementString())
                .putParam("_article_ref_list_", collection.size() == 0 ? "" : listener.getHtmlArticlesList(collection))
                .putParam("_loading_bottom_placeholder_", "")
                .proceed();
    }

    private String getHTMLForFeedItem(FeedItemMO feedItem) {
        final String thumbnail = TextUtils.isEmpty(feedItem.getImageLink()) ? "" : String.format("<div class=\"feed-items-list-thumbnail\"><img src=\"%s\" /></div>", feedItem.getImageLink());
        final String description = HtmlUtils.stripHtml(feedItem.getDescr());
        final String announce = description.length() > 500 ? description.substring(0, 500) + " ..." : description;
        final String favorites_src = getFavoriteIconSrc(feedItem);

        final Template template = sTemplates.useAssetsTemplate(mHost.getContext(), "feed_item_" + (DeviceUtils.isPhone(mHost.getContext()) ? "iPhone" : "iPad"));

        return template
                .reset()
                .putParam("_id_placeholder_", feedItem.getUid())
                .putParam("_title_placeholder_", feedItem.getTitle())
                .putParam("thumbnail", thumbnail)
                .putParam("description", announce)
                .putParam("_bookmark_placeholder_", favorites_src)
                .proceed();
    }

    private String getFavoriteIconSrc(FeedItemMO feedItem) {
        String image = HtmlUtils.getAssetsImgUrl("ArticleList/favorite_normal@2x.png");
        if (feedItem.isFavorite()) {
            image = HtmlUtils.getAssetsImgUrl("ArticleList/favorite_hilighted@2x.png");
        }
        return image;
    }

    private String headingBeforeItem(final FeedItemMO item) {
        String result = "";
        final String template;
        final String publicationDate;
        if (isFavorites) {
            publicationDate = sDateFormat.format(item.getAddedToFavoritesDate());
            template = "<div class=\"section_heading_class\"><p class=\"section_heading_class\">Saved on %s</p></div>";
        } else {
            publicationDate = item.getPubDate() != null ? sDateFormat.format(item.getPubDate()) : "";
            template = "<div class=\"section_heading_class\"><p class=\"section_heading_class\">%s</p></div>";
        }

        if (!publicationDate.equals(mCurrentHeading)) {
            result = format(template, publicationDate);
            mCurrentHeading = publicationDate;
        }

        return result;
    }

}
