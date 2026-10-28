package com.scamshield.app.data.model;

public enum IndicatorCategory {
    URGENCY("Urgency & Pressure"),
    FINANCIAL_DEMAND("Financial & Payment"),
    CREDENTIAL_THEFT("Credential Harvesting"),
    IMPERSONATION("Impersonation"),
    URL_ANOMALY("Link & Domain Anomalies"),
    PRIZE_BAIT("Unsolicited Prize / Lottery"),
    DELIVERY_SCAM("Parcel & Delivery Fraud"),
    SOCIAL_ENGINEERING("Social Engineering Patterns"),
    MALICIOUS_DOWNLOAD("Malicious File & Download"),
    DATA_EXFILTRATION("Data Exfiltration & Privacy Leak");

    private final String displayName;

    IndicatorCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
