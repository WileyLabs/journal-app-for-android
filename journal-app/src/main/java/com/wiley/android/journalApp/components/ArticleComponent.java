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
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.controller.ConnectionController;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.notification.ArticleFavoriteStateChangeProcessor;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.IosUtils;
import com.wiley.android.journalApp.utils.OnShortTouchListener;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.android.journalApp.widget.CircleIndexIndicator;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;

public class ArticleComponent {
    private static final String SCHEME_GET_ACCESS = "getaccess://";
    private static final String SCHEME_BODY_TOUCH = "bodytouched://";
    private static final String SCHEME_FAV_ACTION = "favoriteaction://";
    private static final String SCHEME_OPEN_FIGURE = "openfig://";
    private static final String SCHEME_SCROLL_TO_FIGURE = "scrolltofig://";
    private static final String SCHEME_LOAD_SECTIONS = "sections://";
    private static final String SCHEME_OPEN_AUTHORS = "openauthors://";
    private static final String BLOCK = "block";
    private static final String NONE = "none";
    private static final String INLINE = "inline";
    protected final Templates templates = new Templates();
    private final Context mContext;
    private final ArticleComponentHost mComponentHost;
    @Inject
    private Authorizer mAuthorizer;
    @Inject
    private ConnectionController mConnectionController;
    @Inject
    private Settings mSettings;
    @Inject
    private Theme mTheme;
    @Inject
    private ArticleService articleService;
    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    @InjectCachePath
    protected String cachePath;
    private boolean mShowLoadingError;
    private boolean mShowGetAccess;
    private boolean mShowFullContent;
    private boolean mShowOfflineMsg;
    private boolean mShowLoading;
    private boolean mWebViewLoading;
    private boolean mShowArticleSaving;
    private boolean mMarkedAsRead;
    private String mLoadingErrorMessage;
    private Activity mActivity;
    private ArticleMO mArticle;
    private CustomWebView mWebView;
    private CircleIndexIndicator mIndexIndicator;
    private FloatingStar mFloatingStar;
    private View mProgressBar;
    private Listener mListener;

    // 'Authors Information' popover
    private int authorsPopoverWidth = -1;
    private int authorsPopoverHeight = -1;
    private int scrollValue = -1;

    protected final ArticleFavoriteStateChangeProcessor mArticleFavStateChanged = new ArticleFavoriteStateChangeProcessor() {
        @Override
        protected boolean needProcess(Integer uid) {
            return mArticle != null && mArticle.getUid().equals(uid);
        }

        @Override
        protected void onSuccess(ArticleMO article) {
            mShowArticleSaving = false;
            if (mArticle != null) {
                mArticle = article;
                updateFavouriteState();
            }
        }

        @Override
        protected void onNoConnection(final ArticleMO article) {
            postDelayedUpdateFavouriteState();
            mComponentHost.onSaveArticleNoInternetConnection(article);
        }

        @Override
        protected void onNoAccess(ArticleMO article) {
            postDelayedUpdateFavouriteState();
            mComponentHost.onAccessForbiddenArticle();
        }
    };

