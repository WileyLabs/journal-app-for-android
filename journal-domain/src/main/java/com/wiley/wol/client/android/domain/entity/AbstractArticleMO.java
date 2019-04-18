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

import android.text.TextUtils;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.Utils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import static java.util.Collections.emptyList;

public abstract class AbstractArticleMO {
    public static final String UID = "uid";
    public static final String DOI = "doi";
    public static final String TITLE = "title";
    public static final String SIMPLE_AUTHOR_LIST = "simple_author_list";
    public static final String SHORT_AUTHORS_TITLE = "short_authors_title";
    public static final String TOC_HEADING_1 = "toc_heading_1";
    public static final String TOC_HEADING_2 = "toc_heading_2";
    public static final String TOC_HEADING_3 = "toc_heading_3";
    public static final String KEYWORDS = "keywords";
    public static final String MANUSCRIPT_RECEIVED_DATE = "manuscript_received_date";
    public static final String RESTRICTED = "restricted";
    public static final String OPEN_ACCESS = "open_access";
    public static final String FUNDING = "funding";
    public static final String PUBLICATION_DATE = "publication_date";
    public static final String FAVORITE = "favorite";
    public static final String IS_READ = "is_read";
    public static final String IS_EARLY_VIEW = "is_early_view";
    public static final String NEED_TO_CHECK = "need_to_check";
    public static final String SUMMARY = "summary";
    public static final String THUMBNAIL_LOCAL = "thumbnail_local";
    public static final String THUMBNAIL_URL = "thumbnail_url";
    public static final String THUMBNAIL_HEIGHT = "thumbnail_height";
    public static final String THUMBNAIL_WIDTH = "thumbnail_width";
    public static final String ADDED_TO_FAVORITES_DATE = "added_to_favorites";
    public static final String FIRST_ONLINE_DATE = "first_online_date";
    public static final String HAS_PDF = "has_pdf";
    public static final String PDF_SIZE_MB = "pdf_size_mb";
    public static final String CITATION = "citation";
    public static final String PERMISSIONS = "permissions";
    public static final String PAGE_RANGE = "page_range";
    public static final String LAST_MODIFIED_DATE = "last_modified_date";
    public static final String IMPORTING_DATE = "importing_date";


    @Attribute(name = DOI)
    @DatabaseField(columnName = DOI, canBeNull = false, index = true)
    private String doi;
    @DatabaseField(columnName = HAS_PDF)
    protected boolean hasPdf;
    @DatabaseField(columnName = PDF_SIZE_MB)
    protected String pdfSizeMb;
    @DatabaseField(columnName = CITATION)
    protected String citation;
    @Attribute(name = "permissions", required = false)
    @DatabaseField(columnName = PERMISSIONS)
    private String permissions;
    @Element(name = "title")
    @DatabaseField(columnName = TITLE)
    protected String title;
    @DatabaseField(columnName = TOC_HEADING_1)
    protected String tocHeading1;
    @DatabaseField(columnName = TOC_HEADING_2)
    protected String tocHeading2;
    @DatabaseField(columnName = TOC_HEADING_3)
    protected String tocHeading3;
    @DatabaseField(columnName = FIRST_ONLINE_DATE)
    protected String firstOnlineDate;
    @DatabaseField(columnName = THUMBNAIL_LOCAL)
    private String thumbnailLocal = "";
    @DatabaseField(columnName = THUMBNAIL_URL)
    protected String thumbnailUrl;
    @DatabaseField(columnName = THUMBNAIL_WIDTH)
    protected int thumbnailWidth;
    @DatabaseField(columnName = THUMBNAIL_HEIGHT)
    protected int thumbnailHeight;
    @DatabaseField(columnName = SUMMARY)
    private String summary = "";
    @DatabaseField(columnName = PAGE_RANGE)
    protected String pageRange;
    @Element(name = "simpleAuthorList", required = false)
    @DatabaseField(columnName = SIMPLE_AUTHOR_LIST)
    private String simpleAuthorList;
    @DatabaseField(columnName = SHORT_AUTHORS_TITLE)
    protected String shortAuthorsTitle;
    @DatabaseField(columnName = KEYWORDS)
    protected String keywords;
    @DatabaseField(columnName = MANUSCRIPT_RECEIVED_DATE)
    protected String manuscriptReceivedDate;
    @DatabaseField(columnName = RESTRICTED)
    protected boolean restricted;
    @Attribute(name = "openAccess", required = false)
    @DatabaseField(columnName = OPEN_ACCESS)
    protected boolean openAccess;
    @DatabaseField(columnName = FUNDING)
    protected String funding;
    @DatabaseField(dataType = DataType.DATE, columnName = PUBLICATION_DATE)
    private Date publicationDate;
    @DatabaseField(columnName = FAVORITE)
    private boolean favorite;
    @DatabaseField(columnName = IS_READ)
    private boolean isRead;
    @DatabaseField(columnName = IS_EARLY_VIEW)
    private boolean isEarlyView;
    @DatabaseField(columnName = NEED_TO_CHECK)
    private boolean needToCheck;
    @DatabaseField(dataType = DataType.DATE, columnName = ADDED_TO_FAVORITES_DATE)
    private Date addedToFavoriteDate;
    @DatabaseField(dataType = DataType.DATE, columnName = LAST_MODIFIED_DATE)
    protected Date lastModifiedDate;
    @DatabaseField(dataType = DataType.DATE, columnName = IMPORTING_DATE)
    protected Date importingDate;
    private Collection<SpecialSectionMO> specialSections = emptyList();
    @Attribute(name = "href")
    @Path("link")
    @DatabaseField(columnName = "wol_link_template")
    private String wolLinkTemplate;

