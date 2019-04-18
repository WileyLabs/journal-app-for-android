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

import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.IssueMO;
import com.wiley.wol.client.android.domain.entity.SectionMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;

import java.util.List;

public interface IssueService {

    public static final String ISSUE_MO = "issue_mo";

    IssueMO getIssue(DOI doi) throws ElementNotFoundException;

    boolean isIssueLoading(DOI doi);

    boolean isIssueUpdating(DOI doi);

    boolean isIssueRemoving(DOI doi);

    void setIssueUpdating(DOI doi, boolean updating);

    void downloadIssue(DOI doi);

    void stopIssueLoading(DOI doi);

    void removeLoadedIssues(List<DOI> dois);

    List<IssueMO> getIssues();

    List<SectionMO> getSectionsAndRequestUpdateForTOC(final DOI doi);

    List<SectionMO> tryGetSectionsForTOC(final DOI doi);

    int getNumOfSavedArticles(final DOI doi);

    long countOf();

    long getOpenedIssuesCount();

    long getDownloadedIssuesCount();

    void saveRefs(List<IssueMO> issues);

    void updateIssueList();
}
