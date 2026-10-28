package com.scamshield.app.domain.scanner;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * On-Device Full Phone Security, Malware & Vulnerability Scanner.
 * Audits all installed applications, loose APK files, downloaded scripts,
 * and system security integrity 100% locally.
 */
public class DeviceSecurityScanner {

    private static final String TAG = "DeviceSecurityScanner";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ScanCallback {
        void onScanProgress(String currentPhase, int progressPercent, String currentItemName);
        void onScanComplete(DeviceScanSummary summary);
        void onScanError(String errorMessage);
    }

    // Common trusted installer package signatures
    private static final Set<String> TRUSTED_INSTALLERS = new HashSet<>(Arrays.asList(
            "com.android.vending", // Google Play Store
            "com.google.android.feedback",
            "com.sec.android.app.samsungapps", // Samsung Galaxy Store
            "com.huawei.appmarket", // Huawei AppGallery
            "com.xiaomi.mipicks", // Xiaomi GetApps
            "com.oppo.market", // Oppo App Market
            "com.vivo.appstore", // Vivo V-Appstore
            "org.fdroid.fdroid", // F-Droid
            "com.amazon.venezia" // Amazon Appstore
    ));

    // Common web browsers (used to detect sideloaded APKs from the web)
    private static final Set<String> BROWSER_INSTALLERS = new HashSet<>(Arrays.asList(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.UCMobile.intl"
    ));

