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
package com.wiley.android.journalApp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.domain.entity.FigureMO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;

public class FiguresAdapter extends BaseAdapter {

    private final Context mContext;
    @Inject
    private ImageLoaderHelper mImageLoader;
    @Inject
    private LayoutInflater mInflater;
    private final List<FigureMO> mItems = new ArrayList<>();

    public FiguresAdapter(Context context) {
        mContext = context.getApplicationContext();
        final RoboInjector injector = RoboGuice.getInjector(mContext);
        injector.injectMembersWithoutViews(this);
    }

    public Collection<FigureMO> getFigures() {
        return mItems;
    }

    public void setFigures(Collection<FigureMO> figures) {
        notifyDataSetInvalidated();
        mItems.clear();
        if (figures != null) {
            mItems.addAll(figures);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FigureMO figure = mItems.get(position);
        View rowView = convertView;
        if (rowView == null) {
            rowView = mInflater.inflate(R.layout.slider_panel_figures_layout, parent, false);
        }
        ImageView imageView = (ImageView) rowView.findViewById(R.id.slider_panel_figure);
        TextView figTitle = (TextView) rowView.findViewById(R.id.slider_panel_figure_title);
        figTitle.setText(mItems.get(position).getTitle());

        if ("figure".equals(figure.getKind())) {
            mImageLoader.displayImage("file:/" + figure.getOriginalLocal(), imageView);
        } else {
            imageView.setImageResource(R.drawable.fig_table);
        }

        return rowView;
    }

}