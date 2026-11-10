package com.scamshield.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

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
import com.scamshield.app.util.ContactSyncHelper;
import com.scamshield.app.util.PermissionHelper;
import com.scamshield.app.util.PrivacyConsentHelper;

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
                    // If contact permission was granted, sync contacts immediately
                    if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_CONTACTS))) {
                        new Thread(() -> ContactSyncHelper.syncContacts(MainActivity.this)).start();
                    }

                    // Refresh all shield cards in HomeFragment immediately
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof HomeFragment) {
                        ((HomeFragment) current).refreshAllShieldsUI();
                    }

                    // Check if Notification Listener Access is needed for Universal Chat Watchdog
                    if (!PermissionHelper.hasNotificationListenerAccess(MainActivity.this)) {
                        promptNotificationListenerAccess();
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

        // First-launch Prominent Disclosure & Privacy Consent Dialog (Google Play Compliant)
        PrivacyConsentHelper.checkAndShowConsentDialog(this, preferencesManager, () -> {
            checkAndPromptAllInitialPermissions();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.scamshield.app.util.ClipboardGuardHelper.checkClipboardOnResume(this);
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof HomeFragment) {
            ((HomeFragment) current).refreshAllShieldsUI();
        }
    }

    /**
     * Gathers all core permissions (SMS, Mic, Location, Contacts, Notifications)
     * and requests them in a single, unified upfront prompt on initial launch.
     */
    private void checkAndPromptAllInitialPermissions() {
        List<String> needed = new ArrayList<>();

        if (!PermissionHelper.hasSmsPermissions(this)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.RECEIVE_SMS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_SMS);
            }
        }

        if (!PermissionHelper.hasMicPermission(this)) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!PermissionHelper.hasLocationPermission(this)) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!PermissionHelper.hasContactsPermission(this)) {
            needed.add(Manifest.permission.READ_CONTACTS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!needed.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.initial_setup_dialog_title)
                    .setMessage(R.string.initial_setup_dialog_msg)
                    .setIcon(R.drawable.ic_shield_check)
                    .setCancelable(false)
                    .setPositiveButton(R.string.initial_setup_dialog_grant, (dialog, which) -> {
                        mainPermissionLauncher.launch(needed.toArray(new String[0]));
                    })
                    .setNegativeButton(R.string.btn_dismiss, (dialog, which) -> {
                        dialog.dismiss();
                        if (!PermissionHelper.hasNotificationListenerAccess(MainActivity.this)) {
                            promptNotificationListenerAccess();
                        }
                    })
                    .show();
        } else {
            if (!PermissionHelper.hasNotificationListenerAccess(this)) {
                promptNotificationListenerAccess();
            }
        }
    }

    private void promptNotificationListenerAccess() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.initial_chat_watchdog_title)
                .setMessage(R.string.initial_chat_watchdog_msg)
                .setIcon(R.drawable.ic_shield_check)
                .setCancelable(false)
                .setPositiveButton(R.string.initial_chat_watchdog_btn, (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception ignored) {}
                })
                .setNegativeButton(R.string.btn_dismiss, (dialog, which) -> dialog.dismiss())
                .show();
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
