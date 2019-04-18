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
package com.wiley.wol.client.android.data.http;

import com.wiley.wol.client.android.log.Logger;

import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.FINISHED;
import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.INTERRUPTED;
import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.NOT_STARTED;
import static com.wiley.wol.client.android.data.http.ThreadOperation.OperationState.STARTED;

public abstract class AbstractThreadOperation implements ThreadOperation {

    private static final String TAG = AbstractThreadOperation.class.getSimpleName();

    private OperationState operationState = NOT_STARTED;
    private boolean interrupt = false;

    @Override
    public void run() {
        Logger.i(TAG, "Operation is started");
        try {
            if (interrupt) {
                throw new InterruptedException("Interruption before starting task");
            }

            do {
                operationState = STARTED;

                beforeTask();
                while (condition()) {
                    if (interrupt) {
                        throw new InterruptedException("Interruption during task execution");
                    }

                    taskPart();
                }

                operationState = FINISHED;
            } while(afterTask());
        } catch (final Exception ex) {
            Logger.s(TAG, ex.getMessage(), ex);
            operationState = INTERRUPTED;
            whenError(ex);
        }
    }

    protected abstract void taskPart() throws Exception;

    protected abstract boolean condition() throws Exception;

    protected void beforeTask() throws Exception {
    }

    protected boolean afterTask() {
        return false;
    }

    protected void whenError(final Exception ex) {
    }

    public OperationState getState() {
        return operationState;
    }

    public void interrupt() {
        interrupt = true;
    }
}