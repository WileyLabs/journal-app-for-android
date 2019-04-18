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
package com.wiley.wol.client.android.error;

public enum AppErrorCode {

    UNDEFINED(1),
    IO_EXCEPTION(2),

    //XML/content errors
    XML_PARSE_ERROR(100),
    WRONG_DOI(101),
    FAIL_TO_OPEN_ARCHIVE(102),
    EMPTY_ARCHIVE(103),

    //Server errors
    ACCESS_FORBIDDEN(200),
    ACCESS_FORBIDDEN_ISSUE(201),
    ACCESS_FORBIDDEN_ARTICLE(202),
    ACCESS_FORBIDDEN_APP(203),
    SERVER_ERROR_MESSAGE(204),
    SERVER_ERROR(205),

    //Param errors
    FILENAME_IS_NULL(300),

    //InApp purchase errors
    FAIL_TO_GET_PRODUCT(400),
    TRANSACTION_CANCELED(401),
    PURCHASE_TURNED_OFF(402),
    NO_PREVIOUS_SUBSCRIPTION(403),

    NO_CONNECTION_AVAILABLE(500),
    NO_CONNECTION_AVAILABLE_TO_SAVE_ISSUE(501),
    NO_CONNECTION_AVAILABLE_TO_SAVE_ARTICLE(502),
    NO_CONNECTION_AVAILABLE_FOR_FIRST_LAUNCH(503),

    // OAuth errors
    AUTHORIZATION_FAILED(600),
    CONTENT_LOCKED_FOR_WOL(601),
    CONTENT_LOCKED_FOR_ANDROID(602),

    //Document download errors
    FAIL_TO_GET_DOCUMENT(700),
    FAIL_TO_AUTHORISE_DOCUMENT(701),

    //TPS errors
    TPS_NO_RESPONSE(800),
    TPS_UNKNOWN_RESPONSE(801),
    TPS_WRONG_PASSWORD(802),
    TPS_OAUTH_FAILED(803),
    TPS_NO_ACCESS(804),

    ISSUE_IS_NOT_AVAILABLE_OFFLINE(900),
    NO_FEED_AVAILABLE(901);

    private final int errorCode;

    AppErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}