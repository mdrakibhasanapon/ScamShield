package com.scamshield.app.domain.detector;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class FinancialFraudDetector {

    // Regex patterns for fake bKash / Nagad / Rocket transaction alerts
    private static final Pattern TRX_PATTERN = Pattern.compile("(?i)(TrxID|TxnId|Transaction ID)[:\\s]*[A-Z0-9]{8,12}");
    private static final Pattern FAKE_MISTAKEN_TRANSFER = Pattern.compile("(?i)(ভুল করে|mistake|accidentally sent|send back|টাকা ফেরত|ফেরত দিন|বিকাশ করেছি|টাকা পাঠিয়েছি)");
    private static final Pattern OTP_PIN_REQUEST = Pattern.compile("(?i)(OTP|PIN|verification code|পিন কোড|ওটিপি|গোপন নম্বর|বিকাশ পিন)");
    private static final Pattern BALANCE_FAKE_UPDATE = Pattern.compile("(?i)(New Balance Tk|Fee Tk 0\\.00|Balance Tk|You have received Tk)");

    public static ScamAnalysisResult analyzeFinancialText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ScamAnalysisResult(
                    UUID.randomUUID().toString(),
                    "No financial text provided.",
                    ScanType.FINANCIAL_SMS,
                    0,
                    RiskLevel.LOW_RISK,
                    Collections.emptyList(),
                    Collections.singletonList("Financial text analysis is clear."),
                    Collections.singletonList("No payment fraud detected."),
                    Collections.emptyList(),
                    true,
                    System.currentTimeMillis()
            );
        }

        String lower = text.toLowerCase(Locale.ROOT);
        List<DetectedIndicator> indicators = new ArrayList<>();
        int score = 0;

        boolean hasTrx = TRX_PATTERN.matcher(text).find() || lower.contains("trxid") || lower.contains("balance tk");
        boolean hasMistaken = FAKE_MISTAKEN_TRANSFER.matcher(text).find();
        boolean hasOtpRequest = OTP_PIN_REQUEST.matcher(text).find();

        if (hasMistaken && (hasTrx || lower.contains("tk") || lower.contains("টাকা"))) {
            score += 85;
            indicators.add(new DetectedIndicator(
                    UUID.randomUUID().toString(),
                    "Fake 'Accidental Money Sent' Scam (ভুল করে টাকা পাঠানো প্রতারণা)",
                    "Message claims money was sent by mistake and urgently demands refund. Scammers spoof SMS headers or send fake TrxID alerts without actually sending any balance.",
                    DetectedIndicator.Severity.CRITICAL,
                    IndicatorCategory.FINANCIAL_DEMAND,
                    "Accidental transfer / refund demand pattern detected"
            ));
        }

        if (hasOtpRequest && (lower.contains("share") || lower.contains("বলুন") || lower.contains("দিন") || lower.contains("verify") || lower.contains("dial") || lower.contains("send") || lower.contains("reply") || lower.contains("code"))) {
            score += 80;
            indicators.add(new DetectedIndicator(
                    UUID.randomUUID().toString(),
                    "OTP / Secret PIN Theft Attempt (পিন বা ওটিপি চুরির চেষ্টা)",
                    "Never share your bKash, Nagad, or Bank PIN/OTP. Official representatives will NEVER ask for your secret PIN or OTP codes.",
                    DetectedIndicator.Severity.CRITICAL,
                    IndicatorCategory.CREDENTIAL_THEFT,
                    "OTP/PIN sharing request pattern"
            ));
        }

        if (lower.contains("bkash prize") || lower.contains("লটারি") || lower.contains("বোনাস") || lower.contains("prize winner") || lower.contains("nagad offer")) {
            score += 35;
            indicators.add(new DetectedIndicator(
                    UUID.randomUUID().toString(),
                    "Fake Lottery / Bonus Trap (ভুয়া পুরস্কার বা বোনাস প্রলোভন)",
                    "Unsolicited lottery or prize notices requesting registration fees or wallet PINs are fraudulent.",
                    DetectedIndicator.Severity.WARNING,
                    IndicatorCategory.PRIZE_BAIT,
                    "Lottery / Bonus trigger"
            ));
        }

        int normalizedScore = Math.min(100, score);
        if (indicators.isEmpty()) {
            normalizedScore = hasTrx ? 15 : 5;
        }

        RiskLevel level = RiskLevel.fromScore(normalizedScore);
        List<String> why = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if (level == RiskLevel.HIGH_RISK || level == RiskLevel.SUSPICIOUS) {
            why.add("Financial / MFS Extortion Detected: Message exhibits characteristics of a fake bKash/Nagad transfer or OTP social engineering trick.");
            actions.add("DO NOT send money back immediately. Always check your actual bKash/Nagad/Bank app balance or dial *247# / *167# directly.");
            actions.add("NEVER share your secret 5-digit PIN or 6-digit OTP verification code with anyone over phone or SMS.");
        } else {
            why.add("Normal financial transaction pattern. No extortion or credential phishing triggers detected.");
            actions.add("Always verify your balance through your official banking or wallet app.");
        }

        return new ScamAnalysisResult(
                UUID.randomUUID().toString(),
                text,
                ScanType.FINANCIAL_SMS,
                normalizedScore,
                level,
                indicators,
                why,
                actions,
                Collections.emptyList(),
                true,
                System.currentTimeMillis()
        );
    }
}
