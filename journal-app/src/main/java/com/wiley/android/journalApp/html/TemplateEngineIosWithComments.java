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

import java.util.Collection;
import java.util.Map;

/**
 * Created by taraskreknin on 16.06.14.
 */
public class TemplateEngineIosWithComments extends TemplateEngineIos {

    @Override
    public String proceed(String template, Map<String, String> params, UrlResolver urlResolver) {
        String superResult = super.proceed(template, params, urlResolver);

        Collection<String> keysList = params.keySet();
        for (String key : keysList) {
            if (key.endsWith("_COMMENT") && "block".equals(params.get(key))) {
                superResult = uncomment(key, superResult);
            }
        }

        superResult = superResult.replaceAll("(?s)<!--.*?-->", "");

        return superResult;
    }

    private String uncomment(String commentName, String s) {
        final String regExp = "<!--" + commentName + "|" + commentName + "-->";
        return s.replaceAll(regExp, "");
    }

}
