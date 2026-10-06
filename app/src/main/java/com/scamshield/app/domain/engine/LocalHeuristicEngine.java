package com.scamshield.app.domain.engine;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.domain.detector.TextIndicatorDetector;
import com.scamshield.app.domain.detector.UrlIndicatorDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocalHeuristicEngine implements ScamAnalysisEngine {

    @Override
    public void analyze(String content, ScanType scanType, AnalysisCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            if (callback != null) callback.onError("Input content cannot be empty.");
            return;
        }

        ScamAnalysisResult result = evaluateContent(content, scanType, true);
        if (callback != null) {
            callback.onSuccess(result);
        }
    }

    public ScamAnalysisResult evaluateContent(String content, ScanType scanType, boolean isOffline) {
        List<DetectedIndicator> indicators = new ArrayList<>();

        // 1. Text Heuristics
        List<DetectedIndicator> textIndicators = TextIndicatorDetector.analyzeText(content);
        indicators.addAll(textIndicators);

        // 2. URL Heuristics
        List<String> urlsToAnalyze = new ArrayList<>();
        if (scanType == ScanType.URL || scanType == ScanType.QR_CODE) {
            urlsToAnalyze.add(content.trim());
        } else {
            urlsToAnalyze = UrlIndicatorDetector.extractUrls(content);
        }

        for (String url : urlsToAnalyze) {
            List<DetectedIndicator> urlIndicators = UrlIndicatorDetector.analyzeUrl(url);
            indicators.addAll(urlIndicators);
        }

        // 3. Compute Risk Score (0 - 100)
        int rawScore = 0;
        int criticalCount = 0;
        int warningCount = 0;

        for (DetectedIndicator indicator : indicators) {
            switch (indicator.getSeverity()) {
                case CRITICAL:
                    rawScore += 35;
                    criticalCount++;
                    break;
                case WARNING:
                    rawScore += 18;
                    warningCount++;
                    break;
                case INFO:
                    rawScore += 8;
                    break;
            }
        }

        // Additional synergy penalty: multiple critical vectors (e.g. urgency + credential theft)
        if (criticalCount >= 2) {
            rawScore += 15;
        }

        int normalizedScore = Math.max(0, Math.min(100, rawScore));
        if (indicators.isEmpty()) {
            // Default baseline for clean content (5/100, Low risk)
            normalizedScore = 5;
        }

        RiskLevel riskLevel = RiskLevel.fromScore(normalizedScore);

        // 4. Build "Why Flagged" bullets
        List<String> whyFlagged = new ArrayList<>();
        if (indicators.isEmpty()) {
            whyFlagged.add("No obvious scam indicators detected in the submitted content.");
            whyFlagged.add("Does not contain recognized high-risk financial, urgency, or credential threats.");
        } else {
            for (DetectedIndicator ind : indicators) {
                whyFlagged.add(ind.getTitle() + ": " + ind.getExplanation());
            }
        }

        // 5. Build Contextual Recommendations
        List<String> recommendedActions = new ArrayList<>();
        if (riskLevel == RiskLevel.HIGH_RISK) {
            recommendedActions.add("Do not click any links or download attachments from this message.");
            recommendedActions.add("Do not send money, gift cards, or cryptocurrency under any circumstances.");
            recommendedActions.add("Do not enter your password, PIN, or Social Security Number.");
            recommendedActions.add("Block the sender and report the communication as spam or phishing.");
            recommendedActions.add("Contact the purported organization directly using a phone number or website you locate independently.");
        } else if (riskLevel == RiskLevel.SUSPICIOUS) {
            recommendedActions.add("Proceed with high caution. Do not respond directly to this message.");
            recommendedActions.add("Verify the claim independently through the organization's official app or portal.");
            recommendedActions.add("Inspect any embedded links carefully for subtle misspellings.");
        } else if (riskLevel == RiskLevel.NEEDS_ATTENTION) {
            recommendedActions.add("Review the sender address carefully before taking any action.");
            recommendedActions.add("Never share one-time security codes (OTP) with anyone.");
        } else {
            recommendedActions.add("Content appears standard, but always exercise healthy digital caution.");
            recommendedActions.add("Never disclose passwords or financial PINs via unsolicited communications.");
        }

        // 6. Human Verification Notes
        List<String> humanVerificationNotes = new ArrayList<>();
        humanVerificationNotes.add("Sender Identity: Scammers frequently spoof Caller ID, SMS headers, and email display names.");
        humanVerificationNotes.add("Independent Lookup: Always type official web addresses manually rather than tapping links.");
        humanVerificationNotes.add("Evolving Tactics: Automated checks cannot guarantee safety. If an offer feels too good to be true, verify with family or trusted advisors.");

        return new ScamAnalysisResult(
                UUID.randomUUID().toString(),
                content,
                scanType,
                normalizedScore,
                riskLevel,
                indicators,
                whyFlagged,
                recommendedActions,
                humanVerificationNotes,
                isOffline,
                System.currentTimeMillis()
        );
    }
}
