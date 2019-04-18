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

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.settings.Settings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import roboguice.RoboGuice;

/**
 * Created by taraskreknin on 27.08.14.
 */
public class AdViewController {

    private static final String TAG = AdViewController.class.getSimpleName() + ".Advertisement";
    public static final long THIRTY_DAYS = 60 * 60 * 1000 * 24 * 30L;

    private Activity mActivity;
    private Context context;
    private ViewGroup adContainerView;
    private boolean isPhone;
    private String adUnitId;

    private boolean needLoadNext = true;
    private boolean loaded;

    private PublisherAdView adView;
    private AdListener adListener;
    private String developerText;
    private FrameLayout frameLayout;

    private ValueAnimator animator;

    @Inject
    private Authorizer mAuthoriser;
    @Inject
    private Settings settings;
    @Inject
    private ArticleService articleService;
    @Inject
    private IssueService issueService;
    @Inject
    private SpecialSectionService specialSectionService;
    @Inject
    private Theme mTheme;

    public AdViewController(JournalActivity activity, String adUnitId) {
        init(activity, adUnitId);
    }

    public AdViewController(JournalActivity activity, String adUnitId, AdListener adListener) {
        this.adListener = adListener;
        init(activity, adUnitId);
    }

    private void init(JournalActivity activity, String adUnitId) {
        Logger.d(TAG, "AdViewController(): hashCode = " + this.hashCode());

        this.mActivity = activity;
        this.context = activity.getApplicationContext();
        this.isPhone = DeviceUtils.isPhone(context);
        this.adUnitId = adUnitId;
        this.frameLayout = ((JournalActivity) mActivity).findView(R.id.article_view_ad_layout);

        RoboGuice.getInjector(this.context).injectMembers(this);
    }

    private AdSize[] getAdSizes() {
        final ArrayList<AdSize> validSizes = new ArrayList<>();

        if (isPhone) {
            // element[0] should be max size
            validSizes.add(new AdSize(320, 480));
            validSizes.add(new AdSize(300, 250));
            validSizes.add(new AdSize(180, 150));
        } else {
            // element[0] should be max size
            if (DeviceUtils.isPortrait(context)) {
                validSizes.add(new AdSize(480, 640));
                validSizes.add(new AdSize(160, 600));
            } else {
                validSizes.add(new AdSize(640, 480));
            }

            validSizes.add(new AdSize(320, 480));
            validSizes.add(new AdSize(300, 250));
        }

        AdSize[] sizes = new AdSize[validSizes.size()];
        validSizes.toArray(sizes);
        return sizes;
    }

    public void loadAd() {
        loadAdForArticle(null, null);
    }

