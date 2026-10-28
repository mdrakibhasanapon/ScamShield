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

    // Full Phone Device Security Scan UI
    private TextView tvDeviceScanStatus;

    private ActivityResultLauncher<String[]> smsPermissionLauncher;

    // Real-time broadcast receiver to catch incoming SMS or download updates while viewing Home
    private final BroadcastReceiver realTimeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadRecentScans();
            updateSmsShieldUI();
            updateDownloadShieldUI();
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
        updateDeviceScanStatusUI();

        // Register receiver for real-time background SMS and download updates
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(SmsReceiver.ACTION_SMS_PROCESSED);
            filter.addAction(DownloadReceiver.ACTION_DOWNLOAD_SCANNED);
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
