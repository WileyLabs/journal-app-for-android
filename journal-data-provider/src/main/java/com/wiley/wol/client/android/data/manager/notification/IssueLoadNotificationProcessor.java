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
package com.wiley.wol.client.android.data.manager.notification;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.dao.IssueDao;
import com.wiley.wol.client.android.data.http.Resource;
import com.wiley.wol.client.android.data.manager.ResourceType;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE;
import static com.wiley.wol.client.android.data.manager.ResourceType.ISSUE_ZIP;
import static com.wiley.wol.client.android.data.utils.ZipUtils.unzip;
import static com.wiley.wol.client.android.notification.EventList.DONE_RESOURCE_DOWNLOADING;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.ISSUE_DOWNLOAD_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.IMPORTING;
import static com.wiley.wol.client.android.settings.Settings.DOWNLOAD_ISSUE;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Created by dfedorov
 * on 25/07/14.
 */
public class IssueLoadNotificationProcessor implements NotificationProcessor {

    private static final String TAG = IssueLoadNotificationProcessor.class.getSimpleName();

    @Inject
    private AANHelper aanHelper;
    @Inject
    @InjectCachePath
    private String rootPath;
    @Inject
    private SimpleParser parser;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private IssueService issueService;
    @Inject
    private IssueDao issueDao;
    private Executor executor = newSingleThreadExecutor();

    @Override
    public void processNotification(final Map<String, Object> params) {
        final Resource resource = (Resource) params.get(RESOURCE);
        final ResourceType resourceType = resource.getResourceType();
        if (ISSUE_ZIP != resourceType) {
            return;
        }

        final DOI doi = (DOI) params.get(DOWNLOAD_ISSUE);
        issueService.setIssueUpdating(doi, true);

        params.put(IMPORTING, true);
        notificationCenter.sendNotification(ISSUE_DOWNLOAD_PROGRESS.getEventName(), params);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final DOI doi = (DOI) params.get(DOWNLOAD_ISSUE);
                final Resource resource = (Resource) params.get(RESOURCE);

                final String unzipDir = rootPath + File.separator + "issues";
                try {
                    unzip(resource.getFilePath(), unzipDir);
                    if (!new File(resource.getFilePath()).delete()) {
                        Logger.s(TAG, format("Unable to delete file %s", resource.getFilePath()));
                    }

                    final IssueMO issue = issueService.getIssue(doi);

                    final String issuePath = unzipDir +
                            File.separator + "issue_" + doi.getValue().replace("/", "%2F");
                    final List<SectionMO> sections = getSections(issuePath + File.separator + "toc.xml");
                    issue.setSections(sections);
                    issue.setIsLocal(true);
                    issueDao.save(issue);

                    updateArticlesFromIssue(issuePath);

                    FileUtils.deleteDirectory(new File(unzipDir));

                    issueService.setIssueUpdating(doi, false);
                    params.put(IssueService.ISSUE_MO, issue);
                    notificationCenter.sendNotification(ISSUE_DOWNLOAD_SUCCESS.getEventName(), params);
                    {
                        aanHelper.trackActionIssueDownload(issue.getDoi());
                    }
                } catch (IOException | ElementNotFoundException e) {
                    Logger.s(TAG, e.getMessage(), e);
                    issueService.setIssueUpdating(doi, false);
                    params.put(ERROR, e);
                    notificationCenter.sendNotification(ISSUE_DOWNLOAD_ERROR.getEventName(), params);
                }
            }
        });
    }

    private void updateArticlesFromIssue(String issuePath) throws IOException {
        final File issueDir = new File(issuePath);
        final File[] listFiles = issueDir.listFiles();
        if (listFiles != null) {
            for (File articleDir : listFiles) {
                if (articleDir.isDirectory()) {
                    String dstArticlePath = rootPath + File.separator + articleDir.getName();
                    moveDirectory(articleDir, new File(dstArticlePath));
                    updateArticle(dstArticlePath);
                }
            }
        }
    }

    private List<SectionMO> getSections(final String tocXmlPath) {
        try {
            final SectionsContainer sectionsContainer = parser.parse(new FileInputStream(
                    new File(tocXmlPath)), SectionsContainer.class);

            final List<SectionMO> sections = new ArrayList<>(sectionsContainer.sections.size());
            for (Section section : sectionsContainer.sections) {
                final SectionMO sectionMO = new SectionMO();
                sectionMO.setName(section.name);
                setArticles(sectionMO, section);
                sections.add(sectionMO);
            }

            return sections;
        } catch (Exception e) {
            Logger.s(TAG, e.getMessage(), e);
        }

        return emptyList();
    }

    private void setArticles(final SectionMO sectionMO, final Section section) {
        final List<ArticleMO> articles = new ArrayList<>(section.articles.size());
        for (Article article : section.articles) {
            final ArticleMO articleMO = new ArticleMO();
            articleMO.setDoi(article.doi);
            articleMO.setTitle(article.title);
            articleMO.setTocHeading2(article.tocHeading2);
            articleMO.setTocHeading3(article.tocHeading3);
            articles.add(articleMO);
        }
        sectionMO.setArticles(articles);
    }

    private void updateArticle(final String dstArticlePath) {
        final Resource articleResource = new Resource();
        articleResource.setResourceType(ResourceType.ARTICLE_DIR);
        articleResource.setFilePath(dstArticlePath);
        final HashMap<String, Object> articleParams = new HashMap<>();
        articleParams.put(RESOURCE, articleResource);
        notificationCenter.sendNotification(DONE_RESOURCE_DOWNLOADING.getEventName(), articleParams);
    }

    private void moveDirectory(File srcDir, File dstDir) throws IOException {
        FileUtils.copyDirectory(srcDir, dstDir);
        FileUtils.deleteDirectory(srcDir);
    }

    @Root(name = "toc")
    private static class SectionsContainer {
        @ElementList(inline = true)
        private Collection<Section> sections;
    }

    @Root(name = "level1Heading")
    private static class Section {
        @Attribute(name = "heading")
        private String name;
        @ElementList(inline = true)
        private Collection<Article> articles;
    }

    @Root(name = "article")
    private static class Article {
        @Attribute
        private String doi;
        @Attribute
        private String title;
        @Attribute(name = "level2Heading")
        private String tocHeading2;
        @Attribute(name = "level3Heading")
        private String tocHeading3;
    }
}
