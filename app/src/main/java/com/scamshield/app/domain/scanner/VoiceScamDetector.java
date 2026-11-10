package com.scamshield.app.domain.scanner;

import android.content.Context;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.util.PermissionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class VoiceScamDetector {

    // Common high-pressure voice call phishing phrases (Vishing & AI voice clone scams)
    private static final String[] CRITICAL_VOICE_FRAUD_PATTERNS = {
            "calling from your bank", "fraud department", "security team", "do not hang up",
            "verify your 6-digit", "read me your otp", "temporary safe account", "transfer all funds",
            "arrest warrant issued", "police will arrive", "tax debt", "irs officer", "gift card payment"
    };

    public static boolean isMicActiveAndPermitted(Context context) {
        return PermissionHelper.hasMicPermission(context);
    }

    public static ScamAnalysisResult analyzeVoiceTranscript(String speechText) {
        if (speechText == null || speechText.trim().isEmpty()) {
            return new ScamAnalysisResult(
                    UUID.randomUUID().toString(),
                    "No audio speech detected.",
                    ScanType.MESSAGE,
                    0,
                    RiskLevel.LOW_RISK,
                    Collections.emptyList(),
                    Collections.singletonList("No vocal threat indicators detected in voice audio."),
                    Collections.singletonList("Voice channel is clear."),
                    Collections.emptyList(),
                    true,
                    System.currentTimeMillis()
            );
        }

        String lower = speechText.toLowerCase(Locale.ROOT);
        List<DetectedIndicator> indicators = new ArrayList<>();
        int score = 0;

        for (String pattern : CRITICAL_VOICE_FRAUD_PATTERNS) {
            if (lower.contains(pattern)) {
                score += 30;
                indicators.add(new DetectedIndicator(
                        UUID.randomUUID().toString(),
                        "Voice Phishing (Vishing) Phrase Detected",
                        "Spoken text contains high-pressure extortion phrase: \"" + pattern + "\"",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.URGENCY,
                        pattern
                ));
            }
        }

        int normalizedScore = Math.min(100, score);
        if (indicators.isEmpty()) {
            normalizedScore = 5;
        }

        RiskLevel level = RiskLevel.fromScore(normalizedScore);
        List<String> why = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if (level == RiskLevel.HIGH_RISK || level == RiskLevel.SUSPICIOUS) {
            why.add("Vishing / Voice Call Extortion Pattern Detected: Caller pressures you to transfer funds or share private 2FA codes.");
            actions.add("Hang up immediately. Legitimate banks and government agencies will never ask for your PIN/OTP over the phone.");
            actions.add("Call the official phone number printed on the back of your debit card to verify your account status.");
        } else {
            why.add("Voice conversation analysis shows no recognized extortion or bank impersonation patterns.");
            actions.add("Normal conversational audio verified.");
        }

        return new ScamAnalysisResult(
                UUID.randomUUID().toString(),
                speechText,
                ScanType.MESSAGE,
                normalizedScore,
                level,
                indicators,
                why,
                actions,
                Collections.emptyList(),
                true,
                System.currentTimeMillis()
        );
    }
}
