package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.TrustedContact;

import org.junit.Test;

public class TrustedContactSyncTest {

    @Test
    public void testPhoneNormalization() {
        assertEquals("+15550199432", TrustedContact.normalizePhone("+1 (555) 019-9432"));
        assertEquals("01712345678", TrustedContact.normalizePhone("0171-234-5678"));
        assertEquals("+8801712345678", TrustedContact.normalizePhone("+880 171 234 5678"));
    }

    @Test
    public void testInitialsExtraction() {
        TrustedContact c1 = new TrustedContact("1", "Sarah Jenkins", "+15550199432", "+15550199432", false, null, 0);
        assertEquals("SJ", c1.getInitials());

        TrustedContact c2 = new TrustedContact("2", "Mom", "+15550199432", "+15550199432", false, null, 0);
        assertEquals("MO", c2.getInitials());

        TrustedContact c3 = new TrustedContact("3", "David Michael Miller", "+15550199432", "+15550199432", false, null, 0);
        assertEquals("DM", c3.getInitials());
    }

    @Test
    public void testCleanContactCreation() {
        TrustedContact contact = new TrustedContact(
                "c_101",
                "Alice Cooper",
                "+1 (555) 014-9922",
                null,
                false,
                null,
                1700000000L
        );

        assertNotNull(contact);
        assertEquals("Alice Cooper", contact.getName());
        assertEquals("+15550149922", contact.getNormalizedNumber());
        assertFalse(contact.isSuspicious());
        assertEquals("AC", contact.getInitials());
    }

    @Test
    public void testSuspiciousWangiriTollContact() {
        TrustedContact tollContact = new TrustedContact(
                "c_toll",
                "Unknown Foreign Caller",
                "+232 99 123456",
                "+23299123456",
                true,
                "⚠️ High-Rate International Toll Destination (+232)",
                1700000000L
        );

        assertTrue(tollContact.isSuspicious());
        assertTrue(tollContact.getSuspiciousReason().contains("+232"));
    }
}
