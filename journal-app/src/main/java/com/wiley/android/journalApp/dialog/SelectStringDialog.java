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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalDialogFragment;

/**
 * Created by Andrey Rylov on 07/05/14.
 */
public class SelectStringDialog extends JournalDialogFragment {

    public interface Listener {
        void onDialogStringSelected(String result);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_select_string, container, false);
    }

    protected TextView textSelected;
    protected ListView listValues;
    protected Button buttonPositive;
    protected Button buttonNegative;

    protected String[] values;
    protected String selected;
    protected Listener listener;
    protected String title;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUi();
        updateUi();
    }

    protected void initUi() {
        this.textSelected = findView(R.id.selected);
        this.listValues = findView(R.id.values);
        this.buttonPositive = findView(R.id.button1);
        this.buttonNegative = findView(R.id.button2);

        listValues.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String newSelected = values[i];
                changeSelected(newSelected);
            }
        });
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onDialogStringSelected(selected);
                dismiss();
            }
        });
        buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    protected void updateUi() {
        updateListViewAdapter();
        updateSelected();
        updateTitle();
    }

    protected void updateListViewAdapter() {
        if (this.listValues != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, values);
            listValues.setAdapter(adapter);
        }
    }

    protected void updateSelected() {
        if (this.textSelected != null)
            this.textSelected.setText(this.selected);
    }

    protected void updateTitle() {
        if (getDialog() != null)
            getDialog().setTitle(this.title);
    }

    protected void changeValues(String[] newValues) {
        this.values = newValues;
        updateListViewAdapter();
    }

    protected void changeSelected(String newSelected) {
        this.selected = newSelected;
        updateSelected();
    }

    protected void changeListener(Listener newListener) {
        this.listener = newListener;
    }

    protected void changeTitle(String newTitle) {
        this.title = newTitle;
        updateTitle();
    }

    public static void show(FragmentActivity activity, String title, String[] values, String selectedValue, Listener listener) {
        SelectStringDialog dialog = new SelectStringDialog();
        dialog.changeValues(values);
        dialog.changeSelected(selectedValue);
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
