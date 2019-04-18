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
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.wol.client.android.domain.entity.SupportingInfoMO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;

/**
 * Created by taraskreknin on 16.07.14.
 */
public class SupportingInfoAdapter extends BaseAdapter {

    @Inject
    private LayoutInflater mInflater;

    private final List<SupportingInfoMO> mItems = new ArrayList<>();
    private final static Map<String, Integer> sIconsMap = new ArrayMap<>();
    private final String mFileSizeStringFormat;

    public SupportingInfoAdapter(Context c) {
        RoboGuice.getInjector(c).injectMembersWithoutViews(this);
        registerIcons();
        mFileSizeStringFormat = c.getString(R.string.supporting_info_file_size_format);
    }

    public void setItems(Collection<SupportingInfoMO> newItems) {
        notifyDataSetInvalidated();
        mItems.clear();
        if (newItems != null && !newItems.isEmpty()) {
            mItems.addAll(newItems);
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
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.supporting_info_list_item, parent, false);
            holder = new ViewHolder();
            holder.iconView = (ImageView) view.findViewById(R.id.supporting_info_list_item_icon);
            holder.titleView = (TextView) view.findViewById(R.id.supporting_info_list_item_title);
            holder.sizeView = (TextView) view.findViewById(R.id.supporting_info_list_item_size_mb);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final SupportingInfoMO info = mItems.get(position);
        holder.iconView.setImageResource(getIconResId(info));
        holder.titleView.setText(info.getTitle());
        if (!TextUtils.isEmpty(info.getFileSizeMb()) && !info.getFileSizeMb().equals("0.0")) {
            holder.sizeView.setText(String.format(mFileSizeStringFormat, info.getFileSizeMb()));
        } else {
            holder.sizeView.setVisibility(View.GONE);
        }

        return view;
    }

    private int getIconResId(final SupportingInfoMO info) {
        String ext = null;
        final String mimeType = info.getMimeType();
        int extIndex;
        if (!TextUtils.isEmpty(mimeType)) {
            extIndex = mimeType.lastIndexOf('/');
            ext = extIndex != -1 ? mimeType.substring(extIndex) : null;
        }
        final String title = info.getTitle();
        if (!TextUtils.isEmpty(title)) {
            extIndex = title.lastIndexOf('.');
            ext = extIndex != -1 && extIndex != title.length() - 1 ? title.substring(extIndex + 1) : null;
        }

        if (ext != null && sIconsMap.containsKey(ext)) {
            return sIconsMap.get(ext);
        }
        return R.drawable.icon_file_type_generic;
    }

    private static void registerIcons() {
        if (!sIconsMap.isEmpty()) return;
        sIconsMap.put("3g2", R.drawable.icon_file_type_3g2);
        sIconsMap.put("3gp", R.drawable.icon_file_type_3gp);
        sIconsMap.put("3gp2", R.drawable.icon_file_type_3gp2);
        sIconsMap.put("3gpp", R.drawable.icon_file_type_3gpp);
        sIconsMap.put("aif", R.drawable.icon_file_type_aif);
        sIconsMap.put("aifc", R.drawable.icon_file_type_aifc);
        sIconsMap.put("aiff", R.drawable.icon_file_type_aiff);
        sIconsMap.put("amr", R.drawable.icon_file_type_amr);
        sIconsMap.put("avi", R.drawable.icon_file_type_avi);
        sIconsMap.put("bwf", R.drawable.icon_file_type_bwf);
        sIconsMap.put("cdda", R.drawable.icon_file_type_cdda);
        sIconsMap.put("doc", R.drawable.icon_file_type_doc);
        sIconsMap.put("docx", R.drawable.icon_file_type_docx);
        sIconsMap.put("generic", R.drawable.icon_file_type_generic);
        sIconsMap.put("jpg", R.drawable.icon_file_type_jpg);
        sIconsMap.put("m4a", R.drawable.icon_file_type_m4a);
        sIconsMap.put("m4b", R.drawable.icon_file_type_m4b);
        sIconsMap.put("m4p", R.drawable.icon_file_type_m4p);
        sIconsMap.put("mov", R.drawable.icon_file_type_mov);
        sIconsMap.put("mp3", R.drawable.icon_file_type_mp3);
        sIconsMap.put("mp4", R.drawable.icon_file_type_mp4);
        sIconsMap.put("mpeg", R.drawable.icon_file_type_mpeg);
        sIconsMap.put("mpg", R.drawable.icon_file_type_mpg);
        sIconsMap.put("mqv", R.drawable.icon_file_type_mqv);
        sIconsMap.put("pdf", R.drawable.icon_file_type_pdf);
        sIconsMap.put("png", R.drawable.icon_file_type_png);
        sIconsMap.put("ppt", R.drawable.icon_file_type_ppt);
        sIconsMap.put("ppt", R.drawable.icon_file_type_pptx);
        sIconsMap.put("qt", R.drawable.icon_file_type_qt);
        sIconsMap.put("swa", R.drawable.icon_file_type_swa);
        sIconsMap.put("txt", R.drawable.icon_file_type_txt);
        sIconsMap.put("wav", R.drawable.icon_file_type_wav);
        sIconsMap.put("xls", R.drawable.icon_file_type_xls);
        sIconsMap.put("xlsx", R.drawable.icon_file_type_xlsx);
    }

    private static class ViewHolder {
        TextView titleView, sizeView;
        ImageView iconView;
    }
}
