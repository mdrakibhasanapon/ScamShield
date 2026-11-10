package com.scamshield.app.data.model;

import com.scamshield.app.domain.scanner.ThreatSeverity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppPrivacyProfile implements Serializable {

    private final String packageName;
    private final String appName;
    private final boolean isSystemApp;
    private final boolean isSideloaded;
    private final String installerPackage;

    private final boolean hasMicrophone;
    private final boolean hasLocation;
    private final boolean hasContacts;
    private final boolean hasCamera;
    private final boolean hasAudioPhone;
    private final boolean hasInternet;
    private final boolean hasBootCompleted;

    private final int riskScore;
    private final ThreatSeverity riskSeverity;
    private final boolean isPotentialSpyware;
    private final List<String> riskReasons;
    private final List<String> sensitivePermissions;

    public AppPrivacyProfile(
            String packageName,
            String appName,
            boolean isSystemApp,
            boolean isSideloaded,
            String installerPackage,
            boolean hasMicrophone,
            boolean hasLocation,
            boolean hasContacts,
            boolean hasCamera,
            boolean hasAudioPhone,
            boolean hasInternet,
            boolean hasBootCompleted,
            int riskScore,
            ThreatSeverity riskSeverity,
            boolean isPotentialSpyware,
            List<String> riskReasons,
            List<String> sensitivePermissions) {
        this.packageName = packageName != null ? packageName : "";
        this.appName = appName != null ? appName : this.packageName;
        this.isSystemApp = isSystemApp;
        this.isSideloaded = isSideloaded;
        this.installerPackage = installerPackage;
        this.hasMicrophone = hasMicrophone;
        this.hasLocation = hasLocation;
        this.hasContacts = hasContacts;
        this.hasCamera = hasCamera;
        this.hasAudioPhone = hasAudioPhone;
        this.hasInternet = hasInternet;
        this.hasBootCompleted = hasBootCompleted;
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskSeverity = riskSeverity != null ? riskSeverity : ThreatSeverity.SAFE;
        this.isPotentialSpyware = isPotentialSpyware;
        this.riskReasons = riskReasons != null ? riskReasons : new ArrayList<>();
        this.sensitivePermissions = sensitivePermissions != null ? sensitivePermissions : new ArrayList<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public boolean isSideloaded() {
        return isSideloaded;
    }

    public String getInstallerPackage() {
        return installerPackage;
    }

    public boolean hasMicrophone() {
        return hasMicrophone;
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    public boolean hasContacts() {
        return hasContacts;
    }

    public boolean hasCamera() {
        return hasCamera;
    }

    public boolean hasAudioPhone() {
        return hasAudioPhone;
    }

    public boolean hasInternet() {
        return hasInternet;
    }

    public boolean hasBootCompleted() {
        return hasBootCompleted;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public ThreatSeverity getRiskSeverity() {
        return riskSeverity;
    }

    public boolean isPotentialSpyware() {
        return isPotentialSpyware;
    }

    public List<String> getRiskReasons() {
        return riskReasons;
    }

    public List<String> getSensitivePermissions() {
        return sensitivePermissions;
    }

    public boolean hasAnySensitivePermission() {
        return hasMicrophone || hasLocation || hasContacts || hasCamera || hasAudioPhone;
    }
}
