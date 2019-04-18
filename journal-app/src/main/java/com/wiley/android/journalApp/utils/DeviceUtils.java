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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.wiley.android.journalApp.R;

/**
 * Created by Andrey Rylov on 13/05/14.
 */
public class DeviceUtils {

    public enum DeviceType {
        SmallPhone,
        Phone,
        Tablet7inch,
        Tablet10inch
    }

    private static DeviceType deviceType = null;
    private static String deviceTypeAsString = null;

    private static DeviceType getDeviceTypeFromResources(Context context) {
        String deviceTypeAsString = getDeviceTypeAsString(context);
        switch (deviceTypeAsString) {
            case "smallPhone":
                return DeviceType.SmallPhone;
            case "phone":
                return DeviceType.Phone;
            case "tablet7inch":
                return DeviceType.Tablet7inch;
            case "tablet10inch":
                return DeviceType.Tablet10inch;
            default:
                return DeviceType.Phone;
        }
    }

    public static DeviceType getDeviceType(Context context) {
        if (deviceType == null) {
            deviceType = getDeviceTypeFromResources(context);
        }
        return deviceType;
    }

    public static String getDeviceTypeAsString(Context context) {
        if (deviceTypeAsString == null) {
            deviceTypeAsString = context.getString(R.string.device_type);
        }
        return deviceTypeAsString;
    }

    public static boolean isSmallPhone(Context context) {
        DeviceType deviceType = getDeviceType(context);
        return deviceType == DeviceType.SmallPhone;
    }

    public static boolean isNormalPhone(Context context) {
        DeviceType deviceType = getDeviceType(context);
        return deviceType == DeviceType.Phone;
    }

    public static boolean isPhone(Context context) {
        return isSmallPhone(context) || isNormalPhone(context);
    }

    public static boolean isTablet7Inch(Context context) {
        DeviceType deviceType = getDeviceType(context);
        return deviceType == DeviceType.Tablet7inch;
    }

    public static boolean isTablet10Inch(Context context) {
        DeviceType deviceType = getDeviceType(context);
        return deviceType == DeviceType.Tablet10inch;
    }

    public static boolean isTablet(Context context) {
        return isTablet7Inch(context) || isTablet10Inch(context);
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static boolean isInternetConnectionAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    public static int getRotation(Context context){
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch (rotation) {
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_180:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                case Surface.ROTATION_180:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
        }
    }
}
