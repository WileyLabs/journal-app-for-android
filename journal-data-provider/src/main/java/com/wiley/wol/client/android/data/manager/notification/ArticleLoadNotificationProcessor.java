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

import com.wiley.wol.client.android.data.dao.ArticleDao;
import com.wiley.wol.client.android.data.http.Resource;
import com.wiley.wol.client.android.data.manager.ResourceType;
import com.wiley.wol.client.android.data.utils.ArticleHtmlUtils;
import com.wiley.wol.client.android.data.xml.ArticleSimpleParser;
import com.wiley.wol.client.android.data.xml.transformer.StringConverter;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.CitationMO;
import com.wiley.wol.client.android.domain.entity.ReferenceMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.wiley.wol.client.android.data.http.DownloadOperation.DOI;
import static com.wiley.wol.client.android.data.http.DownloadOperation.RESOURCE;
import static com.wiley.wol.client.android.data.manager.ResourceType.ARTICLE_DIR;
import static com.wiley.wol.client.android.data.manager.ResourceType.ARTICLE_ZIP;
import static com.wiley.wol.client.android.data.utils.ZipUtils.unzip;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_UPDATE_ERROR;
import static com.wiley.wol.client.android.notification.EventList.ARTICLE_UPDATE_SUCCESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class ArticleLoadNotificationProcessor implements NotificationProcessor {
    private static final String TAG = ArticleLoadNotificationProcessor.class.getSimpleName();

    private final ArticleSimpleParser articleParser;
    private final ArticleDao articleDao;
    private final NotificationCenter notificationCenter;
    private ExecutorService executor = newSingleThreadExecutor();

    public ArticleLoadNotificationProcessor(final ArticleSimpleParser articleParser,
                                            final ArticleDao articleDao,
                                            final NotificationCenter notificationCenter) {
        this.articleParser = articleParser;
        this.articleDao = articleDao;
        this.notificationCenter = notificationCenter;
    }

    @Override
    public void processNotification(final Map<String, Object> params) {
        final Resource resource = (Resource) params.get(RESOURCE);
        final ResourceType resourceType = resource.getResourceType();
        if (ARTICLE_DIR != resourceType && ARTICLE_ZIP != resourceType) {
            return;
        }

        executor.execute((new Runnable() {
            @Override
            public void run() {
                final String filePathName = resource.getFilePath();
                final DOI doi = (DOI) params.get(DOI);
                try {
                    final String articlePath;
                    if (ARTICLE_ZIP == resourceType) {
                        final String destinationFolderPathName = new File(filePathName).getParent();
                        unzip(filePathName, destinationFolderPathName);
                        articlePath = destinationFolderPathName + "/" + doi.getArticleZipCompatibleValue();
                    } else {
                        articlePath = resource.getFilePath();
                    }

                    final ArticleMO article = articleParser.parse(new FileInputStream(
                            new File(articlePath + "/article.xml")));

                    ArticleHtmlUtils.prepareArticleFiguresAndFullHtmlBody(article, articlePath);

                    final File referenceBatchFile = new File(articlePath + "/referenceBatch.xml");
                    RefBatch refBatch = null;
                    if (referenceBatchFile.exists()) {
                        refBatch = parseSimpleReferenceBatch(new FileInputStream(referenceBatchFile));
                    }
                    initCitations(article, refBatch);


                    try {
                        final ArticleMO storedArticle = articleDao.findOne(article.getDOI());
                        article.setUid(storedArticle.getUid());
                        article.setImportingDate(new Date());
                        article.setRead(storedArticle.isRead());
                        article.setFavorite(storedArticle.isFavorite());
                        article.setAddedToFavoriteDate(storedArticle.getAddedToFavoriteDate());
                        if (article.getFullTextAbstract() == null) {
                            article.setFullTextAbstract(storedArticle.getFullTextAbstract());
                        }
                        article.setEarlyView(storedArticle.isEarlyView());
                        article.setSection(storedArticle.getSection());
                        article.setSpecialSections(storedArticle.getSpecialSections());
                        article.setHasPdf(storedArticle.hasPdf());
                        article.setPdfSizeMb(storedArticle.getPdfSizeMb());
                        article.setFunding(storedArticle.getFunding());
                        article.setThumbnailHeight(storedArticle.getThumbnailHeight());
                        article.setThumbnailWidth(storedArticle.getThumbnailWidth());
                        article.setSummary(storedArticle.getSummary());
                        if (null == article.getFirstOnlineDate()) {
                            final String firstOnLineDate = storedArticle.getFirstOnlineDate();
                            article.setFirstOnlineDate(null == firstOnLineDate || firstOnLineDate.equals("")
                                    ? (new SimpleDateFormat("yyyy-MM-dd")).format(new Date())
                                    : firstOnLineDate);
                        }
                        if (null == article.getPublicationDate()) {
                            final Date publicationDate = storedArticle.getPublicationDate();
                            article.setPublicationDate(null == publicationDate ? new Date() : publicationDate);
                        }
                        if (null == article.getSimpleAuthorList()) {
                            final String authors = storedArticle.getSimpleAuthorList();
                            article.setSimpleAuthorList(null == authors ? "" : authors);
                        }
                    } catch (final ElementNotFoundException ignored) {
                        article.setPublicationDate(new Date());
                        article.setFirstOnlineDate((new SimpleDateFormat("yyyy-MM-dd")).format(new Date()));
                    }
                    articleDao.save(article);
                    notificationCenter.sendNotification(ARTICLE_UPDATE_SUCCESS.getEventName(), params);
                } catch (final Exception e) {
                    Logger.s(TAG, e);
                    params.put(ERROR, e);
                    notificationCenter.sendNotification(ARTICLE_UPDATE_ERROR.getEventName(), params);
                }
            }
        }));
    }

    private void initCitations(final ArticleMO article, final RefBatch refBatch) {
        int referenceSortIndex = 0;
        for (ReferenceMO reference : article.getReferences()) {
            int citationSortIndex = 0;
            for (CitationMO citation : reference.getCitations()) {
                citation.setId(format("%s_citation_%s", article.getDOI(), citation.getCitId()));
                if (refBatch != null) {
                    for (Ref ref : refBatch.getRefs()) {
                        if (citation.getCitId().equals(ref.getId())) {
                            final String wol = ref.getLocalId();
                            citation.setLinkToWOL(getLinkToWOL(article, wol));
                            Map<String, Object> links = new HashMap<>();
                            Collection<SLink> sLinks = ref.getSLink();
                            if (null != sLinks) {
                                for (SLink sLink : sLinks) {
                                    links.put(sLink.getPartner(), sLink.getText());
                                }
                            }
                            Collection<MLink> mLinks = ref.getMLink();
                            if (null != mLinks) {
                                for (MLink mLink : mLinks) {
                                    Map<String, String> mLinksMap = new HashMap<>();
                                    for (AccessId accessId : mLink.getAccessIds()) {
                                        mLinksMap.put(accessId.getType(), accessId.getText());
                                    }
                                    links.put(mLink.getPartner(), mLinksMap);
                                }
                            }
                            citation.setLinksMap(links);
                        }
                    }
                }
                citation.setSortIndex(citationSortIndex);
                citationSortIndex++;
            }
            reference.setSortIndex(referenceSortIndex);
            referenceSortIndex++;
        }
    }

    private String getLinkToWOL(final ArticleMO article, final String wol) {
        final String wolLinkTemplate = article.getWolLinkTemplate();
        return wol != null && wol.length() > 0 &&
                wolLinkTemplate != null ?
                wolLinkTemplate.replaceAll("\\{doi\\}", wol) : null;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Root(name = "refBatch")
    private static class RefBatch {
        @ElementList(name = "refContainer", entry = "ref")
        private Collection<Ref> ref;

        public RefBatch() {

        }

        public Collection<Ref> getRefs() {
            return ref;
        }
    }

    @Root(name = "ref")
    private static class Ref {
        @Attribute
        private String id;

        @Attribute(required = false)
        private String localId;

        @ElementList(entry = "sLink", inline = true, required = false)
        private Collection<SLink> sLink;

        @ElementList(entry = "mLink", inline = true, required = false)
        private Collection<MLink> mLink;

        public String getId() {
            return id;
        }

        public String getLocalId() {
            return localId;
        }

        public Collection<SLink> getSLink() {
            return sLink;
        }

        public Collection<MLink> getMLink() {
            return mLink;
        }
    }

    @Root(name = "sLink")
    private static class SLink {
        @Attribute
        private String partner;

        @Text
        private String text;

        public String getPartner() {
            return partner;
        }

        public String getText() {
            return text;
        }
    }

    @Root(name = "mLink")
    private static class MLink {
        @Attribute
        private String partner;

        @ElementList(inline = true)
        private Collection<AccessId> accessId;

        public String getPartner() {
            return partner;
        }

        public Collection<AccessId> getAccessIds() {
            return accessId;
        }
    }

    @Root(name = "accessId")
    private static class AccessId {
        @Attribute
        private String type;

        @Text
        private String text;

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }

    public RefBatch parseSimpleReferenceBatch(final InputStream inputStream) {
        try {
            final Registry registry = new Registry();
            registry.bind(String.class, StringConverter.class);
            final Strategy strategy = new RegistryStrategy(registry);

            final Serializer serializer = new Persister(strategy);
            return serializer.read(RefBatch.class, inputStream, false);
        } catch (Exception e) {
            Logger.s(TAG, e);
            return null;
        }
    }
}
