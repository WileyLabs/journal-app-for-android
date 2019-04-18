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
package com.wiley.wol.client.android.domain.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.wiley.wol.client.android.domain.converter.ArticleConverter;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Root(name = "tocHeading1")
@DatabaseTable(tableName = "section")
public class SectionMO {
    public static final String NAME = "name";
    public static final String ISSUE_ID = "issue_id";
    public static final String IMPORTING_DATE = "importing_date";
    public static final String SORT_INDEX = "sort_index";

    @DatabaseField(canBeNull = false, id = true)
    private String uid;
    @Attribute(name = "heading")
    @DatabaseField(columnName = NAME)
    private String name;
    private Collection<ArticleRef> articleRefs;
    private Collection<ArticleMO> articles = new ArrayList<>();
    @DatabaseField(foreign = true, columnName = ISSUE_ID)
    private IssueMO issue;
    @DatabaseField(dataType = DataType.DATE, columnName = IMPORTING_DATE)
    protected Date importingDate;
    @DatabaseField(columnName = SORT_INDEX)
    private int sortIndex;

    public SectionMO() {
    }

    public SectionMO(@ElementList(name = "articleRef", inline = true) final Collection<ArticleRef> articleRefs) {
        this.articleRefs = articleRefs;
        for (final ArticleRef articleRef : articleRefs) {
            this.articles.add(ArticleConverter.convert(articleRef));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Deprecated
    /**
     * This field doesn't annotated by ForeignCollectionField annotation anymore,
     * so for database entities use articleService.getArticlesForSection(section) instead
     */
    public Collection<ArticleMO> getArticles() {
        return articles;
    }

    public void setArticles(final List<ArticleMO> articles) {
        this.articles = articles;
    }

    public void addAll(final List<ArticleMO> articles) {
        this.articles.addAll(articles);
    }

    @ElementList(name = "articleRef", inline = true)
    private Collection<ArticleRef> getArticleRefs() {
        return articleRefs;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public IssueMO getIssue() {
        return issue;
    }

    public void setIssue(final IssueMO issue) {
        this.issue = issue;
    }

    public Date getImportingDate() {
        return importingDate;
    }

    public void setImportingDate(final Date importingDate) {
        this.importingDate = importingDate;
    }

    public boolean hasRestrictedArticles() {
        for (ArticleMO article : articles) {
            if (article.isRestricted()) {
                return true;
            }
        }
        return false;
    }

    public void setSortIndex(final int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public int getSortIndex() {
        return sortIndex;
    }
}
