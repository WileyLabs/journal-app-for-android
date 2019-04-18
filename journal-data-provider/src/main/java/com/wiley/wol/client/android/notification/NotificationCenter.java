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

import java.util.Map;

public interface NotificationCenter {

    static final String SETTING_NAME_KEY = "setting_name";
    static final String ERROR = "ERROR";
    static final String APP_ERROR_CODE = "app_error_code";
    static final String ERROR_MESSAGE = "error_message";
    static final String MODE = "mode";
    static final String SPECIAL_SECTION_ID = "special_section_id";
    static final String SUCCESS = "success";
    static final String FILE_PATH = "file_path";
    static final String CANCELLED = "cancelled";
    static final String PROGRESS = "progress";
    static final String IMPORTING = "importing";
    static final String DOI = "doi";
    static final String LOGGED_IN_AFTER_USER_ACTIVATION = "logged_in_after_user_activation";
    static final String NOT_MODIFIED = "not_modified";
    static final String FEED_MO = "feed_mo";
    static final String UID = "uid";
    static final String IGNORE_LAST_MODIFIED = "ignore_last_modified";
    static final String FEED_ITEM_CONTENT = "feed_item_content";
    static final String FEED_ITEM_URL = "feed_item_url";
    static final String IS_HOME_SHOWN = "IS_HOME_SHOWN";
    static final String CURRENT_TAB_ID = "CURRENT_TAB_ID";
    static final String IS_SOCIETY_FAVORITES_SHOWN = "isSocietyFavoritesShown";
    static final String IS_GLOBAL_SEARCH_SHOWN = "isSocietyGlobalSearchShown";
    static final String KEYWORD_JSON = "keyword_json";
    static final String DOI_LIST = "doi_list";
    static final String TITLE_LIST = "title_list";
    static final String ISSUE_DOI = "issue_doi";
    static final String ARTICLE_DOI = "article_doi";
    
    void subscribeToNotification(String eventName, NotificationProcessor processor);

    void unSubscribeFromNotification(NotificationProcessor processor);

    void sendNotification(String eventName, Map<String, Object> params);

    void sendNotification(String eventName);
}