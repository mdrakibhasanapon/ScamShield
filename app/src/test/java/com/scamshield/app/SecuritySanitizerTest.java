package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.util.SecuritySanitizer;

import org.junit.Test;

public class SecuritySanitizerTest {

    @Test
    public void testEmptyAndShortContent() {
        assertFalse(SecuritySanitizer.isValidContent(null));
        assertFalse(SecuritySanitizer.isValidContent(""));
        assertFalse(SecuritySanitizer.isValidContent("  "));
        assertFalse(SecuritySanitizer.isValidContent("ab"));
        assertTrue(SecuritySanitizer.isValidContent("abc"));
    }

    @Test
    public void testNullByteStripping() {
        String tainted = "Hello\u0000World!\u0007";
        String clean = SecuritySanitizer.sanitizeInput(tainted);
        assertEquals("HelloWorld!", clean);
    }

    @Test
    public void testHugePayloadTruncation() {
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 150_000; i++) {
            large.append("A");
        }
        String sanitized = SecuritySanitizer.sanitizeInput(large.toString());
        assertEquals(SecuritySanitizer.MAX_INPUT_LENGTH, sanitized.length());
    }
}