    public void loadAdForArticle(final DOI doi, final String keywords) {
        Logger.d(TAG, "loadAdForArticle with doi " + doi + " will load " + needLoadNext);

        if (!needLoadNext) {
            return;
        }

        needLoadNext = false;
        loaded = false;

        developerText = "";
        Bundle extras = new Bundle();

        if (null != doi) {
            // keywords
            extras.putString("keyword", null == keywords ? "" : keywords);

            // doi
            extras.putString("DOI", (doi.getValue()));

            // recentlyUpgraded
            final String recentlyUpgraded = settings.getAppLastUpgradeDate() + THIRTY_DAYS - new Date().getTime() > 0  ? "YES" : "NO";
            extras.putString("recentlyUpgraded", recentlyUpgraded);

            // anonymousUser
            extras.putString("anonymousUser", !mAuthoriser.isAuthorized() ? "YES" : "NO");

            // noPDFs
            extras.putString("noPDFs", settings.getCountDownloadedArticlePdf() == 0 ? "YES" : "NO");

            // savedArticles
            extras.putString("savedArticles", articleService.getSavedArticleCount() > 0 ? "YES" : "NO");

            // abstractInfo
            extras.putString("abstractInfo", settings.getArticleShowAbstract() ? "YES" : "NO");

            // subscribedKeywords
            List<String> subscribedKeywords = settings.getKeywords();
            // testing keywords
            //subscribedKeywords.add("DNA");
            //subscribedKeywords.add("Palladium");
            //subscribedKeywords.add("Nanoparticles");
            extras.putString("subscribedKeywords", null != subscribedKeywords && subscribedKeywords.size() > 0 ? "YES" : "NO");
            if (null != subscribedKeywords && subscribedKeywords.size() > 0) {
                String[] array = new String[subscribedKeywords.size()];
                subscribedKeywords.toArray(array);
                extras.putStringArray("listOfSubscribedKeywords", array);
            }

            // lessDownloadedIssues
            extras.putString("lessDownloadedIssues", issueService.getDownloadedIssuesCount() <= 2 ? "YES" : "NO");

            // lessReadArticles
            final String lessReadArticles = articleService.getReadArticleCount() <= 2 ? "YES" : "NO";
            extras.putString("lessReadArticles", lessReadArticles);

            // lessOpenedIssues
            final String lessOpenedIssues = issueService.getOpenedIssuesCount() <= 2 ? "YES" : "NO";
            extras.putString("lessOpenedIssues", lessOpenedIssues);

            // lessOpenedSpecialSections
            final String lessOpenedSpecialSections = specialSectionService.getOpenedSpecialSectionsCount() <= 2 ? "YES" : "NO";
            final boolean isAddSpecialSectionsKey = specialSectionService.count() > 2;
            if (isAddSpecialSectionsKey) {
                extras.putString("lessOpenedSpecialSections", lessOpenedSpecialSections);
            }

            final String browseFreeContent = !mAuthoriser.isAuthorized() ? "YES" : "NO";

            // browseFreeContent
            if (!mTheme.isOpenAccessJournal()) {
                extras.putString("browseFreeContent", browseFreeContent);
            }
        }

        // subscriptionCode
        if (settings.hasAccessCode()) {
            final String accessCode = settings.getAccessCode();
            AuthorizationService.AccessCodeInformation information =
                    new AuthorizationService.AccessCodeInformation(accessCode, settings.getAccessCodeExpirationDate());
            if (!information.isExpired()) {
                extras.putString("subscriptionCode", accessCode);
            }
        }
        // testing subscription code
        //extras.putString("subscriptionCode", "jastesttrialocode2");

        StringBuilder sb = new StringBuilder();
        for (String key : extras.keySet()) {
            String value;
            if (key.equals("listOfSubscribedKeywords")) {
                value = "";
                for (String keyword : extras.getStringArray("listOfSubscribedKeywords")) {
                    value = value + keyword + ',';
                }
            } else {
                value = extras.getString(key);
            }

            if (null != value) {
                sb.append(String.format("%s: %s\n", key, value));
            }
        }
        developerText = sb.toString();

        final PublisherAdRequest adRequest = new PublisherAdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        //.addTestDevice(PublisherAdRequest.DEVICE_ID_EMULATOR)
                .addKeyword(keywords)
                .build();

        crateLayout();

        if (DeviceUtils.isGooglePlayServicesAvailable(context)) {
            adView.loadAd(adRequest);
        }
    }

