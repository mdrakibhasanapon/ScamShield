package com.scamshield.app.domain.scanner;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;

import com.scamshield.app.data.model.NetworkTrafficItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppDataUsageMonitor {

    public static class AppTrafficSnapshot {
        public String packageName;
        public String appName;
        public int uid;
        public long txBytes; // Total transmitted (Uploaded)
        public long rxBytes; // Total received (Downloaded)
        public long uploadSpeedBps; // Bytes per second
        public long downloadSpeedBps;
        public boolean isSystemApp;
        public long lastCheckedTimestamp;
    }

    private static final Map<Integer, AppTrafficSnapshot> previousSnapshots = new HashMap<>();
    private static long lastSampleTime = 0;

    public static synchronized List<AppTrafficSnapshot> sampleAppTraffic(Context context) {
        List<AppTrafficSnapshot> results = new ArrayList<>();
        if (context == null) return results;

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps;
        try {
            apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            return results;
        }

        long now = System.currentTimeMillis();
        long intervalSec = (lastSampleTime > 0) ? Math.max(1, (now - lastSampleTime) / 1000) : 1;
        lastSampleTime = now;

        Map<Integer, AppTrafficSnapshot> currentMap = new HashMap<>();

        for (ApplicationInfo ai : apps) {
            int uid = ai.uid;
            long tx = TrafficStats.getUidTxBytes(uid);
            long rx = TrafficStats.getUidRxBytes(uid);

            if (tx <= 0 && rx <= 0) continue; // No data ever transferred by this app

            AppTrafficSnapshot prev = previousSnapshots.get(uid);

            AppTrafficSnapshot current = new AppTrafficSnapshot();
            current.packageName = ai.packageName;
            try {
                current.appName = pm.getApplicationLabel(ai).toString();
            } catch (Exception e) {
                current.appName = ai.packageName;
            }
            current.uid = uid;
            current.txBytes = tx;
            current.rxBytes = rx;
            current.isSystemApp = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            current.lastCheckedTimestamp = now;

            if (prev != null) {
                long txDelta = Math.max(0, tx - prev.txBytes);
                long rxDelta = Math.max(0, rx - prev.rxBytes);
                current.uploadSpeedBps = txDelta / intervalSec;
                current.downloadSpeedBps = rxDelta / intervalSec;
            } else {
                current.uploadSpeedBps = 0;
                current.downloadSpeedBps = 0;
            }

            currentMap.put(uid, current);
            results.add(current);
        }

        previousSnapshots.clear();
        previousSnapshots.putAll(currentMap);

        // Sort: Active apps with highest current transfer speeds or highest total data first
        Collections.sort(results, (a, b) -> {
            long speedA = a.uploadSpeedBps + a.downloadSpeedBps;
            long speedB = b.uploadSpeedBps + b.downloadSpeedBps;
            if (speedA != speedB) {
                return Long.compare(speedB, speedA);
            }
            return Long.compare((b.txBytes + b.rxBytes), (a.txBytes + a.rxBytes));
        });

        return results;
    }
}
