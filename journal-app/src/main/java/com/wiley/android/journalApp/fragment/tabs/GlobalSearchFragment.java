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

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.GlobalSearchComponent;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.Map;

/**
 * Created by taraskreknin on 23.06.14.
 */
public class GlobalSearchFragment
        extends
        BaseTabArticleComponentHostFragment
        implements
        GlobalSearchComponent.GlobalSearchComponentListener {

    public final static String TAG = GlobalSearchFragment.class.getSimpleName() + ".life";
    public final static int RELEVANCY_MODE = 0;
    public final static int TITLE_MODE = 1;
    public final static int ISSUE_MODE = 2;
    public final static int VOLUME_MODE = 3;
    public final static int SECTION_MODE = 4;
    public final static int DATE_PUBLISHED_MODE = 5;
    public final static int FIRST_ONLINE_MODE = 6;

    @Inject
    private ArticleService mArticleService;
    @Inject
    private IssueService mIssueService;
    @Inject
    private ErrorManager errorManager;
    @Inject
    private NotificationCenter notificationCenter;

    private InterfaceHelper mInterfaceHelper;
    private GlobalSearchComponent mDataModelHelper;

    private NotificationProcessor menuButtonIsShown = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            mInterfaceHelper.menuButtonIsShown();
        }
    };

    private NotificationProcessor settingsWindowIsShownProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            mInterfaceHelper.isTypingSaved = true;
            mInterfaceHelper.mSearcherView.setText("");
            mDataModelHelper.init();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_global_search_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CustomWebView webView = findView(R.id.global_search_content_view_journal);
        mDataModelHelper = new GlobalSearchComponent(this, webView);
        mDataModelHelper.onCreateHost();

        mInterfaceHelper = new InterfaceHelper(new InterfaceHelperListener() {
            @Override
            public void onSearch(String term, int mode, boolean asc) {
                mDataModelHelper.onSearch(term, mode, asc);
            }

            @Override
            public void onCancel() {
                mInterfaceHelper.onCancel();
                mDataModelHelper.onSearchCancel();
            }
        });

        mDataModelHelper.init();
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(EventList.MENU_BUTTON_IS_SHOWN.getEventName(), menuButtonIsShown);
        notificationCenter.subscribeToNotification(EventList.SETTINGS_WINDOW_IS_SHOWN.getEventName(), settingsWindowIsShownProcessor);
        mDataModelHelper.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mInterfaceHelper.onStart();
    }

    @Override
    public void onShow() {
        if (null != mInterfaceHelper) {
            super.onShow();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mInterfaceHelper != null) {
            mInterfaceHelper.onHiddenChanged(hidden);
        }
        if (!hidden && mDataModelHelper != null) {
            mDataModelHelper.executePostponedTasks();
        }
    }

    @Override
    public void onShowTab() {
        super.onShowTab();
        if (null != mDataModelHelper && null != mInterfaceHelper) {
            mInterfaceHelper.onShowTab();
            mDataModelHelper.init();
        }
    }

    @Override
    public void onHideTab() {
        super.onHideTab();
        mInterfaceHelper.onHideTab();
    }

    @Override
    public void onShowBack() {
        super.onShowBack();
        mInterfaceHelper.onShowBack();
        mDataModelHelper.init();
    }

    @Override
    public void onHide() {
        super.onHide();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(menuButtonIsShown);
        notificationCenter.unSubscribeFromNotification(settingsWindowIsShownProcessor);
        mInterfaceHelper.onStop();
        mDataModelHelper.onStop();
    }

    @Override
    public void onDestroyView() {
        mDataModelHelper.onDestroyHost();
        super.onDestroyView();
    }

    public void onSoftKeyboardVisibleChanged(boolean visible) {
        mInterfaceHelper.onSoftKeyboardVisibleChanged(visible);
    }

    @Override
    protected int getTabId() {
        return R.id.global_search_tab;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDataModelHelper.onOrientationChanged();
    }

    @Override
    public void onSortStarted() {
        mInterfaceHelper.onSortStarted();
    }

    @Override
    public void onSortCompleted() {
        mInterfaceHelper.onSortCompleted();
    }

    @Override
    public void onRenderStarted() {
        mInterfaceHelper.onRenderStarted();
    }

    @Override
    public void onRenderCompleted() {
        mInterfaceHelper.onRenderCompleted();
    }

    @Override
    public boolean hasDownloadedIssues() {
        return mIssueService.getDownloadedIssuesCount() > 0;
    }

    @Override
    public void onSearchStarted() {
        mInterfaceHelper.onStarted();
    }

    @Override
    public void onSearchCompleted(final boolean hasMatch) {
        mInterfaceHelper.onCompleted(hasMatch);
    }

    @Override
    public void onProgress(int currentIndex, int size) {
        mInterfaceHelper.onSearchProgress(currentIndex, size);
    }

    private interface InterfaceHelperListener {
        void onSearch(String term, int mode, boolean acs);

        void onCancel();
    }

    private class InterfaceHelper {

        private final static int STATE_INIT = 0;
        private final static int STATE_SEARCH_STARTED = 1;
        private final static int STATE_SEARCH_COMPLETED = 2;
        private final static int STATE_SEARCH_CANCEL = 3;
        private final static int STATE_SORT_STARTED = 4;
        private final static int STATE_SORT_COMPLETED = 5;
        private final static int STATE_RENDER_STARTED = 6;
        private final static int STATE_RENDER_COMPLETED = 7;

        final private InterfaceHelperListener mListener;

        final private EditText mSearcherView;

        final private View mRelevancyView;
        final private View mTitleView;
        final private View mIssueView;
        final private View mVolumeView;
        final private View mSectionView;
        final private View mDatePublishedView;
        final private View mFirstOnlineView;
        final private View mScrollLayoutMenuBar;
        final private View mWebView;
        final private View mFocusDummy;

        int scrollLayoutLeft = 0;
        int scrollLayoutTop = 0;
        int scrollLayoutRight = 0;
        int scrollLayoutBottom = 0;

        private final ViewGroup mProgressViewParent;

        private int mCurrentSelectedButtonId;
        private View mCurrentSelectedButtonView;
        private int mMode;
        private boolean mAsc;
        private boolean mHasMatch;
        private boolean isTyping = true;
        private boolean isTypingSaved = true;

        public InterfaceHelper(final InterfaceHelperListener interfaceHelperListener) {
            mListener = interfaceHelperListener;

            ((MainActivity) getJournalActivity()).setListenerGlobalSearchSortMenu(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBarButtonClick(v);
                }
            });

            mRelevancyView = findView(R.id.relevancy_global_search_sort_menu_button);
            mTitleView = findView(R.id.title_global_search_sort_menu_button);
            mIssueView = findView(R.id.issue_global_search_sort_menu_button);
            mVolumeView = findView(R.id.volume_global_search_sort_menu_button);
            mSectionView = findView(R.id.section_global_search_sort_menu_button);
            mDatePublishedView = findView(R.id.date_published_global_search_sort_menu_button);
            mFirstOnlineView = findView(R.id.first_online_global_search_sort_menu_button);
            mScrollLayoutMenuBar = findView(R.id.global_search_menu_scroll_layout);
            mWebView = findView(R.id.global_search_content_view_journal);
            mFocusDummy = findView(R.id.searcher_focus_dummy_journal);

            mScrollLayoutMenuBar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (scrollLayoutLeft != left || scrollLayoutTop != top || scrollLayoutRight != right || scrollLayoutBottom != bottom) {
                        scrollLayoutLeft = left;
                        scrollLayoutTop = top;
                        scrollLayoutRight = right;
                        scrollLayoutBottom = bottom;

                        mScrollLayoutMenuBar.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int spaceSize = mScrollLayoutMenuBar.getWidth() - findView(R.id.global_search_menu_items_layout).getWidth();
                                spaceSize = spaceSize < 10 ? 0 : spaceSize / 2;

                                View spacerView = findView(R.id.global_search_menu_spacer);
                                ViewGroup.LayoutParams layoutParams = spacerView.getLayoutParams();
                                layoutParams.width = spaceSize;
                                spacerView.setLayoutParams(layoutParams);
                            }
                        }, 100);
                    }
                }
            });

            mCurrentSelectedButtonId = -1;

            mProgressViewParent = findView(R.id.article_progress);
            mProgressViewParent.setVisibility(View.GONE);
            mProgressViewParent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    return true;
                }
            });
            mProgressViewParent.findViewById(R.id.article_progress_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    mListener.onCancel();
                }
            });

            mSearcherView = findView(R.id.searcher_edit_text_journal);
            mSearcherView.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        final String term = mSearcherView.getText().toString().trim();
                        if (term.length() < 3) {
                            errorManager.alertWithErrorMessage(getJournalActivity(),
                                    new ErrorMessage("Too short search term", "Please type 3 or more letters to search"),
                                    ErrorButton.withTitleAndListener(getActivity().getString(R.string.close), new ErrorButton.OnClickListener() {
                                        @Override
                                        public void onClick() {
                                            setTypingState();
                                        }
                                    }));
                        } else {
                            if (null != mListener) {
                                setSearchState();
                                mMode = RELEVANCY_MODE;
                                mAsc = true;
                                mListener.onSearch(term, mMode, mAsc);
                            }
                        }

                        return true;
                    }
                    return false;
                }
            });
            mSearcherView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        isTyping = true;
                        showSoftKeyboard();
                    }
                }
            });

            // add clear button to searcher
            final Drawable drawable = getResources().getDrawable(R.drawable.seacher_text_clear);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            mSearcherView.setCompoundDrawables(null, null, drawable, null);
            ImageView crossImageView = new ImageView(getJournalActivity());
            crossImageView.setImageDrawable(drawable);
            mSearcherView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mSearcherView.getCompoundDrawables()[2] == null) {
                        return false;
                    }
                    if (event.getAction() != MotionEvent.ACTION_UP) {
                        return false;
                    }
                    if (event.getX() > mSearcherView.getWidth() - mSearcherView.getPaddingRight() - drawable.getIntrinsicWidth()) {
                        mSearcherView.setText("");
                        mSearcherView.setCompoundDrawables(null, null, null, null);
                    }
                    return false;
                }
            });
            mSearcherView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mSearcherView.setCompoundDrawables(null, null, mSearcherView.getText().toString().equals("") ? null : drawable, null);
                }

                @Override
                public void afterTextChanged(Editable arg0) {
                }

            });

            mWebView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        setContentState();
                    }
                }
            });
            mFocusDummy.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    isTyping = false;
                }
            });

            init();
        }

        private void showSoftKeyboard() {
            if (((MainActivity) getActivity()).isFragmentOnTop(getParentFragment())) {
                mSearcherView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        UIUtils.showSoftInput(mSearcherView);
                    }
                }, 500);
            }
        }

        private void hideSoftKeyboard() {
            UIUtils.hideSoftInput(mSearcherView);
        }

        private void setTypingState() {
            Logger.d(TAG, "InterfaceHelper.setTypingState()");
            isTyping = true;
            mSearcherView.requestFocus();
            showSoftKeyboard();
        }

        private void setSearchState() {
            Logger.d(TAG, "InterfaceHelper.setSearchState()");
            isTyping = false;
            mFocusDummy.requestFocus();
            hideSoftKeyboard();
        }

        private void setContentState() {
            Logger.d(TAG, "InterfaceHelper.setContentState()");
            isTyping = false;
            mFocusDummy.requestFocus();
            hideSoftKeyboard();
        }

        private void restoreStateTyping() {
            Logger.d(TAG, "InterfaceHelper.restoreStateTyping(): isTypingSaved = " + isTypingSaved);
            isTyping = isTypingSaved;
            if (isTypingSaved) {
                mSearcherView.requestFocus();
                showSoftKeyboard();
            } else {
                mFocusDummy.requestFocus();
                hideSoftKeyboard();
            }
        }

        private void saveStateTyping() {
            Logger.d(TAG, "InterfaceHelper.saveStateTyping()");
            isTypingSaved = isTyping;
            hideSoftKeyboard();
        }

        public void menuButtonIsShown() {
            Logger.d(TAG, "InterfaceHelper.saveStateTyping()");
            isTypingSaved = isTyping;
        }

        private void onStart() {
            if (GlobalSearchFragment.this.isVisible()) {
                restoreStateTyping();
            }
        }

        private void onStop() {
            saveStateTyping();
        }

        private void onSoftKeyboardVisibleChanged(boolean visible) {
            Logger.d(TAG, "InterfaceHelper.onSoftKeyboardVisibleChanged() visible = " + visible);
        }

        public void onHiddenChanged(boolean hidden) {
            Logger.d(TAG, "InterfaceHelper.onHiddenChanged(): hidden = " + hidden);
            if (hidden) {
                isTypingSaved = isTyping;
                hideSoftKeyboard();
            } else {
                restoreStateTyping();
            }
        }

        private void onShowTab() {
            isTypingSaved = true;
            init();
        }

        private void onHideTab() {
            saveStateTyping();
            mInterfaceHelper.onCancel();
            isTypingSaved = true;
        }

        private void onShowBack() {
            isTypingSaved = true;
            init();
        }

        public void init() {
            setTypingState();
            changeState(InterfaceHelper.STATE_INIT);
        }

        public void onBarButtonClick(final View view) {
            int id = view.getId();

            // change select button
            boolean needSort = false;
            if (id != mCurrentSelectedButtonId) {
                if (mCurrentSelectedButtonId >= 0) {
                    mCurrentSelectedButtonView.setSelected(false);
                }

                view.setSelected(true);
                mCurrentSelectedButtonId = id;
                mCurrentSelectedButtonView = view;
                if (id == R.id.relevancy_global_search_sort_menu_button) {
                    mMode = RELEVANCY_MODE;
                } else if (id == R.id.title_global_search_sort_menu_button) {
                    mMode = TITLE_MODE;
                } else if (id == R.id.issue_global_search_sort_menu_button) {
                    mMode = ISSUE_MODE;
                } else if (id == R.id.volume_global_search_sort_menu_button) {
                    mMode = VOLUME_MODE;
                } else if (id == R.id.section_global_search_sort_menu_button) {
                    mMode = SECTION_MODE;
                } else if (id == R.id.date_published_global_search_sort_menu_button) {
                    mMode = DATE_PUBLISHED_MODE;
                } else if (id == R.id.first_online_global_search_sort_menu_button) {
                    mMode = FIRST_ONLINE_MODE;
                }

                mAsc = !((Checkable) view).isChecked();
                needSort = true;
            } else {
                if (mCurrentSelectedButtonId > 0) {
                    final Checkable button = (Checkable) view;
                    button.toggle();

                    mAsc = !button.isChecked();

                    needSort = true;
                }
            }

            if (needSort) {
                mDataModelHelper.onSort(mMode, mAsc);
            }
        }

        public void onSortStarted() {
            changeState(InterfaceHelper.STATE_SORT_STARTED);
        }

        public void onSortCompleted() {
            changeState(InterfaceHelper.STATE_SORT_COMPLETED);
        }

        public void onRenderStarted() {
            changeState(InterfaceHelper.STATE_RENDER_STARTED);
        }

        public void onRenderCompleted() {
            changeState(InterfaceHelper.STATE_RENDER_COMPLETED);
        }

        public void onSearchProgress(int currentIndex, int size) {
            final ProgressBar progressBar = (ProgressBar) mProgressViewParent.findViewById(R.id.article_progress_horizontal);
            if (null != progressBar && View.VISIBLE == progressBar.getVisibility()) {
                progressBar.setProgress(currentIndex * 100 / size);
            }
        }

        public void onStarted() {
            changeState(InterfaceHelper.STATE_SEARCH_STARTED);
        }

        public void onCompleted(final boolean hasMatch) {
            mHasMatch = hasMatch;
            changeState(InterfaceHelper.STATE_SEARCH_COMPLETED);
        }

        public void onCancel() {
            changeState(InterfaceHelper.STATE_SEARCH_CANCEL);
        }

        private void changeState(int newState) {
            switch (newState) {
                case STATE_INIT:
                    mHasMatch = false;
                    resetMenuBar();

                    if (!hasDownloadedIssues()) {
                        mIssueView.setVisibility(View.GONE);
                        mVolumeView.setVisibility(View.GONE);
                        mSectionView.setVisibility(View.GONE);
                    } else {
                        mIssueView.setVisibility(View.VISIBLE);
                        mVolumeView.setVisibility(View.VISIBLE);
                        mSectionView.setVisibility(View.VISIBLE);
                    }

                    mCurrentSelectedButtonId = R.id.relevancy_global_search_sort_menu_button;
                    mCurrentSelectedButtonView = mRelevancyView;
                    mMode = RELEVANCY_MODE;
                    mAsc = true;

                    mSearcherView.setText("");
                    mSearcherView.requestFocus();
                    mSearcherView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            disableMenuBar();
                        }
                    }, 1000);
                    break;
                case STATE_SEARCH_STARTED:
                    disableMenuBar();

                    mProgressViewParent.setVisibility(View.VISIBLE);
                    mProgressViewParent.findViewById(R.id.article_progress_circle).setVisibility(View.GONE);
                    mProgressViewParent.findViewById(R.id.article_progress_horizontal_parent).setVisibility(View.VISIBLE);

                    final TextView progressMsg = (TextView) mProgressViewParent.findViewById(R.id.article_progress_text);
                    progressMsg.setText(getString(R.string.searching_articles));

                    final ProgressBar progressBar = (ProgressBar) mProgressViewParent.findViewById(R.id.article_progress_horizontal);
                    progressBar.setProgress(0);

                    dim(100);
                    break;
                case STATE_SEARCH_COMPLETED:
                    mProgressViewParent.setVisibility(View.GONE);

                    resetMenuBar();

                    mCurrentSelectedButtonId = R.id.relevancy_global_search_sort_menu_button;
                    mCurrentSelectedButtonView = mRelevancyView;

                    undim();
                    break;

                case STATE_SEARCH_CANCEL:
                    mProgressViewParent.setVisibility(View.GONE);

                    if (mHasMatch) {
                        enableMenuBar();
                    }
                    undim();
                    break;

                case STATE_SORT_STARTED:
                    showProgress();
                    disableMenuBar();
                    dim(100);
                    break;
                case STATE_RENDER_COMPLETED:
                    hideProgress();
                    if (mHasMatch) {
                        enableMenuBar();
                    }
                    undim();
                    break;
            }
        }

        private void resetMenuBar() {
            mRelevancyView.setSelected(true);
            mTitleView.setSelected(false);
            mDatePublishedView.setSelected(false);
            mFirstOnlineView.setSelected(false);
            if (hasDownloadedIssues()) {
                mIssueView.setSelected(false);
                mVolumeView.setSelected(false);
                mSectionView.setSelected(false);
            }

            if (mHasMatch) {
                enableMenuBar();
            } else {
                disableMenuBar();
            }
        }

        private void enableMenuBar() {
            mRelevancyView.setEnabled(true);
            findView(R.id.relevancy_global_search_menu_button_label).setEnabled(false);

            mTitleView.setEnabled(true);
            findView(R.id.title_global_search_menu_button_label).setEnabled(false);

            mDatePublishedView.setEnabled(true);
            findView(R.id.date_published_global_search_menu_button_label).setEnabled(false);

            mFirstOnlineView.setEnabled(true);
            findView(R.id.first_online_global_search_menu_button_label).setEnabled(false);

            if (hasDownloadedIssues()) {
                mIssueView.setEnabled(true);
                findView(R.id.issue_global_search_menu_button_label).setEnabled(false);

                mVolumeView.setEnabled(true);
                findView(R.id.volume_global_search_menu_button_label).setEnabled(false);

                mSectionView.setEnabled(true);
                findView(R.id.section_global_search_menu_button_label).setEnabled(false);
            }
        }

        private void disableMenuBar() {
            mRelevancyView.setEnabled(false);
            findView(R.id.relevancy_global_search_menu_button_label).setEnabled(true);

            mTitleView.setEnabled(false);
            findView(R.id.title_global_search_menu_button_label).setEnabled(true);

            mDatePublishedView.setEnabled(false);
            findView(R.id.date_published_global_search_menu_button_label).setEnabled(true);

            mFirstOnlineView.setEnabled(false);
            findView(R.id.first_online_global_search_menu_button_label).setEnabled(true);

            if (hasDownloadedIssues()) {
                mIssueView.setEnabled(false);
                findView(R.id.issue_global_search_menu_button_label).setEnabled(true);

                mVolumeView.setEnabled(false);
                findView(R.id.volume_global_search_menu_button_label).setEnabled(true);

                mSectionView.setEnabled(false);
                findView(R.id.section_global_search_menu_button_label).setEnabled(true);
            }
        }
    }

    //TODO put this to common class
    public void dim(final int alphaLevel) {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(alphaLevel);
        }
    }

    public void undim() {
        final FrameLayout dimmable = getDimmableView();
        if (dimmable != null) {
            dimmable.getForeground().setAlpha(0);
        }
    }

    //    @Override TODO
    public FrameLayout getDimmableView() {
        return (FrameLayout) findView(R.id.article_view_dimmer);
    }
}
