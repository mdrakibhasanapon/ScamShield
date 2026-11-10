package com.scamshield.app.domain.detector;

import java.util.Locale;

public class WebThreatFilter {

    // Common phishing TLDs and known fraud patterns
    private static final String[] HIGH_RISK_TLDS = {
            ".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".xyz", ".buzz", ".work", ".click", ".fit", ".rest"
    };

    private static final String[] FRAUD_DOMAIN_KEYWORDS = {
            "bkash-login", "nagad-reward", "free-recharge", "account-verify", "bank-update",
            "crypto-giveaway", "claim-bonus", "secure-login", "000webhost", "ngrok-free"
    };

    public static boolean isMaliciousDomainOrHost(String host) {
        if (host == null || host.trim().isEmpty()) return false;

        String lower = host.toLowerCase(Locale.ROOT);

        // Check for direct numeric IP accessing web traffic
        if (lower.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            // Direct IP without hostname is suspicious in consumer browsing
            return true;
        }

        // Check high risk phishing TLDs
        for (String tld : HIGH_RISK_TLDS) {
            if (lower.endsWith(tld)) {
                return true;
            }
        }

        // Check fraud domain keywords
        for (String kw : FRAUD_DOMAIN_KEYWORDS) {
            if (lower.contains(kw)) {
                return true;
            }
        }

        // Check punycode xn-- spoofing
        if (lower.contains("xn--")) {
            return true;
        }

        return false;
    }
}
