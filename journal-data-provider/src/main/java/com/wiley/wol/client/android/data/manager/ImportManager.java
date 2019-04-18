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
package com.wiley.wol.client.android.data.manager;

import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;

import org.json.JSONObject;

public interface ImportManager {

    void updateEarlyViewFeed();

    void updateIssueList();

    void updateRestrictedFeed(JSONObject knownArticlesJson);

    void updateSpecialSections();

    boolean isUpdatingEarlyView();

    boolean isUpdatingIssueList();

    boolean isUpdatingSpecialSections();

    boolean isUpdating();

    void loadArticle(DOI doi);

    void loadIssue(DOI doi);

    void stopIssueLoading(DOI doi);

    void removeLoadedIssue(DOI doi);

    boolean isIssueLoading(DOI doi);

    void updateIssuesTOC(DOI doi);

    void updateSpecialSection(String id);

    void updateInAppContent();

    void updateSocietyFeed();

    void updateTPSFeed();

    void updateAdvertisementFeed();

    void updateHomePageFeed();

    void updateRssFeed(FeedMO feed);

    void updateFeedItemContent(FeedItemMO feedItem);

    void updateArticleInfoHtmlBody(ArticleMO article);

    void updateAnnouncementFeed();

    void changeKeyword(String keyword, String action);

    void updateListOfSubscribedKeywords();

    void updateAffiliationFeed();
}
