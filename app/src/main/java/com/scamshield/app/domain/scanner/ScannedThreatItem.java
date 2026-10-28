package com.scamshield.app.domain.scanner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item identified during a full device scan (App, File, or Setting).
 */
public class ScannedThreatItem implements Serializable {

    private final String id;
    private final String title;
    private final String subtitle;
    private final ThreatType type;
    private final ThreatSeverity severity;
    private final int riskScore;
    private final String threatName;
    private final String description;
    private final List<String> reasons;
    private final String packageName;
    private final String filePath;
    private final String fileSizeFormatted;
    private final boolean isSystemApp;
    private boolean isResolved;

    public ScannedThreatItem(String id, String title, String subtitle, ThreatType type,
                             ThreatSeverity severity, int riskScore, String threatName,
                             String description, List<String> reasons, String packageName,
                             String filePath, String fileSizeFormatted, boolean isSystemApp) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
        this.severity = severity;
        this.riskScore = riskScore;
        this.threatName = threatName;
        this.description = description;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.packageName = packageName;
        this.filePath = filePath;
        this.fileSizeFormatted = fileSizeFormatted;
        this.isSystemApp = isSystemApp;
        this.isResolved = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public ThreatType getType() {
        return type;
    }

    public ThreatSeverity getSeverity() {
        return severity;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getThreatName() {
        return threatName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileSizeFormatted() {
        return fileSizeFormatted;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public boolean isDangerous() {
        return severity == ThreatSeverity.CRITICAL || severity == ThreatSeverity.HIGH || severity == ThreatSeverity.MEDIUM;
    }
}
