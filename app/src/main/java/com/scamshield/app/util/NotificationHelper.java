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
}
