package com.scamshield.app.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Handles one-tap deletion of dangerous downloaded files directly from system notifications.
 */
public class DeleteDownloadReceiver extends BroadcastReceiver {

    public static final String ACTION_DELETE_DOWNLOAD = "com.scamshield.app.ACTION_DELETE_DOWNLOAD";
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_FILE_NAME = "extra_file_name";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
        String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        if (notificationId != -1) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(notificationId);
        }

        if (filePath != null) {
            try {
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    Toast.makeText(context, "🗑️ Dangerous file \"" + (fileName != null ? fileName : file.getName()) + "\" deleted successfully!", Toast.LENGTH_LONG).show();
                    Log.i("DeleteDownloadReceiver", "Deleted malicious downloaded file: " + filePath);
                } else {
                    Toast.makeText(context, "File already removed or inaccessible.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("DeleteDownloadReceiver", "Failed to delete file: " + e.getMessage());
                Toast.makeText(context, "Failed to delete file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
