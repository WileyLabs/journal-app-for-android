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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.wiley.android.journalApp.utils.MemorySafeImageDecoder;
import com.wiley.android.journalApp.widget.PhotoView;

public class FigureViewerImageLoaderHelper {
    private boolean drawBorder = false;
    private Float initialScale;

    @Inject
    public FigureViewerImageLoaderHelper(Context c) {
        init(c.getApplicationContext());
    }

    private void init(Context c) {
        DisplayImageOptions defaultOptions = buildDefaultImageOptions();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(c)
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCacheSize(25 * 1024 * 1024)
                .imageDecoder(new MemorySafeImageDecoder(new Handler(c.getMainLooper())))
                .build();
        getLoader().init(config);
    }

    public DisplayImageOptions buildDefaultImageOptions() {
        return prepareDisplayImageOptionsBuilder().build();
    }

    protected DisplayImageOptions.Builder prepareDisplayImageOptionsBuilder() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(200, true, true, false) {
                    @Override
                    public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
                        if (isDrawBorder()) {

                            Bitmap bmpWithWhiteBorder = Bitmap.createBitmap(bitmap.getWidth() + 20,
                                    bitmap.getHeight() + 20, bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bmpWithWhiteBorder);
                            canvas.drawColor(Color.WHITE);
                            canvas.drawBitmap(bitmap, 10, 10, null);

                            Bitmap bmpWithBlackBorder = Bitmap.createBitmap(bmpWithWhiteBorder.getWidth() + 4,
                                    bmpWithWhiteBorder.getHeight() + 4, bmpWithWhiteBorder.getConfig() != null ? bmpWithWhiteBorder.getConfig() : Bitmap.Config.ARGB_8888);
                            canvas = new Canvas(bmpWithBlackBorder);
                            canvas.drawColor(Color.BLACK);
                            canvas.drawBitmap(bmpWithWhiteBorder, 2, 2, null);

                            super.display(bmpWithBlackBorder, imageAware, loadedFrom);
                        } else {
                            super.display(bitmap, imageAware, loadedFrom);
                        }

                        if (getInitialScale() != null && imageAware.getWrappedView().getClass() == PhotoView.class) {
                            ((PhotoView) imageAware.getWrappedView()).setScale(getInitialScale());
                        }
                    }
                })
                .resetViewBeforeLoading(true)
                .delayBeforeLoading(0);
    }

    public ImageLoader getLoader() {
        return FigureViewerImageLoader.getInstance();
    }

    public void displayImage(String uri, ImageView imageView) {
        getLoader().displayImage(uri, imageView);
    }

    public void clearCache() {
        getLoader().clearMemoryCache();
    }

    public boolean isDrawBorder() {
        return drawBorder;
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    public Float getInitialScale() {
        return initialScale;
    }

    public void setInitialScale(Float initialScale) {
        this.initialScale = initialScale;
    }
}
