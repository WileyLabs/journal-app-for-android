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
package com.wiley.wol.client.android.journalApp.theme;

import android.graphics.Color;

/**
 * Created by Andrey Rylov on 29/05/14.
 */
public class ColorUtils {

    public static int modifyHsv(int color, float newS, float newV) {
        int a = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (newS >= 0.0f && newS <= 1.0f) {
            hsv[1] = newS;
        }
        if (newV >= 0.0f && newV <= 1.0f) {
            hsv[2] = newV;
        }
        return Color.HSVToColor(a, hsv);
    }

    public static int modifyColor(int color, float modifyS, float modifyV) {
        if (modifyS <= 0.0f || modifyV <= 0.0f) {
            return color;
        }

        int a = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1.0f, hsv[1] * modifyS);
        hsv[2] = Math.min(1.0f, hsv[2] * modifyV);
        return Color.HSVToColor(a, hsv);
    }

    public static int modifyColor(int color, float modify) {
        return modifyColor(color, 1.0f / modify, modify);
    }

    public static int changeAlpha(int color, int newAlpha) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(newAlpha, r, g, b);
    }

    public static int brighterColorByPercent(int color, float percent) {
        percent = Math.max(percent, 0.0f);
        percent = (percent) / 100.0f;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);
        int newR = (int) (255 - (255 - r) * percent);
        int newG = (int) (255 - (255 - g) * percent);
        int newB = (int) (255 - (255 - b) * percent);
        return Color.argb(a, newR, newG, newB);
    }

    public static int parseHexColor(String hex) {
        if (hex.startsWith("#")) {
            return Color.parseColor(hex);
        } else {
            return Color.parseColor("#" + hex);
        }
    }

    public static float[] colorToHsl(int color) {
        //  Get RGB values in the range 0 - 1
        float r = Color.red(color) / 255.0f;
        float g = Color.green(color) / 255.0f;
        float b = Color.blue(color) / 255.0f;

        //	Minimum and Maximum RGB values are used in the HSL calculations

        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));

        //  Calculate the Hue

        float h = 0;

        if (max == min) {
            h = 0;
        } else if (max == r) {
            h = ((60 * (g - b) / (max - min)) + 360) % 360;
        } else if (max == g) {
            h = (60 * (b - r) / (max - min)) + 120;
        } else if (max == b) {
            h = (60 * (r - g) / (max - min)) + 240;
        }

        //  Calculate the Luminance

        float l = (max + min) / 2;

        //  Calculate the Saturation

        float s;

        if (max == min) {
            s = 0;
        } else if (l <= .5f) {
            s = (max - min) / (max + min);
        } else {
            s = (max - min) / (2 - max - min);
        }

        return new float[]{h, s * 100, l * 100};
    }

    public static int colorFromHsl(float[] hsl) {
        return colorFromHsl(hsl, 1.0f);
    }

    public static int colorFromHsl(float[] hsl, float alpha) {
        float h = hsl[0];
        float s = hsl[1];
        float l = hsl[2];

        if (s < 0.0f || s > 100.0f) {
            String message = "Color parameter outside of expected range - Saturation";
            throw new IllegalArgumentException(message);
        }

        if (l < 0.0f || l > 100.0f) {
            String message = "Color parameter outside of expected range - Luminance";
            throw new IllegalArgumentException(message);
        }

        if (alpha < 0.0f || alpha > 1.0f) {
            String message = "Color parameter outside of expected range - Alpha";
            throw new IllegalArgumentException(message);
        }

        //  Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q;

        if (l < 0.5) {
            q = l * (1 + s);
        } else {
            q = (l + s) - (s * l);
        }

        float p = 2 * l - q;

        float r = Math.max(0, hueToRGB(p, q, h + (1.0f / 3.0f)));
        float g = Math.max(0, hueToRGB(p, q, h));
        float b = Math.max(0, hueToRGB(p, q, h - (1.0f / 3.0f)));

        return colorFromARGBFloat(alpha, r, g, b);
    }

    private static float hueToRGB(float p, float q, float h) {
        if (h < 0) {
            h += 1;
        }

        if (h > 1) {
            h -= 1;
        }

        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }

        if (2 * h < 1) {
            return q;
        }

        if (3 * h < 2) {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }

        return p;
    }

    public static int colorFromARGBFloat(float a, float r, float g, float b) {
        return Color.argb(
                (int) (a * 255.0f),
                (int) (r * 255.0f),
                (int) (g * 255.0f),
                (int) (b * 255.0f));
    }

    public static int changeLuminance(int color, float newL) {
        int alpha = Color.alpha(color);
        float[] hsl = colorToHsl(color);
        hsl[2] = newL;
        int result = colorFromHsl(hsl);
        result = changeAlpha(result, alpha);
        return result;
    }

    public static float getLuminance(int color) {
        float[] hsl = colorToHsl(color);
        return hsl[2];
    }
}
