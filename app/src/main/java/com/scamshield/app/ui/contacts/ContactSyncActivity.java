package com.scamshield.app.ui.contacts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.scamshield.app.R;
import com.scamshield.app.data.local.AppDatabaseHelper;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.TrustedContact;
import com.scamshield.app.util.ContactSyncHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactSyncActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private PreferencesManager prefs;
    private TrustedContactAdapter adapter;
    private RecyclerView rvContacts;
    private View layoutEmpty;
    private TextView tvStatTotal;
    private TextView tvStatSafe;
    private TextView tvStatToll;
    private TextView tvLastSyncInfo;
    private MaterialButton btnSyncNow;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;

    private List<TrustedContact> allContacts = new ArrayList<>();
    private int currentFilterId = R.id.chip_all_contacts;
    private String currentSearchQuery = "";

    private ActivityResultLauncher<String> contactPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_sync);

        dbHelper = AppDatabaseHelper.getInstance(this);
        prefs = new PreferencesManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvContacts = findViewById(R.id.rv_contacts);
        layoutEmpty = findViewById(R.id.layout_empty_contacts);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatSafe = findViewById(R.id.tv_stat_safe);
        tvStatToll = findViewById(R.id.tv_stat_toll);
        tvLastSyncInfo = findViewById(R.id.tv_last_sync_info);
        btnSyncNow = findViewById(R.id.btn_sync_contacts_now);
        etSearch = findViewById(R.id.et_search_contacts);
        chipGroupFilters = findViewById(R.id.chip_group_contact_filters);

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrustedContactAdapter();
        rvContacts.setAdapter(adapter);

        contactPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        performContactSync();
                    } else {
                        Toast.makeText(this, "Contact permission needed to sync safe senders.", Toast.LENGTH_SHORT).show();
                        showPermissionFallbackDialog();
                    }
                }
        );

        btnSyncNow.setOnClickListener(v -> checkPermissionAndSync());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                filterContacts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
            } else {
                currentFilterId = R.id.chip_all_contacts;
            }
            filterContacts();
        });

        loadStoredContacts();
    }

    private void checkPermissionAndSync() {
        if (ContactSyncHelper.hasContactPermission(this)) {
            performContactSync();
        } else {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void performContactSync() {
        btnSyncNow.setEnabled(false);
        btnSyncNow.setText("Syncing Contacts…");

        new Thread(() -> {
            int count = ContactSyncHelper.syncContacts(this);
            runOnUiThread(() -> {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setText(R.string.btn_sync_contacts_now);

                if (count > 0) {
                    Toast.makeText(this, "Successfully synced " + count + " contacts on-device!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No new contacts found or empty phonebook.", Toast.LENGTH_SHORT).show();
                }
                loadStoredContacts();
            });
        }).start();
    }

    private void showPermissionFallbackDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Load Safe Sample Contacts?")
                .setMessage("Since contact permission was not granted, would you like to populate verified sample contacts to test the Safe Sender Shield?")
                .setPositiveButton("Load Samples", (dialog, which) -> {
                    ContactSyncHelper.populateSampleContactsIfEmpty(this);
                    loadStoredContacts();
                    Toast.makeText(this, "Sample trusted contacts loaded!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadStoredContacts() {
        allContacts = dbHelper.getAllTrustedContacts();

        int total = allContacts.size();
        int tollCount = 0;
        for (TrustedContact c : allContacts) {
            if (c.isSuspicious()) tollCount++;
        }
        int safeCount = total - tollCount;

        tvStatTotal.setText(String.valueOf(total));
        tvStatSafe.setText(String.valueOf(safeCount));
        tvStatToll.setText(String.valueOf(tollCount));

        long lastSync = prefs.getLastContactSyncTime();
        if (lastSync > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            tvLastSyncInfo.setText("Last Synced: " + sdf.format(new Date(lastSync)) + " • 🔒 100% On-Device");
        } else {
            tvLastSyncInfo.setText("🔒 100% On-Device Private Sync • Zero Contacts Uploaded");
        }

        filterContacts();
    }

    private void filterContacts() {
        List<TrustedContact> filtered = new ArrayList<>();

        for (TrustedContact contact : allContacts) {
            // Apply chip filter
            if (currentFilterId == R.id.chip_safe_contacts && contact.isSuspicious()) {
                continue;
            }
            if (currentFilterId == R.id.chip_suspicious_contacts && !contact.isSuspicious()) {
                continue;
            }

            // Apply search query
            if (!currentSearchQuery.isEmpty()) {
                String q = currentSearchQuery.toLowerCase(Locale.ROOT);
                boolean matchesName = contact.getName().toLowerCase(Locale.ROOT).contains(q);
                boolean matchesPhone = contact.getPhoneNumber().contains(q) || contact.getNormalizedNumber().contains(q);
                if (!matchesName && !matchesPhone) {
                    continue;
                }
            }

            filtered.add(contact);
        }

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvContacts.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvContacts.setVisibility(View.VISIBLE);
            adapter.updateList(filtered);
        }
    }
}
