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
package com.wiley.android.journalApp.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsReader;
import com.wiley.wol.client.android.settings.Settings;

import org.json.JSONObject;

import java.util.Map;

import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;

import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;
import static com.wiley.wol.client.android.notification.EventList.INFO_ARTICLE_UPDATE_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.KEYWORD_UPDATE_FINISHED;

public class ArticleInfoAdapter extends BaseAdapter {
    private final static int STATE_UNDEFINED = -1;
    private final static int STATE_LOADING_INFO_ARTICLE_BODY = 0;
    private final static int STATE_LOADED_INFO_ARTICLE_BODY = 1;
    private final static int STATE_CHANGING_KEYWORD = 2;
    private final static int STATE_CHANGED_KEYWORD = 3;

    @Inject
    private AANHelper aanHelper;
    @Inject
    private WebController webController;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ArticleService articleService;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private Settings settings;

    private final Context mContext;
    private final Activity mActivity;
    private ArticleMO mArticle;

    private InterfaceHelper mInterfaceHelper;
    private DataModelHelper mDataModelHelper;
    private boolean loadError = false;

    private final NotificationProcessor finishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            loadError = params.containsKey(NotificationCenter.ERROR);
            refreshUi(STATE_LOADED_INFO_ARTICLE_BODY);
        }
    };

    private final NotificationProcessor keywordUpdateFinishedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            final ParamsReader pr = new ParamsReader(params);
            if (pr.succeed()) {
                final JSONObject json = pr.getParam(NotificationCenter.KEYWORD_JSON);
                mInterfaceHelper.changeKeywordStatus(json);
            }

            refreshUi(STATE_CHANGED_KEYWORD);
        }
    };


    public ArticleInfoAdapter(final Activity activity, final ArticleMO article) {
        mContext = activity;
        mActivity = activity;
        mArticle = article;
        mDataModelHelper = new DataModelHelper();
        mInterfaceHelper = new InterfaceHelper(new InterfaceListener() {
            @Override
            public void onStateChanged(int state) {
                switch (state) {
                    case STATE_LOADING_INFO_ARTICLE_BODY: // loading info article body: progress.show
                        notificationCenter.subscribeToNotification(INFO_ARTICLE_UPDATE_FINISHED.getEventName(), finishedProcessor);
                        mDataModelHelper.updateArticleInfoHtmlBody(mArticle);
                        break;
                    case STATE_LOADED_INFO_ARTICLE_BODY: // loaded info body: progress.hide, webView.show
                        notificationCenter.unSubscribeFromNotification(finishedProcessor);
                        break;
                    case STATE_CHANGING_KEYWORD: // changing keyword: progress.show, webView.show
                        break;
                    case STATE_CHANGED_KEYWORD: // changed keyword: progress.hide, webView.show
                        break;
                }

            }
        });

        final RoboInjector injector = RoboGuice.getInjector(mContext);
        injector.injectMembersWithoutViews(this);

        notificationCenter.subscribeToNotification(KEYWORD_UPDATE_FINISHED.getEventName(), keywordUpdateFinishedProcessor);
    }

    public void onStop() {
        notificationCenter.unSubscribeFromNotification(keywordUpdateFinishedProcessor);
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Object getItem(final int i) {
        return mArticle;
    }

    @Override
    public long getItemId(final int i) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        return mInterfaceHelper.getView(parent);
    }
    private void refreshUi(int state) {
        mInterfaceHelper.setState(state);
        notifyDataSetChanged();
    }

    public void setArticle(ArticleMO article) {
        this.mArticle = article;
        refreshUi(STATE_LOADING_INFO_ARTICLE_BODY);
    }

    private String valueOrEmpty(final String value) {
        return TextUtils.isEmpty(value) ? "" : value;
    }

    private interface InterfaceListener {
        void onStateChanged(int state);
    }

    private class InterfaceHelper {
        private final Templates templates = new Templates();
        private View mView;
        private View mProgress;
        private CustomWebView mWebView;
        private int mState = STATE_LOADING_INFO_ARTICLE_BODY;
        private InterfaceListener mInterfaceListener;

        public InterfaceHelper(InterfaceListener changedState) {
            mInterfaceListener = changedState;
        }

        public void setState(int state) {
            mState = state;
        }

        public View getView(final ViewGroup parent) {
            mInterfaceListener.onStateChanged(mState);

            LayoutInflater inflater;
            View view;
            switch (mState) {
                case STATE_LOADING_INFO_ARTICLE_BODY: // loading info article body: progress.show
                    inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.slider_panel_info_layout, parent, false);
                    view.getLayoutParams().height = parent.getHeight();
                    view.getLayoutParams().width = parent.getWidth();

                    mProgress = view.findViewById(R.id.progress);
                    mProgress.setVisibility(View.VISIBLE);
                    break;
                case STATE_LOADED_INFO_ARTICLE_BODY: // loaded info body: progress.hide, webView.show
                    inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.slider_panel_info_layout, parent, false);
                    mState = STATE_UNDEFINED;
                    mProgress.setVisibility(View.INVISIBLE);

                    mWebView = (CustomWebView) view.findViewById(R.id.slider_panel_info_web_view);
                    mWebView.setWebViewClient(webViewClient);

                    showWebView(mWebView, loadInfoBodyHtml());

                    mView = view;
                    break;
                case STATE_CHANGING_KEYWORD: // changing keyword: progress.show, webView.show
                    mProgress.setVisibility(View.VISIBLE);
                    view = mView;
                    break;
                case STATE_CHANGED_KEYWORD: // changed keyword: progress.hide, webView.show
                    mProgress.setVisibility(View.INVISIBLE);
                    view = mView;
                    break;
                default:
                    view = mView;
                    break;
            }

            return view;
        }

        public void changeKeywordStatus(JSONObject json) {
            mWebView.executeJavaScript(String.format("changeKeywordStatus('%s');", json.toString()));
        }

        private void showAlertWithKeywordAndAction(final String keywordId, final String action) {
            if (!DeviceUtils.isInternetConnectionAvailable(mContext)) {
                errorManager.alertWithErrorCode(mActivity, NO_CONNECTION_AVAILABLE);
                mWebView.executeJavaScript(String.format("return revertBackKeywordStatus('%s');", keywordId));
            } else {
                mWebView.executeJavaScriptAndGetResult(String.format("return getKeywordTitleById('%s');", keywordId), new CustomWebView.JavaScriptExecutionCallback() {
                    @Override
                    public void onJavaScriptResult(final String keyword) {

                        if (action.equals(ArticleService.ACTION_UNSUBSCRIBE)) {
                            new AlertDialog.Builder(mContext)
                                    .setTitle(mActivity.getString(R.string.article_info_delete_keyword_title))
                                    .setMessage(String.format(mActivity.getString(R.string.article_info_delete_keyword_text), keyword))
                                    .setCancelable(true)
                                    .setNegativeButton(R.string.alert_button_no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            mWebView.executeJavaScript(String.format("return revertBackKeywordStatus('%s');", keywordId));
                                        }
                                    })
                                    .setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            aanHelper.trackActionUnsubscribeFromKeyword(keyword, mArticle);
                                            articleService.changeKeyword(keyword, ArticleService.ACTION_UNSUBSCRIBE);
                                            refreshUi(STATE_CHANGING_KEYWORD);
                                        }
                                    })
                                    .show();
                        } else {
                            aanHelper.trackActionSubscribeToKeyword(keyword, mArticle);
                            articleService.changeKeyword(keyword, ArticleService.ACTION_SUBSCRIBE);
                            refreshUi(STATE_CHANGING_KEYWORD);
                        }
                    }
                });
            }
        }

        private WebViewClient webViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http")) {
                    {
                        aanHelper.trackActionOpenWebViewerForArticleOnWOL(url, mArticle);
                    }
                    webController.openUrlInternal(url);
                } else if (webController.canOpenUrlExternal(url)) {
                    webController.openUrlExternal(url);
                } else if (url.startsWith(ArticleService.ACTION_SUBSCRIBE)) {
                    showAlertWithKeywordAndAction(url.substring((ArticleService.ACTION_SUBSCRIBE + "://").length()), ArticleService.ACTION_SUBSCRIBE);
                } else if (url.startsWith(ArticleService.ACTION_UNSUBSCRIBE)) {
                    showAlertWithKeywordAndAction(url.substring((ArticleService.ACTION_UNSUBSCRIBE + "://").length()), ArticleService.ACTION_UNSUBSCRIBE);
                } else
                    UIUtils.showLongToast(mContext, "Not implemented action: " + url);
                return true;
            }
        };

        private void showWebView(final CustomWebView webView, final String body) {
            //prepare html
            final String wolLinkTemplate = mArticle.getWolLinkTemplate();
            final String articleWolLink = wolLinkTemplate != null
                    ? wolLinkTemplate.replace("{doi}", mArticle.getDOI().getValue())
                    : null;

            final Template infoTemplate = templates.useAssetsTemplate(mContext, "article_info");
            infoTemplate
                    .putParam("article_wol_link", valueOrEmpty(articleWolLink))
                    .putParam("article_info_body", valueOrEmpty(body));

            webView.loadData(infoTemplate.proceed());

        }

        private String loadInfoBodyHtml() {
            if (!loadError) {
                final String infoBodyHtml = articleService.loadArticleInfoHtmlBody(mArticle);
                if (infoBodyHtml != null) {
                    return infoBodyHtml.replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "");
                }
            }

            final Template infoTemplate = templates.useAssetsTemplate(mContext, "article_info_body");

            final String articleDoi = mArticle.getDOI().getValue();
            final String wolLinkTemplate = mArticle.getWolLinkTemplate();
            final String articleWolLink = wolLinkTemplate != null ?
                    wolLinkTemplate.replace("{doi}", articleDoi) : null;
            final String articleKeywords = mArticle.getKeywords() != null ? mArticle.getKeywords().replace(", ", "<br />") : null;
            final String articleIssueNumber = mArticle.getSection() == null ? "" : mArticle.getSection().getIssue().getIssueNumber();
            final String articleVolumeNumber = mArticle.getSection() == null ? "" : mArticle.getSection().getIssue().getVolumeNumber();
            final String articlePageRange = mArticle.getPageRange();
            final String articleFunding = mArticle.getFundingInfo();
            final String articleFODate = mArticle.getFirstOnlineDate();
            final String articleMRDate = mArticle.getManuscriptReceivedDate();
            final String articleIndexTerms = mArticle.getIndexTerms();

            final String HIDE = "none";
            final String SHOW = "block";

            final String referenceDisplay = mArticle.getSection() != null ? SHOW : HIDE;
            final String fundingInfoDisplay = !TextUtils.isEmpty(articleFunding) ? SHOW : HIDE;
            final String publicationHistoryDisplay = !TextUtils.isEmpty(articleFODate) || !TextUtils.isEmpty(articleMRDate) ? SHOW : HIDE;
            final String keywordsListDisplay = !TextUtils.isEmpty(articleKeywords) ? SHOW : HIDE;
            final String foDateDisplay = !TextUtils.isEmpty(articleFODate) ? SHOW : HIDE;
            final String mrDateDisplay = !TextUtils.isEmpty(articleMRDate) ? SHOW : HIDE;
            final String pagesDisplay = !TextUtils.isEmpty(articlePageRange) ? SHOW : HIDE;
            final String issueNumberDisplay = !TextUtils.isEmpty(articleIssueNumber) ? SHOW : HIDE;
            final String historyDelimDisplay = SHOW.equals(foDateDisplay) && SHOW.equals(mrDateDisplay) ? SHOW : HIDE;
            final String indexTermsDisplay = !TextUtils.isEmpty(articleIndexTerms) ? SHOW : HIDE;

            // Article copyright
            String copyrightText = "";
            String copyrightDisplay = HIDE;
            if (!mArticle.isRestricted() && !TextUtils.isEmpty(mArticle.getAuthorSearchString())) {
                copyrightText = mArticle.getAuthorSearchString();
                copyrightDisplay = SHOW;
            }

            infoTemplate
                    .putParam("article_doi", valueOrEmpty(articleDoi))
                    .putParam("article_wol_link", valueOrEmpty(articleWolLink))
                    .putParam("article_keywords_list", valueOrEmpty(articleKeywords))
                    .putParam("issue_number_display", valueOrEmpty(issueNumberDisplay))
                    .putParam("article_issue_number", valueOrEmpty(articleIssueNumber))
                    .putParam("article_volume_number", valueOrEmpty(articleVolumeNumber))
                    .putParam("keywords_list_display", valueOrEmpty(keywordsListDisplay))
                    .putParam("article_reference_display", valueOrEmpty(referenceDisplay))
                    .putParam("funding_info_display", valueOrEmpty(fundingInfoDisplay))
                    .putParam("publication_history_display", valueOrEmpty(publicationHistoryDisplay))
                    .putParam("article_reference_pages", valueOrEmpty(articlePageRange))
                    .putParam("article_funding_info", valueOrEmpty(articleFunding))
                    .putParam("article_first_online_date", valueOrEmpty(articleFODate))
                    .putParam("article_manuscript_received_date", valueOrEmpty(articleMRDate))
                    .putParam("first_online_date_display", valueOrEmpty(foDateDisplay))
                    .putParam("manuscript_received_date_display", valueOrEmpty(mrDateDisplay))
                    .putParam("references_pages_display", valueOrEmpty(pagesDisplay))
                    .putParam("history_delim_display", valueOrEmpty(historyDelimDisplay))
                    .putParam("index_terms_display", valueOrEmpty(indexTermsDisplay))
                    .putParam("article_index_terms", valueOrEmpty(articleIndexTerms))
                    .putParam("copyright_text", valueOrEmpty(copyrightText))
                    .putParam("copyright_display", valueOrEmpty(copyrightDisplay));

            return infoTemplate.proceed();
        }
    }

    private class DataModelHelper {
        public void updateArticleInfoHtmlBody(ArticleMO article) {
            articleService.updateArticleInfoHtmlBody(article);
        }
    }
}
