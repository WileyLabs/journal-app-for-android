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

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.google.inject.Inject;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.SupportingInfoMO;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.inject.InjectExternalCachePath;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Settings;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static com.wiley.wol.client.android.error.AppErrorCode.FAIL_TO_AUTHORISE_DOCUMENT;
import static com.wiley.wol.client.android.error.AppErrorCode.FAIL_TO_GET_DOCUMENT;
import static com.wiley.wol.client.android.error.AppErrorCode.SERVER_ERROR;
import static com.wiley.wol.client.android.notification.EventList.DOCUMENT_DOWNLOAD_FINISHED;
import static com.wiley.wol.client.android.notification.EventList.DOCUMENT_DOWNLOAD_PROGRESS;
import static com.wiley.wol.client.android.notification.EventList.DOCUMENT_DOWNLOAD_STARTED;
import static com.wiley.wol.client.android.notification.NotificationCenter.CANCELLED;
import static com.wiley.wol.client.android.notification.NotificationCenter.ERROR;
import static com.wiley.wol.client.android.notification.NotificationCenter.FILE_PATH;
import static com.wiley.wol.client.android.notification.NotificationCenter.PROGRESS;
import static com.wiley.wol.client.android.notification.NotificationCenter.SUCCESS;

@TargetApi(Build.VERSION_CODES.FROYO)
public class DocumentsDownloaderImpl implements DocumentsDownloader {

    private static final String TAG = DocumentsDownloaderImpl.class.getSimpleName();

    @Inject
    private NotificationCenter mNotificationCenter;
    @Inject
    private Theme mTheme;
    @Inject
    private Settings mSettings;
    @Inject
    private DownloadManager mDownloadManager;
    @Inject
    @InjectExternalCachePath
    private String appCachePath;

    private File mAbsPathToFile;
    private File mAbsDirectory;
    private DocType mDocType;
    private DocumentDownloadTask mCurrentTask;

    @Override
    public void getArticlePdf(ArticleMO article) {
        if (article == null) {
            return;
        }

        mDocType = DocType.PDF;

        final File pdfDir = new File(getDirPath() + File.separator + article.getPdfLocalDirectory().toString());
        final File pdfPath = new File(pdfDir, article.getPdfFileName());

        if (pdfPath.exists()) {
            Logger.d(TAG, String.format("PDF for article %s was already downloaded", article.getDOI().getValue()));
            sendDownloadSuccess(pdfPath.toString());
        } else {
            String articlePdfUrl = String.format("%s/articlePdf.feed?doi=%s", mTheme.getServerUrlOnServer(mSettings.getCurrentServer()), article.getDOI().getValue());
            downloadDocument(articlePdfUrl, pdfDir, pdfPath);
        }

    }

    @Override
    public void getSupportingInfo(SupportingInfoMO info) {
        if (info == null) {
            return;
        }

        mDocType = DocType.SUPPORTING_INFO;

        final File infoDir = new File(getDirPath() + File.separator + info.getArticle().getLocalPath("supporting_info"));
        final File infoPath = new File(infoDir, info.getTitle());

        if (infoPath.exists()) {
            Logger.d(TAG, "Supporting info was already downloaded");
            sendDownloadSuccess(infoPath.toString());
        } else {
            downloadDocument(info.getAssetRef(), infoDir, infoPath);
        }
    }

    private void downloadDocument(String reference, File directory, File filePath) {
        mAbsDirectory = directory;
        mAbsPathToFile = filePath;

        if (!mAbsDirectory.exists()) {
            mAbsDirectory.mkdirs();
        }

        if (isDownloadInProgress()) {
            cancel();
        }

        mCurrentTask = new DocumentDownloadTask();
        mCurrentTask.execute(reference);
    }

    @Override
    public void cancel() {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
    }


    private String getDirPath() {
        return appCachePath;
    }

    @Override
    public boolean isDownloadInProgress() {
        return mCurrentTask != null && !mCurrentTask.isCancelled();
    }

    @Override
    public int getProgress() {
        return isDownloadInProgress() ? mCurrentTask.lastProgress : -1;
    }

    private void sendDownloadSuccess(String path) {
        sendDownloadCompleted(true, path, false, null);
    }

    private void sendDownloadFailed(AppErrorCode errorCode) {
        sendDownloadCompleted(false, "", false, errorCode);
    }

    private void sendDownloadCancelled() {
        sendDownloadCompleted(false, "", true, null);
    }

    private void sendDownloadStarted() {
        Logger.d(TAG, "Document download started ");
        HashMap<String, Object> params = new HashMap<>();
        params.put(DOCUMENT_TYPE, mDocType);
        mNotificationCenter.sendNotification(DOCUMENT_DOWNLOAD_STARTED.getEventName(), params);
    }

