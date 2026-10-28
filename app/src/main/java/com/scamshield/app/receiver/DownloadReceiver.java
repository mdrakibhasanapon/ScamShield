package com.scamshield.app.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.domain.scanner.DownloadThreatEvaluator;
import com.scamshield.app.util.NotificationHelper;

/**
 * BroadcastReceiver monitoring incoming file downloads on-device.
 * Automatically analyzes downloaded files, APK packages, scripts, and origins,
 * calculates a threat score (0-100), logs to history, and dispatches safety alerts.
 */
public class DownloadReceiver extends BroadcastReceiver {

    private static final String TAG = "DownloadReceiver";
    public static final String ACTION_SIMULATE_DOWNLOAD = "com.scamshield.app.SIMULATE_DOWNLOAD";
    public static final String ACTION_DOWNLOAD_SCANNED = "com.scamshield.app.DOWNLOAD_SCANNED";
    public static final String EXTRA_SIMULATED_FILE_NAME = "extra_sim_file_name";
    public static final String EXTRA_SIMULATED_FILE_PATH = "extra_sim_file_path";
    public static final String EXTRA_SIMULATED_ORIGIN_URL = "extra_sim_origin_url";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isDownloadWatchdogEnabled()) {
            Log.d(TAG, "Download watchdog is disabled by user. Skipping scan.");
            return;
        }

        String action = intent.getAction();

        // Handle simulation testing
        if (ACTION_SIMULATE_DOWNLOAD.equals(action)) {
            String fileName = intent.getStringExtra(EXTRA_SIMULATED_FILE_NAME);
            String filePath = intent.getStringExtra(EXTRA_SIMULATED_FILE_PATH);
            String originUrl = intent.getStringExtra(EXTRA_SIMULATED_ORIGIN_URL);
            if (fileName == null || fileName.isEmpty()) fileName = "invoice_payment.pdf.apk";
            if (originUrl == null || originUrl.isEmpty()) originUrl = "http://phishing-delivery-fee.top/dl/invoice.pdf.apk";
            processDownload(context, fileName, filePath, originUrl);
            return;
        }

        // Handle official system DownloadManager complete event
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == -1) return;

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) return;

            DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
            try (Cursor cursor = dm.query(query)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (statusIdx != -1 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                        int titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                        int uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                        int localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                        String title = titleIdx != -1 ? cursor.getString(titleIdx) : "Downloaded File";
                        String originUrl = uriIdx != -1 ? cursor.getString(uriIdx) : "";
                        String localUriStr = localUriIdx != -1 ? cursor.getString(localUriIdx) : "";
                        String localPath = null;

                        if (localUriStr != null && !localUriStr.isEmpty()) {
                            try {
                                Uri uri = Uri.parse(localUriStr);
                                localPath = uri.getPath();
                            } catch (Exception ignored) {}
                        }

                        processDownload(context, title, localPath, originUrl);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying download manager: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Core scan execution pipeline for any downloaded item.
     */
    public static ScamAnalysisResult processDownload(Context context, String fileName, String filePath, String originUrl) {
        if (context == null || fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        try {
            // 1. Evaluate file and download origin heuristics
            ScamAnalysisResult result = DownloadThreatEvaluator.evaluateDownloadedFile(context, fileName, filePath, originUrl);

            // 2. Save scan to local history
            HistoryRepository historyRepository = new HistoryRepository(context);
            historyRepository.saveScan(result);

            // 3. Dispatch system notification alert
            NotificationHelper.showDownloadScanNotification(context, fileName, filePath, originUrl, result);

            // 4. Send local broadcast to update active UI
            try {
                Intent updateIntent = new Intent(ACTION_DOWNLOAD_SCANNED);
                updateIntent.setPackage(context.getPackageName());
                context.sendBroadcast(updateIntent);
            } catch (Exception ignored) {}

            Log.i(TAG, "Processed download [" + fileName + "] with Risk Score: "
                    + result.getRiskScore() + "/100 (" + result.getRiskLevel().getTitle() + ")");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze download: " + e.getMessage(), e);
            return null;
        }
    }
}
