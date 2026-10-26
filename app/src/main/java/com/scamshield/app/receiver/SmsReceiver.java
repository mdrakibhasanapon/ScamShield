package com.scamshield.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.domain.engine.LocalHeuristicEngine;
import com.scamshield.app.util.NotificationHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BroadcastReceiver listening for incoming SMS messages on-device.
 * Automatically analyzes message content for scam, phishing, and fraud indicators,
 * calculates a risk score (0-100), saves to local scan history, and triggers
 * safety notifications.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    public static final String ACTION_SIMULATE_SMS = "com.scamshield.app.SIMULATE_SMS";
    public static final String EXTRA_SIMULATED_SENDER = "extra_simulated_sender";
    public static final String EXTRA_SIMULATED_BODY = "extra_simulated_body";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isRealTimeSmsShieldEnabled()) {
            Log.d(TAG, "Real-time SMS shield is disabled by user. Skipping analysis.");
            return;
        }

        String action = intent.getAction();

        // Handle simulation intent for testing
        if (ACTION_SIMULATE_SMS.equals(action)) {
            String sender = intent.getStringExtra(EXTRA_SIMULATED_SENDER);
            String body = intent.getStringExtra(EXTRA_SIMULATED_BODY);
            if (sender == null || sender.isEmpty()) sender = "+1 (800) 555-0199";
            if (body == null || body.isEmpty()) body = "Bank Alert: Your card is suspended. Verify at http://bank-update-security.icu";
            processIncomingSms(context, sender, body);
            return;
        }

        // Handle official Telephony SMS_RECEIVED action
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            try {
                SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                if (messages == null || messages.length == 0) return;

                // Multi-part SMS messages can arrive in chunks; assemble per originating address
                Map<String, StringBuilder> assembledMessages = new LinkedHashMap<>();

                for (SmsMessage sms : messages) {
                    if (sms == null) continue;

                    String sender = sms.getDisplayOriginatingAddress();
                    if (sender == null || sender.trim().isEmpty()) {
                        sender = sms.getOriginatingAddress();
                    }
                    if (sender == null || sender.trim().isEmpty()) {
                        sender = "Unknown Sender";
                    }

                    String body = sms.getDisplayMessageBody();
                    if (body == null) {
                        body = sms.getMessageBody();
                    }

                    if (body != null) {
                        if (!assembledMessages.containsKey(sender)) {
                            assembledMessages.put(sender, new StringBuilder());
                        }
                        assembledMessages.get(sender).append(body);
                    }
                }

                // Analyze and score each assembled SMS message
                for (Map.Entry<String, StringBuilder> entry : assembledMessages.entrySet()) {
                    String sender = entry.getKey();
                    String fullBody = entry.getValue().toString();
                    processIncomingSms(context, sender, fullBody);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing incoming SMS broadcast: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Core processing method: Runs on-device heuristic engine, calculates score,
     * logs to local SQLite history (if enabled), and surfaces system notifications.
     */
    public static ScamAnalysisResult processIncomingSms(Context context, String sender, String messageBody) {
        if (context == null || messageBody == null || messageBody.trim().isEmpty()) {
            return null;
        }

        try {
            // 1. Run local on-device heuristics (zero internet required, 100% private)
            LocalHeuristicEngine engine = new LocalHeuristicEngine();
            ScamAnalysisResult result = engine.evaluateContent(messageBody, ScanType.MESSAGE, true);

            // 2. Save scan to device history if enabled
            HistoryRepository historyRepository = new HistoryRepository(context);
            historyRepository.saveScan(result);

            // 3. Dispatch notification with threat score and risk breakdown
            NotificationHelper.showSmsScanNotification(context, sender, messageBody, result);

            Log.i(TAG, "Processed incoming SMS from [" + sender + "] with Risk Score: " 
                    + result.getRiskScore() + "/100 (" + result.getRiskLevel().getTitle() + ")");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze SMS: " + e.getMessage(), e);
            return null;
        }
    }
}
