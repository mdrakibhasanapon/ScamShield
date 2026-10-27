package com.scamshield.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "com.scamshield.app_preferences";
    private static final String KEY_THEME_MODE = "pref_theme_mode"; // "system", "light", "dark"
    private static final String KEY_SAVE_HISTORY = "pref_save_history";
    private static final String KEY_OFFLINE_ONLY = "pref_offline_only";
    private static final String KEY_FIRST_LAUNCH = "pref_first_launch";
    private static final String KEY_REALTIME_SMS = "pref_realtime_sms_shield";
    private static final String KEY_NOTIFY_ALL_SMS = "pref_notify_all_sms";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getThemeMode() {
        return prefs.getString(KEY_THEME_MODE, "system");
    }

    public void setThemeMode(String themeMode) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode).apply();
    }

    public boolean isSaveHistoryEnabled() {
        return prefs.getBoolean(KEY_SAVE_HISTORY, true);
    }

    public void setSaveHistoryEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply();
    }

    public boolean isOfflineOnlyEnabled() {
        return prefs.getBoolean(KEY_OFFLINE_ONLY, false);
    }

    public void setOfflineOnlyEnabled(boolean offlineOnly) {
        prefs.edit().putBoolean(KEY_OFFLINE_ONLY, offlineOnly).apply();
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    public boolean isRealTimeSmsShieldEnabled() {
        return prefs.getBoolean(KEY_REALTIME_SMS, true);
    }

    public void setRealTimeSmsShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REALTIME_SMS, enabled).apply();
    }

    public boolean isNotifyAllSmsEnabled() {
        return prefs.getBoolean(KEY_NOTIFY_ALL_SMS, true);
    }

    public void setNotifyAllSmsEnabled(boolean notifyAll) {
        prefs.edit().putBoolean(KEY_NOTIFY_ALL_SMS, notifyAll).apply();
    }

    public void clearAllPreferences() {
        prefs.edit().clear().apply();
    }
}
