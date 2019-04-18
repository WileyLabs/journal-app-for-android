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
package com.wiley.android.journalApp.utils;

import android.graphics.Bitmap;
import android.os.Handler;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.decode.ImageDecodingInfo;
import com.wiley.android.journalApp.BuildConfig;
import com.wiley.wol.client.android.log.Logger;

import java.io.IOException;
import java.util.HashSet;

public class MemorySafeImageDecoder extends BaseImageDecoder {
	
	private static final int MaxDecodeTryCount = 3;
	private Handler uiThreadHandler = null;
	
	private HashSet<String> blacklist = new HashSet<String>();

	public MemorySafeImageDecoder(Handler uiThreadHandler) {
        super(BuildConfig.DEBUG);
		this.uiThreadHandler = uiThreadHandler;
	}
	
	@Override
	public Bitmap decode(ImageDecodingInfo decodingInfo) throws IOException {
		if (blacklist.contains(decodingInfo.getImageKey()))
			return null;
		
		int decodeTryCount = 0;
		
		while (decodeTryCount < MaxDecodeTryCount) {
			decodeTryCount++;
			try {
				return super.decode(decodingInfo);
			} catch (OutOfMemoryError error) {
				Logger.w("Image decoder", "OutOfMemory error. Downsample image");

				decodingInfo.getDecodingOptions().inSampleSize /= 2;
				
				System.gc();		
				
				uiThreadHandler.post(new Runnable() {
					@Override
					public void run() {
						ImageLoader.getInstance().getMemoryCache().clear();
						System.gc();						
					}
				});
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored) {
				}
			}
		}
		
		blacklist.add(decodingInfo.getImageKey());
		
		return null;
	}
	
}