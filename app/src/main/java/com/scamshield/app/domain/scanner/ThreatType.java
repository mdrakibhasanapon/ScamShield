package com.scamshield.app.domain.scanner;

public enum ThreatType {
    APP("Installed Application"),
    FILE("Downloaded File / APK"),
    SYSTEM_SETTING("System Security Setting");

    private final String label;

    ThreatType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
