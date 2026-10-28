package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.domain.scanner.DeviceScanSummary;
import com.scamshield.app.domain.scanner.ScannedThreatItem;
import com.scamshield.app.domain.scanner.ThreatSeverity;
import com.scamshield.app.domain.scanner.ThreatType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests verifying device malware scanning models and threat score deductions.
 */
public class DeviceSecurityScannerTest {

    @Test
    public void testCleanDeviceScanGivesScore100() {
        List<ScannedThreatItem> cleanItems = new ArrayList<>();
        cleanItems.add(new ScannedThreatItem(
                "com.clean.app", "Clean App", "com.clean.app",
                ThreatType.APP, ThreatSeverity.SAFE, 0,
                "Audited Safe App", "No dangerous permission combinations",
                Collections.emptyList(), "com.clean.app", null, null, false
        ));

        DeviceScanSummary summary = new DeviceScanSummary(1, 0, 1, Collections.emptyList(), cleanItems, 250);

        assertNotNull(summary);
        assertEquals(100, summary.getDeviceSecurityScore());
        assertEquals(0, summary.getThreatCount());
        assertTrue(summary.isDeviceSafe());
    }

    @Test
    public void testCriticalThreatDeductsScore() {
        List<ScannedThreatItem> threats = new ArrayList<>();
        threats.add(new ScannedThreatItem(
                "com.trojan.overlay", "Malicious Overlay Banker", "com.trojan.overlay",
                ThreatType.APP, ThreatSeverity.CRITICAL, 95,
                "Banking Trojan Combo", "Requests overlay and accessibility simultaneously",
                Arrays.asList("SYSTEM_ALERT_WINDOW", "BIND_ACCESSIBILITY_SERVICE"),
                "com.trojan.overlay", null, null, false
        ));

        DeviceScanSummary summary = new DeviceScanSummary(10, 2, 1, threats, Collections.emptyList(), 300);

        assertNotNull(summary);
        assertEquals(70, summary.getDeviceSecurityScore());
        assertEquals(1, summary.getThreatCount());
        assertFalse(summary.isDeviceSafe());
    }

    @Test
    public void testMultipleThreatDeductionAndClamping() {
        List<ScannedThreatItem> threats = new ArrayList<>();
        // 3 Criticals (3 * 30 = 90 deductions) + 2 Highs (2 * 18 = 36 deductions) = 126 deductions -> score clamped to 0
        for (int i = 0; i < 3; i++) {
            threats.add(new ScannedThreatItem(
                    "crit_" + i, "Critical Threat " + i, "path",
                    ThreatType.FILE, ThreatSeverity.CRITICAL, 90,
                    "Malicious File", "Double extension disguise",
                    Collections.emptyList(), null, "/sdcard/Downloads/trojan.pdf.apk", "1.2 MB", false
            ));
        }
        for (int i = 0; i < 2; i++) {
            threats.add(new ScannedThreatItem(
                    "high_" + i, "High Threat " + i, "pkg",
                    ThreatType.APP, ThreatSeverity.HIGH, 75,
                    "SMS Stealer", "Can read and send background SMS",
                    Collections.emptyList(), "pkg_" + i, null, null, false
            ));
        }

        DeviceScanSummary summary = new DeviceScanSummary(5, 5, 1, threats, Collections.emptyList(), 500);

        assertNotNull(summary);
        assertEquals(0, summary.getDeviceSecurityScore()); // clamped at 0
        assertEquals(5, summary.getThreatCount());
        assertFalse(summary.isDeviceSafe());
    }

    @Test
    public void testThreatResolvedState() {
        ScannedThreatItem item = new ScannedThreatItem(
                "com.adware.app", "Adware App", "com.adware.app",
                ThreatType.APP, ThreatSeverity.MEDIUM, 45,
                "Adware Dropper", "Sideloaded from browser with background install permissions",
                Collections.emptyList(), "com.adware.app", null, null, false
        );

        assertTrue(item.isDangerous());
        assertFalse(item.isResolved());

        item.setResolved(true);
        assertTrue(item.isResolved());
    }
}
