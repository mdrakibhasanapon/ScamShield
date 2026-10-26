package com.scamshield.app.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.ScanHistoryItem;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.receiver.SmsReceiver;
import com.scamshield.app.ui.history.HistoryAdapter;
import com.scamshield.app.ui.message.CheckMessageActivity;
import com.scamshield.app.ui.qr.QrScanActivity;
import com.scamshield.app.ui.result.ResultActivity;
import com.scamshield.app.ui.screenshot.ScreenshotAnalysisActivity;
import com.scamshield.app.ui.url.CheckUrlActivity;

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
    private MaterialButton btnSimulateSms;

    private ActivityResultLauncher<String[]> smsPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    updateSmsShieldUI();
                    Boolean smsGranted = result.get(Manifest.permission.RECEIVE_SMS);
                    if (smsGranted != null && smsGranted) {
                        Toast.makeText(requireContext(), "Real-Time SMS Watchdog activated!", Toast.LENGTH_SHORT).show();
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
        btnSimulateSms = view.findViewById(R.id.btn_simulate_sms);

        switchSmsShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setRealTimeSmsShieldEnabled(isChecked);
            if (isChecked) {
                checkAndRequestSmsPermissions();
            }
            updateSmsShieldUI();
        });

        btnSimulateSms.setOnClickListener(v -> showSimulationDialog());

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
    }

    private void updateSmsShieldUI() {
        if (!isAdded()) return;

        boolean isEnabled = preferencesManager.isRealTimeSmsShieldEnabled();
        switchSmsShield.setChecked(isEnabled);

        boolean hasSmsPerm = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;

        if (!isEnabled) {
            tvSmsShieldStatus.setText(R.string.sms_shield_status_inactive);
            tvSmsShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary_light));
        } else if (!hasSmsPerm) {
            tvSmsShieldStatus.setText(R.string.sms_shield_status_permission_needed);
            tvSmsShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_suspicious));
        } else {
            tvSmsShieldStatus.setText(R.string.sms_shield_status_active);
            tvSmsShieldStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_low));
        }
    }

    private void checkAndRequestSmsPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
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

    private void loadRecentScans() {
        recentScansList.clear();
        List<ScanHistoryItem> items = historyRepository.getRecentScans(5);
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
