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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.wiley.wol.client.android.domain.converter.ArticleConverter;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;


/**
 * Created by dfedorov
 * on 09/07/14.
 */
@Root(name = "special-section")
@DatabaseTable(tableName = "special_section")
public class SpecialSectionMO {
    public static final String IMPORTING_DATE = "importing_date";
    @Attribute(name = "id")
    @DatabaseField(canBeNull = false, columnName = "uid", id = true)
    private String uid;
    @Element(data = true)
    @DatabaseField
    private String title;
    @DatabaseField(columnName = "unescaped_title")
    private String unescapedTitle;
    @Element(name = "description", data = true)
    @DatabaseField
    private String desc;
    @DatabaseField(columnName = "search_string")
    private String searchString;
    @DatabaseField(columnName = "is_new")
    private boolean isNew;
    @DatabaseField(columnName = "need_to_check")
    private boolean needToCheck;
    @DatabaseField(columnName = "first_letter")
    private char firstLetter;
    @DatabaseField(columnName = "in_feed_date")
    private Date inFeedDate;
    @DatabaseField(columnName = IMPORTING_DATE)
    private Date importingDate;
    private Collection<ArticleRef> articleRefs;
    private Collection<ArticleMO> articles = new ArrayList<>();

    public SpecialSectionMO() {
    }

    public SpecialSectionMO(@Element(name = "title") String title,
                            @ElementList(name="articles", entry="articleRef", empty=false, required = false) final Collection<ArticleRef> articleRefs) {
        this.title = title.trim();

        unescapedTitle = this.title.replaceAll("<[^>]+>", "")
                .replaceAll("\\\\s*\\n\\\\s*\\n\\\\s*", "\n");
        searchString = unescapedTitle.toLowerCase();

        if (unescapedTitle.length() > 0) {
            firstLetter = unescapedTitle.toUpperCase().charAt(0);
        }
        inFeedDate = new Date();

        this.articleRefs = articleRefs;
        for (final ArticleRef articleRef : articleRefs) {
            this.articles.add(ArticleConverter.convert(articleRef));
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUnescapedTitle() {
        return unescapedTitle;
    }

    public void setUnescapedTitle(String unescapedTitle) {
        this.unescapedTitle = unescapedTitle;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isNeedToCheck() {
        return needToCheck;
    }

    public void setNeedToCheck(boolean needToCheck) {
        this.needToCheck = needToCheck;
    }

    public char getFirstLetter() {
        return firstLetter;
    }

    public void setFirstLetter(char firstLetter) {
        this.firstLetter = firstLetter;
    }

    public Date getInFeedDate() {
        return inFeedDate;
    }

    public void setInFeedDate(Date inFeedDate) {
        this.inFeedDate = inFeedDate;
    }

    public Collection<ArticleMO> getArticles() {
        return articles;
    }

    public Date getImportingDate() {
        return importingDate;
    }

    public void setImportingDate(Date importingDate) {
        this.importingDate = importingDate;
    }

    public void setArticles(Collection<ArticleMO> articles) {
        this.articles = articles;
    }

    @ElementList(name="articles", entry="articleRef", empty=false, required = false)
    private Collection<ArticleRef> getArticleRefs() {
        return articleRefs;
    }
}
