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
package com.wiley.android.journalApp.fragment.popups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.utils.BundleUtils;
import com.wiley.wol.client.android.data.utils.AANHelper;

import java.util.List;

public class PopupSelectLink extends PopupFragment {

    public static final String Param_Titles = "titles";
    public static final String Param_Urls = "urls";

    @Inject
    private AANHelper aanHelper;
    @Inject
    private WebController webController;

    private List<String> titles = null;
    private List<String> urls = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        titles = BundleUtils.getListFromBundle(getArguments(), Param_Titles);
        urls = BundleUtils.getListFromBundle(getArguments(), Param_Urls);
        assert(titles.size() == urls.size());
    }

    public static PopupSelectLink newInstance(List<String> titles, List<String> urls) {
        PopupSelectLink fragment = new PopupSelectLink();
        Bundle args = makeArguments(titles, urls);
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle makeArguments(List<String> titles, List<String> urls) {
        Bundle args = new Bundle();
        BundleUtils.putListToBundle(args, Param_Titles, titles);
        BundleUtils.putListToBundle(args, Param_Urls, urls);
        return args;
    }

    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_select_link, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = findView(R.id.values);
        listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, titles));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hideSelf();
                {
                    aanHelper.trackActionOpenWebViewerForReference(urls.get(position));
                }
                webController.openUrlInternal(urls.get(position));
            }
        });
    }
}
