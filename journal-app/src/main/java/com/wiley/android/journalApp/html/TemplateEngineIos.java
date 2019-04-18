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

import android.text.TextUtils;

import java.util.Map;

/**
 * Created by Andrey Rylov on 12/05/14.
 */
public class TemplateEngineIos implements TemplateEngine {

    @Override
    public String proceed(String template, Map<String, String> params, UrlResolver urlResolver) {
        if (TextUtils.isEmpty(template))
            return template;

        final StringBuilder builder = new StringBuilder(template.length());

        int macrosEnd = -1;
        int macrosStart = template.indexOf("@@");
        if (macrosStart < 0)
            return template;

        while (macrosStart >= 0) {
            final String stringToAppend;
            if (macrosEnd >= 0) {
                stringToAppend = template.substring(macrosEnd + 2, macrosStart);
            } else {
                stringToAppend = template.substring(0, macrosStart);
            }
            builder.append(stringToAppend);

            macrosEnd = template.indexOf("@@", macrosStart + 2);
            if (macrosEnd < 0) {
                builder.append(template.substring(macrosStart));
                break;
            }

            String macros = template.substring(macrosStart + 2, macrosEnd);
            String value = proceedMacros(macros, params, urlResolver);
            if (value == null) {
                builder.append("@@");
                builder.append(macros);
                builder.append("@@");
            } else {
                builder.append(value);
            }

            macrosStart = template.indexOf("@@", macrosEnd + 2);
        }

        if (macrosEnd >= 0)
            builder.append(template.substring(macrosEnd + 2));

        return builder.toString();
    }

    protected String proceedMacros(String macros, Map<String, String> params, UrlResolver urlResolver) {
        String value = null;
        if (macros.startsWith("!")) {
            if (urlResolver != null) {
                String key = macros.substring(1);
                value = urlResolver.resolve(key);
            }
        } else {
            value = params.get(macros);
        }
        return value;
    }
}
