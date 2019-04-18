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
package com.wiley.android.journalApp.fragment.feeds;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.base.Society;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.components.SocietyGlobalSearchComponent;
import com.wiley.android.journalApp.error.ErrorButton;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.error.ErrorMessage;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.StartActivityForResultHelper;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.android.journalApp.widget.TouchRefreshLayout;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.Map;

/**
 * Created by alobachev on 23.06.14.
 */
public class SocietyGlobalSearchFragment
        extends
        JournalFragment
        implements
        StartActivityForResultHelper,
        SocietyGlobalSearchComponent.SocietyGlobalSearchComponentListener,
        Society {

    private final static String TAG = SocietyGlobalSearchFragment.class.getSimpleName() + ".life";

    public final static int RELEVANCY_MODE = 0;
    public final static int TITLE_MODE = 1;
    public final static int DATE_PUBLISHED_MODE = 2;

    @Inject
    private ErrorManager errorManager;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private QuickLinkMenuComponent quickLinkMenuComponent;

    private InterfaceHelper mInterfaceHelper;
    private SocietyGlobalSearchComponent mDataModelHelper;
    private boolean isShowing = false;

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
        Logger.d(TAG, "onCreateView()");
        return inflater.inflate(R.layout.frag_society_global_search_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        CustomWebView webView = findView(R.id.global_search_content_view_society);
        mDataModelHelper = new SocietyGlobalSearchComponent(this, webView);

        mInterfaceHelper = new InterfaceHelper(new InterfaceHelperListener() {
            @Override
            public MainActivity getMainActivity() {
                return (MainActivity) getJournalActivity();
            }

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
        Logger.d(TAG, "onStart()");
        super.onStart();
        notificationCenter.subscribeToNotification(EventList.MENU_BUTTON_IS_SHOWN.getEventName(), menuButtonIsShown);
        notificationCenter.subscribeToNotification(EventList.SETTINGS_WINDOW_IS_SHOWN.getEventName(), settingsWindowIsShownProcessor);
    }

    @Override
    public void onResume() {
        Logger.d(TAG, "onResume()");
        super.onResume();
        isShowing = true;
        mInterfaceHelper.onStart();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mInterfaceHelper != null) {
            mInterfaceHelper.onHiddenChanged(hidden);
        }
    }

    @Override
    public void onPause() {
        Logger.d(TAG, "onPause()");
        super.onPause();
        isShowing = false;
    }

    @Override
    public void onStop() {
        Logger.d(TAG, "onStop()");
        super.onStop();
        notificationCenter.unSubscribeFromNotification(menuButtonIsShown);
        notificationCenter.unSubscribeFromNotification(settingsWindowIsShownProcessor);
        mInterfaceHelper.onStop();
    }

    @Override
    public void onDestroyView() {
        Logger.d(TAG, "onDestroyView()");
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onAccessForbiddenArticle() {

    }

    @Override
    public void onSaveArticleNoInternetConnection(ArticleMO article) {
        if (isShowing) {
            onSaveArticleError(article);
        }
    }

    @Override
    public StartActivityForResultHelper getStartActivityForResultHelper() {
        return this;
    }

    @Override
    public Context getContext() {
        return getActivity();
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
    public void onSearchStarted() {
        mInterfaceHelper.onSearchStarted();
    }

    @Override
    public void onSearchCompleted(final boolean hasMatch) {

        mInterfaceHelper.onCompleted(hasMatch);
    }

    @Override
    public void onSearchProgress(int currentIndex, int size) {
        mInterfaceHelper.onSearchProgress(currentIndex, size);
    }

    private interface InterfaceHelperListener {
        MainActivity getMainActivity();

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

        private int mState = STATE_INIT;

        private final InterfaceHelperListener mHost;

        private ActionBarSherlockCompat mSherlock;

        private final EditText mSearcherView;

        private final View mRelevancyView;
        private final View mTitleView;
        private final View mDatePublishedView;
        private final View mWebView;
        private final View mFocusDummy;

        private final ViewGroup mProgressViewParent;
        private Handler hideProgressHandler = new Handler();
        private View progress;

        private int mCurrentSelectedButtonId;
        private View mCurrentSelectedButtonView;
        private int mMode;
        private boolean mAsc;
        private boolean mHasMatch;
        private boolean isTyping = true;
        private boolean isTypingSaved = true;
        private boolean isHidden = false;

        public InterfaceHelper(final InterfaceHelperListener interfaceHelperListener) {
            mHost = interfaceHelperListener;

            mRelevancyView = findView(R.id.relevancy_global_search_sort_menu_button);
            mTitleView = findView(R.id.title_global_search_sort_menu_button);
            mDatePublishedView = findView(R.id.date_published_global_search_sort_menu_button);
            mWebView = findView(R.id.global_search_content_view_society);
            mFocusDummy = findView(R.id.searcher_focus_dummy_society);

            // action bar
            if (DeviceUtils.isPhone(getContext())) {
                mSherlock = new ActionBarSherlockCompat(getJournalActivity(), 0,
                        (ViewGroup) findView(R.id.abs__screen_action_bar));
                float titlePaddingLeft = DeviceUtils.isPhone(getContext()) ?
                        getActivity().getResources().getDimension(R.dimen.action_bar_title_padding_left) : 0;
                mSherlock.setupActionBar()
                        .setBackgroundDrawable(theme.getMainColor())
                        .setTitleActionBar(getResources().getString(R.string.society_search_title), (int) titlePaddingLeft);
                mSherlock.setListener(new ActionBarSherlock.OnCreatePanelMenuListener() {
                    @Override
                    public boolean onCreatePanelMenu(int featureId, Menu menu) {
                        if (DeviceUtils.isPhone(mHost.getMainActivity())) {
                            menu.add(getString(R.string.action_show_menu))
                                    .setIcon(ActionBarUtils.getMenuIconResource(theme))
                                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem item) {
                                            mHost.getMainActivity().onSideMenuButtonClicked();
                                            return true;
                                        }
                                    });
                        }

                        return true;
                    }
                });
            }

            // feature: quick link menu
            if (DeviceUtils.isPhone(getJournalActivity())) {
                // touch layout
                ((TouchRefreshLayout) findView(R.id.touch_container)).setOnRefreshListener(new TouchRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        showQuickLinkMenu();
                    }
                });

                quickLinkMenuComponent.initQuickLink(getActivity(), SocietyGlobalSearchFragment.this);
            }


            ((MainActivity) getJournalActivity()).setListenerGlobalSearchSortMenu(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSortMenuButtonClick(v);
                }
            });

            mSearcherView = findView(R.id.searcher_edit_text_society);
            mSearcherView.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        final String term = mSearcherView.getText().toString();
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
                            if (null != mHost) {
                                setSearchState();
                                mMode = RELEVANCY_MODE;
                                mAsc = true;
                                mHost.onSearch(term, mMode, mAsc);
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
                    mHost.onCancel();
                }
            });
            progress = findView(R.id.progress);


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
            mSearcherView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    UIUtils.showSoftInput(mSearcherView);
                }
            }, 500);
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
            Logger.d(TAG, "InterfaceHelper.saveStateTyping(): isTyping = " + isTyping);
            isTypingSaved = isTyping;
            hideSoftKeyboard();
        }

        public void menuButtonIsShown() {
            saveStateTyping();
        }

        private void onStart() {
            if (!isHidden) {
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
            isHidden = hidden;
            if (hidden) {
                mDataModelHelper.onSearchCancel();
                isTypingSaved = isTyping;
                hideSoftKeyboard();
            } else {
                restoreStateTyping();
            }
        }

        public void init() {
            setTypingState();
            changeState(STATE_INIT);
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

        public void onSearchStarted() {
            changeState(InterfaceHelper.STATE_SEARCH_STARTED);
        }

        public void onSearchProgress(int currentIndex, int size) {
            final ProgressBar progressBar = (ProgressBar) mProgressViewParent.findViewById(R.id.article_progress_horizontal);
            if (null != progressBar && View.VISIBLE == progressBar.getVisibility()) {
                progressBar.setProgress(currentIndex * 100 / size);
            }
        }

        public void onCompleted(final boolean hasMatch) {
            mHasMatch = hasMatch;
            changeState(InterfaceHelper.STATE_SEARCH_COMPLETED);
        }

        public void onCancel() {
            changeState(InterfaceHelper.STATE_SEARCH_CANCEL);
        }

        private void onSortMenuButtonClick(final View view) {
            int id = view.getId();

            // change select button
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
                } else if (id == R.id.date_published_global_search_sort_menu_button) {
                    mMode = DATE_PUBLISHED_MODE;
                }

                mAsc = !((Checkable) view).isChecked();
                mDataModelHelper.onSort(mMode, mAsc);
            } else {
                if (mCurrentSelectedButtonId > 0) {
                    final Checkable button = (Checkable) view;
                    button.toggle();

                    if (!button.isChecked()) {
                        mAsc = true;
                    } else {
                        mAsc = false;
                    }

                    mDataModelHelper.onSort(mMode, mAsc);
                }
            }
        }

        private void changeState(int newState) {
            switch (newState) {
                case STATE_INIT:
                    mHasMatch = false;
                    resetMenuBar();

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

                    final ProgressBar progressBar = (ProgressBar) mProgressViewParent.findViewById(R.id.article_progress_horizontal);
                    progressBar.setProgress(0);

                    final TextView progressMsg = (TextView) mProgressViewParent.findViewById(R.id.article_progress_text);
                    progressMsg.setText(getString(R.string.searching_articles));

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
                    disableMenuBar();
                    showProgress();
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
            mState = newState;
        }

        private void showProgress() {
            if (progress == null) {
                return;
            }
            Logger.d(getClass().getSimpleName(), "showProgress");
            hideProgressHandler.removeCallbacksAndMessages(null);
            progress.setVisibility(View.VISIBLE);
        }

        private void hideProgress() {
            if (progress == null || View.VISIBLE != progress.getVisibility()) {
                return;
            }
            Logger.d(getClass().getSimpleName(), "hideProgress");
            hideProgressHandler.removeCallbacksAndMessages(null);
            hideProgressHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            progress.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    fadeOut.setDuration(500);
                    progress.startAnimation(fadeOut);
                }
            }, 200);
        }

        private void resetMenuBar() {
            mRelevancyView.setSelected(true);
            mTitleView.setSelected(false);
            mDatePublishedView.setSelected(false);

            if (mHasMatch) {
                enableMenuBar();
            } else {
                disableMenuBar();
            }
        }

        private void enableMenuBar() {
            setEnabledMenuBar(true);
        }

        private void disableMenuBar() {
            setEnabledMenuBar(false);
        }

        private void setEnabledMenuBar(final boolean enabled) {
            mRelevancyView.setEnabled(enabled);
            final View relevancyMenuButtonLabel = findView(R.id.relevancy_global_search_menu_button_label);
            if (relevancyMenuButtonLabel == null) {
                return;
            }

            relevancyMenuButtonLabel.setEnabled(!enabled);

            mTitleView.setEnabled(enabled);
            findView(R.id.title_global_search_menu_button_label).setEnabled(!enabled);

            mDatePublishedView.setEnabled(enabled);
            findView(R.id.date_published_global_search_menu_button_label).setEnabled(!enabled);
        }

        /**
         * feature: quick link menu
         */
        private void showQuickLinkMenu() {
            quickLinkMenuComponent.showQuickLinkMenu();
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
