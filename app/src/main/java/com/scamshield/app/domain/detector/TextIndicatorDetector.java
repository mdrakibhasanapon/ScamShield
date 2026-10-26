package com.scamshield.app.domain.detector;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextIndicatorDetector {

    // 1. Urgency & Threats
    private static final String[] URGENCY_PATTERNS = {
            "immediately", "within 24 hours", "within 12 hours", "within 48 hours",
            "account will be suspended", "suspended immediately", "final notice",
            "urgent action required", "act now", "immediate action", "limited time",
            "time is running out", "failure to respond will result", "threat of arrest",
            "law enforcement", "warrant for your arrest", "legal consequences",
            "account is locked", "restricted access", "frozen permanently",
            "temporarily locked", "card is frozen", "card is locked", "account is frozen",
            "verify immediately", "verify now", "urgent bank alert"
    };

    // 2. Financial Demand & Irreversible Payments
    private static final String[] FINANCIAL_PATTERNS = {
            "wire transfer", "western union", "moneygram", "send money", "unpaid fee",
            "processing fee", "customs fee", "redelivery fee", "activation deposit",
            "cash app", "zelle transfer", "venmo me", "upfront payment"
    };

    // 3. Gift Card Demands (Classic Impersonation / Tech Support Scam)
    private static final String[] GIFT_CARD_PATTERNS = {
            "apple gift card", "itunes card", "google play card", "target gift card",
            "steam card", "ebay gift card", "buy gift cards", "scratch the back",
            "send the code on the back", "pay with gift cards"
    };

    // 4. Cryptocurrency & Seed Phrase Harvesting
    private static final String[] CRYPTO_PATTERNS = {
            "send btc", "send eth", "bitcoin deposit", "crypto wallet", "gas fee to claim",
            "airdrop claim", "12-word seed phrase", "24-word seed phrase", "private key",
            "secret recovery phrase", "wallet connect", "claim free crypto", "double your bitcoin",
            "gas fee", "btc giveaway", "crypto giveaway", "claim payout"
    };

    private static final Pattern BTC_ADDRESS_PATTERN = Pattern.compile("\\b(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\\b");
    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("\\b0x[a-fA-F0-9]{40}\\b");

    // 5. Credential Harvesting & 2FA / OTP Theft
    private static final String[] CREDENTIAL_PATTERNS = {
            "enter your password", "confirm your pin", "verify your ssn", "social security number",
            "submit your otp", "one-time password", "share your code", "cvv number",
            "card expiration date", "atm pin", "security questions", "verify your credentials",
            "verify credentials", "verify your account"
    };

    // 6. Prize, Lottery & Inheritance Bait
    private static final String[] PRIZE_PATTERNS = {
            "you have won", "lottery winner", "congratulations selected", "inheritance fund",
            "claim your prize", "cash reward", "lucky winner", "unclaimed funds",
            "grant approved", "free gift card reward", "selected for a grant",
            "you have been selected", "selected to win", "giveaway"
    };

    // 7. Delivery & Postal Surcharge Fraud
    private static final String[] DELIVERY_PATTERNS = {
            "parcel delivery failed", "package pending", "unpaid delivery fee",
            "redelivery required", "usps failed delivery", "fedex parcel update",
            "dhl customs clearance", "ups shipping label error", "incomplete address"
    };

    // 8. Fake Job & Work-From-Home Scam
    private static final String[] JOB_SCAM_PATTERNS = {
            "work from home $500", "earn $1000 a day", "no experience required job",
            "package forwarding agent", "mystery shopper payment", "send money for home office equipment",
            "remote data entry $80/hr", "instant job offer via telegram"
    };

    // 9. Secrecy & Social Engineering Pressure
    private static final String[] SECRECY_PATTERNS = {
            "do not tell anyone", "keep this confidential", "do not call the bank",
            "do not inform family", "strictly between us", "confidential transaction"
    };

    public static List<DetectedIndicator> analyzeText(String text) {
        List<DetectedIndicator> indicators = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return indicators;

        String lower = text.toLowerCase(Locale.US);

        // Check 1: Urgency & Threats
        for (String pattern : URGENCY_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_URGENCY",
                        "Urgency & Pressure Tactics",
                        "The message pressures you with phrases like '" + pattern + "'. Scammers use artificial deadlines and threats to prevent careful verification.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.URGENCY,
                        pattern
                ));
                break;
            }
        }

        // Check 2: Financial Demands
        for (String pattern : FINANCIAL_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_FINANCIAL_DEMAND",
                        "Direct / Irreversible Payment Demand",
                        "Requests payment via non-standard or irreversible methods ('" + pattern + "'). Once sent, funds cannot be recalled.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.FINANCIAL_DEMAND,
                        pattern
                ));
                break;
            }
        }

        // Check 3: Gift Cards
        for (String pattern : GIFT_CARD_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_GIFT_CARD",
                        "Gift Card Payment Demand (Major Red Flag)",
                        "Demands payment or verification using gift cards ('" + pattern + "'). No legitimate company or government agency ever accepts gift cards as payment.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.FINANCIAL_DEMAND,
                        pattern
                ));
                break;
            }
        }

        // Check 4: Crypto Addresses & Seed Phrases
        boolean cryptoFound = false;
        for (String pattern : CRYPTO_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_CRYPTO_FRAUD",
                        "Cryptocurrency Request or Seed Phrase Bait",
                        "Mentions cryptocurrency or recovery phrases ('" + pattern + "'). Legitimate entities never ask for wallet seed phrases or upfront gas fees.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.FINANCIAL_DEMAND,
                        pattern
                ));
                cryptoFound = true;
                break;
            }
        }

        if (!cryptoFound) {
            Matcher btcMatcher = BTC_ADDRESS_PATTERN.matcher(text);
            if (btcMatcher.find()) {
                indicators.add(new DetectedIndicator(
                        "TEXT_BTC_ADDRESS",
                        "Bitcoin Wallet Address Detected",
                        "Contains an embedded Bitcoin wallet address (" + btcMatcher.group() + "). Irreversible cryptocurrency transfers are heavily favored in extortion and investment scams.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.FINANCIAL_DEMAND,
                        btcMatcher.group()
                ));
            } else {
                Matcher ethMatcher = ETH_ADDRESS_PATTERN.matcher(text);
                if (ethMatcher.find()) {
                    indicators.add(new DetectedIndicator(
                        "TEXT_ETH_ADDRESS",
                        "Ethereum Wallet Address Detected",
                        "Contains an embedded Ethereum wallet address (" + ethMatcher.group() + ").",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.FINANCIAL_DEMAND,
                        ethMatcher.group()
                    ));
                }
            }
        }

        // Check 5: Credential Harvesting
        for (String pattern : CREDENTIAL_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_CREDENTIAL_HARVESTING",
                        "Credential / Sensitive Data Harvesting",
                        "Directly asks for sensitive security data ('" + pattern + "'). Genuine organizations will never ask for your passwords, OTP codes, or full SSN via text/email.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.CREDENTIAL_THEFT,
                        pattern
                ));
                break;
            }
        }

        // Check 6: Prize / Lottery Bait
        for (String pattern : PRIZE_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_PRIZE_BAIT",
                        "Unsolicited Prize / Lottery Claim",
                        "Claims you won an unentered lottery or reward ('" + pattern + "'). This is textbook advance-fee fraud.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.PRIZE_BAIT,
                        pattern
                ));
                break;
            }
        }

        // Check 7: Delivery / Parcel Surcharge
        for (String pattern : DELIVERY_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_DELIVERY_FRAUD",
                        "Parcel Delivery Surcharge Phishing",
                        "Claims an urgent issue or small fee for a parcel delivery ('" + pattern + "'). A widely automated phishing campaign designed to steal credit card details.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.DELIVERY_SCAM,
                        pattern
                ));
                break;
            }
        }

        // Check 8: Fake Job Scam
        for (String pattern : JOB_SCAM_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_JOB_SCAM",
                        "Unrealistic Job Offer / Work-from-Home Fraud",
                        "Promises high earnings with little to no qualifications ('" + pattern + "'). Often involves fake checks or laundering stolen funds.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.SOCIAL_ENGINEERING,
                        pattern
                ));
                break;
            }
        }

        // Check 9: Secrecy & Isolation Tactics
        for (String pattern : SECRECY_PATTERNS) {
            if (lower.contains(pattern)) {
                indicators.add(new DetectedIndicator(
                        "TEXT_SECRECY",
                        "Isolation / Secrecy Pressure",
                        "Instructs you to keep this interaction secret ('" + pattern + "'). Scammers isolate victims so family or bank tellers cannot intervene.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.SOCIAL_ENGINEERING,
                        pattern
                ));
                break;
            }
        }

        return indicators;
    }
}
