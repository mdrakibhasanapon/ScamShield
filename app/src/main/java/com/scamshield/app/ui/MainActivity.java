package com.scamshield.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scamshield.app.R;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.ui.education.EducationFragment;
import com.scamshield.app.ui.history.HistoryFragment;
import com.scamshield.app.ui.home.HomeFragment;
import com.scamshield.app.ui.settings.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private ActivityResultLauncher<String[]> mainPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        preferencesManager = new PreferencesManager(this);
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof HomeFragment) {
                        ((HomeFragment) current).updateSmsShieldUI();
                    }
                }
        );

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_history) {
                replaceFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_education) {
                replaceFragment(new EducationFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                replaceFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        // Load Adaptive AdMob Bottom Banner
        android.widget.FrameLayout bannerContainer = findViewById(R.id.banner_ad_container);
        if (bannerContainer != null) {
            com.scamshield.app.ads.AdMobManager.getInstance().loadAdaptiveBanner(this, bannerContainer);
        }

        // Register modern OnBackPressedCallback for exit confirmation dialog
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });

        // Prompt user to grant SMS & Notification permissions if needed
        checkAndPromptSmsPermissions();
    }

    private void checkAndPromptSmsPermissions() {
        if (!preferencesManager.isRealTimeSmsShieldEnabled()) return;

        boolean hasReceiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        if (!hasReceiveSms || !hasReadSms || !hasNotif) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sms_shield_dialog_title)
                    .setMessage(R.string.sms_shield_dialog_msg)
                    .setIcon(R.drawable.ic_shield_check)
                    .setCancelable(false)
                    .setPositiveButton(R.string.sms_permission_grant, (dialog, which) -> {
                        requestSmsPermissions();
                    })
                    .setNegativeButton(R.string.btn_dismiss, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    public void requestSmsPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_SMS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!needed.isEmpty()) {
            mainPermissionLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_exit_title)
                .setMessage(R.string.dialog_exit_message)
                .setIcon(R.drawable.ic_scamshield_logo)
                .setPositiveButton(R.string.dialog_exit_positive, (dialog, which) -> finishAffinity())
                .setNegativeButton(R.string.dialog_exit_negative, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void applySavedTheme() {
        String theme = preferencesManager.getThemeMode();
        if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
