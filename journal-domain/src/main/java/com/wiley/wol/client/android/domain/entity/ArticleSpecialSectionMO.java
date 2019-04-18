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

/**
 * Created by dfedorov
 * on 11/07/14.
 */
public class ArticleSpecialSectionMO {
    @DatabaseField(generatedId = true)
    private Integer uid;

    @DatabaseField(foreign = true, columnName = "article_uid", uniqueCombo = true)
    private ArticleMO article;

    @DatabaseField(foreign = true, columnName = "special_section_uid", uniqueCombo = true)
    private SpecialSectionMO specialSection;

    public ArticleSpecialSectionMO() {
    }

    public ArticleSpecialSectionMO(final ArticleMO article, final SpecialSectionMO specialSection) {
        this.article = article;
        this.specialSection = specialSection;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public ArticleMO getArticle() {
        return article;
    }

    public void setArticle(ArticleMO article) {
        this.article = article;
    }

    public SpecialSectionMO getSpecialSection() {
        return specialSection;
    }

    public void setSpecialSection(SpecialSectionMO specialSection) {
        this.specialSection = specialSection;
    }
}
