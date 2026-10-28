package com.scamshield.app.data.model;

import java.io.Serializable;
import java.util.UUID;

public class NetworkTrafficItem implements Serializable {

    public enum Direction {
        UPLOAD("Uploading ⬆️"),
        DOWNLOAD("Downloading ⬇️"),
        BIDIRECTIONAL("Exchange 🔁");

        private final String label;
        Direction(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String id;
    private final String packageName;
    private final String appName;
    private final String destinationIp;
    private final String destinationHost;
    private final int destinationPort;
    private final String protocol;
    private final Direction direction;
    private final long bytesUploaded;
    private final long bytesDownloaded;
    private final String payloadSummary;
    private final long timestamp;
    private final int riskScore;
    private final RiskLevel riskLevel;
    private final String threatCategory;
    private final String whyFlagged;
    private final String recommendedAction;

    public NetworkTrafficItem(String id, String packageName, String appName,
                              String destinationIp, String destinationHost, int destinationPort,
                              String protocol, Direction direction, long bytesUploaded,
                              long bytesDownloaded, String payloadSummary, long timestamp,
                              int riskScore, RiskLevel riskLevel, String threatCategory,
                              String whyFlagged, String recommendedAction) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.packageName = packageName;
        this.appName = appName;
        this.destinationIp = destinationIp;
        this.destinationHost = destinationHost != null ? destinationHost : destinationIp;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.direction = direction;
        this.bytesUploaded = bytesUploaded;
        this.bytesDownloaded = bytesDownloaded;
        this.payloadSummary = payloadSummary;
        this.timestamp = timestamp;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.threatCategory = threatCategory;
        this.whyFlagged = whyFlagged;
        this.recommendedAction = recommendedAction;
    }

    public String getId() { return id; }
    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public String getDestinationIp() { return destinationIp; }
    public String getDestinationHost() { return destinationHost; }
    public int getDestinationPort() { return destinationPort; }
    public String getProtocol() { return protocol; }
    public Direction getDirection() { return direction; }
    public long getBytesUploaded() { return bytesUploaded; }
    public long getBytesDownloaded() { return bytesDownloaded; }
    public long getTotalBytes() { return bytesUploaded + bytesDownloaded; }
    public String getPayloadSummary() { return payloadSummary; }
    public long getTimestamp() { return timestamp; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getThreatCategory() { return threatCategory; }
    public String getWhyFlagged() { return whyFlagged; }
    public String getRecommendedAction() { return recommendedAction; }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
