package com.scamshield.app.domain.scanner;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.domain.detector.UrlIndicatorDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * On-device evaluator for downloaded files and web downloads.
 * Detects disguised extensions, trojan APKs, executable scripts, and deceptive download links.
 */
public class DownloadThreatEvaluator {

    public static ScamAnalysisResult evaluateDownloadedFile(Context context, String fileName, String filePath, String originUrl) {
        String cleanName = fileName != null ? fileName.trim() : "Unknown Download";
        String lowerName = cleanName.toLowerCase(Locale.US);

        List<DetectedIndicator> indicators = new ArrayList<>();
        List<String> whyFlagged = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        List<String> verificationNotes = new ArrayList<>();

        int riskScore = 0;

        // 1. Check for Disguised Double Extensions (Classic Trojan Tactic)
        if (lowerName.endsWith(".pdf.apk") || lowerName.endsWith(".doc.apk") ||
            lowerName.endsWith(".docx.apk") || lowerName.endsWith(".jpg.apk") ||
            lowerName.endsWith(".jpeg.apk") || lowerName.endsWith(".png.apk") ||
            lowerName.endsWith(".mp4.apk") || lowerName.endsWith(".zip.apk") ||
            lowerName.endsWith(".xls.apk") || lowerName.endsWith(".xlsx.apk")) {

            riskScore = Math.max(riskScore, 95);
            indicators.add(new DetectedIndicator(
                    "DOWNLOAD_DISGUISED_APK",
                    "Disguised Executable Trojan (.apk disguised as document/media)",
                    "This file attempts to deceive users by pretending to be a document or photo while actually being an executable Android Application Package (.apk). Attackers use this to trick victims into running malware.",
                    DetectedIndicator.Severity.CRITICAL,
                    IndicatorCategory.MALICIOUS_DOWNLOAD,
                    cleanName
            ));
            whyFlagged.add("File hides an executable .apk extension behind a fake document or image name.");
            recommendedActions.add("DO NOT open or install this file.");
            recommendedActions.add("Delete this file immediately from your Downloads folder.");
        }

        // 2. Check for Standalone APK File
        else if (lowerName.endsWith(".apk")) {
            int apkScore = 70;
            String apkDetails = "Standalone APK installer package";

            // If file is available on disk, deep-inspect its requested permissions
            if (context != null && filePath != null) {
                try {
                    File file = new File(filePath);
                    if (file.exists()) {
                        PackageManager pm = context.getPackageManager();
                        PackageInfo pi = pm.getPackageArchiveInfo(filePath, PackageManager.GET_PERMISSIONS);
                        if (pi != null && pi.requestedPermissions != null) {
                            Set<String> perms = new HashSet<>(Arrays.asList(pi.requestedPermissions));
                            boolean overlay = perms.contains("android.permission.SYSTEM_ALERT_WINDOW");
                            boolean access = perms.contains("android.permission.BIND_ACCESSIBILITY_SERVICE");
                            boolean smsRead = perms.contains("android.permission.READ_SMS") || perms.contains("android.permission.RECEIVE_SMS");
                            boolean smsSend = perms.contains("android.permission.SEND_SMS");

                            if (overlay && access) {
                                apkScore = 95;
                                apkDetails = "Banking Trojan Combo detected (Overlay + Accessibility Service)";
                                whyFlagged.add("Downloaded APK requests dangerous overlay drawing and accessibility services.");
                            } else if (smsRead && smsSend) {
                                apkScore = 85;
                                apkDetails = "SMS Stealer Combo detected (Read SMS + Send SMS)";
                                whyFlagged.add("Downloaded APK has permissions to intercept and send text messages in background.");
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            riskScore = Math.max(riskScore, apkScore);
            indicators.add(new DetectedIndicator(
                    "DOWNLOAD_UNVERIFIED_APK",
                    "Unverified Sideloaded APK Package",
                    "Downloaded APK files bypass Google Play Protect verification. Sideloading APKs from internet browsers is the primary distribution method for Android malware and banking trojans.",
                    apkScore >= 85 ? DetectedIndicator.Severity.CRITICAL : DetectedIndicator.Severity.WARNING,
                    IndicatorCategory.MALICIOUS_DOWNLOAD,
                    apkDetails
            ));
            whyFlagged.add("File is an unverified executable application package (.apk).");
            recommendedActions.add("Only install applications from official stores (Google Play Store, Samsung Galaxy Store).");
            recommendedActions.add("Delete this APK if you did not intentionally download it from a verified publisher.");
        }

        // 3. Check for Executable Scripts or Windows Binaries
        else if (lowerName.endsWith(".sh") || lowerName.endsWith(".bat") ||
                 lowerName.endsWith(".vbs") || lowerName.endsWith(".dex") ||
                 lowerName.endsWith(".jar") || lowerName.endsWith(".so") ||
                 lowerName.endsWith(".exe") || lowerName.endsWith(".cmd") ||
                 lowerName.endsWith(".ps1")) {

            riskScore = Math.max(riskScore, 75);
            indicators.add(new DetectedIndicator(
                    "DOWNLOAD_EXECUTABLE_SCRIPT",
                    "Suspicious Executable Script / Binary",
                    "The downloaded file contains executable scripts or compiled code. Such files are frequently used to download second-stage malware or exploit device vulnerabilities.",
                    DetectedIndicator.Severity.CRITICAL,
                    IndicatorCategory.MALICIOUS_DOWNLOAD,
                    cleanName
            ));
            whyFlagged.add("File contains executable scripting commands or binary code.");
            recommendedActions.add("Delete this file unless you are a developer executing specific terminal commands.");
        }

        // 4. Check for Downloaded HTML Forms (Offline Phishing Portals)
        else if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            riskScore = Math.max(riskScore, 50);
            indicators.add(new DetectedIndicator(
                    "DOWNLOAD_HTML_PORTAL",
                    "Downloaded Offline HTML Document",
                    "Phishing campaigns often distribute HTML attachments that load offline login portals designed to capture credentials without triggering network-based firewall blocks.",
                    DetectedIndicator.Severity.WARNING,
                    IndicatorCategory.MALICIOUS_DOWNLOAD,
                    cleanName
            ));
            whyFlagged.add("Downloaded HTML file may be an offline credential harvesting portal.");
            recommendedActions.add("Do not enter passwords or personal info into locally opened HTML files.");
        }

        // 5. Inspect Origin Download Link (if available)
        if (originUrl != null && !originUrl.trim().isEmpty()) {
            List<DetectedIndicator> urlIndicators = UrlIndicatorDetector.analyzeUrl(originUrl);
            if (!urlIndicators.isEmpty()) {
                indicators.addAll(urlIndicators);
                whyFlagged.add("Download originated from a suspicious URL: " + originUrl);
                riskScore += 25;
            }
        }

        riskScore = Math.min(100, riskScore);
        RiskLevel riskLevel = RiskLevel.fromScore(riskScore);

        // Safe Clean File Fallback
        if (indicators.isEmpty()) {
            riskScore = 0;
            riskLevel = RiskLevel.LOW_RISK;
            whyFlagged.add("Standard non-executable file format.");
            whyFlagged.add("No disguised extensions, script payloads, or deceptive indicators detected.");
            recommendedActions.add("This file appears safe based on on-device heuristic analysis.");
        }

        verificationNotes.add("File Name: " + cleanName);
        if (filePath != null) verificationNotes.add("Location: " + filePath);
        if (originUrl != null && !originUrl.isEmpty()) verificationNotes.add("Origin: " + originUrl);

        return new ScamAnalysisResult(
                UUID.randomUUID().toString(),
                "Downloaded File: " + cleanName + (originUrl != null ? " (" + originUrl + ")" : ""),
                ScanType.FILE_DOWNLOAD,
                riskScore,
                riskLevel,
                indicators,
                whyFlagged,
                recommendedActions,
                verificationNotes,
                true,
                System.currentTimeMillis()
        );
    }
}
