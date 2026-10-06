package com.scamshield.app.data.model;

public enum ScanType {
    MESSAGE("Message / SMS / Email"),
    URL("Link / URL"),
    SCREENSHOT("Screenshot OCR"),
    QR_CODE("QR Code");

    private final String label;

    ScanType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
