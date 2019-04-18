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
package com.wiley.wol.client.android.data.xml;

import com.wiley.wol.client.android.data.xml.transformer.StringConverter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;

import java.io.InputStream;

/**
 * Created by dfedorov
 * on 02/07/14.
 */
public class SimpleParserImpl implements SimpleParser {
    @Override
    public <T> T parse(InputStream inputStream, Class<T> itemClass) throws Exception {
        final Registry registry = new Registry();
        registry.bind(String.class, StringConverter.class);
        final Strategy strategy = new RegistryStrategy(registry);

        final Serializer serializer = new Persister(strategy);
        return serializer.read(itemClass, inputStream, false);
    }
}
