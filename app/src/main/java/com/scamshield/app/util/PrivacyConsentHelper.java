package com.scamshield.app.util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;

public class PrivacyConsentHelper {

    public static final String FULL_LEGAL_POLICY_TEXT =
            "SCAMSHIELD PRIVACY POLICY & USER DATA AGREEMENT\n" +
            "Last Updated: November 11, 2026\n\n" +
            "1. ZERO CLOUD EXFILTRATION PRINCIPLE\n" +
            "ScamShield is engineered exclusively as an on-device cybersecurity scanner. We do not operate remote data-collection servers, and we never collect, monetize, sell, or rent your personal data, messages, voice audio, or location history.\n\n" +
            "2. ON-DEVICE HEURISTIC EVALUATION\n" +
            "All threat detection algorithms—including SMS and chat analysis, URL structure validation, punycode spoof detection, and OCR image parsing—execute locally using your device CPU. No sensitive content is transmitted across external networks for evaluation.\n\n" +
            "3. MICROPHONE ACCESS (VOICE PHISHING SHIELD)\n" +
            "Microphone permission (android.permission.RECORD_AUDIO) is requested solely for optional real-time voice call phishing detection. Audio transcripts are analyzed ephemerally in device RAM to identify extortion, impersonation, and gift-card payment fraud. Audio is never saved to storage or uploaded to the cloud.\n\n" +
            "4. LOCATION ACCESS (REGIONAL THREAT RADAR)\n" +
            "Location permissions (ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION) are used locally to correlate known regional scam advisories and alert you when connecting to unencrypted or rogue Wi-Fi hotspots. GPS coordinates are never stored or tracked.\n\n" +
            "5. NOTIFICATION ACCESS (UNIVERSAL MESSAGING WATCHDOG)\n" +
            "Notification Listener access (android.permission.BIND_NOTIFICATION_LISTENER_SERVICE) allows ScamShield to inspect incoming message previews from apps like WhatsApp, Telegram, and Messenger strictly to identify phishing URLs. Message texts are discarded immediately after scanning.\n\n" +
            "6. CONTACTS & SAFE SENDERS\n" +
            "Contact access (android.permission.READ_CONTACTS) enables on-device Safe Sender Whitelisting. Recognized contacts are matched in your local SQLite database so legitimate family messages are never flagged as scams. Contacts are never uploaded.\n\n" +
            "7. DATA RETENTION & COMPLETE ERASURE\n" +
            "All scan history and cached risk assessments reside in sandboxed local device storage. You can toggle off any sensor anytime in Settings, or erase all local databases permanently via Settings -> Erase All Data.\n\n" +
            "8. THIRD-PARTY ADVERTISING (GOOGLE ADMOB)\n" +
            "ScamShield uses Google Mobile Ads to display optional banners. AdMob may process pseudonymous device IDs (Advertising ID) in accordance with Google's Advertising Policies.\n\n" +
            "By clicking 'I Agree & Accept', you confirm your consent to this on-device security processing.";

    public static void checkAndShowConsentDialog(Activity activity, PreferencesManager preferencesManager, Runnable onAgreed) {
        if (!preferencesManager.isFirstLaunch()) {
            if (onAgreed != null) {
                onAgreed.run();
            }
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(activity.getString(R.string.consent_dialog_intro)).append("\n\n");
        sb.append(activity.getString(R.string.consent_point_ondevice)).append("\n\n");
        sb.append(activity.getString(R.string.consent_point_mic)).append("\n\n");
        sb.append(activity.getString(R.string.consent_point_location)).append("\n\n");
        sb.append(activity.getString(R.string.consent_point_notif)).append("\n\n");
        sb.append(activity.getString(R.string.consent_point_control));

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.consent_dialog_title)
                .setMessage(sb.toString())
                .setIcon(R.drawable.ic_shield_check)
                .setCancelable(false)
                .setPositiveButton(R.string.consent_btn_agree, (dialog, which) -> {
                    preferencesManager.setFirstLaunchCompleted();
                    dialog.dismiss();
                    if (onAgreed != null) {
                        onAgreed.run();
                    }
                })
                .setNeutralButton(R.string.consent_btn_view_full, (dialog, which) -> {
                    showFullPrivacyPolicyDialog(activity);
                })
                .setNegativeButton(R.string.consent_btn_decline, (dialog, which) -> {
                    dialog.dismiss();
                    activity.finishAffinity();
                })
                .show();
    }

    public static void showFullPrivacyPolicyDialog(Context context) {
        TextView tv = new TextView(context);
        tv.setText(FULL_LEGAL_POLICY_TEXT);
        tv.setTextSize(13f);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        tv.setPadding(padding, padding, padding, padding);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.addView(tv);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.consent_dialog_title)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }
}
