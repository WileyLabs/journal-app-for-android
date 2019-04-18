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

import android.os.AsyncTask;
import android.text.TextUtils;

import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.search.HtmlSearch;
import com.wiley.android.journalApp.fragment.tabs.GlobalSearchFragment;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.log.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

/**
 * Created by taraskreknin on 16.07.14.
 */
public class GlobalSearchComponent extends ArticleRefComponent {

    public interface GlobalSearchComponentListener {
        void onSearchStarted();

        void onSearchCompleted(boolean hasMatch);

        void onProgress(int currentIndex, int size);

        void onSortStarted();

        void onSortCompleted();

        boolean hasDownloadedIssues();
    }

    final private static String TAG = GlobalSearchComponent.class.getSimpleName();

    private GlobalSearchComponentListener mListener;
    private List<ArticleMO> mArticles;
    private String mTerm;
    private int mMode = -1;
    private boolean mAsc;
    private AsyncTask<Void, Integer, List<ArticleMO>> mSearchTask;

    private String currentSectionHeader = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
    private static SimpleDateFormat mFirstOnLineDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final Comparator<ArticleMO> sMatchCountComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            return one.getUid() - another.getUid();
        }
    };

    private static final Comparator<ArticleMO> sMatchCountComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            return another.getUid() - one.getUid();
        }
    };

    private static final Comparator<ArticleMO> sTitleComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            final String anotherTitle = another.getTitle().replaceAll("<[^>]+>", "")
                    .replaceAll("\\\\s*\\n\\\\s*\\n\\\\s*", "\n");

            final String oneTitle = one.getTitle().replaceAll("<[^>]+>", "")
                    .replaceAll("\\\\s*\\n\\\\s*\\n\\\\s*", "\n");
            return anotherTitle.compareTo(oneTitle);
        }
    };

    private static final Comparator<ArticleMO> sTitleComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            final String anotherTitle = another.getTitle().replaceAll("<[^>]+>", "")
                    .replaceAll("\\\\s*\\n\\\\s*\\n\\\\s*", "\n");

            final String oneTitle = one.getTitle().replaceAll("<[^>]+>", "")
                    .replaceAll("\\\\s*\\n\\\\s*\\n\\\\s*", "\n");
            return oneTitle.compareTo(anotherTitle);
        }
    };

    private static final Comparator<ArticleMO> sPublishedDateComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            Date oneDate = one.getPublicationDate();
            Date anotherDate = another.getPublicationDate();
            long diff = anotherDate.getTime() - oneDate.getTime();
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

    private static final Comparator<ArticleMO> sPublishedDateComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            Date oneDate = one.getPublicationDate();
            Date anotherDate = another.getPublicationDate();
            long diff = oneDate.getTime() - anotherDate.getTime();
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

    private static final Comparator<ArticleMO> sFirstOnlineDateComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            if (one.getFirstOnlineDate().equals(another.getFirstOnlineDate())) {
                return 0;
            }
            if ("".equals(one.getFirstOnlineDate())) {
                return -1;
            }
            if ("".equals(another.getFirstOnlineDate())) {
                return 1;
            }

            Date oneDate;
            try {
                oneDate = mFirstOnLineDateFormat.parse(one.getFirstOnlineDate());
            } catch (ParseException ex) {
                Logger.s(TAG, ex);
                return -1;
            }

            Date anotherDate;
            try {
                anotherDate = mFirstOnLineDateFormat.parse(another.getFirstOnlineDate());
            } catch (ParseException ex) {
                Logger.s(TAG, ex);
                return 1;
            }

            long diff = anotherDate.getTime() - oneDate.getTime();
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

    private static final Comparator<ArticleMO> sFirstOnlineDateComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            if (one.getFirstOnlineDate().equals(another.getFirstOnlineDate())) {
                return 0;
            }
            if ("".equals(one.getFirstOnlineDate())) {
                return 1;
            }
            if ("".equals(another.getFirstOnlineDate())) {
                return -1;
            }

            Date oneDate;
            try {
                oneDate = mFirstOnLineDateFormat.parse(one.getFirstOnlineDate());
            } catch (ParseException ex) {
                Logger.s(TAG, ex);
                return 1;
            }

            Date anotherDate;
            try {
                anotherDate = mFirstOnLineDateFormat.parse(another.getFirstOnlineDate());
            } catch (ParseException ex) {
                Logger.s(TAG, ex);
                return -1;
            }

            long diff = oneDate.getTime() - anotherDate.getTime();
            return diff > 0 ? -1 : diff < 0 ? 1 : 0;
        }
    };

    private static final Comparator<ArticleMO> sIssueComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            String oneString = "";
            if (null != one.getSection() && null != one.getSection().getIssue()) {
                oneString = String.format("Issue %s/%s",
                        one.getSection().getIssue().getVolumeNumber(),
                        one.getSection().getIssue().getIssueNumber());
            } else if (null != one.getSpecialSections() && one.getSpecialSections().size() > 0) {
                oneString = "Special Sections";
            } else if (one.isEarlyView()) {
                oneString = "Early View";
            }

            String anotherString = "";
            if (null != another.getSection() && null != another.getSection().getIssue()) {
                anotherString = String.format("Issue %s/%s",
                        another.getSection().getIssue().getVolumeNumber(),
                        another.getSection().getIssue().getIssueNumber());
            } else if (null != another.getSpecialSections() && another.getSpecialSections().size() > 0) {
                anotherString = "Special Sections";
            } else if (another.isEarlyView()) {
                anotherString = "Early View";
            }

            return oneString.compareTo(anotherString);
        }
    };

    private static final Comparator<ArticleMO> sIssueComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            String oneString = "";
            if (null != one.getSection() && null != one.getSection().getIssue()) {
                oneString = String.format("Issue %s/%s",
                        one.getSection().getIssue().getVolumeNumber(),
                        one.getSection().getIssue().getIssueNumber());
            } else if (null != one.getSpecialSections() && one.getSpecialSections().size() > 0) {
                oneString = "Special Sections";
            } else if (one.isEarlyView()) {
                oneString = "Early View";
            }

            String anotherString = "";
            if (null != another.getSection() && null != another.getSection().getIssue()) {
                anotherString = String.format("Issue %s/%s",
                        another.getSection().getIssue().getVolumeNumber(),
                        another.getSection().getIssue().getIssueNumber());
            } else if (null != another.getSpecialSections() && another.getSpecialSections().size() > 0) {
                anotherString = "Special Sections";
            } else if (another.isEarlyView()) {
                anotherString = "Early View";
            }

            return anotherString.compareTo(oneString);
        }
    };

    private static final Comparator<ArticleMO> sVolumeComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            String oneString = "";
            if (null != one.getSection() && null != one.getSection().getIssue()) {
                oneString = String.format("Volume Number %s", one.getSection().getIssue().getVolumeNumber());
            } else if (null != one.getSpecialSections() && one.getSpecialSections().size() > 0) {
                oneString = "Special Sections";
            } else if (one.isEarlyView()) {
                oneString = "Early View";
            }

            String anotherString = "";
            if (null != another.getSection() && null != another.getSection().getIssue()) {
                anotherString = String.format("Volume Number %s", another.getSection().getIssue().getVolumeNumber());
            } else if (null != another.getSpecialSections() && another.getSpecialSections().size() > 0) {
                anotherString = "Special Sections";
            } else if (another.isEarlyView()) {
                anotherString = "Early View";
            }

            return oneString.compareTo(anotherString);
        }
    };

    private static final Comparator<ArticleMO> sVolumeComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            String oneString = "";
            if (null != one.getSection() && null != one.getSection().getIssue()) {
                oneString = String.format("Volume Number %s", one.getSection().getIssue().getVolumeNumber());
            } else if (null != one.getSpecialSections() && one.getSpecialSections().size() > 0) {
                oneString = "Special Sections";
            } else if (one.isEarlyView()) {
                oneString = "Early View";
            }

            String anotherString = "";
            if (null != another.getSection() && null != another.getSection().getIssue()) {
                anotherString = String.format("Volume Number %s", another.getSection().getIssue().getVolumeNumber());
            } else if (null != another.getSpecialSections() && another.getSpecialSections().size() > 0) {
                anotherString = "Special Sections";
            } else if (another.isEarlyView()) {
                anotherString = "Early View";
            }

            return anotherString.compareTo(oneString);
        }
    };

    private static final Comparator<ArticleMO> sSectionComparatorDescending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            String oneString = "";
            if (null != one.getSection() && null != one.getSection().getName()) {
                final String nameSection = one.getSection().getName();
                oneString = String.format("Section \"%s\"", nameSection.length() > 30 ? nameSection.substring(0, 30) : nameSection);
            } else if (null != one.getSpecialSections() && one.getSpecialSections().size() > 0) {
                oneString = "Special Sections";
            } else if (one.isEarlyView()) {
                oneString = "Early View";
            }

            String anotherString = "";
            if (null != another.getSection() && null != another.getSection().getName()) {
                final String nameSection = another.getSection().getName();
                anotherString = String.format("Section \"%s\"", nameSection.length() > 30 ? nameSection.substring(0, 30) : nameSection);
            } else if (null != another.getSpecialSections() && another.getSpecialSections().size() > 0) {
                anotherString = "Special Sections";
            } else if (another.isEarlyView()) {
                anotherString = "Early View";
            }

            return oneString.compareTo(anotherString);
        }
    };

    private static final Comparator<ArticleMO> sSectionComparatorAscending = new Comparator<ArticleMO>() {
        @Override
        public int compare(ArticleMO one, ArticleMO another) {
            String oneString = "";
            if (null != one.getSection() && null != one.getSection().getName()) {
                final String nameSection = one.getSection().getName();
                oneString = String.format("Section \"%s\"", nameSection.length() > 30 ? nameSection.substring(0, 30) : nameSection);
            } else if (null != one.getSpecialSections() && one.getSpecialSections().size() > 0) {
                oneString = "Special Sections";
            } else if (one.isEarlyView()) {
                oneString = "Early View";
            }

            String anotherString = "";
            if (null != another.getSection() && null != another.getSection().getName()) {
                final String nameSection = another.getSection().getName();
                anotherString = String.format("Section \"%s\"", nameSection.length() > 30 ? nameSection.substring(0, 30) : nameSection);
            } else if (null != another.getSpecialSections() && another.getSpecialSections().size() > 0) {
                anotherString = "Special Sections";
            } else if (another.isEarlyView()) {
                anotherString = "Early View";
            }

            return anotherString.compareTo(oneString);
        }
    };


    public GlobalSearchComponent(ArticleComponentHost host, CustomWebView webView) {
        super(host, webView);
        mListener = (GlobalSearchComponentListener) host;
    }

    public void init() {
        mTerm = "";
        mMode = -1;
        sortAndRender(new ArrayList<ArticleMO>());
    }

    private void sortAndRender(final List<ArticleMO> articles) {
        currentSectionHeader = null;

        mArticles = articles;

        // sorting
        if (articles.size() > 0) {
            mListener.onSortStarted();
            final Comparator<ArticleMO> comparator = getArticleComparator();

            if (comparator != null) {
                Collections.sort(mArticles, comparator);
            }
            mListener.onSortCompleted();
        }

        render(mArticles);
    }

    private Comparator<ArticleMO> getArticleComparator() {
        switch (mMode) {
            case GlobalSearchFragment.RELEVANCY_MODE:
                return mAsc ? sMatchCountComparatorAscending : sMatchCountComparatorDescending;
            case GlobalSearchFragment.TITLE_MODE:
                return mAsc ? sTitleComparatorAscending : sTitleComparatorDescending;
            case GlobalSearchFragment.DATE_PUBLISHED_MODE:
                return mAsc ? sPublishedDateComparatorAscending : sPublishedDateComparatorDescending;
            case GlobalSearchFragment.FIRST_ONLINE_MODE:
                return mAsc ? sFirstOnlineDateComparatorAscending : sFirstOnlineDateComparatorDescending;
            case GlobalSearchFragment.ISSUE_MODE:
                return mAsc ? sIssueComparatorAscending : sIssueComparatorDescending;
            case GlobalSearchFragment.VOLUME_MODE:
                return mAsc ? sVolumeComparatorAscending : sVolumeComparatorDescending;
            case GlobalSearchFragment.SECTION_MODE:
                return mAsc ? sSectionComparatorAscending : sSectionComparatorDescending;
            default:
                return null;
        }
    }

    @Override
    protected String htmlCodeForListHeading() {
        String result = "";

        String helpText = getListHeading();

        final Template template = templates.useAssetsTemplate(context, "list_heading_template" + (DeviceUtils.isTablet(context) ? "_iPad" : "_iPhone"));
        String html = template
                .putParam("show_heading_placeholder", TextUtils.isEmpty(helpText) ? "none" : "block")
                .putParam("heading_text_placeholder", helpText)
                .proceed();
        return result + html;
    }

    @Override
    protected String htmlCodeForTitleInArticle(final ArticleMO article) {
        HtmlSearch search = new HtmlSearch();

        // highlight title
        final String title = article.getTitle();
        HtmlSearch.Result bodyResult = search.find(title, mTerm);
        final String highlightedTitle;
        if (bodyResult.items.size() > 0) {
            highlightedTitle = bodyResult.highlightedHtml;
        } else {
            highlightedTitle = title;
        }
        return highlightedTitle;
    }

    @Override
    protected String htmlCodeForAbstractInArticle(final ArticleMO article) {
        // highlight abstract
        final String arAbstract = article.getSummary();

        HtmlSearch search = new HtmlSearch();
        HtmlSearch.Result bodyResult = search.find(arAbstract, mTerm);
        final String highlightedAbstract;
        if (bodyResult.items.size() > 0) {
            highlightedAbstract = bodyResult.highlightedHtml;
        } else {
            highlightedAbstract = arAbstract;
        }

        final String articleHTMLItemAbstractID = "abstract_placeholder_for_article_id_" + article.getDOI().getIdCompatibleValue();
        return format("<div class=\"abstract_class\" id=\"%s\">%s</div>", articleHTMLItemAbstractID, highlightedAbstract);
    }

    @Override
    protected String htmlCodeForAuthorsInArticle(final ArticleMO article) {
        // highlight authors
        final String authorList = article.getSimpleAuthorList();

        HtmlSearch search = new HtmlSearch();
        HtmlSearch.Result bodyResult = search.find(authorList, mTerm);
        String highlightedAuthors;
        if (bodyResult.items.size() > 0) {
            highlightedAuthors = bodyResult.highlightedHtml;
        } else {
            highlightedAuthors = authorList;
        }

        final Template articleItemAuthorsTemplate = templates.useAssetsTemplate(context, "ArticleListItemAuthorsTemplate");
        if (isEmpty(highlightedAuthors)) {
            if (isEmpty(article.getTitle())) {
                highlightedAuthors = format("<div class=\"authors_placeholder_for_article\" id=\"authors_placeholder_for_article_id_%s\"></div>", article.getDOI().getIdCompatibleValue());
            } else {
                highlightedAuthors = "";
            }
        } else {
            articleItemAuthorsTemplate
                    .putParam("_authors_", highlightedAuthors)
                    .putParam("_author_count_modifier_placeholder_", article.isOneAuthor() ? "" : "s");
            highlightedAuthors = articleItemAuthorsTemplate.proceed();
        }
        return highlightedAuthors;
    }

    private String getListHeading() {
        boolean hasDownloadedIssues = mListener.hasDownloadedIssues();

        String helpText = "";

        if (!articleService.hasArticles()) {
            helpText = "In order to use the search feature, there must be at least one issue present in the app. Please go to the issue screen and tap at least one issue to download content.";
        } else if (!hasDownloadedIssues) {
            helpText = "To see more results please go to the issues tab in the right hand menu and download at least one issue, you will be able to sort your results further once you have download content.";
        }

        String br = helpText.length() > 0 && mArticles.size() > 0 ? "<br/>" : "";
        String totalText = mArticles.size() > 0 ? "Articles found: " + mArticles.size() : "";

        return helpText + br + totalText;
    }

    @Override
    protected String htmlCodeForNoArticlesElement() {

        int countArticles = (null == mArticles) ? 0 : mArticles.size();
        final String term = (null == mTerm) ? "" : mTerm;

        final Template template = templates.useAssetsTemplate(context, "no_found_articles");

        return template
                .putParam("search_instruction_display_placeholder", 0 == countArticles && 0 == term.length() ? "" : "display:none")
                .putParam("no_search_results_display_placeholder", 0 == countArticles && 0 < term.length() ? "display:block" : "display:none")
                .putParam("search_term_placeholder", term)
                .proceed();
    }

    @Override
    protected String headingBeforeArticle(final ArticleMO article) {
        String result = null;
        String text = "";

        switch (mMode) {
            case GlobalSearchFragment.RELEVANCY_MODE:
                text = "Occurrences: " + article.getUid();
                break;
            case GlobalSearchFragment.TITLE_MODE:
                text = article.getTitle().substring(0, 1);
                break;
            case GlobalSearchFragment.DATE_PUBLISHED_MODE:
                text = article.getPublicationDate() != null ? dateFormat.format(article.getPublicationDate()) : "";
                break;
            case GlobalSearchFragment.FIRST_ONLINE_MODE:
                try {
                    final String firstOnlineDate = article.getFirstOnlineDate();
                    if (null != firstOnlineDate && !"".equals(firstOnlineDate)) {
                        text = dateFormat.format(mFirstOnLineDateFormat.parse(firstOnlineDate));
                    }
                } catch (ParseException ignored) {
                }
                break;
            case GlobalSearchFragment.ISSUE_MODE:
                if (null != article.getSection() && null != article.getSection().getIssue()) {
                    text = String.format("Issue %s/%s",
                            article.getSection().getIssue().getVolumeNumber(),
                            article.getSection().getIssue().getIssueNumber());
                } else if (null != article.getSpecialSections() && article.getSpecialSections().size() > 0) {
                    text = "Special Sections";
                } else if (article.isEarlyView()) {
                    text = "Early View";
                }
                break;
            case GlobalSearchFragment.VOLUME_MODE:
                if (null != article.getSection() && null != article.getSection().getIssue()) {
                    text = String.format("Volume Number %s", article.getSection().getIssue().getVolumeNumber());
                } else if (null != article.getSpecialSections() && article.getSpecialSections().size() > 0) {
                    text = "Special Sections";
                } else if (article.isEarlyView()) {
                    text = "Early View";
                }
                break;
            case GlobalSearchFragment.SECTION_MODE:
                if (null != article.getSection() && null != article.getSection().getName()) {
                    final String nameSection = article.getSection().getName();
                    text = String.format("Section \"%s\"", nameSection.length() > 30 ? nameSection.substring(0, 30) : nameSection);
                } else if (null != article.getSpecialSections() && article.getSpecialSections().size() > 0) {
                    text = "Special Sections";
                } else if (article.isEarlyView()) {
                    text = "Early View";
                }
                break;
        }

        if (null == currentSectionHeader || !currentSectionHeader.equals(text)) {
            currentSectionHeader = text;
            result = String.format("<div class=\"section_heading_class\"><p class=\"section_heading_class\">%s</p></div>", text);
        }

        return result;
    }

    protected boolean isDivideByDate() {
        return true;
    }

    @Override
    protected void openArticles(List<DOI> doiList, DOI doiForOpen) {
        {
            aanHelper.trackActionOpenArticleFromGlobalSearchResult(mTerm, sortMethod());
        }
        final String title = "Found Articles";
        ((MainActivity) componentHost.getActivity()).openArticles(doiList, doiForOpen, title, false, mTerm);
    }

    private String sortMethod() {
        final String mode;
        switch (mMode) {
            case GlobalSearchFragment.RELEVANCY_MODE: mode = "Relevancy"; break;
            case GlobalSearchFragment.TITLE_MODE: mode = "Title"; break;
            case GlobalSearchFragment.ISSUE_MODE: mode = "Issue"; break;
            case GlobalSearchFragment.VOLUME_MODE: mode = "Volume"; break;
            case GlobalSearchFragment.SECTION_MODE: mode = "Section"; break;
            case GlobalSearchFragment.DATE_PUBLISHED_MODE: mode = "Date Published"; break;
            case GlobalSearchFragment.FIRST_ONLINE_MODE: mode = "First Online"; break;
            default: mode = "Undefined"; break;
        }

        return String.format("%s %s", mode, mAsc ? "Ascending" : "Descending");
    }

    public void onSort(int mode, boolean asc) {
        mMode = mode;
        mAsc = asc;

        aanHelper.trackActionSortGlobalSearchResults(mTerm, sortMethod());
        sortAndRender(mArticles);
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

    private AsyncTask<Void, Integer, List<ArticleMO>> createCheckAction() {
        return new AsyncTask<Void, Integer, List<ArticleMO>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mListener.onSearchStarted();
            }

            @Override
            protected List<ArticleMO> doInBackground(Void... params) {
                int index = 0;
                List<ArticleMO> allArticles = articleService.getAllArticleDOIs();
                List<ArticleMO> foundArticles = new ArrayList<>();
                for (ArticleMO articleDOI : allArticles) {
                    if (isCancelled()) {
                        return null;
                    }

                    final ArticleMO article = articleService.getArticleQuietly(articleDOI.getDOI());
                    if (null == article) {
                        continue;
                    }
                    int matchCount = matchTerm(article, mTerm);
                    if (matchCount > 0) {
                        // todo other 'count' property
                        article.setUid(matchCount);
                        foundArticles.add(article);
                    }

                    publishProgress(index++, allArticles.size());
                }
                return foundArticles;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                mListener.onProgress(values[0], values[1]);
            }

            @Override
            protected void onPostExecute(List<ArticleMO> data) {
                super.onPostExecute(data);

                mListener.onSearchCompleted(!data.isEmpty());
                sortAndRender(data);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }

        };
    }

    private int matchTerm(final ArticleMO article, final String term) {
        HtmlSearch search = new HtmlSearch();

        boolean isArticleValid = article.isLocal() || (!article.isRestricted() && !article.isExpired());
        int matchCount = 0;

        final String body = isArticleValid ? articleService.getFullHtmlBody(article) : null;
        if (!TextUtils.isEmpty(body)) {
            HtmlSearch.Result bodyResult = search.find(body, term);
            matchCount += bodyResult.items.size();
        }

        final String arAbstract = isArticleValid ? article.getFullTextAbstract() : article.getSummary();
        if (!TextUtils.isEmpty(arAbstract)) {
            HtmlSearch.Result abstractResult = search.find(arAbstract, term);
            matchCount += abstractResult.items.size();
        }

        final String title = article.getTitle();
        if (!TextUtils.isEmpty(title)) {
            HtmlSearch.Result titleResult = search.find(title, term);
            matchCount += titleResult.items.size();
        }

        final String authorList = article.getSimpleAuthorList();
        if (!TextUtils.isEmpty(authorList)) {
            HtmlSearch.Result authorListResult = search.find(authorList, term);
            matchCount += authorListResult.items.size();
        }

        return matchCount;

    }
}
