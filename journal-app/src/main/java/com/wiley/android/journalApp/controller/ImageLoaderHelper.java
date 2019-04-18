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
package com.wiley.android.journalApp.controller;

import android.content.Context;
import android.os.Handler;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.wiley.android.journalApp.utils.MemorySafeImageDecoder;

/**
 * Created by taraskreknin on 12.05.14.
 */
public class ImageLoaderHelper {

    @Inject
    public ImageLoaderHelper(Context c) {
        init(c.getApplicationContext());
    }

    private void init(Context c) {
        DisplayImageOptions defaultOptions = buildDefaultImageOptions();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(c)
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCacheSize(25 * 1024 * 1024)
                .imageDecoder(new MemorySafeImageDecoder(new Handler(c.getMainLooper())))
                .build();
        ImageLoader.getInstance().init(config);
    }

    private DisplayImageOptions.Builder prepareDisplayImageOptionsBuilder() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(200, true, true, false))
                .resetViewBeforeLoading(true)
                .delayBeforeLoading(0);
    }

    public DisplayImageOptions buildDefaultImageOptions() {
        return prepareDisplayImageOptionsBuilder().build();
    }

    public DisplayImageOptions buildDefaultImageOptions(int emptyAndFailImageId, boolean showStub) {
        DisplayImageOptions.Builder builder = prepareDisplayImageOptionsBuilder()
                .showImageOnFail(emptyAndFailImageId);
        if (showStub) {
            builder.showImageOnLoading(emptyAndFailImageId);
            builder.showImageForEmptyUri(emptyAndFailImageId);
            builder.displayer(new SimpleBitmapDisplayer());
        }
        return builder.build();
    }

    public ImageLoader getLoader() {
        return ImageLoader.getInstance();
    }

    public void displayImage(String uri, ImageView imageView) {
        getLoader().displayImage(uri, imageView);
    }

}
