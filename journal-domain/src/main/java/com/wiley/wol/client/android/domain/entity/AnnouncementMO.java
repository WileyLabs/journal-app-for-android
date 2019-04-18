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
import com.j256.ormlite.table.DatabaseTable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Root(name = "announcement")
@DatabaseTable(tableName = "announcement")
public class AnnouncementMO {
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    public static final String IMAGE_PORTRAIT_LOCAL_URL = "image_portrait_local_url";
    public static final String IMAGE_LANDSCAPE_LOCAL_URL = "image_landscape_local_url";
    @Attribute(name = "id")
    @DatabaseField(columnName = "uid", id = true)
    private String uid;
    @Attribute
    @DatabaseField
    private String name;
    @Attribute
    @DatabaseField(columnName = "click_url")
    private String clickUrl;
    @Attribute
    @DatabaseField(columnName = START_DATE, dataType = DataType.DATE)
    private Date startDate;
    @Attribute
    @DatabaseField(columnName = END_DATE, dataType = DataType.DATE)
    private Date endDate;
    @Attribute(name = "lastModified")
    @DatabaseField(columnName = "last_modified_date", dataType = DataType.DATE)
    private Date lastModifiedDate;

    @DatabaseField(columnName = "image_portrait_url")
    private String imagePortraitURL;
    @DatabaseField(columnName = IMAGE_PORTRAIT_LOCAL_URL)
    private String imagePortraitLocalURL;
    @DatabaseField(columnName = "image_portrait_name")
    private String imagePortraitName;
    @DatabaseField(columnName = "image_portrait_width")
    private Integer imagePortraitWidth;
    @DatabaseField(columnName = "image_portrait_height")
    private Integer imagePortraitHeight;

    @DatabaseField(columnName = "image_landscape_url")
    private String imageLandscapeURL;
    @DatabaseField(columnName = IMAGE_LANDSCAPE_LOCAL_URL)
    private String imageLandscapeLocalURL;
    @DatabaseField(columnName = "image_landscape_name")
    private String imageLandscapeName;
    @DatabaseField(columnName = "image_landscape_width")
    private Integer imageLandscapeWidth;
    @DatabaseField(columnName = "image_landscape_height")
    private Integer imageLandscapeHeight;

    @DatabaseField(columnName = "in_feed_date", dataType = DataType.DATE)
    private Date inFeedDate;
    @DatabaseField(columnName = "sort_index")
    private Integer sortIndex;
    @DatabaseField(columnName = "need_to_reload_images")
    private Boolean needToReloadImages;

    @ElementList(entry="image", inline = true, required = false)
    @Path("images")
    private List<Image> imagesList = new ArrayList<>();

    public void initImages(boolean isPhone) {
        for (Image image : imagesList) {
            if ("announce_tablet_landscape".equals(image.name)) {
                imageLandscapeURL = image.url;
                imageLandscapeName = image.name;
                imageLandscapeWidth = image.width;
                imageLandscapeHeight = image.height;
            } else if ((isPhone && "announce_phone_portrait".equals(image.name)) ||
                    (!isPhone && "announce_tablet_portrait".equals(image.name))) {
                imagePortraitURL = image.url;
                imagePortraitName = image.name;
                imagePortraitWidth = image.width;
                imagePortraitHeight = image.height;
            }
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClickUrl() {
        return clickUrl;
    }

    public void setClickUrl(String clickUrl) {
        this.clickUrl = clickUrl;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getImagePortraitURL() {
        return imagePortraitURL;
    }

    public void setImagePortraitURL(String imagePortraitURL) {
        this.imagePortraitURL = imagePortraitURL;
    }

    public String getImagePortraitLocalURL() {
        return imagePortraitLocalURL;
    }

    public void setImagePortraitLocalURL(String imagePortraitLocalURL) {
        this.imagePortraitLocalURL = imagePortraitLocalURL;
    }

    public String getImagePortraitName() {
        return imagePortraitName;
    }

    public void setImagePortraitName(String imagePortraitName) {
        this.imagePortraitName = imagePortraitName;
    }

    public Integer getImagePortraitWidth() {
        return imagePortraitWidth;
    }

    public void setImagePortraitWidth(Integer imagePortraitWidth) {
        this.imagePortraitWidth = imagePortraitWidth;
    }

    public Integer getImagePortraitHeight() {
        return imagePortraitHeight;
    }

    public void setImagePortraitHeight(Integer imagePortraitHeight) {
        this.imagePortraitHeight = imagePortraitHeight;
    }

    public String getImageLandscapeURL() {
        return imageLandscapeURL;
    }

    public void setImageLandscapeURL(String imageLandscapeURL) {
        this.imageLandscapeURL = imageLandscapeURL;
    }

    public String getImageLandscapeLocalURL() {
        return imageLandscapeLocalURL;
    }

    public void setImageLandscapeLocalURL(String imageLandscapeLocalURL) {
        this.imageLandscapeLocalURL = imageLandscapeLocalURL;
    }

    public String getImageLandscapeName() {
        return imageLandscapeName;
    }

    public void setImageLandscapeName(String imageLandscapeName) {
        this.imageLandscapeName = imageLandscapeName;
    }

    public Integer getImageLandscapeWidth() {
        return imageLandscapeWidth;
    }

    public void setImageLandscapeWidth(Integer imageLandscapeWidth) {
        this.imageLandscapeWidth = imageLandscapeWidth;
    }

    public Integer getImageLandscapeHeight() {
        return imageLandscapeHeight;
    }

    public void setImageLandscapeHeight(Integer imageLandscapeHeight) {
        this.imageLandscapeHeight = imageLandscapeHeight;
    }

    public Date getInFeedDate() {
        return inFeedDate;
    }

    public void setInFeedDate(Date inFeedDate) {
        this.inFeedDate = inFeedDate;
    }

    public Integer getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

    public Boolean getNeedToReloadImages() {
        return needToReloadImages;
    }

    public void setNeedToReloadImages(Boolean needToReloadImages) {
        this.needToReloadImages = needToReloadImages;
    }

    @Root(name="image")
    private static final class Image {
        @Attribute
        private String url;
        @Attribute
        private String name;
        @Attribute
        private Integer width;
        @Attribute
        private Integer height;
    }
}
