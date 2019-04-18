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
package com.wiley.wol.client.android.data.dao;

import android.annotation.TargetApi;
import android.os.Build;

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.wiley.wol.client.android.data.dao.filter.Filter;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.AbstractArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleSpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.CitationMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.ReferenceMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;
import com.wiley.wol.client.android.domain.entity.SupportingInfoMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArticleDaoImpl implements ArticleDao {
    private static final String TAG = ArticleDao.class.getSimpleName();

    @Inject
    private ArticleSpecialSectionDao articleSpecialSectionDao;

    private final Dao<ArticleMO, Integer> articleDao;
    private final Dao<FigureMO, Integer> figureDao;
    private final Dao<ReferenceMO, String> referenceDao;
    private final Dao<CitationMO, String> citationDao;
    private final Dao<SupportingInfoMO, String> supportingInfoDao;
    private final Dao<SpecialSectionMO, String> specialSectionDao;
    private final Dao<SectionMO, String> sectionDao;
    private final Dao<IssueMO, Integer> issueDao;

    private PreparedQuery<SpecialSectionMO> specialSectionForArticlePreparedQuery;

    private final List<String> articleColumnsToQuery;
    private static final Set<String> articleFieldsForLazyLoad =
            new HashSet<>(asList(ArticleMO.BODY, ArticleMO.FULL_HTML_BODY));

    @Inject
    public ArticleDaoImpl(final OrmLiteSqliteOpenHelper helper) {
        articleDao = new DaoProvider<>(helper, ArticleMO.class, Integer.class).get();
        figureDao = new DaoProvider<>(helper, FigureMO.class, Integer.class).get();
        referenceDao = new DaoProvider<>(helper, ReferenceMO.class, String.class).get();
        citationDao = new DaoProvider<>(helper, CitationMO.class, String.class).get();
        supportingInfoDao = new DaoProvider<>(helper, SupportingInfoMO.class, String.class).get();
        specialSectionDao = new DaoProvider<>(helper, SpecialSectionMO.class, String.class).get();
        sectionDao = new DaoProvider<>(helper, SectionMO.class, String.class).get();
        issueDao = new DaoProvider<>(helper, IssueMO.class, Integer.class).get();

        articleColumnsToQuery = new LinkedList<>();
        for (Field field : ArticleMO.class.getDeclaredFields()) {
            addFieldToQueryList(field);
        }
        for (Field field : AbstractArticleMO.class.getDeclaredFields()) {
            addFieldToQueryList(field);
        }
    }

    private void addFieldToQueryList(final Field field) {
        if (!field.isAnnotationPresent(DatabaseField.class) &&
                !field.isAnnotationPresent(ForeignCollectionField.class)) {
            return;
        }

        final String columnName;
        final DatabaseField databaseFieldAnnotation = field.getAnnotation(DatabaseField.class);
        if (databaseFieldAnnotation == null || databaseFieldAnnotation.columnName().length() == 0) {
            columnName = field.getName();
        } else {
            columnName = databaseFieldAnnotation.columnName();
        }

        if (!articleFieldsForLazyLoad.contains(columnName)) {
            articleColumnsToQuery.add(columnName);
        }
    }

    @Override
    public Object executeBatch(Callable task) {
        try {
            return articleDao.callBatchTasks(task);
        } catch (Exception e) {
            Logger.s(TAG, e);
            return null;
        }
    }

    @Override
    public void save(final ArticleMO article) {
        try {
            if (article.getUid() == null) {
                articleDao.create(article);
                for (SpecialSectionMO specialSection : article.getSpecialSections()) {
                    articleSpecialSectionDao.create(new ArticleSpecialSectionMO(article, specialSection));
                }
            } else {
                update(article);
                updateSpecialSectionsForArticle(article);

                deleteFigures(article);
                deleteCitations(article);
                deleteReferences(article);
                deleteSupportingInfo(article);
            }
            createFiguresForArticle(article);
            createReferencesForArticle(article);
            createCitationsForArticle(article);
            createSupportingInfoRefs(article);
        } catch (final Exception e) {
            Logger.s(TAG, "save() failed for article.doi=" + article.getDOI(), e);
        }
    }

    private void update(ArticleMO article) throws SQLException {
        boolean needToClearBody = false;
        if (article.getBody() == null) {
            final ArticleMO articleWithBody = getBodyAndFullHtmlBody(article);
            if (articleWithBody.getFullHtmlBody() != null) {
                article.setFullHtmlBody(articleWithBody.getFullHtmlBody());
            }
            needToClearBody = true;
        }
        articleDao.update(article);
        if (needToClearBody) {
            article.setBody(null);
            article.setFullHtmlBody(null);
        }
    }

    @Override
    public String getFullHtmlBody(ArticleMO article) {
        ByteArrayOutputStream result = null;
        InputStream inputStream = null;
        try {
            final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
            String fullHtmlBody = articleQueryBuilder
                    .selectColumns(ArticleMO.FULL_HTML_BODY)
                    .where()
                    .eq("uid", article.getUid()).queryForFirst().getFullHtmlBody();

            if (!fullHtmlBody.endsWith("article_full_html_body.html")) {
                return fullHtmlBody;
            }

            final File articleContentFile = new File(fullHtmlBody);
            inputStream = new FileInputStream(articleContentFile);
            result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString("UTF-8");
        } catch (Exception e) {
            Logger.d(TAG, "getFullHtmlBody() failed for article.doi=" + article.getDOI().getValue());
            return null;
        } finally {
            IOUtils.closeQuietly(result);
            IOUtils.closeQuietly(inputStream);
        }
    }

    private ArticleMO getBodyAndFullHtmlBody(ArticleMO article) throws SQLException {
        final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
        return articleQueryBuilder
                .selectColumns(ArticleMO.BODY, ArticleMO.FULL_HTML_BODY)
                .where()
                .eq("uid", article.getUid()).queryForFirst();
    }

    private void deleteFigures(ArticleMO article) throws SQLException {
        final DeleteBuilder<FigureMO, Integer> figureDeleteBuilder = figureDao.deleteBuilder();
        figureDeleteBuilder.where().eq("article_uid", article.getUid());
        figureDeleteBuilder.delete();
    }

    private void deleteCitations(final ArticleMO article) throws SQLException {
        final DeleteBuilder<CitationMO, String> citationDeleteBuilder = citationDao.deleteBuilder();
        final QueryBuilder<ReferenceMO, String> referenceQueryBuilder = referenceDao.queryBuilder();
        referenceQueryBuilder.selectColumns("uid").where().eq("article_uid", article.getUid());
        citationDeleteBuilder.where().in("reference_uid", referenceQueryBuilder);
        citationDeleteBuilder.delete();
    }

    private void deleteReferences(final ArticleMO article) throws SQLException {
        final DeleteBuilder<ReferenceMO, String> referenceDeleteBuilder = referenceDao.deleteBuilder();
        referenceDeleteBuilder.where().eq("article_uid", article.getUid());
        referenceDeleteBuilder.delete();
    }

    private void deleteSupportingInfo(final ArticleMO article) throws SQLException {
        final DeleteBuilder<SupportingInfoMO, String> supportingInfoDeleteBuilder =
                supportingInfoDao.deleteBuilder();
        supportingInfoDeleteBuilder.where().eq("article_uid", article.getUid());
        supportingInfoDeleteBuilder.delete();
    }

    @Override
    public void updateSpecialSectionsForArticle(ArticleMO article) {
        try {
            for (SpecialSectionMO specialSection : article.getSpecialSections()) {
                articleSpecialSectionDao.create(new ArticleSpecialSectionMO(article, specialSection));
            }

            for (SpecialSectionMO specialSection : getSpecialSections(article)) {
                if (!isArticleContainsSpecialSection(article, specialSection)) {
                    articleSpecialSectionDao.delete(new ArticleSpecialSectionMO(article, specialSection));
                }
            }
        } catch (SQLException e) {
            Logger.s(TAG, e.getMessage(), e);
        }
    }

    private boolean isArticleContainsSpecialSection(ArticleMO article, SpecialSectionMO specialSection) {
        for (SpecialSectionMO articlesSpecialSection : article.getSpecialSections()) {
            if (articlesSpecialSection.getUid().equals(specialSection.getUid())) {
                return true;
            }
        }
        return false;
    }

    private void createSupportingInfoRefs(final ArticleMO article) throws Exception {
        citationDao.callBatchTasks(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                int supportingInfoReferenceSortIndex = 1;
                for (SupportingInfoMO supportingInfo : article.getSupportingInfoRefs()) {
                    supportingInfo.setUid(format("%d_supportingInfoRef_%d", article.getUid(),
                            supportingInfoReferenceSortIndex));

                    supportingInfo.setSortIndex(supportingInfoReferenceSortIndex++);

                    supportingInfo.setArticle(article);
                    supportingInfoDao.create(supportingInfo);
                }
                return null;
            }
        });
    }

    private void createCitationsForArticle(final ArticleMO article) throws Exception {
        citationDao.callBatchTasks(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (ReferenceMO reference : article.getReferences()) {
                    for (CitationMO citation : reference.getCitations()) {
                        citation.setReference(reference);
                        citationDao.create(citation);
                    }

                }
                return null;
            }
        });
    }

    private void createReferencesForArticle(final ArticleMO article) throws Exception {
        citationDao.callBatchTasks(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (ReferenceMO reference : article.getReferences()) {
                    reference.setArticle(article);
                    referenceDao.create(reference);
                }
                return null;
            }
        });
    }

    private void createFiguresForArticle(final ArticleMO article) throws Exception {
        citationDao.callBatchTasks(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (FigureMO figure : article.getFigures()) {
                    figure.setArticle(article);
                    figureDao.create(figure);
                }
                return null;
            }
        });
    }

    @Override
    public void save(final List<ArticleMO> articles) {
        for (final ArticleMO article : articles) {
            save(article);
        }
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    public ArticleMO findOneQuietly(DOI doi) {
        try {
            final ArticleMO articleMO = findArticleLite(doi);
            if (null != articleMO)
                articleMO.setSpecialSections(getSpecialSections(articleMO));

            return articleMO;
        } catch (SQLException e) {
            Logger.d(TAG, "findOneQuietly() failed for article.doi=" + doi.getValue(), e);
            return null;
        }
    }

    @Override
    public ArticleMO findOne(final DOI doi) throws ElementNotFoundException {
        try {
            final ArticleMO articleMO = findArticleLite(doi);
            if (null == articleMO) {
                throw new ElementNotFoundException("Can't find ArticleMO.doi=" + doi);
            }

            articleMO.setSpecialSections(getSpecialSections(articleMO));

            return articleMO;
        } catch (final SQLException e) {
            throw new ElementNotFoundException(e);
        }
    }

    private ArticleMO findArticleLite(DOI doi) throws SQLException {
        final QueryBuilder<ArticleMO, Integer> queryBuilder = articleDao.queryBuilder();
        queryBuilder.selectColumns(articleColumnsToQuery).where().eq(ArticleMO.DOI, doi.getValue());
        final PreparedQuery<ArticleMO> preparedQuery = queryBuilder.prepare();
        return articleDao.queryForFirst(preparedQuery);
    }

    @Override
    public ArticleMO findOne(final String uri) throws ElementNotFoundException {
        return findOne(new DOI(uri.replace('_', '/')));
    }

    @Override
    public List<ArticleMO> find(final Filter<ArticleMO, Integer> filter) {
        try {
            final List<ArticleMO> articles = articleDao.query(filter.getOrmSpecificRequest());
            for (ArticleMO article : articles) {
                article.setSpecialSections(getSpecialSections(article));
            }
            return articles;
        } catch (final SQLException e) {
            return new ArrayList<>();
        } catch (final ElementNotFoundException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public long getCount(Filter<ArticleMO, Integer> filter) {
        try {
            return articleDao.countOf(filter.countOf().getOrmSpecificRequest());
        } catch (final SQLException e) {
            return 0;
        } catch (final ElementNotFoundException e) {
            return 0;
        }
    }

    @Override
    public QueryBuilder<ArticleMO, Integer> queryBuilder() {
        return articleDao.queryBuilder();
    }

    @Override
    public List<ArticleMO> query(PreparedQuery<ArticleMO> preparedQuery) throws SQLException {
        return articleDao.query(preparedQuery);
    }

    @Override
    public List<ArticleMO> getArticlesToRemoveFromSection(final SectionMO section) {
        final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();

        try {
            return articleQueryBuilder
                    .selectColumns("uid", AbstractArticleMO.DOI)
                    .where()
                    .eq(ArticleMO.SECTION_ID, section.getUid())
                    .and()
                    .ne(AbstractArticleMO.IMPORTING_DATE, section.getImportingDate()).query();
        } catch (SQLException e) {
            Logger.s(TAG, "get articles to remove from section failed", e);
        }

        return null;
    }

    @Override
    public void saveRef(ArticleMO article) {
        try {
            if (article.getUid() != null) {
                update(article);
            } else {
                articleDao.create(article);
            }
        } catch (SQLException e) {
            Logger.s(TAG, "saveRef() failed for article.doi=" + article.getDOI());
        }
    }

    @Override
    public void updateSupportingInfo(final ArticleMO article) {
        try {
            deleteSupportingInfo(article);
            createSupportingInfoRefs(article);
        } catch (Exception e) {
            Logger.s(TAG, "updateSupportingInfo() failed for article.doi=" + article.getDOI());
        }
    }

    @Override
    public void updateArticleRestrictedStatus(final List<String> dois, final boolean status) {
        try {
            final UpdateBuilder<ArticleMO, Integer> articleUpdateBuilder = articleDao.updateBuilder();
            articleUpdateBuilder.updateColumnValue( AbstractArticleMO.RESTRICTED, status)
                    .where()
                    .in(AbstractArticleMO.DOI, dois);
            articleDao.update(articleUpdateBuilder.prepare());
        } catch (SQLException e) {
            Logger.s(TAG, e);
        }
    }

    @Override
    public void makeArticleAsRead(final DOI doi) {
        changeArticleField(doi, AbstractArticleMO.IS_READ, true);
    }

    @Override
    public void changeArticleLocalThumbnail(DOI doi, String thumbnailLocal) {
        changeArticleField(doi, AbstractArticleMO.THUMBNAIL_LOCAL, thumbnailLocal);
    }

    @Override
    public List<ArticleMO> getArticlesForSection(final SectionMO section) {
        try {
            final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
            return articleQueryBuilder
                    .selectColumns(articleColumnsToQuery)
                    .where()
                    .eq(ArticleMO.SECTION_ID, section.getUid()).query();
        } catch (SQLException e) {
            Logger.s(TAG, "get articles to remove from section failed", e);
        }

        return emptyList();
    }

    @Override
    public List<ArticleMO> getArticlesForIssueTOC(DOI issueDoi) {
        try {
            final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
            final QueryBuilder<SectionMO, String> sectionQueryBuilder = sectionDao.queryBuilder();
            final QueryBuilder<IssueMO, Integer> issueQueryBuilder = issueDao.queryBuilder();

            issueQueryBuilder
                    .selectColumns("uid")
                    .where()
                    .eq(IssueMO.DOI, issueDoi.getValue());

            sectionQueryBuilder
                    .selectColumns("uid")
                    .where()
                    .in(SectionMO.ISSUE_ID, issueQueryBuilder);

            final Set<String> columns = new HashSet<>(articleColumnsToQuery);
            columns.remove("figures");
            columns.remove("references");
            columns.remove("supportingInfoRefs");

            List<ArticleMO> articles = articleQueryBuilder
                    .selectColumns(columns)
                    .where()
                    .in(ArticleMO.SECTION_ID, sectionQueryBuilder).query();

            Collections.sort(articles, new Comparator<ArticleMO>() {
                @Override
                public int compare(final ArticleMO lhs, final ArticleMO rhs) {
                    Integer sortIndex = lhs.getSection().getSortIndex();
                    return sortIndex.compareTo(rhs.getSection().getSortIndex());
                }
            });

            return articles;
        } catch (SQLException e) {
            Logger.s(TAG, "getArticlesForIssueTOC() section failed", e);
        }

        return emptyList();
    }

    @Override
    public long getNumOfArticlesForIssueTOC(DOI issueDoi) {
        try {
            final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
            final QueryBuilder<SectionMO, String> sectionQueryBuilder = sectionDao.queryBuilder();
            final QueryBuilder<IssueMO, Integer> issueQueryBuilder = issueDao.queryBuilder();

            issueQueryBuilder
                    .selectColumns("uid")
                    .where()
                    .eq(IssueMO.DOI, issueDoi.getValue());

            sectionQueryBuilder
                    .selectColumns("uid")
                    .where()
                    .in(SectionMO.ISSUE_ID, issueQueryBuilder);

            final Set<String> columns = new HashSet<>(articleColumnsToQuery);
            columns.remove("figures");
            columns.remove("references");
            columns.remove("supportingInfoRefs");

            return articleQueryBuilder
                    .selectColumns(columns)
                    .where()
                    .in(ArticleMO.SECTION_ID, sectionQueryBuilder).countOf();
        } catch (SQLException e) {
            Logger.s(TAG, "getArticlesForIssueTOC() section failed", e);
        }

        return 0;
    }

    @Override
    public List<ArticleMO> getAllArticleDOIs() {
        QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();
        articleQueryBuilder.selectColumns(ArticleMO.UID, ArticleMO.DOI);
        try {
            return articleQueryBuilder.query();
        } catch (SQLException e) {
            Logger.s(TAG, e);
            return emptyList();
        }
    }

    @Override
    public boolean hasArticles() {
        try {
            return articleDao.countOf() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    @Override
    public ArticleMO findByUid(final Integer uid) {
        try {
            return articleDao.queryForId(uid);
        } catch (SQLException e) {
            Logger.s(TAG, "Unable to find article by uid " + uid, e);
            return null;
        }
    }

    private void changeArticleField(final DOI doi, final String fieldName, final Object value) {
        try {
            final UpdateBuilder<ArticleMO, Integer> articleUpdateBuilder = articleDao.updateBuilder();
            articleUpdateBuilder.updateColumnValue(fieldName, value)
                    .where()
                    .eq(AbstractArticleMO.DOI, doi.getValue());
            articleDao.update(articleUpdateBuilder.prepare());
        } catch (SQLException e) {
            Logger.s(TAG, "makeArticleAsRead() failed for article.doi=" + doi, e);
        }
    }

    @Override
    public void delete(ArticleMO articleMO) {
        try {
            deleteFigures(articleMO);
            deleteCitations(articleMO);
            deleteReferences(articleMO);
            deleteSupportingInfo(articleMO);
            articleSpecialSectionDao.delete(articleSpecialSectionDao.queryForArticle(articleMO));
            articleDao.delete(articleMO);
        } catch (final SQLException ignored) {
            Logger.s(TAG, "delete() failed for article.doi=" + articleMO.getDOI());
        }
    }

    private Collection<SpecialSectionMO> getSpecialSections(final ArticleMO article) throws SQLException {
        if (specialSectionForArticlePreparedQuery == null) {
            specialSectionForArticlePreparedQuery = makeSpecialSectionsForArticleQuery();
        }
        specialSectionForArticlePreparedQuery.setArgumentHolderValue(0, article);
        return specialSectionDao.query(specialSectionForArticlePreparedQuery);
    }

    private PreparedQuery<SpecialSectionMO> makeSpecialSectionsForArticleQuery() throws SQLException {
        final QueryBuilder<ArticleSpecialSectionMO, Integer> articleSpecialSectionQb =
                articleSpecialSectionDao.queryBuilder();

        articleSpecialSectionQb.selectColumns("special_section_uid");
        SelectArg specialSectionSelectArg = new SelectArg();

        articleSpecialSectionQb.where().eq("article_uid", specialSectionSelectArg);
        QueryBuilder<SpecialSectionMO, String> specialSectionQb = specialSectionDao.queryBuilder();
        specialSectionQb.where().in("uid", articleSpecialSectionQb);

        return specialSectionQb.prepare();
    }
}
