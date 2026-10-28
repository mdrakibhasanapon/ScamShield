package com.scamshield.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.scamshield.app.R;
import com.scamshield.app.data.local.AppDatabaseHelper;
import com.scamshield.app.data.local.PreferencesManager;

public class SettingsFragment extends Fragment {

    private PreferencesManager preferencesManager;
    private RadioGroup rgTheme;
    private MaterialSwitch switchSaveHistory;
    private MaterialSwitch switchOfflineOnly;
    private MaterialSwitch switchSmsShield;
    private MaterialSwitch switchNotifyAllSms;
    private MaterialSwitch switchDownloadShield;
    private MaterialSwitch switchNotifyCleanDownloads;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        preferencesManager = new PreferencesManager(requireContext());

        // Theme Setup
        rgTheme = view.findViewById(R.id.rg_theme);
        RadioButton rbSystem = view.findViewById(R.id.rb_theme_system);
        RadioButton rbLight = view.findViewById(R.id.rb_theme_light);
        RadioButton rbDark = view.findViewById(R.id.rb_theme_dark);

        String currentTheme = preferencesManager.getThemeMode();
        if ("light".equals(currentTheme)) {
            rbLight.setChecked(true);
        } else if ("dark".equals(currentTheme)) {
            rbDark.setChecked(true);
        } else {
            rbSystem.setChecked(true);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_theme_light) {
                preferencesManager.setThemeMode("light");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.rb_theme_dark) {
                preferencesManager.setThemeMode("dark");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                preferencesManager.setThemeMode("system");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });

        // Switches
        switchSaveHistory = view.findViewById(R.id.switch_save_history);
        switchSaveHistory.setChecked(preferencesManager.isSaveHistoryEnabled());
        switchSaveHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setSaveHistoryEnabled(isChecked);
        });

        switchOfflineOnly = view.findViewById(R.id.switch_offline_only);
        switchOfflineOnly.setChecked(preferencesManager.isOfflineOnlyEnabled());
        switchOfflineOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setOfflineOnlyEnabled(isChecked);
        });

        // Real-Time Incoming SMS Shield Switches
        switchSmsShield = view.findViewById(R.id.switch_sms_shield_settings);
        switchSmsShield.setChecked(preferencesManager.isRealTimeSmsShieldEnabled());
        switchSmsShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setRealTimeSmsShieldEnabled(isChecked);
        });

        switchNotifyAllSms = view.findViewById(R.id.switch_notify_all_sms);
        switchNotifyAllSms.setChecked(preferencesManager.isNotifyAllSmsEnabled());
        switchNotifyAllSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setNotifyAllSmsEnabled(isChecked);
        });

        // Real-Time Download Watchdog Switches
        switchDownloadShield = view.findViewById(R.id.switch_download_shield_settings);
        switchDownloadShield.setChecked(preferencesManager.isDownloadWatchdogEnabled());
        switchDownloadShield.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDownloadWatchdogEnabled(isChecked);
        });

        switchNotifyCleanDownloads = view.findViewById(R.id.switch_notify_clean_downloads);
        switchNotifyCleanDownloads.setChecked(preferencesManager.isNotifyCleanDownloadsEnabled());
        switchNotifyCleanDownloads.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setNotifyCleanDownloadsEnabled(isChecked);
        });

        // Delete All Data Button
        view.findViewById(R.id.btn_delete_all_data).setOnClickListener(v -> showDeleteDataConfirmation());

        // Privacy Explainer Dialog
        view.findViewById(R.id.tv_what_happens_data).setOnClickListener(v -> showPrivacyExplainerDialog());

        view.findViewById(R.id.tv_privacy_policy).setOnClickListener(v -> {
            showSimpleDialog("Privacy Policy", "ScamShield never accesses private messages, contacts, or call logs without intentional submission. No message bodies are logged or retained on servers. Data processing is local-first.");
        });

        view.findViewById(R.id.tv_terms).setOnClickListener(v -> {
            showSimpleDialog("Terms of Service", "ScamShield provides security risk assessments for informational assistance only. It cannot guarantee 100% detection of all cyber threats. Users must remain vigilant.");
        });

        return view;
    }

    private void showDeleteDataConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Local Data?")
                .setMessage("This will permanently erase all scan history, user feedback, and cached preferences on this device. This cannot be undone.")
                .setPositiveButton("Erase Everything", (dialog, which) -> {
                    AppDatabaseHelper.getInstance(requireContext()).deleteAllScans();
                    preferencesManager.clearAllPreferences();
                    switchSaveHistory.setChecked(true);
                    switchOfflineOnly.setChecked(false);
                    Toast.makeText(requireContext(), "All local data permanently deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPrivacyExplainerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.privacy_dialog_title)
                .setMessage(R.string.privacy_dialog_content)
                .setPositiveButton("I Understand", null)
                .show();
    }

    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }
}
