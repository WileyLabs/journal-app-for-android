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
package com.wiley.wol.client.android.domain.converter;

import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.ArticleRef;

public class ArticleConverter {

    public static ArticleMO convert(final ArticleRef articleRef) {
        final ArticleMO articleMO = new ArticleMO();
        articleMO.setTitle(articleRef.getTitle());
        articleMO.setTocHeading1(articleRef.getTocHeading1());
        articleMO.setTocHeading2(articleRef.getTocHeading2());
        articleMO.setTocHeading3(articleRef.getTocHeading3());
        articleMO.setDoi(articleRef.getDOI().getValue());
        articleMO.setSimpleAuthorList(null == articleRef.getSimpleAuthorList() ? "" : articleRef.getSimpleAuthorList());
        articleMO.setKeywords(null == articleRef.getKeywords() ? "" : articleRef.getKeywords());
        articleMO.setManuscriptReceivedDate(articleRef.getManuscriptReceivedDate());
        articleMO.setRestricted(articleRef.isRestricted());
        articleMO.setFunding(null == articleRef.getFunding() ? "" : articleRef.getFunding());
        articleMO.setPublicationDate(articleRef.getPublicationDate());
        articleMO.setSummary(articleRef.getSummary());
        articleMO.setLastModifiedDate(articleRef.getLastModifiedDate());
        articleMO.setHasPdf(articleRef.getHasPdf());
        articleMO.setPdfSizeMb(articleRef.getPdfSizeMb());
        articleMO.setThumbnailUrl(null == articleRef.getThumbnailUrl() ? "" : articleRef.getThumbnailUrl());
        articleMO.setThumbnailWidth(articleRef.getThumbnailWidth());
        articleMO.setThumbnailHeight(articleRef.getThumbnailHeight());
        articleMO.setCitation(null == articleRef.getCitation() ? "" : articleRef.getCitation());
        articleMO.setPermissions(null == articleRef.getPermissions() ? "" : articleRef.getPermissions());
        articleMO.setPageRange(articleRef.getPageRange());
        articleMO.setFirstOnlineDate(null == articleRef.getFirstOnlineDate() ? "" : articleRef.getFirstOnlineDate());
        articleMO.setSupportingInfoRefs(articleRef.getSupportingInfoRefs());
        articleMO.setOpenAccess(articleRef.isOpenAccess());

        return articleMO;
    }
}
