package com.scamshield.app.domain.scanner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeviceScanSummary implements Serializable {

    private final int totalItemsScanned;
    private final int appsScanned;
    private final int filesScanned;
    private final int settingsAudited;
    private final List<ScannedThreatItem> threatItems;
    private final List<ScannedThreatItem> cleanItems;
    private final int deviceSecurityScore;
    private final long scanDurationMillis;
    private final long timestamp;

    public DeviceScanSummary(int appsScanned, int filesScanned, int settingsAudited,
                             List<ScannedThreatItem> threatItems, List<ScannedThreatItem> cleanItems,
                             long scanDurationMillis) {
        this.appsScanned = appsScanned;
        this.filesScanned = filesScanned;
        this.settingsAudited = settingsAudited;
        this.threatItems = threatItems != null ? threatItems : new ArrayList<>();
        this.cleanItems = cleanItems != null ? cleanItems : new ArrayList<>();
        this.totalItemsScanned = appsScanned + filesScanned + settingsAudited;
        this.scanDurationMillis = scanDurationMillis;
        this.timestamp = System.currentTimeMillis();

        // Calculate 0-100 Device Security Score (100 = completely secure)
        int deductions = 0;
        for (ScannedThreatItem item : this.threatItems) {
            if (item.getSeverity() == ThreatSeverity.CRITICAL) {
                deductions += 30;
            } else if (item.getSeverity() == ThreatSeverity.HIGH) {
                deductions += 18;
            } else if (item.getSeverity() == ThreatSeverity.MEDIUM) {
                deductions += 8;
            } else if (item.getSeverity() == ThreatSeverity.LOW) {
                deductions += 3;
            }
        }
        this.deviceSecurityScore = Math.max(0, 100 - deductions);
    }

    public int getTotalItemsScanned() {
        return totalItemsScanned;
    }

    public int getAppsScanned() {
        return appsScanned;
    }

    public int getFilesScanned() {
        return filesScanned;
    }

    public int getSettingsAudited() {
        return settingsAudited;
    }

    public List<ScannedThreatItem> getThreatItems() {
        return threatItems;
    }

    public List<ScannedThreatItem> getCleanItems() {
        return cleanItems;
    }

    public int getThreatCount() {
        return threatItems.size();
    }

    public int getDeviceSecurityScore() {
        return deviceSecurityScore;
    }

    public long getScanDurationMillis() {
        return scanDurationMillis;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isDeviceSafe() {
        return threatItems.isEmpty();
    }
}
