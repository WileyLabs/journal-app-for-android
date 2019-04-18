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
package com.wiley.android.journalApp.utils;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.log.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by taraskreknin on 15.05.14.
 */
public abstract class IssueDownloadHandler {
    private final static String TAG = IssueDownloadHandler.class.getSimpleName();
    private final String mProgressTextFormat;
    private final String mDownloadButtonTextFormat;
    private final String mDownloadButtonTextSimple;

    // Views
    private View mProgressParentView;
    private ProgressBar mProgress;
    private TextView mState;
    private Button mDownloadButtonView;

    // Ids // TODO add setters
    private int mCancelButtonId = R.id.download_cancel;
    private int mProgressId = R.id.download_progress;
    private int mParentProgressId = R.id.download_progress_parent;
    private int mStateTextId = R.id.download_state_text;
    private int mDownloadButtonId = R.id.download_issue;

    private boolean mShowDownloadButton;
    private boolean mInited = false;

    private static final Map<String, Float> downloadStatesHolder = new HashMap<>();

    private String currentIssueDoi;

    protected abstract void onCancelDownload();
    protected abstract void onDownloadButtonClick();

    public IssueDownloadHandler(Context c) {
        final Context mContext = c.getApplicationContext();
        mProgressTextFormat = mContext.getResources().getString(R.string.download_progress_text_format);
        mDownloadButtonTextFormat = mContext.getResources().getString(R.string.download_issue_format);
        mDownloadButtonTextSimple = mContext.getString(R.string.download_issue);
    }

    public void init(View parent, IssueMO issue) {
        if (parent == null) {
            mInited = false;
            return;
        }

        currentIssueDoi = issue.getDoi();

        mProgressParentView = parent.findViewById(mParentProgressId);
        mProgress = (ProgressBar) parent.findViewById(mProgressId);
        if (mProgress == null) {
            mInited = false;
            return;
        }

        mProgress.setMax(100);

        final View cancel = parent.findViewById(mCancelButtonId);
        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCancelDownload();
                }
            });
        }

        mState = (TextView) parent.findViewById(mStateTextId);

        mDownloadButtonView = (Button) parent.findViewById(mDownloadButtonId);
        if (mDownloadButtonView != null) {
            mDownloadButtonView.setEnabled(true);
            mDownloadButtonView.setText(getDownloadButtonText(issue));
            mDownloadButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDownloadButtonClick();
                }
            });
            mDownloadButtonView.setVisibility(mShowDownloadButton && !issue.isLocal() ? View.VISIBLE : View.GONE);
        }

        if (issue.isLocal()) {
            hideAllViews();
        }

        mInited = true;
    }

    private String getDownloadButtonText(IssueMO issue) {
        // TODO check 0.xx size
        float downloadSize;

        try {
            downloadSize = Float.parseFloat(issue.getDownloadSize());
        } catch (NumberFormatException ex) {
            Logger.d(TAG, ex.getMessage());
            return mDownloadButtonTextSimple;
        }

        if (downloadSize >= 10) {
            return String.format(mDownloadButtonTextFormat, String.format("%d", Math.round(downloadSize)));
        } else if (downloadSize >= 1) {
            return String.format(mDownloadButtonTextFormat, String.format("%.1f", downloadSize));
        } else {
            return String.format(mDownloadButtonTextFormat, String.format("%.2f", downloadSize));
        }
    }

    public void onProgress(float current, float total) {
        if (!mInited) {
            return;
        }
        if (mProgressParentView != null) {
            mProgressParentView.setVisibility(View.VISIBLE);
        }

        downloadStatesHolder.put(currentIssueDoi, current);
        mState.setText(String.format(mProgressTextFormat, current, total));
        int progress = (int) (current / total * 100);
        mProgress.setProgress(progress);
    }

    public void onDownloadStarted(String total) {
        float issueSize = Float.parseFloat(total);
        float downloadState = downloadStatesHolder.containsKey(currentIssueDoi) ?
                downloadStatesHolder.get(currentIssueDoi) : 0.0F;
        if (mState != null) {
            mState.setText(String.format(mProgressTextFormat, downloadState, issueSize));
        }
        if (mProgress != null) {
            int progress = (int) (downloadState / issueSize * 100);
            mProgress.setProgress(progress);
        }
        if (mProgressParentView != null) {
            mProgressParentView.setVisibility(View.VISIBLE);
        }
        if (mDownloadButtonView != null) {
            mDownloadButtonView.setVisibility(View.GONE);
        }
    }

    public void onDownloadStopped() {
        downloadStatesHolder.remove(currentIssueDoi);
        if (mProgressParentView != null) {
            mProgressParentView.setVisibility(View.GONE);
        }
    }

    public void hideAllViews() {
        if (mProgressParentView != null) {
            mProgressParentView.setVisibility(View.GONE);
        }
        if (mDownloadButtonView != null) {
            mDownloadButtonView.setVisibility(View.GONE);
        }
    }

    public void setShowDownloadButton(boolean showDownloadButton) {
        mShowDownloadButton = showDownloadButton;
    }

    public boolean isProgressShowing() {
        return mProgressParentView != null && mProgressParentView.getVisibility() == View.VISIBLE;
    }

    public boolean isDownloadButtonShowing() {
        return mDownloadButtonView != null && mDownloadButtonView.getVisibility() == View.VISIBLE;
    }
}
