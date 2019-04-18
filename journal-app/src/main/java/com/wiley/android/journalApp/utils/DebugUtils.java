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
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;

import com.wiley.wol.client.android.log.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class DebugUtils {
    private final static String TAG = DebugUtils.class.getSimpleName();
    public static final String NullString = "null";

    public static String dumpIntentExtras(Context context, Intent intent) {
        if (intent == null)
            return NullString;

        Bundle bundle = intent.getExtras();
        return dumpBundle(context, bundle);
    }

    public static String dumpBundle(Context context, Bundle bundle) {
        if (bundle == null)
            return NullString;

        StringBuilder strings = new StringBuilder();
        dumpBundle(context, bundle, strings);
        return strings.toString();
    }

    protected static void dumpBundle(Context context, Bundle bundle, StringBuilder strings) {
        for (String key : bundle.keySet()) {
            strings.append(String.format("Key: %s\n", key));
            strings.append("Value: ");
            Object value = bundle.get(key);
            dumpObject(context, value, strings);
            strings.append("\n");
        }
    }

    protected static void dumpObject(Context context, Object object, StringBuilder strings) {
        if (object == null) {
            strings.append(NullString);
            return;
        }

        if (object instanceof Bundle) {
            Bundle bundle = (Bundle) object;
            if (bundle.isEmpty()) {
                strings.append("Empty Bundle");
                return;
            }
            strings.append("Start of Bundle\n");
            dumpBundle(context, bundle, strings);
            strings.append("End of Bundle\n");
            return;
        }

        if (object instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) object;
            if (map.isEmpty()) {
                strings.append("Empty Map");
                return;
            }
            strings.append("Start of Map\n");
            dumpMap(context, map, strings);
            strings.append("End of Map\n");
            return;
        }

        if (object instanceof SparseArray<?>) {
            SparseArray<?> array = (SparseArray<?>) object;
            if (array.size() == 0) {
                strings.append("Empty SparseArray");
                return;
            }
            strings.append("Start of SparseArray\n");
            dumpSparseArray(context, array, strings);
            strings.append("End of SparseArray\n");
            return;
        }

        if (object.getClass().isArray()) {
            if (Array.getLength(object) == 0) {
                strings.append("Empty array of " + object.getClass().getComponentType().toString());
                return;
            }
            strings.append("Start of array of " + object.getClass().getComponentType().toString() + "\n");
            dumpArray(context, object, strings);
            strings.append("End of array\n");
            return;
        }

        if (object.getClass().getName().contains("SavedState")) {
            strings.append("Start of SavedState " + object.getClass().toString() + "\n");
            dumpObjectFields(context, object, strings);
            strings.append("End of SavedState\n");
            return;
        }

        if (object.getClass().getName().contains("FragmentManagerState") || object.getClass().getName().contains("FragmentState")) {
            strings.append("Start of object " + object.getClass().getSimpleName() + "\n");
            dumpObjectFields(context, object, strings);
            strings.append("End of object\n");
            return;
        }

        strings.append(formatString(context, object));
    }

    protected static void dumpObjectFields(Context context, Object object, StringBuilder strings) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            strings.append(String.format("Field: %s\n", field.getName()));
            strings.append("Value: ");
            Object value = null;
            try {
                value = field.get(object);
            } catch (IllegalAccessException e) {
                Logger.d(TAG, e.getMessage(), e);
            }
            dumpObject(context, value, strings);
            strings.append("\n");
        }
    }

    protected static void dumpMap(Context context, Map<?, ?> map, StringBuilder strings) {
        for (Object key : map.keySet()) {
            strings.append(String.format("Key: %s\n", key.toString()));
            strings.append("Value: ");
            Object value = map.get(key);
            dumpObject(context, value, strings);
            strings.append("\n");
        }
    }

    protected static void dumpSparseArray(Context context, SparseArray<?> array, StringBuilder strings) {
        for (int i = 0; i < array.size(); i++) {
            int key = array.keyAt(i);
            strings.append(String.format("Key: %s\n", formatString(context, key)));
            strings.append("Value: ");
            Object value = array.get(key);
            dumpObject(context, value, strings);
            strings.append("\n");
        }
    }

    protected static void dumpArray(Context context, Object object, StringBuilder strings) {
        int length = Array.getLength(object);
        for (int i = 0; i < length; i++) {
            strings.append(i).append(": ");
            Object value = Array.get(object, i);
            dumpObject(context, value, strings);
            strings.append("\n");
        }
    }

    protected static String formatString(Context context, Object object) {
        String string = object.toString();
        if (TextUtils.isDigitsOnly(string) && string.length() > 3) {
            try {
                int number = Integer.parseInt(string);
                String resourceName = context.getResources().getResourceEntryName(number);
                String resourceType = context.getResources().getResourceTypeName(number);
                return String.format("%d (%s.%s)", number, resourceType, resourceName);
            } catch (NumberFormatException e) {
            } catch (Resources.NotFoundException e) {
            }
        }
        return string;
    }
}
