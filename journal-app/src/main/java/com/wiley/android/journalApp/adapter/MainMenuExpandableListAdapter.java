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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.components.MainMenu;

import java.util.LinkedList;
import java.util.List;

public class MainMenuExpandableListAdapter extends BaseExpandableListAdapter {
    private List<MainMenuGroup> mainMenuGroups;
    private Context context;
    public final MainMenu.HighlightMenuHelper highlightMenuHelper;

    public MainMenuExpandableListAdapter(Context context, MainMenu.HighlightMenuHelper highlightMenuHelper) {
        this.context = context;
        this.highlightMenuHelper = highlightMenuHelper;
    }

    @Override
    public int getGroupCount() {
        return mainMenuGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mainMenuGroups.get(groupPosition).getItems().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mainMenuGroups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mainMenuGroups.get(groupPosition).getItems().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        if (groupPosition >= mainMenuGroups.size()) {
            return -1;
        }

        return mainMenuGroups.get(groupPosition).hashCode();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return mainMenuGroups.get(groupPosition).getItems().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.main_menu_group, parent, false);
        }

        final TextView groupName = (TextView) v.findViewById(R.id.groupName);

        final MainMenuGroup group = mainMenuGroups.get(groupPosition);

        ImageView indicator = (ImageView) v.findViewById(R.id.group_indicator);
        if (isExpanded) {
            indicator.setImageResource(R.drawable.menu_triangle_open);
        } else {
            indicator.setImageResource(R.drawable.menu_triangle_close);
        }

        groupName.setText(group.getTitle());

        return v;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.main_menu_item, parent, false);
        }

        TextView itemName = (TextView) v.findViewById(R.id.itemName);
        ImageView itemIcon = (ImageView) v.findViewById(R.id.itemIcon);

        MainMenuItem menuItem = mainMenuGroups.get(groupPosition).getItems().get(childPosition);

        itemName.setText(menuItem.getTitle());
        if (menuItem.getImageResourceId() != null) {
            itemIcon.setImageResource(menuItem.getImageResourceId());
        } else {
            itemIcon.setImageResource(R.drawable.transparent_drawable);
        }

        final View separator = v.findViewById(R.id.group_separator);
        if (isLastChild && groupPosition != (mainMenuGroups.size() - 1)) {
            separator.setVisibility(View.VISIBLE);
        } else {
            separator.setVisibility(View.GONE);
        }

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    highlightMenuHelper.highlightMenuItem(groupPosition, childPosition);
                }

                return false;
            }
        });

        return v;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void addGroup(int location, MainMenuGroup mainMenuGroup) {
        if (mainMenuGroups == null) {
            mainMenuGroups = new LinkedList<>();
        }

        if (!mainMenuGroups.contains(mainMenuGroup)) {
            mainMenuGroups.add(location, mainMenuGroup);
        }
    }

    public void removeGroup(MainMenuGroup mainMenuGroup) {
        mainMenuGroups.remove(mainMenuGroup);
    }

    public boolean isGroupExists(MainMenuGroup mainMenuGroup) {
        return mainMenuGroup != null && mainMenuGroups.contains(mainMenuGroup);
    }
}
