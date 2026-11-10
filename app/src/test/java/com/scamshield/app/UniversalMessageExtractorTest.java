package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.domain.engine.LocalHeuristicEngine;
import com.scamshield.app.service.UniversalMessageListenerService;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests verifying Universal Messaging Watchdog (WhatsApp, Telegram, Messenger, Signal, etc.)
 * heuristic scoring and message analysis capabilities.
 */
public class UniversalMessageExtractorTest {

    private LocalHeuristicEngine engine;

    @Before
    public void setUp() {
        engine = new LocalHeuristicEngine();
    }

    @Test
    public void testWhatsAppBankKyCOtpScam() {
        String message = "URGENT: Your Chase checking debit card is temporarily blocked due to unauthorized login attempts. To prevent permanent closure, verify your 4-digit ATM PIN and SSN immediately at http://chase-security-verify.top/login";
        ScamAnalysisResult result = engine.evaluateContent(message, ScanType.CHAT_MESSAGE, true);

        assertNotNull(result);
        assertEquals(ScanType.CHAT_MESSAGE, result.getScanType());
        assertTrue("Risk score should be high for WhatsApp bank phishing, actual: " + result.getRiskScore(), result.getRiskScore() >= 75);
        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertFalse("Indicators must not be empty", result.getIndicators().isEmpty());
    }

    @Test
    public void testTelegramCryptoGiveawayScam() {
        String message = "Congratulations! You have received 2.5 ETH in your VIP wallet. Send 0.08 ETH gas fee to claim before timer expires: https://claim-crypto-giveaway.xyz";
        ScamAnalysisResult result = engine.evaluateContent(message, ScanType.CHAT_MESSAGE, true);

        assertNotNull(result);
        assertEquals(ScanType.CHAT_MESSAGE, result.getScanType());
        assertTrue("Risk score should be elevated for Telegram crypto scam, actual: " + result.getRiskScore(), result.getRiskScore() >= 50);
        assertTrue("Risk level should be SUSPICIOUS or HIGH_RISK",
                result.getRiskLevel() == RiskLevel.SUSPICIOUS || result.getRiskLevel() == RiskLevel.HIGH_RISK);
    }

    @Test
    public void testMessengerEmergencyImpersonationScam() {
        String message = "Hey man! I'm stuck at Heathrow Airport and lost my wallet. Can you urgently send me $400 via Apple Gift Cards or Wire? I will pay you back double tomorrow morning please hurry!";
        ScamAnalysisResult result = engine.evaluateContent(message, ScanType.CHAT_MESSAGE, true);

        assertNotNull(result);
        assertEquals(ScanType.CHAT_MESSAGE, result.getScanType());
        assertTrue("Risk score should indicate threat for gift card / wire extortion, actual: " + result.getRiskScore(), result.getRiskScore() >= 30);
        assertTrue("Risk level should not be LOW_RISK", result.getRiskLevel() != RiskLevel.LOW_RISK);
    }

    @Test
    public void testCleanChatMessage() {
        String message = "Hey! Are we still meeting for lunch at 1 PM today? Let me know if that works for you.";
        ScamAnalysisResult result = engine.evaluateContent(message, ScanType.CHAT_MESSAGE, true);

        assertNotNull(result);
        assertEquals(ScanType.CHAT_MESSAGE, result.getScanType());
        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertTrue("Score should be low for normal chat, actual: " + result.getRiskScore(), result.getRiskScore() <= 20);
        assertTrue(result.getIndicators().isEmpty());
    }

    @Test
    public void testScanTypeLabel() {
        assertEquals("Chat App Message (WhatsApp/Telegram/Messenger)", ScanType.CHAT_MESSAGE.getLabel());
    }
}
