package com.scamshield.app.ui.privacy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.scamshield.app.R;
import com.scamshield.app.data.model.AppPrivacyProfile;
import com.scamshield.app.data.model.AppPrivacySummary;
import com.scamshield.app.domain.scanner.AppPrivacyAuditor;

import java.util.ArrayList;
import java.util.List;

public class PrivacyGuardActivity extends AppCompatActivity implements AppPrivacyAdapter.OnActionListener {

    private AppPrivacyAuditor auditor;
    private AppPrivacyAdapter adapter;
    private RecyclerView rvApps;
    private View layoutLoading;
    private View layoutEmpty;
    private TextView tvLoadingStatus;
    private TextView tvCountMic;
    private TextView tvCountLocation;
    private TextView tvCountContacts;
    private TextView tvCountSpyware;
    private ChipGroup chipGroupFilters;

    private AppPrivacySummary currentSummary;
    private int currentFilterId = R.id.chip_all;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_guard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvApps = findViewById(R.id.rv_privacy_apps);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvLoadingStatus = findViewById(R.id.tv_loading_status);
        tvCountMic = findViewById(R.id.tv_count_mic);
        tvCountLocation = findViewById(R.id.tv_count_location);
        tvCountContacts = findViewById(R.id.tv_count_contacts);
        tvCountSpyware = findViewById(R.id.tv_count_spyware);
        chipGroupFilters = findViewById(R.id.chip_group_filters);

        rvApps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppPrivacyAdapter(this);
        rvApps.setAdapter(adapter);

        auditor = new AppPrivacyAuditor();

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
            } else {
                currentFilterId = R.id.chip_all;
            }
            applyCurrentFilter();
        });

        loadPrivacyAudit();
    }

    private void loadPrivacyAudit() {
        layoutLoading.setVisibility(View.VISIBLE);
        rvApps.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        auditor.auditInstalledAppsAsync(this, new AppPrivacyAuditor.AuditCallback() {
            @Override
            public void onProgress(int current, int total, String currentAppName) {
                if (tvLoadingStatus != null) {
                    tvLoadingStatus.setText("Scanning (" + current + "/" + total + "): " + currentAppName);
                }
            }

            @Override
            public void onComplete(AppPrivacySummary summary) {
                layoutLoading.setVisibility(View.GONE);
                currentSummary = summary;

                tvCountMic.setText(String.valueOf(summary.getMicCount()));
                tvCountLocation.setText(String.valueOf(summary.getLocationCount()));
                tvCountContacts.setText(String.valueOf(summary.getContactsCount()));
                tvCountSpyware.setText(String.valueOf(summary.getSpywareThreatCount()));

                applyCurrentFilter();
            }

            @Override
            public void onError(String error) {
                layoutLoading.setVisibility(View.GONE);
                Toast.makeText(PrivacyGuardActivity.this, "Audit Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyCurrentFilter() {
        if (currentSummary == null) return;

        List<AppPrivacyProfile> all = currentSummary.getProfiles();
        List<AppPrivacyProfile> filtered = new ArrayList<>();

        if (currentFilterId == R.id.chip_spyware) {
            for (AppPrivacyProfile p : all) {
                if (p.isPotentialSpyware() || p.getRiskScore() >= 70) {
                    filtered.add(p);
                }
            }
        } else if (currentFilterId == R.id.chip_mic) {
            for (AppPrivacyProfile p : all) {
                if (p.hasMicrophone()) filtered.add(p);
            }
        } else if (currentFilterId == R.id.chip_location) {
            for (AppPrivacyProfile p : all) {
                if (p.hasLocation()) filtered.add(p);
            }
        } else if (currentFilterId == R.id.chip_contacts) {
            for (AppPrivacyProfile p : all) {
                if (p.hasContacts()) filtered.add(p);
            }
        } else if (currentFilterId == R.id.chip_audio) {
            for (AppPrivacyProfile p : all) {
                if (p.hasAudioPhone()) filtered.add(p);
            }
        } else {
            filtered.addAll(all);
        }

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvApps.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvApps.setVisibility(View.VISIBLE);
            adapter.updateList(filtered);
        }
    }

    @Override
    public void onManageApp(AppPrivacyProfile profile) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", profile.getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUninstallApp(AppPrivacyProfile profile) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.fromParts("package", profile.getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot prompt uninstall: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh audit when returning from system app permission settings
        if (currentSummary != null) {
            loadPrivacyAudit();
        }
    }
}
