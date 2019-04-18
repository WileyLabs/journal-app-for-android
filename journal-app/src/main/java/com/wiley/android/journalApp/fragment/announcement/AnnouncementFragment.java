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
package com.wiley.android.journalApp.fragment.announcement;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.adapter.ImageSlideAdapter;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.utils.CirclePageIndicator;
import com.wiley.android.journalApp.utils.PageIndicator;
import com.wiley.wol.client.android.data.service.AnnouncementService;
import com.wiley.wol.client.android.domain.entity.AnnouncementMO;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;
import com.wiley.wol.client.android.settings.Settings;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class AnnouncementFragment extends JournalFragment {
    private ViewPager viewPager;
    private PageIndicator indicator;
    private ToggleButton showHideAnnouncementsButton;
    private View announcementsContent;

    private List<AnnouncementMO> announcements;
    @Inject
    private AnnouncementService announcementService;
    @Inject
    private NotificationCenter notificationCenter;
    @Inject
    private Settings settings;

    private NotificationProcessor announcementsUpdatedProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final List<AnnouncementMO> newAnnouncements = announcementService.getActualAnnouncements();
            if (announcements != null && isNewAnnouncementsAppears(announcements, newAnnouncements) && showHideAnnouncementsButton.isChecked()) {
                showHideAnnouncementsButton.performClick();
            }

            announcements = newAnnouncements;
            viewPager.setAdapter(new ImageSlideAdapter(getActivity(), announcements));
            indicator.setViewPager(viewPager);
        }

        private boolean isNewAnnouncementsAppears(List<AnnouncementMO> oldAnnouncements, List<AnnouncementMO> newAnnouncements) {
            for (AnnouncementMO newAnnouncement : newAnnouncements) {
                boolean found = false;
                for (AnnouncementMO oldAnnouncement : oldAnnouncements) {
                    if (oldAnnouncement.getUid().equals(newAnnouncement.getUid())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.announcement_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        indicator = (CirclePageIndicator) view.findViewById(R.id.indicator);
        announcementsContent = view.findViewById(R.id.announcement_content);
        showHideAnnouncementsButton = (ToggleButton) view.findViewById(R.id.hide_announcements_button);
        showHideAnnouncementsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (showHideAnnouncementsButton.isChecked()) {
                    hideAnnouncements();
                } else {
                    showAnnouncements();
                }
            }
        });

        if (!showHideAnnouncementsButton.isChecked() && !settings.isAnnouncementPanelOpen()) {
            showHideAnnouncementsButton.performClick();
        }
    }

    private void showAnnouncements() {
        announcementsContent.setVisibility(View.VISIBLE);
        announcementsContent.startAnimation(new AnnouncementsScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f, 500, announcementsContent, false, -100, 0));
        showHideAnnouncementsButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.hide_arr, 0);
    }

    private void hideAnnouncements() {
        announcementsContent.startAnimation(new AnnouncementsScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f, 500, announcementsContent, true, 0, -100));
        showHideAnnouncementsButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.show_arr, 0);
        settings.setAnnouncementPanelOpen(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        notificationCenter.subscribeToNotification(EventList.ANNOUNCEMENTS_UPDATE_SUCCESS.getEventName(), announcementsUpdatedProcessor);
        settings.setAnnouncementPanelOpen(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationCenter.unSubscribeFromNotification(announcementsUpdatedProcessor);
    }

    @Override
    public void onResume() {
        announcements = announcementService.getActualAnnouncements();
        if (viewPager.getAdapter() == null) {
            viewPager.setAdapter(new ImageSlideAdapter(getActivity(), announcements));
        }
        indicator.setViewPager(viewPager);

        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        viewPager.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public class AnnouncementsScaleAnimation extends ScaleAnimation {

        private View view;

        private FrameLayout.LayoutParams layoutParams;

        private int marginBottomFromY, marginBottomToY;

        private boolean vanishAfter = false;

        public AnnouncementsScaleAnimation(float fromX, float toX, float fromY, float toY, int duration, View view,
                                           boolean vanishAfter, int marginBottomFromY, int marginBottomToY) {
            super(fromX, toX, fromY, toY);
            setDuration(duration);
            this.view = view;
            this.vanishAfter = vanishAfter;
            layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
            this.marginBottomFromY = marginBottomFromY;
            this.marginBottomToY = marginBottomToY;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if (interpolatedTime < 1.0f) {
                int newMarginBottom = marginBottomFromY
                        + (int) ((marginBottomToY - marginBottomFromY) * interpolatedTime);
                layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin,
                        layoutParams.rightMargin, newMarginBottom);
                view.getParent().requestLayout();
            } else if (vanishAfter) {
                view.setVisibility(View.GONE);
            }
        }

    }
}
