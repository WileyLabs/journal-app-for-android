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

import android.text.TextUtils;

import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.html.Template;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.SpecialSectionMO;

import java.util.List;

/**
 * Created by taraskreknin on 16.07.14.
 */
public class SpecialSectionArticlesComponent extends ArticleRefComponent {

    private SpecialSectionMO section;

    public SpecialSectionArticlesComponent(SpecialSectionMO section, ArticleComponentHost host, CustomWebView webView) {
        super(host, webView);
        this.section = section;
    }

    @Override
    protected String htmlCodeForListHeading() {
        String result = "";

        String realDescr = HtmlUtils.stripHtml(section.getDesc());

        final Template template = templates.useAssetsTemplate(context, "special_section_heading_template" + (DeviceUtils.isTablet(context) ? "_iPad" : "_iPhone"));
        String html = template
                .putParam("show_special_section_heading", TextUtils.isEmpty(realDescr) ? "none" : "block")
                .putParam("special_section_heading_placeholder", !TextUtils.isEmpty(section.getDesc()) ? section.getDesc() : "")
                .proceed();
        return result + html;
    }

    @Override
    protected void openArticles(List<DOI> doiList, DOI doiForOpen) {
        final String title = this.section.getUnescapedTitle();
        ((MainActivity)componentHost.getActivity()).openArticles(doiList, doiForOpen, title, false);
    }

    public void updateSection(SpecialSectionMO section) {
        this.section = section;
    }

    public SpecialSectionMO getSection() {
        return section;
    }
}
