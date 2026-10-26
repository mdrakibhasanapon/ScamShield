package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.domain.engine.LocalHeuristicEngine;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests verifying real-time incoming SMS screening and heuristic scoring.
 */
public class IncomingSmsScoringTest {

    private LocalHeuristicEngine engine;

    @Before
    public void setUp() {
        engine = new LocalHeuristicEngine();
    }

    @Test
    public void testIncomingBankPhishingSms() {
        String incomingSms = "URGENT BANK ALERT: Suspicious debit detected. Your card is temporarily locked immediately. Verify your credentials at http://bank-update-security.icu";
        ScamAnalysisResult result = engine.evaluateContent(incomingSms, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertTrue("Risk score should be high for bank phishing SMS, actual: " + result.getRiskScore(), result.getRiskScore() >= 75);
        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertFalse("Should contain detected indicators", result.getIndicators().isEmpty());
    }

    @Test
    public void testIncomingPackageDeliveryScamSms() {
        String incomingSms = "USPS: We cannot deliver your parcel due to unpaid customs fee of $1.85. Settle fee immediately: https://bit.ly/usps-fee-pay";
        ScamAnalysisResult result = engine.evaluateContent(incomingSms, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertTrue("Risk score should indicate threat for parcel fee scam, actual: " + result.getRiskScore(), result.getRiskScore() >= 51);
        assertTrue("Risk level should be SUSPICIOUS or HIGH_RISK", 
                result.getRiskLevel() == RiskLevel.SUSPICIOUS || result.getRiskLevel() == RiskLevel.HIGH_RISK);
    }

    @Test
    public void testIncomingCleanPersonalSms() {
        String incomingSms = "Hey, are you free for a coffee catchup today around 3 PM at Central Park?";
        ScamAnalysisResult result = engine.evaluateContent(incomingSms, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertTrue("Risk score must be <= 20 for clean personal message, actual: " + result.getRiskScore(), result.getRiskScore() <= 20);
        assertTrue("No indicators should be triggered", result.getIndicators().isEmpty());
    }

    @Test
    public void testIncomingCryptoGiveawayScamSms() {
        String incomingSms = "Congratulations! You have been selected to win 1.5 BTC in our giveaway. Send 0.05 BTC gas fee to claim payout wallet.";
        ScamAnalysisResult result = engine.evaluateContent(incomingSms, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertTrue("Crypto giveaway fee scam must score threat level (actual: " + result.getRiskScore() + ")", result.getRiskScore() >= 50);
        assertTrue(result.getRiskLevel() == RiskLevel.SUSPICIOUS || result.getRiskLevel() == RiskLevel.HIGH_RISK);
    }
}
