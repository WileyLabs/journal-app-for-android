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

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.IssueTOCFragment;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.controller.IssueTOCController;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.utils.AssetsUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.IssueDownloadHandler;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    public interface EditModeListener {
        void onToggleEditMode(IssueView sender);

        void onToggleEditModeSelected(IssueView sender);
    }

    public interface OnDownloadStartClickListener {
        void onClick(IssueView sender, DOI doi);
    }

    public interface OnDownloadCancelClickListener {
        void onClick(IssueView sender, DOI doi);
    }

    public interface IssueViewHost {
        ImageLoaderHelper getImageLoader();

        Theme getJournalTheme();

        IssueService getIssueService();

        ArticleService getArticleService();

        void alertIssueIsNotAvailableOffline();
    }

    private final static String TAG = IssueView.class.getSimpleName();

    private static String sNumberFormat;
    private static String sVolumeFormat;

    private boolean showNumOfSavedArticles;
    private boolean mShowCover;
    private boolean isEditMode = false;
    private boolean online = false;
    private ProgressBar mProgress;
    private TextView mSavedArticlesCountView;
    private IssueMO mIssue;
    private ImageView mCoverView;
    private TextView issuedAtTextView;
    private TextView volumeTextView;
    private TextView numberTextView;
    private TextView issueDescription;
    private Context mContext;

    private EditModeListener mEditListener;
    private IssueDownloadHandler mDownloadHandler;
    private OnDownloadStartClickListener mDownloadStartClickListener;
    private OnDownloadCancelClickListener mDownloadCancelClickListener;
    private IssueViewHost mHost;

    public IssueView(final Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public IssueView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    public IssueView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(attrs);
    }

    private void init(final AttributeSet attrs) {

        sNumberFormat = getResources().getString(R.string.issue_number);
        sVolumeFormat = getResources().getString(R.string.issue_volume);

        showNumOfSavedArticles = isShowNumOfSavedArticles(attrs);
        mShowCover = needShowCover(attrs);
        mProgress = (ProgressBar) findViewById(R.id.issue_progress);
        mDownloadHandler = new IssueDownloadHandler(getContext()) {
            @Override
            protected void onCancelDownload() {
                if (mDownloadCancelClickListener != null) {
                    mDownloadCancelClickListener.onClick(IssueView.this, mIssue.getDOI());
                }
            }

            @Override
            protected void onDownloadButtonClick() {
                if (mDownloadStartClickListener != null) {
                    mDownloadStartClickListener.onClick(IssueView.this, mIssue.getDOI());
                }
            }
        };
        mDownloadHandler.setShowDownloadButton(needShowDownloadButton(attrs));
    }

    public void onNetworkStateChanged(final boolean available) {
        online = available;
    }

    public DOI getDOI() {
        return mIssue.getDOI();
    }

    public void fillBy(final IssueMO issue) {
        mIssue = issue;
        mDownloadHandler.init(findViewById(R.id.download_issue_parent), issue);
        updateView();
    }

    private void updateView() {
        if (mIssue == null) {
            return;
        }

        if (mCoverView == null) {
            mCoverView = (ImageView) findViewById(R.id.cover);
        }

        if (mCoverView != null) {
            if (mCoverView.isClickable()) {
                mCoverView.setOnClickListener(this);
            }

            if (mCoverView.isLongClickable()) {
                mCoverView.setOnLongClickListener(this);
            }

            mCoverView.setTag(mIssue.getDOI());
            if (mShowCover) {
                if (TextUtils.isEmpty(mIssue.getCoverImageUrl())) {
                    AssetsUtils.showBitmap(mCoverView, "Newsstand-Cover-Icon.png", DisplayMetrics.DENSITY_XHIGH);
                } else {
                    mHost.getImageLoader().displayImage(mIssue.getCoverImageUrl(), mCoverView);
                }
            } else {
                mCoverView.setImageBitmap(null);
            }
        }

        final View issueContentGroup = findViewById(R.id.issue_content);
        if (issueContentGroup != null) {
            if (issueContentGroup.isClickable()) {
                issueContentGroup.setOnClickListener(this);
            }

            if (issueContentGroup.isLongClickable()) {
                issueContentGroup.setOnLongClickListener(this);
            }

            issueContentGroup.setTag(mIssue.getDOI());
        }

        if (issuedAtTextView == null) {
            issuedAtTextView = (TextView) findViewById(R.id.issuedAt);
        }

        String coverDate = mIssue.getCoverDate();
        if (mHost.getClass() != IssueTOCFragment.class && DeviceUtils.isPortrait(mContext)) {
            final String[] months = coverDate.split("/");

            if (months.length > 1) {
                final String pattern = "((Jan|Feb|Mar|Apr|May|June|July|Aug|Sept|Oct|Nov|Dec)(ruary|uary|ch|il|ust|ember|ober|ember)).*";
                final Pattern p = Pattern.compile(pattern);

                for (String month : months) {
                    final Matcher matcher = p.matcher(month);
                    if (matcher.matches()) {
                        coverDate = coverDate.replaceFirst(matcher.group(1), matcher.group(2));
                    }
                }
            }
        }

        setTextOrHide(issuedAtTextView, coverDate);

        if (volumeTextView == null) {
            volumeTextView = (TextView) findViewById(R.id.volume);
        }
        setTextOrHide(volumeTextView, sVolumeFormat, mIssue.getVolumeNumber());

        if (numberTextView == null) {
            numberTextView = (TextView) findViewById(R.id.number);
        }
        setTextOrHide(numberTextView, sNumberFormat, mIssue.getIssueNumber());

        if (issueDescription == null) {
            issueDescription = (TextView) findViewById(R.id.issue_description);
        }
        if (issueDescription != null) {
            final String description = TextUtils.isEmpty(mIssue.getIssueDescription()) ? "" :
                    ("Special Issue: " + mIssue.getIssueDescription());
            setTextOrHide(issueDescription, HtmlUtils.stripHtml(description));
        }

        if (showNumOfSavedArticles && mSavedArticlesCountView == null) {
            mSavedArticlesCountView = (TextView) findViewById(R.id.numOfSavedArticles);
        }

        if (mProgress == null) {
            mProgress = (ProgressBar) findViewById(R.id.issue_progress);
        }

        updateDownloadState();
        updateSavedArticlesCountView(mIssue);
        updateStateViews(mIssue);
    }

    public void updateDownloadState() {
        boolean isRemoving = mHost.getIssueService().isIssueRemoving(mIssue.getDOI());
        if (mIssue.isLocal() && !isRemoving) {
            stopProgress();
            mDownloadHandler.onDownloadStopped();
            return;
        }
        boolean isLoading = mHost.getIssueService().isIssueLoading(mIssue.getDOI());
        boolean isUpdating = mHost.getIssueService().isIssueUpdating(mIssue.getDOI());
        if (isLoading) {
            mDownloadHandler.onDownloadStarted(mIssue.getDownloadSize());
        } else {
            mDownloadHandler.onDownloadStopped();
        }
        if (isUpdating || isRemoving) {
            startProgress();
        } else {
            stopProgress();
        }
    }

    private void updateSavedArticlesCountView(IssueMO issue) {
        if (showNumOfSavedArticles && issue != null) {
            if (mDownloadHandler.isProgressShowing() || mDownloadHandler.isDownloadButtonShowing()) {
                mSavedArticlesCountView.setVisibility(View.GONE);
                return;
            }
            final int numOfSavedArticles = mIssue.getFavoritesCounter();
            String numString = null;
            if (numOfSavedArticles > 0) {
                numString = getResources().getQuantityString(R.plurals.num_of_saved_articles, numOfSavedArticles, numOfSavedArticles);
            }
            setTextOrHide(mSavedArticlesCountView, numString);
        }
    }

    private void updateStateViews(final DOI doi) {
        final IssueMO issue;
        try {
            issue = mHost.getIssueService().getIssue(doi);
        } catch (ElementNotFoundException e) {
            throw new RuntimeException(e);
        }
        mIssue = issue;
        updateStateViews(mIssue);
    }

    private void updateStateViews(final IssueMO issue) {
        final TextView issueStateView = (TextView) findViewById(R.id.issue_state);
        final View coverSubstrateView = findViewById(R.id.issue_cover_substrate);
        if (coverSubstrateView == null) {
            return;
        }

        final boolean isNew = issue.isNew();
        final boolean isDownloaded = issue.isLocal();

        if (isDownloaded) {
            issueStateView.setText(getContext().getString(R.string.issue_downloaded));
            coverSubstrateView.setBackgroundColor(mHost.getJournalTheme().getColorForDownloadedIssue());
            issueStateView.setVisibility(View.VISIBLE);
        } else if (isNew) {
            issueStateView.setText(getContext().getString(R.string.issue_new));
            coverSubstrateView.setBackgroundColor(mHost.getJournalTheme().getColorForNewIssue());
            issueStateView.setVisibility(View.VISIBLE);
        } else if (!mShowCover) {
            issueStateView.setText("");
            coverSubstrateView.setBackgroundColor(mHost.getJournalTheme().getColorForNoCoverIssue());
            issueStateView.setVisibility(View.VISIBLE);
        } else {
            coverSubstrateView.setBackgroundColor(Color.TRANSPARENT);
            issueStateView.setVisibility(View.INVISIBLE);
        }
    }

    private void setTextOrHide(final TextView view, final String text) {
        setTextOrHide(view, null, text);
    }

    private void setTextOrHide(final TextView view, final String format, final String value) {
        if (TextUtils.isEmpty(value) || value.equals("0")) {
            view.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(format)) {
            view.setText(value);
        } else {
            view.setText(String.format(format, value));
        }
        view.setVisibility(View.VISIBLE);
    }

    private boolean isShowNumOfSavedArticles(final AttributeSet attrs) {
        return attrs.getAttributeBooleanValue(null, "showNumOfSavedArticles", true);
    }

    private boolean needShowDownloadButton(final AttributeSet attrs) {
        return attrs != null && attrs.getAttributeBooleanValue(null, "showDownloadButton", false);
    }

    private boolean needShowCover(final AttributeSet attrs) {
        return attrs == null || attrs.getAttributeBooleanValue(null, "showCover", true);
    }

    public void setEditMode(final boolean isEditMode) {
        this.isEditMode = isEditMode;
        if (!isEditMode) {
            setSelected(false);
        }
    }

    @Override
    public void onClick(final View v) {
        Logger.d(TAG, "Clicked on issue");
        if (isEditMode) {
            mEditListener.onToggleEditModeSelected(this);
        } else {
            if (!online && mHost.getArticleService().getNumOfArticlesForIssueTOC(mIssue.getDOI()) == 0) {
                mHost.alertIssueIsNotAvailableOffline();
            } else {
                new IssueTOCController(v.getContext()).open((DOI) v.getTag());
            }
        }
    }

    public boolean canSelectForEdit() {
        return mIssue.isLocal();
    }

    public void setEditListener(final EditModeListener l) {
        mEditListener = l;
    }

    public void setDownloadStartClickListener(OnDownloadStartClickListener l) {
        mDownloadStartClickListener = l;
    }

    public void setDownloadCancelClickListener(OnDownloadCancelClickListener l) {
        mDownloadCancelClickListener = l;
    }

    @Override
    public boolean onLongClick(final View view) {
        Logger.d(TAG, "Long click on issue");
        if (!mIssue.isLocal()) {
            return false;
        }
        if (mEditListener != null) {
            mEditListener.onToggleEditMode(this);
        }
        return true;
    }

    private void startProgress() {
        if (mProgress != null) {
            mProgress.setVisibility(View.VISIBLE);
        }
    }

    private void stopProgress() {
        if (mProgress != null) {
            mProgress.setVisibility(View.INVISIBLE);
        }
    }

    public void updateUiState() {
        final View rootView = findViewById(R.id.issue_parent);
        if (rootView == null) {
            return;
        }
        rootView.setBackgroundDrawable(null);
        if (isEditMode) {
            if (canSelectForEdit()) {
                updateControlsAlpha(1.0f);
                if (isSelected()) {
                    rootView.setBackgroundColor(mHost.getJournalTheme().getColorForSelectedIssue());
                }
            } else {
                updateControlsAlpha(0.25f);
            }
        } else {
            if (online || mHost.getArticleService().getNumOfArticlesForIssueTOC(mIssue.getDOI()) > 0) {
                updateControlsAlpha(1.0f);
            } else {
                updateControlsAlpha(0.5f);
            }
        }
    }

    private void updateControlsAlpha(float alpha) {
        View coverView = findViewById(R.id.cover);
        if (coverView != null) {
            coverView.setAlpha(alpha);
        } else {
            findViewById(R.id.issuedAt).setAlpha(alpha);
            findViewById(R.id.volume).setAlpha(alpha);
            findViewById(R.id.number).setAlpha(alpha);
            findViewById(R.id.numOfSavedArticles).setAlpha(alpha);
        }
    }

    public void onDownloadStarted() {
        mDownloadHandler.onDownloadStarted(mIssue.getDownloadSize());
        if (mSavedArticlesCountView != null) {
            mSavedArticlesCountView.setVisibility(View.GONE);
        }
    }

    public void onDownloadStopped(boolean success) {
        stopProgress();
        mDownloadHandler.onDownloadStopped();
        if (success) {
            IssueMO downloadedIssue = null;
            try {
                downloadedIssue = mHost.getIssueService().getIssue(mIssue.getDOI());
            } catch (ElementNotFoundException e) {
                Logger.d(TAG, e.getMessage(), e);
            }
            if (downloadedIssue != null) {
                mIssue = downloadedIssue;
                updateStateViews(mIssue);
            }
        }
        updateSavedArticlesCountView(mIssue);
    }

    public void onIssueImportingStarted() {
        startProgress();
        mDownloadHandler.onDownloadStopped();
    }

    public void onDownloadProgress(float progress, float total) {
        mDownloadHandler.onProgress(progress, total);
        if (mSavedArticlesCountView != null) {
            mSavedArticlesCountView.setVisibility(View.GONE);
        }
    }

    public void onIssueRemoved() {
        stopProgress();
        mIssue.setIsLocal(false);
        setDownloadButtonEnabled(true);
        updateStateViews(mIssue.getDOI());
    }

    public void setShowCover(boolean showCover) {
        if (mShowCover != showCover) {
            mShowCover = showCover;
            updateView();
        }
    }

    public void setDownloadButtonEnabled(boolean enabled) {
        mDownloadHandler.setShowDownloadButton(enabled && canShowDownloadButton());
        fillBy(mIssue);
    }

    private boolean canShowDownloadButton() {
        boolean isLoading = mHost.getIssueService().isIssueLoading(mIssue.getDOI());
        boolean isUpdating = mHost.getIssueService().isIssueUpdating(mIssue.getDOI());
        return !mIssue.isLocal() && !isLoading && !isUpdating;
    }

    public void setHost(IssueViewHost host) {
        mHost = host;
    }
}