    private void postDelayedUpdateFavouriteState() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateFavouriteState();
            }
        }, 100);
    }

    public ArticleComponent(final ArticleComponentHost host,
                            final CustomWebView webView,
                            final CircleIndexIndicator indexIndicator,
                            final FloatingStar floatingStar,
                            final View progressBar) {
        mComponentHost = host;
        mActivity = mComponentHost.getActivity();
        mContext = mActivity.getApplicationContext();
        mWebView = webView;
        mIndexIndicator = indexIndicator;
        mIndexIndicator.clearItems();
        mIndexIndicator.addListener(mIndexIndicatorListener);
        mProgressBar = progressBar;
        initializeWebView();

        mFloatingStar = floatingStar;
        if (mFloatingStar != null) {
            mFloatingStar.hide();
            mFloatingStar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mFloatingStar.getState() != FloatingStar.State.Spinning) {
                        toggleFavoriteState();
                    }
                }
            });
        }

        RoboInjector injector = RoboGuice.getInjector(mContext);
        injector.injectMembersWithoutViews(this);
    }

    private void initializeWebView() {
        mWebView.setOnTouchListener(new OnShortTouchListener() {
            @Override
            public void onShortTouch() {
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mListener != null) {
                    mListener.onWebViewTouched();
                }
                return super.onTouch(v, event);
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (mListener != null) {
                    mListener.onArticleViewStartLoadContent(ArticleComponent.this, mArticle);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                onWebViewFinishedPageLoad();
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                if (url == null) {
                    return true;
                }
                if (url.contains(SCHEME_GET_ACCESS)) {
                    if (mListener != null) {
                        mListener.onAccessRequested();
                    }
                } else if (url.startsWith(SCHEME_BODY_TOUCH)) {
                    if (mListener != null) {
                        mListener.onBodyTouched(url.replace(SCHEME_BODY_TOUCH, ""));
                    }
                } else if (url.startsWith(SCHEME_FAV_ACTION)) {
                    toggleFavoriteState(mArticle);
                } else if (url.startsWith(SCHEME_OPEN_FIGURE)) {
                    if (mListener != null) {
                        mListener.onOpenFigure(url.replace(SCHEME_OPEN_FIGURE, ""));
                    }
                } else if (url.startsWith(SCHEME_SCROLL_TO_FIGURE)) {
                    if (mListener != null) {
                        mListener.onOpenFigure(url.replace(SCHEME_SCROLL_TO_FIGURE, ""));
                    }
                } else if (url.startsWith(SCHEME_LOAD_SECTIONS)) {
                    mWebView.post(new Runnable() {
                        @Override
                        public void run() {
                            reloadSections();
                        }
                    });
                } else if (url.startsWith(SCHEME_OPEN_AUTHORS)) {
                    onOpenAuthors(url.replace(SCHEME_OPEN_AUTHORS, ""));
                } else {
                    if (mListener != null) {
                        mListener.onOpenCustomScheme(url);
                    }
                }
                return true;
            }
        });

        mWebView.setOnScrollListener(new CustomWebView.OnScrollListener() {
            @Override
            public void onScrollStarted(int l, int t) {
                hideFloatingStar();
            }

            @Override
            public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
                markArticleAsReadIfNeeded();
                if (!dontChangeSelectedSectionByScroll) {
                    changeSelectedSectionByScroll(t);
                }
            }

            @Override
            public void onScrollEnded(int l, int t) {
                showFloatingStar(true);
                if (dontChangeSelectedSectionByScroll) {
                    dontChangeSelectedSectionByScroll = false;
                    changeSelectedSectionByScroll(t);
                }
            }
        });
    }

    private void onOpenAuthors(String target) {
        if (null == mListener || TextUtils.isEmpty(mArticle.getAffiliationBlock())) {
            return;
        }

        scrollValue = -1;
        authorsPopoverWidth = -1;
        final boolean isPhone = DeviceUtils.isPhone(mContext);
        final String js = String.format("return getElement%sRect('%s');", isPhone ? "Abs" : "", target);
        mWebView.executeJavaScriptAndGetResult(js, new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(final String result) {
                Rect rect = IosUtils.parseIosRectFromString(result);
                if (null == rect) {
                    return;
                }

                if (isPhone) {
                    Rect sourceRect = rect;
                    rect = UIUtils.dpToPxRect(mContext, rect);
                    int[] loc = new int[2];
                    mWebView.getLocationInWindow(loc);
                    rect.offset(loc[0], loc[1]);
                    mListener.onOpenAuthors(new Point(rect.centerX(), rect.top + rect.height()), sourceRect);
                } else {
                    Rect sourceRect = rect;
                    rect = UIUtils.dpToPxRect(mContext, rect);

                    authorsPopoverWidth = rect.width();
                    int[] loc = new int[2];
                    mWebView.getLocationInWindow(loc);
                    int xNewCenterPopover = loc[0] + (mWebView.getWidth() / 2);
                    int yNewTopPopover = loc[1] + mWebView.getHeight() - (getAuthorsInformationPopoverHeight());
                    int bottomWebElement = (rect.centerY() + rect.height() / 2);
                    if (mWebView.getHeight() - bottomWebElement > getAuthorsInformationPopoverHeight()) {
                        yNewTopPopover = loc[1] + bottomWebElement;
                    } else {
                        // calculate scrolling for 'Authors' web element
                        int topPopoverInWindow = loc[1] + mWebView.getHeight() - (getAuthorsInformationPopoverHeight());

                        Rect rectWebElement = UIUtils.dpToPxRect(mContext, IosUtils.parseIosRectFromString(result));
                        int bottomWebElementInWindow = loc[1] + rectWebElement.centerY() + rectWebElement.height() / 2;

                        scrollValue = bottomWebElementInWindow - topPopoverInWindow;
                    }

                    mListener.onOpenAuthors(new Point(xNewCenterPopover, yNewTopPopover), sourceRect);
                }
            }
        });
    }

    private int getAuthorsInformationPopoverHeight() {
        if (authorsPopoverHeight < 0) {
            authorsPopoverHeight = ((int) mContext.getResources().getDimension(R.dimen.article_authors_info_view_height)) + 15;
        }
        return authorsPopoverHeight;
    }

    public int getAuthorsWebElementWidth() {
        if (authorsPopoverWidth < 0) {
            authorsPopoverWidth = ((int) mContext.getResources().getDimension(R.dimen.article_authors_info_view_width));
        }
        return authorsPopoverWidth;
    }


    public void scrollAuthorsInformationWebElement() {
        if (scrollValue > 0) {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.scrollBy(0, scrollValue);
                }
            });
        }
    }

    private List<Runnable> pendingActionsForFinishedPageLoad = new ArrayList<>();

    private void onWebViewFinishedPageLoad() {
        if (mWebViewLoading) {
            mWebViewLoading = false;
            mComponentHost.onRenderCompleted();
        }

        // update article view
        setShowLoading(mShowLoading);
        setShowGetAccess(mShowGetAccess);
        setShowOfflineMsg(mShowOfflineMsg);
        setShowFullContent(mShowFullContent);
        setShowLoadingError(mShowLoadingError);
        setShowArticleSaving(mShowArticleSaving);

        showFloatingStar(true);

        if (mListener != null) {
            mListener.onArticleViewHasLoadedContent(this, mArticle);
        }

        for (Runnable action : pendingActionsForFinishedPageLoad)
            action.run();
        pendingActionsForFinishedPageLoad.clear();
    }

    private String emptyOrValue(final String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value;
    }

    private String blockOrNone(final String value) {
        return !TextUtils.isEmpty(value) ? BLOCK : NONE;
    }

    private String blockOrNoneBool(final boolean value) {
        return value ? BLOCK : NONE;
    }

    private String inlineOrNone(final String value) {
        return !TextUtils.isEmpty(value) ? INLINE : NONE;
    }

    private String inlineOrNoneBool(final boolean value) {
        return value ? INLINE : NONE;
    }

    private void loadContent() {
        mWebViewLoading = true;
        boolean isPhone = DeviceUtils.isPhone(mContext);

        String fullText = articleService.getFullHtmlBody(mArticle);
        String arAbstract = getAbstract();

        String atitle = !TextUtils.isEmpty(mArticle.getTitle()) ? mArticle.getTitle() : "";
        String section = (mArticle.getSection() != null && !TextUtils.isEmpty(mArticle.getSection().getName()))
                ? mArticle.getSection().getName() : mArticle.getTocHeading1();
        String subsection = emptyOrValue(mArticle.getTocHeading2());
        String pdate = emptyOrValue(mArticle.getFirstOnlineDate());
        String authorsTop = emptyOrValue(mArticle.getFullAuthorList());
        authorsTop = !TextUtils.isEmpty(authorsTop) ? authorsTop : emptyOrValue(mArticle.getSimpleAuthorList());
        String authorsTitle = mArticle.isOneAuthor() ? mContext.getString(R.string.author) : mContext.getString(R.string.authors);
        String keywords = emptyOrValue(mArticle.getKeywords());
        String doi = mArticle.getDOI() == null ? "" : mArticle.getDOI().getValue();
        String note = emptyOrValue(mArticle.getNote());

        int[] widthsDp = getContentWidthSizesDp();
        int portWidthDp = widthsDp[0];
        int landWidthDp = widthsDp[1];
        String minContentWidthPortDp = Integer.toString(portWidthDp);
        String maxContentWidthPortDp = Integer.toString(portWidthDp);
        String minContentWidthLandDp = Integer.toString(landWidthDp);
        String maxContentWidthLandDp = Integer.toString(landWidthDp);

        String favIconPath = getFavIconPath();

        String authorsListColor = (!TextUtils.isEmpty(mArticle.getAffiliationBlock()) && !mArticle.isRestricted()) ? "#1271c9" : "#4444444";
        String authorsAffiliationBlockNotExists = (!TextUtils.isEmpty(mArticle.getAffiliationBlock()) && !mArticle.isRestricted()) ? "false" : "true";

        if (authorsTop.endsWith("<br />")) {
            authorsTop = authorsTop.substring(0, authorsTop.length() - 6);
        }
        authorsTop = authorsTop.replaceAll("<br />", ", ");

        String thumb = "";
        if (!TextUtils.isEmpty(mArticle.getThumbnailLocal())) {
            //to do not treat abstract image as figure
            boolean isWholeWidthThumbnail = isPhone || mArticle.isWholeWidthThumbnail();
            boolean isAbstract = !arAbstract.contains("<div class=\"para\"></div>");

            thumb = String.format("%s<img src='%s' class='%s %s' />%s",
                    isWholeWidthThumbnail ? "<center class='abstract_thumbnail'>" : "",
                    mArticle.getThumbnailLocal(),
                    isWholeWidthThumbnail ? "wholeWidthImg" : "halfWidthImg",
                    isWholeWidthThumbnail ? "" : isAbstract ? "pull-right" : "pull-left",
                    isWholeWidthThumbnail ? "</center>" : "");
        }

        section = emptyOrValue(section);

        String displayMeta = blockOrNoneBool(false);
        String displayLeftMeta = blockOrNoneBool(true);

        int fontSizeInt = mSettings.getArticleFontSize();
        String fs = "" + fontSizeInt;

        String subsectionBackColor = !TextUtils.isEmpty(subsection) ? "#fafafa" : "#ffffff";

        String displayAuthors = blockOrNone(authorsTop);
        String displayKeywords = blockOrNone(keywords);
        String displayPDate = blockOrNone(pdate);
        String displayDoi = blockOrNone(doi);
        String displaySectsDelim = inlineOrNoneBool(TextUtils.isEmpty(section) || TextUtils.isEmpty(subsection));//section.length == 0 || subsection.length == 0 ? dnone : dinline;
        String displayHeader = (TextUtils.isEmpty(section) && TextUtils.isEmpty(subsection)) ? "display: none" : "";
        String displayAbstractDelim = blockOrNone(arAbstract);
        String displayFTDelim;
        if (!TextUtils.isEmpty(fullText) || mArticle.isRestricted()) {
            // TODO check this
            displayFTDelim = blockOrNoneBool(!TextUtils.isEmpty(arAbstract) || TextUtils.isEmpty(authorsTop));
        } else {
            displayFTDelim = NONE;
        }
        String displayNote = blockOrNone(note);
        String displayArticleHeadingBranding = inlineOrNoneBool(false);
        String displayLoadingErrorMessage = blockOrNoneBool(mShowLoadingError);
        String displaySectionNameDivider = inlineOrNone(mArticle.getTocHeading2());
        String displayAbstractBlock = getShowAbstract() ? BLOCK : NONE;

        // Access
        String displayGetAccess = blockOrNoneBool(mShowGetAccess && mArticle.isRestricted() && !isAuthorized());
        String displayGetAccessNotSubscribed = blockOrNoneBool(mShowGetAccess && mArticle.isRestricted() && isAuthorized());

        String displayArticleFullContent = blockOrNoneBool(NONE.equals(displayGetAccess) && NONE.equals(displayGetAccessNotSubscribed) && mShowFullContent);
        String displayOfflineMsg = blockOrNoneBool(mShowOfflineMsg);

        final Template articleTemplate = templates.useAssetsTemplate(mContext, "article");

        final String articleHtml = articleTemplate.
                putParam("authors_title", authorsTitle).
                putParam("artile_left_meta_display", displayLeftMeta).
                putParam("article_top_meta_display", displayMeta).
                putParam("article_section_display", BLOCK).
                putParam("article_publishing_date", pdate).
                putParam("_heading_divider_display_placeholder_", displaySectionNameDivider).
                putParam("article_subsection", subsection).
                putParam("article_keywords", keywords).
                putParam("article_authors_top", authorsTop).
                putParam("article_section", section).
                putParam("article_title", atitle).
                putParam("article_doi", doi).
                putParam("article_note", note).
                putParam("article_full_text", fullText).
                putParam("article_abstract", arAbstract).
                putParam("article_abstract_thumbnail", thumb).
                putParam("font_size", fs).

                putParam("min_content_width_port", minContentWidthPortDp).
                putParam("max_content_width_port", maxContentWidthPortDp).
                putParam("min_content_width_land", minContentWidthLandDp).
                putParam("max_content_width_land", maxContentWidthLandDp).

                putParam("article_subsection_background_color", subsectionBackColor).

                putParam("article_abstract_delim_display", displayAbstractDelim).
                putParam("article_note_display", displayNote).
                putParam("article_full_text_delim_display", displayFTDelim).

                putParam("article_sections_delim_display", displaySectsDelim).
                putParam("article_publishing_date_display", displayPDate).
                putParam("article_authors_display", displayAuthors).
                putParam("article_keywords_display", displayKeywords).
                putParam("article_doi_display", displayDoi).
                putParam("acie_article_top_logo_display", displayArticleHeadingBranding).
                putParam("article_full_content_div_display", displayArticleFullContent).
                putParam("access_denied_display", displayGetAccess).
                putParam("access_denied_not_subscribed_display", displayGetAccessNotSubscribed).
                putParam("offline_div_display", displayOfflineMsg).
                putParam("article_abstract_block_display", displayAbstractBlock).
                putParam("header_display", displayHeader).

                putParam("loading_error_message_display", displayLoadingErrorMessage).
                putParam("loading_error_message", emptyOrValue(mLoadingErrorMessage)).

                putParam("authors_list_color", authorsListColor).
                putParam("authors_affiliation_not_exists", authorsAffiliationBlockNotExists).
                putParam("favorite_icon_path", favIconPath).
                putParam(!isPhone ? "favorite_icon_ipad" : "favorite_icon_iphone", "favorite_icon").
                putParam("journal_name", mTheme.getJournalName()).
                proceed();
        mWebView.loadData(articleHtml);
    }

    private int[] getContentWidthSizesDp() {
        int defaultContentPaddingDp = 100;
        Point screenSizeDp = UIUtils.getRawDisplaySizeDp(mContext);

        if (DeviceUtils.isPhone(mContext)) {
            int screenWidthDp = screenSizeDp.x;
            int contentWidthDp = screenWidthDp - defaultContentPaddingDp;
            return new int[]{contentWidthDp, contentWidthDp};
        } else {
            int minScreenSideDp = Math.min(screenSizeDp.x, screenSizeDp.y);
            int maxScreenSideDp = Math.max(screenSizeDp.x, screenSizeDp.y);

            int sideMenuWidthDp = UIUtils.pxToDp(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.article_side_menu_width));
            int centerMenuWidthDp = UIUtils.pxToDp(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.article_center_menu_width));

            int portraitSizeDp = minScreenSideDp - sideMenuWidthDp - defaultContentPaddingDp;
            int landscapeSizeDp = maxScreenSideDp - sideMenuWidthDp - centerMenuWidthDp;

            return new int[]{portraitSizeDp, landscapeSizeDp};
        }
    }

    public void reloadArticleWithCustomData(final String customBody, final String customAbstract, final String customTitle, final String customAuthors) {
        if (mArticle == null) {
            return;
        }

        if (mWebViewLoading) {
            pendingActionsForFinishedPageLoad.add(new Runnable() {
                @Override
                public void run() {
                    reloadArticleWithCustomData(customBody, customAbstract, customTitle, customAuthors);
                }
            });
            return;
        }

        String bodyValue = "";
        boolean bodyPermitted = mArticle.isLocal() || (!mArticle.isRestricted() && !mArticle.isExpired());
        String realHtmlBody = articleService.getFullHtmlBody(mArticle);
        if (TextUtils.isEmpty(customBody) && (bodyPermitted && !TextUtils.isEmpty(realHtmlBody))) {
            bodyValue = realHtmlBody;
        } else if (!TextUtils.isEmpty(customBody)) {
            bodyValue = customBody;
        }

        String abstractValue = "";
        if (TextUtils.isEmpty(customAbstract) && !TextUtils.isEmpty(getAbstract())) {
            abstractValue = getAbstract();
        } else if (!TextUtils.isEmpty(customAbstract)) {
            abstractValue = customAbstract;
        }

        String article_title = "";
        final String storedTitle = mArticle.getTitle();
        if (TextUtils.isEmpty(customTitle) && !TextUtils.isEmpty(storedTitle)) {
            article_title = storedTitle;
        } else if (!TextUtils.isEmpty(customTitle)) {
            article_title = customTitle;
        }

        String section = (mArticle.getSection() != null && !TextUtils.isEmpty(mArticle.getSection().getName()))
                ? mArticle.getSection().getName() : mArticle.getTocHeading1();
        String subsection = emptyOrValue(mArticle.getTocHeading2());
        String displayHeader = (TextUtils.isEmpty(section) && TextUtils.isEmpty(subsection)) ? "display: none" : "";
        String displaySectionNameDivider = inlineOrNone(mArticle.getTocHeading2());
        String displayFavoriteIcon = DeviceUtils.isPhone(mContext) ? "display: none" : "";
        String titleValue = "<div id=\"article_title\" style=\"position:relative;\">"
                + "  <div class=\"article_section_info_class\" style=\"line-height:normal; margin-bottom:7px; " + displayHeader + "\">"
                + "    <span class=\"level1_heding_class\">" + section + "</span>"
                + "    <span calss=\"heading_divider_class\" style=\"display:" + displaySectionNameDivider + "\"> | </span>"
                + "    <span class=\"level2_heding_class\">" + subsection + "</span>"
                + "  </div>"
                + "  <div ontouchend=\"window.location.href='favoriteaction://'\" class=\"touchable_element article_favorite_icon iPad\" style=\"" + displayFavoriteIcon + "\">"
                + "    <image src=\"" + getFavIconPath() + "\" id=\"favorite_icon\" width=\"28px\" height=\"28px\" />"
                + "  </div>"
                + article_title
                + "</div>";

        String authorsValue = "";
        final String storedAuthors = mArticle.getSimpleAuthorList();
        if (TextUtils.isEmpty(customAuthors) && !TextUtils.isEmpty(storedAuthors)) {
            authorsValue = storedAuthors;
        } else if (!TextUtils.isEmpty(customAuthors)) {
            authorsValue = customAuthors;
        }

        String bodyTempName = mWebView.pushTempData(bodyValue);
        String abstractTempName = mWebView.pushTempData(abstractValue);
        String titleTempName = mWebView.pushTempData(titleValue);
        String authorsTempName = mWebView.pushTempData(authorsValue);

        mWebView.executeJavaScript(String.format("document.getElementById('article_abstract_div').innerHTML = hostCallbacks.popTempData('%s');" +
                "document.getElementById('article_full_text_div').innerHTML = hostCallbacks.popTempData('%s');" +
                "document.getElementById('article_title').innerHTML = hostCallbacks.popTempData('%s');" +
                "document.getElementById('article_authors_list_top').innerHTML = hostCallbacks.popTempData('%s');" +
                "domContentLoadedFunctions();", abstractTempName, bodyTempName, titleTempName, authorsTempName));
        updateFavouriteState();
    }

    private String getAbstract() {
        if (mArticle.isRestricted() || TextUtils.isEmpty(mArticle.getFullTextAbstract())) {
            return mArticle.getSummary();
        }
        return mArticle.getFullTextAbstract();
    }

    public void showArticle(final ArticleMO article, boolean loading) {
        mArticle = article;
        if (mArticle == null) {
            mWebView.loadData("");
            return;
        }

        boolean offline = !mConnectionController.isOnline();

        mShowGetAccess = !loading && !offline && mArticle.isRestricted();
        mShowFullContent = !loading && ((!mArticle.isRestricted() && !mArticle.isExpired()) || mArticle.isLocal());
        mShowOfflineMsg = offline && !mShowFullContent;
        mShowLoading = !offline && loading;
        if (mShowGetAccess || mShowOfflineMsg || mShowFullContent || mShowLoading) {
            mLoadingErrorMessage = "";
            mShowLoadingError = false;
        }
        mMarkedAsRead = mArticle.isRead();
        loadContent();
    }

    public ArticleMO getLoadedArticle() {
        return mArticle;
    }

    public DOI getDoi() {
        return mArticle.getDOI();
    }

    private boolean isAuthorized() {
        return mAuthorizer.isAuthorized();
    }

    private String getFavIconPath() {
        String image = HtmlUtils.getAssetsImgUrl("ArticleList/favorite_normal@2x.png");
        boolean showLoading = articleService.isArticleRefFavoriteChangingInProgress(mArticle.getDOI())
                || articleService.isArticleRefUpdating(mArticle.getDOI());
        if (showLoading) {
            image = HtmlUtils.getAssetsImgUrl("ArticleList/loading.gif");
        } else if (articleService.isArticleFavorite(mArticle.getDOI())) {
            image = HtmlUtils.getAssetsImgUrl("ArticleList/favorite_hilighted@2x.png");
        }
        return image;
    }

    public void setListener(final Listener l) {
        mListener = l;
    }

    public void setShowLoading(boolean showLoading) {
        mShowLoading = showLoading;
        doShowLoading();
    }

    private void doShowLoading() {
        if (mWebViewLoading) {
            return;
        }

        if (mShowLoading) {

            String javascript = String.format("document.getElementById('article_abstract_block').style.display = 'none';" +
                            "document.getElementById('loading_error_message_div').style.display = 'none';" +
                            "document.getElementById('loading_div').style.display = 'block';" +
                            "document.getElementById('loading_div').style.height = '%dpx';" +
                            "return getElementAbsRect('loading_div');",
                    UIUtils.pxToDp(mActivity, mProgressBar.getMeasuredHeight())
            );
            mWebView.executeJavaScriptAndGetResult(javascript, new CustomWebView.JavaScriptExecutionCallback() {
                @Override
                public void onJavaScriptResult(String result) {
                    FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout.gravity = Gravity.CENTER;

                    if (!TextUtils.isEmpty(result) && !result.equals("undefined")) {
                        Rect rectDpInWebView = parseRect(result);
                        if (rectDpInWebView != null) {
                            int yPx = UIUtils.dpToPx(mActivity, rectDpInWebView.top);
                            if ((yPx + mProgressBar.getMeasuredHeight()) < mWebView.getMeasuredHeight()) {
                                layout.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                                layout.topMargin = yPx;
                            }
                        }
                    }
                    mProgressBar.setLayoutParams(layout);
                }
            });
        } else {
            mWebView.executeJavaScript(String.format(
                    "document.getElementById('article_abstract_block').style.display = '%s';" +
                            "document.getElementById('loading_div').style.display = 'none';",
                    getShowAbstract() ? "block" : "none"
            ));

        }
    }

    public void setShowGetAccess(boolean showGetAccess) {
        this.mShowGetAccess = showGetAccess;
        doShowGetAccess();
    }

    private void doShowGetAccess() {
        if (mWebViewLoading) {
            return;
        }

        String displayAccess = NONE;
        String displayAccessNotSubscribed = NONE;
        if (mShowGetAccess) {
            if (isAuthorized()) {
                displayAccessNotSubscribed = BLOCK;
            } else {
                displayAccess = BLOCK;
            }
        }

        final String javascript = String.format("document.getElementById('access_denied').style.display='%s';" +
                        "document.getElementById('access_denied_not_subscribed').style.display='%s';",
                displayAccess,
                displayAccessNotSubscribed
        );

        mWebView.executeJavaScript(javascript);
    }

    public boolean getShowFullContent() {
        return mShowFullContent;
    }

    public void setShowFullContent(boolean showFullContent) {
        this.mShowFullContent = showFullContent;
        if (mWebViewLoading) {
            return;
        }

        final String javascript = String.format("showArticleFullContent('%s');", mShowFullContent ? BLOCK : NONE);
        mWebView.executeJavaScript(javascript);
    }

    public void setShowOfflineMsg(boolean showOfflineMsg) {
        this.mShowOfflineMsg = showOfflineMsg;
        doShowOfflineMsg();
    }

    private void doShowOfflineMsg() {
        if (mWebViewLoading) {
            return;
        }

        final String javascript = String.format("document.getElementById('article_abstract_block').style.display = '%s';document.getElementById('offline_div').style.display='%s';",
                getShowAbstract() ? BLOCK : NONE,
                mShowOfflineMsg ? BLOCK : NONE);
        mWebView.executeJavaScript(javascript);
    }

    public void setShowLoadingError(boolean showLoadingError) {
        this.mShowLoadingError = showLoadingError;
        doShowLoadingError();
    }

    private void doShowLoadingError() {
        if (mWebViewLoading || (TextUtils.isEmpty(mLoadingErrorMessage) && mShowLoading)) {
            return;
        }

        mWebView.executeJavaScript(String.format(
                "document.getElementById('article_abstract_block').style.display = '%s';" +
                        "document.getElementById('article_full_text_delim').style.display = '%s';" +
                        "document.getElementById('loading_error_message_div').style.display = '%s';" +
                        "document.getElementById('loading_error_message_text_div').innerHTML = '%s';",
                getShowAbstract() ? BLOCK : NONE,
                (mShowLoadingError && !TextUtils.isEmpty(getAbstract())) ? BLOCK : NONE,
                mShowLoadingError ? BLOCK : NONE,
                mLoadingErrorMessage.replaceAll("'", "\\\\'")
        )); // TODO check count of '\'
    }

    public void setShowArticleSaving(boolean showArticleSaving) {
        this.mShowArticleSaving = showArticleSaving;
        doShowArticleSaving();
    }

    private void doShowArticleSaving() {
        if (mWebViewLoading) {
            return;
        }

        if (mShowArticleSaving) {
            if (DeviceUtils.isTablet(mContext)) {
                String loadingIcon = HtmlUtils.getAssetsImgUrl("loading.gif");
                final String javascript = String.format("document.getElementById('favorite_icon').src='%s';", loadingIcon);

                mWebView.executeJavaScript(javascript);
            } else {
                setFloatingStarLoading();
            }
        } else {
            updateFavouriteState();
        }
    }

    private boolean getShowAbstract() {
        return !mShowLoading;
    }

    private void showFloatingStar(boolean animate) {
        if (mFloatingStar != null) {
            mFloatingStar.show(animate);
        }
    }

    private void hideFloatingStar() {
        if (mFloatingStar != null) {
            mFloatingStar.hide();
        }
    }

    private void updateFavouriteState() {
        if (DeviceUtils.isTablet(mContext)) {
            String favIconPath = getFavIconPath();
            mWebView.executeJavaScript(String.format("document.getElementById('favorite_icon').src='%s';", favIconPath));
        } else {
            // Update floating star state img
            FloatingStar.State state;
            boolean showLoading = articleService.isArticleRefFavoriteChangingInProgress(mArticle.getDOI())
                    || articleService.isArticleRefUpdating(mArticle.getDOI());
            if (showLoading) {
                state = FloatingStar.State.Spinning;
            } else {
                state = mArticle.isFavorite() ? FloatingStar.State.Activated : FloatingStar.State.Deactivated;
            }
            if (mFloatingStar != null) {
                mFloatingStar.setState(state);
            }
        }
    }

    public void setFloatingStarLoading() {
        if (mFloatingStar != null) {
            mFloatingStar.setState(FloatingStar.State.Spinning);
        }
    }

    private CharSequence[] mSectionTitles = null;
    private Rect[] mSectionRectsDp = null;
    private Rect[] mSectionRectsPx = null;

    protected void reloadSections() {
        if (!getShowFullContent()) {
            clearSections();
            return;
        }

        mSectionTitles = null;
        mSectionRectsDp = null;
        mSectionRectsPx = null;
        mIndexIndicator.clearItems();
        reloadSectionTitles();

    }

    protected void reloadSectionTitles() {
        mWebView.executeJavaScriptAndGetResult("return loadSectionsTitles();", new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                if (TextUtils.isEmpty(result) || result.equals("undefined")) {
                    clearSections();
                    return;
                }

                String[] parts = result.split("\\|");
                mSectionTitles = new CharSequence[parts.length + 1];
                mSectionTitles[0] = "Article Title";
                System.arraycopy(parts, 0, mSectionTitles, 1, parts.length);
                mIndexIndicator.setItems(mSectionTitles);
                if (DeviceUtils.isPhone(mActivity)) {
                    mWebView.executeJavaScript("$('.article_body').css('margin-right', '24px');");
                }
                reloadSectionRects();
            }
        });
    }


    private final Pattern rectPattern = Pattern.compile("^\\s*\\{\\s*\\{\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\}\\s*,\\s*\\{\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\}\\s*\\}\\s*$");

    private Rect parseRect(String string) {
        Matcher matcher = rectPattern.matcher(string);
        if (matcher.find()) {
            return new Rect(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)));
        } else {
            return null;
        }
    }

    protected void reloadSectionRects() {
        mSectionRectsDp = null;
        mSectionRectsPx = null;
        mWebView.executeJavaScriptAndGetResult("return loadSectionsRects();", new CustomWebView.JavaScriptExecutionCallback() {

            @Override
            public void onJavaScriptResult(String result) {
                if (TextUtils.isEmpty(result) || result.equals("undefined")) {
                    clearSections();
                    return;
                }

                String[] parts = result.split("\\|");
                mSectionRectsDp = new Rect[parts.length + 1];
                mSectionRectsDp[0] = new Rect(0, 0, 1, 1);
                for (int i = 0; i < parts.length; i++) {
                    mSectionRectsDp[i + 1] = parseRect(parts[i]);
                }

                mSectionRectsPx = new Rect[mSectionRectsDp.length];
                for (int i = 0; i < mSectionRectsDp.length; i++) {
                    Rect rectDp = mSectionRectsDp[i];
                    Rect rectPx = new Rect(
                            UIUtils.dpToPx(mActivity, rectDp.left),
                            UIUtils.dpToPx(mActivity, rectDp.top),
                            UIUtils.dpToPx(mActivity, rectDp.right),
                            UIUtils.dpToPx(mActivity, rectDp.bottom));
                    mSectionRectsPx[i] = rectPx;
                }
            }
        });
    }

    protected void clearSections() {
        mSectionTitles = null;
        mSectionRectsDp = null;
        mSectionRectsPx = null;
        mIndexIndicator.clearItems();

        if (DeviceUtils.isPhone(mActivity)) {
            mWebView.executeJavaScript("$('.article_body').css('margin-right', '')");
        }
    }

    protected CircleIndexIndicator.Listener mIndexIndicatorListener = new CircleIndexIndicator.Listener() {
        @Override
        public void onSelectItem(int index) {
            scrollToSection(index);
        }
    };

    private boolean dontChangeSelectedSectionByScroll = false;

    protected void scrollToSection(int index) {
        if (mSectionRectsDp == null) {
            return;
        }

        if (index < 0 || index >= mSectionRectsDp.length) {
            return;
        }

        Rect sectionRect = mSectionRectsDp[index];

        dontChangeSelectedSectionByScroll = true;

        Point offset = new Point(0, sectionRect.top - (sectionRect.top > 15 ? 15 : 0));
        mWebView.scrollToPositionDp(offset.x, offset.y);
    }

    protected void changeSelectedSectionByScroll(int y) {
        if (mSectionRectsPx == null) {
            return;
        }

        int offset = mWebView.getMeasuredHeight() / 2;

        int selectedIndex = -1;
        if (mSectionRectsPx.length > 0 && y == 0) {
            selectedIndex = 0;
        } else {
            for (Rect rectPx : mSectionRectsPx) {
                if (y + offset >= rectPx.top) {
                    selectedIndex++;
                }
            }

            if (mSectionRectsPx.length > 1) {
                Rect lastRectPx = mSectionRectsPx[mSectionRectsPx.length - 1];
                if (lastRectPx.height() <= offset) {
                    if (y + 2 * offset >= lastRectPx.top) {
                        selectedIndex = mSectionRectsPx.length - 1;
                    }
                }
            }
        }
        mIndexIndicator.setSelectedItem(selectedIndex);
    }

    public void changeFontSize(int size) {
        mWebView.executeJavaScript(String.format("window.document.getElementById('article_main').style.fontSize='%spx'", size));
        reloadSectionRects();
    }

    public void scrollToBioSection() {
        if (mSectionRectsDp == null) {
            return;
        }
        scrollToSection(mSectionRectsDp.length - 1);
    }

    public void scrollToFigure(FigureMO figure) {
        mWebView.scrollToElement(figure.getShortCaption(), -50);
    }

    public void checkHasBiographySection(final Runnable onTrue, final Runnable onFalse) {
        String js = "return isBiographySectionExists();";
        mWebView.executeJavaScriptAndGetResult(js, new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                if ("true".equalsIgnoreCase(result)) {
                    if (onTrue != null) {
                        onTrue.run();
                    }
                } else {
                    if (onFalse != null) {
                        onFalse.run();
                    }
                }
            }
        });
    }

    public void toggleFavoriteState() {
        if (mArticle != null) {
            toggleFavoriteState(mArticle);
        }
    }

    private void toggleFavoriteState(final ArticleMO article) {
        if (articleService.isArticleRefFavoriteChangingInProgress(article.getDOI())) {
            return;
        }

        if (mArticle.isFavorite()) {
            UIUtils.showCancelRemove(mActivity,
                    mContext.getString(R.string.favorite_delete_article_title),
                    mContext.getString(R.string.favorite_delete_article_text),
                    new Runnable() {
                        @Override
                        public void run() {
                            setShowArticleSaving(true);
                            articleService.removeArticleRefFromFavorites(article);
                        }
                    }
            );
        } else {
            if (mArticle.isRestricted()) {
                if (mAuthorizer.isAuthorized()) {
                    setShowArticleSaving(true);
                    articleService.addArticleRefToFavorites(article);
                } else {
                    hidePopup();
                    mAuthorizer.requestAccessFromFavoriteAction(mComponentHost.getStartActivityForResultHelper(), article.getDOI());
                }
            } else {
                setShowArticleSaving(true);
                articleService.addArticleRefToFavorites(article);
                if (DeviceUtils.isPhone(mActivity)) {
                    ((MainActivity) mActivity).highlightJournalFavoriteMenuItem();
                }
            }
        }
    }

    private void hidePopup() {
        if (mComponentHost.getClass() == ArticleViewFragment.class) {
            ((ArticleViewFragment) mComponentHost).hidePopup();
        }
    }

    public void onStart() {
        mNotificationCenter.subscribeToNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), mArticleFavStateChanged);
    }

    public void onStop() {
        mNotificationCenter.unSubscribeFromNotification(mArticleFavStateChanged);
    }

    public void markArticleAsReadIfNeeded() {
        if (mMarkedAsRead) {
            return;
        }
        if (mArticle != null && !mArticle.isRead()) {
            articleService.markArticleAsRead(mArticle.getDOI());
            mArticle.setRead(true);
        }
        mMarkedAsRead = true;
    }

    /**
     * @see "https://code.google.com/p/android/issues/detail?id=42518#c12"
     */

    private final Runnable jsNotRespondingFixRunnable = new Runnable() {
        @Override
        public void run() {
            mWebView.callSuperOnScrollChanged(mWebView.getScrollX(), mWebView.getScrollY(), mWebView.getScrollX(), mWebView.getScrollY());
        }
    };

    public void applyJsNotRespondingFix() {
        mWebView.removeCallbacks(jsNotRespondingFixRunnable);
        mWebView.postDelayed(jsNotRespondingFixRunnable, 100);
    }

    public void onConfigurationChanged() {
        if (!DeviceUtils.isPhone(mContext)) {
            mWebView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    reloadSectionRects();
                    mIndexIndicator.requestLayout();
                }
            }, 500);
        }
    }

    public interface Listener {
        void onBodyTouched(String url);

        void onWebViewTouched();

        void onAccessRequested();

        void onOpenCustomScheme(String url);

        void onOpenFigure(String figId);

        void onArticleViewHasLoadedContent(ArticleComponent sender, ArticleMO article);

        void onArticleViewStartLoadContent(ArticleComponent sender, ArticleMO article);

        /**
         * @param at          Point for the authors popup to be shown under. In global px coordinates.
         * @param authorsRect Authors rect. In local webview's coordinates
         */
        void onOpenAuthors(Point at, Rect authorsRect);
    }

}
