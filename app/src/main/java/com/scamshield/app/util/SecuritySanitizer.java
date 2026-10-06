package com.scamshield.app.util;

public class SecuritySanitizer {

    // Maximum input length allowed to prevent memory exhaustion DOS
    public static final int MAX_INPUT_LENGTH = 100_000;

    /**
     * Sanitizes user or external input:
     * - Truncates excessive payloads
     * - Strips null bytes and control characters
     * - Trims surrounding whitespace
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";

        String sanitized = input;
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
        }

        // Remove dangerous control characters while preserving standard newlines/tabs
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return sanitized.trim();
    }

    /**
     * Validates if the content has meaningful content to analyze.
     */
    public static boolean isValidContent(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return !trimmed.isEmpty() && trimmed.length() >= 3;
    }
}
