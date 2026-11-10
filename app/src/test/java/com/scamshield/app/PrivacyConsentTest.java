package com.scamshield.app;

import com.scamshield.app.util.PrivacyConsentHelper;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrivacyConsentTest {

    @Test
    public void testFullLegalPolicyContainsEssentialClauses() {
        String policy = PrivacyConsentHelper.FULL_LEGAL_POLICY_TEXT;
        assertNotNull(policy);
        assertFalse(policy.isEmpty());

        // Verify key Google Play required disclosures
        assertTrue(policy.contains("ZERO CLOUD EXFILTRATION PRINCIPLE"));
        assertTrue(policy.contains("ON-DEVICE HEURISTIC EVALUATION"));
        assertTrue(policy.contains("MICROPHONE ACCESS"));
        assertTrue(policy.contains("LOCATION ACCESS"));
        assertTrue(policy.contains("NOTIFICATION ACCESS"));
        assertTrue(policy.contains("CONTACTS & SAFE SENDERS"));
        assertTrue(policy.contains("DATA RETENTION & COMPLETE ERASURE"));
        assertTrue(policy.contains("GOOGLE ADMOB"));
    }
}