    private void sendDownloadCompleted(boolean success, String path, boolean cancelled, AppErrorCode errorCode) {
        final HashMap<String, Object> params = new HashMap<>();
        params.put(SUCCESS, success);
        params.put(FILE_PATH, path);
        params.put(CANCELLED, cancelled);
        params.put(ERROR, errorCode);
        params.put(DOCUMENT_TYPE, mDocType);
        mNotificationCenter.sendNotification(DOCUMENT_DOWNLOAD_FINISHED.getEventName(), params);
    }

    private void sendDownloadProgress(int progressPercents) {
        Logger.d(TAG, "Document download progress " + progressPercents);
        HashMap<String, Object> params = new HashMap<>();
        params.put(PROGRESS, progressPercents);
        params.put(DOCUMENT_TYPE, mDocType);
        mNotificationCenter.sendNotification(DOCUMENT_DOWNLOAD_PROGRESS.getEventName(), params);
    }

    private class DocumentDownloadTask extends AsyncTask<String, Integer, String> {

        private InputStream input = null;
        private OutputStream output = null;
        private int lastProgress = -1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            sendDownloadStarted();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values[0] != lastProgress) {
                sendDownloadProgress(values[0]);
                lastProgress = values[0];
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            sendDownloadCancelled();
            mCurrentTask = null;
            mDocType = null;
            mAbsDirectory = null;
            mAbsPathToFile = null;
        }

        @Override
        protected String doInBackground(String... params) {
            final HttpResponse response = getWOLResponseForArticlePdfSync(params[0]);
            if (response == null) {
                sendDownloadFailed(FAIL_TO_GET_DOCUMENT);
                return null;
            }
            String result = null;
            int status = mDownloadManager.getStatusCodeFor(response);
            switch (status) {
                case 401:
                case 403:
                    sendDownloadFailed(FAIL_TO_AUTHORISE_DOCUMENT);
                    break;
                case 400:
                case 500:
                case 503:
                    sendDownloadFailed(SERVER_ERROR);
                    break;
                default:
                    long fileLength = mDownloadManager.getContentLengthFor(response);

                    try {
                        if (!mAbsDirectory.exists()) {
                            mAbsDirectory.mkdirs();
                        }
                        input = mDownloadManager.getContentFor(response);
                        output = new FileOutputStream(mAbsPathToFile);
                        byte data[] = new byte[4096];
                        long total = 0;
                        int count;
                        while ((count = input.read(data)) != -1) {
                            if (isCancelled()) {
                                input.close();
                                output.close();
                                removeFileIfExists(mAbsPathToFile);
                                return null;
                            }
                            total += count;
                            if (fileLength > 0)
                                publishProgress((int) (total * 100 / fileLength));
                            output.write(data, 0, count);
                        }
                        result = mAbsPathToFile.toString();
                    } catch (IOException e) {
                        Logger.s(TAG, e);
                        sendDownloadFailed(FAIL_TO_GET_DOCUMENT);
                        removeFileIfExists(mAbsPathToFile);
                    } finally {
                        try {
                            input.close();
                            output.close();
                            mDownloadManager.close(response);
                        } catch (IOException e) {
                            Logger.d(TAG, e.getMessage(), e);
                        }
                    }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (!TextUtils.isEmpty(s)) {
                sendDownloadSuccess(s);
            }
            mCurrentTask = null;
            mDocType = null;
            mAbsDirectory = null;
            mAbsPathToFile = null;
        }
    }

    private void removeFileIfExists(File path) {
        if (path != null && path.exists()) {
            Logger.d(TAG, "Deleting document at " + path);
            path.delete();
        }
    }

    private HttpResponse getWOLResponseForArticlePdfSync(String url) {
        final ArrayList<Header> headers = new ArrayList<>();
        final String accessToken = mSettings.getAccessToken();
        final String accessTokenSecret = mSettings.getAccessTokenSecret();
        if (mSettings.hasSubscriptionReceipt()) {
            headers.add(mDownloadManager.createGooglePlayHeaderWithIdentityAndType(url, mSettings.getSubscriptionReceipt()));
        } else if (!TextUtils.isEmpty(accessToken) && !TextUtils.isEmpty(accessTokenSecret)) {
            headers.add(mDownloadManager.createAuthHeaders(url, accessToken, accessTokenSecret));
        }
        HttpResponse response = null;
        try {
            response = mDownloadManager.connectTo(url, headers);
        } catch (IOException e) {
            Logger.s(TAG, e);
        }
        return response;
    }

}
