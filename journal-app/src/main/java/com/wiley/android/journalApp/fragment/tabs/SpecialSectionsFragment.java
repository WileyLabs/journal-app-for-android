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
package com.wiley.android.journalApp.fragment.tabs;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.utils.Dimmable;
import com.wiley.android.journalApp.utils.IdUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTIONS_ERROR;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTIONS_NOT_MODIFIED;
import static com.wiley.wol.client.android.notification.EventList.SPECIAL_SECTIONS_UPDATED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;

/**
 * Created by taraskreknin on 07.07.14.
 */
public class SpecialSectionsFragment extends BaseTabFragment implements Dimmable, AdapterView.OnItemClickListener {

    private static final int MIN_SECTIONS_TO_SHOW_LETTERS_SIDE_BAR = 10;

    @Inject
    private LayoutInflater mInflater;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private ErrorManager errorManager;
    private final List<ListItem> mAllSpecialSections = new ArrayList<>();
    private final List<ListItem> mCurrentSpecialSections = new ArrayList<>();
    private final Map<Character, Integer> mLetterSections = new ArrayMap<>();
    private final SpecialSectionsAdapter mAdapter = new SpecialSectionsAdapter();
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onFilterSections(s.toString());
        }
    };
    private final TextView.OnEditorActionListener mEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIUtils.hideSoftInput(getActivity());
                return true;
            }
            return false;
        }
    };

    private ListView mSectionsListView;
    private ViewGroup mLettersBarView;
    private EditText mSearchView;
    private View mClearSearchView;
    private View mDimView;
    private View mNoResultsView;

    private GestureDetector mGestureDetector;
    private final GestureDetector.OnGestureListener mLettersBarGestureListener = new LettersSideBarGestureListener();

    private boolean mJournalHasDarkBackground;

    private final NotificationProcessor successProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
            updateSectionsAsync();
        }
    };

    private final NotificationProcessor notModifiedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map<String, Object> params) {
        }
    };

    private final NotificationProcessor errorProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(final Map params) {
            errorManager.alertWithException(SpecialSectionsFragment.this.getActivity(), (Throwable) params.get(ERROR));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mJournalHasDarkBackground = theme.isJournalHasDarkBackground();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_special_sections, container, false);
    }

    @Override
    public void onSoftKeyboardVisibleChanged(boolean visible) {
        super.onSoftKeyboardVisibleChanged(visible);
        if (visible) {
            mSearchView.setCursorVisible(true);
        } else {
            mSearchView.setCursorVisible(false);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationCenter.subscribeToNotification(SPECIAL_SECTIONS_UPDATED.getEventName(), successProcessor);
        mSectionsListView = findView(R.id.special_sections_list);
        mSectionsListView.setAdapter(mAdapter);
        mSectionsListView.setOnItemClickListener(this);

        mGestureDetector = new GestureDetector(getActivity(), mLettersBarGestureListener);

        mLettersBarView = findView(R.id.special_sections_letters_parent);
        mLettersBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
        fillLetters();

        mSearchView = findView(R.id.special_sections_search);
        mSearchView.setOnEditorActionListener(mEditorActionListener);
        mSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(getSearchText())) {
                    dim(70);
                }
            }
        });
        mSearchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && TextUtils.isEmpty(getSearchText())) {
                    dim(70);
                }
            }
        });

        mDimView = findView(R.id.special_sections_dimmer);
        mDimView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtils.hideSoftInput(getActivity());
                undim();
            }
        });
        undim();

        mClearSearchView = findView(R.id.special_sections_clear_search);
        mClearSearchView.setVisibility(View.GONE);
        mClearSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.setText("");
                mClearSearchView.setVisibility(View.GONE);
                if (!mSearchView.hasFocus()) {
                    mSearchView.requestFocusFromTouch();
                }
            }
        });

        mNoResultsView = findView(R.id.special_sections_no_results);
        mNoResultsView.setVisibility(View.GONE);

        updateSectionsAsync();
    }

    private void fillLetters() {
        mLettersBarView.removeAllViews();
        TextView letter = (TextView) mInflater.inflate(R.layout.special_section_side_letter, mLettersBarView, false);
        for (char i = 'A'; i <= 'Z'; i++) {
            letter.setText(String.format("%c", i));
            mLettersBarView.addView(letter);
            letter = (TextView) mInflater.inflate(R.layout.special_section_side_letter, mLettersBarView, false);
        }
        letter.setText(String.format("%c", '#'));
        mLettersBarView.addView(letter);
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(SPECIAL_SECTIONS_NOT_MODIFIED.getEventName(), notModifiedProcessor);
        notificationCenter.subscribeToNotification(SPECIAL_SECTIONS_ERROR.getEventName(), errorProcessor);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(notModifiedProcessor);
        notificationCenter.unSubscribeFromNotification(errorProcessor);
    }

    @Override
    public void onShow() {
        if (mSearchView != null) {
            super.onShow();
            mSearchView.addTextChangedListener(mTextWatcher);
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        mSearchView.removeTextChangedListener(mTextWatcher);
        mSearchView.clearFocus();
        UIUtils.hideSoftInput(getActivity());
        undim();
    }

    private void scrollToSectionHeaderItem(char letter) {
        if (mLetterSections.containsKey(letter)) {
            int positionToScroll = mLetterSections.get(letter);
            int currentPosition = mSectionsListView.getFirstVisiblePosition();
            if (positionToScroll != currentPosition) {
                mSectionsListView.setSelection(positionToScroll);
            }
        }
    }

    private String getSearchText() {
        return mSearchView.getText().toString();
    }

    private void onAllSectionsUpdated(List<SpecialSectionMO> newSections) {
        mAdapter.notifyDataSetInvalidated();
        mAllSpecialSections.clear();
        mLetterSections.clear();
        mCurrentSpecialSections.clear();

        Collections.sort(newSections, new Comparator<SpecialSectionMO>() {
            @Override
            public int compare(SpecialSectionMO lhs, SpecialSectionMO rhs) {
                char lhsGroup = getSectionGroup(lhs);
                if (lhsGroup == '#') {
                    lhsGroup = 255;
                }
                char rhsGroup = getSectionGroup(rhs);
                if (rhsGroup == '#') {
                    rhsGroup = 255;
                }

                int result = lhsGroup - rhsGroup;
                if (result == 0) {
                    result = lhs.getTitle().compareTo(rhs.getTitle());
                }

                return result;
            }
        });

        char currentGroup = 0;
        for (SpecialSectionMO section : newSections) {
            char sectionGroup = getSectionGroup(section);
            if (currentGroup != sectionGroup) {
                currentGroup = sectionGroup;
                mAllSpecialSections.add(new SectionHeaderItem(String.format("%c", sectionGroup)));
                mLetterSections.put(sectionGroup, mAllSpecialSections.size() - 1);
            }
            mAllSpecialSections.add(new SectionItem(section));
        }

        if (getSearchText().length() > 0) {
            onFilterSections(getSearchText());
            return;
        }

        mCurrentSpecialSections.addAll(mAllSpecialSections);
        mAdapter.notifyDataSetChanged();
        updateLettersBarVisibility();
    }

    protected char getSectionGroup(SpecialSectionMO section) {
        char group = section.getFirstLetter();
        if (group >= 'A' && group <= 'Z') {
            return group;
        } else {
            return '#';
        }
    }

    private void showAllSections() {
        mNoResultsView.setVisibility(View.GONE);
        mAdapter.notifyDataSetInvalidated();
        mCurrentSpecialSections.clear();
        mCurrentSpecialSections.addAll(mAllSpecialSections);
        mAdapter.notifyDataSetChanged();
        updateLettersBarVisibility();
    }

    private void updateLettersBarVisibility() {
        int visibility = mLetterSections.size() < MIN_SECTIONS_TO_SHOW_LETTERS_SIDE_BAR ? View.GONE : View.VISIBLE;
        mLettersBarView.setVisibility(visibility);
    }

    private void onFilterSections(String searchText) {
        if (TextUtils.isEmpty(searchText)) {
            mClearSearchView.setVisibility(View.GONE);
            showAllSections();
            dim(70);
            return;
        }
        mAdapter.filter(searchText);
        mLettersBarView.setVisibility(View.GONE);
        mClearSearchView.setVisibility(View.VISIBLE);
        undim();
    }

    @Override
    public void dim(int alphaLevel) {
        mDimView.setAlpha(alphaLevel / 255.0f);
        if (!isDimmed()) {
            mDimView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void undim() {
        mDimView.setVisibility(View.GONE);
    }

    private boolean isDimmed() {
        return mDimView.getVisibility() == View.VISIBLE && mDimView.getAlpha() > 0.0f;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        UIUtils.hideSoftInput(getActivity());
        final String sectionId = ((SectionItem) mCurrentSpecialSections.get(position)).getSectionId();
//        startActivity(SpecialSectionArticlesActivity.getStartingIntent(getActivity(), sectionId));
        ((MainActivity) getActivity()).openSpecialSectionsArticle(sectionId);
    }

    @Override
    public boolean onBackPressed() {
        if (!TextUtils.isEmpty(getSearchText())) {
            mSearchView.setText("");
            return true;
        }
        if (isDimmed()) {
            undim();
            return true;
        }
        return false;
    }

    @Override
    protected int getTabId() {
        return R.id.special_sections_tab;
    }

    private void updateSectionsAsync() {
        Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        final LoaderManager lm = activity.getLoaderManager();
        if (lm.getLoader(LoaderId_getSpecialSections) != null) {
            lm.restartLoader(LoaderId_getSpecialSections, null, loaderCallbacks);
        } else {
            lm.initLoader(LoaderId_getSpecialSections, null, loaderCallbacks);
        }
    }

    private static final int LoaderId_getSpecialSections = IdUtils.generateIntId();

    private final LoaderManager.LoaderCallbacks<List<SpecialSectionMO>> loaderCallbacks = new LoaderManager.LoaderCallbacks<List<SpecialSectionMO>>() {
        @Override
        public Loader<List<SpecialSectionMO>> onCreateLoader(int id, Bundle args) {
            return new AsyncTaskLoader<List<SpecialSectionMO>>(getActivity().getApplicationContext()) {
                @Override
                public List<SpecialSectionMO> loadInBackground() {
                    return specialSectionService.getSpecialSections();
                }

                @Override
                protected void onStartLoading() {
                    super.onStartLoading();
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<SpecialSectionMO>> loader, List<SpecialSectionMO> data) {
            onAllSectionsUpdated(data);

            final Activity activity = getActivity();
            if (activity != null) {
                final LoaderManager loaderManager = activity.getLoaderManager();
                if (loaderManager != null) {
                    loaderManager.destroyLoader(LoaderId_getSpecialSections);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<List<SpecialSectionMO>> loader) {
        }
    };

    private interface ListItem {
        int ITEM = 0;
        int HEADER = 1;

        int getType();

        View getView(View convertView, ViewGroup parentView);
    }

    private class SectionItem implements ListItem {

        private final SpecialSectionMO mSection;

        public SectionItem(SpecialSectionMO section) {
            mSection = section;
        }

        public String getTitle() {
            return mSection.getTitle();
        }

        @Override
        public int getType() {
            return ITEM;
        }

        @Override
        public View getView(View convertView, ViewGroup parentView) {
            TextView text = (TextView) convertView;
            if (text == null) {
                text = (TextView) mInflater.inflate(R.layout.special_sections_list_item, parentView, false);
            }
            text.setText(Html.fromHtml(mSection.getTitle()));
            return text;
        }

        public String getSectionId() {
            return mSection.getUid();
        }
    }

    private class SectionHeaderItem implements ListItem {

        private final String mTitle;

        public SectionHeaderItem(String title) {
            mTitle = title.toUpperCase();
        }

        @Override
        public int getType() {
            return HEADER;
        }

        @Override
        public View getView(View convertView, ViewGroup parentView) {
            TextView text = (TextView) convertView;
            if (text == null) {
                text = (TextView) mInflater.inflate(R.layout.special_sections_section_list_item, parentView, false);
                text.setBackgroundColor(theme.getHeaderBackColor(mJournalHasDarkBackground));
                text.setTextColor(theme.getHeaderTextColor());
            }
            text.setText(mTitle);
            return text;
        }
    }

    private class SpecialSectionsAdapter extends BaseAdapter {

        private final Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                final List<ListItem> values = new ArrayList<>();
                if (constraint != null) {
                    final String searchText = constraint.toString().toLowerCase();
                    for (ListItem item : mAllSpecialSections) {
                        if (item.getType() == ListItem.HEADER) {
                            continue;
                        }
                        SectionItem sectionItem = (SectionItem) item;
                        if (sectionItem.getTitle().toLowerCase().contains(searchText)) {
                            values.add(sectionItem);
                        }
                    }
                }
                filterResults.values = values;
                filterResults.count = values.size();
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                List<ListItem> values = (List<ListItem>) results.values;
                mAdapter.notifyDataSetInvalidated();
                mCurrentSpecialSections.clear();
                mCurrentSpecialSections.addAll((values));
                mAdapter.notifyDataSetChanged();

                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                mNoResultsView.setPadding(0, (int) (size.y * 0.3), 0, 0);
                mNoResultsView.setVisibility(mCurrentSpecialSections.isEmpty() ? View.VISIBLE : View.GONE);
            }
        };

        @Override
        public int getCount() {
            return mCurrentSpecialSections.size();
        }

        @Override
        public Object getItem(int position) {
            return mCurrentSpecialSections.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return mCurrentSpecialSections.get(position).getType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCurrentSpecialSections.get(position).getView(convertView, parent);
        }

        @Override
        public boolean isEnabled(int position) {
            return mCurrentSpecialSections.get(position).getType() == ListItem.ITEM;
        }

        public void filter(CharSequence searchText) {
            filter.filter(searchText);
        }

    }

    private class LettersSideBarGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float barHeightPx = mLettersBarView.getHeight();
            float letterHeightPx = barHeightPx / mLettersBarView.getChildCount();
            float yPx = e2.getY();
            boolean consumed = (yPx >= 0) && (yPx <= barHeightPx);
            if (consumed) {
                int letterIndex = (int) (yPx / letterHeightPx);
                scrollToSectionHeaderItem(indexToCharLetter(letterIndex));
            }
            return consumed;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float barHeightPx = mLettersBarView.getHeight();
            float letterHeightPx = barHeightPx / mLettersBarView.getChildCount();
            float yPx = e.getY();
            int letterIndex = (int) (yPx / letterHeightPx);
            scrollToSectionHeaderItem(indexToCharLetter(letterIndex));
            return true;
        }

        private char indexToCharLetter(int index) {
            if (index == 26) {
                return '#';
            }
            return (char) ('A' + index);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

}