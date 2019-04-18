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
package com.wiley.wol.client.android.data.service;

import com.wiley.wol.client.android.data.http.DownloadManager;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;
import com.wiley.wol.client.android.settings.AuthToken;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by alobachev on 7/10/14.
 */
public interface AuthorizationService {

    boolean isUpdateContentAfterAuthorisationExpected();

    void setUpdateContentAfterAuthorisationExpected(boolean isUpdateContentAfterAuthorisationExpected);

    class LoggedInInformation implements Serializable {
        public final int lastSuccessfulLoginScreen;
        public final boolean viaTps;
        public final String tpsSiteName;

        public LoggedInInformation(String tpsSiteName, int screen) {
            this.tpsSiteName = tpsSiteName;
            this.viaTps = true;
            this.lastSuccessfulLoginScreen = screen;
        }

        public LoggedInInformation(int screen) {
            this.tpsSiteName = null;
            this.viaTps = false;
            this.lastSuccessfulLoginScreen = screen;
        }
    }

    class AccessCodeInformation implements Serializable {
        public final String code;
        public final Date expirationDate;

        public AccessCodeInformation(String code, Date expirationDate) {
            this.code = code;
            this.expirationDate = expirationDate;
        }

        public boolean hasExpiration() {
            return expirationDate != null;
        }

        public boolean isExpired() {
            return daysToExpiration() < 0;
        }

        private static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;

        public int daysToExpiration() {
            if (expirationDate == null) {
                return 0;
            }
            Date now = new Date();
            long diff = (expirationDate.getTime() / MILLIS_IN_DAY) - (now.getTime() / MILLIS_IN_DAY);
            return (int) diff;
        }
    }

    boolean hasSociety();

    String getSocietyLoginInstructions();

    String getSocietyUrl();

    String getSocietyInformation();

    boolean hasTPS();

    String getTPSUsername();

    String getTPSPassword();

    int getTPSTimeout();

    List<TPSSiteMO> getAllTPSSites();

    void saveLastLoginInformation(LoggedInInformation info);

    LoggedInInformation getLastLoginInformation();

    void saveAuthToken(AuthToken token);

    void clearAuthToken();

    AccessCodeInformation getAccessCodeInformation();

    boolean hasAccessCode();

    boolean checkAccessCode(String code);

    DownloadManager.JsonResponse useAccessCode(String code);

    class NewUserData {
        public String firstName;
        public String lastName;
        public String email;
        public String password;
        public String accessCode;
    }

    enum RegisterNewUserResult {
        Success,
        NetworkError,
        EmailError
    }

    RegisterNewUserResult registerNewUser(NewUserData data);

    boolean activateUser(String query);
}
