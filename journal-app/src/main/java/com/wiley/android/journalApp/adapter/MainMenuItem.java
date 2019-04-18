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
package com.wiley.android.journalApp.adapter;

public class MainMenuItem {
    private String uniqueId;
    private String title;
    private Integer imageResourceId;
    private OnClickAction onClickAction;

    public MainMenuItem() {
    }

    public MainMenuItem(String title, Integer imageResourceId, OnClickAction onClickAction) {
        this.title = title;
        this.imageResourceId = imageResourceId;
        this.onClickAction = onClickAction;
    }

    public MainMenuItem(String title, String uniqueId, Integer imageResourceId, OnClickAction onClickAction) {
        this.title = title;
        this.imageResourceId = imageResourceId;
        this.onClickAction = onClickAction;
        this.uniqueId = uniqueId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(Integer imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    public OnClickAction getOnClickAction() {
        return onClickAction;
    }

    public void setOnClickAction(OnClickAction onClickAction) {
        this.onClickAction = onClickAction;
    }

    public interface OnClickAction {
        void onItemClick();
    }

    public String getUniqueId() {
        return uniqueId != null ? uniqueId : title;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
