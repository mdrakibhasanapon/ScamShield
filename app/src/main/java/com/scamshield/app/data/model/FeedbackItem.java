package com.scamshield.app.data.model;

import java.io.Serializable;

public class FeedbackItem implements Serializable {
    public enum Rating {
        HELPFUL,
        INCORRECT,
        NOT_SURE
    }

    private final String scanId;
    private final Rating rating;
    private final String reason;
    private final long timestamp;

    public FeedbackItem(String scanId, Rating rating, String reason, long timestamp) {
        this.scanId = scanId;
        this.rating = rating;
        this.reason = reason;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public String getScanId() {
        return scanId;
    }

    public Rating getRating() {
        return rating;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
