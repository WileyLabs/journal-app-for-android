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

import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.popup.PopupHost;

public class PopupFragment extends JournalFragment {

    public static final String Param_HostId = "hostId";

    private int popupHostId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            popupHostId = arguments.getInt(Param_HostId);
        }
    }

    public PopupHost getHost_() {
        JournalActivity journalActivity = getJournalActivity();
        return journalActivity != null ? (PopupHost) journalActivity.findViewById(popupHostId) : null;
    }

    public void hideSelf() {
        PopupHost host = getHost_();
        if (host != null) {
            host.hide();
        }
    }
}
