package com.scamshield.app.ui.home;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.ScanHistoryItem;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.receiver.DownloadReceiver;
import com.scamshield.app.receiver.SmsReceiver;
import com.scamshield.app.ui.history.HistoryAdapter;
import com.scamshield.app.ui.message.CheckMessageActivity;
import com.scamshield.app.ui.qr.QrScanActivity;
import com.scamshield.app.ui.result.ResultActivity;
import com.scamshield.app.ui.screenshot.ScreenshotAnalysisActivity;
import com.scamshield.app.ui.url.CheckUrlActivity;
import com.scamshield.app.util.SmsInboxHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements HistoryAdapter.OnItemClickListener {

    private HistoryRepository historyRepository;
    private PreferencesManager preferencesManager;
    private RecyclerView rvRecentScans;
    private View layoutEmptyScans;
    private HistoryAdapter historyAdapter;
    private final List<ScanHistoryItem> recentScansList = new ArrayList<>();

    // SMS Watchdog UI
    private MaterialSwitch switchSmsShield;
    private TextView tvSmsShieldStatus;
    private View layoutPermissionWarning;
    private MaterialButton btnGrantPermissions;
    private MaterialButton btnOpenAppSettings;
    private MaterialButton btnScanInboxSms;
    private MaterialButton btnSimulateSms;
    private TextView tvSmsTroubleshoot;

    // Real-Time Download Watchdog UI
    private MaterialSwitch switchDownloadShield;
    private TextView tvDownloadShieldStatus;
    private MaterialButton btnSimulateDownload;

    // Universal Messaging Watchdog UI
    private MaterialSwitch switchChatWatchdog;
    private TextView tvChatWatchdogStatus;
    private View layoutChatPermissionWarning;
    private MaterialButton btnGrantChatPermission;
    private MaterialButton btnSimulateChat;

    // Full Phone Device Security Scan UI
    private TextView tvDeviceScanStatus;

    // Trusted Contacts Sync UI
    private TextView tvContactSyncStatus;

    // Voice Phishing (Mic) UI
    private MaterialSwitch switchMicShield;
    private TextView tvMicShieldStatus;
    private View layoutMicPermissionWarning;
    private MaterialButton btnGrantMicPermission;
    private MaterialButton btnTestMicShield;

    // Regional Threat Radar (Location) UI
    private MaterialSwitch switchLocationRadar;
    private TextView tvLocationRadarStatus;
    private View layoutLocationPermissionWarning;
    private MaterialButton btnGrantLocationPermission;
    private MaterialButton btnTestLocationRadar;

    private ActivityResultLauncher<String[]> smsPermissionLauncher;
    private ActivityResultLauncher<String> micPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    // Real-time broadcast receiver to catch incoming SMS or download updates while viewing Home
    private final BroadcastReceiver realTimeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadRecentScans();
            updateSmsShieldUI();
            updateDownloadShieldUI();
            updateChatWatchdogUI();
            updateContactSyncUI();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    updateSmsShieldUI();
                    boolean smsGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECEIVE_SMS)) ||
                                         Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS));
                    if (smsGranted) {
                        Toast.makeText(requireContext(), "SMS Watchdog is now fully active!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        micPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    updateMicShieldUI();
                    if (isGranted) {
                        Toast.makeText(requireContext(), "Microphone Voice Shield is now active!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    updateLocationRadarUI();
                    boolean locGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                                         Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (locGranted) {
                        Toast.makeText(requireContext(), "Regional Threat Radar & Location Guard is now active!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        historyRepository = new HistoryRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());

        rvRecentScans = view.findViewById(R.id.rv_recent_scans);
        layoutEmptyScans = view.findViewById(R.id.layout_empty_scans);

        rvRecentScans.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new HistoryAdapter(recentScansList, this);
        rvRecentScans.setAdapter(historyAdapter);

        // SMS Watchdog Setup
        switchSmsShield = view.findViewById(R.id.switch_sms_shield);
        tvSmsShieldStatus = view.findViewById(R.id.tv_sms_shield_status);
        layoutPermissionWarning = view.findViewById(R.id.layout_permission_warning);
        btnGrantPermissions = view.findViewById(R.id.btn_grant_permissions);
        btnOpenAppSettings = view.findViewById(R.id.btn_open_app_settings);
        btnScanInboxSms = view.findViewById(R.id.btn_scan_inbox_sms);
        btnSimulateSms = view.findViewById(R.id.btn_simulate_sms);
        tvSmsTroubleshoot = view.findViewById(R.id.tv_sms_troubleshoot);

        switchSmsShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setRealTimeSmsShieldEnabled(isChecked);
            if (isChecked) {
                checkAndRequestSmsPermissions();
            }
            updateSmsShieldUI();
        });

        btnGrantPermissions.setOnClickListener(v -> checkAndRequestSmsPermissions());
        btnOpenAppSettings.setOnClickListener(v -> openAppSettings());
        btnScanInboxSms.setOnClickListener(v -> handleScanInbox());
        btnSimulateSms.setOnClickListener(v -> showSimulationDialog());
        tvSmsTroubleshoot.setOnClickListener(v -> showTroubleshootDialog());

        // Download Watchdog Setup
        switchDownloadShield = view.findViewById(R.id.switch_download_shield);
        tvDownloadShieldStatus = view.findViewById(R.id.tv_download_shield_status);
        btnSimulateDownload = view.findViewById(R.id.btn_simulate_download);

        switchDownloadShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDownloadWatchdogEnabled(isChecked);
            updateDownloadShieldUI();
        });

        btnSimulateDownload.setOnClickListener(v -> showDownloadSimulationDialog());

        // Universal Messaging Watchdog Setup
        switchChatWatchdog = view.findViewById(R.id.switch_chat_watchdog);
        tvChatWatchdogStatus = view.findViewById(R.id.tv_chat_watchdog_status);
        layoutChatPermissionWarning = view.findViewById(R.id.layout_chat_permission_warning);
        btnGrantChatPermission = view.findViewById(R.id.btn_grant_chat_permission);
        btnSimulateChat = view.findViewById(R.id.btn_simulate_chat);

        switchChatWatchdog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setUniversalChatWatchdogEnabled(isChecked);
            if (isChecked && !com.scamshield.app.service.UniversalMessageListenerService.isNotificationAccessGranted(requireContext())) {
                com.scamshield.app.service.UniversalMessageListenerService.openNotificationAccessSettings(requireContext());
            }
            updateChatWatchdogUI();
        });

        btnGrantChatPermission.setOnClickListener(v -> {
            com.scamshield.app.service.UniversalMessageListenerService.openNotificationAccessSettings(requireContext());
        });

        btnSimulateChat.setOnClickListener(v -> showChatSimulationDialog());

        // Full Phone Device Security Scan Setup
        tvDeviceScanStatus = view.findViewById(R.id.tv_device_scan_status);
        view.findViewById(R.id.btn_start_device_scan).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.scamshield.app.ui.scan.DeviceScanActivity.class));
        });
        view.findViewById(R.id.card_device_scan).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.scamshield.app.ui.scan.DeviceScanActivity.class));
        });

        // App Data & Network Watchdog Setup
        View.OnClickListener openNetworkTraffic = v -> {
            startActivity(new Intent(requireContext(), com.scamshield.app.ui.network.NetworkTrafficActivity.class));
        };
        view.findViewById(R.id.btn_open_network_watchdog).setOnClickListener(openNetworkTraffic);
        view.findViewById(R.id.card_network_watchdog).setOnClickListener(openNetworkTraffic);

        // App Permission & Privacy Guard Setup
        View.OnClickListener openPrivacyGuard = v -> {
            startActivity(new Intent(requireContext(), com.scamshield.app.ui.privacy.PrivacyGuardActivity.class));
        };
        view.findViewById(R.id.btn_open_privacy_guard).setOnClickListener(openPrivacyGuard);
        view.findViewById(R.id.card_privacy_guard).setOnClickListener(openPrivacyGuard);

        // Trusted Contacts & Safe Sender Sync Setup
        tvContactSyncStatus = view.findViewById(R.id.tv_contact_sync_status);
        View.OnClickListener openContactSync = v -> {
            startActivity(new Intent(requireContext(), com.scamshield.app.ui.contacts.ContactSyncActivity.class));
        };
        view.findViewById(R.id.btn_open_contact_sync).setOnClickListener(openContactSync);
        view.findViewById(R.id.card_contact_sync).setOnClickListener(openContactSync);

        // Voice Phishing (Mic) Shield Setup
        switchMicShield = view.findViewById(R.id.switch_mic_shield);
        tvMicShieldStatus = view.findViewById(R.id.tv_mic_shield_status);
        layoutMicPermissionWarning = view.findViewById(R.id.layout_mic_permission_warning);
        btnGrantMicPermission = view.findViewById(R.id.btn_grant_mic_permission);
        btnTestMicShield = view.findViewById(R.id.btn_test_mic_shield);

        switchMicShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setMicShieldEnabled(isChecked);
            if (isChecked && !com.scamshield.app.util.PermissionHelper.hasMicPermission(requireContext())) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
            updateMicShieldUI();
        });
        btnGrantMicPermission.setOnClickListener(v -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO));
        btnTestMicShield.setOnClickListener(v -> showVoiceScamSimulationDialog());

        // Regional Threat Radar (Location) Setup
        switchLocationRadar = view.findViewById(R.id.switch_location_radar);
        tvLocationRadarStatus = view.findViewById(R.id.tv_location_radar_status);
        layoutLocationPermissionWarning = view.findViewById(R.id.layout_location_permission_warning);
        btnGrantLocationPermission = view.findViewById(R.id.btn_grant_location_permission);
        btnTestLocationRadar = view.findViewById(R.id.btn_test_location_radar);

        switchLocationRadar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setLocationRadarEnabled(isChecked);
            if (isChecked && !com.scamshield.app.util.PermissionHelper.hasLocationPermission(requireContext())) {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
            updateLocationRadarUI();
        });
        btnGrantLocationPermission.setOnClickListener(v -> locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }));
        btnTestLocationRadar.setOnClickListener(v -> showLocationRadarDialog());

        // 🚨 Emergency Panic Lockdown Button & Card
        View cardPanic = view.findViewById(R.id.card_panic_lockdown);
        if (cardPanic != null) {
            cardPanic.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.scamshield.app.ui.emergency.EmergencyLockdownActivity.class));
            });
        }
        View btnTriggerPanic = view.findViewById(R.id.btn_trigger_panic);
        if (btnTriggerPanic != null) {
            btnTriggerPanic.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.scamshield.app.ui.emergency.EmergencyLockdownActivity.class));
            });
        }

        // 📊 Weekly Security Health Score Card
        View cardHealth = view.findViewById(R.id.card_security_health);
        if (cardHealth != null) {
            updateHealthScoreUI(view);
            cardHealth.setOnClickListener(v -> showSecurityHealthDialog());
        }

        // 💰 Financial Fraud Shield Setup
        MaterialSwitch switchFinancial = view.findViewById(R.id.switch_financial_shield);
        if (switchFinancial != null) {
            switchFinancial.setChecked(preferencesManager.isFinancialShieldEnabled());
            switchFinancial.setOnCheckedChangeListener((btn, isChecked) -> {
                preferencesManager.setFinancialShieldEnabled(isChecked);
            });
        }
        View btnTestFinancial = view.findViewById(R.id.btn_test_financial_shield);
        if (btnTestFinancial != null) {
            btnTestFinancial.setOnClickListener(v -> showFinancialFraudSimulationDialog());
        }

        // Action Buttons Setup
        view.findViewById(R.id.card_action_message).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CheckMessageActivity.class));
        });

        view.findViewById(R.id.card_action_link).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CheckUrlActivity.class));
        });

        view.findViewById(R.id.card_action_screenshot).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ScreenshotAnalysisActivity.class));
        });

        view.findViewById(R.id.card_action_qr).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), QrScanActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentScans();
        updateSmsShieldUI();
        updateDownloadShieldUI();
        updateChatWatchdogUI();
        updateDeviceScanStatusUI();
        updateContactSyncUI();
        updateMicShieldUI();
        updateLocationRadarUI();

        // Register receiver for real-time background SMS and download updates
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(SmsReceiver.ACTION_SMS_PROCESSED);
            filter.addAction(DownloadReceiver.ACTION_DOWNLOAD_SCANNED);
            filter.addAction(com.scamshield.app.service.UniversalMessageListenerService.ACTION_CHAT_SCANNED);
            filter.addAction(com.scamshield.app.util.ContactSyncHelper.ACTION_CONTACTS_SYNCED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(realTimeUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                requireContext().registerReceiver(realTimeUpdateReceiver, filter);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(realTimeUpdateReceiver);
        } catch (Exception ignored) {}
    }

    public void updateSmsShieldUI() {
        if (!isAdded()) return;

        boolean isEnabled = preferencesManager.isRealTimeSmsShieldEnabled();
        switchSmsShield.setChecked(isEnabled);

        boolean hasReceiveSms = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadSms = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        boolean allGranted = hasReceiveSms && hasReadSms && hasNotif;

        if (!isEnabled) {
            tvSmsShieldStatus.setText(R.string.sms_shield_status_inactive);
            tvSmsShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary_light));
            layoutPermissionWarning.setVisibility(View.GONE);
        } else if (!allGranted) {
            tvSmsShieldStatus.setText(R.string.sms_shield_status_permission_needed);
            tvSmsShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_suspicious));
            layoutPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            tvSmsShieldStatus.setText(R.string.sms_shield_status_active);
            tvSmsShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
            layoutPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void updateDownloadShieldUI() {
        if (!isAdded() || tvDownloadShieldStatus == null) return;
        boolean isEnabled = preferencesManager.isDownloadWatchdogEnabled();
        switchDownloadShield.setChecked(isEnabled);
        if (isEnabled) {
            tvDownloadShieldStatus.setText(R.string.download_shield_status_active);
            tvDownloadShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
        } else {
            tvDownloadShieldStatus.setText(R.string.download_shield_status_inactive);
            tvDownloadShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary_light));
        }
    }

    private void updateChatWatchdogUI() {
        if (!isAdded() || tvChatWatchdogStatus == null) return;

        boolean isEnabled = preferencesManager.isUniversalChatWatchdogEnabled();
        switchChatWatchdog.setChecked(isEnabled);

        boolean hasAccess = com.scamshield.app.service.UniversalMessageListenerService.isNotificationAccessGranted(requireContext());

        if (!isEnabled) {
            tvChatWatchdogStatus.setText(R.string.chat_watchdog_status_inactive);
            tvChatWatchdogStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary_light));
            layoutChatPermissionWarning.setVisibility(View.GONE);
        } else if (!hasAccess) {
            tvChatWatchdogStatus.setText(R.string.chat_watchdog_status_permission_needed);
            tvChatWatchdogStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_suspicious));
            layoutChatPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            tvChatWatchdogStatus.setText(R.string.chat_watchdog_status_active);
            tvChatWatchdogStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
            layoutChatPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void updateDeviceScanStatusUI() {
        if (!isAdded() || tvDeviceScanStatus == null) return;
        int lastScore = preferencesManager.getLastDeviceScore();
        int threatCount = preferencesManager.getLastDeviceThreatCount();
        if (lastScore == -1) {
            tvDeviceScanStatus.setText(R.string.home_device_scan_status_never);
            tvDeviceScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
        } else if (threatCount == 0) {
            tvDeviceScanStatus.setText(getString(R.string.home_device_scan_status_clean, lastScore));
            tvDeviceScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
        } else {
            tvDeviceScanStatus.setText(getString(R.string.home_device_scan_status_threats, threatCount, lastScore));
            tvDeviceScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_high));
        }
    }

    private void updateContactSyncUI() {
        if (!isAdded() || tvContactSyncStatus == null) return;
        int count = preferencesManager.getSyncedContactsCount();
        if (count > 0) {
            tvContactSyncStatus.setText(getString(R.string.home_contact_sync_status_synced, count));
            tvContactSyncStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
        } else {
            tvContactSyncStatus.setText(R.string.home_contact_sync_status_none);
            tvContactSyncStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
        }
    }

    private void updateMicShieldUI() {
        if (!isAdded() || tvMicShieldStatus == null) return;
        boolean isEnabled = preferencesManager.isMicShieldEnabled();
        switchMicShield.setChecked(isEnabled);

        boolean hasPerm = com.scamshield.app.util.PermissionHelper.hasMicPermission(requireContext());
        if (!isEnabled) {
            tvMicShieldStatus.setText(R.string.mic_shield_status_inactive);
            tvMicShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary_light));
            layoutMicPermissionWarning.setVisibility(View.GONE);
        } else if (!hasPerm) {
            tvMicShieldStatus.setText(R.string.mic_shield_status_permission_needed);
            tvMicShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_suspicious));
            layoutMicPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            tvMicShieldStatus.setText(R.string.mic_shield_status_active);
            tvMicShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
            layoutMicPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void updateLocationRadarUI() {
        if (!isAdded() || tvLocationRadarStatus == null) return;
        boolean isEnabled = preferencesManager.isLocationRadarEnabled();
        switchLocationRadar.setChecked(isEnabled);

        boolean hasPerm = com.scamshield.app.util.PermissionHelper.hasLocationPermission(requireContext());
        if (!isEnabled) {
            tvLocationRadarStatus.setText(R.string.location_radar_status_inactive);
            tvLocationRadarStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary_light));
            layoutLocationPermissionWarning.setVisibility(View.GONE);
        } else if (!hasPerm) {
            tvLocationRadarStatus.setText(R.string.location_radar_status_permission_needed);
            tvLocationRadarStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_suspicious));
            layoutLocationPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            tvLocationRadarStatus.setText(R.string.location_radar_status_active);
            tvLocationRadarStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
            layoutLocationPermissionWarning.setVisibility(View.GONE);
        }
    }

    public void checkAndRequestSmsPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            smsPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Open Settings > Apps > ScamShield > Permissions", Toast.LENGTH_LONG).show();
        }
    }

    private void handleScanInbox() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Grant SMS permission to scan device inbox", Toast.LENGTH_SHORT).show();
            checkAndRequestSmsPermissions();
            return;
        }

        List<ScamAnalysisResult> scanned = SmsInboxHelper.scanRecentInbox(requireContext(), 15);
        if (scanned.isEmpty()) {
            Toast.makeText(requireContext(), "No SMS messages found in device inbox, or permission needed.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Scanned " + scanned.size() + " inbox messages! Scores updated.", Toast.LENGTH_LONG).show();
            loadRecentScans();
        }
    }

    private void showTroubleshootDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.troubleshoot_dialog_title)
                .setMessage(R.string.troubleshoot_dialog_content)
                .setIcon(R.drawable.ic_shield_check)
                .setPositiveButton("Open App Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Got It", null)
                .show();
    }

    private void showSimulationDialog() {
        String[] scenarios = {
                "🚨 Bank Phishing SMS (Score: 85 - Likely Scam)",
                "📦 USPS Redelivery Fee Phish (Score: 75 - Suspicious)",
                "🎁 Bitcoin Airdrop Gas Fee (Score: 80 - High Risk)",
                "✓ Clean SMS from Colleague (Score: 0 - Low Risk)"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Simulate Incoming SMS")
                .setItems(scenarios, (dialog, which) -> {
                    String sender;
                    String messageBody;

                    switch (which) {
                        case 0:
                            sender = "BANK-ALERT";
                            messageBody = "Urgent: Your card has been temporarily locked due to suspicious activity. Verify credentials at http://bank-update-security.icu immediately.";
                            break;
                        case 1:
                            sender = "+1 (800) 275-8777";
                            messageBody = "USPS: Your package could not be delivered due to unpaid surcharge ($1.85). Update details immediately at https://bit.ly/usps-fee-pay";
                            break;
                        case 2:
                            sender = "CRYPTO-AIRDROP";
                            messageBody = "Congratulations! You have been selected to receive 1.25 BTC. Deposit 0.05 BTC gas fee to unlock your payout wallet.";
                            break;
                        default:
                            sender = "+1 (555) 019-2831";
                            messageBody = "Hey Alex, are we still meeting for lunch tomorrow around 1:00 PM at the bistro?";
                            break;
                    }

                    // Process through SmsReceiver
                    ScamAnalysisResult result = SmsReceiver.processIncomingSms(requireContext(), sender, messageBody);

                    Toast.makeText(requireContext(), "Incoming SMS simulated and scored! Check notifications.", Toast.LENGTH_LONG).show();
                    loadRecentScans();

                    // Offer to view assessment right away
                    if (result != null) {
                        Intent intent = new Intent(requireContext(), ResultActivity.class);
                        intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChatSimulationDialog() {
        String[] scenarios = {
                "🚨 WhatsApp: Urgent Bank KYC & OTP Phishing (Score: 85/100)",
                "💰 Telegram: 2.5 ETH VIP Airdrop Scam (Score: 80/100)",
                "👤 Messenger: Friend Impersonation & Urgent Wire Scam (Score: 75/100)",
                "✓ WhatsApp: Clean Lunch Message from Friend (Score: 5/100)"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Test Incoming Chat Scam Alert")
                .setItems(scenarios, (dialog, which) -> {
                    String appName;
                    String sender;
                    String text;

                    switch (which) {
                        case 0:
                            appName = "WhatsApp";
                            sender = "Chase Fraud Security (+1 800-555-0199)";
                            text = "URGENT: Your Chase checking debit card is temporarily blocked due to unauthorized login attempts. To prevent permanent closure, verify your 4-digit ATM PIN and SSN immediately at http://chase-security-verify.top/login";
                            break;
                        case 1:
                            appName = "Telegram";
                            sender = "Binance VIP Airdrop Bot";
                            text = "Congratulations! You have received 2.5 ETH in your VIP wallet. Send 0.08 ETH gas fee to claim before timer expires: https://claim-crypto-giveaway.xyz";
                            break;
                        case 2:
                            appName = "Messenger";
                            sender = "David Miller";
                            text = "Hey man! I'm stuck at Heathrow Airport and lost my wallet. Can you urgently send me $400 via Apple Gift Cards or Wire? I will pay you back double tomorrow morning please hurry!";
                            break;
                        default:
                            appName = "WhatsApp";
                            sender = "Sarah";
                            text = "Hey! Are we still meeting for lunch at 1 PM today? Let me know if that works for you.";
                            break;
                    }

                    com.scamshield.app.service.UniversalMessageListenerService.simulateIncomingChatScan(requireContext(), appName, sender, text);
                    Toast.makeText(requireContext(), "Simulated " + appName + " chat alert triggered! Check notifications.", Toast.LENGTH_LONG).show();
                    loadRecentScans();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDownloadSimulationDialog() {
        String[] downloadScenarios = {
                "🚨 Disguised Malware (invoice_payment.pdf.apk - Score: 95/100)",
                "⚠️ Sideloaded Banking APK (chase_update.apk - Score: 85/100)",
                "🔍 Executable Script File (device_root.sh - Score: 75/100)",
                "✓ Clean PDF Document (quarterly_statement.pdf - Score: 0/100)"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Simulate Download & Notification Alert")
                .setItems(downloadScenarios, (dialog, which) -> {
                    String fileName;
                    String fakePath;
                    String originUrl;

                    switch (which) {
                        case 0:
                            fileName = "invoice_payment.pdf.apk";
                            fakePath = "/sdcard/Download/invoice_payment.pdf.apk";
                            originUrl = "http://secure-billing-update.icu/dl/invoice_payment.pdf.apk";
                            break;
                        case 1:
                            fileName = "chase_security_update.apk";
                            fakePath = "/sdcard/Download/chase_security_update.apk";
                            originUrl = "http://chase-security-verify.top/chase_security_update.apk";
                            break;
                        case 2:
                            fileName = "device_root.sh";
                            fakePath = "/sdcard/Download/device_root.sh";
                            originUrl = "https://unverified-scripts.xyz/scripts/device_root.sh";
                            break;
                        default:
                            fileName = "quarterly_statement.pdf";
                            fakePath = "/sdcard/Download/quarterly_statement.pdf";
                            originUrl = "https://chase.com/statements/quarterly_statement.pdf";
                            break;
                    }

                    ScamAnalysisResult result = DownloadReceiver.processDownload(requireContext(), fileName, fakePath, originUrl);

                    Toast.makeText(requireContext(), "Download scan alert sent! Check your notification bar.", Toast.LENGTH_LONG).show();
                    loadRecentScans();

                    if (result != null) {
                        Intent intent = new Intent(requireContext(), ResultActivity.class);
                        intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showVoiceScamSimulationDialog() {
        String[] voiceScenarios = {
                "🚨 Bank Impersonation Vishing (\"Calling from fraud team, read me your 6-digit OTP\")",
                "⚠️ Arrest Warrant Phone Extortion (\"Police arriving in 30 mins, transfer all funds\")",
                "✓ Safe Family Conversation (\"Hey, calling to ask about dinner plans tonight\")"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Test Voice Scam Detection (Mic Shield)")
                .setItems(voiceScenarios, (dialog, which) -> {
                    String transcript;
                    switch (which) {
                        case 0:
                            transcript = "This is officer John calling from your bank fraud department. Do not hang up the call. Your debit card is compromised. Read me your 6-digit verification OTP immediately to transfer funds to a temporary safe account.";
                            break;
                        case 1:
                            transcript = "An urgent arrest warrant has been issued by the IRS officer. Police will arrive at your home unless you make an immediate gift card payment to clear your debt.";
                            break;
                        default:
                            transcript = "Hey, just calling to see how you're doing and if we are still meeting up for dinner tonight!";
                            break;
                    }

                    ScamAnalysisResult result = com.scamshield.app.domain.scanner.VoiceScamDetector.analyzeVoiceTranscript(transcript);
                    historyRepository.saveScan(result);
                    loadRecentScans();

                    Intent intent = new Intent(requireContext(), ResultActivity.class);
                    intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLocationRadarDialog() {
        boolean hasPerm = com.scamshield.app.util.PermissionHelper.hasLocationPermission(requireContext());
        if (!hasPerm) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        String locationSummary = com.scamshield.app.domain.scanner.LocationScamRadar.getCoarseLocationSummary(requireContext());
        boolean isUnsecuredWifi = com.scamshield.app.domain.scanner.LocationScamRadar.isConnectedToPublicUnsecuredWifi(requireContext());

        String message = locationSummary + "\n\n" +
                "• Regional SMS & Toll Scam Filter: Active\n" +
                "• Rogue Cell Tower & Fake BTS Guard: Active\n" +
                "• Wi-Fi Security Status: " + (isUnsecuredWifi ? "⚠️ Unsecured Public Wi-Fi Detected" : "✓ Secure / Cellular Protected");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Regional Threat Radar")
                .setMessage(message)
                .setIcon(R.drawable.ic_location)
                .setPositiveButton("Close", null)
                .show();
    }

    private void updateHealthScoreUI(View view) {
        if (view == null || getContext() == null) return;
        com.scamshield.app.domain.scanner.SecurityHealthAuditor.HealthReport report =
                com.scamshield.app.domain.scanner.SecurityHealthAuditor.generateWeeklyHealthReport(requireContext());

        TextView tvBadge = view.findViewById(R.id.tv_health_grade_badge);
        TextView tvTitle = view.findViewById(R.id.tv_health_status_title);
        TextView tvSub = view.findViewById(R.id.tv_health_status_sub);

        if (tvBadge != null) tvBadge.setText(report.getGrade());
        if (tvTitle != null) tvTitle.setText(report.getStatusTitle());
        if (tvSub != null) tvSub.setText(report.getScorePercentage() + "% Device Safety Score • " + report.getActiveShields().size() + " active defenses");
    }

    private void showSecurityHealthDialog() {
        if (getContext() == null) return;
        com.scamshield.app.domain.scanner.SecurityHealthAuditor.HealthReport report =
                com.scamshield.app.domain.scanner.SecurityHealthAuditor.generateWeeklyHealthReport(requireContext());

        StringBuilder sb = new StringBuilder();
        sb.append("Weekly Security Health Score: ").append(report.getScorePercentage()).append("% (Grade ").append(report.getGrade()).append(")\n\n");
        sb.append("ACTIVE DEFENSE MODULES (").append(report.getActiveShields().size()).append("):\n");
        for (String shield : report.getActiveShields()) {
            sb.append("• ").append(shield).append("\n");
        }
        if (!report.getRecommendations().isEmpty()) {
            sb.append("\nRECOMMENDED ACTIONS:\n");
            for (String rec : report.getRecommendations()) {
                sb.append("⚠️ ").append(rec).append("\n");
            }
        } else {
            sb.append("\n🎉 All threat sensors and defensive shields are operating at peak efficiency!");
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📊 Security Health & Threat Audit")
                .setMessage(sb.toString())
                .setIcon(R.drawable.ic_shield_check)
                .setPositiveButton("Done", null)
                .show();
    }

    private void showFinancialFraudSimulationDialog() {
        if (getContext() == null) return;
        String[] samples = {
                "🚨 Fake 'Mistakenly Sent 10,000 Tk' bKash SMS",
                "🚨 OTP Verification PIN Theft Attempt",
                "✅ Legitimate Personal bKash Transfer"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("💰 Simulate Mobile Financial Scam")
                .setItems(samples, (dialog, which) -> {
                    String sampleText;
                    if (which == 0) {
                        sampleText = "You have received Tk 10,000.00 from 01712345678. Ref: Gift. Fee Tk 0.00. Balance Tk 10,050.00. TrxID 9K7A2BC1D8. ভাই ভুল করে ১০,০০০ টাকা চলে গেছে দয়া করে এখনই টাকা ফেরত দিন নাহলে পুলিশে দেব।";
                    } else if (which == 1) {
                        sampleText = "bKash: Your secret verification OTP is 492018. বিকাশ সিকিউরিটি টিম: আপনার একাউন্ট সাময়িক ব্লক করা হয়েছে, ভেরিফিকেশন পিন কোড ও ওটিপি এখনই এই নম্বরে মেসেজ করুন।";
                    } else {
                        sampleText = "You have received Tk 500.00 from 01987654321. Ref: Lunch. Fee Tk 0.00. Balance Tk 550.00. TrxID 8M2N5P9Q1Z.";
                    }

                    com.scamshield.app.data.model.ScamAnalysisResult result =
                            com.scamshield.app.domain.detector.FinancialFraudDetector.analyzeFinancialText(sampleText);

                    Intent intent = new Intent(requireContext(), com.scamshield.app.ui.result.ResultActivity.class);
                    intent.putExtra(com.scamshield.app.ui.result.ResultActivity.EXTRA_RESULT, result);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadRecentScans() {
        recentScansList.clear();
        List<ScanHistoryItem> items = historyRepository.getRecentScans(10);
        if (items.isEmpty()) {
            layoutEmptyScans.setVisibility(View.VISIBLE);
            rvRecentScans.setVisibility(View.GONE);
        } else {
            layoutEmptyScans.setVisibility(View.GONE);
            rvRecentScans.setVisibility(View.VISIBLE);
            recentScansList.addAll(items);
            historyAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(ScanHistoryItem item) {
        Intent intent = new Intent(requireContext(), CheckMessageActivity.class);
        intent.putExtra("PREFILL_CONTENT", item.getSnippet());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(ScanHistoryItem item, int position) {
        historyRepository.deleteScan(item.getId());
        loadRecentScans();
    }
}
