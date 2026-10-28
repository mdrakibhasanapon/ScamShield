package com.scamshield.app.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.ui.result.ResultActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID_ALERTS = "scamshield_threat_alerts";
    public static final String CHANNEL_ID_CLEAN = "scamshield_clean_messages";
    public static final String CHANNEL_ID_DOWNLOADS = "scamshield_download_alerts";
    public static final String CHANNEL_ID_NETWORK = "scamshield_network_alerts";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Channel 1: High Priority Threat Alerts (Sound, Vibration, Heads-up)
            NotificationChannel threatChannel = new NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Scam & Phishing Threat Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            threatChannel.setDescription("Instant alerts when incoming SMS messages show scam, phishing, or fraud indicators.");
            threatChannel.enableLights(true);
            threatChannel.setLightColor(Color.RED);
            threatChannel.enableVibration(true);
            manager.createNotificationChannel(threatChannel);

            // Channel 2: Low Priority Informational Clean Scans
            NotificationChannel cleanChannel = new NotificationChannel(
                    CHANNEL_ID_CLEAN,
                    "Clean Message Summaries",
                    NotificationManager.IMPORTANCE_LOW
            );
            cleanChannel.setDescription("Subtle summaries for incoming messages with no obvious scam indicators.");
            manager.createNotificationChannel(cleanChannel);

            // Channel 3: High Priority Download Security Alerts
            NotificationChannel downloadChannel = new NotificationChannel(
                    CHANNEL_ID_DOWNLOADS,
                    "Download Security Watchdog",
                    NotificationManager.IMPORTANCE_HIGH
            );
            downloadChannel.setDescription("Instant alerts when downloaded files show trojan, malware, or disguised indicators.");
            downloadChannel.enableLights(true);
            downloadChannel.setLightColor(Color.RED);
            downloadChannel.enableVibration(true);
            manager.createNotificationChannel(downloadChannel);

            // Channel 4: High Priority App Network & Data Exfiltration Alerts
            NotificationChannel networkChannel = new NotificationChannel(
                    CHANNEL_ID_NETWORK,
                    "App Data & Network Watchdog",
                    NotificationManager.IMPORTANCE_HIGH
            );
            networkChannel.setDescription("Instant alerts when apps upload sensitive data, leak passwords over HTTP, or contact spyware C2 servers.");
            networkChannel.enableLights(true);
            networkChannel.setLightColor(Color.RED);
            networkChannel.enableVibration(true);
            manager.createNotificationChannel(networkChannel);
        }
    }

    public static void showSmsScanNotification(Context context, String sender, String content, ScamAnalysisResult result) {
        if (context == null || result == null) return;

        // Check Android 13+ POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannels(context);

        PreferencesManager prefs = new PreferencesManager(context);
        RiskLevel riskLevel = result.getRiskLevel();
        int score = result.getRiskScore();

        // If low risk and user has not enabled all-SMS notifications, remain quiet to avoid notification clutter
        if (riskLevel == RiskLevel.LOW_RISK && !prefs.isNotifyAllSmsEnabled()) {
            return;
        }

        int notificationId = (sender + content).hashCode();

        // Pending Intent to open full analysis in ResultActivity
        Intent openIntent = new Intent(context, ResultActivity.class);
        openIntent.putExtra(ResultActivity.EXTRA_RESULT, result);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title;
        String snippet = result.getSnippet(100);
        String reasonSnippet = (result.getWhyFlagged() != null && !result.getWhyFlagged().isEmpty())
                ? result.getWhyFlagged().get(0)
                : "Exercise caution before responding.";

        int color;
        String channelId = CHANNEL_ID_ALERTS;
        int priority = NotificationCompat.PRIORITY_HIGH;

        if (riskLevel == RiskLevel.HIGH_RISK) {
            title = "🚨 Likely Scam SMS from " + sender + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_high);
        } else if (riskLevel == RiskLevel.SUSPICIOUS) {
            title = "⚠️ Suspicious SMS from " + sender + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_suspicious);
        } else if (riskLevel == RiskLevel.NEEDS_ATTENTION) {
            title = "🔍 Review Message from " + sender + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_attention);
        } else {
            title = "✓ Clean SMS from " + sender + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_low);
            channelId = CHANNEL_ID_CLEAN;
            priority = NotificationCompat.PRIORITY_LOW;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_shield_check)
                .setContentTitle(title)
                .setContentText(reasonSnippet)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText("Message: \"" + snippet + "\"\n\nRisk Assessment: " + score + "/100 (" + riskLevel.getTitle() + ")\n• " + reasonSnippet))
                .setColor(color)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(priority)
                .addAction(R.drawable.ic_shield_check, "Inspect Threat Report", pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    public static void showDownloadScanNotification(Context context, String fileName, String filePath, String originUrl, ScamAnalysisResult result) {
        if (context == null || result == null) return;

        // Check Android 13+ POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannels(context);

        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isDownloadWatchdogEnabled()) return;

        RiskLevel riskLevel = result.getRiskLevel();
        int score = result.getRiskScore();

        if (riskLevel == RiskLevel.LOW_RISK && !prefs.isNotifyCleanDownloadsEnabled()) {
            return;
        }

        int notificationId = (fileName + (filePath != null ? filePath : "")).hashCode();

        // Pending Intent to open full analysis in ResultActivity
        Intent openIntent = new Intent(context, ResultActivity.class);
        openIntent.putExtra(ResultActivity.EXTRA_RESULT, result);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title;
        String reasonSnippet = (result.getWhyFlagged() != null && !result.getWhyFlagged().isEmpty())
                ? result.getWhyFlagged().get(0)
                : "Exercise caution before running or opening.";

        int color;
        String channelId = CHANNEL_ID_DOWNLOADS;
        int priority = NotificationCompat.PRIORITY_HIGH;

        if (riskLevel == RiskLevel.HIGH_RISK) {
            title = "🚨 Dangerous Download: " + fileName + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_high);
        } else if (riskLevel == RiskLevel.SUSPICIOUS) {
            title = "⚠️ Suspicious Download: " + fileName + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_suspicious);
        } else if (riskLevel == RiskLevel.NEEDS_ATTENTION) {
            title = "🔍 Review Download: " + fileName + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_attention);
        } else {
            title = "✓ Download Verified Safe: " + fileName + " (" + score + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_low);
            channelId = CHANNEL_ID_CLEAN;
            priority = NotificationCompat.PRIORITY_LOW;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_shield_check)
                .setContentTitle(title)
                .setContentText(reasonSnippet)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText("File: \"" + fileName + "\"\n\nRisk Assessment: " + score + "/100 (" + riskLevel.getTitle() + ")\n• " + reasonSnippet +
                                (originUrl != null && !originUrl.isEmpty() ? "\n\nOrigin Link: " + originUrl : "")))
                .setColor(color)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(priority)
                .addAction(R.drawable.ic_shield_check, "Inspect Report", pendingIntent);

        // If threat and file path is available, add one-tap Delete File action right in notification
        if (score > 20 && filePath != null) {
            Intent deleteIntent = new Intent(context, com.scamshield.app.receiver.DeleteDownloadReceiver.class);
            deleteIntent.setAction(com.scamshield.app.receiver.DeleteDownloadReceiver.ACTION_DELETE_DOWNLOAD);
            deleteIntent.putExtra(com.scamshield.app.receiver.DeleteDownloadReceiver.EXTRA_FILE_PATH, filePath);
            deleteIntent.putExtra(com.scamshield.app.receiver.DeleteDownloadReceiver.EXTRA_FILE_NAME, fileName);
            deleteIntent.putExtra(com.scamshield.app.receiver.DeleteDownloadReceiver.EXTRA_NOTIFICATION_ID, notificationId);

            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 1,
                    deleteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            builder.addAction(R.drawable.ic_delete_trash, "🗑️ Delete File", deletePendingIntent);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    public static void showNetworkAlertNotification(Context context, com.scamshield.app.data.model.NetworkTrafficItem item) {
        if (context == null || item == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannels(context);

        int notificationId = ("network_" + item.getId()).hashCode();

        Intent openIntent = new Intent(context, com.scamshield.app.ui.network.NetworkTrafficActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title;
        int color;
        if (item.getRiskLevel() == RiskLevel.HIGH_RISK) {
            title = "🚨 Suspicious Data Upload: " + item.getAppName() + " (" + item.getRiskScore() + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_high);
        } else {
            title = "⚠️ App Data Warning: " + item.getAppName() + " (" + item.getRiskScore() + "/100)";
            color = ContextCompat.getColor(context, R.color.risk_suspicious);
        }

        String dataAmount = item.getDirection() == com.scamshield.app.data.model.NetworkTrafficItem.Direction.UPLOAD ?
                "Uploaded: " + com.scamshield.app.data.model.NetworkTrafficItem.formatBytes(item.getBytesUploaded()) :
                "Downloaded: " + com.scamshield.app.data.model.NetworkTrafficItem.formatBytes(item.getBytesDownloaded());

        String details = "App: " + item.getAppName() + " (" + item.getPackageName() + ")\n" +
                "Destination: " + item.getDestinationHost() + " (" + item.getDestinationIp() + ":" + item.getDestinationPort() + ")\n" +
                "Traffic: " + dataAmount + " via " + item.getProtocol() + "\n\n" +
                "Threat Assessment:\n• " + item.getWhyFlagged() + "\n\n" +
                "Action: " + item.getRecommendedAction();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_NETWORK)
                .setSmallIcon(R.drawable.ic_shield_check)
                .setContentTitle(title)
                .setContentText(item.getWhyFlagged())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(details))
                .setColor(color)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_action_link, "Inspect Traffic", pendingIntent);

        // Action to open Android System App Settings so user can restrict background data or uninstall
        if (item.getPackageName() != null && !item.getPackageName().isEmpty()) {
            Intent settingsIntent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            settingsIntent.setData(android.net.Uri.parse("package:" + item.getPackageName()));
            PendingIntent settingsPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 2,
                    settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            builder.addAction(R.drawable.ic_device_scan, "Restrict App", settingsPendingIntent);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}
