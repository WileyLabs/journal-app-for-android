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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Created by dfedorov
 * on 30/06/14.
 */
@Root(name = "ref")
@DatabaseTable(tableName = "figure")
public class FigureMO {
    @DatabaseField(generatedId = true, canBeNull = false, columnName = "uid")
    private Integer uid;
    @DatabaseField(foreign = true, columnName = "article_uid")
    private ArticleMO article;
    @DatabaseField(columnName = "original_local")
    private String originalLocal;
    @DatabaseField(columnName = "short_caption")
    @Attribute(name = "id")
    private String shortCaption;
    @DatabaseField(columnName = "caption")
    private String caption;
    @DatabaseField(columnName = "title")
    @Attribute(name = "label")
    private String title;
    @DatabaseField(columnName = "kind")
    @Attribute(name = "refType")
    private String kind;
    @Attribute(name = "imageref")
    @DatabaseField(columnName = "image_ref")
    private String imageRef;
    @DatabaseField(columnName = "sort_index")
    private int sortIndex;

    public Integer getUid() {
        return uid;
    }

    public String getOriginalLocal() {
        return originalLocal;
    }

    public void setOriginalLocal(String originalLocal) {
        this.originalLocal = originalLocal;
    }

    public String getShortCaption() {
        return shortCaption;
    }

    public void setShortCaption(String shortCaption) {
        this.shortCaption = shortCaption;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public String getImageRef() {
        return imageRef;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = imageRef;
    }

    public ArticleMO getArticle() {
        return article;
    }

    public void setArticle(ArticleMO article) {
        this.article = article;
    }
}
