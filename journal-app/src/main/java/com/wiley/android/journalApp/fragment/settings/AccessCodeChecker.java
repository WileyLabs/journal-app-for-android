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
package com.wiley.android.journalApp.fragment.settings;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.google.inject.Inject;
import com.wiley.wol.client.android.data.service.AuthorizationService;

import java.util.ArrayList;
import java.util.List;

import roboguice.RoboGuice;

public class AccessCodeChecker {

    public enum State {
        None,
        Progress,
        Invalid,
        Valid
    };

    private State state = State.None;

    public State getState() {
        return state;
    }

    public interface Listener {
        void onStateChanged();
    }

    private List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    private void onStateChanged() {
        for (Listener listener : listeners)
            listener.onStateChanged();
    }

    @Inject
    private AuthorizationService authorizationService;

    public AccessCodeChecker(Context context) {
        RoboGuice.getInjector(context).injectMembersWithoutViews(this);
    }

    private String lastRequestedAccessCode = null;

    public void requestAccessCodeCheck(String accessCode) {
        this.state = State.Progress;
        this.lastRequestedAccessCode = accessCode;
        onStateChanged();

        handler.removeCallbacks(runnableCreateCheckAction);
        handler.postDelayed(runnableCreateCheckAction, 500);
    }

    private Handler handler = new Handler();

    private Runnable runnableCreateCheckAction = new Runnable() {
        @Override
        public void run() {
            AsyncTask<Void, Void, Void> task = createCheckAction(lastRequestedAccessCode);
            task.execute();
        }
    };

    private AsyncTask<Void, Void, Void> createCheckAction(final String accessCode) {
        return new AsyncTask<Void, Void, Void>() {
            private boolean valid = false;

            @Override
            protected Void doInBackground(Void... params) {
                if (!accessCode.equals(lastRequestedAccessCode))
                    return null;
                this.valid = authorizationService.checkAccessCode(accessCode);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (!accessCode.equals(lastRequestedAccessCode))
                    return;

                onAccessCodeChecked(this.valid);
            }
        };
    };

    private void onAccessCodeChecked(boolean valid) {
        this.state = valid ? State.Valid : State.Invalid;
        onStateChanged();
    }
}
