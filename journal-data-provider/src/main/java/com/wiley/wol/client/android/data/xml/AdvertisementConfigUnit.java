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

public class AdvertisementConfigUnit {

    public static final int minOtherAdArticleView = 2;

    private String adUnitId;
    private Integer firstAdArticleView;
    private Integer otherAdArticleView;
    private String sponsoredAdUnitId;

    public String getAdUnitId() {
        return adUnitId;
    }

    public void setAdUnitId(final String adUnitId) {
        this.adUnitId = adUnitId;
    }

    public Integer getFirstAdArticleView() {
        return firstAdArticleView;
    }

    public void setFirstAdArticleView(final Integer firstAdArticleView) {
        this.firstAdArticleView = firstAdArticleView;
    }

    public boolean needShowAd() {
        return adUnitId != null && firstAdArticleView >= 0 && otherAdArticleView > 0;
    }

    public Integer getOtherAdArticleView() {
        return otherAdArticleView;
    }

    public Integer getRealOtherAdArticleView() {
        Integer articleView = getOtherAdArticleView();
        if (articleView == null)
            return null;
        if (articleView.intValue() < minOtherAdArticleView)
            return minOtherAdArticleView;
        return articleView;
    }

    public void setOtherAdArticleView(final Integer otherAdArticleView) {
        this.otherAdArticleView = otherAdArticleView;
    }

    public String getSponsoredAdUnitId() {
        return sponsoredAdUnitId;
    }

    public void setSponsoredAdUnitId(final String sponsoredAdUnitId) {
        this.sponsoredAdUnitId = sponsoredAdUnitId;
    }
}
