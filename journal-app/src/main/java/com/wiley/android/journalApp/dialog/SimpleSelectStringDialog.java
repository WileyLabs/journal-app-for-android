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
package com.wiley.android.journalApp.dialog;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalDialogFragment;

public class SimpleSelectStringDialog extends JournalDialogFragment {

    public interface Listener {
        void onDialogStringSelected(int index);
        void onDialogCancel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_simple_select_string, container, false);
    }

    protected ListView listValues;

    protected String[] values;
    protected Listener listener;
    protected String title;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUi();
        updateUi();
    }

    protected void initUi() {
        this.listValues = findView(R.id.values);

        listValues.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                if (index < values.length) {
                    listener.onDialogStringSelected(index);
                    dismiss();
                } else {
                    listener.onDialogCancel();
                    dismiss();
                }
            }
        });
    }

    protected void updateUi() {
        updateListViewAdapter();
        updateTitle();
    }

    protected void updateListViewAdapter() {
        if (this.listValues != null) {
            Adapter adapter = new Adapter();
            listValues.setAdapter(adapter);
        }
    }

    protected class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return values.length + 1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            if (position < values.length) {
                textView.setText(values[position]);
                textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else {
                textView.setText(android.R.string.cancel);
                textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            }
            return view;
        }
    }

    protected void updateTitle() {
        if (getDialog() != null)
            getDialog().setTitle(this.title);
    }

    protected void changeValues(String[] newValues) {
        this.values = newValues;
        updateListViewAdapter();
    }

    protected void changeListener(Listener newListener) {
        this.listener = newListener;
    }

    protected void changeTitle(String newTitle) {
        this.title = newTitle;
        updateTitle();
    }

    public static void show(FragmentActivity activity, String title, String[] values, Listener listener) {
        SimpleSelectStringDialog dialog = new SimpleSelectStringDialog();
        dialog.changeValues(values);
        dialog.changeListener(listener);
        dialog.changeTitle(title);

        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("select_string");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        dialog.show(ft, "select_string");
    }
}
