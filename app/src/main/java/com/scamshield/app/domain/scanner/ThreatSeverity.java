package com.scamshield.app.domain.scanner;

import com.scamshield.app.R;

public enum ThreatSeverity {
    CRITICAL("Critical Risk", R.color.risk_high, R.color.risk_high_bg),
    HIGH("High Risk", R.color.risk_high, R.color.risk_high_bg),
    MEDIUM("Suspicious", R.color.risk_suspicious, R.color.risk_suspicious_bg),
    LOW("Needs Review", R.color.risk_attention, R.color.risk_attention_bg),
    SAFE("Clean", R.color.risk_low, R.color.risk_low_bg);

    private final String label;
    private final int colorRes;
    private final int bgRes;

    ThreatSeverity(String label, int colorRes, int bgRes) {
        this.label = label;
        this.colorRes = colorRes;
        this.bgRes = bgRes;
    }

    public String getLabel() {
        return label;
    }

    public int getColorRes() {
        return colorRes;
    }

    public int getBgRes() {
        return bgRes;
    }
}
