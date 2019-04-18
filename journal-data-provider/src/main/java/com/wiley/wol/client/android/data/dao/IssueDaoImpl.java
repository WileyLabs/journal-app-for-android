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

import com.google.inject.Inject;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.TableUtils;
import com.wiley.wol.client.android.data.dao.filter.Filter;
import com.wiley.wol.client.android.data.provider.DaoProvider;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.AbstractArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;

public class IssueDaoImpl implements IssueDao {
    private static final String TAG = IssueDaoImpl.class.getSimpleName();

    private final Dao<IssueMO, Integer> issueDao;
    private final Dao<SectionMO, String> sectionDao;
    private final Dao<ArticleMO, Integer> articleDao;
    private final OrmLiteSqliteOpenHelper helper;
    @Inject
    private ArticleService articleService;

    @Inject
    public IssueDaoImpl(final OrmLiteSqliteOpenHelper helper) {
        this.helper = helper;
        issueDao = new DaoProvider<>(this.helper, IssueMO.class, Integer.class).get();
        sectionDao = new DaoProvider<>(this.helper, SectionMO.class, String.class).get();
        articleDao = new DaoProvider<>(this.helper, ArticleMO.class, Integer.class).get();
    }

    @Override
    public List<IssueMO> findAll() {
        try {
            return issueDao.queryForAll();
        } catch (final SQLException e) {
            Logger.s(TAG, "findAll() failed");
        }

        return new ArrayList<>();
    }

    @Override
    public long countOf() {
        try {
            return issueDao.countOf();
        } catch (SQLException e) {
            Logger.s(TAG, "findAll() failed");
            return 0L;
        }
    }

    @Override
    public List<SectionMO> getSectionsForTOC(final DOI doi) throws ElementNotFoundException {
        final QueryBuilder<SectionMO, String> sectionQueryBuilder = sectionDao.queryBuilder();
        try {
            QueryBuilder<IssueMO, Integer> issueQueryBuilder = issueDao
                    .queryBuilder();

            issueQueryBuilder
                    .selectColumns("uid")
                    .where()
                    .eq(IssueMO.DOI, doi.getValue());

            sectionQueryBuilder
                    .orderBy(SectionMO.SORT_INDEX, true)
                    .where()
                    .in(SectionMO.ISSUE_ID, issueQueryBuilder);

            final PreparedQuery<SectionMO> preparedQuery = sectionQueryBuilder.prepare();
            return sectionDao.query(preparedQuery);
        } catch (SQLException e) {
            throw new ElementNotFoundException(e);
        }
    }

    @Override
    public int getNumOfSavedArticles(final DOI doi) throws ElementNotFoundException {
        final QueryBuilder<IssueMO, Integer> queryBuilder = issueDao.queryBuilder();
        final List<IssueMO> issues;
        try {
            issues = queryBuilder
                    .distinct()
                    .selectColumns("favorites_counter")
                    .where()
                    .eq(IssueMO.DOI, doi.getValue())
                    .query();
        } catch (SQLException e) {
            throw new ElementNotFoundException(e);
        }

        if (issues.isEmpty()) {
            throw new ElementNotFoundException("Can't find issue.doi=" + doi);
        }

        return issues.get(0).getFavoritesCounter();
    }

    @Override
    public IssueMO findOne(final DOI doi) throws ElementNotFoundException {
        try {
            final QueryBuilder<IssueMO, Integer> queryBuilder = issueDao.queryBuilder();
            queryBuilder.where().eq(IssueMO.DOI, doi.getValue());
            final PreparedQuery<IssueMO> preparedQuery = queryBuilder.prepare();
            final IssueMO issueMO = issueDao.queryForFirst(preparedQuery);
            if (null == issueMO) {
                throw new ElementNotFoundException("Can't find issue.doi=" + doi);
            }

            return issueMO;
        } catch (final SQLException e) {
            throw new ElementNotFoundException(e);
        }
    }

    @Override
    public void save(final IssueMO issue) {
        try {
            if (issue.getUid() != null) {
                update(issue);
            } else {
                issueDao.create(issue);
                saveSectionsFor(issue);
            }
        } catch (final SQLException e) {
            Logger.s(TAG, "save() failed for issue.doi=" + issue.getDOI());
        }
    }

    @Override
    public void saveRef(IssueMO issueRef) {
        try {
            if (issueRef.getUid() != null) {
                issueDao.update(issueRef);
            } else {
                issueDao.create(issueRef);
            }
        } catch (final SQLException e) {
            Logger.s(TAG, "save() failed for issue.doi=" + issueRef.getDOI());
        }
    }

    private void saveSectionsFor(final IssueMO issue) {
        int sectionCounter = 0;
        for (final SectionMO section : issue.getSections()) {
            try {
                section.setSortIndex(sectionCounter);
                section.setUid(format("%d_level1_%d", issue.getUid(), sectionCounter++));
                section.setIssue(issue);
                sectionDao.create(section);
                saveArticleRefsFor(section);
            } catch (final SQLException e) {
                Logger.s(TAG, "save() failed for section.name=" + section.getName());
            }
        }
    }

    private void saveArticleRefsFor(final SectionMO section) {
        for (final ArticleMO article : section.getArticles()) {
            try {
                article.setSection(section);
                articleDao.create(article);
            } catch (final SQLException e) {
                Logger.s(TAG, "save() failed for article.doi=" + article.getDOI());
            }
        }
    }

    @Override
    public void save(final List<IssueMO> issues) {
        for (final IssueMO issue : issues) {
            save(issue);
        }
    }

