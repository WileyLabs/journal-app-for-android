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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collection;

import static java.lang.Integer.parseInt;

@Root(name = "inAppContents")
public class AdvertisementConfigContainer {

    @ElementList(inline = true, type = AdvertisementConfigEntry.class, empty = false)
    private Collection<AdvertisementConfigEntry> advertisementConfigEntries;

    public Collection<AdvertisementConfigEntry> getAdvertisementConfigEntries() {
        return advertisementConfigEntries;
    }

    public AdvertisementConfigUnit getConfigUnit() {
        final AdvertisementConfigUnit advertisementConfigUnit = new AdvertisementConfigUnit();
        for (final AdvertisementConfigEntry advertisementConfigEntry : advertisementConfigEntries) {
            final String key = advertisementConfigEntry.getKey() != null ? advertisementConfigEntry.getKey().toUpperCase() : "";
            switch (key) {
                case "AD_UNIT_ID":
                    advertisementConfigUnit.setAdUnitId(advertisementConfigEntry.getContent().trim());
                    break;
                case "FIRST_AD_ARTICLE_VIEW":
                    advertisementConfigUnit.setFirstAdArticleView(parseInt(advertisementConfigEntry.getContent().trim()));
                    break;
                case "OTHER_AD_ARTICLE_VIEW":
                    advertisementConfigUnit.setOtherAdArticleView(parseInt(advertisementConfigEntry.getContent().trim()));
                    break;
                case "SPONSORED_AD_UNIT_ID":
                    advertisementConfigUnit.setSponsoredAdUnitId(advertisementConfigEntry.getContent().trim());
                    break;
            }
        }

        return advertisementConfigUnit;
    }
}

@Root(name = "inAppContent")
class AdvertisementConfigEntry {
    @Attribute(name = "key")
    private String key;
    @Element(name = "content", data = true, required = false)
    private String content;

    public String getKey() {
        return key;
    }

    public String getContent() {
        return null == content ? "" : content;
    }
}
