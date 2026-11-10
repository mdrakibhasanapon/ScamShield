package com.scamshield.app.domain.scanner;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.scamshield.app.data.model.AppPrivacyProfile;
import com.scamshield.app.data.model.AppPrivacySummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppPrivacyAuditor {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuditCallback {
        void onProgress(int current, int total, String currentAppName);
        void onComplete(AppPrivacySummary summary);
        void onError(String error);
    }

    // Common legitimate communication and system apps that inherently require multiple sensitive permissions
    private static final Set<String> KNOWN_COMMUNICATION_APPS = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.plus",
            "org.thunderdog.challegram",
            "com.facebook.orca",
            "com.facebook.mlite",
            "org.thoughtcrime.securesms",
            "com.imo.android.imoim",
            "com.imo.android.imoimbeta",
            "com.discord",
            "com.viber.voip",
            "com.skype.raider",
            "com.tencent.mm",
            "jp.naver.line.android",
            "com.snapchat.android",
            "com.instagram.android",
            "com.google.android.apps.messaging",
            "com.google.android.dialer",
            "com.google.android.contacts",
            "com.google.android.apps.maps",
            "com.samsung.android.messaging",
            "com.samsung.android.dialer",
            "com.samsung.android.contacts"
    ));

    // Trusted store installers
    private static final Set<String> TRUSTED_INSTALLERS = new HashSet<>(Arrays.asList(
            "com.android.vending",
            "com.google.android.feedback",
            "com.sec.android.app.samsungapps",
            "com.huawei.appmarket",
            "com.xiaomi.mipicks",
            "com.oppo.market",
            "com.vivo.appstore",
            "org.fdroid.fdroid",
            "com.amazon.venezia"
    ));

    public void auditInstalledAppsAsync(Context context, AuditCallback callback) {
        if (context == null || callback == null) return;

        executor.execute(() -> {
            try {
                AppPrivacySummary summary = auditInstalledApps(context, callback);
                mainHandler.post(() -> callback.onComplete(summary));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public AppPrivacySummary auditInstalledApps(Context context) {
        return auditInstalledApps(context, null);
    }

    public AppPrivacySummary auditInstalledApps(Context context, AuditCallback callback) {
        if (context == null) {
            return new AppPrivacySummary(0, 0, 0, 0, 0, 0, 0, 0, Collections.emptyList());
        }

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        String myPackage = context.getPackageName();

        List<AppPrivacyProfile> profiles = new ArrayList<>();
        int micTotal = 0;
        int locTotal = 0;
        int contactsTotal = 0;
        int audioTotal = 0;
        int cameraTotal = 0;
        int spywareTotal = 0;
        int totalAudited = 0;

        int total = installedPackages.size();

        for (int i = 0; i < total; i++) {
            PackageInfo pkg = installedPackages.get(i);
            if (pkg == null || pkg.packageName == null) continue;
            if (pkg.packageName.equals(myPackage)) continue;

            totalAudited++;
            String appName = pkg.applicationInfo != null
                    ? pm.getApplicationLabel(pkg.applicationInfo).toString()
                    : pkg.packageName;

            if (callback != null && i % 5 == 0) {
                final int current = i;
                final String currentName = appName;
                mainHandler.post(() -> callback.onProgress(current, total, currentName));
            }

            boolean isSystem = (pkg.applicationInfo != null &&
                    (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (pkg.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0);

            Set<String> perms = new HashSet<>();
            if (pkg.requestedPermissions != null) {
                perms.addAll(Arrays.asList(pkg.requestedPermissions));
            }

            boolean hasMic = perms.contains("android.permission.RECORD_AUDIO");
            boolean hasLoc = perms.contains("android.permission.ACCESS_FINE_LOCATION") ||
                    perms.contains("android.permission.ACCESS_COARSE_LOCATION") ||
                    perms.contains("android.permission.ACCESS_BACKGROUND_LOCATION");
            boolean hasContacts = perms.contains("android.permission.READ_CONTACTS") ||
                    perms.contains("android.permission.WRITE_CONTACTS") ||
                    perms.contains("android.permission.GET_ACCOUNTS");
            boolean hasCamera = perms.contains("android.permission.CAMERA");
            boolean hasAudioPhone = perms.contains("android.permission.MODIFY_AUDIO_SETTINGS") ||
                    perms.contains("android.permission.READ_PHONE_STATE") ||
                    perms.contains("android.permission.READ_CALL_LOG") ||
                    perms.contains("android.permission.PROCESS_OUTGOING_CALLS") ||
                    perms.contains("android.permission.CAPTURE_AUDIO_OUTPUT");
            boolean hasInternet = perms.contains("android.permission.INTERNET");
            boolean hasBoot = perms.contains("android.permission.RECEIVE_BOOT_COMPLETED");

            if (!hasMic && !hasLoc && !hasContacts && !hasCamera && !hasAudioPhone) {
                // App does not request sensitive surveillance permissions
                continue;
            }

            if (hasMic) micTotal++;
            if (hasLoc) locTotal++;
            if (hasContacts) contactsTotal++;
            if (hasAudioPhone) audioTotal++;
            if (hasCamera) cameraTotal++;

            String installer = getInstallerPackage(context, pkg.packageName);
            boolean isSideloaded = !isSystem && (installer == null || !TRUSTED_INSTALLERS.contains(installer));

            List<String> riskReasons = new ArrayList<>();
            List<String> sensitivePermsList = new ArrayList<>();

            if (hasMic) sensitivePermsList.add("Microphone (Audio Recording)");
            if (hasLoc) sensitivePermsList.add("GPS Location");
            if (hasContacts) sensitivePermsList.add("Address Book Contacts");
            if (hasCamera) sensitivePermsList.add("Camera");
            if (hasAudioPhone) sensitivePermsList.add("Phone Call / Speaker Audio");

            boolean isKnownCommunication = KNOWN_COMMUNICATION_APPS.contains(pkg.packageName);

            int riskScore = 0;
            boolean isPotentialSpyware = false;
            ThreatSeverity severity = ThreatSeverity.SAFE;

            // Spyware Combo 1: Mic + Location + Contacts + Internet in non-standard app
            if (hasMic && hasLoc && hasContacts && hasInternet && !isKnownCommunication && !isSystem) {
                isPotentialSpyware = true;
                riskScore = 95;
                severity = ThreatSeverity.CRITICAL;
                riskReasons.add("🚨 Critical Spyware Vector: Combines silent Microphone listening, GPS Location tracking, and Address Book exfiltration with Internet upload capability.");
            }
            // Spyware Combo 2: Mic + Location + Boot Completed in non-standard sideloaded app
            else if (hasMic && hasLoc && hasBoot && isSideloaded && !isKnownCommunication) {
                isPotentialSpyware = true;
                riskScore = 85;
                severity = ThreatSeverity.HIGH;
                riskReasons.add("⚠️ Stealth Surveillance: Possesses Microphone and GPS location access with persistent auto-start on device reboot.");
            }
            // Spyware Combo 3: Contacts + Internet + SMS in non-standard app
            else if (hasContacts && hasInternet && (perms.contains("android.permission.READ_SMS") || perms.contains("android.permission.RECEIVE_SMS")) && !isKnownCommunication && !isSystem) {
                isPotentialSpyware = true;
                riskScore = 80;
                severity = ThreatSeverity.HIGH;
                riskReasons.add("⚠️ Contact & SMS Exfiltration: Can read private contacts and SMS verification codes with Internet upload capability.");
            } else {
                // General privacy scoring
                if (hasMic) riskScore += 25;
                if (hasContacts) riskScore += 25;
                if (hasLoc) riskScore += 20;
                if (hasCamera) riskScore += 15;
                if (hasAudioPhone) riskScore += 15;
                if (isSideloaded) riskScore += 15;

                if (isSystem) {
                    riskScore = Math.min(riskScore, 20); // system apps are lower risk
                    severity = ThreatSeverity.SAFE;
                    riskReasons.add("System Application (Verified OS Component).");
                } else if (isKnownCommunication) {
                    riskScore = Math.min(riskScore, 35);
                    severity = ThreatSeverity.LOW;
                    riskReasons.add("Recognized Communication / Messaging App.");
                } else if (riskScore >= 70) {
                    severity = ThreatSeverity.HIGH;
                    riskReasons.add("High Sensitive Privileges: App holds multiple sensitive hardware access permissions.");
                } else if (riskScore >= 40) {
                    severity = ThreatSeverity.MEDIUM;
                    riskReasons.add("Elevated Permissions: Review if this app legitimately requires microphone, location, or contacts.");
                } else {
                    severity = ThreatSeverity.LOW;
                    riskReasons.add("Standard permission usage.");
                }
            }

            if (isPotentialSpyware) {
                spywareTotal++;
            }

            profiles.add(new AppPrivacyProfile(
                    pkg.packageName,
                    appName,
                    isSystem,
                    isSideloaded,
                    installer,
                    hasMic,
                    hasLoc,
                    hasContacts,
                    hasCamera,
                    hasAudioPhone,
                    hasInternet,
                    hasBoot,
                    riskScore,
                    severity,
                    isPotentialSpyware,
                    riskReasons,
                    sensitivePermsList
            ));
        }

        // Sort: Potential spyware first, then by risk score descending, then by app name
        Collections.sort(profiles, (a, b) -> {
            if (a.isPotentialSpyware() != b.isPotentialSpyware()) {
                return a.isPotentialSpyware() ? -1 : 1;
            }
            int scoreCmp = Integer.compare(b.getRiskScore(), a.getRiskScore());
            if (scoreCmp != 0) return scoreCmp;
            return a.getAppName().compareToIgnoreCase(b.getAppName());
        });

        return new AppPrivacySummary(
                totalAudited,
                profiles.size(),
                micTotal,
                locTotal,
                contactsTotal,
                audioTotal,
                cameraTotal,
                spywareTotal,
                profiles
        );
    }

    private String getInstallerPackage(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return pm.getInstallSourceInfo(packageName).getInstallingPackageName();
            } else {
                return pm.getInstallerPackageName(packageName);
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
