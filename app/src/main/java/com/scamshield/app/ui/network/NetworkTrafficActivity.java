package com.scamshield.app.ui.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scamshield.app.R;
import com.scamshield.app.data.model.NetworkTrafficItem;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.repository.NetworkTrafficRepository;
import com.scamshield.app.domain.scanner.AppDataUsageMonitor;
import com.scamshield.app.domain.scanner.NetworkThreatEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NetworkTrafficActivity extends AppCompatActivity implements NetworkTrafficAdapter.OnItemClickListener {

    private NetworkTrafficRepository repository;
    private NetworkTrafficAdapter adapter;
    private RecyclerView rvTraffic;
    private View layoutEmpty;
    private TextView tvUploadSpeed;
    private TextView tvDownloadSpeed;
    private TextView tvThreatsCount;
    private ChipGroup chipGroupFilters;

    private int currentFilterId = R.id.chip_all;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isMonitoring = false;

    private final BroadcastReceiver trafficReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshLogs();
        }
    };

    private final Runnable sampleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring) return;
            sampleNetworkSpeeds();
            handler.postDelayed(this, 2500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_traffic);

        repository = NetworkTrafficRepository.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvTraffic = findViewById(R.id.rv_network_traffic);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvUploadSpeed = findViewById(R.id.tv_upload_speed);
        tvDownloadSpeed = findViewById(R.id.tv_download_speed);
        tvThreatsCount = findViewById(R.id.tv_threats_count);
        chipGroupFilters = findViewById(R.id.chip_group_filters);

        adapter = new NetworkTrafficAdapter(this);
        rvTraffic.setLayoutManager(new LinearLayoutManager(this));
        rvTraffic.setAdapter(adapter);

        findViewById(R.id.btn_simulate_traffic).setOnClickListener(v -> showSimulationDialog());
        findViewById(R.id.btn_refresh_traffic).setOnClickListener(v -> {
            sampleNetworkSpeeds();
            Toast.makeText(this, "Active network apps updated", Toast.LENGTH_SHORT).show();
        });

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
                refreshLogs();
            }
        });

        refreshLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isMonitoring = true;
        handler.post(sampleRunnable);

        try {
            IntentFilter filter = new IntentFilter(NetworkTrafficRepository.ACTION_TRAFFIC_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(trafficReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(trafficReceiver, filter);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        isMonitoring = false;
        handler.removeCallbacks(sampleRunnable);
        try {
            unregisterReceiver(trafficReceiver);
        } catch (Exception ignored) {}
    }

    private void sampleNetworkSpeeds() {
        List<AppDataUsageMonitor.AppTrafficSnapshot> snapshots =
                AppDataUsageMonitor.sampleAppTraffic(this);

        long totalTxSpeed = 0;
        long totalRxSpeed = 0;

        for (AppDataUsageMonitor.AppTrafficSnapshot s : snapshots) {
            totalTxSpeed += s.uploadSpeedBps;
            totalRxSpeed += s.downloadSpeedBps;

            // If an app is currently transmitting a noticeable burst of data (> 15 KB), log it
            if (s.uploadSpeedBps > 15 * 1024) {
                repository.logTraffic(NetworkThreatEvaluator.evaluateTraffic(
                        s.packageName,
                        s.appName,
                        "Remote Gateway",
                        "Active Uplink",
                        443,
                        "HTTPS",
                        NetworkTrafficItem.Direction.UPLOAD,
                        s.uploadSpeedBps,
                        0,
                        String.format(Locale.ROOT, "Active upload: %s/s transmitted by %s",
                                NetworkTrafficItem.formatBytes(s.uploadSpeedBps), s.appName)
                ));
            } else if (s.downloadSpeedBps > 20 * 1024) {
                repository.logTraffic(NetworkThreatEvaluator.evaluateTraffic(
                        s.packageName,
                        s.appName,
                        "Remote Gateway",
                        "Active Downlink",
                        443,
                        "HTTPS",
                        NetworkTrafficItem.Direction.DOWNLOAD,
                        0,
                        s.downloadSpeedBps,
                        String.format(Locale.ROOT, "Active download: %s/s received by %s",
                                NetworkTrafficItem.formatBytes(s.downloadSpeedBps), s.appName)
                ));
            }
        }

        tvUploadSpeed.setText(NetworkTrafficItem.formatBytes(totalTxSpeed) + "/s");
        tvDownloadSpeed.setText(NetworkTrafficItem.formatBytes(totalRxSpeed) + "/s");
    }

    private void refreshLogs() {
        List<NetworkTrafficItem> all = repository.getAllLogs();
        List<NetworkTrafficItem> filtered = new ArrayList<>();
        int threatCount = 0;

        for (NetworkTrafficItem item : all) {
            if (item.getRiskScore() >= 50) {
                threatCount++;
            }

            if (currentFilterId == R.id.chip_threats) {
                if (item.getRiskScore() >= 50) filtered.add(item);
            } else if (currentFilterId == R.id.chip_uploads) {
                if (item.getDirection() == NetworkTrafficItem.Direction.UPLOAD) filtered.add(item);
            } else if (currentFilterId == R.id.chip_downloads) {
                if (item.getDirection() == NetworkTrafficItem.Direction.DOWNLOAD) filtered.add(item);
            } else {
                filtered.add(item);
            }
        }

        tvThreatsCount.setText(String.valueOf(threatCount));
        adapter.updateList(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTraffic.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTraffic.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(NetworkTrafficItem item) {
        if (item == null) return;

        String details = "📦 Application:\n" + item.getAppName() + " (" + item.getPackageName() + ")\n\n" +
                "🌐 Destination Endpoint:\n" + item.getDestinationHost() + "\n" +
                "IP & Port: " + item.getDestinationIp() + ":" + item.getDestinationPort() + " (" + item.getProtocol() + ")\n\n" +
                "📊 Transfer Data:\n" +
                "• Uploaded: " + NetworkTrafficItem.formatBytes(item.getBytesUploaded()) + "\n" +
                "• Downloaded: " + NetworkTrafficItem.formatBytes(item.getBytesDownloaded()) + "\n\n" +
                "🛡️ Threat Evaluation:\n" +
                "• Risk Score: " + item.getRiskScore() + "/100 (" + item.getRiskLevel().getTitle() + ")\n" +
                "• Reason: " + item.getWhyFlagged() + "\n\n" +
                "💡 Recommended Action:\n" + item.getRecommendedAction();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Network Inspection Details")
                .setMessage(details)
                .setPositiveButton("Open App Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + item.getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Unable to open app settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSimulationDialog() {
        String[] options = {
                "🚨 Background Spyware Upload (Port 4444 to .xyz C2)",
                "⚠️ Plaintext Password Leak (Port 80 HTTP Upload)",
                "📊 Telemetry / Ad Tracker (AppsFlyer Fingerprinting)",
                "🛡️ Clean Encrypted Sync (Verified WhatsApp Gateway)"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("🧪 Test Network Threat Watchdog")
                .setItems(options, (dialog, which) -> {
                    repository.simulateScenario(which);
                    refreshLogs();
                    Toast.makeText(this, "Simulated network event logged!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