    public void crateLayout() {

        destroyLayout();

        int maxWidthDp = 0;
        int maxWidthPx;
        Point rawDisplaySizePx = UIUtils.getRawDisplaySize(mActivity);
        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
        AdSize adSizes[] = getAdSizes();

        int widthAbsPx, heightAbsPx, widthAdViewPx, heightAdViewPx;

        if (isPhone) {
            widthAbsPx = rawDisplaySizePx.x;
            heightAbsPx = rawDisplaySizePx.y;
            widthAdViewPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, adSizes[0].getWidth(), displayMetrics);
            heightAdViewPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, adSizes[0].getHeight(), displayMetrics);
        } else {
            // define maxWidth
            for (AdSize adSize : adSizes) {
                final int width = adSize.getWidth();
                final int height = adSize.getHeight();
                if (width > height) {
                    if (width > maxWidthDp) {
                        maxWidthDp = width;
                    }
                } else if (height > maxWidthDp) {
                    maxWidthDp = height;
                }
            }
            maxWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxWidthDp, displayMetrics);

            if (maxWidthPx < rawDisplaySizePx.x) {
                maxWidthPx = rawDisplaySizePx.x;
            }
            if (maxWidthPx < rawDisplaySizePx.y) {
                maxWidthPx = rawDisplaySizePx.y;
            }

            widthAbsPx = maxWidthPx;
            heightAbsPx = maxWidthPx;
            widthAdViewPx = maxWidthPx;
            heightAdViewPx = maxWidthPx;

        }

        // define topMargin
        int widthCloseButtonDp = 60;
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthCloseButtonDp, displayMetrics);

        // layout
        FrameLayout.LayoutParams paramsScroll = new FrameLayout.LayoutParams(rawDisplaySizePx.x, rawDisplaySizePx.y);
        frameLayout.setLayoutParams(paramsScroll);
        frameLayout.setBackgroundColor(mActivity.getResources().getColor(android.R.color.black));

        // ad container
        adContainerView = new AbsoluteLayout(mActivity);

        FrameLayout.LayoutParams paramsAbs = new FrameLayout.LayoutParams(widthAbsPx, heightAbsPx);
        paramsAbs.topMargin = topMargin;
        adContainerView.setLayoutParams(paramsAbs);
        adContainerView.setBackgroundColor(mActivity.getResources().getColor(android.R.color.black));

        frameLayout.addView(adContainerView);

        // PublisherAdView
        adView = new PublisherAdView(adContainerView.getContext());
        adView.setAdSizes(adSizes);
        adView.setAdUnitId(adUnitId);
        adView.setAdListener(null != this.adListener ? this.adListener : new CustomAdListener());

        AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(widthAdViewPx, heightAdViewPx, 0, 0);
        adView.setLayoutParams(lp);
        adView.setBackgroundColor(mActivity.getResources().getColor(android.R.color.black));

        adContainerView.addView(adView);

        adView.postDelayed(new Runnable() {
            @Override
            public void run() {
                FrameLayout.LayoutParams lpLayout = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
                FrameLayout.LayoutParams lpContainer = (FrameLayout.LayoutParams) adContainerView.getLayoutParams();
                AbsoluteLayout.LayoutParams lpAdView = (AbsoluteLayout.LayoutParams) adView.getLayoutParams();
                Logger.d(TAG, "crateLayout() : adUnitId = " + adUnitId
                        + "; adContainerView.hashCode = " + adContainerView.hashCode()
                        + "; lpLayout: w = " + lpLayout.width + ", h = " + lpLayout.height
                        + "; lpContainer: w = " + lpContainer.width + ", h = " + lpContainer.height
                        + "; lpAdView: w = " + lpAdView.width + "; h = " + lpAdView.height);
            }
        }, 100);
    }

    public void updateLayout() {
        if (null == frameLayout || null == adContainerView || null == adView) {
            return;
        }

        AdSize adSize = adView.getAdSize();
        if (null == adSize) {
            return;
        }

        if (!isPhone) {
            // layout
            Point rawDisplaySizePx = UIUtils.getDisplaySizePx(mActivity);
            FrameLayout.LayoutParams paramsScroll = new FrameLayout.LayoutParams(rawDisplaySizePx.x, rawDisplaySizePx.y);
            frameLayout.setLayoutParams(paramsScroll);

            // ad container
            DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
            int widthLoadedAdSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, adSize.getWidth(), displayMetrics);
            int heightLoadedAdSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, adSize.getHeight(), displayMetrics);
            if (widthLoadedAdSizePx < rawDisplaySizePx.x) {
                widthLoadedAdSizePx = rawDisplaySizePx.x;
            }
            if (heightLoadedAdSizePx < rawDisplaySizePx.y) {
                heightLoadedAdSizePx = rawDisplaySizePx.y;
            }
            FrameLayout.LayoutParams lpContainer = (FrameLayout.LayoutParams) adContainerView.getLayoutParams();
            lpContainer.width = widthLoadedAdSizePx;
            lpContainer.height = heightLoadedAdSizePx;
            adContainerView.setLayoutParams(lpContainer);

            // PublisherAdView
            AbsoluteLayout.LayoutParams lpAdView = (AbsoluteLayout.LayoutParams) adView.getLayoutParams();
            lpAdView.width = widthLoadedAdSizePx;
            lpAdView.height = heightLoadedAdSizePx;
            adView.setLayoutParams(lpAdView);
        } else {
            AbsoluteLayout.LayoutParams lpAdView = (AbsoluteLayout.LayoutParams) adView.getLayoutParams();
            lpAdView.y = 0;
            adView.setLayoutParams(lpAdView);
        }

        adView.postDelayed(new Runnable() {
            @Override
            public void run() {
                FrameLayout.LayoutParams lpLayout = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
                FrameLayout.LayoutParams lpContainer = (FrameLayout.LayoutParams) adContainerView.getLayoutParams();
                AbsoluteLayout.LayoutParams lpAdView = (AbsoluteLayout.LayoutParams) adView.getLayoutParams();
                Logger.d(TAG, "updateLayout(): lpLayout: w =" + lpLayout.width + ", h = " + lpLayout.height
                        + "; lpContainer: w = " + lpContainer.width + ", h = " + lpContainer.height
                        + "; lpAdView: w = " + lpAdView.width + "; h = " + lpAdView.height);
                Point rawDisplaySizeDp = UIUtils.getRawDisplaySizeDp(mActivity);
                Logger.d(TAG, "updateLayout(): display size dp : w =" + rawDisplaySizeDp.x + ", h = " + rawDisplaySizeDp.y);
                moveAdViewToCenterAnimated();
            }
        }, 100);
    }

    private void destroyLayout() {
        if (null != adView) {
            adView.destroy();
        }
        if (null != adContainerView) {
            adContainerView.removeAllViews();
        }
        if (null != frameLayout) {
            frameLayout.removeAllViews();
        }
    }

    public String getDeveloperText() {
        return developerText;
    }

    public void pause() {
        Logger.d(TAG, "pause");
        adView.pause();
    }

    public void resume() {
        Logger.d(TAG, "resume(): adView.hashCode = " + adView.hashCode());
        adView.resume();
    }

    public void destroy() {
        Logger.d(TAG, "destroy");
        destroyLayout();
    }

    public void onAdShow() {
        this.needLoadNext = true;
    }

    public void moveAdView(int viewPagerWidth, int xOffset, boolean movingAdPage) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            animator = null;
        }
        if (adContainerView.getChildCount() < 2) {
            Point defaultCenterPos = getCenterPosition(viewPagerWidth);
            int realOffset;
            int leftMenusWidth = frameLayout.getMeasuredWidth() - viewPagerWidth;
            if (movingAdPage) {
                realOffset = -xOffset;
            } else {
                realOffset = viewPagerWidth - xOffset + leftMenusWidth;
            }

            int requiredX = defaultCenterPos.x + realOffset + leftMenusWidth;

            updateAdViewXPosition(requiredX);

        } else {
            throw new RuntimeException("adViewController supports only one child view!");
        }
    }

    private void updateAdViewXPosition(int requiredX) {
        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) adView.getLayoutParams();
        lp.x = requiredX;
        adView.setLayoutParams(lp);
    }

    private Point getCenterPosition(int width) {
        Point screenSize = UIUtils.getDisplaySizePx(context);
        int xPos = width / 2 - adView.getMeasuredWidth() / 2;
        int yPos = screenSize.y / 2 - adView.getMeasuredHeight() / 2;
        return new Point(xPos, yPos);
    }

    private Point getDefaultCenterPosition() {
        Point screenSize = UIUtils.getDisplaySizePx(context);
        int xPos = screenSize.x / 2 - adView.getMeasuredWidth() / 2;
        int yPos = screenSize.y;
        return new Point(xPos, yPos);
    }

    public void moveAdViewToCenterAnimated() {
        Point defaultCenterPos = getDefaultCenterPosition();

        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) adView.getLayoutParams();
        int from = lp.x;
        int to = defaultCenterPos.x;

        if (Math.abs(from - to) < 3) {
            return;
        }

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(500);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int val = (Integer) animation.getAnimatedValue();
                updateAdViewXPosition(val);
            }
        });
        animator.start();
    }

    private static final Interpolator interpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public boolean isReadyToShowAd() {
        Logger.d(TAG, "isReadyToShowAd loaded " + loaded + " needLoadNext " + needLoadNext);
        return loaded && !needLoadNext;
    }

    public boolean dispatchMotionEvent(MotionEvent event) {
        return adContainerView.dispatchTouchEvent(event);
    }

    public void onConfigurationChanged(final Configuration newConfig) {
        updateLayout();
    }

    private class CustomAdListener extends AdListener {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            Logger.d(TAG, "onAdClosed");
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            super.onAdFailedToLoad(errorCode);
            Logger.d(TAG, "onAdFailedToLoad " + errorCode);
            needLoadNext = true;
            loaded = false;
        }

        @Override
        public void onAdLeftApplication() {
            super.onAdLeftApplication();
            Logger.d(TAG, "onAdLeftApplication");
        }

        @Override
        public void onAdOpened() {
            super.onAdOpened();
            Logger.d(TAG, "onAdOpened");
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            Logger.d(TAG, "onAdLoaded(): AdSize = " + adView.getAdSize().toString());
            needLoadNext = false;
            loaded = true;
        }
    }

}