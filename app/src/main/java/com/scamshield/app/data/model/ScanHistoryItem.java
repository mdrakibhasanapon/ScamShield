package com.scamshield.app.data.model;

import java.io.Serializable;

public class ScanHistoryItem implements Serializable {
    private final String id;
    private final long timestamp;
    private final ScanType scanType;
    private final String snippet;
    private final int riskScore;
    private final RiskLevel riskLevel;

    public ScanHistoryItem(String id, long timestamp, ScanType scanType, String snippet, int riskScore, RiskLevel riskLevel) {
        this.id = id;
        this.timestamp = timestamp;
        this.scanType = scanType;
        this.snippet = snippet;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.fromScore(riskScore);
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public String getSnippet() {
        return snippet;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }
}
