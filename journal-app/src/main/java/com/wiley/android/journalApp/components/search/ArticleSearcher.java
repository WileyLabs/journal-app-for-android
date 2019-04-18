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
package com.wiley.android.journalApp.components.search;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.ArticleComponent;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taraskreknin
 * on 23.05.14.
 */
public class ArticleSearcher extends JournalFragment {
    private static final String HIGHLIGHTED_ITEM_NAME_PREFIX = "hitem_";

    @Inject
    private ArticleService mArticleService;
    @Inject
    private AANHelper aanHelper;

    private CustomWebView mWebView;
    private View mRootView;
    private EditText mSearchField;
    private TextView mStatusText;

    private float mShowFactor = 1.0f;

    private ResultsFormatter mFormatter;

    private int mActiveOccurrence = 0;
    private int mHighlightedOccurrence = 0;
    private int mTotalOccurrences = 0;

    private SearchResultsAdapter mAdapter = new SearchResultsAdapter();
    private List<String> mSearchResults;
    private boolean mIsPhone;

    private LayoutInflater mInflater;
    private int highlightedPosition = -1;

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String input = editable.toString();
            onSearch(input);
        }
    };

    private final TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (mSearchResults != null) {
                    findNext();
                }
                return true;
            }
            return false;
        }
    };

    private void onSearch(String input) {
        if (mWebView == null) {
            return;
        }
        if (input.length() >= 2) {
            findAll(input);
            GANHelper.trackEvent(GANHelper.EVENT_SEARCH_ARTICLE,
                    GANHelper.ACTION_SEARCH_FOR_TERM,
                    input,
                    0L);
        } else {
            resetMatches();
            unhighlightMathes();
        }
    }

    private void unhighlightMathes() {
        if (getArticleComponent() != null) {
            getArticleComponent().reloadArticleWithCustomData(null, null, null, null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        return inflater.inflate(R.layout.article_searcher, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRootView = findView(R.id.root);
        mSearchField = findView(R.id.searcher_edit_text);
        View findNext = findView(R.id.searcher_find_next);
        View findPrev = findView(R.id.searcher_find_prev);
        View hide = findView(R.id.searcher_hide);
        mStatusText = findView(R.id.searcher_status);

        mIsPhone = DeviceUtils.isPhone(getActivity());
        final int visibility = mIsPhone ? View.VISIBLE : View.GONE;
        findNext.setVisibility(visibility);
        findNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findNext();
            }
        });
        findPrev.setVisibility(visibility);
        findPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findPrev();
            }
        });
        hide.setVisibility(visibility);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSearcherAnimated(true);
            }
        });
        mSearchField.addTextChangedListener(mTextWatcher);
        mSearchField.setOnEditorActionListener(onEditorActionListener);
        mFormatter = mIsPhone ? new PhoneFormatter() : new TabletFormatter();
        mStatusText.setVisibility(visibility);
        mStatusText.setBackgroundColor(theme.getMainColor());

        // add clear button to searcher
        final Drawable drawable = getResources().getDrawable(R.drawable.seacher_text_clear);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mSearchField.setCompoundDrawables(null, null, drawable, null);
        ImageView crossImageView = new ImageView(getJournalActivity());
        crossImageView.setImageDrawable(drawable);
        mSearchField.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSearchField.getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > mSearchField.getWidth() - mSearchField.getPaddingRight() - drawable.getIntrinsicWidth()) {
                    mSearchField.setText("");
                    mSearchField.setCompoundDrawables(null, null, null, null);
                }
                return false;
            }
        });
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchField.setCompoundDrawables(null, null, mSearchField.getText().toString().equals("") ? null : drawable, null);
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

        });

    }

    public boolean hasFocus() {
        return mSearchField.hasFocus();
    }

    public void showKeyboard() {
        UIUtils.showSoftInput(mSearchField);
    }

    public void setText(String text) {
        mSearchField.setText(text);
    }

    private void findPrev() {
        decActiveOcc();
        scrollToHighlightedItem(mActiveOccurrence, true);
    }

    private void findNext() {
        incActiveOcc();
        scrollToHighlightedItem(mActiveOccurrence, true);
    }

    private void incActiveOcc() {
        mActiveOccurrence = mActiveOccurrence == mTotalOccurrences - 1 ? 0 : mActiveOccurrence + 1;
        updateStatus(mActiveOccurrence, mTotalOccurrences);
    }

    private void decActiveOcc() {
        mActiveOccurrence = mActiveOccurrence == 0 ? mTotalOccurrences - 1 : mActiveOccurrence - 1;
        updateStatus(mActiveOccurrence, mTotalOccurrences);
    }

    protected List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    protected void onShowSearcher() {
        for (Listener listener : listeners)
            listener.onShowSearcher();
    }

    protected void onHideListener() {
        for (Listener listener : listeners)
            listener.onHideSearcher();
    }

    public enum ContentState {
        None,
        Closed,
        Open
    }

    protected ContentState contentState = ContentState.None;
    protected boolean scrolling = false;

    public void showSearcher(CustomWebView webView) {
        if (contentState == ContentState.Open && mWebView == webView) {
            return;
        }

        highlightedPosition = -1;

        stopScrolling();
        boolean webViewChanged = contentState == ContentState.Open && mWebView != webView;
        contentState = ContentState.Open;
        setArticleContent(webView);
        mRootView.setVisibility(View.VISIBLE);
        mShowFactor = 1.0f;
        updateShowFactor();
        if (webViewChanged) {
            doSearch();
        } else {
            mSearchField.requestFocus();
            UIUtils.showSoftInput(mSearchField);
        }
        onShowSearcher();
    }

    public void doSearch(CustomWebView webView) {
        setArticleContent(webView);
        doSearch();
    }
    private void doSearch() {
        onSearch(mSearchField.getText().toString());
    }

    public void hideSearcher(boolean clearResults) {
        if (contentState == ContentState.Closed) {
            return;
        }

        stopScrolling();
        contentState = ContentState.Closed;
        if (clearResults) {
            mSearchField.setText("");
            mActiveOccurrence = mTotalOccurrences = 0;
            unhighlightMathes();
        }
        mShowFactor = 0.0f;
        UIUtils.hideSoftInput(mSearchField);
        if (mRootView != null) {
            mRootView.setVisibility(View.GONE);
        }
        onHideListener();
    }

    public void showSearcherAnimated(CustomWebView webView) {
        if (contentState == ContentState.Open) {
            return;
        }

        stopScrolling();
        contentState = ContentState.Open;
        setArticleContent(webView);
        mRootView.setVisibility(View.VISIBLE);
        doScroll(1.0f, 0.0f);
        mSearchField.requestFocus();
        UIUtils.showSoftInput(mSearchField);
        onShowSearcher();
    }

    public void hideSearcherAnimated(boolean clearResults) {
        if (contentState == ContentState.Closed) {
            return;
        }

        stopScrolling();
        contentState = ContentState.Closed;
        if (clearResults) {
            mSearchField.setText("");
            mActiveOccurrence = mTotalOccurrences = 0;
            unhighlightMathes();
        }
        UIUtils.hideSoftInput(mSearchField);
        doScroll(0.0f, 0.0f);
        onHideListener();
    }

    public void hideSoftInput() {
        UIUtils.hideSoftInput(mSearchField);
    }

    public boolean isShowed() {
        return contentState == ContentState.Open;
    }

    protected void stopScrolling() {
        if (mRootView != null) {
            mRootView.removeCallbacks(scrollRunnable);
        }
        scrolling = false;
    }

    protected Scroller centerMenuScroller = null;

    private static final Interpolator interpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    protected Runnable scrollRunnable = new Runnable() {

        @Override
        public void run() {
            if (centerMenuScroller != null) {
                if (centerMenuScroller.computeScrollOffset()) {
                    int percent = centerMenuScroller.getCurrX();
                    float newShowFactor = (float) percent / 100.0f;
                    setShowFactor(newShowFactor);
                }
                if (centerMenuScroller.isFinished()) {
                    centerMenuScroller = null;
                }
            }

            boolean needContinue = centerMenuScroller != null;
            if (needContinue) {
                ViewCompat.postOnAnimation(mRootView, this);
            } else {
                finishScrolling();
            }
        }
    };

    protected void finishScrolling() {
        stopScrolling();
        int contentVisibility = this.contentState == ContentState.Open ? View.VISIBLE : View.GONE;
        mRootView.setVisibility(contentVisibility);
    }

    protected void doScroll(float targetShowFactor, float velocity) {
        scrolling = true;
        if (this.mShowFactor != targetShowFactor) {
            centerMenuScroller = new Scroller(getActivity(), interpolator);
            int from = (int) (this.mShowFactor * 100.0f);
            int to = (int) (targetShowFactor * 100.0f);
            int dx = to - from;
            int duration = calculateScrollDuration(dx, 100, velocity);
            centerMenuScroller.startScroll(from, 0, dx, 0, duration);
        }
        if (this.centerMenuScroller != null) {
            ViewCompat.postOnAnimation(mRootView, scrollRunnable);
        } else {
            finishScrolling();
        }
    }

    private static final int scrollDurationMax = 600;

    private int calculateScrollDuration(int dx, int width, float velocity) {
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth *
                distanceInfluenceForSnapDuration(distanceRatio);

        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageDelta = (float) Math.abs(dx) / width;
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, scrollDurationMax);
        return duration;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f;
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    public void setShowFactor(float showFactor) {
        if (mShowFactor != showFactor) {
            mShowFactor = showFactor;
            updateShowFactor();
        }
    }

    protected void updateShowFactor() {
        int height = mRootView.getMeasuredHeight();
        float offsetFactor = 1.0f - mShowFactor;
        int defaultOffset = scrolling ? 1000 : 0;
        int offset = height == 0 ? defaultOffset : (int) (height * offsetFactor);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mRootView.getLayoutParams();
        layoutParams.topMargin = -offset;
        mRootView.setLayoutParams(layoutParams);
    }

    public SearchResultsAdapter getAdapter() {
        return mAdapter;
    }

    public void setArticleContent(CustomWebView webView) {
        mWebView = webView;
    }

    private void updateStatus(int activeMatch, int matchesCount) {
        if (!mIsPhone && matchesCount == 0) {
            mStatusText.setVisibility(View.GONE);
            return;
        }
        mStatusText.setVisibility(View.VISIBLE);
        mStatusText.setText(mFormatter.format(activeMatch + 1, matchesCount));
    }

    private void resetMatches() {
        mActiveOccurrence = 0;
        mHighlightedOccurrence = 0;
        if (!mIsPhone) {
            mStatusText.setVisibility(View.GONE);
        } else {
            mStatusText.setText(R.string.type_two_or_more);
        }
        mSearchResults = null;
        mAdapter.notifyDataSetChanged();
    }

    private boolean hasResults() {
        return mSearchResults != null && mSearchResults.size() != 0;
    }

    private void findAll(final String input) {
        HtmlSearch search = new HtmlSearch();
        search.addAnchor = false;
        search.highlightOpenTag = "<span class=\"highlighted_text\" id=\"" + HIGHLIGHTED_ITEM_NAME_PREFIX + "@index@\">";
        search.highlightCloseTag = "</span>";

        //TODO: do it asynchronously

        final ArticleMO article = getArticleComponent().getLoadedArticle();
        if (article == null) {
            return;
        }
        boolean isArticleValid = article.isLocal() || (!article.isRestricted() && !article.isExpired());
        final String body = isArticleValid ? mArticleService.getFullHtmlBody(article) : null;
        final String arAbstract = isArticleValid ? article.getFullTextAbstract() : article.getSummary();
        final String title = article.getTitle();
        final String authors = article.getSimpleAuthorList();

        String hBody = "", hAbstract = "", hTitle = "", hAuthors = "";

        int highlightIndex = 0;

        mSearchResults = new ArrayList<>();

        if (!TextUtils.isEmpty(title)) {
            HtmlSearch.Result titleResult = search.find(title, input);
            highlightIndex = titleResult.items.size();
            mSearchResults.addAll(titleResult.items);
            hTitle = titleResult.highlightedHtml;
        }

        if (!TextUtils.isEmpty(authors)) {
            search.highlightingStartIndex += highlightIndex;
            HtmlSearch.Result authorsResult = search.find(authors, input);
            highlightIndex = authorsResult.items.size();
            mSearchResults.addAll(authorsResult.items);
            hAuthors = authorsResult.highlightedHtml;
        }

        if (!TextUtils.isEmpty(body)) {
            search.highlightingStartIndex += highlightIndex;
            HtmlSearch.Result bodyResult = search.find(body, input);
            highlightIndex = bodyResult.items.size();
            mSearchResults.addAll(bodyResult.items);
            hBody = bodyResult.highlightedHtml;
        }

        if (!TextUtils.isEmpty(arAbstract)) {
            search.highlightingStartIndex += highlightIndex;
            HtmlSearch.Result abstractResult = search.find(arAbstract, input);
            mSearchResults.addAll(abstractResult.items);
            hAbstract = abstractResult.highlightedHtml;
        }

        mTotalOccurrences = mSearchResults.size();
        mActiveOccurrence = 0;
        getArticleComponent().reloadArticleWithCustomData(hBody, hAbstract, hTitle, hAuthors);

        if (mTotalOccurrences > 0) {
            if (mIsPhone) {
                scrollToHighlightedItem(mActiveOccurrence, true);
            }
        }

        mAdapter.notifyDataSetChanged();
        updateStatus(mActiveOccurrence, mTotalOccurrences);
    }

    private void scrollToHighlightedItem(int toId, boolean highlight) {
        String toName = HIGHLIGHTED_ITEM_NAME_PREFIX + toId;
        if (highlight) {
            mWebView.unhighlightElement(HIGHLIGHTED_ITEM_NAME_PREFIX + mHighlightedOccurrence);
            mWebView.highlightElement(toName);
            mHighlightedOccurrence = toId;
        }
        mWebView.scrollToElement(toName);
        {
            aanHelper.trackActionJumpToSearchTerm(mSearchField.getText().toString(), getArticleComponent().getLoadedArticle());
        }
    }

    private ArticleComponent getArticleComponent() {
        return ((ArticleViewFragment) getParentFragment()).getCurrentArticleComponent();
    }

    private interface ResultsFormatter {
        String format(int active, int total);
    }

    private class TabletFormatter implements ResultsFormatter {
        @Override
        public String format(int active, int total) {
            String formatted;
            if (total == 0) {
                formatted = getString(R.string.no_results);
            } else {
                formatted = String.format(getString(R.string.search_result_format), total);
            }
            return formatted;
        }
    }

    private class PhoneFormatter implements ResultsFormatter {
        @Override
        public String format(int active, int total) {
            String formatted;
            if (total == 0) {
                formatted = getString(R.string.no_results);
            } else {
                formatted = String.format(getString(R.string.search_result_format_phone), active, total);
            }
            return formatted;
        }
    }

    private class SearchResultsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (!hasResults()) {
                return 1;
            }
            return mSearchResults.size();
        }

        @Override
        public Object getItem(int i) {
            return mSearchResults.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View reusable, ViewGroup parent) {
            View view = reusable;
            ((ListView) parent).setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            boolean hasResults = hasResults();
            if (view == null) {
                view = mInflater.inflate(R.layout.search_result_item, parent, false);
            }
            TextView resultText = (TextView) view;

            if (hasResults) {
                setupResultItem(resultText, position, parent);
            } else {
                setupNoResultsItem(resultText);
            }

            return resultText;
        }

        private void setupNoResultsItem(TextView textView) {
            String text;
            if (mSearchResults == null) {
                text = getString(R.string.type_two_or_more);
            } else {
                text = getString(R.string.no_results);
            }
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(getResources().getColor(R.color.search_no_results_text));
            textView.setText(Html.fromHtml(text));
            textView.setOnClickListener(null);
        }

        private void setupResultItem(TextView textView, final int position, final ViewGroup parent) {
            String text = mSearchResults.get(position);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setTextColor(Color.BLACK);
            textView.setText(Html.fromHtml(text));
            if (position == highlightedPosition) {
                ((ListView) parent).setItemChecked(position, true);
            }

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((ListView) parent).setItemChecked(position, true);

                    highlightedPosition = position;
                    scrollToHighlightedItem(position, true);
                    UIUtils.hideSoftInput(mSearchField);
                }
            });
        }
    }

    public interface Listener {
        void onShowSearcher();

        void onHideSearcher();
    }

    public static class BaseListener implements Listener {
        @Override
        public void onShowSearcher() {
        }

        @Override
        public void onHideSearcher() {
        }
    }
}
