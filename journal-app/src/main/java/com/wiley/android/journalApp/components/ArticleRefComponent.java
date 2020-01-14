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
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.controller.ArticleController;
import com.wiley.android.journalApp.controller.VideoController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.notification.ArticleFavoriteStateChangeProcessor;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.android.journalApp.utils.MathUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import com.wiley.wol.client.android.data.http.Resource;
import com.wiley.wol.client.android.data.http.ResourceManager;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Settings;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;

import static android.text.TextUtils.isEmpty;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE_TYPE;
import static com.wiley.wol.client.android.data.manager.ResourceType.THUMBNAIL;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_FAVORITE_STATE_CHANGED;
import static com.wiley.wol.client.android.notification.EventList.DONE_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.ERROR_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.SECTION_DID_BECOME_VISIBLE;
import static com.wiley.wol.client.android.notification.EventList.SETTINGS_CHANGED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.SETTING_NAME_KEY;
import static java.lang.String.format;

public abstract class ArticleRefComponent implements SwipeRefreshLayout.OnChildScrollUpCallback {
    private static final String TAG = ArticleRefComponent.class.getSimpleName();

    private static final String BASE_TOC_URL = "app:toc";
    protected final Templates templates = new Templates();
    protected Context context;
    protected CustomWebView webView;
    @Inject
    protected AANHelper aanHelper;
    @Inject
    protected Settings settings;
    @Inject
    protected Theme theme;
    @Inject
    protected Authorizer authorizer;
    @Inject
    protected ArticleController articleController;
    @Inject
    protected WebController webController;
    @Inject
    protected VideoController videoController;
    @Inject
    protected ImageLoader imageLoader;
    @Inject
    protected ResourceManager resourceManager;
    @Inject
    protected NotificationCenter notificationCenter;
    @Inject
    protected ArticleService articleService;
    @Inject
    private ErrorManager errorManager;
    @InjectCachePath
    protected String cachePath;
    private StickyHeaderComponent stickyHeaderComponent;
    @Inject
    private LayoutInflater inflater;
    @Inject
    private EmailSender emailSender;

    protected List<ArticleMO> currentArticles = new ArrayList<>();
    protected DOI pendingDoi;
    protected ArticleComponentHost componentHost;

    private final Map<String, PostponeTask> postponedTasks = new HashMap<>();
    private RelativeLayout headersLayout;

    protected CustomWebView.OnScrollListener onWebViewScrollListener = new CustomWebView.OnScrollListenerBase() {
        @Override
        public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
            retrieveCurrentSectionId();

            refreshStickyHeaders(t);
        }