    public void scanDeviceAsync(Context context, ScanCallback callback) {
        if (context == null || callback == null) return;

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            List<ScannedThreatItem> threatItems = new ArrayList<>();
            List<ScannedThreatItem> cleanItems = new ArrayList<>();

            try {
                // ==========================================
                // PHASE 1: System Security & Integrity Audit
                // ==========================================
                postProgress(callback, "Auditing System Security Integrity…", 5, "Root & Configuration Check");
                int settingsAudited = auditSystemIntegrity(context, threatItems, cleanItems);

                // ==========================================
                // PHASE 2: Installed Applications Malware Scan
                // ==========================================
                postProgress(callback, "Scanning Installed Applications…", 20, "Auditing Application Permissions");
                int appsScanned = auditInstalledApplications(context, threatItems, cleanItems, callback);

                // ==========================================
                // PHASE 3: Storage & Downloaded Files Scan
                // ==========================================
                postProgress(callback, "Scanning Downloaded Files & APKs…", 80, "Checking Downloads Folder");
                int filesScanned = auditDownloadedFiles(context, threatItems, cleanItems, callback);

                postProgress(callback, "Compiling Security Assessment…", 100, "Done");

                long duration = System.currentTimeMillis() - startTime;
                DeviceScanSummary summary = new DeviceScanSummary(
                        appsScanned,
                        filesScanned,
                        settingsAudited,
                        threatItems,
                        cleanItems,
                        duration
                );

                mainHandler.post(() -> callback.onScanComplete(summary));
            } catch (Exception e) {
                Log.e(TAG, "Device scan failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onScanError("Scan encountered an error: " + e.getMessage()));
            }
        });
    }

    private int auditSystemIntegrity(Context context, List<ScannedThreatItem> threats, List<ScannedThreatItem> clean) {
        int count = 0;

        // 1. Root Check
        count++;
        boolean isRooted = checkDeviceRoot();
        if (isRooted) {
            threats.add(new ScannedThreatItem(
                    "sys_root",
                    "Root Access / Superuser Detected",
                    "System Partition Compromised",
                    ThreatType.SYSTEM_SETTING,
                    ThreatSeverity.CRITICAL,
                    95,
                    "Rooted Operating System",
                    "Superuser binaries or test-keys detected. Rooting strips Android security boundaries, allowing malicious apps to access banking data, passwords, and private messages without restriction.",
                    Arrays.asList("su binary found in system paths", "Android sandbox protections disabled"),
                    null, null, null, true
            ));
        } else {
            clean.add(new ScannedThreatItem(
                    "sys_root_clean",
                    "System Integrity: Secure",
                    "No unauthorized superuser binaries detected",
                    ThreatType.SYSTEM_SETTING,
                    ThreatSeverity.SAFE,
                    0,
                    "Verified System Partition",
                    "Official OS build integrity intact.",
                    Collections.emptyList(),
                    null, null, null, true
            ));
        }

        // 2. USB Debugging Check
        count++;
        try {
            boolean isAdbEnabled = Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.ADB_ENABLED, 0) == 1;

            if (isAdbEnabled) {
                threats.add(new ScannedThreatItem(
                        "sys_adb",
                        "USB Debugging is Enabled",
                        "Developer Mode Active",
                        ThreatType.SYSTEM_SETTING,
                        ThreatSeverity.MEDIUM,
                        55,
                        "Open Debugging Interface",
                        "USB Debugging allows connected computers to run terminal commands, extract app data, or install software without on-screen prompts. Disable when not developing apps.",
                        Collections.singletonList("Settings.Global.ADB_ENABLED = 1"),
                        null, null, null, true
                ));
            }
        } catch (Exception ignored) {}

        // 3. Screen Lock Check
        count++;
        try {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && !km.isDeviceSecure()) {
                threats.add(new ScannedThreatItem(
                        "sys_lock",
                        "No Device Screen Lock Configured",
                        "Device Unsecured",
                        ThreatType.SYSTEM_SETTING,
                        ThreatSeverity.LOW,
                        40,
                        "Physical Security Risk",
                        "Your device does not have a PIN, password, pattern, or biometric lock enabled. Anyone with physical access can view your messages, emails, and accounts.",
                        Collections.singletonList("KeyguardManager.isDeviceSecure() == false"),
                        null, null, null, true
                ));
            }
        } catch (Exception ignored) {}

        return count;
    }

    private int auditInstalledApplications(Context context, List<ScannedThreatItem> threats,
                                           List<ScannedThreatItem> clean, ScanCallback callback) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        int total = packages.size();
        int scanned = 0;

        String ourPackage = context.getPackageName();

        for (int i = 0; i < total; i++) {
            PackageInfo pkg = packages.get(i);
            if (pkg == null || pkg.packageName == null) continue;
            if (pkg.packageName.equals(ourPackage)) continue;

            scanned++;
            String appName = pkg.applicationInfo != null
                    ? pm.getApplicationLabel(pkg.applicationInfo).toString()
                    : pkg.packageName;

            if (i % 5 == 0) {
                int progress = 20 + (int) ((i / (float) total) * 55);
                postProgress(callback, "Scanning Applications (" + i + "/" + total + ")", progress, appName);
            }

            boolean isSystem = (pkg.applicationInfo != null &&
                    (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (pkg.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0);

            // Skip trusted base system apps unless debuggable
            if (isSystem && (pkg.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                continue;
            }

            // Analyze permissions
            Set<String> perms = new HashSet<>();
            if (pkg.requestedPermissions != null) {
                perms.addAll(Arrays.asList(pkg.requestedPermissions));
            }

            List<String> riskReasons = new ArrayList<>();
            int riskScore = 0;
            ThreatSeverity severity = ThreatSeverity.SAFE;
            String threatTitle = "App Audited";

            // Heuristic 1: Banking Trojan Combo (Overlay + Accessibility)
            boolean hasOverlay = perms.contains("android.permission.SYSTEM_ALERT_WINDOW");
            boolean hasAccessibility = perms.contains("android.permission.BIND_ACCESSIBILITY_SERVICE");
            if (hasOverlay && hasAccessibility) {
                riskScore += 90;
                severity = ThreatSeverity.CRITICAL;
                threatTitle = "Banking Trojan / Keylogger Combo";
                riskReasons.add("Requests Overlay Drawing (SYSTEM_ALERT_WINDOW)");
                riskReasons.add("Requests Accessibility Service (BIND_ACCESSIBILITY_SERVICE)");
            }

            // Heuristic 2: SMS Stealer / Toll Fraud Combo
            boolean hasSmsRead = perms.contains("android.permission.READ_SMS") || perms.contains("android.permission.RECEIVE_SMS");
            boolean hasSmsSend = perms.contains("android.permission.SEND_SMS");
            boolean hasNet = perms.contains("android.permission.INTERNET");
            if (hasSmsRead && hasSmsSend && hasNet && !isKnownSmsApp(pkg.packageName)) {
                riskScore += 75;
                if (severity == ThreatSeverity.SAFE) severity = ThreatSeverity.HIGH;
                threatTitle = "SMS Stealer & Toll Fraud Risk";
                riskReasons.add("Can read incoming OTP and verification SMS");
                riskReasons.add("Can send unauthorized background SMS");
            }

            // Heuristic 3: Spyware Combo (Audio + Camera + Location + Auto-start)
            boolean hasMic = perms.contains("android.permission.RECORD_AUDIO");
            boolean hasCamera = perms.contains("android.permission.CAMERA");
            boolean hasLocation = perms.contains("android.permission.ACCESS_FINE_LOCATION");
            boolean hasBoot = perms.contains("android.permission.RECEIVE_BOOT_COMPLETED");
            if (hasMic && hasCamera && hasLocation && hasBoot && !isKnownSocialApp(pkg.packageName)) {
                riskScore += 65;
                if (severity == ThreatSeverity.SAFE) severity = ThreatSeverity.HIGH;
                threatTitle = "Background Spyware Indicators";
                riskReasons.add("Silent background microphone & camera access");
                riskReasons.add("Persistent auto-start on device reboot");
            }

            // Heuristic 4: Sideloading Dropper
            boolean hasInstall = perms.contains("android.permission.REQUEST_INSTALL_PACKAGES") ||
                                 perms.contains("android.permission.INSTALL_PACKAGES");
            if (hasInstall && !TRUSTED_INSTALLERS.contains(pkg.packageName)) {
                riskScore += 45;
                if (severity == ThreatSeverity.SAFE) severity = ThreatSeverity.MEDIUM;
                if (!threatTitle.contains("Trojan")) threatTitle = "App Dropper / Sideloading Capability";
                riskReasons.add("Can silently trigger background APK installations");
            }

            // Heuristic 5: Debuggable / Tampered Release APK
            if (pkg.applicationInfo != null && (pkg.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                riskScore += 60;
                if (severity == ThreatSeverity.SAFE) severity = ThreatSeverity.HIGH;
                threatTitle = "Debuggable / Tampered Build";
                riskReasons.add("Application left in debug mode (vulnerable to memory injection)");
            }

            // Heuristic 6: Sideloaded from Web Browser with sensitive permissions
            String installer = getInstallerPackage(context, pkg.packageName);
            if (installer != null && BROWSER_INSTALLERS.contains(installer) && riskScore > 0) {
                riskScore += 20;
                riskReasons.add("Downloaded directly from web browser (" + installer + ")");
            }

            // Heuristic 7: Outdated Target SDK Evader
            if (pkg.applicationInfo != null && pkg.applicationInfo.targetSdkVersion <= 22) {
                riskScore += 35;
                if (severity == ThreatSeverity.SAFE) severity = ThreatSeverity.MEDIUM;
                riskReasons.add("Uses legacy target SDK (" + pkg.applicationInfo.targetSdkVersion + ") to evade modern Android runtime permissions");
            }

            if (severity != ThreatSeverity.SAFE) {
                threats.add(new ScannedThreatItem(
                        pkg.packageName,
                        appName,
                        pkg.packageName,
                        ThreatType.APP,
                        severity,
                        Math.min(100, riskScore),
                        threatTitle,
                        "This app possesses high-risk capabilities frequently observed in malware, spyware, or adware. Review permissions or uninstall if untrusted.",
                        riskReasons,
                        pkg.packageName,
                        null,
                        null,
                        isSystem
                ));
            } else {
                clean.add(new ScannedThreatItem(
                        pkg.packageName,
                        appName,
                        pkg.packageName,
                        ThreatType.APP,
                        ThreatSeverity.SAFE,
                        0,
                        "Verified Normal App",
                        "Standard permissions without high-risk anomalies.",
                        Collections.emptyList(),
                        pkg.packageName,
                        null,
                        null,
                        isSystem
                ));
            }
        }

        return scanned;
    }

    private int auditDownloadedFiles(Context context, List<ScannedThreatItem> threats,
                                     List<ScannedThreatItem> clean, ScanCallback callback) {
        int scanned = 0;
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null || !downloadsDir.exists() || !downloadsDir.isDirectory()) {
            return scanned;
        }

        File[] files = downloadsDir.listFiles();
        if (files == null) return scanned;

        PackageManager pm = context.getPackageManager();

        for (File file : files) {
            if (file == null || file.isDirectory()) continue;
            scanned++;
            String fileName = file.getName().toLowerCase(Locale.US);

            postProgress(callback, "Scanning Storage & Files…", 85, file.getName());

            // 1. Disguised Double Extension Malware (e.g. statement.pdf.apk, photo.jpg.apk)
            if (fileName.endsWith(".pdf.apk") || fileName.endsWith(".jpg.apk") ||
                fileName.endsWith(".doc.apk") || fileName.endsWith(".docx.apk") ||
                fileName.endsWith(".zip.apk") || fileName.endsWith(".mp4.apk")) {
                threats.add(new ScannedThreatItem(
                        file.getAbsolutePath(),
                        file.getName(),
                        file.getAbsolutePath(),
                        ThreatType.FILE,
                        ThreatSeverity.CRITICAL,
                        95,
                        "Disguised Malicious APK",
                        "This file disguises an executable Android package as a document or photo. Attackers use this tactic to trick users into running trojans.",
                        Arrays.asList("Double file extension deception", "File format: .apk"),
                        null,
                        file.getAbsolutePath(),
                        formatFileSize(file.length()),
                        false
                ));
                continue;
            }

            // 2. Loose Uninstalled APK Files in Downloads
            if (fileName.endsWith(".apk")) {
                String pkgName = "Unknown Package";
                try {
                    PackageInfo pi = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    if (pi != null) pkgName = pi.packageName;
                } catch (Exception ignored) {}

                threats.add(new ScannedThreatItem(
                        file.getAbsolutePath(),
                        file.getName(),
                        file.getAbsolutePath(),
                        ThreatType.FILE,
                        ThreatSeverity.HIGH,
                        70,
                        "Unverified Loose APK Installer",
                        "Standalone APK file sitting in Downloads folder. Unverified APK files downloaded from internet sites frequently bundle adware, spyware, or trojans.",
                        Arrays.asList("Package ID: " + pkgName, "Size: " + formatFileSize(file.length())),
                        null,
                        file.getAbsolutePath(),
                        formatFileSize(file.length()),
                        false
                ));
                continue;
            }

            // 3. Script Executables in public storage (.sh, .dex, .bat, .jar, .so)
            if (fileName.endsWith(".sh") || fileName.endsWith(".dex") ||
                fileName.endsWith(".bat") || fileName.endsWith(".jar") || fileName.endsWith(".vbs")) {
                threats.add(new ScannedThreatItem(
                        file.getAbsolutePath(),
                        file.getName(),
                        file.getAbsolutePath(),
                        ThreatType.FILE,
                        ThreatSeverity.HIGH,
                        65,
                        "Suspicious Script / Binary File",
                        "Executable script file detected in user storage. Scripts can be executed by sideloaded apps to bypass Android application isolation.",
                        Collections.singletonList("Executable extension detected"),
                        null,
                        file.getAbsolutePath(),
                        formatFileSize(file.length()),
                        false
                ));
            }
        }

        return scanned;
    }

    private boolean checkDeviceRoot() {
        String[] paths = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/data/local/su",
                "/system/app/Superuser.apk"
        };
        for (String path : paths) {
            try {
                if (new File(path).exists()) return true;
            } catch (Exception ignored) {}
        }

        // Test-keys build check
        return Build.TAGS != null && Build.TAGS.contains("test-keys");
    }

    private String getInstallerPackage(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.content.pm.InstallSourceInfo info = pm.getInstallSourceInfo(packageName);
                return info.getInstallingPackageName();
            } else {
                return pm.getInstallerPackageName(packageName);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isKnownSmsApp(String pkg) {
        return pkg.equals("com.google.android.apps.messaging") ||
               pkg.equals("com.samsung.android.messaging") ||
               pkg.equals("com.android.mms");
    }

    private boolean isKnownSocialApp(String pkg) {
        return pkg.startsWith("com.whatsapp") ||
               pkg.startsWith("org.telegram") ||
               pkg.startsWith("com.facebook") ||
               pkg.startsWith("com.instagram") ||
               pkg.startsWith("com.google.android");
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void postProgress(ScanCallback callback, String phase, int progress, String item) {
        mainHandler.post(() -> callback.onScanProgress(phase, progress, item));
    }

    /**
     * Launch standard Android package uninstaller dialog for an identified app.
     */
    public static void requestAppUninstall(Context context, String packageName) {
        if (context == null || packageName == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package uninstaller: " + e.getMessage());
        }
    }

    /**
     * Delete an identified malicious or unwanted file from disk.
     */
    public static boolean deleteMaliciousFile(String filePath) {
        if (filePath == null) return false;
        try {
            File f = new File(filePath);
            return f.exists() && f.delete();
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete file: " + e.getMessage());
            return false;
        }
    }
}
