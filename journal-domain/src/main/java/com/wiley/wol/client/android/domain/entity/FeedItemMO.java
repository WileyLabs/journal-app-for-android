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
package com.wiley.wol.client.android.domain.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

/**
 * Created by taraskreknin on 02.10.14.
 */
public class FeedItemMO extends BaseRssMO {
    public static final String ADDED_TO_FAVORITES_DATE_COLUMN = "added_to_favorites_date";
    public static final String FAVORITE_COLUMN = "favorite";
    @DatabaseField
    private String author;
    @DatabaseField(columnName = "need_to_check_image")
    private Boolean needToCheckImage;
    @DatabaseField
    private String url;
    @DatabaseField(columnName = "description")
    private String descr;
    @DatabaseField(columnName = "pub_date", dataType = DataType.DATE)
    private Date pubDate;
    @DatabaseField(columnName = "date_string")
    private String dateString;
    @DatabaseField(columnName = "need_to_check")
    private boolean needToCheck;
    @DatabaseField(columnName = "hash_string")
    private String hashString;
    @DatabaseField(columnName = "image_link")
    private String imageLink;
    @DatabaseField(columnName = "plain_descr")
    private String plainDescr;
    @DatabaseField(columnName = "image_lmd")
    private Date imageLMD;
    @DatabaseField(columnName = "image_local")
    private String imageLocal;
    @DatabaseField(columnName = "feed_uid", foreign = true, foreignAutoRefresh = true)
    private FeedMO feed;
    @DatabaseField(columnName = FAVORITE_COLUMN)
    private Boolean isFavorite;
    @DatabaseField(columnName = ADDED_TO_FAVORITES_DATE_COLUMN)
    private Date addedToFavoritesDate;
    @DatabaseField(columnName = "relevance")
    private int relevance;
    @DatabaseField(columnName = "search_sort_order")
    private int searchSortOrder;

    public String getAuthor() {
        return author;
    }

    public Boolean getNeedToCheckImage() {
        return needToCheckImage;
    }

    public String getUrl() {
        return url;
    }

    public String getDescr() {
        return descr;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public String getDateString() {
        return dateString;
    }

    public Boolean getNeedToCheck() {
        return needToCheck;
    }

    public String getHashString() {
        return hashString;
    }

    public String getImageLink() {
        return imageLink;
    }

    public String getPlainDescr() {
        return plainDescr;
    }

    public Date getImageLMD() {
        return imageLMD;
    }

    public String getImageLocal() {
        return imageLocal;
    }

    public FeedMO getFeed() {
        return feed;
    }

    public Boolean isFavorite() {
        return isFavorite == null ? false : isFavorite;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public void setFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public void setFeed(FeedMO feed) {
        this.feed = feed;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRelevance() {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }

    public Date getAddedToFavoritesDate() {
        return addedToFavoritesDate;
    }

    public void setAddedToFavoritesDate(final Date addedToFavoritesDate) {
        this.addedToFavoritesDate = addedToFavoritesDate;
    }
}
