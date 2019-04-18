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

import com.wiley.android.journalApp.components.MainMenu;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainMenuGroup {
    private String title;
    private final List<MainMenuItem> items = new LinkedList<>();

    public MainMenuGroup() {
    }

    public MainMenuGroup(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<MainMenuItem> getItems() {
        return items;
    }

    public boolean addMenuItem(int location, MainMenuItem newItem) {
        if (null == newItem.getTitle() || newItem.getTitle().equals("")) {
            return false;
        }

        for (MainMenuItem item : items) {
            if (item.getUniqueId().equals(newItem.getUniqueId())) {
                return false;
            }
        }

        items.add(location, newItem);
        return true;
    }

    public void removeItem(String title) {
        final Iterator it = items.iterator();
        while (it.hasNext()) {
            final MainMenuItem item = (MainMenuItem) it.next();
            if (item.getTitle().equals(title)) {
                it.remove();
                return;
            }
        }
    }

    public void clearFeedItems() {
        final Iterator<MainMenuItem> iterator = items.iterator();
        while(iterator.hasNext()) {
            MainMenuItem menuItem = iterator.next();
            if (!MainMenu.GLOBAL_SEARCH.equals(menuItem.getTitle()) &&
                    !MainMenu.SOCIETY_HOME.equals(menuItem.getTitle()) &&
                    !menuItem.getTitle().startsWith(MainMenu.SOCIETY_FAVORITES)) {
                iterator.remove();
            }
        }
    }
}
