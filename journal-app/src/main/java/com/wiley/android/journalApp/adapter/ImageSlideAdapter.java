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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.WebBrowserActivity;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.domain.entity.AnnouncementMO;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ImageSlideAdapter extends PagerAdapter {
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private DisplayImageOptions options;
    private ImageLoadingListener imageListener;
    private FragmentActivity activity;
    private List<AnnouncementMO> announcements;

    public ImageSlideAdapter(FragmentActivity activity, List<AnnouncementMO> announcements) {
        this.activity = activity;
        this.announcements = announcements;
        options = new DisplayImageOptions.Builder().build();

        imageListener = new ImageDisplayListener();
    }

    @Override
    public int getCount() {
        return announcements.size();
    }

    @Override
    public View instantiateItem(ViewGroup container, final int position) {
        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.vp_image, container, false);

        ImageView imageView = (ImageView) view
                .findViewById(R.id.image_display);
        imageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AnnouncementMO announcement = announcements.get(position);
                Intent intent = WebBrowserActivity.getStartingIntent(activity, announcement.getClickUrl());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        });

        final String imageUrl;
        final AnnouncementMO currentAnnouncement = announcements.get(position);
        if (DeviceUtils.isLandscape(activity)) {
            imageUrl = Uri.fromFile(new File(currentAnnouncement.getImageLandscapeLocalURL())).toString();
        } else {
            imageUrl = Uri.fromFile(new File(currentAnnouncement.getImagePortraitLocalURL())).toString();
        }
        imageLoader.displayImage(imageUrl, imageView,
                options, imageListener);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    private static class ImageDisplayListener extends
            SimpleImageLoadingListener {

        static final List<String> displayedImages = Collections
                .synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view,
                                      Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }
}