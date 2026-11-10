package com.scamshield.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppPrivacySummary implements Serializable {

    private final int totalAppsAudited;
    private final int sensitiveAppsCount;
    private final int micCount;
    private final int locationCount;
    private final int contactsCount;
    private final int audioPhoneCount;
    private final int cameraCount;
    private final int spywareThreatCount;
    private final List<AppPrivacyProfile> profiles;

    public AppPrivacySummary(
            int totalAppsAudited,
            int sensitiveAppsCount,
            int micCount,
            int locationCount,
            int contactsCount,
            int audioPhoneCount,
            int cameraCount,
            int spywareThreatCount,
            List<AppPrivacyProfile> profiles) {
        this.totalAppsAudited = totalAppsAudited;
        this.sensitiveAppsCount = sensitiveAppsCount;
        this.micCount = micCount;
        this.locationCount = locationCount;
        this.contactsCount = contactsCount;
        this.audioPhoneCount = audioPhoneCount;
        this.cameraCount = cameraCount;
        this.spywareThreatCount = spywareThreatCount;
        this.profiles = profiles != null ? profiles : new ArrayList<>();
    }

    public int getTotalAppsAudited() {
        return totalAppsAudited;
    }

    public int getSensitiveAppsCount() {
        return sensitiveAppsCount;
    }

    public int getMicCount() {
        return micCount;
    }

    public int getLocationCount() {
        return locationCount;
    }

    public int getContactsCount() {
        return contactsCount;
    }

    public int getAudioPhoneCount() {
        return audioPhoneCount;
    }

    public int getCameraCount() {
        return cameraCount;
    }

    public int getSpywareThreatCount() {
        return spywareThreatCount;
    }

    public List<AppPrivacyProfile> getProfiles() {
        return profiles;
    }
}
