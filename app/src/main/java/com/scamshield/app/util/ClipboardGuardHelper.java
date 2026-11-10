package com.scamshield.app.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.domain.engine.LocalHeuristicEngine;

public class ClipboardGuardHelper {

    private static String lastCheckedSnippet = "";

    public static void checkClipboardOnResume(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        PreferencesManager preferencesManager = new PreferencesManager(activity);
        if (!preferencesManager.isClipboardGuardEnabled()) return;

        try {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) return;

            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData == null || clipData.getItemCount() == 0) return;

            CharSequence textSeq = clipData.getItemAt(0).getText();
            if (textSeq == null) return;

            String copiedText = textSeq.toString().trim();
            if (copiedText.isEmpty() || copiedText.length() < 8) return;
            if (copiedText.equals(lastCheckedSnippet)) return;

            lastCheckedSnippet = copiedText;

            // Analyze text using LocalHeuristicEngine
            LocalHeuristicEngine engine = new LocalHeuristicEngine();
            ScamAnalysisResult result = engine.evaluateContent(copiedText, com.scamshield.app.data.model.ScanType.MESSAGE, true);

            if (result.getRiskLevel() == RiskLevel.HIGH_RISK || result.getRiskLevel() == RiskLevel.SUSPICIOUS) {
                // Vibrate with threat pattern
                VibrationHelper.triggerNotificationVibration(activity, result.getRiskLevel());

                // Show urgent warning dialog with 1-tap clipboard purge
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("⚠️ Clipboard Phishing Alert!")
                        .setMessage("ScamShield detected high-risk scam/phishing indicators in the text copied to your clipboard:\n\n\"" +
                                (copiedText.length() > 100 ? copiedText.substring(0, 97) + "..." : copiedText) +
                                "\"\n\nRisk Score: " + result.getRiskScore() + "/100 (" + result.getRiskLevel().getTitle() + ").\nDo NOT paste or open this link!")
                        .setIcon(R.drawable.ic_shield_check)
                        .setPositiveButton("Purge / Clear Clipboard", (dialog, which) -> {
                            clearClipboard(activity);
                        })
                        .setNegativeButton("Keep Anyway", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        } catch (Exception ignored) {
            // Safe fallback for permission-restricted background clipboards
        }
    }

    public static void clearClipboard(Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData emptyClip = ClipData.newPlainText("", "");
                clipboard.setPrimaryClip(emptyClip);
            }
        } catch (Exception ignored) {}
    }
}
