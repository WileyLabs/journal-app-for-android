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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

public class BundleUtils {
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> getListFromBundle(Bundle bundle, String name) {
		Object[] objects = (Object[]) bundle.getSerializable(name);
		ArrayList<T> list = new ArrayList<T>();
		for (Object obj : objects)
			list.add((T) obj);
		return list;
	}
	
	public static <T> void putListToBundle(Bundle bundle, String name, List<T> list) {
		Object[] objects = list.toArray();
		bundle.putSerializable(name, objects);
	}
	
	public static <T> void putListToIntent(Intent intent, String name, List<T> list) {
		Object[] objects = list.toArray();
		intent.putExtra(name, (Serializable) objects);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Parcelable> List<T> getParcelableListFromBundle(Bundle bundle, String name) {
		Parcelable[] objects = bundle.getParcelableArray(name);
		ArrayList<T> list = new ArrayList<T>();
		for (Object obj : objects)
			list.add((T) obj);
		return list;
	}
	
	public static <T extends Parcelable> void putParcelableListToBundle(Bundle bundle, String name, List<T> list) {
		Parcelable[] objects = new Parcelable[list.size()];
		for (int i = 0; i < list.size(); i++)
			objects[i] = (Parcelable) list.get(i);
		bundle.putParcelableArray(name, objects);
	}
	
	public static <T extends Parcelable> void putParcelableListToIntent(Intent intent, String name, List<T> list) {
		Parcelable[] objects = new Parcelable[list.size()];
		for (int i = 0; i < list.size(); i++)
			objects[i] = (Parcelable) list.get(i);
		intent.putExtra(name, objects);		
	}
	
	public static void putEnumToBundle(Bundle bundle, String name, Enum<?> value) {
		bundle.putString(name, value.name());
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> T getEnumFromBundle(Bundle bundle, String name, T defaultValue) {
		String valueAsString = bundle.getString(name);
		if (valueAsString == null)
			return defaultValue;
		return (T) T.valueOf(defaultValue.getClass(), valueAsString);
	}
	
	public static void putEnumToIntent(Intent intent, String name, Enum<?> value) {
		intent.putExtra(name, value.name());
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> T getEnumFromIntent(Intent intent, String name, T defaultValue) {
		String valueAsString = intent.getStringExtra(name);
		if (valueAsString == null)
			return defaultValue;
		return (T) T.valueOf(defaultValue.getClass(), valueAsString);
	}
}