    public abstract String getManuscriptReceivedDate();

    public void setManuscriptReceivedDate(final String manuscriptReceivedDate) {
        this.manuscriptReceivedDate = manuscriptReceivedDate;
    }

    public abstract String getKeywords();

    public void setKeywords(final String keywords) {
        this.keywords = keywords;
    }

    public String getFirstOnlineDate() {
        return firstOnlineDate;
    }

    public void setFirstOnlineDate(final String firstOnlineDate) {
        this.firstOnlineDate = firstOnlineDate;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(final String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailLocal() {
        return thumbnailLocal;
    }

    public void setThumbnailLocal(final String thumbnailLocal) {
        this.thumbnailLocal = thumbnailLocal;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(final int thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(final int thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    private File getParentFolder() {
        final String folder = Utils.oneLetterHash(this.getDOI().toString());
        return new File(new File(new File("Articles"), folder), getDOI().getAssetCompatibleValue());
    }

    public File getLocalPath(final String filename) {
        return new File(getParentFolder(), filename);
    }

    public File getPdfLocalDirectory() {
        return new File(getParentFolder(), "pdf");
    }

    public String getPdfFileName() {
        return "article_pdf";
    }

    public DOI getDOI() {
        return new DOI(doi);
    }

    public void setDoi(final String doi) {
        this.doi = doi;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public abstract String getTocHeading1();

    public void setTocHeading1(final String tocHeading1) {
        this.tocHeading1 = tocHeading1;
    }

    public abstract String getTocHeading2();

    public void setTocHeading2(final String tocHeading2) {
        this.tocHeading2 = tocHeading2;
    }

    public abstract String getTocHeading3();

    public void setTocHeading3(final String tocHeading3) {
        this.tocHeading3 = tocHeading3;
    }

    public String getSimpleAuthorList() {
        return simpleAuthorList;
    }

    public void setSimpleAuthorList(final String simpleAuthorList) {
        this.simpleAuthorList = simpleAuthorList;
    }

    public String getShortAuthorsTitle() {
        return shortAuthorsTitle;
    }

    public void setShortAuthorsTitle(final String shortAuthorsTitle) {
        this.shortAuthorsTitle = shortAuthorsTitle;
    }

    public String getWolLinkTemplate() {
        return !TextUtils.isEmpty(wolLinkTemplate) ? wolLinkTemplate : "http://onlinelibrary.wiley.com/enhanced/doi/{doi}";
    }

    public String getFundingInfo() {
        return funding;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public boolean isOpenAccess() {
        return openAccess;
    }

    public void setRestricted(final boolean restricted) {
        this.restricted = restricted;
    }

    public void setOpenAccess(final boolean openAccess) {
        this.openAccess = openAccess;
    }

    public boolean isWholeWidthThumbnail() {
        // TODO: implement
        return false;
    }

    public abstract boolean isLocal();

    public boolean isExpired() {
        final Date contentExpireDate = new Date(importingDate.getTime() + Utils.getContentExpireTimeInterval());
        final Date currentDate = new Date();
        return importingDate == null || (!isLocal() && contentExpireDate.before(currentDate));
    }

    public String getFunding() {
        return funding;
    }

    public void setFunding(final String funding) {
        this.funding = funding;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(final Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
        addedToFavoriteDate = new Date();
    }

    public Date getAddedToFavoriteDate() {
        return addedToFavoriteDate;
    }

    public void setAddedToFavoriteDate(final Date addedToFavoriteDate) {
        this.addedToFavoriteDate = addedToFavoriteDate;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(final boolean isRead) {
        this.isRead = isRead;
    }

    public boolean getNeedToCheck() {
        return needToCheck;
    }

    public void setNeedToCheck(final boolean needToCheck) {
        this.needToCheck = needToCheck;
    }

    public void setEarlyView(final boolean isEarlyView) {
        this.isEarlyView = isEarlyView;
    }

    public boolean isEarlyView() {
        return isEarlyView;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public boolean hasPdf() {
        return hasPdf;
    }

    public void setHasPdf(final boolean hasPdf) {
        this.hasPdf = hasPdf;
    }

    public String getPdfSizeMb() {
        return pdfSizeMb;
    }

    public void setPdfSizeMb(final String pdfSizeMb) {
        this.pdfSizeMb = pdfSizeMb;
    }

    public abstract String getCitation();

    public void setCitation(final String citation) {
        this.citation = citation;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(final String permissions) {
        this.permissions = permissions;
    }

    public String getPageRange() {
        return pageRange;
    }

    public void setPageRange(final String pageRange) {
        this.pageRange = pageRange;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Date getImportingDate() {
        return importingDate;
    }

    public void setImportingDate(final Date importingDate) {
        this.importingDate = importingDate;
    }

    public Collection<SpecialSectionMO> getSpecialSections() {
        return specialSections;
    }

    public void setSpecialSections(Collection<SpecialSectionMO> specialSections) {
        this.specialSections = specialSections;
    }
}
