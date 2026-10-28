package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.domain.scanner.DownloadThreatEvaluator;

import org.junit.Test;

/**
 * Unit tests verifying real-time download threat evaluation.
 */
public class DownloadThreatEvaluatorTest {

    @Test
    public void testDisguisedPdfApkTriggersCriticalRisk() {
        ScamAnalysisResult result = DownloadThreatEvaluator.evaluateDownloadedFile(
                null,
                "invoice_receipt_october.pdf.apk",
                null,
                "http://phishing-invoicing.icu/dl/invoice_receipt_october.pdf.apk"
        );

        assertNotNull(result);
        assertEquals(ScanType.FILE_DOWNLOAD, result.getScanType());
        assertTrue("Disguised APK must score critical threat >= 90", result.getRiskScore() >= 90);
        assertEquals(RiskLevel.HIGH_RISK, result.getRiskLevel());
        assertFalse(result.getIndicators().isEmpty());
    }

    @Test
    public void testLooseApkFileTriggersWarning() {
        ScamAnalysisResult result = DownloadThreatEvaluator.evaluateDownloadedFile(
                null,
                "cracked_utility.apk",
                null,
                "https://trusted-mirror.com/cracked_utility.apk"
        );

        assertNotNull(result);
        assertTrue("Loose APK should score threat level >= 70", result.getRiskScore() >= 70);
        assertTrue(result.getRiskLevel() == RiskLevel.HIGH_RISK || result.getRiskLevel() == RiskLevel.SUSPICIOUS);
    }

    @Test
    public void testExecutableScriptTriggersHighRisk() {
        ScamAnalysisResult result = DownloadThreatEvaluator.evaluateDownloadedFile(
                null,
                "installer_script.sh",
                null,
                null
        );

        assertNotNull(result);
        assertTrue("Shell script must score threat level >= 75", result.getRiskScore() >= 75);
    }

    @Test
    public void testCleanPdfDocumentIsSafe() {
        ScamAnalysisResult result = DownloadThreatEvaluator.evaluateDownloadedFile(
                null,
                "quarterly_financials.pdf",
                null,
                "https://company.org/reports/quarterly_financials.pdf"
        );

        assertNotNull(result);
        assertEquals(0, result.getRiskScore());
        assertEquals(RiskLevel.LOW_RISK, result.getRiskLevel());
        assertTrue(result.getIndicators().isEmpty());
    }

    @Test
    public void testSuspiciousOriginUrlIncreasesScore() {
        ScamAnalysisResult result = DownloadThreatEvaluator.evaluateDownloadedFile(
                null,
                "update_notes.txt",
                null,
                "http://bank-verification.icu/update_notes.txt"
        );

        assertNotNull(result);
        assertTrue("Suspicious domain must trigger elevated score", result.getRiskScore() > 0);
        assertFalse("Should contain URL anomaly indicators", result.getIndicators().isEmpty());
    }
}
