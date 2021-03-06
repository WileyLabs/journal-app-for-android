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
package com.wiley.wol.client.android.notification;

public enum EventList {
    EARLY_VIEW_FEED_UPDATED,
    EARLY_VIEW_FEED_NOT_MODIFIED,
    EARLY_VIEW_FEED_ERROR,
    SPECIAL_SECTION_FEED_UPDATED,
    SPECIAL_SECTION_FEED_NOT_MODIFIED,
    SPECIAL_SECTION_FEED_ERROR,
    ISSUE_LIST_UPDATED,
    ISSUE_LIST_NOT_MODIFIED,
    ISSUE_LIST_ERROR,
    ISSUE_LIST_NEED_UPDATE,
    IN_APP_ERROR,
    IN_APP_NEED_UPDATE,
    IN_APP_CONTENT_UPDATED,
    SPECIAL_SECTIONS_UPDATED,
    SPECIAL_SECTIONS_NOT_MODIFIED,
    SPECIAL_SECTIONS_ERROR,
    ARTICLE_UPDATE_SUCCESS,
    ARTICLE_UPDATE_ERROR,
    ARTICLE_FAVORITE_STATE_CHANGED,
    ARTICLE_MARK_AS_READ,
    INFO_ARTICLE_UPDATE_FINISHED,
    KEYWORD_UPDATE_STARTED,
    KEYWORD_UPDATE_FINISHED,
    KEYWORDS_UPDATED,
    KEYWORDS_DEVICE_REGISTERED_ON_MCS,
    ISSUE_FAVORITES_COUNT_CHANGED,
    SOCIETY_FAVORITES_COUNT_CHANGED,
    ISSUE_TOC_UPDATE_SUCCESS,
    ISSUE_TOC_UPDATE_NOT_MODIFIED,
    ISSUE_TOC_UPDATE_ERROR,
    SETTINGS_CHANGED,
    AUTH_SUCCESS,
    AUTH_ERROR,
    SECTION_DID_BECOME_VISIBLE,
    DOCUMENT_DOWNLOAD_STARTED,
    DOCUMENT_DOWNLOAD_PROGRESS,
    DOCUMENT_DOWNLOAD_FINISHED,
    SOCIETY_UPDATED_SUCCESS,
    SOCIETY_UPDATED_NOT_MODIFIED,
    SOCIETY_UPDATED_ERROR,
    TPS_SITES_UPDATED_SUCCESS,
    TPS_SITES_UPDATED_NOT_MODIFIED,
    TPS_SITES_UPDATED_ERROR,
    ERROR_RESOURCE_DOWNLOADING,
    PROCESS_RESOURCE_DOWNLOADING,
    DONE_RESOURCE_DOWNLOADING,
    NETWORK_STATE_CHANGED,
    ISSUE_DOWNLOAD_STARTED,
    ISSUE_DOWNLOAD_SUCCESS,
    ISSUE_DOWNLOAD_ERROR,
    ISSUE_DOWNLOAD_PROGRESS,
    ISSUE_DOWNLOAD_CANCEL,
    ISSUE_REMOVED,
    ALL_CONTENT_UPDATE_STARTED,
    ALL_CONTENT_UPDATE_FINISHED,
    ADV_FEED_UPDATE_SUCCESS,
    ADV_FEED_UPDATE_ERROR,
    ADV_FEED_UPDATE_NOT_MODIFIED,
    HOME_PAGE_FEEDS_UPDATE_STARTED,
    HOME_PAGE_FEEDS_UPDATE_FINISHED,
    HOME_FEED_UPDATED_SUCCESS,
    HOME_FEED_UPDATED_NOT_MODIFIED,
    HOME_FEED_UPDATED_ERROR,
    RSS_FEED_UPDATE_STARTED,
    RSS_FEED_UPDATE_COMPLETED,
    ANNOUNCEMENTS_UPDATE_SUCCESS,
    ANNOUNCEMENTS_UPDATE_NOT_MODIFIED,
    ANNOUNCEMENTS_UPDATE_ERROR,
    FEED_ITEM_CONTENT_STARTED,
    FEED_ITEM_CONTENT_SUCCESS,
    FEED_ITEM_CONTENT_NOT_MODIFIED,
    FEED_ITEM_CONTENT_ERROR,
    AFFILIATION_INFO_CHANGED,
    AFFILIATION_INFO_UPDATE_FINISHED,
    JOURNAL_PAGE_NAVIGATED,
    SOCIETY_PAGE_NAVIGATED,
    MENU_ITEM_COUNT_CHANGED,
    MENU_BUTTON_IS_SHOWN,
    IN_APP_BILLING_PURCHASE_SUBS_SUCCESS,
    IN_APP_BILLING_PURCHASE_SUBS_ERROR,
    IN_APP_BILLING_LOAD_PURCHASES_COMPLETED,
    IN_APP_BILLING_CHECK_MCS_PURCHASES_COMPLETED,
    PUSH_NOTIFICATION_OPEN_EARLY_VIEW_ARTICLE,
    PUSH_NOTIFICATION_OPEN_ISSUE_ARTICLE,
    PUSH_NOTIFICATION_OPEN_SPECIAL_SECTION_ARTICLE,
    PUSH_NOTIFICATION_OPEN_ISSUE,
    AFFILIATION_INFO_NEED_UPDATE,
    MAIN_ACTIVITY_IS_SHOWN,
    SETTINGS_WINDOW_IS_SHOWN;

    public String getEventName() {
        return this.name();
    }
}
