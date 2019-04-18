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
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by dfedorov
 * on 03/07/14.
 */
@Root(name = "bib")
@DatabaseTable(tableName = "reference")
public class ReferenceMO {
    @DatabaseField(generatedId = true, canBeNull = false, columnName = "uid")
    private Long uid;
    @Attribute
    @DatabaseField(canBeNull = false, columnName = "ref_id")
    private String id;
    @DatabaseField(foreign = true, columnName = "article_uid")
    private ArticleMO article;
    @Attribute(name = "label")
    @DatabaseField(columnName = "title")
    private String title;
    @ElementList(inline = true)
    @ForeignCollectionField
    private Collection<CitationMO> citations;
    @DatabaseField(columnName = "sort_index")
    private int sortIndex;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Collection<CitationMO> getCitations() {
        return citations;
    }

    public List<CitationMO> getCitationSorted() {
        List<CitationMO> result = new ArrayList<>(getCitations());
        Collections.sort(result, new Comparator<CitationMO>() {
            @Override
            public int compare(CitationMO lhs, CitationMO rhs) {
                return lhs.getSortIndex() < rhs.getSortIndex() ? -1 : (lhs.getSortIndex() == rhs.getSortIndex() ? 0 : 1);
            }
        });
        return result;
    }

    public void setCitations(Collection<CitationMO> citations) {
        this.citations = citations;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public ArticleMO getArticle() {
        return article;
    }

    public void setArticle(ArticleMO article) {
        this.article = article;
    }
}
