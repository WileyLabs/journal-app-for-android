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
package com.wiley.android.journalApp.fragment.access;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.wiley.android.journalApp.R;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.TPSSiteMO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taraskreknin on 04.08.14.
 */
public class TPSSelectionFragment extends AbstractScreenFragment implements AdapterView.OnItemClickListener {

    private ListView mSitesListView;
    private ArrayAdapter<String> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_tps_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, getSitesNames());
        mSitesListView = findView(R.id.get_access_tps_sites_list);
        mSitesListView.setOnItemClickListener(this);
        mSitesListView.setAdapter(mAdapter);
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getAccessDialogFragment().setSelectedSite(getSiteAtPosition(position));
        getAccessDialogFragment().openTpsLoginScreen();
    }

    @Override
    public void updateUi() {
        super.updateUi();
        mAdapter.clear();
        mAdapter.addAll(getSitesNames());
    }

    @Override
    protected void openPreviousScreen() {
        getAccessDialogFragment().backToScreenB();
    }

    private List<String> getSitesNames() {
        final List<TPSSiteMO> sites = getAccessDialogFragment().getTpsSites();
        final List<String> names = new ArrayList<>(sites.size());
        for (TPSSiteMO site : sites) {
            names.add(site.getTPSName());
        }
        return names;
    }

    private TPSSiteMO getSiteAtPosition(int position) {
        return getAccessDialogFragment().getTpsSites().get(position);
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_TPS_SELECTION;
    }

}
