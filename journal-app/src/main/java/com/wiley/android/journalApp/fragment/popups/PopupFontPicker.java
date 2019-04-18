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
import android.widget.SeekBar;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.settings.Settings;

public class PopupFontPicker extends PopupFragment {

    @Inject
    private Theme mTheme;
    @Inject
    private Settings mSettings;
    private SeekBar mSeekBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.font_size_picker, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initContent();
    }

    private void initContent() {
        mSeekBar = findView(R.id.font_size_seek_bar);
        mSeekBar.setMax(Settings.ARTICLE_FONT_SIZE_MAX - Settings.ARTICLE_FONT_SIZE_MIN);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int newFontSize = Settings.ARTICLE_FONT_SIZE_MIN + i;
                ((ArticleViewFragment) getParentFragment()).changeFontSize(newFontSize);
                mSettings.changeArticleFontSize(newFontSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    private void update() {
        int fontSize = mTheme.getDefaultArticleFontSize();
        if (mSettings.hasArticleFontSize()) {
            fontSize = mSettings.getArticleFontSize();
        }
        mSeekBar.setProgress(fontSize - Settings.ARTICLE_FONT_SIZE_MIN);
    }
}
