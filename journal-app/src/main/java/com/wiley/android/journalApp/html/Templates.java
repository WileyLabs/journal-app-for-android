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
package com.wiley.android.journalApp.html;

import android.content.Context;
import android.content.res.Resources;

import com.wiley.android.journalApp.utils.ContentIOUtils;
import com.wiley.android.journalApp.utils.DeviceUtils;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

public class Templates {

    protected TemplateEngine engine = new TemplateEngineIos();
    protected UrlResolver urlResolver = null;

    private static final Map<String, String> assetsTemplates = synchronizedMap(new HashMap<String, String>());

    public Template useAssetsTemplate(final Context context, final String templateName) {
        String template = assetsTemplates.get(templateName);
        if (template == null) {
            String devicePostfix = DeviceUtils.isTablet(context) ? "iPad" : "iPhone";
            String templatePath = String.format("html/HTMLTemplates/%s_%s.html", templateName, devicePostfix);
            if (!ContentIOUtils.hasAssetsContent(context, templatePath))
                templatePath = String.format("html/HTMLTemplates/%s.html", templateName);
            template = ContentIOUtils.getAssetsContent(context, templatePath);
            assetsTemplates.put(templateName, template);
        }
        return useTemplate(context, template);
    }

    public Template useTemplate(Context context, String template) {
        if (urlResolver == null) {
            urlResolver = new UrlResolverAssets(context.getApplicationContext());
        }
        return new Template(template)
                .useEngine(engine)
                .useUrlResolver(urlResolver);
    }
}