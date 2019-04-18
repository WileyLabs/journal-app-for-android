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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;

import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.html.TemplateEngineIosWithComments;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.settings.Environment;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class EmailSender {
    private static final String TAG = EmailSender.class.getSimpleName();
    private static final String BLOCK = "block";
    private static final String NONE = "none";
    private static final String EXTRA_BODY_SIMPLE = "com.wiley.android.journalApp.utils.EmailSender_extraBodySimple";

    protected final Templates mTemplates = new Templates();
    protected static final TemplateEngineIosWithComments sTemplateEngine = new TemplateEngineIosWithComments();
    protected static final HtmlUtils.TagHandlerListSupport sTagHandler = new HtmlUtils.TagHandlerListSupport();

    @Inject
    private Context mContext;
    @Inject
    private Theme mTheme;
    @Inject
    private Environment mEnvironment;
    @Inject
    private ArticleService mArticleService;

    public static void sendIntent(Activity activity, Intent intent) {
        activity.startActivity(Intent.createChooser(intent, "Send email:"));
    }

    public void sendFeedBack(Activity from) {
        final String deviceInfo = mEnvironment.getDeviceName();
        final String androidVersion = mEnvironment.getAndroidVersion();
        final String journalName = mTheme.getJournalName();
        final String appVersion = mEnvironment.getAppVersion();

        String body = String.format("<br /><br />Device: %s<br />Android %s<br />App Name: %s<br />App version: %s", deviceInfo, androidVersion, journalName, appVersion);

        EmailSender.Builder builder = new EmailSender.Builder();
        Intent intent = builder
                .setBodyHtml(body)
                .setSubject(mTheme.getAppFeedBackSubject())
                .addRecipient(mTheme.getAppFeedBackEmail())
                .build();
        sendIntent(from, intent);
    }

    public void sendArticle(DOI doi, final Activity fromActivity) {
        final ArticleMO article;
        try {
            article = mArticleService.getArticleFromDao(doi);
        } catch (ElementNotFoundException ignored) {
            return;
        }

        if (TextUtils.isEmpty(article.getThumbnailLocal())) {
            doSendArticle(fromActivity, article, null);
            return;
        }

        final File externalThumbnailFile = FileUtils.joinPathToFile(
                fromActivity.getExternalCacheDir().getAbsolutePath(),
                article.getDOI().getArticleZipCompatibleValue(),
                article.getAbstractImageHref());
        if (externalThumbnailFile.exists() && externalThumbnailFile.isFile()) {
            doSendArticle(fromActivity, article, Uri.fromFile(externalThumbnailFile));
            return;
        }

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                externalThumbnailFile.getParentFile().mkdirs();
                File internalThumbnailFile = new File(URI.create(article.getThumbnailLocal()));
                try {
                    org.apache.commons.io.FileUtils.copyFile(internalThumbnailFile, externalThumbnailFile);
                } catch (IOException e) {
                    Logger.s(TAG, e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (fromActivity.isFinishing())
                    return;
                Uri uri = externalThumbnailFile.exists() ? Uri.fromFile(externalThumbnailFile) : null;
                doSendArticle(fromActivity, article, uri);
            }
        };
        task.execute();
    }

    private void doSendArticle(Activity fromActivity, ArticleMO article, Uri thumbnailUri) {
        boolean isArticleValid = article.isLocal() || (!article.isRestricted() && !article.isExpired());
        String arAbstract = isArticleValid ? article.getFullTextAbstract() : article.getSummary();
        String abstractText = !TextUtils.isEmpty(arAbstract) ? arAbstract : "No Abstract Available";

        String atitle = HtmlUtils.stripHtml(article.getTitle());
        String pdate = !TextUtils.isEmpty(article.getFirstOnlineDate()) ? article.getFirstOnlineDate() : "";
        String authorsTitle = article.isOneAuthor() ? "Author" : "Authors";
        String authors = article.getSimpleAuthorList();
        String doiStr = article.getDOI().getValue();
        String volNumber = getIssueValueFromArticle(article, 1);
        String issueNumber = getIssueValueFromArticle(article, 0);
        String wolLink = article.getWolLinkTemplate().replace("{doi}", doiStr);

        String articlePermissionsRequestInfo = "Please contact the journal for Rights and Permission request process.";

        String dnone = "none";
        String dblock = "block";

        String displayAuthors = blockOrNone(authors);
        String displayPDate = blockOrNone(pdate);
        String displayDoi = blockOrNone(doiStr);
        String displayNumbers = dblock.equals(blockOrNone(volNumber)) && dblock.equals(blockOrNone(issueNumber)) ? dblock : dnone;

        abstractText = abstractText.replaceAll("<h3>Abstract</h3>", "");

        final Template emailTemplate = mTemplates.useAssetsTemplate(mContext, "email_article");
        final String emailBody = emailTemplate.putParam("authors_title", authorsTitle)
                .putParam("article_publishing_date", pdate)
                .putParam("article_authors", authors)
                .putParam("article_title", atitle)
                .putParam("article_doi", doiStr)
                .putParam("article_abstract", abstractText)
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("article_wol_link", wolLink)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("journal_name_placeholder", mTheme.getJournalName())
                .putParam("journal_doi_placeholder", mTheme.getJournalDoi())
                .putParam("NUMBERS_COMMENT", displayNumbers)
                .putParam("PDATE_COMMENT", displayPDate)
                .putParam("AUTHORS_COMMENT", displayAuthors)
                .putParam("ABSTRACT_COMMENT", dblock)
                .putParam("DOI_COMMENT", displayDoi)
                .useEngine(sTemplateEngine)
                .proceed();

        final Template emailTemplateSimple = mTemplates.useAssetsTemplate(mContext, "email_article_simple");
        final String emailBodySimple = emailTemplateSimple.putParam("authors_title", authorsTitle)
                .putParam("article_publishing_date", pdate)
                .putParam("article_authors", authors)
                .putParam("article_title", atitle)
                .putParam("article_doi", doiStr)
                .putParam("article_abstract", abstractText)
                .putParam("article_volume_number", volNumber)
                .putParam("article_issue_number", issueNumber)
                .putParam("article_wol_link", wolLink)
                .putParam("article_permissions_request_info", articlePermissionsRequestInfo)
                .putParam("journal_name_placeholder", mTheme.getJournalName())
                .putParam("journal_doi_placeholder", mTheme.getJournalDoi())
                .putParam("NUMBERS_COMMENT", displayNumbers)
                .putParam("PDATE_COMMENT", displayPDate)
                .putParam("AUTHORS_COMMENT", displayAuthors)
                .putParam("ABSTRACT_COMMENT", dblock)
                .putParam("DOI_COMMENT", displayDoi)
                .useEngine(sTemplateEngine)
                .proceed();

        Builder builder = new Builder();
        Intent intent = builder.setSubject(atitle)
                .setBodyHtml(emailBody)
                .setSimpleBody(emailBodySimple)
                .attachImage(thumbnailUri)
                .build();

        sendEmailIntent(intent, fromActivity);
    }

    private String getIssueValueFromArticle(ArticleMO article, int which) {
        if (article.getSection() == null || article.getSection().getIssue() == null) {
            return "";
        }
        if (which == 0) {
            return article.getSection().getIssue().getIssueNumber();
        } else if (which == 1) {
            return article.getSection().getIssue().getVolumeNumber();
        } else {
            return "";
        }
    }

    private String blockOrNone(final String value) {
        return !TextUtils.isEmpty(value) ? BLOCK : NONE;
    }

    public void emailCitation(ArticleMO article, Activity fromActivity) {
        String subject = String.format("Citation: %s", HtmlUtils.stripHtml(article.getTitle()));
        String body = String.format("Citation for <a href='%s'>%s</a><br /><br />%s",
                article.getWolLinkTemplate().replace("{doi}",
                        article.getDOI().getValue()),
                article.getTitle(),
                article.getCitation());

        EmailSender.Builder builder = new EmailSender.Builder();
        Intent emailIntent = builder
                .setSubject(subject)
                .setBodyHtml(body)
                .build();

        sendIntent(fromActivity, emailIntent);
    }

    public void sendEmailText(Activity fromActivity,
                              String subject, String richText, String simpleText, String attachmentPath) {
        Builder builder = new Builder();
        Intent intent = builder.setSubject(subject)
                .setBodyHtml(richText)
                .setSimpleBody(simpleText)
                .attachImage(attachmentPath)
                .build();
        sendEmailIntent(intent, fromActivity);
    }

    public void sendEmailText(Activity fromActivity,
                              String subject, String richText, String simpleText, String attachmentPath, String email) {
        Builder builder = new Builder();
        Intent intent = builder
                .setSubject(subject)
                .setBodyHtml(richText)
                .setSimpleBody(simpleText)
                .attachImage(attachmentPath)
                .addRecipient(email)
                .build();
        sendEmailIntent(intent, fromActivity);
    }

    private static void sendEmailIntent(Intent baseIntent, Activity fromActivity) {

        PackageManager packageManager = fromActivity.getPackageManager();
        baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(baseIntent, 0);
        List<Intent> captureIntents = getCaptureIntents(baseIntent, resolveInfos);

        startChooserIntent(baseIntent, fromActivity, captureIntents, "Send email:");

    }

    private static void startChooserIntent(Intent baseIntent, Activity fromActivity, List<Intent> captureIntents, String title) {
        Intent intent = captureIntents.size() > 0 ? (captureIntents.remove(captureIntents.size() - 1)) : baseIntent;
        Intent chooserIntent = Intent.createChooser(intent, title);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, captureIntents.toArray(new Parcelable[captureIntents.size()]));

        fromActivity.startActivity(chooserIntent);
    }

    private static List<Intent> getCaptureIntents(Intent baseIntent, List<ResolveInfo> resolveInfos) {
        return getCaptureIntents(baseIntent, resolveInfos, null);
    }

    private static List<Intent> getCaptureIntents(Intent baseIntent, List<ResolveInfo> resolveInfos, String category) {
        List<Intent> captureIntents = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent intent = new Intent(baseIntent);
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            intent.setPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (category != null) {
                intent.addCategory(category);
            }
            if ("com.google.android.gm".equals(resolveInfo.activityInfo.packageName)) {
                captureIntents.add(0, intent);
            } else {
                String simpleBody = intent.getStringExtra(EXTRA_BODY_SIMPLE);
                if (simpleBody != null) {
                    intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(simpleBody, null, sTagHandler));
                }
                if ("com.google.android.email".equals(resolveInfo.activityInfo.packageName)) {
                    int index = captureIntents.size() > 0 ? 1 : 0;
                    captureIntents.add(index, intent);
                } else {
                    captureIntents.add(intent);
                }
            }
        }
        return captureIntents;
    }

    public static class Builder {
        private final Intent mEmailIntent;
        private final List<String> mRecipients = new ArrayList<>();

        public Builder() {
            mEmailIntent = new Intent(Intent.ACTION_SEND);
            mEmailIntent.setType("message/rfc822");
        }

        public Builder setSubject(String subject) {
            mEmailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            return this;
        }

        public Builder setBodyHtml(String body) {
            return setBody(Html.fromHtml(body, null, sTagHandler));
        }

        public Builder setBody(CharSequence body) {
            mEmailIntent.putExtra(Intent.EXTRA_TEXT, body);
            return this;
        }

        public Builder setSimpleBody(String body) {
            mEmailIntent.putExtra(EXTRA_BODY_SIMPLE, body);
            return  this;
        }

        public Builder attachImage(Uri uri) {
            if (uri == null) {
                return this;
            }
            mEmailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            return this;
        }

        public Builder attachImage(String path) {
            if (TextUtils.isEmpty(path)) {
                return this;
            }
            mEmailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
            return this;
        }

        public Builder addRecipient(String email) {
            mRecipients.add(email);
            return this;
        }

        public Intent build() {
            int count = mRecipients.size();
            if (count > 0) {
                String[] recipients = new String[count];
                recipients = mRecipients.toArray(recipients);
                mEmailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
            }
            return mEmailIntent;
        }

    }
}
