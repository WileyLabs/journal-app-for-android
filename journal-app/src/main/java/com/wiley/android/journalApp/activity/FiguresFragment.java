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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.app.Extras;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.fragment.figures.FigureFragment;
import com.wiley.android.journalApp.fragment.figures.FigureImageFragment;
import com.wiley.android.journalApp.fragment.figures.FigureTableFragment;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.ActionBarUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.ArticleHtmlUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FiguresFragment extends JournalFragment implements FigureFragment.Host,
        ActionBarSherlock.OnCreatePanelMenuListener,
        ActionBarSherlock.OnMenuItemSelectedListener,
        ActionBarSherlock.OnPreparePanelListener,
        Journal {
    private static final String TAG = FiguresFragment.class.getSimpleName();
    private static final String State_SelectedFigureId = "selectedFigureId";
    private static final String CONTACT_THE_JOURNAL_REQUEST = "Please contact the journal for Rights and Permission request process.";

    @Inject
    private AANHelper aanHelper;
    @Inject
    private ArticleService articleService;
    @Inject
    protected EmailSender emailSender;

    private ViewPager pager;
    private ImageButton buttonPrev;
    private ImageButton buttonNext;

    private ArticleMO article;
    private List<FigureMO> figures;
    private ActionBarSherlockCompat mSherlock;
    protected final Templates templates = new Templates();

    private FigureMO findFigureById(int id) {
        for (FigureMO figure : figures)
            if (figure.getUid().equals(id)) {
                return figure;
            }
        return null;
    }

    private FigureMO getSelectedFigure() {
        int selectedFigureIndex = pager.getCurrentItem();
        return figures.get(selectedFigureIndex);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.figures, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initContentView(savedInstanceState);
    }

    protected void initContentView(Bundle savedInstanceState) {
        getActivity().getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        final DOI doi = getActivity().getIntent().getParcelableExtra(Extras.EXTRA_ARTICLE_DOI);
        try {
            this.article = articleService.getArticleFromDao(doi);
        } catch (ElementNotFoundException e) {
            Logger.s(TAG, e);
            getActivity().onBackPressed();
            return;
        }
        this.figures = article.getFiguresSorted();

        this.buttonPrev = findView(R.id.button_prev);
        this.buttonPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPrev();
            }
        });
        this.buttonNext = findView(R.id.button_next);
        this.buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNext();
            }
        });

        this.pager = findView(R.id.pager);
        this.pager.setAdapter(new FiguresPagerAdapter());
        this.pager.setOnPageChangeListener(pagerListener);

        // action bar
        mSherlock = ActionBarUtils.initActionBarWithCloseIcon(getJournalActivity(), "", this, theme);

        updateUi();

        int figureIdForSelect;
        if (savedInstanceState != null) {
            figureIdForSelect = savedInstanceState.getInt(State_SelectedFigureId);
        } else {
            figureIdForSelect = getActivity().getIntent().getIntExtra(Extras.EXTRA_FIGURE_ID, 0);
        }

        FigureMO figureForSelect = findFigureById(figureIdForSelect);
        if (figureForSelect != null) {
            pager.setCurrentItem(this.figures.indexOf(figureForSelect));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int selectedFigureId = getSelectedFigure().getUid();
        outState.putInt(State_SelectedFigureId, selectedFigureId);
    }

    @Override
    public FigureMO getFigure(int index) {
        return figures.get(index);
    }

    @Override
    public void openFigureByShortCaption(String shortCaption) {
        for (int i = 0; i < figures.size(); i++) {
            if (figures.get(i).getShortCaption().equals(shortCaption)) {
                pager.setCurrentItem(i, true);
                break;
            }
        }
    }

    @Override
    public void toggleUiFullscreen() {
        if (canFullscreen()) {
            changeFullscreen(!isFullscreen());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DeviceUtils.isPhone(getActivity())) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DeviceUtils.isPhone(getActivity())) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public boolean isFullscreen() {
        return fullscreen;
    }

    private boolean fullscreen = false;

    public boolean canFullscreen() {
        return DeviceUtils.isPhone(this.getActivity());
    }

    public void changeFullscreen(boolean newFullscreen) {
        if (!canFullscreen()) {
            return;
        }
        this.fullscreen = newFullscreen;


        if (newFullscreen) {
            mSherlock.getActionBar().hide();
        } else {
            mSherlock.getActionBar().show();
        }

        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof FigureFragment) {
                ((FigureFragment) fragment).onUiFullscreenChanged(newFullscreen);
            }
        }
    }

    protected void updateUi() {
        boolean hasItems = pager.getAdapter() != null && pager.getAdapter().getCount() > 0;

        CharSequence pageTitle = null;
        if (hasItems) {
            pageTitle = pager.getAdapter().getPageTitle(pager.getCurrentItem());
        }
        mSherlock.setTitleActionBar(pageTitle);
        mSherlock.dispatchInvalidateOptionsMenu();

        buttonPrev.setVisibility(hasItems && pager.getCurrentItem() > 0 ? View.VISIBLE : View.GONE);
        buttonNext.setVisibility(hasItems && pager.getCurrentItem() < pager.getAdapter().getCount() - 1 ? View.VISIBLE : View.GONE);
        {
            aanHelper.trackFigureViewer(article, getSelectedFigure().getTitle());
        }
    }

    protected void openPrev() {
        boolean hasItems = pager.getAdapter() != null && pager.getAdapter().getCount() > 0;
        if (!hasItems) {
            return;
        }

        if (pager.getCurrentItem() == 0) {
            return;
        }

        pager.setCurrentItem(pager.getCurrentItem() - 1, true);
    }

    protected void openNext() {
        boolean hasItems = pager.getAdapter() != null && pager.getAdapter().getCount() > 0;
        if (!hasItems) {
            return;
        }

        if (pager.getCurrentItem() == pager.getAdapter().getCount() - 1) {
            return;
        }

        pager.setCurrentItem(pager.getCurrentItem() + 1, true);
    }

    private class FiguresPagerAdapter extends FragmentPagerAdapter {

        public FiguresPagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            FigureMO figure = figures.get(position);
            if ("figure".equals(figure.getKind())) {
                return FigureImageFragment.newInstance(position);
            } else {
                return FigureTableFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            return figures.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return figures.get(position).getTitle();
        }
    }

    private ViewPager.OnPageChangeListener pagerListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            updateUi();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    protected boolean canSendEmail() {
        return true;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, com.actionbarsherlock.view.Menu menu) {
        Logger.d(TAG, "onCreatePanelMenu()");

        final FigureMO figure = figures.get(pager.getCurrentItem());
        if (canSendEmail()) {
            menu.add(R.string.email)
                    .setIcon(ActionBarUtils.getLargeEnvelopIconResource(theme))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if ("figure".equals(figure.getKind())) {
                                sendEmailImage(figure);
                            } else {
                                sendEmailTable(figure);
                            }
                            return true;
                        }
                    });
        }
        if (!DeviceUtils.isPhone(this.getActivity())) {
            if ("figure".equals(figure.getKind())) {
                menu.add(R.string.figure_save)
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                saveImageToGallery(figure);
                                return true;
                            }
                        });
                menu.add(R.string.figure_send)
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                sendImageToExternal(figure);
                                return true;
                            }
                        });
            }
        }

        return true;
    }

    private void sendEmailTable(final FigureMO figure) {
        {
            aanHelper.trackActionOpenEmailForm("Figure");
        }
        String subject = String.format("Table from %s", HtmlUtils.stripHtml(figure.getArticle().getTitle()));
        String simpleBody = formatSimpleMailBodyTable(figure);
        String richBody = formatRichMailBodyTable(figure);
        String attachment = formatMailAttachmentTable(figure);

        File figureLocalPath = figure.getArticle().getLocalPath(String.format("tables/%s.html", figure.getShortCaption()));
        File attachmentPath = new File(getActivity().getExternalCacheDir(), figureLocalPath.toString());
        try {
            FileUtils.write(attachmentPath, attachment, "UTF-8");
            Uri attachmentUri = Uri.fromFile(attachmentPath);
            emailSender.sendEmailText(getActivity(), subject, richBody, simpleBody, attachmentUri.toString());
        } catch (IOException e) {
            Logger.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        Logger.d(TAG, "onMenuItemSelected()");
        if (item.getItemId() == android.R.id.home) {
            MainActivity mainActivity = (MainActivity) getJournalActivity();
            mainActivity.onBackPressed();
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, com.actionbarsherlock.view.Menu menu) {
        Logger.d(TAG, "onPreparePanel()");
        return true;
    }

    private void sendEmailImage(final FigureMO figure) {
        {
            aanHelper.trackActionOpenEmailForm("Figure");
        }
        final File originalPath = new File(figure.getOriginalLocal());
        File figureLocalPath = figure.getArticle().getLocalPath(figure.getImageRef());
        final File targetPath = new File(getActivity().getExternalCacheDir(), figureLocalPath.toString());
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                targetPath.getParentFile().mkdirs();
                try {
                    FileUtils.copyFile(originalPath, targetPath);
                    return true;
                } catch (IOException e) {
                    Logger.d(TAG, e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result != null && result) {
                    String subject = String.format("Figure from %s", HtmlUtils.stripHtml(figure.getArticle().getTitle()));
                    String simpleBody = formatSimpleMailBodyImage(figure);
                    String richBody = formatRichMailBodyImage(figure);
                    Uri imageUri = Uri.fromFile(targetPath);
                    emailSender.sendEmailText(getActivity(), subject, richBody, simpleBody, imageUri.toString());
                }
            }
        };
        task.execute();
    }

    private String formatSimpleMailBodyImage(final FigureMO figure) {
        String description = figure.getCaption() == null ? "" : figure.getCaption();
        description = HtmlUtils.stripHtml(description);
        String articleWolLink = figure.getArticle().getWolLinkTemplate().replace("{doi}", figure.getArticle().getDOI().getValue());
        String articleTitle = figure.getArticle().getTitle();
        String articleCitation = figure.getArticle().getCitation() == null ? "" : figure.getArticle().getCitation();

        String rightsLink = figure.getArticle().getRightsLinkUrl();
        String articlePermissionsRequestInfo = getSimpleArticlePermissionsRequestInfo(rightsLink);

        String volNumber = getVolNumber(figure);
        String issueNumber = getIssueNumber(figure);

        String numbers = !TextUtils.isEmpty(volNumber) && !TextUtils.isEmpty(issueNumber)
                ? String.format("<br/>Volume %s, Issue %s<br/>", volNumber, issueNumber)
                : "";

        String citations = !TextUtils.isEmpty(articleCitation)
                ? String.format("<br/>Citation<br/>%s<br/>", articleCitation)
                : "";

        return templates.useAssetsTemplate(getActivity(), "email_figure_image_simple")
                .putParam("article_wol_link", articleWolLink)
                .putParam("article_title", articleTitle)
                .putParam("article_citation", articleCitation)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("article_abstract", HtmlUtils.stripHtml(description))
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("numbers", numbers)
                .putParam("citations", citations)
                .putParam("journal_name_placeholder", theme.getJournalName())
                .putParam("journal_doi_placeholder", theme.getJournalDoi())
                .proceed();
    }

    private String formatRichMailBodyImage(final FigureMO figure) {
        String description = figure.getCaption() == null ? "" : figure.getCaption();
        description = ArticleHtmlUtils.disableAllLinksIn(description);
        String articleWolLink = figure.getArticle().getWolLinkTemplate().replace("{doi}", figure.getArticle().getDOI().getValue());
        String articleTitle = figure.getArticle().getTitle();
        String articleCitation = figure.getArticle().getCitation() == null ? "" : figure.getArticle().getCitation();

        String rightsLink = figure.getArticle().getRightsLinkUrl();
        String articlePermissionsRequestInfo = getRichArticlePermissionsRequestInfo(rightsLink);

        String volNumber = getVolNumber(figure);
        String issueNumber = getIssueNumber(figure);

        String numbers = !TextUtils.isEmpty(volNumber) && !TextUtils.isEmpty(issueNumber)
                ? String.format("\nVolume %s, Issue %s\n", volNumber, issueNumber)
                : "";

        String citations = !TextUtils.isEmpty(articleCitation)
                ? String.format("\nCitation\n%s\n", articleCitation)
                : "";

        return templates.useAssetsTemplate(getActivity(), "email_figure_image")
                .putParam("article_wol_link", articleWolLink)
                .putParam("article_title", articleTitle)
                .putParam("article_citation", articleCitation)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("article_abstract", description)
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("numbers", numbers)
                .putParam("citations", citations)
                .putParam("journal_name_placeholder", theme.getJournalName())
                .putParam("journal_doi_placeholder", theme.getJournalDoi())
                .proceed();
    }

    private String formatSimpleMailBodyTable(final FigureMO figure) {
        String articleWolLink = figure.getArticle().getWolLinkTemplate().replace("{doi}", figure.getArticle().getDOI().getValue());
        String articleTitle = figure.getArticle().getTitle();
        String articleCitation = figure.getArticle().getCitation() == null ? "" : figure.getArticle().getCitation();

        String rightsLink = figure.getArticle().getRightsLinkUrl();
        String articlePermissionsRequestInfo = getSimpleArticlePermissionsRequestInfo(rightsLink);

        String volNumber = getVolNumber(figure);
        String issueNumber = getIssueNumber(figure);

        String numbers = !TextUtils.isEmpty(volNumber) && !TextUtils.isEmpty(issueNumber)
                ? String.format("<br/>Volume %s, Issue %s<br/>", volNumber, issueNumber)
                : "";

        String citations = !TextUtils.isEmpty(articleCitation)
                ? String.format("<br/>Citation<br/>%s<br/>", articleCitation)
                : "";

        return templates.useAssetsTemplate(getActivity(), "email_figure_table_simple")
                .putParam("article_wol_link", articleWolLink)
                .putParam("article_title", articleTitle)
                .putParam("article_citation", articleCitation)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("numbers", numbers)
                .putParam("citations", citations)
                .putParam("journal_name_placeholder", theme.getJournalName())
                .putParam("journal_doi_placeholder", theme.getJournalDoi())
                .proceed();
    }

    private String formatRichMailBodyTable(final FigureMO figure) {
        String articleWolLink = figure.getArticle().getWolLinkTemplate().replace("{doi}", figure.getArticle().getDOI().getValue());
        String articleTitle = figure.getArticle().getTitle();
        String articleCitation = figure.getArticle().getCitation() == null ? "" : figure.getArticle().getCitation();

        String rightsLink = figure.getArticle().getRightsLinkUrl();
        String articlePermissionsRequestInfo = getRichArticlePermissionsRequestInfo(rightsLink);

        String volNumber = getVolNumber(figure);
        String issueNumber = getIssueNumber(figure);

        String numbers = !TextUtils.isEmpty(volNumber) && !TextUtils.isEmpty(issueNumber)
                ? String.format("\nVolume %s, Issue %s\n", volNumber, issueNumber)
                : "";

        String citations = !TextUtils.isEmpty(articleCitation)
                ? String.format("\nCitation\n%s\n", articleCitation)
                : "";

        return templates.useAssetsTemplate(getActivity(), "email_figure_table")
                .putParam("article_wol_link", articleWolLink)
                .putParam("article_title", articleTitle)
                .putParam("article_citation", articleCitation)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("numbers", numbers)
                .putParam("citations", citations)
                .putParam("journal_name_placeholder", theme.getJournalName())
                .putParam("journal_doi_placeholder", theme.getJournalDoi())
                .proceed();
    }

    private String formatMailAttachmentTable(final FigureMO figure) {
        String description = figure.getCaption() == null ? "" : figure.getCaption();
        description = ArticleHtmlUtils.disableAllLinksIn(description);
        String articleWolLink = figure.getArticle().getWolLinkTemplate().replace("{doi}", figure.getArticle().getDOI().getValue());
        String articleTitle = figure.getArticle().getTitle();
        String articleCitation = figure.getArticle().getCitation() == null ? "" : figure.getArticle().getCitation();

        String rightsLink = figure.getArticle().getRightsLinkUrl();
        String articlePermissionsRequestInfo = getRichArticlePermissionsRequestInfo(rightsLink);

        String volNumber = getVolNumber(figure);
        String issueNumber = getIssueNumber(figure);

        String dnone = "none";
        String dblock = "block";

        String fs = String.format("%.0f", 14.0f);

        String displayNumbers = (volNumber.length() > 0 && issueNumber.length() > 0) ? dblock : dnone;
        String displayCitation = articleCitation.length() > 0 ? dblock : dnone;

        return templates.useAssetsTemplate(getActivity(), "email_figure_table_attachment")
                .putParam("article_wol_link", articleWolLink)
                .putParam("article_title", articleTitle)
                .putParam("article_citation", articleCitation)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("font_size", fs)
                .putParam("journal_numbers_display", displayNumbers)
                .putParam("article_citation_display", displayCitation)
                .putParam("journal_name_placeholder", theme.getJournalName())
                .putParam("journal_doi_placeholder", theme.getJournalDoi())
                .putParam("article_abstract", description)
                .proceed();
    }

    private String getSimpleArticlePermissionsRequestInfo(final String rightsLink) {
        return !TextUtils.isEmpty(rightsLink)
                ? String.format("Please request Rights and Permissions via Rights Link %s.", rightsLink)
                : CONTACT_THE_JOURNAL_REQUEST;
    }

    private String getRichArticlePermissionsRequestInfo(final String rightsLink) {
        return !TextUtils.isEmpty(rightsLink)
                ? String.format("Please request Rights and Permissions via <a href=\"%s\">Rights Link</a>.", rightsLink)
                : CONTACT_THE_JOURNAL_REQUEST;
    }

    @NonNull
    private String getVolNumber(final FigureMO figure) {
        return (figure.getArticle().getSection() == null) ||
                (figure.getArticle().getSection().getIssue().getVolumeNumber() == null) ?
                "" : figure.getArticle().getSection().getIssue().getVolumeNumber();
    }

    @NonNull
    private String getIssueNumber(final FigureMO figure) {
        return (figure.getArticle().getSection() == null)
                || (figure.getArticle().getSection().getIssue().getIssueNumber() == null) ?
                "" : figure.getArticle().getSection().getIssue().getIssueNumber();
    }

    private void saveImageToGallery(final FigureMO figure) {
        final File originalPath = new File(figure.getOriginalLocal());
        String journalName = theme.getAppPrefix();
        String filename = new File(figure.getImageRef()).getName();
        final File pathInGallery = new File(journalName, filename);
        final File targetPath = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                pathInGallery.toString());
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                targetPath.getParentFile().mkdirs();
                try {
                    FileUtils.copyFile(originalPath, targetPath);
                    return true;
                } catch (IOException e) {
                    Logger.d(TAG, e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result != null && result) {
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(targetPath);
                    mediaScanIntent.setData(contentUri);
                    getActivity().sendBroadcast(mediaScanIntent);
                    UIUtils.showShortToast(getActivity(), getString(R.string.image_was_saved_in_gallery, pathInGallery.toString()));
                }
            }
        };
        task.execute();
    }

    private void sendImageToExternal(final FigureMO figure) {
        final File originalPath = new File(figure.getOriginalLocal());
        File figureLocalPath = figure.getArticle().getLocalPath(figure.getImageRef());
        final File targetPath = new File(getActivity().getExternalCacheDir(), figureLocalPath.toString());
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                targetPath.getParentFile().mkdirs();
                try {
                    FileUtils.copyFile(originalPath, targetPath);
                    return true;
                } catch (IOException e) {
                    Logger.d(TAG, e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result != null && result) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(targetPath));
                    intent.setType("image/*");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        UIUtils.showShortToast(getActivity(), getActivity().getString(R.string.no_image_applications));
                    }
                }
            }
        };
        task.execute();
    }

}
