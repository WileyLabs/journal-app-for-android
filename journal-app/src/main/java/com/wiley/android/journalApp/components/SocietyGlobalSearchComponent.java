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

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.search.HtmlSearch;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.fragment.feeds.SocietyGlobalSearchFragment;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import roboguice.RoboGuice;

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

/**
 * Created by taraskreknin on 16.07.14.
 */
public class SocietyGlobalSearchComponent {

    public interface SocietyGlobalSearchComponentListener extends ArticleComponentHost {
        void onSearchStarted();
        void onSearchCompleted(boolean hasMatch);
        void onSearchProgress(int currentIndex, int size);
        void onRenderStarted();
        void onRenderCompleted();
        void onSortStarted();
        void onSortCompleted();

        Context getContext();
        Activity getActivity();
    }

    @Inject
    private Theme theme;
    @Inject
    private Settings mSettings;
    @Inject
    private HomePageService mHomePageService;
    @Inject
    protected WebController webController;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    protected AANHelper aanHelper;

    private static final Templates mTemplates = new Templates();
    private CustomWebView mWebView;
    private SocietyGlobalSearchComponentListener mHost;
    private List<FeedItemMO> mFeedItems;
    private String mTerm;
    private int mMode = -1;
    private boolean mAsc;
    private AsyncTask<Void, Integer, List<FeedItemMO>> mSearchTask;
    private StickyHeaderComponent stickyHeaderComponent;

    private String currentSectionHeader = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
    private static SimpleDateFormat mFirstOnLineDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected CustomWebView.OnScrollListener onWebViewScrollListener = new CustomWebView.OnScrollListenerBase() {
        @Override
        public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
            stickyHeaderComponent.refreshStickyHeaders(mHost.getContext(), t);
        }

