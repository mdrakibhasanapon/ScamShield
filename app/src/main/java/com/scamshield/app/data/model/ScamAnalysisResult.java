package com.scamshield.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScamAnalysisResult implements Serializable {
    private final String id;
    private final String submittedContent;
    private final ScanType scanType;
    private final int riskScore; // 0 - 100
    private final RiskLevel riskLevel;
    private final List<DetectedIndicator> indicators;
    private final List<String> whyFlagged;
    private final List<String> recommendedActions;
    private final List<String> humanVerificationNotes;
    private final boolean isOfflineResult;
    private final long timestamp;

    public ScamAnalysisResult(
            String id,
            String submittedContent,
            ScanType scanType,
            int riskScore,
            RiskLevel riskLevel,
            List<DetectedIndicator> indicators,
            List<String> whyFlagged,
            List<String> recommendedActions,
            List<String> humanVerificationNotes,
            boolean isOfflineResult,
            long timestamp) {
        this.id = id;
        this.submittedContent = submittedContent;
        this.scanType = scanType;
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.fromScore(this.riskScore);
        this.indicators = indicators != null ? indicators : new ArrayList<>();
        this.whyFlagged = whyFlagged != null ? whyFlagged : new ArrayList<>();
        this.recommendedActions = recommendedActions != null ? recommendedActions : new ArrayList<>();
        this.humanVerificationNotes = humanVerificationNotes != null ? humanVerificationNotes : new ArrayList<>();
        this.isOfflineResult = isOfflineResult;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getSubmittedContent() {
        return submittedContent;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<DetectedIndicator> getIndicators() {
        return indicators;
    }

    public List<String> getWhyFlagged() {
        return whyFlagged;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public List<String> getHumanVerificationNotes() {
        return humanVerificationNotes;
    }

    public boolean isOfflineResult() {
        return isOfflineResult;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSnippet(int maxLength) {
        if (submittedContent == null) return "";
        String clean = submittedContent.trim().replaceAll("\\s+", " ");
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength) + "…";
    }
}
