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
package com.wiley.wol.client.android.data.utils;

import android.text.TextUtils;

import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.CitationMO;
import com.wiley.wol.client.android.domain.entity.FigureMO;
import com.wiley.wol.client.android.domain.entity.ReferenceMO;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArticleHtmlUtils {

    public static void prepareArticleFiguresAndFullHtmlBody(ArticleMO article, String articlePath) throws IOException {
        int i = 0;
        for (FigureMO figure : article.getFigures()) {
            figure.setOriginalLocal(articlePath + "/" + figure.getImageRef());
            figure.setSortIndex(i++);
        }

        String body = article.getBody();
        String articleHtmlPath = "file://" + articlePath.replace("%", "%25") + "/";
        if (!TextUtils.isEmpty(article.getAbstractImageHref())) {
            article.setThumbnailLocal(articleHtmlPath + article.getAbstractImageHref());
        }
        body = encloseFiguresForArticleHtml(article, body, articleHtmlPath);
        body = body.replace("imageref:", articleHtmlPath);
        loadTablesAsFigures(article, body);
        loadImageTablesAsFigures(article, body);
        body = body.replace("href=\"#bib", "href=\"openbib://bib");

        final String fullHtmlBodyPath = articlePath + "/article_full_html_body.html";
        IOUtils.write(body, new FileOutputStream(new File(fullHtmlBodyPath)));

        article.setBody(null);
        article.setFullHtmlBody(fullHtmlBodyPath);
    }

    public static String encloseFiguresForArticleHtml(ArticleMO article, String html, String articleHtmlPath) {
        final List<FigureMO> figures = new ArrayList<>(article.getFigures());
        Collections.sort(figures, new Comparator<FigureMO>() {
            @Override
            public int compare(FigureMO lhs, FigureMO rhs) {
                return lhs.getShortCaption().compareTo(rhs.getShortCaption());
            }
        });

        final StringBuilder stringBuilder = new StringBuilder(html.length() + 50 * figures.size());

        int oldPosition = 0;
        int currentPosition = html.indexOf("<div class=\"figure", oldPosition);
        while (currentPosition >= 0) {
            currentPosition += 1;
            final int endIndex = html.indexOf("\">", currentPosition) + 3;
            final String figId = html.substring(html.indexOf("id=\"", currentPosition) + 4, endIndex - 3);
            stringBuilder.append(html.substring(oldPosition, endIndex));
            stringBuilder.append(String.format("<a href=\"openfig://%s\">", figId));

            oldPosition = html.indexOf("</div>", endIndex) + 6;
            stringBuilder.append(html.substring(endIndex, oldPosition));
            stringBuilder.append("</a>");

            currentPosition = html.indexOf("<div class=\"figure", oldPosition);

            final int figCaptionStartIndex = html.indexOf("<p>", endIndex);
            String figCaption = isCaptionExists(currentPosition, figCaptionStartIndex) ?
                    html.substring(figCaptionStartIndex, html.indexOf("</p>", endIndex) + 5) : "";
            int figIndex = indexOfFigureByShortCaption(figures, figId);
            if (figIndex >= 0) {
                figCaption = figCaption.replace("imageref:", articleHtmlPath);
                figures.get(figIndex).setCaption(figCaption);
            }
        }

        stringBuilder.append(html.substring(oldPosition));

        return stringBuilder.toString();
    }

    private static boolean isCaptionExists(int currentPosition, int figCaptionStartIndex) {
        return figCaptionStartIndex >= 0 && (currentPosition < 0 || figCaptionStartIndex < currentPosition);
    }

    public static void loadTablesAsFigures(ArticleMO article, String html) {
        List<FigureMO> figures = new ArrayList<>(article.getFigures());
        Collections.sort(figures, new Comparator<FigureMO>() {
            @Override
            public int compare(FigureMO lhs, FigureMO rhs) {
                return lhs.getShortCaption().compareTo(rhs.getShortCaption());
            }
        });

        // Iterate through all table blocks and assign their content to article.figures array
        Range range = rangeOfString(html, "<div class=\"table");
        while (!range.isEmpty()) {
            // range.location points to start of table block
            Range tableDivRange = new Range();
            tableDivRange.location = range.location;
            String tableString = null;

            // extract figure id
            String figId = null;
            Range idStartRange = rangeOfString(html, "id=\"",
                    new Range(range.location, html.length() - range.location));
            if (!idStartRange.isEmpty()) {
                range = rangeOfString(html, "\"", new Range(idStartRange.location + idStartRange.length, html.length() - (idStartRange.location + idStartRange.length)));
                if (!range.isEmpty())
                    figId = substringWithRange(html, new Range(idStartRange.location + idStartRange.length, range.location - (idStartRange.location + idStartRange.length)));
            }

            if (figId != null) {
                // We have found table block id and now we need to define end of table(s) within this table block
                Range closeTableTagRange = rangeOfString(html, "</table>", new Range(tableDivRange.location, html.length() - tableDivRange.location));

                // NOTE: We may have multiple tables within table block
                // Iterate through all tables within table block to find the last one
                Range nextCloseTableTagRange = new Range(closeTableTagRange);
                while (!nextCloseTableTagRange.isEmpty()) {
                    nextCloseTableTagRange = rangeOfString(html, "</table>",
                            new Range(nextCloseTableTagRange.location + 8, html.length() - closeTableTagRange.location - 8));

                    if (nextCloseTableTagRange.isEmpty()) {
                        break;
                    } else {
                        Range nextRange = rangeOfString(html, "<div class=\"table",
                                new Range(closeTableTagRange.location, html.length() - closeTableTagRange.location));


                        if (!nextRange.isEmpty() && (nextRange.location < nextCloseTableTagRange.location)) {
                            break;
                        }
                    }
                    closeTableTagRange = new Range(nextCloseTableTagRange);
                }

                // We found the last table within table block
                if (!closeTableTagRange.isEmpty()) {
                    tableDivRange.length = closeTableTagRange.location - tableDivRange.location;
                    tableString = substringWithRange(html, tableDivRange);
                }

            }

            if (tableString != null) {
                int figIndex = indexOfFigureByShortCaption(figures, figId);

                if (figIndex >= 0)
                    figures.get(figIndex).setCaption(String.format("%s</table></div>", tableString));
            }

            // Modify range for the next iteration
            range = rangeOfString(html, "<div class=\"table",
                    new Range(range.location + range.length, html.length() - (range.location + range.length)));
        }
    }

    public static void loadImageTablesAsFigures(ArticleMO article, String html) {
        List<FigureMO> figures = new ArrayList<>(article.getFigures());
        Collections.sort(figures, new Comparator<FigureMO>() {
            @Override
            public int compare(FigureMO lhs, FigureMO rhs) {
                return lhs.getShortCaption().compareTo(rhs.getShortCaption());
            }
        });

        Range range = rangeOfString(html, "<div class=\"imageTable");
        while (!range.isEmpty()) {
            Range tableDivRange = new Range();
            tableDivRange.location = range.location;
            String tableString = null;

            // extract figure id
            String figId = null;
            Range idStartRange = rangeOfString(html, "id=\"",
                    new Range(range.location, html.length() - range.location));
            if (!idStartRange.isEmpty()) {
                range = rangeOfString(html, "\"",
                        new Range(idStartRange.location + idStartRange.length, html.length() - (idStartRange.location + idStartRange.length)));
                if (!range.isEmpty())
                    figId = substringWithRange(html, new Range(idStartRange.location + idStartRange.length, range.location - (idStartRange.location + idStartRange.length)));
            }

            if (figId != null) {
                Range closeTableTagRange = rangeOfString(html, "</div>",
                        new Range(tableDivRange.location, html.length() - tableDivRange.location));
                if (!closeTableTagRange.isEmpty()) {
                    tableDivRange.length = closeTableTagRange.location - tableDivRange.location;
                    tableString = substringWithRange(html, tableDivRange);
                }
            }

            if (tableString != null) {
                int figIndex = indexOfFigureByShortCaption(figures, figId);

                if (figIndex >= 0)
                    figures.get(figIndex).setCaption(String.format("%s</div>", tableString));
            }

            range = rangeOfString(html, "<div class=\"imageTable",
                    new Range(range.location + range.length, html.length() - (range.location + range.length)));
        }
    }

    public static String expandLinksInFigureCaption(String html, FigureMO figure, boolean journalHasNoReferenceNumbers) {
        html = expandReferencesInFigureCaption(html, figure, journalHasNoReferenceNumbers);
        html = expandEquationsInFigureCaption(html, figure);
        html = replaceHrefValues(html);
        return html;
    }

    private static String expandReferencesInFigureCaption(String html, FigureMO figure, boolean journalHasNoReferenceNumbers) {
        String ret = html;

        StringBuilder referencesString = new StringBuilder("<div class=\"references\">");

        boolean hasReferences = false;

        List<ReferenceMO> alreadyAddedRefs = new ArrayList<>();

        Matcher regExp = Pattern.compile("<a\\s+href=\\\"openbib:\\/\\/([\\w\\d-]+)\\\"").matcher(ret);

        while (regExp.find()) {
            String refID = regExp.group(1);
            ReferenceMO prevRef = null;

            List<ReferenceMO> refs = figure.getArticle().getReferencesSorted();

            for (ReferenceMO ref : refs) {
                if (alreadyAddedRefs.contains(ref)) {
                    prevRef = null;
                    continue;
                }

                if (refID.equals(ref.getId()) || (prevRef != null && ref.getId().startsWith(prevRef.getId()))) {
                    if (refID.equals(ref.getId())) {
                        prevRef = ref;
                    }
                    alreadyAddedRefs.add(ref);
                    List<CitationMO> cits = ref.getCitationSorted();
                    StringBuilder citsHtml = null;

                    for (CitationMO cit : cits) {
                        if ((cit.getText() == null || cit.getText().trim().length() == 0)
                                && cit.getLinks() == null
                                && TextUtils.isEmpty(cit.getLinkToWOL())) {
                            continue;
                        }

                        if (citsHtml == null)
                            citsHtml = new StringBuilder();

                        citsHtml.append(String.format("%s<br />", cit.getText()));

                        if (cit.getLinks() != null) {
                            if (!TextUtils.isEmpty(cit.getLinkToWOL())) {
                                citsHtml.append(String.format("| <a href=\"%s\">Open on Wiley Online Library</a> |<br/>", cit.getLinkToWOL()));
                            } else {
                                Map<String, Object> links = cit.getLinksMap();
                                for (CitationUtils.LinkType citationLinkType : CitationUtils.LinkType.values()) {
                                    String citationAbbr = CitationUtils.getLinkString(citationLinkType);
                                    if (links.containsKey(citationAbbr)) {
                                        String citationId = null;
                                        if (citationLinkType == CitationUtils.LinkType.ISI)
                                            citationId = ((Map<String, String>) links.get(citationAbbr)).get("ISI_ID");
                                        else
                                            citationId = (String) links.get(citationAbbr);
                                        String url = String.format("http://onlinelibrary.wiley.com/resolve/reference/%s?id=%s", citationAbbr, citationId);
                                        String citationTitle = CitationUtils.getLinkTitle(citationLinkType);
                                        citsHtml.append(String.format("| <a href=\"%s\">%s </a> ", url, citationTitle));
                                    }
                                }
                            }
                        }
                    }

                    if (citsHtml != null) {
                        referencesString.append(String.format("<a name=\"%s\"></a>", refID));
                        if (journalHasNoReferenceNumbers) {
                            referencesString.append("<table class=\"main_table_class\" style=\"margin-top:8px;\"><tr><td>");
                        } else {
                            referencesString.append(String.format("<table class=\"main_table_class\"><tr><td style=\"padding-right:20px;\"><b>%s.</b></td><td>", ref.getTitle()));
                        }
                        referencesString.append(citsHtml);
                        hasReferences = true;
                        referencesString.append("</td></tr></table>");
                    }
                } else {
                    prevRef = null;
                }
            }
        }

        referencesString.append("</div>");

        if (hasReferences) {
            ret = ret + referencesString;
        }

        return ret;
    }

    private static String expandEquationsInFigureCaption(String html, FigureMO figure) {
        String ret = html;

        if (ret == null || ret.length() == 0) {
            return ret;
        }

        List<String> eqIds = new ArrayList<>();

        Matcher eqLinks = Pattern.compile("<a href=\"#(\\S+?)\"(?=[^>]+class=\"eqnLink\")").matcher(ret);
        while (eqLinks.find()) {
            String eqId = eqLinks.group(1);
            eqIds.add(eqId);
        }

        if (eqIds.size() > 0) {
            StringBuilder equations = new StringBuilder();
            equations.append("<div class=\"equations\">");

            String articleHtml = figure.getArticle().getFullHtmlBody();

            for (String eqId : eqIds) {
                Pattern pattern = Pattern.compile(String.format("<div class=\"equation\" id=\"%s\">.+?</div>", eqId));

                if (articleHtml != null && articleHtml.length() > 0) {
                    Matcher foundEquations = pattern.matcher(articleHtml);
                    while (foundEquations.find()) {
                        String equation = foundEquations.group(0);
                        equations.append(String.format("<a name=\"%s\"></a>%s", eqId, equation));
                    }
                }
            }

            equations.append("</div>");

            ret = ret + equations.toString();
        }

        return ret;
    }

    private static String replaceHrefValues(String html) {
        if (html == null) {
            return html;
        }
        String ret = html;
        ret = ret.replaceAll("<a href=\"#(\\S+?)\"(?=[^>]+class=\"(figureLink|tableLink|sectionLink)\")", "<a href=\"openfig://$1\"");
        ret = ret.replaceAll("openbib://", "#");
        return ret;
    }

    public static String disableAllLinksIn(String html) {
        return html.replace("<a href=\"#", "<a class=\"disabled_class\" href=\"#");
    }

    private static class Range {
        public int location;
        public int length;

        public Range(int location, int length) {
            this.location = location;
            this.length = length;
        }

        public Range() {
            this.location = 0;
            this.length = 0;
        }

        public Range(Range other) {
            this.location = other.location;
            this.length = other.length;
        }

        public boolean isEmpty() {
            return length == 0;
        }

        public static final Range Empty = new Range(-1, 0);
    }

    private static Range rangeOfString(String str, String target) {
        int location = str.indexOf(target);
        if (location >= 0) {
            return new Range(location, target.length());
        } else {
            return Range.Empty;
        }
    }

    private static Range rangeOfString(String str, String target, Range inRange) {
        if (inRange.length == 0)
            return Range.Empty;
        int location = str.indexOf(target, inRange.location);
        if (location >= 0 && location < (inRange.location + inRange.length)) {
            return new Range(location, target.length());
        } else {
            return Range.Empty;
        }
    }

    private static String substringWithRange(String str, Range range) {
        return str.substring(range.location, range.location + range.length);
    }

    private static String insertString(String str, String target, int position) {
        return str.substring(0, position) + target + str.substring(position);
    }

    private static int indexOfFigureByShortCaption(List<FigureMO> figures, String shortCaption) {
        for (int i = 0; i < figures.size(); i++) {
            if (shortCaption.equals(figures.get(i).getShortCaption()))
                return i;
        }
        return -1;
    }
}
