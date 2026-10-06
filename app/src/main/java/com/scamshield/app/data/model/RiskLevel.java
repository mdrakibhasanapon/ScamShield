package com.scamshield.app.data.model;

import com.scamshield.app.R;

public enum RiskLevel {
    LOW_RISK(
            "No obvious scam indicators detected",
            "This content does not show common scam indicators, but ScamShield cannot guarantee that any content is 100% safe. Always remain vigilant.",
            R.color.risk_low,
            R.color.risk_low_bg,
            R.drawable.ic_risk_low
    ),
    NEEDS_ATTENTION(
            "Needs attention",
            "Minor anomalies or unusual patterns were detected. Verify sender identity through independent channels before taking action.",
            R.color.risk_attention,
            R.color.risk_attention_bg,
            R.drawable.ic_risk_attention
    ),
    SUSPICIOUS(
            "Suspicious",
            "Multiple suspicious indicators were detected commonly associated with social engineering and phishing.",
            R.color.risk_suspicious,
            R.color.risk_suspicious_bg,
            R.drawable.ic_risk_suspicious
    ),
    HIGH_RISK(
            "Likely scam",
            "Critical scam patterns detected. Strong indicators of financial fraud, credential harvesting, or deceptive impersonation. Do not interact.",
            R.color.risk_high,
            R.color.risk_high_bg,
            R.drawable.ic_risk_high
    );

    private final String title;
    private final String description;
    private final int colorResId;
    private final int backgroundColorResId;
    private final int iconResId;

    RiskLevel(String title, String description, int colorResId, int backgroundColorResId, int iconResId) {
        this.title = title;
        this.description = description;
        this.colorResId = colorResId;
        this.backgroundColorResId = backgroundColorResId;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getColorResId() {
        return colorResId;
    }

    public int getBackgroundColorResId() {
        return backgroundColorResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    /**
     * Determines RiskLevel from normalized risk score [0..100].
     * 0-20: Low risk
     * 21-50: Needs attention
     * 51-75: Suspicious
     * 76-100: High risk
     */
    public static RiskLevel fromScore(int score) {
        if (score <= 20) {
            return LOW_RISK;
        } else if (score <= 50) {
            return NEEDS_ATTENTION;
        } else if (score <= 75) {
            return SUSPICIOUS;
        } else {
            return HIGH_RISK;
        }
    }
}
