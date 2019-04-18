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
package com.wiley.android.journalApp.notification;

import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.util.Map;

import static com.wiley.wol.client.android.data.service.ArticleService.ARTICLE_MO;

/**
 * Created by taraskreknin on 18.07.14.
 */
public abstract class ArticleFavoriteStateChangeProcessor implements NotificationProcessor {

    @Override
    public void processNotification(Map<String, Object> params) {
        ArticleMO article = (ArticleMO) params.get(ARTICLE_MO);
        if (!needProcess(article.getUid())) {
            return;
        }
        AppErrorCode error = (AppErrorCode) params.get("error");
        if (error == null) {
            onSuccess(article);
        } else {
            switch (error) {
                case NO_CONNECTION_AVAILABLE:
                    onNoConnection(article);
                    break;
                case ACCESS_FORBIDDEN_ARTICLE:
                    onNoAccess(article);
                    break;
            }
        }
    }

    protected abstract void onSuccess(final ArticleMO article);
    protected void onNoAccess(final ArticleMO article) {}
    protected void onNoConnection(final ArticleMO article) {}
    protected boolean needProcess(final Integer uid) {
        return true;
    }
}
