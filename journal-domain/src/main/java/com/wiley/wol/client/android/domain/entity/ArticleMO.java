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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static java.lang.String.format;

@Root(name = "article")
@DatabaseTable(tableName = "article")
public class ArticleMO extends AbstractArticleMO {
    public static final String BODY = "body";
    public static final String SECTION_ID = "section_id";
    public static final String ABSTRACT = "full_text_abstract";

    public static final int VOLUME_NUMBER = 0;
    public static final int ISSUE_NUMBER = 1;
    public static final String FULL_HTML_BODY = "full_html_body";
    public static final String AFFILIATION_BLOCK = "affiliation_block";
    private static final int INIT_CAPACITY = 128;

    @DatabaseField(generatedId = true, canBeNull = false, columnName = UID)
    private Integer uid;

    @Element(name = "body")
    @DatabaseField(columnName = BODY)
    private String body;
    @DatabaseField(columnName = FULL_HTML_BODY)
    private String fullHtmlBody;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = SECTION_ID)
    private SectionMO section;
    @Element(name = "abstract")
    @DatabaseField(columnName = ABSTRACT)
    private String fullTextAbstract;
    @ElementList(name = "figureReferences", type = FigureMO.class, empty = false)
    @ForeignCollectionField
    private Collection<FigureMO> figures = new ArrayList<>();
    @ElementList(name = "bibliography", type = ReferenceMO.class, empty = false)
    @ForeignCollectionField
    private Collection<ReferenceMO> references = new ArrayList<>();
    @ElementList(name = "supportingInfoReferences", type = SupportingInfoMO.class, empty = false)
    @ForeignCollectionField(eager = true)
    private Collection<SupportingInfoMO> supportingInfoRefs = new ArrayList<>();
    @ElementList(name = "subjectInfo", required = false)
    private Collection<Subject> subjects;
    @DatabaseField(columnName = "index_terms")
    private String indexTerms;
    @Element(name = "copyright", required = false)
    @DatabaseField(columnName = "author_search_string")
    private String authorSearchString;
    @Element(name = "affiliationBlock", required = false)
    @DatabaseField(columnName = AFFILIATION_BLOCK)
    private String affiliationBlock;
    @DatabaseField(columnName = "full_author_list")
    private String fullAuthorList;
    @ElementList(name = "fullAuthorList", required = false)
    private Collection<Author> authors;
    @DatabaseField(columnName = "one_author")
    private boolean oneAuthor;
    @Element
    @DatabaseField
    private String note;
    @Element(name = "abstractImageHref")
    @DatabaseField(columnName = "abstract_image_href")
    private String abstractImageHref;

    public ArticleMO() {
    }

    @SuppressWarnings("unused")
    public ArticleMO(@Element(name = "tocHeading1") final String tocHeading1,
                     @Element(name = "tocHeading2") final String tocHeading2,
                     @Element(name = "tocHeading3") final String tocHeading3,
                     @Element(name = "keywords") final String keywords,
                     @Element(name = "citation") final String citation,
                     @Element(name = "manuscriptReceivedDate") final String manuscriptReceivedDate,
                     @Element(name = "shortAuthorsTitle", required = false) final String shortAuthorsTitle,
                     @ElementList(name = "subjectInfo", required = false) final Collection<Subject> subjects,
                     @ElementList(name = "fullAuthorList", required = false) final Collection<Author> authors

    ) {
        this.tocHeading1 = tocHeading1;
        this.tocHeading2 = tocHeading2;
        this.tocHeading3 = tocHeading3;
        this.keywords = keywords;
        this.citation = citation;
        this.manuscriptReceivedDate = manuscriptReceivedDate;
        this.shortAuthorsTitle = shortAuthorsTitle;

        if (subjects != null) {
            initIndexTerms(subjects);
        }

        if (authors != null) {
            oneAuthor = (authors.size() == 1);
            initFullAuthorList(authors);
        }
    }

    private void initFullAuthorList(Collection<Author> authors) {
        final StringBuilder fullAuthorList = new StringBuilder(INIT_CAPACITY);
        for (Author author : authors) {
            fullAuthorList.append(format("<span>%s</span><br />", author.author));
        }

        this.fullAuthorList = fullAuthorList.toString();
    }

    private void initIndexTerms(Collection<Subject> subjects) {
        final StringBuilder indexTerms = new StringBuilder(INIT_CAPACITY);
        for (Subject subject : subjects) {
            if (!isEmpty(subject.getName()) && !isEmpty(subject.getHref())) {
                indexTerms.append(format("<a class=\"index_term_class\" href=\"%s\">%s</a><br />\n",
                        subject.getHref(), subject.getName()));
            } else if (!isEmpty(subject.getName())) {
                indexTerms.append(format("<span class=\"index_term_class\">%s</span><br />\n", subject.getName()));
            }
        }

        this.indexTerms = indexTerms.toString();
    }

    public void setUid(final Integer uid) {
        this.uid = uid;
    }

    @Override
    @Element(name = "tocHeading1")
    public String getTocHeading1() {
        return tocHeading1;
    }

    @Override
    @Element(name = "tocHeading2")
    public String getTocHeading2() {
        return tocHeading2;
    }

    @Override
    @Element(name = "tocHeading3")
    public String getTocHeading3() {
        return tocHeading3;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public String getFullHtmlBody() {
        return fullHtmlBody;
    }

    public void setFullHtmlBody(String fullHtmlBody) {
        this.fullHtmlBody = fullHtmlBody;
    }

    public Integer getUid() {
        return uid;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleMO)) return false;

        ArticleMO articleMO = (ArticleMO) o;

        return !(uid != null ? !uid.equals(articleMO.uid) : articleMO.uid != null);
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }

    @Element(name = "keywords")
    public String getKeywords() {
        return keywords;
    }

    @Element(name = "manuscriptReceivedDate")
    public String getManuscriptReceivedDate() {
        return manuscriptReceivedDate;
    }

    @Override
    @Element(name = "shortAuthorsTitle", required = false)
    public String getShortAuthorsTitle() {
        return shortAuthorsTitle;
    }

    @Override
    public boolean isLocal() {
        return isFavorite() || (section != null && section.getIssue() != null && section.getIssue().isLocal());
    }

    @Override
    public boolean isRestricted() {
        return super.isRestricted() && !isLocal();
    }

    public SectionMO getSection() {
        return section;
    }

    public Collection<FigureMO> getFigures() {
        return figures;
    }

    public List<FigureMO> getFiguresSorted() {
        List<FigureMO> result = new ArrayList<FigureMO>(this.figures);
        Collections.sort(result, new Comparator<FigureMO>() {
            @Override
            public int compare(FigureMO lhs, FigureMO rhs) {
                return lhs.getSortIndex() < rhs.getSortIndex() ? -1 : (lhs.getSortIndex() == rhs.getSortIndex() ? 0 : 1);
            }
        });
        return result;
    }

    public void setFigures(Collection<FigureMO> figures) {
        this.figures = figures;
    }

    /**
     * @deprecated shouldn't be set directly because of foreign reference
     */
    public void setSection(final SectionMO section) {
        this.section = section;
    }

    public String getFullTextAbstract() {
        return fullTextAbstract;
    }

    public void setFullTextAbstract(final String fullTextAbstract) {
        this.fullTextAbstract = fullTextAbstract;
    }

    public Collection<ReferenceMO> getReferences() {
        return references;
    }

    public List<ReferenceMO> getReferencesSorted() {
        List<ReferenceMO> result = new ArrayList<>(getReferences());
        Collections.sort(result, new Comparator<ReferenceMO>() {
            @Override
            public int compare(ReferenceMO lhs, ReferenceMO rhs) {
                return lhs.getSortIndex() < rhs.getSortIndex() ? -1 : (lhs.getSortIndex() == rhs.getSortIndex() ? 0 : 1);
            }
        });
        return result;
    }

    public void setReferences(Collection<ReferenceMO> references) {
        this.references = references;
    }

    public Collection<SupportingInfoMO> getSupportingInfoRefs() {
        return supportingInfoRefs;
    }

    public void setSupportingInfoRefs(Collection<SupportingInfoMO> supportingInfoRefs) {
        this.supportingInfoRefs = supportingInfoRefs;
    }

    @Override
    @Element(name = "citation", required = false)
    public String getCitation() {
        return citation;
    }

    public String getRightsLinkUrl() {
        // TODO implement
        return null;
    }

    public IssueMO getParentIssue() {
        return section == null ? null : section.getIssue();
    }

    public String getIssueValue(int which) {
        return getIssueValue(which, null);
    }

    public String getIssueValue(int which, String defValue) {
        if (section == null || section.getIssue() == null) {
            return "";
        }
        if (which == ISSUE_NUMBER) {
            return section.getIssue().getIssueNumber();
        } else if (which == VOLUME_NUMBER) {
            return section.getIssue().getVolumeNumber();
        } else {
            return defValue;
        }
    }

    public String getIndexTerms() {
        return indexTerms;
    }

    public void setIndexTerms(String indexTerms) {
        this.indexTerms = indexTerms;
    }

    public String getAuthorSearchString() {
        return authorSearchString;
    }

    public void setAuthorSearchString(String authorSearchString) {
        this.authorSearchString = authorSearchString;
    }

    public String getFullAuthorList() {
        return fullAuthorList;
    }

    public void setFullAuthorList(String fullAuthorList) {
        this.fullAuthorList = fullAuthorList;
    }

    @Override
    public String getPdfFileName() {
        return format("article_%s.pdf", uid.toString());
    }

    public String getAffiliationBlock() {
        return affiliationBlock;
    }

    public void setAffiliationBlock(String affiliationBlock) {
        this.affiliationBlock = affiliationBlock;
    }

    public boolean isOneAuthor() {
        return oneAuthor;
    }

    public void setOneAuthor(boolean oneAuthor) {
        this.oneAuthor = oneAuthor;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Root(name = "author")
    private static final class Author {
        @Text
        private String author;
    }

    public String getAbstractImageHref() {
        return abstractImageHref;
    }

    public void setAbstractImageHref(String abstractImageHref) {
        this.abstractImageHref = abstractImageHref;
    }
}
