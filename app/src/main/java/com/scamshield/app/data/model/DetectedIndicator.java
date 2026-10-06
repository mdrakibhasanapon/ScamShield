package com.scamshield.app.data.model;

import java.io.Serializable;

public class DetectedIndicator implements Serializable {
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    private final String id;
    private final String title;
    private final String explanation;
    private final Severity severity;
    private final IndicatorCategory category;
    private final String evidenceSnippet;

    public DetectedIndicator(String id, String title, String explanation, Severity severity, IndicatorCategory category, String evidenceSnippet) {
        this.id = id;
        this.title = title;
        this.explanation = explanation;
        this.severity = severity;
        this.category = category;
        this.evidenceSnippet = evidenceSnippet;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }

    public Severity getSeverity() {
        return severity;
    }

    public IndicatorCategory getCategory() {
        return category;
    }

    public String getEvidenceSnippet() {
        return evidenceSnippet;
    }
}
