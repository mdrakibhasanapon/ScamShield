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

    public static final String KEY_DOWNLOAD_WATCHDOG = "download_watchdog_enabled";
    public static final String KEY_NOTIFY_CLEAN_DOWNLOADS = "notify_clean_downloads";
    public static final String KEY_NOTIFICATION_VIBRATION = "pref_notification_vibration";
    public static final String KEY_UNIVERSAL_CHAT_WATCHDOG = "pref_universal_chat_watchdog";
    public static final String KEY_NOTIFY_SAFE_CHATS = "pref_notify_safe_chats";

    public boolean isUniversalChatWatchdogEnabled() {
        return prefs.getBoolean(KEY_UNIVERSAL_CHAT_WATCHDOG, true);
    }

    public void setUniversalChatWatchdogEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_UNIVERSAL_CHAT_WATCHDOG, enabled).apply();
    }

    public boolean isNotifySafeChatsEnabled() {
        return prefs.getBoolean(KEY_NOTIFY_SAFE_CHATS, false);
    }

    public void setNotifySafeChatsEnabled(boolean notifySafe) {
        prefs.edit().putBoolean(KEY_NOTIFY_SAFE_CHATS, notifySafe).apply();
    }

    public boolean isNotificationVibrationEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true);
    }

    public void setNotificationVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, enabled).apply();
    }

    public boolean isDownloadWatchdogEnabled() {
        return prefs.getBoolean(KEY_DOWNLOAD_WATCHDOG, true);
    }

    public void setDownloadWatchdogEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_WATCHDOG, enabled).apply();
    }

    public boolean isNotifyCleanDownloadsEnabled() {
        return prefs.getBoolean(KEY_NOTIFY_CLEAN_DOWNLOADS, true);
    }

    public void setNotifyCleanDownloadsEnabled(boolean notifyClean) {
        prefs.edit().putBoolean(KEY_NOTIFY_CLEAN_DOWNLOADS, notifyClean).apply();
    }

    public static final String KEY_LAST_DEVICE_SCAN_TIME = "last_device_scan_time";
    public static final String KEY_LAST_DEVICE_SCORE = "last_device_score";
    public static final String KEY_LAST_DEVICE_THREAT_COUNT = "last_device_threat_count";

    public long getLastDeviceScanTime() {
        return prefs.getLong(KEY_LAST_DEVICE_SCAN_TIME, 0);
    }

    public void setLastDeviceScanTime(long time) {
        prefs.edit().putLong(KEY_LAST_DEVICE_SCAN_TIME, time).apply();
    }

    public int getLastDeviceScore() {
        return prefs.getInt(KEY_LAST_DEVICE_SCORE, -1);
    }

    public void setLastDeviceScore(int score) {
        prefs.edit().putInt(KEY_LAST_DEVICE_SCORE, score).apply();
    }

    public int getLastDeviceThreatCount() {
        return prefs.getInt(KEY_LAST_DEVICE_THREAT_COUNT, 0);
    }

    public void setLastDeviceThreatCount(int count) {
        prefs.edit().putInt(KEY_LAST_DEVICE_THREAT_COUNT, count).apply();
    }

    public static final String KEY_CONTACT_SYNC_ENABLED = "pref_contact_sync_enabled";
    public static final String KEY_LAST_CONTACT_SYNC_TIME = "pref_last_contact_sync_time";
    public static final String KEY_SYNCED_CONTACTS_COUNT = "pref_synced_contacts_count";
    public static final String KEY_MIC_SHIELD = "pref_mic_shield_enabled";
    public static final String KEY_LOCATION_RADAR = "pref_location_radar_enabled";
    public static final String KEY_CLIPBOARD_GUARD = "pref_clipboard_guard_enabled";
    public static final String KEY_CALL_SAFETY_SHIELD = "pref_call_safety_shield_enabled";
    public static final String KEY_WEB_SENTINEL = "pref_web_sentinel_enabled";
    public static final String KEY_FINANCIAL_SHIELD = "pref_financial_shield_enabled";

    public boolean isMicShieldEnabled() {
        return prefs.getBoolean(KEY_MIC_SHIELD, true);
    }

    public void setMicShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MIC_SHIELD, enabled).apply();
    }

    public boolean isLocationRadarEnabled() {
        return prefs.getBoolean(KEY_LOCATION_RADAR, true);
    }

    public void setLocationRadarEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCATION_RADAR, enabled).apply();
    }

    public boolean isClipboardGuardEnabled() {
        return prefs.getBoolean(KEY_CLIPBOARD_GUARD, true);
    }

    public void setClipboardGuardEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CLIPBOARD_GUARD, enabled).apply();
    }

    public boolean isCallSafetyShieldEnabled() {
        return prefs.getBoolean(KEY_CALL_SAFETY_SHIELD, true);
    }

    public void setCallSafetyShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CALL_SAFETY_SHIELD, enabled).apply();
    }

    public boolean isWebSentinelEnabled() {
        return prefs.getBoolean(KEY_WEB_SENTINEL, true);
    }

    public void setWebSentinelEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WEB_SENTINEL, enabled).apply();
    }

    public boolean isFinancialShieldEnabled() {
        return prefs.getBoolean(KEY_FINANCIAL_SHIELD, true);
    }

    public void setFinancialShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FINANCIAL_SHIELD, enabled).apply();
    }

    public boolean isContactSyncEnabled() {
        return prefs.getBoolean(KEY_CONTACT_SYNC_ENABLED, true);
    }

    public void setContactSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CONTACT_SYNC_ENABLED, enabled).apply();
    }

    public long getLastContactSyncTime() {
        return prefs.getLong(KEY_LAST_CONTACT_SYNC_TIME, 0);
    }

    public void setLastContactSyncTime(long time) {
        prefs.edit().putLong(KEY_LAST_CONTACT_SYNC_TIME, time).apply();
    }

    public int getSyncedContactsCount() {
        return prefs.getInt(KEY_SYNCED_CONTACTS_COUNT, 0);
    }

    public void setSyncedContactsCount(int count) {
        prefs.edit().putInt(KEY_SYNCED_CONTACTS_COUNT, count).apply();
    }

    public void clearAllPreferences() {
        prefs.edit().clear().apply();
    }
}
