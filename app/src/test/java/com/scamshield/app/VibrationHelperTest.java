package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.util.NotificationHelper;
import com.scamshield.app.util.VibrationHelper;

import org.junit.Test;

public class VibrationHelperTest {

    @Test
    public void testThreatPatternHasValidTimingsAndAmplitudes() {
        assertNotNull(VibrationHelper.PATTERN_THREAT);
        assertNotNull(VibrationHelper.AMPLITUDE_THREAT);
        assertEquals(VibrationHelper.PATTERN_THREAT.length, VibrationHelper.AMPLITUDE_THREAT.length);
        assertTrue("Threat vibration pattern must have at least 3 pulses", VibrationHelper.PATTERN_THREAT.length >= 6);
    }

    @Test
    public void testWarningPatternHasValidTimingsAndAmplitudes() {
        assertNotNull(VibrationHelper.PATTERN_WARNING);
        assertNotNull(VibrationHelper.AMPLITUDE_WARNING);
        assertEquals(VibrationHelper.PATTERN_WARNING.length, VibrationHelper.AMPLITUDE_WARNING.length);
        assertTrue("Warning pattern must have at least 2 pulses", VibrationHelper.PATTERN_WARNING.length >= 4);
    }

    @Test
    public void testCleanPatternHasValidTimingsAndAmplitudes() {
        assertNotNull(VibrationHelper.PATTERN_CLEAN);
        assertNotNull(VibrationHelper.AMPLITUDE_CLEAN);
        assertEquals(VibrationHelper.PATTERN_CLEAN.length, VibrationHelper.AMPLITUDE_CLEAN.length);
        assertTrue("Clean pattern must have at least 1 pulse", VibrationHelper.PATTERN_CLEAN.length >= 2);
    }

    @Test
    public void testNotificationHelperChannelsExposeVibrationPatterns() {
        assertNotNull(NotificationHelper.VIBRATION_PATTERN_THREAT);
        assertEquals(VibrationHelper.PATTERN_THREAT.length, NotificationHelper.VIBRATION_PATTERN_THREAT.length);
    }

    @Test
    public void testTriggerNullContextSafelyDoesNotThrow() {
        // Must handle null context gracefully without throwing NPE
        VibrationHelper.triggerNotificationVibration(null, RiskLevel.HIGH_RISK);
        VibrationHelper.triggerThreatVibration(null, true);
        VibrationHelper.triggerTestVibration(null);
    }
}
