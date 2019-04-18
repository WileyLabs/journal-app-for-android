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
package com.wiley.android.journalApp.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.inject.Inject;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;
import com.joanzapata.pdfview.listener.OnPageChangeListener;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.ActivityWithActionBar;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.log.Logger;

import java.io.File;

import static com.wiley.wol.client.android.error.AppErrorCode.FAIL_TO_GET_DOCUMENT;

/**
 * Created by taraskreknin on 15.07.14.
 */
public class PdfViewActivity extends ActivityWithActionBar implements OnLoadCompleteListener, OnPageChangeListener {

    public static Intent getStartingIntent(Activity from, String path) {
        Intent intent = new Intent(from, PdfViewActivity.class);
        intent.putExtra(EXTRA_PATH_TO_PDF, path);
        return intent;
    }

    private static final String TAG = PdfViewActivity.class.getSimpleName();
    private static final String EXTRA_CURRENT_PAGE = "com.wiley.android.journalApp.activity.PdfViewActivity_current_page";
    private static final String EXTRA_PATH_TO_PDF = "com.wiley.android.journalApp.activity.PdfViewActivity_path_to_pdf";

    @Inject
    private ErrorManager mErrorManager;

    private boolean mDisplayed;
    private String mPathToPdf;
    private PDFView mPdfView;
    private ProgressBar mProgressView;
    private TextView mPagesIndicatorView;
    private int mCurrentPage = 1;

    private final Animation mFadeInAnimation = new AlphaAnimation(0, 1);
    private final Animation mFadeOutAnimation = new AlphaAnimation(1, 0);

    private Animation.AnimationListener mFadeInAnimListener = new SimpleAnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mPagesIndicatorView.setVisibility(View.VISIBLE);
        }
    };
    private Animation.AnimationListener mFadeOutAnimListener = new SimpleAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mPagesIndicatorView.setVisibility(View.GONE);
        }
    };
    private final Runnable mHidePagesIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            hidePagesIndicatorAnimated();
        }
    };

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_pdf_view);
        setTitle("");
        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(EXTRA_CURRENT_PAGE, 1);
        }
        initUi();
        processIntent();
        display();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pdf_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.pdf_open_external) {
            openPdfExternal();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mDisplayed) {
            onDisplayError();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState == null) {
            outState = new Bundle();
        }
        outState.putInt(EXTRA_CURRENT_PAGE, mCurrentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(EXTRA_CURRENT_PAGE, 1);
        }
    }

    private void openPdfExternal() {
        Intent baseIntent = new Intent(Intent.ACTION_VIEW);
        baseIntent.setDataAndType(Uri.fromFile(new File(mPathToPdf)), "application/pdf");
        baseIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            startActivity(baseIntent);
        } catch (ActivityNotFoundException e) {
            UIUtils.showShortToast(this, getString(R.string.no_pdf_applications));
        }
    }

    private void initUi() {
        mPdfView = findView(R.id.pdf_view);
        mProgressView = findView(R.id.pdf_view_progress);
        mPagesIndicatorView = findView(R.id.pdf_view_pages_count);

        mFadeInAnimation.setInterpolator(new AccelerateInterpolator());
        mFadeInAnimation.setDuration(300);
        mFadeInAnimation.setAnimationListener(mFadeInAnimListener);
        mFadeOutAnimation.setInterpolator(new DecelerateInterpolator());
        mFadeOutAnimation.setDuration(200);
        mFadeOutAnimation.setAnimationListener(mFadeOutAnimListener);
    }

    private void display() {
        mProgressView.setVisibility(View.VISIBLE);
        displaySafe();
    }

    private void displaySafe() {
        try {
            mPdfView.fromFile(new File(mPathToPdf))
                    .defaultPage(mCurrentPage)
                    .showMinimap(false)
                    .enableSwipe(true)
                    .onLoad(this)
                    .onPageChange(this)
                    .load();
            mDisplayed = true;
        } catch (Exception e) {
            Logger.s(TAG, e);
            mDisplayed = false;
        }
        if (!mDisplayed) {
            onDisplayError();
        }
    }

    private void onDisplayError() {
        mErrorManager.alertWithErrorCode(this, FAIL_TO_GET_DOCUMENT);
        mProgressView.setVisibility(View.GONE);
    }

    private void processIntent() {
        final Bundle extras = getIntent().getExtras();
        if (extras == null) return;
        mPathToPdf = extras.getString(EXTRA_PATH_TO_PDF);
    }

    @Override
    public void loadComplete(int nbPages) {
        Logger.d(TAG, "load complete, pages " + nbPages);
        mProgressView.setVisibility(View.GONE);
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        Logger.d(TAG, "page changed, page " + page + " of " + pageCount);
        mCurrentPage = page;
        final String pagesText = String.format("%d of %d", page, pageCount);
        mPagesIndicatorView.setText(pagesText);
        showPagesIndicatorAnimated();
    }

    private void showPagesIndicatorAnimated() {
        if (!mDisplayed) return;
        if (mPagesIndicatorView.getVisibility() != View.VISIBLE) {
            mPagesIndicatorView.startAnimation(mFadeInAnimation);
        }
        mPagesIndicatorView.removeCallbacks(mHidePagesIndicatorRunnable);
        mPagesIndicatorView.postDelayed(mHidePagesIndicatorRunnable, 2000);
    }

    private void hidePagesIndicatorAnimated() {
        if (!mDisplayed) return;
        if (mPagesIndicatorView.getVisibility() != View.GONE) {
            mPagesIndicatorView.startAnimation(mFadeOutAnimation);
        }
    }

    @Override
    protected void onHome() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (mDisplayed && mPdfView.getZoom() > 1.0f) {
            mPdfView.resetZoomWithAnimation();
        } else {
            super.onBackPressed();
        }
    }

    private class SimpleAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {}
        @Override
        public void onAnimationEnd(Animation animation) {}
        @Override
        public void onAnimationRepeat(Animation animation) {}
    }
}