        @Override
        public void onScrollEnded(int l, int t) {
        }
    };

    private static final Comparator<FeedItemMO> sMatchCountComparatorDescending = new Comparator<FeedItemMO>() {
        @Override
        public int compare(FeedItemMO one, FeedItemMO another) {
            return one.getRelevance() - another.getRelevance();
        }
    };

    private static final Comparator<FeedItemMO> sMatchCountComparatorAscending = new Comparator<FeedItemMO>() {
        @Override
        public int compare(FeedItemMO one, FeedItemMO another) {
            return another.getRelevance() - one.getRelevance();
        }
    };

    private static final Comparator<FeedItemMO> sTitleComparatorDescending = new Comparator<FeedItemMO>() {
        @Override
        public int compare(FeedItemMO one, FeedItemMO another) {
            return another.getTitle().compareTo(one.getTitle());
        }
    };

    private static final Comparator<FeedItemMO> sTitleComparatorAscending = new Comparator<FeedItemMO>() {
        @Override
        public int compare(FeedItemMO one, FeedItemMO another) {
            return one.getTitle().compareTo(another.getTitle());
        }
    };

    private static final Comparator<FeedItemMO> sPublishedDateComparatorDescending = new Comparator<FeedItemMO>() {
        @Override
        public int compare(FeedItemMO one, FeedItemMO another) {
            Date oneDate = one.getPubDate();
            Date anotherDate = another.getPubDate();
            long diff = anotherDate.getTime() - oneDate.getTime();
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

    private static final Comparator<FeedItemMO> sPublishedDateComparatorAscending = new Comparator<FeedItemMO>() {
        @Override
        public int compare(FeedItemMO one, FeedItemMO another) {
            Date oneDate = one.getPubDate();
            Date anotherDate = another.getPubDate();
            long diff = oneDate.getTime() - anotherDate.getTime();
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

    public SocietyGlobalSearchComponent(SocietyGlobalSearchComponentListener host, CustomWebView webView) {
        mHost = host;
        mWebView = webView;
        setupWebView(mWebView);

        RoboGuice.getInjector(mHost.getContext()).injectMembersWithoutViews(this);
        stickyHeaderComponent = new StickyHeaderComponent(theme);
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

                    aanHelper.trackActionOpenItemFromSocietyContentSearchResults(mTerm, sortMethod());

                    final List<FeedItemMO> items = new ArrayList<>(item.getFeed().getItems());
                    final List<String> itemsUids = new ArrayList<>(items.size());
                    for (final FeedItemMO anItem : items) {
                        itemsUids.add(anItem.getUid());
                    }

                    int index = itemsUids.indexOf(item.getUid());

                    ((MainActivity) mHost.getActivity()).openFeedItems(itemsUids, index == -1 ? 0 : index);

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

    private void updateWebUiFavoriteState(final FeedItemMO item, final boolean favorite) {
        mWebView.executeJavaScript(format("document.getElementById(\"%s\").getElementsByTagName(\"img\")[0].src=\"%s\";",
                "article_item_element_id_" + item.getUid(), favorite ? HtmlUtils.getAssetsImgUrl("ArticleList/favorite_hilighted@2x.png") :
                        HtmlUtils.getAssetsImgUrl("ArticleList/favorite_normal@2x.png")));
    }

    public void init() {
        mTerm = "";
        mMode = -1;
        sortAndRender(new ArrayList<FeedItemMO>());
    }


    private void sortAndRender(List<FeedItemMO> feedItems) {
        currentSectionHeader = null;

        mFeedItems = feedItems;

        // sorting
        if (feedItems.size() > 0) {
            mHost.onSortStarted();
            switch (mMode) {
                case SocietyGlobalSearchFragment.RELEVANCY_MODE:
                    if (mAsc)
                        Collections.sort(mFeedItems, sMatchCountComparatorAscending);
                    else
                        Collections.sort(mFeedItems, sMatchCountComparatorDescending);
                    break;
                case SocietyGlobalSearchFragment.TITLE_MODE:
                    if (mAsc)
                        Collections.sort(mFeedItems, sTitleComparatorAscending);
                    else
                        Collections.sort(mFeedItems, sTitleComparatorDescending);
                    break;
                case SocietyGlobalSearchFragment.DATE_PUBLISHED_MODE:
                    if (mAsc)
                        Collections.sort(mFeedItems, sPublishedDateComparatorAscending);
                    else
                        Collections.sort(mFeedItems, sPublishedDateComparatorDescending);
                    break;
            }
            mHost.onSortCompleted();
        }


        mHost.onRenderStarted();
        // TODO use loader
        final String html = generateItemListHtml(mFeedItems, itemListGenerateHtmlListener);
        mWebView.loadData(html);
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
            return "";
        }

        @Override
        public String getNoInternetConnection() {
            final Template template = mTemplates.useAssetsTemplate(mHost.getContext(), "no_internet_connection");

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
            int countArticles = (null == mFeedItems ) ? 0 : mFeedItems.size();
            final String term = (null == mTerm) ? "" : mTerm;

            final Template template = mTemplates.useAssetsTemplate(mHost.getContext(), "no_found_articles");

            return template
                    .putParam("search_instruction_display_placeholder", 0 == countArticles && 0 == term.length() ? "" : "display:none" )
                    .putParam("no_search_results_display_placeholder", 0 == countArticles && 0 < term.length() ? "display:block" : "display:none")
                    .putParam("search_term_placeholder", term)
                    .proceed();
        }

        @Override
        public String getHtmlCodeForLoadMoreWithIndex(int lastIndex) {
            final Template template = mTemplates.useAssetsTemplate(mHost.getContext(), "LoadMoreArticlesTemplate");

            return template
                    .reset()
                    .putParam("_atricle_index_", "" + lastIndex)
                    .proceed();
        }

        @Override
        public String getHtmlCodeForListHeading(Collection collection) {
            if (collection.size() > 0) {
                final Template template = mTemplates.useAssetsTemplate(mHost.getContext(), "list_heading_template_" + (DeviceUtils.isPhone(mHost.getContext()) ? "iPhone" : "iPad"));

                return template
                        .putParam("heading_text_placeholder", "Articles found: " + collection.size())
                        .putParam("show_heading_placeholder", "block")
                        .proceed();
            }

            return "";
        }

        @Override
        public String getLoadingElementString() {
            return "<p>Loading...</p>";
        }

        @Override
        public String getHtmlArticlesList() {
            if (mFeedItems.size() == 0) {
                return "";
            }

            final Template feedItemTemplate = mTemplates.useAssetsTemplate(mHost.getContext(), "ArticleListItemTemplate");

            final List<String> htmlList = new ArrayList<>();
            int capacity = 0;

            for (int i = 0; i < mFeedItems.size(); i++) {
                final FeedItemMO feedItem = mFeedItems.get(i);
                String feedItemHtml = "";

                feedItemHtml = getHTMLForFeedItem(feedItem);
                feedItemTemplate
                        .reset()
                        .putParam("_id_placeholder_", feedItem.getUid())
                        .putParam("_article_item_placeholder_", feedItemHtml)
                        .putParam("_hr_display_placeholder_", "block")
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
        String getHtmlArticlesList();
    }

    private String generateItemListHtml(Collection collection, final ItemListGenerateHtmlListener listener) {
        final Template template = mTemplates.useAssetsTemplate(mHost.getContext(), "ArticleListTemplate");

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
                .putParam("_article_ref_list_", listener.getHtmlArticlesList())
                .putParam("_loading_bottom_placeholder_", "")
                .proceed();
    }

    private String getHTMLForFeedItem(FeedItemMO feedItem) {
        final String thumbnail = TextUtils.isEmpty(feedItem.getImageLink()) ? "" : String.format("<div class=\"feed-thumbnail\"><img src=\"%s\" /></div>", feedItem.getImageLink());
        final String description = HtmlUtils.stripHtml(feedItem.getDescr());
        final String announce = description.length() > 500 ? description.substring(0, 500) + " ..." : description;
        final String favorites_src = getFavoriteIconSrc(feedItem);
        final String title = feedItem.getTitle();

        final Template template = mTemplates.useAssetsTemplate(mHost.getContext(), "feed_item_" + (DeviceUtils.isPhone(mHost.getContext()) ? "iPhone" : "iPad"));

        //
        HtmlSearch search = new HtmlSearch();

        HtmlSearch.Result bodyResult = search.find(title, mTerm);
        final String highlightedTitle;
        if (bodyResult.items.size() > 0) {
            highlightedTitle = bodyResult.highlightedHtml;
        } else {
            highlightedTitle = title;
        }

        bodyResult = search.find(announce, mTerm);
        final String highlightedDescription;
        if (bodyResult.items.size() > 0) {
            highlightedDescription = bodyResult.highlightedHtml;
        } else {
            highlightedDescription = announce;
        }

        return template
                .reset()
                .putParam("_id_placeholder_", feedItem.getUid())
                .putParam("_title_placeholder_", highlightedTitle)
                .putParam("thumbnail", thumbnail)
                .putParam("description", highlightedDescription)
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


    protected String headingBeforeItem(final FeedItemMO feedItem) {
        String result = null;
        String text = "";

        switch (mMode) {
            case SocietyGlobalSearchFragment.RELEVANCY_MODE:
                text = "Occurrences: " + feedItem.getRelevance();
                break;
            case SocietyGlobalSearchFragment.TITLE_MODE:
                text = feedItem.getTitle().substring(0, 1);
                break;
            case SocietyGlobalSearchFragment.DATE_PUBLISHED_MODE:
                text = feedItem.getPubDate() != null ? dateFormat.format(feedItem.getPubDate()) : "";
                break;
        }

        if (null == currentSectionHeader || !currentSectionHeader.equals(text)) {
            currentSectionHeader = text;
            result = String.format("<div class=\"section_heading_class\"><p class=\"section_heading_class\">%s</p></div>", text);
        }

        return result;
    }

    private String sortMethod() {
        final String mode;
        switch (mMode) {
            case SocietyGlobalSearchFragment.RELEVANCY_MODE: mode = "Relevancy"; break;
            case SocietyGlobalSearchFragment.TITLE_MODE: mode = "Title"; break;
            case SocietyGlobalSearchFragment.DATE_PUBLISHED_MODE: mode = "Date Published"; break;
            default: mode = "Undefined"; break;
        }

        return String.format("%s %s", mode, mAsc ? "Ascending" : "Descending");
    }

    public void onSort(int mode, boolean asc) {
        mMode = mode;
        mAsc = asc;

        aanHelper.trackActionSortSocietyContentSearchResults(mTerm, sortMethod());

        sortAndRender(mFeedItems);
    }

    public void onSearchCancel() {
        if (mSearchTask != null) {
            aanHelper.trackActionCancelSearch();
            mSearchTask.cancel(false);
        }
    }

    public void onSearch(final String term, int mode, boolean asc) {
        mTerm = term;
        mMode = mode;
        mAsc = asc;

        mSearchTask = createCheckAction();
        mSearchTask.execute();
    }

    private AsyncTask<Void, Integer, List<FeedItemMO>> createCheckAction() {
        return new AsyncTask<Void, Integer, List<FeedItemMO>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mHost.onSearchStarted();
            }

            @Override
            protected List<FeedItemMO> doInBackground(Void... params) {
                Collection<FeedMO> allFeeds = mHomePageService.getFeeds();

                // calculate all feedItem
                int sizeAll = 0;
                for (FeedMO feed : allFeeds) {
                    sizeAll += feed.getItems().size();
                }

                // search
                int index = 0;
                List<FeedItemMO> foundFeedItems = new ArrayList<>();
                for (FeedMO feed : allFeeds) {
                    for (FeedItemMO feedItem : feed.getItems()) {
                        if (isCancelled())
                            return null;

                        int matchCount = matchTerm(feedItem, mTerm);
                        if (matchCount > 0) {
                            feedItem.setRelevance(matchCount);
                            foundFeedItems.add(feedItem);
                        }

                        publishProgress(index++, sizeAll);
                    }
                }

                return foundFeedItems;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                mHost.onSearchProgress(values[0], values[1]);
            }

            @Override
            protected void onPostExecute(List<FeedItemMO> data) {
                super.onPostExecute(data);

                mHost.onSearchCompleted(!data.isEmpty());
                sortAndRender(data);
//            if (!data.isEmpty()) {
//                render(data);
//            } else {
//                componentHost.onRenderCompleted();
//            }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }

        };
    }

    private int matchTerm(final FeedItemMO feedItem, final String term) {
        HtmlSearch search = new HtmlSearch();
        int matchCount = 0;

        if (feedItem.getDescr().length() > 0) {
            HtmlSearch.Result bodyResult = search.find(feedItem.getDescr(), term);
            matchCount += bodyResult.items.size();
        }

        if (feedItem.getTitle().length() > 0) {
            HtmlSearch.Result bodyResult = search.find(feedItem.getTitle(), term);
            matchCount += bodyResult.items.size();
        }

        return matchCount;

    }
}
