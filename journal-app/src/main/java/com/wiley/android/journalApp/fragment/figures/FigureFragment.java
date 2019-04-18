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
package com.wiley.android.journalApp.fragment.figures;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.wol.client.android.domain.entity.FigureMO;

public class FigureFragment extends JournalFragment {

    public static final String Argument_FigureIndex = "figure_index";

    public interface Host {
        FigureMO getFigure(int index);
        void openFigureByShortCaption(String figureId);
        void toggleUiFullscreen();
        boolean isFullscreen();
    }

    public Host getHost() {
        return (Host) ((Fragment) this).getParentFragment();
    }

    protected int figureIndex;
    protected FigureMO figure;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.figureIndex = getArguments().getInt(Argument_FigureIndex);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.figure = getHost().getFigure(figureIndex);
    }

    public void onUiFullscreenChanged(boolean fullscreen) {
    }

}
