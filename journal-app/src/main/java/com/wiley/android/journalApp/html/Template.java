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

import java.util.HashMap;
import java.util.Map;

/**
* Created by Andrey Rylov on 12/05/14.
*/
public class Template {
    private final String template;
    private final Map<String, String> params = new HashMap<String, String>();
    private UrlResolver urlResolver = null;
    private TemplateEngine engine = null;

    Template(final String template) {
        this.template = template;
    }

    public Template putParam(final String key, final String value) {
        String realKey = key;
        if (realKey.startsWith("@@"))
            realKey = realKey.substring(2);
        if (realKey.endsWith("@@"))
            realKey = realKey.substring(0, realKey.length() - 2);

        params.put(realKey, value == null ? "" : value);
        return this;
    }

    public Template putParam(final String key, final int value) {
        return this.putParam(key, Integer.toString(value));
    }

    public Template reset() {
        params.clear();
        return this;
    }

    public Template useUrlResolver(UrlResolver urlResolver) {
        this.urlResolver = urlResolver;
        return this;
    }

    public Template useEngine(TemplateEngine engine) {
        this.engine = engine;
        return this;
    }

    public String proceed() {
        assert(this.engine != null);
        String result = this.engine.proceed(this.template, this.params, this.urlResolver);
        return result;
    }
}
