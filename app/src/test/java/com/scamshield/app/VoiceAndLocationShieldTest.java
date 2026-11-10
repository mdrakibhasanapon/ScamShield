package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.domain.scanner.VoiceScamDetector;

import org.junit.Test;

public class VoiceAndLocationShieldTest {

    @Test
    public void testBankVishingScamAnalysis() {
        String transcript = "This is officer John calling from your bank fraud department. Do not hang up the call. Your debit card is compromised. Read me your 6-digit verification OTP immediately to transfer funds to a temporary safe account.";
        ScamAnalysisResult result = VoiceScamDetector.analyzeVoiceTranscript(transcript);

        assertNotNull(result);
        assertTrue("Risk score should be elevated for bank vishing, actual: " + result.getRiskScore(), result.getRiskScore() >= 60);
        assertTrue("Risk level should be SUSPICIOUS or HIGH_RISK",
                result.getRiskLevel() == RiskLevel.SUSPICIOUS || result.getRiskLevel() == RiskLevel.HIGH_RISK);
        assertFalse("Indicators must not be empty", result.getIndicators().isEmpty());
    }

    @Test
    public void testArrestWarrantVoiceExtortion() {
        String transcript = "An urgent arrest warrant has been issued by the IRS officer. Police will arrive at your home unless you make an immediate gift card payment to clear your debt.";
        ScamAnalysisResult result = VoiceScamDetector.analyzeVoiceTranscript(transcript);

        assertNotNull(result);
        assertTrue("Risk score should be elevated for police extortion, actual: " + result.getRiskScore(), result.getRiskScore() >= 60);
        assertTrue("Risk level should not be LOW_RISK", result.getRiskLevel() != RiskLevel.LOW_RISK);
    }

    @Test
    public void testCleanVoiceConversation() {
        String transcript = "Hey, just calling to see how you're doing and if we are still meeting up for dinner tonight!";
        ScamAnalysisResult result = VoiceScamDetector.analyzeVoiceTranscript(transcript);

        assertNotNull(result);
        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertTrue("Risk score must be low for clean conversation", result.getRiskScore() <= 20);
        assertTrue(result.getIndicators().isEmpty());
    }

    @Test
    public void testEmptyVoiceTranscript() {
        ScamAnalysisResult result = VoiceScamDetector.analyzeVoiceTranscript("");
        assertNotNull(result);
        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertEquals(0, result.getRiskScore());
    }
}
