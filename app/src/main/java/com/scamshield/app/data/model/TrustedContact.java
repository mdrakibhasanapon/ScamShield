package com.scamshield.app.data.model;

import java.io.Serializable;

public class TrustedContact implements Serializable {

    private final String id;
    private final String name;
    private final String phoneNumber;
    private final String normalizedNumber;
    private final boolean isSuspicious;
    private final String suspiciousReason;
    private final long syncTimestamp;

    public TrustedContact(
            String id,
            String name,
            String phoneNumber,
            String normalizedNumber,
            boolean isSuspicious,
            String suspiciousReason,
            long syncTimestamp) {
        this.id = id != null ? id : "";
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Unnamed Contact";
        this.phoneNumber = phoneNumber != null ? phoneNumber.trim() : "";
        this.normalizedNumber = normalizedNumber != null ? normalizedNumber : normalizePhone(this.phoneNumber);
        this.isSuspicious = isSuspicious;
        this.suspiciousReason = suspiciousReason != null ? suspiciousReason : "";
        this.syncTimestamp = syncTimestamp > 0 ? syncTimestamp : System.currentTimeMillis();
    }

    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9+]", "");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getNormalizedNumber() {
        return normalizedNumber;
    }

    public boolean isSuspicious() {
        return isSuspicious;
    }

    public String getSuspiciousReason() {
        return suspiciousReason;
    }

    public long getSyncTimestamp() {
        return syncTimestamp;
    }

    public String getInitials() {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (!parts[0].isEmpty()) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return "?";
    }
}
