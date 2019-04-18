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
package com.wiley.android.journalApp.fragment.articleView;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.components.popup.PopupHost;
import com.wiley.android.journalApp.utils.BundleUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.android.journalApp.widget.SeekBarCompat;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.exception.ElementNotFoundException;
import com.wiley.wol.client.android.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrey Rylov on 24/06/14.
 */
public class ArticleViewPageIndicator extends JournalFragment {
    private final static String TAG = ArticleViewPageIndicator.class.getSimpleName();
    public static final String Extra_ViewPagerId = "viewPagerId";
    public static final String Extra_PopupHostId = "popupHostId";
    public static final String Extra_DoiList = "doiList";

    private PopupHost popupHost = null;
    private int popupHostId = 0;
    private List<DOI> doiList = new ArrayList<DOI>();
    private DOI currentDoi = null;

    private SeekBarCompat seekBar = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRetainInstance(true);TODO
        popupHostId = getArguments().getInt(Extra_PopupHostId);
        doiList = BundleUtils.getParcelableListFromBundle(getArguments(), Extra_DoiList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_article_view_page_indicator, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.seekBar = findView(R.id.seek_bar);
        this.popupHost = getJournalActivity().findView(popupHostId);
    }

    @Override
    public void onStart() {
        super.onStart();
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        updateUi();
    }

    @Override
    public void onStop() {
        super.onStop();
        seekBar.setOnSeekBarChangeListener(null);
    }

    public DOI getCurrentDoi() {
        return currentDoi;
    }

    public void setCurrentDoi(DOI doi) {
        this.currentDoi = doi;
        updateUi();
    }

    public interface OnIndicatorChangeListener {
        void onNeedScrollToDoi(DOI doi);
    };

    protected OnIndicatorChangeListener onIndicatorChangeListener = null;

    public OnIndicatorChangeListener getOnIndicatorChangeListener() {
        return onIndicatorChangeListener;
    }

    public void setOnIndicatorChangeListener(OnIndicatorChangeListener listener) {
        this.onIndicatorChangeListener = listener;
    }

    protected SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private boolean progressWasChangedFromUser = false;
        private boolean scrolling = false;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if (scrolling) {
                    progressWasChangedFromUser = true;
                    showPopup();
                } else {
                    if (onIndicatorChangeListener != null)
                        onIndicatorChangeListener.onNeedScrollToDoi(doiList.get(progress));
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            scrolling = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (scrolling) {
                scrolling = false;
                hidePopup();
                if (progressWasChangedFromUser) {
                    progressWasChangedFromUser = false;
                    if (onIndicatorChangeListener != null)
                        onIndicatorChangeListener.onNeedScrollToDoi(doiList.get(seekBar.getProgress()));
                }
            }
        }
    };

    protected void showPopup() {
        int indexForPopup = seekBar.getProgress();
        DOI doiForPopup = doiList.get(indexForPopup);

        Drawable thumb = seekBar.getThumbCompat();
        Point pointInSeekBar = new Point(
                seekBar.getPaddingLeft() + thumb.getBounds().left,
                -UIUtils.dpToPx(getActivity(), 8));
        int[] seekBarInWindow = new int[2];
        seekBar.getLocationInWindow(seekBarInWindow);
        Point popupPoint = new Point(pointInSeekBar.x + seekBarInWindow[0], pointInSeekBar.y + seekBarInWindow[1]);

        Bundle args = new Bundle();
        args.putParcelable(ArticleInfoFragment.Extra_Doi, doiForPopup);

        popupHost.showFragmentAtPoint(ArticleInfoFragment.class, getFragmentManager(), popupPoint, args, PopupHost.Orientation.Vertical);
    }

    protected void hidePopup() {
        popupHost.hide();
    }

    protected void updateUi() {
        seekBar.setMax(doiList.size() - 1);
        int index = doiList.indexOf(currentDoi);
        if (index >= 0)
            seekBar.setProgress(index);
    }

    public void updateDoiList(List<DOI> newList) {
        doiList = newList;
    }

    public static class ArticleInfoFragment extends JournalFragment {

        public static final String Extra_Doi = "doi";

        @Inject
        private ArticleService articleService;
        private DOI doi = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            doi = getArguments().getParcelable(Extra_Doi);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.popup_article_page_indicator_info, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            TextView heading1 = findView(R.id.heading1);
            TextView heading2 = findView(R.id.heading2);
            TextView authors = findView(R.id.authors);
            TextView text = findView(R.id.text);

            heading1.setText("");
            heading2.setText("");
            authors.setText("");
            text.setText("");

            try {
                ArticleMO article = articleService.getArticleFromDao(doi);
                heading1.setText(article.getTocHeading1());
                heading2.setText(article.getTocHeading2());
                authors.setText(html(article.getShortAuthorsTitle()));
                text.setText(html(article.getTitle()));
            } catch (ElementNotFoundException e) {
                Logger.d(TAG, e.getMessage(), e);
            }

            if (authors.getText().length() == 0) {
                authors.setVisibility(View.GONE);
            } else {
                authors.setVisibility(View.VISIBLE);
            }
        }

        private CharSequence html(String from) {
            if (TextUtils.isEmpty(from))
                return "";
            else
                return Html.fromHtml(from);
        }
    }
}
