package com.scamshield.app;

import static org.junit.Assert.assertEquals;

import com.scamshield.app.data.model.RiskLevel;

import org.junit.Test;

public class RiskScoringCalibrationTest {

    @Test
    public void testScoreCalibrationBoundaries() {
        // Low risk: 0 - 20
        assertEquals(RiskLevel.LOW_RISK, RiskLevel.fromScore(0));
        assertEquals(RiskLevel.LOW_RISK, RiskLevel.fromScore(10));
        assertEquals(RiskLevel.LOW_RISK, RiskLevel.fromScore(20));

        // Needs attention: 21 - 50
        assertEquals(RiskLevel.NEEDS_ATTENTION, RiskLevel.fromScore(21));
        assertEquals(RiskLevel.NEEDS_ATTENTION, RiskLevel.fromScore(35));
        assertEquals(RiskLevel.NEEDS_ATTENTION, RiskLevel.fromScore(50));

        // Suspicious: 51 - 75
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(51));
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(65));
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(75));

        // High risk: 76 - 100
        assertEquals(RiskLevel.HIGH_RISK, RiskLevel.fromScore(76));
        assertEquals(RiskLevel.HIGH_RISK, RiskLevel.fromScore(90));
        assertEquals(RiskLevel.HIGH_RISK, RiskLevel.fromScore(100));
    }
}
