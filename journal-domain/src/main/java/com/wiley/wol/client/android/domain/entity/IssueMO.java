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
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.DOIAware;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collection;
import java.util.Date;

import static java.util.Collections.emptyList;

@DatabaseTable(tableName = "issue")
@Root(name = "issue")
public class IssueMO implements DOIAware {
    public static final String DOI = "doi";
    public static final String VOLUME_NUMBER = "volume_number";
    public static final String ISSUE_NUMBER = "issue_number";
    public static final String COVER_DATE = "cover_date";
    public static final String COVER_YEAR = "cover_year";
    public static final String PUBLICATION_DATE = "publication_date";
    public static final String DOWNLOAD_SIZE = "download_size";
    public static final String IS_SAMPLE_ISSUE = "is_sample_issue";
    public static final String IMPORTING_DATE = "importing_date";
    public static final String TOC_IMPORTING_DATE = "toc_importing_date";
    public static final String LOCAL = "local";

    @DatabaseField(dataType = DataType.BOOLEAN, columnName = LOCAL)
    private boolean isLocal;
    @DatabaseField(dataType = DataType.BOOLEAN, columnName = "is_new")
    private boolean isNew;
    @DatabaseField(dataType = DataType.BOOLEAN, columnName = "restricted")
    protected boolean restricted;

    @DatabaseField(generatedId = true, canBeNull = false)
    private Integer uid;

    @DatabaseField(columnName = DOI, canBeNull = false, index = true)
    @Attribute(name = "doi")
    private String doi;
    @DatabaseField(dataType = DataType.DATE, columnName = IMPORTING_DATE)
    private Date importingDate;
    @DatabaseField(columnName = "sort_index")
    private int sortIndex;
    @DatabaseField(columnName = "issue_description")
    @Attribute(name = "issueTitle", required = false)
    private String issueDescription = "";
    @DatabaseField(columnName = "cover_image_url")
    @Attribute(name = "coverImgUrl", required = false)
    private String coverImageUrl;
    @DatabaseField(columnName = "cover_image_local")
    private String coverImageLocal;
    @DatabaseField(columnName = "favorites_counter")
    private int favoritesCounter;
    @DatabaseField(dataType = DataType.DATE, columnName = TOC_IMPORTING_DATE)
    private Date tocImportingDate;

    @DatabaseField(columnName = VOLUME_NUMBER)
    @Attribute(name = "volumeNumber")
    private String volumeNumber;
    @DatabaseField(columnName = ISSUE_NUMBER)
    @Attribute(name = "issueNumber")
    private String issueNumber;
    @DatabaseField(columnName = COVER_DATE)
    @Attribute(name = "coverDate")
    private String coverDate;
    @DatabaseField(columnName = COVER_YEAR)
    @Attribute(name = "coverYear")
    private String coverYear;
    @DatabaseField(dataType = DataType.DATE, columnName = PUBLICATION_DATE)
    @Attribute(name = "publicationDate")
    private Date publicationDate;
    @DatabaseField(columnName = DOWNLOAD_SIZE)
    @Attribute(name = "archiveSizeMb")
    private String downloadSize;
    @DatabaseField(dataType = DataType.BOOLEAN, columnName = IS_SAMPLE_ISSUE)
    @Attribute(name = "sample")
    private boolean isSampleIssue;
    @ElementList(name = "toc", required = false)
    @ForeignCollectionField
    private Collection<SectionMO> sections;

    @Attribute(name = "pageRanges")
    private String pageRanges;

    public IssueMO() {
    }

    @Override
    public DOI getDOI() {
        return new DOI(doi);
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(final String doi) {
        this.doi = doi;
    }

    public String getVolumeNumber() {
        return volumeNumber;
    }

    public void setVolumeNumber(final String volumeNumber) {
        this.volumeNumber = volumeNumber;
    }

    public String getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(final String issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getCoverDate() {
        return coverDate;
    }

    public void setCoverDate(final String coverDate) {
        this.coverDate = coverDate;
    }

    public String getCoverYear() {
        return coverYear;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(final Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setDownloadSize(final String downloadSize) {
        this.downloadSize = downloadSize;
    }

    public String getDownloadSize() {
        return downloadSize;
    }

    public boolean isSampleIssue() {
        return isSampleIssue;
    }


    public Integer getUid() {
        return uid;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueMO)) return false;

        final IssueMO issueMO = (IssueMO) o;

        if (!doi.equals(issueMO.doi)) return false;
        if (uid != null ? !uid.equals(issueMO.uid) : issueMO.uid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + doi.hashCode();
        return result;
    }

    public Collection<SectionMO> getSections() {
        if (sections != null) {
            return sections;
        } else {
            return emptyList();
        }
    }

    public void setSections(final Collection<SectionMO> sections) {
        this.sections = sections;
    }

    public boolean before(final IssueMO another) {
        return this.getPublicationDate().before(another.getPublicationDate());
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setCoverYear(final String coverYear) {
        this.coverYear = coverYear;
    }

    public void setIsSampleIssue(final boolean isFreeSample) {
        this.isSampleIssue = isFreeSample;
    }

    public void setUid(final Integer uid) {
        this.uid = uid;
    }

    public void setIsLocal(final boolean local) {
        isLocal = local;
    }

    public void setIsNew(final boolean isNew) {
        this.isNew = isNew;
    }

    public Date getImportingDate() {
        return importingDate;
    }

    public void setImportingDate(final Date importingDate) {
        this.importingDate = importingDate;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(final int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(final String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(final String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getCoverImageLocal() {
        return coverImageLocal;
    }

    public void setCoverImageLocal(final String coverImageLocal) {
        this.coverImageLocal = coverImageLocal;
    }

    public int getFavoritesCounter() {
        return favoritesCounter;
    }

    public void setFavoritesCounter(final int favoritesCounter) {
        this.favoritesCounter = favoritesCounter;
    }

    public Date getTocImportingDate() {
        return tocImportingDate;
    }

    public void setTocImportingDate(final Date tocImportingDate) {
        this.tocImportingDate = tocImportingDate;
    }

    public String getPageRanges() {
        return pageRanges;
    }

    public void setPageRanges(final String pageRanges) {
        this.pageRanges = pageRanges;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(final boolean restricted) {
        this.restricted = restricted;
    }
}
