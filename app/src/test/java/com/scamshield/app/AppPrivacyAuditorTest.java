package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.AppPrivacyProfile;
import com.scamshield.app.data.model.AppPrivacySummary;
import com.scamshield.app.domain.scanner.ThreatSeverity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppPrivacyAuditorTest {

    @Test
    public void testCleanAppProfile() {
        AppPrivacyProfile profile = new AppPrivacyProfile(
                "com.example.calculator",
                "Calculator",
                false,
                false,
                "com.android.vending",
                false, // mic
                false, // location
                false, // contacts
                false, // camera
                false, // audio
                false, // internet
                false, // boot
                0,
                ThreatSeverity.SAFE,
                false,
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertNotNull(profile);
        assertFalse(profile.hasAnySensitivePermission());
        assertFalse(profile.isPotentialSpyware());
        assertEquals(ThreatSeverity.SAFE, profile.getRiskSeverity());
    }

    @Test
    public void testSpywareComboProfile() {
        List<String> reasons = new ArrayList<>();
        reasons.add("Critical Spyware Vector: Combines silent Microphone listening, GPS Location tracking, and Contacts exfiltration.");

        AppPrivacyProfile profile = new AppPrivacyProfile(
                "com.stealth.tracker",
                "Free Super Wallpaper",
                false,
                true, // sideloaded
                null,
                true, // mic
                true, // location
                true, // contacts
                true, // camera
                true, // audio
                true, // internet
                true, // boot
                95,
                ThreatSeverity.CRITICAL,
                true, // potential spyware
                reasons,
                Arrays.asList("Microphone", "Location", "Contacts")
        );

        assertTrue(profile.hasAnySensitivePermission());
        assertTrue(profile.hasMicrophone());
        assertTrue(profile.hasLocation());
        assertTrue(profile.hasContacts());
        assertTrue(profile.isPotentialSpyware());
        assertEquals(ThreatSeverity.CRITICAL, profile.getRiskSeverity());
        assertTrue("Risk score must be >= 90 for spyware combo", profile.getRiskScore() >= 90);
    }

    @Test
    public void testAppPrivacySummaryAggregation() {
        List<AppPrivacyProfile> profiles = new ArrayList<>();

        profiles.add(new AppPrivacyProfile(
                "com.app.one", "App One", false, false, "com.android.vending",
                true, false, false, false, false, true, false, 25, ThreatSeverity.LOW, false, null, null
        ));
        profiles.add(new AppPrivacyProfile(
                "com.app.two", "App Two", false, false, "com.android.vending",
                false, true, true, false, false, true, false, 45, ThreatSeverity.MEDIUM, false, null, null
        ));
        profiles.add(new AppPrivacyProfile(
                "com.app.spy", "Spy App", false, true, null,
                true, true, true, true, true, true, true, 95, ThreatSeverity.CRITICAL, true, null, null
        ));

        AppPrivacySummary summary = new AppPrivacySummary(
                50, // total audited
                profiles.size(),
                2, // mic count (app one + spy)
                2, // location count (app two + spy)
                2, // contacts count (app two + spy)
                1, // audio count (spy)
                1, // camera count (spy)
                1, // spyware count (spy)
                profiles
        );

        assertEquals(50, summary.getTotalAppsAudited());
        assertEquals(3, summary.getSensitiveAppsCount());
        assertEquals(2, summary.getMicCount());
        assertEquals(2, summary.getLocationCount());
        assertEquals(2, summary.getContactsCount());
        assertEquals(1, summary.getSpywareThreatCount());
        assertEquals(3, summary.getProfiles().size());
    }
}
