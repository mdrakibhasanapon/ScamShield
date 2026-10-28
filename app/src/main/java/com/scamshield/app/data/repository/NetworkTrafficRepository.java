package com.scamshield.app.data.repository;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.scamshield.app.data.model.NetworkTrafficItem;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.domain.scanner.NetworkThreatEvaluator;
import com.scamshield.app.util.NotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class NetworkTrafficRepository {

    public static final String ACTION_TRAFFIC_UPDATED = "com.scamshield.app.ACTION_TRAFFIC_UPDATED";
    private static final String PREF_NAME = "scamshield_network_traffic";
    private static final String KEY_LOGS = "traffic_logs_json";
    private static final int MAX_LOGS = 150;

    private static NetworkTrafficRepository instance;
    private final Context context;
    private final LinkedList<NetworkTrafficItem> inMemoryLogs = new LinkedList<>();

    private NetworkTrafficRepository(Context context) {
        this.context = context.getApplicationContext();
        loadFromPrefs();
        if (inMemoryLogs.isEmpty()) {
            seedInitialSamples();
        }
    }

    public static synchronized NetworkTrafficRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkTrafficRepository(context);
        }
        return instance;
    }

    public synchronized void logTraffic(NetworkTrafficItem item) {
        if (item == null) return;

        inMemoryLogs.addFirst(item);
        while (inMemoryLogs.size() > MAX_LOGS) {
            inMemoryLogs.removeLast();
        }

        saveToPrefs();

        // Broadcast to UI
        Intent intent = new Intent(ACTION_TRAFFIC_UPDATED);
        context.sendBroadcast(intent);

        // Also record to main History Repository if high or suspicious threat
        if (item.getRiskScore() >= 50) {
            new HistoryRepository(context).saveScan(
                    NetworkThreatEvaluator.toScamAnalysisResult(item)
            );
        }

        // Show Notification Alert for High-Risk Exfiltration or Leaks
        if (item.getRiskScore() >= 70) {
            NotificationHelper.showNetworkAlertNotification(context, item);
        }
    }

    public synchronized List<NetworkTrafficItem> getAllLogs() {
        return new ArrayList<>(inMemoryLogs);
    }

    public synchronized void clearLogs() {
        inMemoryLogs.clear();
        saveToPrefs();
        context.sendBroadcast(new Intent(ACTION_TRAFFIC_UPDATED));
    }

    private void saveToPrefs() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray();
            for (NetworkTrafficItem item : inMemoryLogs) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("pkg", item.getPackageName());
                obj.put("app", item.getAppName());
                obj.put("ip", item.getDestinationIp());
                obj.put("host", item.getDestinationHost());
                obj.put("port", item.getDestinationPort());
                obj.put("proto", item.getProtocol());
                obj.put("dir", item.getDirection().name());
                obj.put("tx", item.getBytesUploaded());
                obj.put("rx", item.getBytesDownloaded());
                obj.put("summary", item.getPayloadSummary());
                obj.put("time", item.getTimestamp());
                obj.put("score", item.getRiskScore());
                obj.put("level", item.getRiskLevel().name());
                obj.put("cat", item.getThreatCategory());
                obj.put("why", item.getWhyFlagged());
                obj.put("action", item.getRecommendedAction());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_LOGS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void loadFromPrefs() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_LOGS, null);
            if (raw == null) return;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                NetworkTrafficItem.Direction dir;
                try {
                    dir = NetworkTrafficItem.Direction.valueOf(obj.getString("dir"));
                } catch (Exception e) {
                    dir = NetworkTrafficItem.Direction.BIDIRECTIONAL;
                }
                RiskLevel level;
                try {
                    level = RiskLevel.valueOf(obj.getString("level"));
                } catch (Exception e) {
                    level = RiskLevel.LOW_RISK;
                }

                NetworkTrafficItem item = new NetworkTrafficItem(
                        obj.optString("id"),
                        obj.optString("pkg"),
                        obj.optString("app"),
                        obj.optString("ip"),
                        obj.optString("host"),
                        obj.optInt("port", 443),
                        obj.optString("proto", "HTTPS (TLS)"),
                        dir,
                        obj.optLong("tx", 0),
                        obj.optLong("rx", 0),
                        obj.optString("summary"),
                        obj.optLong("time", System.currentTimeMillis()),
                        obj.optInt("score", 0),
                        level,
                        obj.optString("cat", "CLEAN_TRAFFIC"),
                        obj.optString("why"),
                        obj.optString("action")
                );
                inMemoryLogs.add(item);
            }
        } catch (Exception ignored) {}
    }

    private void seedInitialSamples() {
        // Initial clean sample to populate empty UI
        inMemoryLogs.add(NetworkThreatEvaluator.evaluateTraffic(
                "com.google.android.apps.docs",
                "Google Drive",
                "142.250.190.46",
                "drive.googleapis.com",
                443,
                "HTTPS (TLS)",
                NetworkTrafficItem.Direction.BIDIRECTIONAL,
                14200,
                384000,
                "Encrypted cloud document sync"
        ));
    }

    public void simulateScenario(int scenarioIndex) {
        NetworkTrafficItem item;
        switch (scenarioIndex) {
            case 0:
                // Background Spyware Exfiltration
                item = NetworkThreatEvaluator.evaluateTraffic(
                        "com.utility.superflashlight",
                        "Super Flashlight HD",
                        "194.87.142.18",
                        "c2-exfil-node.xyz",
                        4444,
                        "TCP",
                        NetworkTrafficItem.Direction.UPLOAD,
                        348000, // 348 KB
                        1200,
                        "Uploaded 348 KB device identifiers to command-and-control port 4444"
                );
                break;
            case 1:
                // Unencrypted Plaintext Password / HTTP Leak
                item = NetworkThreatEvaluator.evaluateTraffic(
                        "com.fastdeal.shopping",
                        "FastShop Rewards",
                        "93.184.216.34",
                        "api.fastshop-unencrypted.com",
                        80,
                        "HTTP (Plaintext)",
                        NetworkTrafficItem.Direction.UPLOAD,
                        18500,
                        4200,
                        "Uploaded login form payload over unencrypted HTTP (Port 80)"
                );
                break;
            case 2:
                // Adware / Behavioral Telemetry Tracker
                item = NetworkThreatEvaluator.evaluateTraffic(
                        "com.freegames.solitaire",
                        "Classic Solitaire Plus",
                        "104.244.42.1",
                        "analytics.appsflyer.com",
                        443,
                        "HTTPS (TLS)",
                        NetworkTrafficItem.Direction.UPLOAD,
                        12800,
                        2100,
                        "Transmitted device fingerprint & advertising ID to behavioral tracker"
                );
                break;
            case 3:
            default:
                // Clean Verified Sync
                item = NetworkThreatEvaluator.evaluateTraffic(
                        "com.whatsapp",
                        "WhatsApp Messenger",
                        "157.240.22.60",
                        "g.whatsapp.net",
                        443,
                        "HTTPS (TLS)",
                        NetworkTrafficItem.Direction.DOWNLOAD,
                        4200,
                        845000,
                        "Standard end-to-end encrypted message sync via verified WhatsApp gateway"
                );
                break;
        }
        logTraffic(item);
    }
}
