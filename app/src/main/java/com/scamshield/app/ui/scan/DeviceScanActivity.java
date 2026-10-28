package com.scamshield.app.ui.scan;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.domain.scanner.DeviceScanSummary;
import com.scamshield.app.domain.scanner.DeviceSecurityScanner;
import com.scamshield.app.domain.scanner.ScannedThreatItem;
import com.scamshield.app.domain.scanner.ThreatType;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity implements ThreatItemAdapter.OnThreatActionListener {

    private DeviceSecurityScanner scanner;
    private PreferencesManager preferencesManager;

    // Scanning State Views
    private View layoutScanProgress;
    private CircularProgressIndicator progressIndicator;
    private TextView tvProgressPercent;
    private TextView tvScanPhase;
    private TextView tvCurrentItem;

    // Results State Views
    private View layoutScanResults;
    private MaterialCardView cardHealthScore;
    private TextView tvHealthScoreTitle;
    private TextView tvHealthScoreDesc;
    private TextView tvHealthScoreNumber;
    private TextView tvStatApps;
    private TextView tvStatFiles;
    private TextView tvStatThreats;
    private MaterialButton btnFilterThreats;
    private MaterialButton btnFilterAll;
    private View layoutEmptyThreats;
    private RecyclerView rvThreatItems;
    private MaterialButton btnRescan;

    private DeviceScanSummary currentSummary;
    private ThreatItemAdapter adapter;
    private final List<ScannedThreatItem> displayedItems = new ArrayList<>();
    private boolean showingOnlyThreats = true;
    private String pendingUninstallPackage = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        scanner = new DeviceSecurityScanner();
        preferencesManager = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_device_scan);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        startDeviceScan();
    }

    private void initViews() {
        // Scanning Progress Views
        layoutScanProgress = findViewById(R.id.layout_scan_progress);
        progressIndicator = findViewById(R.id.progress_indicator);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        tvScanPhase = findViewById(R.id.tv_scan_phase);
        tvCurrentItem = findViewById(R.id.tv_current_item);

        // Results Views
        layoutScanResults = findViewById(R.id.layout_scan_results);
        cardHealthScore = findViewById(R.id.card_health_score);
        tvHealthScoreTitle = findViewById(R.id.tv_health_score_title);
        tvHealthScoreDesc = findViewById(R.id.tv_health_score_desc);
        tvHealthScoreNumber = findViewById(R.id.tv_health_score_number);
        tvStatApps = findViewById(R.id.tv_stat_apps);
        tvStatFiles = findViewById(R.id.tv_stat_files);
        tvStatThreats = findViewById(R.id.tv_stat_threats);
        btnFilterThreats = findViewById(R.id.btn_filter_threats);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        layoutEmptyThreats = findViewById(R.id.layout_empty_threats);
        rvThreatItems = findViewById(R.id.rv_threat_items);
        btnRescan = findViewById(R.id.btn_rescan);

        rvThreatItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ThreatItemAdapter(displayedItems, this);
        rvThreatItems.setAdapter(adapter);

        btnFilterThreats.setOnClickListener(v -> {
            showingOnlyThreats = true;
            updateFilterButtons();
            populateList();
        });

        btnFilterAll.setOnClickListener(v -> {
            showingOnlyThreats = false;
            updateFilterButtons();
            populateList();
        });

        btnRescan.setOnClickListener(v -> startDeviceScan());
    }

    private void startDeviceScan() {
        layoutScanProgress.setVisibility(View.VISIBLE);
        layoutScanResults.setVisibility(View.GONE);
        progressIndicator.setProgress(0);
        tvProgressPercent.setText("0%");
        tvScanPhase.setText("Auditing System Security Integrity…");
        tvCurrentItem.setText("Checking for root binaries and debug flags…");

        scanner.scanDeviceAsync(this, new DeviceSecurityScanner.ScanCallback() {
            @Override
            public void onScanProgress(String currentPhase, int progressPercent, String currentItemName) {
                progressIndicator.setProgress(progressPercent);
                tvProgressPercent.setText(progressPercent + "%");
                tvScanPhase.setText(currentPhase);
                tvCurrentItem.setText(currentItemName);
            }

            @Override
            public void onScanComplete(DeviceScanSummary summary) {
                currentSummary = summary;
                layoutScanProgress.setVisibility(View.GONE);
                layoutScanResults.setVisibility(View.VISIBLE);

                // Save scan results in preferences
                preferencesManager.setLastDeviceScanTime(summary.getTimestamp());
                preferencesManager.setLastDeviceScore(summary.getDeviceSecurityScore());
                preferencesManager.setLastDeviceThreatCount(summary.getThreatCount());

                bindSummary(summary);
            }

            @Override
            public void onScanError(String errorMessage) {
                layoutScanProgress.setVisibility(View.GONE);
                Toast.makeText(DeviceScanActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindSummary(DeviceScanSummary summary) {
        int score = summary.getDeviceSecurityScore();
        tvHealthScoreNumber.setText(String.valueOf(score));
        tvStatApps.setText("Apps: " + summary.getAppsScanned());
        tvStatFiles.setText("Files: " + summary.getFilesScanned());
        tvStatThreats.setText("Threats: " + summary.getThreatCount());

        btnFilterThreats.setText("Threats (" + summary.getThreatCount() + ")");
        btnFilterAll.setText("All Items (" + summary.getTotalItemsScanned() + ")");

        if (summary.isDeviceSafe()) {
            tvHealthScoreTitle.setText("Phone is Protected");
            tvHealthScoreDesc.setText("All scanned applications, storage, and settings passed security checks.");
            cardHealthScore.setCardBackgroundColor(ContextCompat.getColor(this, R.color.risk_low));
        } else if (score < 60) {
            tvHealthScoreTitle.setText("High Risk Detected");
            tvHealthScoreDesc.setText(summary.getThreatCount() + " malicious or suspicious items require action.");
            cardHealthScore.setCardBackgroundColor(ContextCompat.getColor(this, R.color.risk_high));
        } else {
            tvHealthScoreTitle.setText("Review Recommended");
            tvHealthScoreDesc.setText(summary.getThreatCount() + " warning items found on your device.");
            cardHealthScore.setCardBackgroundColor(ContextCompat.getColor(this, R.color.risk_suspicious));
        }

        updateFilterButtons();
        populateList();
    }

    private void updateFilterButtons() {
        if (showingOnlyThreats) {
            btnFilterThreats.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_primary));
            btnFilterThreats.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnFilterAll.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btnFilterAll.setTextColor(ContextCompat.getColor(this, R.color.text_primary_light));
        } else {
            btnFilterAll.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_primary));
            btnFilterAll.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnFilterThreats.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btnFilterThreats.setTextColor(ContextCompat.getColor(this, R.color.text_primary_light));
        }
    }

    private void populateList() {
        displayedItems.clear();
        if (currentSummary == null) return;

        if (showingOnlyThreats) {
            displayedItems.addAll(currentSummary.getThreatItems());
            if (displayedItems.isEmpty()) {
                layoutEmptyThreats.setVisibility(View.VISIBLE);
                rvThreatItems.setVisibility(View.GONE);
            } else {
                layoutEmptyThreats.setVisibility(View.GONE);
                rvThreatItems.setVisibility(View.VISIBLE);
            }
        } else {
            displayedItems.addAll(currentSummary.getThreatItems());
            displayedItems.addAll(currentSummary.getCleanItems());
            layoutEmptyThreats.setVisibility(View.GONE);
            rvThreatItems.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActionClick(ScannedThreatItem item, int position) {
        if (item.getType() == ThreatType.APP) {
            confirmUninstallApp(item, position);
        } else if (item.getType() == ThreatType.FILE) {
            confirmDeleteFile(item, position);
        } else if (item.getType() == ThreatType.SYSTEM_SETTING) {
            handleSystemSettingFix(item);
        }
    }

    private void confirmUninstallApp(ScannedThreatItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Uninstall " + item.getTitle() + "?")
                .setMessage("ScamShield will launch the system uninstaller to remove this application and its dangerous permissions from your phone.")
                .setIcon(R.drawable.ic_delete_trash)
                .setPositiveButton("Uninstall", (dialog, which) -> {
                    pendingUninstallPackage = item.getPackageName();
                    DeviceSecurityScanner.requestAppUninstall(this, item.getPackageName());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteFile(ScannedThreatItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Permanently Delete File?")
                .setMessage("Are you sure you want to delete \"" + item.getTitle() + "\" (" + item.getFileSizeFormatted() + ") from your device storage? This cannot be undone.")
                .setIcon(R.drawable.ic_delete_trash)
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = DeviceSecurityScanner.deleteMaliciousFile(item.getFilePath());
                    if (deleted) {
                        item.setResolved(true);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "Malicious file deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete file. Check file permissions.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSystemSettingFix(ScannedThreatItem item) {
        if ("sys_adb".equals(item.getId())) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        } else if ("sys_lock".equals(item.getId())) {
            try {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        } else {
            Toast.makeText(this, "Follow on-screen instructions to resolve this configuration.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if pending uninstalled app was removed
        if (pendingUninstallPackage != null && currentSummary != null) {
            try {
                getPackageManager().getPackageInfo(pendingUninstallPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Package was successfully uninstalled!
                for (int i = 0; i < displayedItems.size(); i++) {
                    ScannedThreatItem item = displayedItems.get(i);
                    if (pendingUninstallPackage.equals(item.getPackageName())) {
                        item.setResolved(true);
                        adapter.notifyItemChanged(i);
                        Toast.makeText(this, "App uninstalled successfully!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                pendingUninstallPackage = null;
            }
        }
    }
}
