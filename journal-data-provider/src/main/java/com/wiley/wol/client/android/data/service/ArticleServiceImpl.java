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
package com.wiley.wol.client.android.data.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.wiley.wol.client.android.data.dao.ArticleDao;
import com.wiley.wol.client.android.data.dao.IssueDao;
import com.wiley.wol.client.android.data.dao.filter.FilterFactory;
import com.wiley.wol.client.android.data.http.DownloadOperation;
import com.wiley.wol.client.android.data.http.ResourceManager;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.manager.ResourceType;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.AppErrorUtils;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.data.xml.ArticleRefSimpleParser;
import com.wiley.wol.client.android.data.xml.ArticleSimpleParser;
import com.wiley.wol.client.android.data.xml.loader.EarlyViewFeedLoader;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.AbstractArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.utils.NetUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ARTICLE;
import static com.wiley.wol.client.android.error.AppErrorCode.NO_CONNECTION_AVAILABLE;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class ArticleServiceImpl implements ArticleService {

    private static final String TAG = ArticleServiceImpl.class.getSimpleName();

    @Inject
    private AANHelper aanHelper;
    @Inject
    ArticleSimpleParser parser;
    @Inject
    private ArticleDao articleDao;
    @Inject
    private IssueDao issueDao;
    @Inject
    private EarlyViewFeedLoader earlyViewFeedLoader;
    @Inject
    private ArticleRefSimpleParser articleRefSimpleParser;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private FilterFactory filterFactory;
    @Inject
    private ImportManager importManager;
    @Inject
    private Settings settings;
    @Inject
    @InjectCachePath
    private String cachePath;
    @Inject
    private Context context;
    private final Dao<SpecialSectionMO, String> specialSectionDao;

    private NotificationCenter notificationCenter;

    private final Map<DOI, ArticleFavStateChangeTask> favStateChangeTaskMap = new HashMap<>();
    private final Set<DOI> updatingArticleRefsSet = new HashSet<>();

    @Inject
    public ArticleServiceImpl(final NotificationCenter notificationCenter, final OrmLiteSqliteOpenHelper helper) {
        NotificationProcessor articleUpdateSuccessProcessor = new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                processArticleUpdateResult(params, true);
            }
        };
        NotificationProcessor articleUpdateErrorProcessor = new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                processArticleUpdateResult(params, false);
            }
        };
        NotificationProcessor articleZipDownloadFailedProcessor = new NotificationProcessor() {
            @Override
            public void processNotification(Map<String, Object> params) {
                final ResourceType resType = (ResourceType) params.get(DownloadOperation.RESOURCE_TYPE);
                if (ResourceType.ARTICLE_ZIP == resType) {
                    processArticleUpdateResult(params, false);
                }
            }
        };
        this.notificationCenter = notificationCenter;
        this.notificationCenter.subscribeToNotification(EventList.ARTICLE_UPDATE_SUCCESS.getEventName(), articleUpdateSuccessProcessor);
        this.notificationCenter.subscribeToNotification(EventList.ARTICLE_UPDATE_ERROR.getEventName(), articleUpdateErrorProcessor);
        this.notificationCenter.subscribeToNotification(EventList.ERROR_RESOURCE_DOWNLOADING.getEventName(), articleZipDownloadFailedProcessor);

        specialSectionDao = new DaoProvider<>(helper, SpecialSectionMO.class, String.class).get();
    }

    @Override
    public ArticleMO getArticle(final DOI doi) {
        final ArticleMO article;
        try {
            article = articleDao.findOne(doi);
        } catch (final ElementNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (!isDownloaded(doi) && !article.isRestricted()) {
            importManager.loadArticle(doi);
        }


        return article;
    }

    @Override
    public ArticleMO getArticleQuietly(DOI doi) {
        return articleDao.findOneQuietly(doi);
    }

    private void onArticleRefFavoriteStateChanged(final ArticleMO articleRef, final AppErrorCode error) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ARTICLE_MO, articleRef);
        params.put("error", error);
        notificationCenter.sendNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.name(), params);
    }

    @Override
    public List<ArticleMO> getArticlesForEarlyView() {
        Logger.d(TAG, "getArticlesForEarlyView");

        final List<ArticleMO> articles;
        try {
            articles = articleDao.find(filterFactory
                    .getFor(ArticleMO.class)
                    .where()
                    .eq(ArticleMO.IS_EARLY_VIEW, true)
                    .orderBy(ArticleMO.PUBLICATION_DATE, false));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        return articles;
    }

    @Override
    public boolean hasArticlesForEarlyView() {
        try {
            long count = articleDao.getCount(filterFactory.getFor(ArticleMO.class).where().eq(ArticleMO.IS_EARLY_VIEW, true));
            return count > 0;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ArticleMO> getSavedArticles() {
        Logger.d(TAG, "getSavedArticles");

        final List<ArticleMO> articles;
        try {
            articles = articleDao.find(filterFactory
                    .getFor(ArticleMO.class)
                    .where()
                    .eq(ArticleMO.FAVORITE, true)
                    .orderBy(ArticleMO.ADDED_TO_FAVORITES_DATE, false));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        return articles;
    }

    @Override
    public long getSavedArticleCount() {
        try {
            return articleDao.getCount(filterFactory.getFor(ArticleMO.class).where().eq(ArticleMO.FAVORITE, true));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getReadArticleCount() {
        try {
            return articleDao.getCount(filterFactory.getFor(ArticleMO.class).where().eq(ArticleMO.IS_READ, true));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArticleMO getArticleFromDao(final DOI doi) throws ElementNotFoundException {
        Logger.d(TAG, "getArticleFromDao");
        return articleDao.findOne(doi);
    }

    @Override
    public ArticleMO getArticleFromDaoByUri(final String uri) throws ElementNotFoundException {
        return articleDao.findOne(uri);
    }

    @Override
    public void addArticleRefToFavorites(final ArticleMO article) {
        new AddToFavoritesTask(article).execute();
    }

    @Override
    public void removeArticleRefFromFavorites(final ArticleMO article) {
        new RemoveFromFavoritesTask(article).execute();
    }

    private void processArticleUpdateResult(Map<String, Object> params, boolean addToFavorites) {
        final DOI doi = (DOI) params.get(DownloadOperation.DOI);
        if (!updatingArticleRefsSet.contains(doi)) {
            return;
        }
        updatingArticleRefsSet.remove(doi);
        try {
            ArticleMO article = getArticleFromDao(doi);
            if (addToFavorites) {
                addArticleRefToFavorites(article);
            } else {
                final Throwable error = (Throwable) params.get(NotificationCenter.ERROR);
                onArticleRefFavoriteStateChanged(article, AppErrorUtils.getAppErrorCode(error));
            }
        } catch (ElementNotFoundException e) {
            Logger.s(TAG, e);
        }
    }

    private void updateIssueFavoriteArticlesCount(final ArticleMO article) {
        final IssueMO issue = article.getSection() == null ? null : article.getSection().getIssue();
        if (issue == null) {
            return;
        }

        issueDao.updateIssueFavoriteArticlesCount(issue);
        notificationCenter.sendNotification(EventList.ISSUE_FAVORITES_COUNT_CHANGED.getEventName(), new ParamsBuilder().withIssue(issue).get());
    }

    @Override
    public boolean isArticleRefFavoriteChangingInProgress(final DOI doi) {
        return favStateChangeTaskMap.containsKey(doi);
    }

    @Override
    public boolean isArticleRefUpdating(final DOI doi) {
        return updatingArticleRefsSet.contains(doi);
    }

    @Override
    public void markArticleAsRead(final DOI doi) {
        ArticleMO article = getArticleQuietly(new DOI(doi.getValue()));
        if (null != article) {
            articleDao.makeArticleAsRead(doi);
            final ParamsBuilder paramsBuilder = new ParamsBuilder().withArticleDoi(doi);
            notificationCenter.sendNotification(EventList.ARTICLE_MARK_AS_READ.getEventName(), paramsBuilder.get());
        }
    }

    @Override
    public void changeArticleLocalThumbnail(final DOI doi, final String path) {
        articleDao.changeArticleLocalThumbnail(doi, path);
    }

    @Override
    public void updateArticleRestrictedStatus(final List<String> dois, final boolean status) {
        articleDao.updateArticleRestrictedStatus(dois, status);
    }

    @Override
    public void updateEarlyViewFeed() {
        importManager.updateEarlyViewFeed();
    }

    @Override
    public boolean isDownloaded(final DOI doi) {
        return new File(resourceManager.getArticleLocalPath(doi)).exists();
    }

    @Override
    public void setPropertiesFromStored(final Collection<ArticleMO> articleRefs, final Date importingDate) {
        for (final ArticleMO articleRef : articleRefs) {
            final ArticleMO storedArticle = articleDao.findOneQuietly(articleRef.getDOI());
            if (null != storedArticle) {
                articleRef.setUid(storedArticle.getUid());

                articleRef.setFullTextAbstract(storedArticle.getFullTextAbstract());
                articleRef.setFigures(storedArticle.getFigures());
                articleRef.setReferences(storedArticle.getReferences());
                if (null == articleRef.getSupportingInfoRefs() || 0 == articleRef.getSupportingInfoRefs().size()) {
                    articleRef.setSupportingInfoRefs(storedArticle.getSupportingInfoRefs());
                }
                // relationship properties
                articleRef.setFavorite(storedArticle.isFavorite());
                articleRef.setAddedToFavoriteDate(storedArticle.getAddedToFavoriteDate());
                articleRef.setEarlyView(storedArticle.isEarlyView());
                articleRef.setSection(storedArticle.getSection());
                articleRef.setSpecialSections(storedArticle.getSpecialSections());
                articleRef.setRead(storedArticle.isRead());
                articleRef.setFunding(storedArticle.getFunding());

                articleRef.setNote(storedArticle.getNote());
                articleRef.setAuthorSearchString(storedArticle.getAuthorSearchString());
                articleRef.setFullAuthorList(storedArticle.getFullAuthorList());
                articleRef.setOneAuthor(storedArticle.isOneAuthor());
                articleRef.setIndexTerms(storedArticle.getIndexTerms());
                articleRef.setAffiliationBlock(storedArticle.getAffiliationBlock());

                articleRef.setThumbnailHeight(storedArticle.getThumbnailHeight());
                articleRef.setThumbnailWidth(storedArticle.getThumbnailWidth());
                articleRef.setSummary(storedArticle.getSummary());
                articleRef.setHasPdf(storedArticle.hasPdf());
                articleRef.setPdfSizeMb(storedArticle.getPdfSizeMb());

                if (null == articleRef.getFirstOnlineDate()) {
                    final String firstOnLineDate = storedArticle.getFirstOnlineDate();
                    articleRef.setFirstOnlineDate(null == firstOnLineDate || firstOnLineDate.equals("")
                            ? (new SimpleDateFormat("yyyy-MM-dd")).format(new Date())
                            : firstOnLineDate);
                }
                if (null == articleRef.getPublicationDate()) {
                    final Date publicationDate = storedArticle.getPublicationDate();
                    articleRef.setPublicationDate(null == publicationDate ? new Date() : publicationDate);
                }
                if (null == articleRef.getSimpleAuthorList()) {
                    final String authors = storedArticle.getSimpleAuthorList();
                    articleRef.setSimpleAuthorList(null == authors ? "" : authors);
                }
            }

            articleRef.setImportingDate(importingDate);
        }
    }

    @Override
    public void deleteFromEarlyView(final ArticleMO article) {
        try {
            final ArticleMO storedArticle = articleDao.findOne(article.getDOI());
            if (null != storedArticle.getSection()
                    || storedArticle.getSpecialSections().size() > 0) {
                return;
            }

            if (storedArticle.isFavorite()) {
                storedArticle.setEarlyView(false);
                articleDao.saveRef(storedArticle);
                return;
            }

            deleteWithFiles(storedArticle);

        } catch (ElementNotFoundException ignored) {
        }
    }

    @Override
    public void deleteFromIssueToc(final ArticleMO article) {
        try {
            final ArticleMO storedArticle = articleDao.findOne(article.getDOI());
            if (storedArticle.isFavorite()
                    || storedArticle.isEarlyView()
                    || storedArticle.getSpecialSections().size() > 0) {
                return;
            }

            deleteWithFiles(storedArticle);

        } catch (ElementNotFoundException ignored) {
        }
    }

    @Override
    public void deleteFromSpecialSection(ArticleMO article) {
        try {
            final ArticleMO storedArticle = articleDao.findOne(article.getDOI());
            if (storedArticle.isFavorite()
                    || storedArticle.isEarlyView()
                    || null != storedArticle.getSection()
                    || storedArticle.getSpecialSections().size() > 0) {
                return;
            }

            deleteWithFiles(storedArticle);

        } catch (ElementNotFoundException ignored) {
        }
    }

    @Override
    public void removeOutdatedArticlesFromSection(SectionMO section) {
        final List<ArticleMO> articlesToRemove = articleDao.getArticlesToRemoveFromSection(section);
        for (final ArticleMO article : articlesToRemove) {
            deleteFromIssueToc(article);
        }
    }

    @Override
    public boolean hasArticlePdf(final DOI doi) {
        final ArticleMO article = getArticleField(doi, AbstractArticleMO.HAS_PDF);
        return article != null && article.hasPdf();

    }

    @Override
    public String getPdfSize(final DOI doi) {
        final ArticleMO article = getArticleField(doi, AbstractArticleMO.PDF_SIZE_MB);
        if (article != null) {
            return article.getPdfSizeMb();
        }
        return null;
    }

    @Override
    public String getArticleCitation(DOI doi) {
        final ArticleMO article = getArticleField(doi, AbstractArticleMO.CITATION);
        if (article != null) {
            return article.getCitation();
        }
        return "";

    }

    @Override
    public boolean isArticleRestricted(DOI doi) {
        final ArticleMO article = articleDao.findOneQuietly(doi);
        return article != null && article.isRestricted();
    }

    @Override
    public boolean isArticleFavorite(DOI doi) {
        final ArticleMO article = getArticleField(doi, AbstractArticleMO.FAVORITE);
        return article == null || article.isFavorite();
    }

    @Override
    public List<ArticleMO> getArticlesForSection(final SectionMO section) {
        return articleDao.getArticlesForSection(section);
    }

    @Override
    public List<ArticleMO> getArticlesForIssueTOC(DOI issueDoi) {
        return articleDao.getArticlesForIssueTOC(issueDoi);
    }

    @Override
    public long getNumOfArticlesForIssueTOC(DOI issueDoi) {
        return articleDao.getNumOfArticlesForIssueTOC(issueDoi);
    }

    @Override
    public String getFullHtmlBody(ArticleMO article) {
        return articleDao.getFullHtmlBody(article);
    }

    @Override
    public String getKeywords(DOI doi) {
        final ArticleMO article = getArticleField(doi, AbstractArticleMO.KEYWORDS);
        if (article != null) {
            return article.getKeywords();
        }
        return null;
    }

    @Override
    public void updateArticleInfoHtmlBody(ArticleMO article) {
        importManager.updateArticleInfoHtmlBody(article);
    }

    @Override
    public String loadArticleInfoHtmlBody(ArticleMO article) {
        String infoHtml = null;
        final String filePath = resourceManager.getArticleLocalPath(article.getDOI()) + "/article_info.html";
        final File file = new File(filePath);
        if (file.exists() && file.isFile()) {

            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(filePath);
                infoHtml = IOUtils.toString(fileInputStream);
            } catch (IOException e) {
                Logger.d(TAG, e.getMessage(), e);
            } finally {
                if (null != fileInputStream) {
                    IOUtils.closeQuietly(fileInputStream);
                }

            }
        }

        return infoHtml;
    }

    @Override
    public void changeKeyword(String keyword, String action) {
        importManager.changeKeyword(keyword, action);
    }

    @Override
    public void updateListOfSubscribedKeywords() {
        importManager.updateListOfSubscribedKeywords();
    }

    @Override
    public List<ArticleMO> getAllArticleDOIs() {
        return articleDao.getAllArticleDOIs();
    }

    @Override
    public boolean hasArticles() {
        return articleDao.hasArticles();
    }

    @Override
    public ArticleMO getArticleByUid(final Integer uid) {
        return articleDao.findByUid(uid);
    }

    private ArticleMO getArticleField(final DOI doi, final String fieldName) {
        try {
            final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
            articleQueryBuilder.selectColumns(fieldName)
                    .where()
                    .eq(AbstractArticleMO.DOI, doi.getValue());
            return articleQueryBuilder.queryForFirst();
        } catch (SQLException e) {
            Logger.d(TAG, "getArticleField() failed for article.doi=" + doi.getValue());
            return null;
        }
    }

    @Override
    public void saveRefsFromSpecialSectionFeed(final Collection<ArticleMO> articleRefs) {
        for (final ArticleMO articleRef : articleRefs) {
            saveRef(articleRef);
        }

    }

    @Override
    public void saveRefsFromEarlyViewFeed(final Collection<ArticleMO> articleRefs) {
        articleDao.executeBatch(new Callable() {
            @Override
            public Object call() throws Exception {
                for (final ArticleMO articleRef : articleRefs) {
                    articleRef.setEarlyView(true);
                    saveRef(articleRef);
                }

                return null;
            }
        });
    }

    @Override
    public void saveRefsFromIssueTocFeed(final SectionMO section, final Collection<ArticleMO> articleRefs) {
        for (final ArticleMO articleRef : articleRefs) {
            articleRef.setSection(section);
            saveRef(articleRef);
        }

    }

    private boolean isFullArticle(final ArticleMO storedArticle) {
        return null != storedArticle.getFullHtmlBody();
    }

    private void saveRef(final ArticleMO articleRef) {
        if (null != articleRef.getUid()) {
            final ArticleMO storedArticle = articleDao.findOneQuietly(articleRef.getDOI());
            if (null != storedArticle && isFullArticle(storedArticle) &&
                    (null == storedArticle.getLastModifiedDate() ||
                            null == articleRef.getLastModifiedDate() ||
                            storedArticle.getLastModifiedDate().before(articleRef.getLastModifiedDate()))) {
                articleRef.setNeedToCheck(true);
            }
        }
        articleDao.saveRef(articleRef);
        articleDao.updateSupportingInfo(articleRef);
        articleDao.updateSpecialSectionsForArticle(articleRef);
    }

    private void deleteWithFiles(final ArticleMO article) {
        try {
            FileUtils.deleteDirectory(new File(resourceManager.getArticleLocalPath(article.getDOI())));
        } catch (IOException ignored) {
        }
        articleDao.delete(article);
    }

    private class Tuple2d {
        ArticleMO article;
        AppErrorCode errorCode;

        public Tuple2d(ArticleMO article) {
            this(article, null);
        }

        public Tuple2d(ArticleMO article, AppErrorCode errorCode) {
            this.article = article;
            this.errorCode = errorCode;
        }

    }

    private abstract class ArticleFavStateChangeTask extends AsyncTask<Void, Void, Tuple2d> {

        protected final ArticleMO article;

        public ArticleFavStateChangeTask(ArticleMO article) {
            this.article = article;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            synchronized (favStateChangeTaskMap) {
                favStateChangeTaskMap.put(article.getDOI(), this);
            }
        }

        @Override
        protected void onPostExecute(Tuple2d result) {
            super.onPostExecute(result);
            synchronized (favStateChangeTaskMap) {
                favStateChangeTaskMap.remove(result.article.getDOI());
            }
            onArticleRefFavoriteStateChanged(result.article, result.errorCode);
        }
    }

    private class AddToFavoritesTask extends ArticleFavStateChangeTask {

        public AddToFavoritesTask(ArticleMO article) {
            super(article);
        }

        @Override
        protected Tuple2d doInBackground(Void... params) {
            if (article.isFavorite()) {
                return new Tuple2d(article);
            }
            AppErrorCode error = null;
            if (article.isRestricted()) {
                if (!NetUtils.isOnline(context)) {
                    error = NO_CONNECTION_AVAILABLE;
                } else {
                    error = ACCESS_FORBIDDEN_ARTICLE;
                }
            } else {
                final DOI doi = article.getDOI();
                if (!isDownloaded(doi) && !article.isRestricted()) {
                    updatingArticleRefsSet.add(doi);
                    importManager.loadArticle(doi);
                    return new Tuple2d(article);
                }

                article.setFavorite(true);
                articleDao.saveRef(article);
                updateIssueFavoriteArticlesCount(article);

                GANHelper.trackEvent(GANHelper.EVENT_FAVORITES,
                        GANHelper.ACTION_ADD,
                        doi.getValue(),
                        0L);

                {
                    aanHelper.trackActionSaveArticle(article.getDOI().getValue());
                }
            }

            return new Tuple2d(article, error);
        }
    }

    private class RemoveFromFavoritesTask extends ArticleFavStateChangeTask {

        public RemoveFromFavoritesTask(ArticleMO article) {
            super(article);
        }

        @Override
        protected Tuple2d doInBackground(Void... params) {
            if (!article.isFavorite()) {
                article.setAddedToFavoriteDate(new Date());
                articleDao.saveRef(article);
                return new Tuple2d(article);
            }
            article.setFavorite(false);
            articleDao.saveRef(article);
            updateIssueFavoriteArticlesCount(article);
            GANHelper.trackEvent(GANHelper.EVENT_FAVORITES,
                    GANHelper.ACTION_REMOVE,
                    article.getDOI().getValue(),
                    0L);

            {
                aanHelper.trackActionDeleteArticle(article.getDOI().getValue());
            }

            return new Tuple2d(article);
        }
    }

}