        @Override
        public void onScrollEnded(int l, int t) {
            sendScrollNotifications = true;
            loadThumbnailsForRect(new Rect(l, t, l + webView.getWidth(), t + webView.getHeight()));
        }
    };

    public void refreshStickyHeaders() {
        refreshStickyHeaders(webView.getScrollY());
    }

    public void refreshStickyHeaders(int t) {
        stickyHeaderComponent.refreshStickyHeaders(context, t);
    }

    protected final ArticleFavoriteStateChangeProcessor articleFavStateChangedProcessor = new ArticleFavoriteStateChangeProcessor() {
        @Override
        protected void onSuccess(ArticleMO article) {
            updateWebUiFavoriteState(article.getDOI(), article.isFavorite(), article.isRestricted());
        }

        @Override
        protected void onNoConnection(ArticleMO article) {
            if ((componentHost instanceof Fragment) && ((MainActivity) componentHost.getActivity()).isFragmentOnTop((Fragment) componentHost)) {
                componentHost.onSaveArticleNoInternetConnection(article);
            }
            updateWebUiFavoriteState(article.getDOI(), article.isFavorite(), article.isRestricted());
        }

        @Override
        protected void onNoAccess(ArticleMO article) {
            if ((componentHost instanceof Fragment) && ((MainActivity) componentHost.getActivity()).isFragmentOnTop((Fragment) componentHost)) {
                componentHost.onAccessForbiddenArticle();
            }
            updateWebUiFavoriteState(article.getDOI(), article.isFavorite(), article.isRestricted());
        }
    };

    protected final NotificationProcessor settingsProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final String settingName = (String) params.get(SETTING_NAME_KEY);
            if (Settings.SETTING_ARTICLE_FONT_SIZE.equals(settingName)) {
                postponedTasks.put("updateFontSize", new PostponeTask() {
                    @Override
                    public void doTask() {
                        updateFontSize();
                    }
                });
            } else if (Settings.SETTING_ARTICLE_SHOW_ABSTRACT.equals(settingName)) {
                updateUi();
            }
        }
    };

    protected final NotificationProcessor downloadCompleteNotificationProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final Resource resource = (Resource) params.get(RESOURCE);
            if (THUMBNAIL == resource.getResourceType()) {
                Logger.d(TAG, "processor start:" + resource.getFilePath());

                final List<ThumbnailInfo> loadedThumbnails = getThumbnailsForUrl(resource.getUrl());
                final String localUrl = new File(resource.getFilePath()).toURI().toString();

                for (final ThumbnailInfo loadedThumbnail : loadedThumbnails) {
                    articleService.changeArticleLocalThumbnail(loadedThumbnail.doi, localUrl);
                    webView.setElementPropertyValue(loadedThumbnail.id + "_thumbnail_center", "src", localUrl);
                    webView.setElementPropertyValue(loadedThumbnail.id + "_thumbnail_right", "src", localUrl);
                }

                synchronized (thumbnails) {
                    thumbnails.removeAll(loadedThumbnails);
                }
                Logger.d(TAG, "processor stop:" + resource.getFilePath());
            }
        }
    };

    protected final NotificationProcessor downloadErrorNotificationProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final Resource resource = (Resource) params.get(RESOURCE);
            if (resource == null || THUMBNAIL != resource.getResourceType()) {
                return;
            }
            Logger.d(TAG, "error processor for: " + resource.getFilePath());

            final List<ThumbnailInfo> thumbnails = getThumbnailsForUrl(resource.getUrl());
            for (final ThumbnailInfo thumbnail : thumbnails) {
                thumbnail.loading = false;
            }

            errorManager.alertWithException(componentHost.getActivity(), (Throwable) params.get(ERROR));
        }
    };

    private final NotificationProcessor articleMarkAsReadProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);
            final DOI doi = pr.getArticleDoi();

            for (ArticleMO article : currentArticles) {
                if (article.getDOI().getValue().equals(doi.getValue())) {
                    article.setRead(true);
                    final String id = doi.getIdCompatibleValue() + "_article_title_font_weight";
                    webView.setElementPropertyValue(id, "style.fontWeight", "normal");
                    break;
                }
            }
        }
    };

    private int lastLoadedArticleIndex = -1;
    private String generalErrorString = null;

    protected boolean sendScrollNotifications = true;

    @Override
    public boolean canChildScrollUp(SwipeRefreshLayout parent, View child) {
        return webView.getScrollY() > 0;
    }

    public ArticleRefComponent(final ArticleComponentHost host, final CustomWebView webView) {
        this.componentHost = host;
        this.context = componentHost.getActivity();
        RoboGuice.injectMembers(componentHost.getActivity(), this);

        stickyHeaderComponent = new StickyHeaderComponent(theme);

        initializeWebView(webView);
    }

    private void initializeWebView(final CustomWebView webView) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                return onWebViewShouldOverrideUrlLoading(url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                componentHost.onRenderCompleted();

                initStickyHeaders(webView);

                UIUtils.refreshWebView(webView, context);
                updateHeaderLocations();
            }
        });
        webView.setOnScrollListener(onWebViewScrollListener);
        this.webView = webView;
    }

    private void initStickyHeaders(CustomWebView webView) {
        if (headersLayout == null) {
            stickyHeaderComponent.initStickyHeaders(componentHost, webView);
        } else {
            stickyHeaderComponent.initStickyHeaders(componentHost, webView, headersLayout);
        }
    }

    public void onCreateHost() {
        notificationCenter.subscribeToNotification(ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), articleFavStateChangedProcessor);
    }

    public void onDestroyHost() {
        notificationCenter.unSubscribeFromNotification(articleFavStateChangedProcessor);
    }

    public void onStart() {
        notificationCenter.subscribeToNotification(SETTINGS_CHANGED.getEventName(), settingsProcessor);
        notificationCenter.subscribeToNotification(DONE_RESOURCE_DOWNLOADING.getEventName(), downloadCompleteNotificationProcessor);
        notificationCenter.subscribeToNotification(ERROR_RESOURCE_DOWNLOADING.getEventName(), downloadErrorNotificationProcessor);
        notificationCenter.subscribeToNotification(EventList.ARTICLE_MARK_AS_READ.getEventName(), articleMarkAsReadProcessor);
        onOrientationChanged();
        updateAllWebUiFavoriteState();
    }

    public void onStop() {
        notificationCenter.unSubscribeFromNotification(settingsProcessor);
        notificationCenter.unSubscribeFromNotification(downloadCompleteNotificationProcessor);
        notificationCenter.unSubscribeFromNotification(downloadErrorNotificationProcessor);
        notificationCenter.unSubscribeFromNotification(articleMarkAsReadProcessor);
    }

    public void executePostponedTasks() {
        for (PostponeTask task : postponedTasks.values()) {
            task.doTask();
        }
    }

    private void toggleArticleRefFavoriteState(final ArticleMO article) {
        if (articleService.isArticleRefFavoriteChangingInProgress(article.getDOI())) {
            return;
        }

        if (article.isFavorite()) {
            UIUtils.showCancelRemove(context,
                    context.getString(R.string.favorite_delete_article_title),
                    context.getString(R.string.favorite_delete_article_text),
                    new Runnable() {
                        @Override
                        public void run() {

                            setArticleFavStateIsChanging(article);
                            articleService.removeArticleRefFromFavorites(article);
                        }
                    }
            );
        } else {
            if (article.isRestricted()) {
                {
                    aanHelper.setCurrentPageForGetAccess_MainScreen();
                }
                if (authorizer.isAuthorized()) {
                    setArticleFavStateIsChanging(article);
                    articleService.addArticleRefToFavorites(article);
                } else {
                    authorizer.requestAccessFromFavoriteAction(componentHost.getStartActivityForResultHelper(),
                            article.getDOI());
                }
            } else {
                setArticleFavStateIsChanging(article);
                articleService.addArticleRefToFavorites(article);
                if (DeviceUtils.isPhone(componentHost.getContext())) {
                    ((MainActivity) componentHost.getActivity()).highlightJournalFavoriteMenuItem();
                }
            }
        }
    }

    protected void updateUi() {
        Logger.d(TAG, "updateUi()");
        this.lastLoadedArticleIndex = -1;

        final Activity activity = componentHost.getActivity();
        if (activity == null) {
            return;
        }
        final LoaderManager lm = activity.getLoaderManager();
        loaderId_generateHtml = LOADER_ID_GENERATE_HTML + this.hashCode();
        if (lm.getLoader(loaderId_generateHtml) != null) {
            lm.restartLoader(loaderId_generateHtml, null, loaderCallbacksGenerateHtml);
        } else {
            lm.initLoader(loaderId_generateHtml, null, loaderCallbacksGenerateHtml);
        }
    }

    protected String generateHtml() {
        final String abstracts = isShowAbstract() ? "" : "withoutAbstracts";
        final String mainColor = theme.getMainColorHEX();
        final String themeClass = theme.isJournalHasDarkBackground() ? "darkTheme" : "";

        int articleFontSize = settings.getArticleFontSize();

        final Template articleTemplate = templates.useAssetsTemplate(context, "ArticleListTemplate");

        final String loadingElementString = isEmpty(generalErrorString) ? "<p>Loading...</p>" : format("<p>%s</p>", generalErrorString);
        final String noInternetConnection = htmlCodeForNoInternetConnectionItem();
        final String noArticlesElement = htmlCodeForNoArticlesElement();

        final String htmlArticlesList = createHTMLFromArticles(currentArticles, 0, currentArticles.size() - 1);

        articleTemplate
                .putParam("_abstracts_", abstracts)
                .putParam("_main_color_", mainColor)
                .putParam("_theme_class_", themeClass)
                .putParam("_no_internet_connection_placeholder_", noInternetConnection)
                .putParam("_list_font_size_", articleFontSize)
                .putParam("_no_article_placeholder_", noArticlesElement)
                .putParam("_load_more_element_", htmlCodeForLoadMoreWithIndex(lastLoadedArticleIndex))
                .putParam("_list_heading_placeholder_", htmlCodeForListHeading())
                .putParam("_loading_top_placeholder_", loadingElementString)
                .putParam("_article_ref_list_", htmlArticlesList)
                .putParam("_loading_bottom_placeholder_", "");

        return articleTemplate.proceed();
    }

    protected String htmlCodeForNoInternetConnectionItem() {
        return templates
                .useAssetsTemplate(context, "no_internet_connection")
                .putParam("offline_div_display", "none")
                .proceed();
    }

    protected String htmlCodeForNoArticlesElement() {
        return null;
    }

    protected String htmlCodeForLoadMoreWithIndex(final int index) {
        return templates.useAssetsTemplate(context, "LoadMoreArticlesTemplate")
                .putParam("_atricle_index_", index)
                .proceed();
    }

    protected String htmlCodeForListHeading() {
        return null;
    }

    protected boolean onWebViewShouldOverrideUrlLoading(final String url) {
        final Uri uri = Uri.parse(url);
        if ("openarticle".equals(uri.getScheme())) {
            final String aid = uri.getHost();
            final List<DOI> doiList = new ArrayList<>();
            for (ArticleMO article : currentArticles) {
                doiList.add(article.getDOI());
            }
            ArticleMO article = getArticleByUid(aid);
            DOI doi = article != null ? article.getDOI() : new DOI("");
            openArticles(doiList, doi);
            return true;
        } else if ("openvideo".equals(uri.getScheme())) {
            videoController.openVideoUrl(url);
            return true;
        } else if ("favoriteaction".equals(uri.getScheme())) {
            final String aid = uri.getHost();
            final ArticleMO article = getArticleByUid(aid);
            if (article != null) {
                toggleArticleRefFavoriteState(article);
            }

            return true;
        } else if (uri.getScheme().startsWith("http")) {
            webController.openUrlInternal(url);
            return true;
        } else if ("onready".equals(uri.getScheme())) {
            listDidLoaded();
            updateHeaderLocations();
            return true;
        } else if ("onload".equals(uri.getScheme())) {
            updateHeaderLocations();
            return true;
        } else if ("mailto".equals(uri.getScheme())) {
            emailSender.sendEmailText((Activity) context, "", "", "", "", uri.getEncodedSchemeSpecificPart());
        }

        return true;
    }

    @Nullable
    private ArticleMO getArticleByUid(final String aid) {
        ArticleMO article = null;
        try {
            article = articleService.getArticleByUid(Integer.valueOf(aid));
        } catch (NumberFormatException e) {
            Logger.s(TAG, "Unable to parse article id", e);
        }
        return article;
    }

    protected void listDidLoaded() {
        retrieveCurrentSectionId();
        addPendingToFavorites();
        if (this.rendering) {
            this.rendering = false;
            if (componentHost != null) {
                componentHost.onRenderCompleted();
            }
        }
    }

    protected abstract void openArticles(final List<DOI> doiList, final DOI doiForOpen);

    protected void getTopVisibleHeaderPosition(CustomWebView.JavaScriptExecutionJsonCallback callback) {
        webView.executeJavaScriptAndGetJsonResult("return getTopVisibleHeaderPosition();", callback);
    }

    protected void updateHeaderLocations() {
        webView.executeJavaScriptAndGetJsonResult("return getThumbnailsLocation();", new CustomWebView.JavaScriptExecutionJsonCallback() {
            @Override
            public void onJavaScriptResult(final JSONObject json) {
                updateThumbnailLocations(json);
                loadThumbnailsForRect(new Rect(0, 0, webView.getWidth(), webView.getHeight()));
            }
        });
    }

    protected void updateThumbnailLocations(final JSONObject locations) {
        synchronized (thumbnails) {
            for (final ThumbnailInfo thumbnailInfo : thumbnails) {
                final JSONObject location = locations.optJSONObject(thumbnailInfo.id);
                if (location != null) {
                    thumbnailInfo.y = UIUtils.dpToPx(componentHost.getContext(), location.optInt("y", 0));
                }
            }
        }
    }

    protected void loadThumbnailsForRect(final Rect rect) {
        synchronized (thumbnails) {
            for (final ThumbnailInfo thumbnail : thumbnails) {
                if (thumbnail.y == null) {
                    continue;
                }
                final int y = thumbnail.y;
                if (!thumbnail.loading && y >= rect.top && y <= rect.bottom) {
                    final HashMap<String, Object> params = new HashMap<>();
                    params.put(RESOURCE_TYPE, THUMBNAIL);
                    Logger.d(TAG, "Start thumbnail loading for: " + thumbnail.localPath.getAbsolutePath());
                    resourceManager.addSmallTask(thumbnail.url, thumbnail.localPath.getAbsolutePath(), false, params);
                }
            }
        }
    }

    protected String createHTMLFromArticles(final List<ArticleMO> articles, final int startIndex, final int endIndex) {
        if (currentArticles.size() == 0) {
            return isEmpty(generalErrorString) ? "" : format("<p>%s</p>", generalErrorString);
        }

        final Template articleItemTemplate = templates.useAssetsTemplate(context, "ArticleListItemTemplate");
        final List<String> htmlList = new ArrayList<>();
        int capacity = 0;

        for (int i = startIndex; i <= endIndex; i++) {
            final ArticleMO article = articles.get(i);

            String articleContent;
            articleContent = htmlCodeForContentInArticle(article);
            articleItemTemplate
                    .reset()
                    .putParam("_id_placeholder_", article.getUid())
                    .putParam("_article_item_placeholder_", articleContent)
                    .putParam("_hr_display_placeholder_", "block")
                    .putParam("_article_item_opacity_placeholder_", "1");
            final String articleHTMLString = articleItemTemplate.proceed();

            final String heading = headingBeforeArticle(article);
            if (!isEmpty(heading)) {
                htmlList.add(heading);
            }

            htmlList.add(articleHTMLString);
            capacity += articleHTMLString.length();
        }

        final StringBuilder fullHtml = new StringBuilder(capacity);
        for (String s : htmlList) {
            fullHtml.append(s);
        }

        return fullHtml.toString();
    }

    public void onScrollViewScrolled(int scrollY, int offset) {
        stickyHeaderComponent.refreshStickyHeaders(context, scrollY, offset);
    }

    protected static class ThumbnailInfo {
        public DOI doi;
        public String id = null;
        public File localPath = null;
        public String url = null;
        public Integer y = null;
        public boolean loading = false;
    }

    protected final List<ThumbnailInfo> thumbnails = new ArrayList<>();

    protected List<ThumbnailInfo> getThumbnailsForUrl(final String url) {
        final List<ThumbnailInfo> result = new ArrayList<>();
        synchronized (thumbnails) {
            for (final ThumbnailInfo thumbnail : thumbnails) {
                if (thumbnail.url.equals(url)) {
                    result.add(thumbnail);
                }
            }
        }
        return result;
    }

    protected File getFullPathToArticleFile(final ArticleMO article, final String filename) {
        final File localPath = article.getLocalPath(filename);
        return new File(cachePath, localPath.toString());
    }

    protected boolean hideSectionHeader() {
        return false;
    }

    protected String htmlCodeForContentInArticle(final ArticleMO article) {
        final String title = htmlCodeForTitleInArticle(article);
        final String summary = this.isShowAbstract() ? htmlCodeForAbstractInArticle(article) : "";
        final String authors = htmlCodeForAuthorsInArticle(article);
        final String date = this.isShowAbstract() ? article.getFirstOnlineDate() : "";
        final String bookmark = htmlCodeForBookmarkElement(article.getDOI(), article.isFavorite());
        String section = isEmpty(article.getTocHeading1()) ? "" : article.getTocHeading1();
        final String subsection = isEmpty(article.getTocHeading2()) ? "" : article.getTocHeading2();
        final String subsectionBackColor = isEmpty(subsection) ? "#fff" : "#fafafa";

        final String lockIconID = article.getDOI().getIdCompatibleValue() + "_lock_icon";
        final String lockIconDisplay = article.isRestricted() ? "inline" : "none";
        final String unLockIconID = article.getDOI().getIdCompatibleValue() + "_unlock_icon";
        final String unLockIconDisplay = article.isOpenAccess() ? "inline" : "none";


        String sectionDivederDisplay = isEmpty(article.getTocHeading2()) ? "none" : "inline";
        String headingDisplay = isEmpty(article.getTocHeading1()) && isEmpty(article.getTocHeading2()) ? "display:none" : "";

        if (hideSectionHeader()) {
            section = "";
            if (TextUtils.isEmpty(subsection)) {
                headingDisplay = "display:none";
            } else {
                sectionDivederDisplay = "none";
            }
        }

        String centerThumb = "";
        String rightThumb = "";

        final String articleThumbnailID = "article_thumbnail_" + article.getDOI().getIdCompatibleValue();

        if (!isEmpty(article.getThumbnailUrl()) && isEmpty(article.getThumbnailLocal())) {
            final ThumbnailInfo thumbnailInfo = new ThumbnailInfo();
            thumbnailInfo.doi = article.getDOI();
            thumbnailInfo.id = articleThumbnailID;
            thumbnailInfo.localPath = getFullPathToArticleFile(article, "thumbnailImage.gif");
            thumbnailInfo.url = article.getThumbnailUrl();
            synchronized (thumbnails) {
                thumbnails.add(thumbnailInfo);
            }
        }

        final boolean isTablet = DeviceUtils.isTablet(context);

        boolean wideImage = false;
        if (!isEmpty(article.getThumbnailUrl()) || !isEmpty(article.getThumbnailLocal())) {
            final int maxWidth = isTablet ? 580 : 300;
            final int maxHeight = isTablet ? 100 : 0;
            int imgWidth = article.getThumbnailWidth();
            int imgHeight = article.getThumbnailHeight();
            if (imgWidth == 0) {
                imgWidth = isTablet ? 480 : 100;
            }
            if (imgHeight == 0) {
                imgHeight = isTablet ? 320 : 60;
            }

            final boolean center = imgWidth > 1.75 * imgHeight;
            wideImage = center;

            String centerSizeString = "";
            String rightSizeString = "";
            String centerMarginsString = "";
            String rightMarginsString = "";

            if (!this.isShowAbstract()) {
                final Point centerSize = MathUtils.inflateSize(new Point(imgWidth, imgHeight), new Point(maxWidth, maxHeight));
                centerSizeString = format("width='%dpx' height='%dpx'", centerSize.x, centerSize.y);
                if (isTablet) {
                    centerMarginsString = "margin: 15px 15px 3px 15px;";
                }

                final Point rightSize = MathUtils.inflateSize(new Point(imgWidth, imgHeight), new Point(180, 90));
                rightSizeString = format("width='%dpx' height='%dpx'", rightSize.x, rightSize.y);
                rightMarginsString = "margin: 3px 5px 3px 15px;";
            } else {
                final Point centerSize = MathUtils.inflateSize(new Point(imgWidth, imgHeight), new Point(maxWidth, 0));
                centerSizeString = format("width='%dpx' height='%dpx'", centerSize.x, centerSize.y);

                final Point rightSize = MathUtils.inflateSize(new Point(imgWidth, imgHeight), new Point((int) (180 * 1.3f), 0));
                rightSizeString = format("width='%dpx' height='%dpx'", rightSize.x, rightSize.y);
                rightMarginsString = "margin: 0px 24px 24px 24px;";
            }

            String srcString = "";
            if (!isEmpty(article.getThumbnailLocal())) {
                srcString = format("src='%s'", article.getThumbnailLocal());
            }

            boolean displayInCenter = center;
            final boolean isLandscape = DeviceUtils.isLandscape(context);

            if (!this.isShowAbstract() && isLandscape) {
                displayInCenter = false;
            }

            if (DeviceUtils.isPhone(context)) {
                displayInCenter = theme.isShowGraphicalAbstract();
            }


            centerThumb = format("<center><img %s %s id='%s_thumbnail_center' data-thumbnail='%s' %s style='background:#AAA; max-width:100%%; %s float:none; display:%s' /></center>",
                    center ? "class=\"centered_images_class\"" : "",
                    srcString,
                    articleThumbnailID,
                    articleThumbnailID,
                    centerSizeString,
                    centerMarginsString,
                    displayInCenter ? "block" : "none");

            rightThumb = format("<img %s %s id='%s_thumbnail_right' data-thumbnail='%s' %s valign='top' style='background:#AAA; max-width:100%%; %s float:right; display:%s' />",
                    center ? "class=\"right_images_class\"" : "",
                    srcString,
                    articleThumbnailID,
                    articleThumbnailID,
                    rightSizeString,
                    rightMarginsString,
                    displayInCenter ? "none" : "block");
        }


        final String wwthumb = !this.isShowAbstract() ? centerThumb : (wideImage ? centerThumb : rightThumb.replace("thumbnail_right", "thumbnail_center"));

        final Template articleContentTemplate = templates.useAssetsTemplate(context, "ArticleContentTemplate");
        articleContentTemplate
                .putParam("_is_new_article_placeholder_", htmlCodeForNewIconElement(article))
                .putParam("_article_subsection_background_color_", subsectionBackColor)
                .putParam("_article_section_", section)
                .putParam("_article_subsection_", subsection)
                .putParam("_heading_display_placeholder_", headingDisplay)
                .putParam("_heading_divider_display_placeholder_", sectionDivederDisplay)
                .putParam("_title_placeholder_", title)
                .putParam("_id_article_title_font_weight_", article.getDOI().getIdCompatibleValue() + "_article_title_font_weight")
                .putParam("article_title_font_weight", article.isRead() ? "normal" : "bold")
                .putParam("_date_placeholder_", date)
                .putParam("_abstract_placeholder_", summary)
                .putParam("_authors_placeholder_", authors)
                .putParam("_whole_width_thumbnail_placeholder_", wwthumb)
                .putParam("_partial_width_thumbnail_placeholder_", !this.isShowAbstract() ? rightThumb : "")
                .putParam("_id_placeholder_", article.getUid())
                .putParam("_bookmark_placeholder_", bookmark)
                .putParam("_lock_icon_id_placeholder_", lockIconID)
                .putParam("_lock_icon_display_placeholder_", lockIconDisplay)
                .putParam("_unlock_icon_id_placeholder_", unLockIconID)
                .putParam("_unlock_icon_display_placeholder_", unLockIconDisplay);

        return articleContentTemplate.proceed();
    }

    protected String htmlCodeForTitleInArticle(final ArticleMO article) {
        return isEmpty(article.getTitle()) ? "" : article.getTitle();
    }

    protected String htmlCodeForNewIconElement(final ArticleMO article) {
        return "&nbsp;<br/>&nbsp;";
    }

    protected String htmlCodeForAbstractInArticle(final ArticleMO article) {
        final String articleHTMLItemAbstractID = "abstract_placeholder_for_article_id_" + article.getDOI().getIdCompatibleValue();
        return format("<div class=\"abstract_class\" id=\"%s\">%s</div>", articleHTMLItemAbstractID, article.getSummary());
    }

    protected String htmlCodeForAuthorsInArticle(final ArticleMO article) {
        final Template articleItemAuthorsTemplate = templates.useAssetsTemplate(context, "ArticleListItemAuthorsTemplate");
        String authors = article.getSimpleAuthorList();
        if (isEmpty(authors)) {
            if (isEmpty(article.getTitle())) {
                authors = format("<div class=\"authors_placeholder_for_article\" id=\"authors_placeholder_for_article_id_%s\"></div>", article.getDOI().getIdCompatibleValue());
            } else {
                authors = "";
            }
        } else {
            articleItemAuthorsTemplate
                    .putParam("_authors_", authors)
                    .putParam("_author_count_modifier_placeholder_", article.isOneAuthor() ? "" : "s");
            authors = articleItemAuthorsTemplate.proceed();
        }
        return authors;
    }

    protected String htmlCodeForBookmarkElement(final DOI doi, final boolean favorite) {
        String image = HtmlUtils.getAssetsImgUrl("ArticleList/favorite_normal@2x.png");
        boolean showLoading = articleService.isArticleRefFavoriteChangingInProgress(doi)
                || articleService.isArticleRefUpdating(doi);
        if (showLoading) {
            image = HtmlUtils.getAssetsImgUrl("ArticleList/loading.gif");
        } else if (favorite) {
            image = HtmlUtils.getAssetsImgUrl("ArticleList/favorite_hilighted@2x.png");
        }
        return image;
    }

    protected String headingBeforeArticle(final ArticleMO article) {
        return "";
    }

    private boolean rendering = false;

    public boolean isRendering() {
        return rendering;
    }

    public void render(final List<ArticleMO> articles) {
        assert (articles != null);
        Logger.d(TAG, "render(final List<ArticleMO> " + articles.size() + ")");
        this.componentHost.onRenderStarted();
        this.rendering = true;
        this.currentArticles = articles;
        updateUi();
    }

    private static final int LOADER_ID_UPDATE_ALL_WEB_UI_FAVORITE_STATE = IdUtils.generateIntId();
    private static final int LOADER_ID_GENERATE_HTML = IdUtils.generateIntId();
    private int loaderId_generateHtml;

    protected void updateAllWebUiFavoriteState() {
        LoaderManager loaderManager = componentHost.getActivity().getLoaderManager();
        if (loaderManager.getLoader(LOADER_ID_UPDATE_ALL_WEB_UI_FAVORITE_STATE) != null) {
            loaderManager.restartLoader(LOADER_ID_UPDATE_ALL_WEB_UI_FAVORITE_STATE, null, loaderCallbacksFavoriteState);
        } else {
            loaderManager.initLoader(LOADER_ID_UPDATE_ALL_WEB_UI_FAVORITE_STATE, null, loaderCallbacksFavoriteState);
        }
    }

    private static class LoaderDataFavoriteState {
        public final DOI doi;
        public boolean favorite = false;
        public boolean restricted = false;

        public LoaderDataFavoriteState(DOI doi) {
            this.doi = doi;
        }
    }

    private LoaderManager.LoaderCallbacks<List<LoaderDataFavoriteState>> loaderCallbacksFavoriteState = new LoaderManager.LoaderCallbacks<List<LoaderDataFavoriteState>>() {
        @Override
        public Loader<List<LoaderDataFavoriteState>> onCreateLoader(int id, Bundle args) {
            final List<DOI> doisForCheck = new ArrayList<>();
            for (ArticleMO article : currentArticles)
                doisForCheck.add(article.getDOI());
            return new AsyncTaskLoader<List<LoaderDataFavoriteState>>(componentHost.getContext()) {
                @Override
                public List<LoaderDataFavoriteState> loadInBackground() {
                    List<LoaderDataFavoriteState> result = new ArrayList<>();
                    for (DOI doi : doisForCheck) {
                        LoaderDataFavoriteState state = new LoaderDataFavoriteState(doi);
                        state.favorite = articleService.isArticleFavorite(doi);
                        state.restricted = articleService.isArticleRestricted(doi);
                        result.add(state);
                    }
                    return result;
                }

                @Override
                protected void onStartLoading() {
                    super.onStartLoading();
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<LoaderDataFavoriteState>> loader, List<LoaderDataFavoriteState> data) {
            applyFavoriteStates(data);

            final Activity activity = componentHost.getActivity();
            if (activity != null) {
                final LoaderManager loaderManager = activity.getLoaderManager();
                if (loaderManager != null) {
                    loaderManager.destroyLoader(LOADER_ID_UPDATE_ALL_WEB_UI_FAVORITE_STATE);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<List<LoaderDataFavoriteState>> loader) {
        }
    };

    private final LoaderManager.LoaderCallbacks<String> loaderCallbacksGenerateHtml = new LoaderManager.LoaderCallbacks<String>() {

        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            componentHost.onRenderStarted();
            return new AsyncTaskLoader<String>(componentHost.getContext()) {
                @Override
                public String loadInBackground() {
                    return generateHtml();
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
            webView.loadDataWithBaseURL(BASE_TOC_URL, data, "text/html", "UTF-8", null);

            final Activity activity = componentHost.getActivity();
            if (activity != null) {
                final LoaderManager loaderManager = activity.getLoaderManager();
                if (loaderManager != null) {
                    loaderManager.destroyLoader(loaderId_generateHtml);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
        }
    };

    private void applyFavoriteStates(List<LoaderDataFavoriteState> states) {
        for (LoaderDataFavoriteState state : states)
            updateWebUiFavoriteState(state.doi, state.favorite, state.restricted);
    }

    protected void updateWebUiFavoriteState(final DOI doi, final boolean favorite, final boolean restricted) {
        final ArticleMO article = articleService.getArticle(doi);
        webView.setElementPropertyValue("article_bookmark_element_id_" + article.getUid(), "src", htmlCodeForBookmarkElement(doi, favorite));
        updateRestrictedStatus(doi, restricted);
    }

    protected void setArticleFavStateIsChanging(final ArticleMO article) {
        String image = HtmlUtils.getAssetsImgUrl("ArticleList/loading.gif");
        webView.setElementPropertyValue("article_bookmark_element_id_" + article.getUid(), "src", image);
    }

    protected void updateRestrictedStatus(DOI doi, boolean restricted) {
        final String lockIconID = doi.getIdCompatibleValue() + "_lock_icon";
        String lockIconDisplay = restricted ? "inline" : "none";
        webView.setElementPropertyValue(lockIconID, "style.display", lockIconDisplay);
    }

    protected void updateFontSize() {
        final int fontSize = settings.getArticleFontSize();
        final String js = format("window.document.body.style.fontSize='%dpx'", fontSize);
        webView.executeJavaScript(js);
        updateHeaderLocations();
        initStickyHeaders(webView);
    }

    public void onOrientationChanged() {
        if (!isShowAbstract() && !DeviceUtils.isPhone(context)) {
            webView.setElementVisibility(".centered_images_class", !DeviceUtils.isLandscape(context));
            webView.setElementVisibility(".right_images_class", DeviceUtils.isLandscape(context));
        }
    }

    private boolean isShowAbstract() {
        return !theme.isJournalHasNoTextAbstracts() && settings.getArticleShowAbstract();
    }

    private void notifySectionBecomeVisible(String sectionId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("section_id", sectionId);
        notificationCenter.sendNotification(SECTION_DID_BECOME_VISIBLE.getEventName(), params);
    }

    protected boolean needSectionId() {
        return false;
    }

    private void retrieveCurrentSectionId() {
        if (!needSectionId() || !sendScrollNotifications) {
            return;
        }
        webView.executeJavaScriptAndGetResult("return getFirstVisibleSectionId();", new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                if (isEmpty(result)) {
                    return;
                }
                notifySectionBecomeVisible(result);
            }
        });
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == Authorizer.REQUEST_CODE_ADD_TO_FAVORITES) {
            pendingDoi = data.getExtras() != null ? (DOI) data.getExtras().get(Extras.EXTRA_DOI) : null;
            return true;
        }
        return false;
    }

    public void setHeadersLayout(RelativeLayout headersLayout) {
        this.headersLayout = headersLayout;
    }

    private void addPendingToFavorites() {
        if (pendingDoi != null) {
            try {
                final ArticleMO article = articleService.getArticleFromDao(pendingDoi);
                setArticleFavStateIsChanging(article);
                articleService.addArticleRefToFavorites(article);
            } catch (ElementNotFoundException e) {
                Logger.d(TAG, e.getMessage(), e);
            }
            pendingDoi = null;
        }
    }

    private interface PostponeTask {
        void doTask();
    }
}