    @Override
    public void delete(final IssueMO issue) {
        try {
            for (SectionMO section : issue.getSections()) {
                deleteArticlesFromSection(section);
                sectionDao.delete(section);
            }
            issueDao.delete(issue);
        } catch (final SQLException ignored) {
        }
    }

    @Override
    public void updateIssueFavoriteArticlesCount(final IssueMO issue) {
        final QueryBuilder<SectionMO, String> sectionQueryBuilder = sectionDao.queryBuilder();
        final QueryBuilder<ArticleMO, Integer> articleQueryBuilder = articleDao.queryBuilder();

        try {
            sectionQueryBuilder
                    .selectColumns("uid")
                    .where()
                    .eq(SectionMO.ISSUE_ID, issue.getUid());

            articleQueryBuilder
                    .where()
                    .in(ArticleMO.SECTION_ID, sectionQueryBuilder)
                    .and().eq(AbstractArticleMO.FAVORITE, true);

            issue.setFavoritesCounter((int) articleQueryBuilder.countOf());
            saveRef(issue);
        } catch (SQLException e) {
            Logger.s(TAG, "updateIssueFavoriteArticlesCount() failed for issue.doi=" + issue.getDOI(), e);
        }
    }

    @Override
    public long getCount(Filter<IssueMO, Integer> filter) {
        try {
            return issueDao.countOf(filter.countOf().getOrmSpecificRequest());
        } catch (final SQLException e) {
            return 0;
        } catch (final ElementNotFoundException e) {
            return 0;
        }
    }

    @Override
    public void clear() {
        try {
            TableUtils.clearTable(this.helper.getConnectionSource(), IssueMO.class);
            TableUtils.clearTable(this.helper.getConnectionSource(), SectionMO.class);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void update(final IssueMO issueFeed) throws SQLException {
        final IssueMO issueStored;
        try {
            issueStored = findOne(issueFeed.getDOI());
        } catch (ElementNotFoundException e) {
            throw new RuntimeException(e);
        }

        final Date now = new Date();
        int sectionCounter = 0;
        for (SectionMO sectionFeed : issueFeed.getSections()) {
            sectionFeed.setSortIndex(sectionCounter);
            sectionFeed.setUid(format("%d_level1_%d", issueStored.getUid(), sectionCounter++));
            SectionMO storedSection = sectionDao.queryForId(sectionFeed.getUid());
            if (storedSection != null) {
                storedSection.setImportingDate(now);
                storedSection.setIssue(issueStored);
                try {
                    sectionDao.update(storedSection);
                    updateArticlesFromSection(storedSection, sectionFeed.getArticles());
                } catch (SQLException e) {
                    Logger.s(TAG, "update() failed for section.name=" + storedSection.getName());
                }
            } else {
                sectionFeed.setImportingDate(now);
                sectionFeed.setIssue(issueStored);
                try {
                    sectionDao.create(sectionFeed);
                    createArticlesFromSection(sectionFeed, sectionFeed.getArticles());
                } catch (final SQLException e) {
                    Logger.s(TAG, "create() failed for section.name=" + sectionFeed.getName());
                }
            }
        }

        deleteOutdatedSections(issueStored, now);

        issueFeed.setFavoritesCounter(issueStored.getFavoritesCounter());
        issueDao.update(issueFeed);
    }

    private void deleteOutdatedSections(IssueMO issueStored, Date now) throws SQLException {
        final QueryBuilder<SectionMO, String> sectionQueryBuilder = sectionDao.queryBuilder();
        sectionQueryBuilder.selectColumns("uid").where()
                .eq(SectionMO.ISSUE_ID, issueStored.getUid())
                .and()
                .ne(AbstractArticleMO.IMPORTING_DATE, now);

        final List<ArticleMO> articles = articleDao.queryBuilder()
                .where()
                .in(ArticleMO.SECTION_ID, sectionQueryBuilder)
                .query();

        for (final ArticleMO storedArticle : articles) {
            articleService.deleteFromIssueToc(storedArticle);
        }

        final DeleteBuilder<SectionMO, String> deleteSectionsBuilder = sectionDao.deleteBuilder();
        deleteSectionsBuilder
                .where()
                .eq(SectionMO.ISSUE_ID, issueStored.getUid())
                .and()
                .ne(AbstractArticleMO.IMPORTING_DATE, now);
        sectionDao.delete(deleteSectionsBuilder.prepare());
    }

    private void deleteArticlesFromSection(final SectionMO section) {
        final List<ArticleMO> sectionArticles = articleService.getArticlesForSection(section);
        for (final ArticleMO storedArticle : sectionArticles) {
            articleService.deleteFromIssueToc(storedArticle);
        }
    }

    private  void createArticlesFromSection(final SectionMO section, final Collection<ArticleMO> articleRefs) {
        articleService.setPropertiesFromStored(articleRefs, section.getImportingDate());
        articleService.saveRefsFromIssueTocFeed(section, articleRefs);
    }

    private void updateArticlesFromSection(final SectionMO section, final Collection<ArticleMO> articleRefs) {
        if (articleRefs.isEmpty()) {
            articleRefs.addAll(articleService.getArticlesForSection(section));
            for (ArticleMO article : articleRefs) {
                article.setImportingDate(section.getImportingDate());
            }
        } else {
            articleService.setPropertiesFromStored(articleRefs, section.getImportingDate());
        }

        articleService.saveRefsFromIssueTocFeed(section, articleRefs);
        articleService.removeOutdatedArticlesFromSection(section);
    }
}
