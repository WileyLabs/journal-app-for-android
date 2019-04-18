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
package com.wiley.android.journalApp.fragment.issue;

import android.view.View;
import android.widget.AdapterView;

import com.wiley.android.journalApp.R;

/**
 * Created by taraskreknin on 23.09.14.
 */
public class IssueTocSectionsFragment10inch extends BaseIssueTocSectionsFragment {

    @Override
    protected void initUi() {
        sectionsListView = findView(R.id.issue_toc_sections_list);
        issueView = findView(R.id.issue);
        setupIssueView(issueView);

        if (theme.isJournalHasNoTocForIssues()) {
            sectionsListView.setVisibility(View.GONE);
        } else {
            sectionsListView.setAdapter(sectionsAdapter);
            sectionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedId = sectionItems.get(position).getId();
                    currentSelectionId = selectedId;
                    sectionSelectedListener.onSectionSelected(selectedId);
                }
            });
        }
    }

}
