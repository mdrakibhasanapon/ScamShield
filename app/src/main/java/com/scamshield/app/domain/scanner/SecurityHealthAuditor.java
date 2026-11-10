package com.scamshield.app.domain.scanner;

import android.content.Context;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.util.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

public class SecurityHealthAuditor {

    public static class HealthReport {
        private final int scorePercentage;
        private final String grade;
        private final String statusTitle;
        private final List<String> activeShields;
        private final List<String> recommendations;

        public HealthReport(int scorePercentage, String grade, String statusTitle, List<String> activeShields, List<String> recommendations) {
            this.scorePercentage = scorePercentage;
            this.grade = grade;
            this.statusTitle = statusTitle;
            this.activeShields = activeShields;
            this.recommendations = recommendations;
        }

        public int getScorePercentage() {
            return scorePercentage;
        }

        public String getGrade() {
            return grade;
        }

        public String getStatusTitle() {
            return statusTitle;
        }

        public List<String> getActiveShields() {
            return activeShields;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }
    }

    public static HealthReport generateWeeklyHealthReport(Context context) {
        PreferencesManager prefs = new PreferencesManager(context);
        List<String> activeShields = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 40; // Base baseline

        if (prefs.isRealTimeSmsShieldEnabled()) {
            score += 10;
            activeShields.add("SMS & Text Scam Shield");
        } else {
            recommendations.add("Enable SMS Shield in Settings");
        }

        if (prefs.isUniversalChatWatchdogEnabled() && PermissionHelper.hasNotificationListenerAccess(context)) {
            score += 15;
            activeShields.add("Universal Messaging Watchdog (WhatsApp/Telegram)");
        } else {
            recommendations.add("Grant Notification Access for Universal Messaging Watchdog");
        }

        if (prefs.isMicShieldEnabled() && PermissionHelper.hasMicPermission(context)) {
            score += 10;
            activeShields.add("Voice Phishing & Vishing Shield");
        } else {
            recommendations.add("Enable Voice Phishing Protection in Home dashboard");
        }

        if (prefs.isLocationRadarEnabled() && PermissionHelper.hasLocationPermission(context)) {
            score += 10;
            activeShields.add("Regional Threat Radar & Rogue Wi-Fi Guard");
        } else {
            recommendations.add("Enable Location Radar for Wi-Fi threat detection");
        }

        if (prefs.isClipboardGuardEnabled()) {
            score += 5;
            activeShields.add("Clipboard Live Copy Guard");
        }

        if (prefs.isCallSafetyShieldEnabled()) {
            score += 5;
            activeShields.add("Incoming Call OTP Safety Shield");
        }

        if (prefs.isFinancialShieldEnabled()) {
            score += 5;
            activeShields.add("bKash & Payment Fraud Shield");
        }

        int finalScore = Math.min(100, score);
        String grade;
        String statusTitle;

        if (finalScore >= 90) {
            grade = "A+";
            statusTitle = "🛡️ Maximum Protection Active";
        } else if (finalScore >= 75) {
            grade = "A";
            statusTitle = "✅ High Defense Readiness";
        } else if (finalScore >= 60) {
            grade = "B";
            statusTitle = "⚠️ Moderate Protection (Action Recommended)";
        } else {
            grade = "C";
            statusTitle = "🚨 Vulnerabilities Detected";
        }

        return new HealthReport(finalScore, grade, statusTitle, activeShields, recommendations);
    }
}
