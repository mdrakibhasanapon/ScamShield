package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.NetworkTrafficItem;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.domain.scanner.NetworkThreatEvaluator;

import org.junit.Test;

/**
 * Unit tests verifying real-time network traffic threat evaluation.
 */
public class NetworkThreatEvaluatorTest {

    @Test
    public void testTrojanPortTriggersCriticalRisk() {
        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                "com.suspicious.app",
                "Malicious App",
                "194.87.142.18",
                "remote-server.com",
                4444,
                "TCP",
                NetworkTrafficItem.Direction.UPLOAD,
                15000,
                0,
                null
        );

        assertNotNull(item);
        assertTrue("Port 4444 must score threat >= 90", item.getRiskScore() >= 90);
        assertEquals(RiskLevel.HIGH_RISK, item.getRiskLevel());
        assertEquals("SUSPICIOUS_C2_PORT", item.getThreatCategory());
    }

    @Test
    public void testPlaintextHttpUploadTriggersHighRisk() {
        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                "com.leaky.app",
                "Shopping App",
                "93.184.216.34",
                "api.shop.com",
                80,
                "HTTP",
                NetworkTrafficItem.Direction.UPLOAD,
                24000,
                1200,
                null
        );

        assertNotNull(item);
        assertTrue("Unencrypted HTTP upload must score >= 70", item.getRiskScore() >= 70);
        assertEquals(RiskLevel.SUSPICIOUS, item.getRiskLevel());
        assertEquals("UNENCRYPTED_DATA_LEAK", item.getThreatCategory());
    }

    @Test
    public void testSuspiciousTldScoresHighThreat() {
        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                "com.free.utility",
                "Clean Tool",
                "45.33.32.156",
                "data-sink.xyz",
                443,
                "HTTPS",
                NetworkTrafficItem.Direction.UPLOAD,
                5000,
                5000,
                null
        );

        assertNotNull(item);
        assertTrue("Suspicious TLD must score >= 75", item.getRiskScore() >= 75);
        assertEquals(RiskLevel.SUSPICIOUS, item.getRiskLevel());
        assertEquals("SUSPICIOUS_DESTINATION", item.getThreatCategory());
    }

    @Test
    public void testAdwareTrackerFlagged() {
        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                "com.game.puzzle",
                "Puzzle Game",
                "104.244.42.1",
                "analytics.appsflyer.com",
                443,
                "HTTPS",
                NetworkTrafficItem.Direction.UPLOAD,
                4200,
                1100,
                null
        );

        assertNotNull(item);
        assertEquals("AD_TRACKER", item.getThreatCategory());
        assertTrue(item.getRiskScore() >= 40);
    }

    @Test
    public void testMaliciousC2Keywords() {
        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                "com.stealer.trojan",
                "Trojan Utility",
                "185.220.101.5",
                "c2-node-stealer.org",
                443,
                "HTTPS",
                NetworkTrafficItem.Direction.UPLOAD,
                120000,
                500,
                null
        );

        assertNotNull(item);
        assertTrue("C2 keyword must score >= 90", item.getRiskScore() >= 90);
        assertEquals(RiskLevel.HIGH_RISK, item.getRiskLevel());
        assertEquals("MALICIOUS_C2_SERVER", item.getThreatCategory());
    }

    @Test
    public void testOfflineUtilityAbnormalUpload() {
        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                "com.tool.brightlight",
                "Bright Flashlight",
                "198.51.100.22",
                "gateway.toolserver.net",
                443,
                "HTTPS",
                NetworkTrafficItem.Direction.UPLOAD,
                250 * 1024, // 250 KB
                1000,
                null
        );

        assertNotNull(item);
        assertTrue("Abnormal utility upload must score >= 80", item.getRiskScore() >= 80);
        assertEquals("ANOMALOUS_EXFILTRATION", item.getThreatCategory());
    }

    @Test
    public void testVerifiedGoogleAndWhatsAppSafe() {
        NetworkTrafficItem googleDrive = NetworkThreatEvaluator.evaluateTraffic(
                "com.google.android.apps.docs",
                "Google Drive",
                "142.250.190.46",
                "drive.googleapis.com",
                443,
                "HTTPS (TLS)",
                NetworkTrafficItem.Direction.BIDIRECTIONAL,
                12000,
                48000,
                null
        );

        assertNotNull(googleDrive);
        assertEquals(0, googleDrive.getRiskScore());
        assertEquals(RiskLevel.LOW_RISK, googleDrive.getRiskLevel());
        assertEquals("CLEAN_TRAFFIC", googleDrive.getThreatCategory());
    }
}
