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


import com.wiley.wol.client.android.domain.DOIAware;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.Collection;
import java.util.Date;

@Root(name = "articleRef")
public class ArticleRef extends AbstractArticleMO implements DOIAware {
    @Attribute(name = "publicationDate")
    private Date publicationDate;
    @Element(name = "abstract")
    private String summary;

    @Attribute(name = "modifiedDate")
    private Date lastModifiedDate;

    @Attribute(name = "abstractImageUrl",    required = false)
    private String thumbnailUrl;
    @Attribute(name = "abstractImageWidth",  required = false)
    private int thumbnailWidth = 0;
    @Attribute(name = "abstractImageHeight", required = false)
    private int thumbnailHeight = 0;

    @Path("info")
    @Attribute(name = "firstOnlineDate", required = false)
    private String firstOnlineDate;

    @Path("info")
    @Attribute  (name = "pageRange")
    private String pageRange;

    @ElementList(name = "supportingInfo", empty = false, required = false)
    private Collection<SupportingInfoMO> supportingInfoRefs;

    public ArticleRef() {
    }

    public ArticleRef(@Attribute(name = "tocHeading1") final String tocHeading1,
                      @Attribute(name = "tocHeading2") final String tocHeading2,
                      @Attribute(name = "tocHeading3") final String tocHeading3,
                      @Attribute(name = "citation") final String citation,
                      @Attribute(name = "hasPdf", required = false) final Boolean hasPdf,
                      @Attribute(name = "pdfSizeMb", required = false) final String pdfSizeMb,
                      @Path("info") @Element(name = "keywords", required = false) final String keywords,
                      @Path("info") @Attribute(name = "manuscriptReceivedDate", required = false) final String manuscriptReceivedDate,
                      @Attribute(name = "restricted") final boolean restricted,
                      @Path("info") @Element(name = "funding", required = false) final String funding

    ) {
        this.tocHeading1 = tocHeading1;
        this.tocHeading2 = tocHeading2;
        this.tocHeading3 = tocHeading3;
        this.citation = citation;
        this.hasPdf = (hasPdf != null && hasPdf);
        this.pdfSizeMb = pdfSizeMb;
        this.keywords = keywords;
        this.manuscriptReceivedDate = manuscriptReceivedDate;
        this.restricted = restricted;
        this.funding = funding;
    }

    @Override
    @Attribute(name = "tocHeading1")
    public String getTocHeading1() {
        return tocHeading1;
    }

    @Override
    @Attribute(name = "tocHeading2")
    public String getTocHeading2() {
        return tocHeading2;
    }

    @Override
    @Attribute(name = "tocHeading3")
    public String getTocHeading3() {
        return tocHeading3;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    @Attribute(name = "citation", required = false)
    public String getCitation() {
        return citation;
    }

    @Attribute(name = "hasPdf", required = false)
    public boolean getHasPdf() {
        return hasPdf;
    }

    @Override
    @Attribute(name = "pdfSizeMb", required = false)
    public String getPdfSizeMb() {
        return pdfSizeMb;
    }

    @Path("info")
    @Element(name = "funding", required = false)
    @Override
    public String getFunding() {
        return funding;
    }

    @Override
    public Date getPublicationDate() {
        return publicationDate;
    }

    @Path("info")
    @Element(name = "keywords", required = false)
    public String getKeywords() {
        return keywords;
    }

    @Path("info")
    @Attribute(name = "manuscriptReceivedDate", required = false)
    public String getManuscriptReceivedDate() {
        return manuscriptReceivedDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @Attribute(name = "restricted")
    public boolean getRestricted() {
        return restricted;
    }

    @Override
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    @Override
    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    @Override
    public String getFirstOnlineDate() {
        return firstOnlineDate;
    }

    @Override
    public String getPageRange() {
        return pageRange;
    }

    @Override
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Collection<SupportingInfoMO> getSupportingInfoRefs() {
        return supportingInfoRefs;
    }
}
