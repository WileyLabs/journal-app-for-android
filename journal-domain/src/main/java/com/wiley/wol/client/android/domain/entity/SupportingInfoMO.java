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
import org.simpleframework.xml.Text;

/**
 * Created by dfedorov
 * on 08/07/14.
 */
@Root(name = "ref")
@DatabaseTable(tableName = "supporting_info")
public class SupportingInfoMO {
    @DatabaseField(canBeNull = false, columnName = "uid", id = true)
    private String uid;
    @DatabaseField(foreign = true, columnName = "article_uid")
    private ArticleMO article;
    @DatabaseField
    private String title;
    @Attribute(name = "assetref")
    @DatabaseField(columnName = "asset_ref")
    private String assetRef;
    @Attribute(name = "mimeType", required = false)
    @DatabaseField(columnName = "mime_type")
    private String mimeType;
    @Attribute(name = "fileSizeMb", required = false)
    @DatabaseField(columnName = "file_size_mb")
    private String fileSizeMb;
    @DatabaseField(columnName = "sort_index")
    private int sortIndex;

    @Attribute(required = false)
    private String label;
    @Text(required = false)
    private String text;

    public SupportingInfoMO() {
    }

    public SupportingInfoMO(final @Attribute(name = "label") String label, final @Text String text) {
        if (label == null || label.length() == 0) {
            title = text;
        } else {
            title = label;
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public ArticleMO getArticle() {
        return article;
    }

    public void setArticle(ArticleMO article) {
        this.article = article;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAssetRef() {
        return assetRef;
    }

    public void setAssetRef(String assetRef) {
        this.assetRef = assetRef;
    }

    public String getMimeType() {
        return null == mimeType ? "" : mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileSizeMb() {
        return fileSizeMb;
    }

    public void setFileSizeMb(String fileSizeMb) {
        this.fileSizeMb = fileSizeMb;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }
}
