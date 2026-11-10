package com.scamshield.app;

import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.domain.detector.FinancialFraudDetector;
import com.scamshield.app.domain.detector.WebThreatFilter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinancialFraudDetectorTest {

    @Test
    public void testFakeMistakenMoneySentScam() {
        String fakeSms = "You have received Tk 10,000.00 from 01712345678. Ref: Gift. TrxID 9K7A2BC1D8. ভাই ভুল করে ১০,০০০ টাকা চলে গেছে দয়া করে এখনই টাকা ফেরত দিন";
        ScamAnalysisResult result = FinancialFraudDetector.analyzeFinancialText(fakeSms);

        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertTrue(result.getRiskScore() >= 50);
        assertFalse(result.getIndicators().isEmpty());
    }

    @Test
    public void testOtpPinTheftAttempt() {
        String otpTheft = "bKash Security Alert: Your account is suspended. Verify your secret PIN and OTP immediately by replying to this SMS";
        ScamAnalysisResult result = FinancialFraudDetector.analyzeFinancialText(otpTheft);

        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertTrue(result.getRiskScore() >= 50);
    }

    @Test
    public void testLegitimatePaymentTransfer() {
        String cleanSms = "You have received Tk 250.00 from 01987654321. Ref: Snacks. Balance Tk 300.00. TrxID 4J8K2L1M9N.";
        ScamAnalysisResult result = FinancialFraudDetector.analyzeFinancialText(cleanSms);

        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertTrue(result.getRiskScore() <= 20);
    }

    @Test
    public void testWebThreatFilter() {
        // High risk phishing TLDs
        assertTrue(WebThreatFilter.isMaliciousDomainOrHost("login-update.tk"));
        assertTrue(WebThreatFilter.isMaliciousDomainOrHost("bkash-bonus.xyz"));
        assertTrue(WebThreatFilter.isMaliciousDomainOrHost("free-recharge.buzz"));

        // Direct IP addressing
        assertTrue(WebThreatFilter.isMaliciousDomainOrHost("192.168.1.100"));

        // Punycode spoofing
        assertTrue(WebThreatFilter.isMaliciousDomainOrHost("xn--gogle-pra.com"));

        // Legitimate domains
        assertFalse(WebThreatFilter.isMaliciousDomainOrHost("google.com"));
        assertFalse(WebThreatFilter.isMaliciousDomainOrHost("bkash.com"));
        assertFalse(WebThreatFilter.isMaliciousDomainOrHost("github.com"));
    }
}
