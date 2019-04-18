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
import android.text.TextUtils;
import android.view.View;

import com.wiley.android.journalApp.R;
import com.wiley.wol.client.android.data.utils.GANHelper;

public class ScreenCFragment extends BaseCDScreenFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        aanHelper.trackGetAccessDialogueSocietyInfo();
    }
    @Override
    protected String getMessageHtml() {
        String msg;
        if (TextUtils.isEmpty(mSettings.getSocietyLoginInstructions())) {
            msg = getString(R.string.if_you_need_help_message);
        } else {
            msg = mSettings.getSocietyLoginInstructions();
        }
        return addColor(msg);
    }

    @Override
    protected boolean needMoreInfoBlock() {
        return false;
    }

    @Override
    protected boolean needDescriptionMessage() {
        return false;
    }

    @Override
    protected String getGANHelperEvent() {
        return GANHelper.EVENT_GET_ACCESS_C;
    }
}
