package com.scamshield.app.domain.scanner;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;
import com.scamshield.app.data.model.NetworkTrafficItem;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class NetworkThreatEvaluator {

    private static final Set<String> HIGH_RISK_TLDS = new HashSet<>(Arrays.asList(
            ".ru", ".su", ".top", ".xyz", ".icu", ".tk", ".click", ".buzz", ".pw", ".cc", ".to", ".live"
    ));

    private static final Set<String> KNOWN_TRACKERS = new HashSet<>(Arrays.asList(
            "appsflyer.com", "adjust.com", "kochava.com", "branch.io", "flurry.com",
            "scorecardresearch.com", "doubleclick.net", "app-measurement.com",
            "admob.com", "adservice.google.com", "analytics.tiktok.com",
            "graph.facebook.com", "metrics.icloud.com"
    ));

    private static final Set<Integer> SUSPICIOUS_TROJAN_PORTS = new HashSet<>(Arrays.asList(
            4444, 5555, 6666, 6667, 7777, 8888, 9999, 1337, 31337
    ));

    private static final Set<String> TRUSTED_HOST_DOMAINS = new HashSet<>(Arrays.asList(
            "google.com", "googleapis.com", "gstatic.com", "android.com", "play.google.com",
            "microsoft.com", "apple.com", "cloudflare.com", "whatsapp.net", "telegram.org"
    ));

    public static NetworkTrafficItem evaluateTraffic(
            String packageName,
            String appName,
            String remoteIp,
            String remoteHost,
            int port,
            String protocol,
            NetworkTrafficItem.Direction direction,
            long bytesUploaded,
            long bytesDownloaded,
            String initialPayloadSummary) {

        int riskScore = 0;
        String threatCategory = "CLEAN_TRAFFIC";
        StringBuilder whyFlagged = new StringBuilder();
        StringBuilder recommendedAction = new StringBuilder();

        String hostLower = remoteHost != null ? remoteHost.toLowerCase(Locale.ROOT) : "";
        String appNameLower = appName != null ? appName.toLowerCase(Locale.ROOT) : "";

        // 1. Check for Trojan / Reverse Shell Ports
        if (SUSPICIOUS_TROJAN_PORTS.contains(port)) {
            riskScore = Math.max(riskScore, 90);
            threatCategory = "SUSPICIOUS_C2_PORT";
            whyFlagged.append(String.format(Locale.ROOT, "App connecting to notorious C2/trojan port %d. ", port));
            recommendedAction.append("Immediately inspect or uninstall this application. ");
        }

        // 2. Check for Plaintext HTTP Leakage on Uploads
        if (port == 80 || "HTTP".equalsIgnoreCase(protocol)) {
            if (direction == NetworkTrafficItem.Direction.UPLOAD && bytesUploaded > 0) {
                riskScore = Math.max(riskScore, 70);
                threatCategory = "UNENCRYPTED_DATA_LEAK";
                whyFlagged.append("App is uploading data over unencrypted HTTP (Port 80). Passwords and private data can be intercepted. ");
                recommendedAction.append("Avoid entering sensitive information inside this app on public Wi-Fi. ");
            } else {
                riskScore = Math.max(riskScore, 35);
                whyFlagged.append("Connection uses unencrypted HTTP protocol. ");
            }
        }

        // 3. Check for Suspicious TLDs
        for (String tld : HIGH_RISK_TLDS) {
            if (hostLower.endsWith(tld)) {
                riskScore = Math.max(riskScore, 75);
                threatCategory = "SUSPICIOUS_DESTINATION";
                whyFlagged.append(String.format(Locale.ROOT, "Destination domain uses high-risk TLD '%s'. ", tld));
                recommendedAction.append("Check if this app is a verified official release. ");
                break;
            }
        }

        // 4. Check for Adware / Telemetry Trackers
        for (String tracker : KNOWN_TRACKERS) {
            if (hostLower.contains(tracker)) {
                if (riskScore < 50) {
                    riskScore = Math.max(riskScore, 45);
                    threatCategory = "AD_TRACKER";
                    whyFlagged.append(String.format(Locale.ROOT, "Connection to known behavioral telemetry/ad tracking network (%s). ", tracker));
                    recommendedAction.append("App collects usage analytics and device identifiers. ");
                }
                break;
            }
        }

        // 5. Check for Suspicious C2 Keywords in Hostname
        if (hostLower.contains("c2-") || hostLower.contains("exfil") || hostLower.contains("botnet") ||
                hostLower.contains("miner") || hostLower.contains("stealer")) {
            riskScore = Math.max(riskScore, 95);
            threatCategory = "MALICIOUS_C2_SERVER";
            whyFlagged.append("Destination domain matches malware command-and-control keywords. ");
            recommendedAction.append("Severe risk: delete this application immediately. ");
        }

        // 6. Utility App Anomalous Uploads (e.g. Calculator, Flashlight uploading data)
        if (direction == NetworkTrafficItem.Direction.UPLOAD && bytesUploaded > 50 * 1024) {
            if (appNameLower.contains("flashlight") || appNameLower.contains("calculator") ||
                    appNameLower.contains("wallpaper") || appNameLower.contains("battery saver")) {
                riskScore = Math.max(riskScore, 80);
                threatCategory = "ANOMALOUS_EXFILTRATION";
                whyFlagged.append(String.format(Locale.ROOT, "Offline utility app '%s' is uploading an abnormal amount of data (%s). ",
                        appName, NetworkTrafficItem.formatBytes(bytesUploaded)));
                recommendedAction.append("Restrict background data permissions in Android Settings for this app. ");
            }
        }

        // 7. Safe / Trusted Traffic Verification
        boolean isTrusted = false;
        for (String trusted : TRUSTED_HOST_DOMAINS) {
            if (hostLower.endsWith(trusted)) {
                isTrusted = true;
                break;
            }
        }

        if (isTrusted && threatCategory.equals("CLEAN_TRAFFIC")) {
            riskScore = 0;
            whyFlagged.append("Verified trusted infrastructure endpoint. ");
            recommendedAction.append("Standard encrypted communication. Safe.");
        } else if (riskScore == 0) {
            whyFlagged.append("Standard encrypted TLS communication. ");
            recommendedAction.append("Normal application network traffic.");
        }

        riskScore = Math.min(100, Math.max(0, riskScore));
        RiskLevel level = RiskLevel.fromScore(riskScore);

        String summary = initialPayloadSummary;
        if (summary == null || summary.isEmpty()) {
            if (direction == NetworkTrafficItem.Direction.UPLOAD) {
                summary = String.format(Locale.ROOT, "Sent %s to %s via %s",
                        NetworkTrafficItem.formatBytes(bytesUploaded), remoteHost, protocol);
            } else if (direction == NetworkTrafficItem.Direction.DOWNLOAD) {
                summary = String.format(Locale.ROOT, "Received %s from %s via %s",
                        NetworkTrafficItem.formatBytes(bytesDownloaded), remoteHost, protocol);
            } else {
                summary = String.format(Locale.ROOT, "%s with %s (Port %d)", protocol, remoteHost, port);
            }
        }

        return new NetworkTrafficItem(
                UUID.randomUUID().toString(),
                packageName,
                appName,
                remoteIp,
                remoteHost,
                port,
                protocol,
                direction,
                bytesUploaded,
                bytesDownloaded,
                summary,
                System.currentTimeMillis(),
                riskScore,
                level,
                threatCategory,
                whyFlagged.toString().trim(),
                recommendedAction.toString().trim()
        );
    }

    public static ScamAnalysisResult toScamAnalysisResult(NetworkTrafficItem item) {
        List<DetectedIndicator> indicators = new ArrayList<>();
        if (item.getRiskScore() > 20) {
            indicators.add(new DetectedIndicator(
                    item.getThreatCategory(),
                    "Network Alert: " + item.getThreatCategory().replace("_", " "),
                    item.getWhyFlagged(),
                    item.getRiskScore() >= 75 ? DetectedIndicator.Severity.CRITICAL : DetectedIndicator.Severity.WARNING,
                    IndicatorCategory.DATA_EXFILTRATION,
                    item.getDestinationHost() + ":" + item.getDestinationPort()
            ));
        }

        List<String> whyFlagged = new ArrayList<>();
        whyFlagged.add(item.getWhyFlagged());

        List<String> actions = new ArrayList<>();
        actions.add(item.getRecommendedAction());

        List<String> notes = new ArrayList<>();
        notes.add("Application: " + item.getAppName() + " (" + item.getPackageName() + ")");
        notes.add("Destination: " + item.getDestinationHost() + " (" + item.getDestinationIp() + ":" + item.getDestinationPort() + ")");
        notes.add("Protocol: " + item.getProtocol());
        notes.add("Traffic: Uploaded " + NetworkTrafficItem.formatBytes(item.getBytesUploaded()) + ", Downloaded " + NetworkTrafficItem.formatBytes(item.getBytesDownloaded()));

        return new ScamAnalysisResult(
                item.getId(),
                "Network Traffic: " + item.getAppName() + " ➔ " + item.getDestinationHost(),
                ScanType.NETWORK_TRAFFIC,
                item.getRiskScore(),
                item.getRiskLevel(),
                indicators,
                whyFlagged,
                actions,
                notes,
                true,
                item.getTimestamp()
        );
    }
}
