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
package com.wiley.android.journalApp.components;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;

import java.util.List;

public class TocComponent extends ArticleRefComponent {

    protected String currentSectionHeader = null;

    private final IssueMO issue;

    public TocComponent(final ArticleComponentHost host, final CustomWebView webView, final IssueMO issue) {
        super(host, webView);
        this.issue = issue;
    }

    @Override
    protected void openArticles(List<DOI> doiList, DOI doiForOpen) {
        final String title = String.format(context.getString(R.string.issue_volume_title), issue.getVolumeNumber(), issue.getIssueNumber());
        ((MainActivity)componentHost.getActivity()).openArticles(doiList, doiForOpen, title, false);
    }

    @Override
    protected String createHTMLFromArticles(final List<ArticleMO> articles, final int startIndex, final int endIndex) {
        currentSectionHeader = null;
        return super.createHTMLFromArticles(articles, startIndex, endIndex);
    }

    @Override
    protected String headingBeforeArticle(final ArticleMO article) {
        String result = null;
        final SectionMO section = article.getSection();
        final String sectionText = section.getName();
        if (!sectionText.equals(currentSectionHeader)) {

            result = templates
                    .useAssetsTemplate(context, "IssueSectionTemplate")
                    .putParam("_section_id_placeholder_", section.getUid())
                    .putParam("_section_heading_placeholder_", section.getName())
                    .putParam("_section_heading_class_placeholder_", "section_heading_class")
                    .putParam("_header_style_placeholder_", theme.isJournalHasDarkBackground() ? "" : "color:#727374")
                    .proceed();

            currentSectionHeader = sectionText;
        }
        return result;
    }

    public void selectSection(final String sectionId) {
        final String elementId = "section_with_id_" + sectionId;
        sendScrollNotifications = false;
        webView.scrollToElement(elementId);
    }

    @Override
    protected boolean needSectionId() {
        return !theme.isJournalHasNoTocForIssues() && DeviceUtils.isTablet(context);
    }

    @Override
    protected boolean hideSectionHeader() {
        return true;
    }
}
