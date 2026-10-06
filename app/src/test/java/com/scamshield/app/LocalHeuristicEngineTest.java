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

public class LocalHeuristicEngineTest {

    private LocalHeuristicEngine engine;

    @Before
    public void setUp() {
        engine = new LocalHeuristicEngine();
    }

    @Test
    public void testCleanMessageProducesLowRisk() {
        String benignMessage = "Hi Mom, are we still meeting for lunch at 12:30 tomorrow?";
        ScamAnalysisResult result = engine.evaluateContent(benignMessage, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertTrue(result.getRiskScore() <= 20);
        assertTrue(result.getIndicators().isEmpty());
        assertFalse(result.getWhyFlagged().isEmpty());
    }

    @Test
    public void testUrgentBankPhishingProducesHighRisk() {
        String bankScam = "URGENT BANK ALERT: Your account is locked immediately. Enter your password and SSN at http://chase-security-verify.top/login";
        ScamAnalysisResult result = engine.evaluateContent(bankScam, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertTrue(result.getRiskScore() >= 76);
        assertFalse(result.getIndicators().isEmpty());
        assertTrue(result.getRecommendedActions().size() >= 3);
    }

    @Test
    public void testGiftCardDemandsTriggerCriticalIndicator() {
        String giftCardScam = "This is tech support. You have a virus. Buy apple gift card and scratch the back to pay.";
        ScamAnalysisResult result = engine.evaluateContent(giftCardScam, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 35);
        boolean foundGiftCard = result.getIndicators().stream()
                .anyMatch(i -> "TEXT_GIFT_CARD".equals(i.getId()));
        assertTrue(foundGiftCard);
    }

    @Test
    public void testCryptoBaitTriggersFinancialIndicator() {
        String cryptoScam = "Double your bitcoin! Send 0.1 BTC to bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq to claim 1.0 BTC bonus.";
        ScamAnalysisResult result = engine.evaluateContent(cryptoScam, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 35);
        boolean foundCrypto = result.getIndicators().stream()
                .anyMatch(i -> "TEXT_CRYPTO_FRAUD".equals(i.getId()) || "TEXT_BTC_ADDRESS".equals(i.getId()));
        assertTrue(foundCrypto);
    }

    @Test
    public void testDeliveryFeeSmishing() {
        String deliveryScam = "USPS: Your parcel delivery failed due to unpaid delivery fee. Verify address at https://bit.ly/usps-fix";
        ScamAnalysisResult result = engine.evaluateContent(deliveryScam, ScanType.MESSAGE, true);

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 35);
        boolean foundDelivery = result.getIndicators().stream()
                .anyMatch(i -> "TEXT_DELIVERY_FRAUD".equals(i.getId()));
        assertTrue(foundDelivery);
    }
}
