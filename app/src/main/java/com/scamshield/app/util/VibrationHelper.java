package com.scamshield.app.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.RiskLevel;

public class VibrationHelper {

    // Distinctive SOS/Threat Alert: 3 sharp vibration pulses
    public static final long[] PATTERN_THREAT = new long[]{0, 250, 100, 250, 100, 350};
    public static final int[] AMPLITUDE_THREAT = new int[]{0, 255, 0, 255, 0, 255};

    // Warning/Attention Alert: 2 medium vibration pulses
    public static final long[] PATTERN_WARNING = new long[]{0, 200, 100, 200};
    public static final int[] AMPLITUDE_WARNING = new int[]{0, 220, 0, 220};

    // Subtle Info/Clean Summary: 1 light pulse
    public static final long[] PATTERN_CLEAN = new long[]{0, 120};
    public static final int[] AMPLITUDE_CLEAN = new int[]{0, 150};

    /**
     * Triggers vibration when a security notification is dispatched, based on risk level.
     */
    public static void triggerNotificationVibration(Context context, RiskLevel riskLevel) {
        if (context == null) return;

        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isNotificationVibrationEnabled()) {
            return;
        }

        if (riskLevel == RiskLevel.HIGH_RISK) {
            vibrateWithPattern(context, PATTERN_THREAT, AMPLITUDE_THREAT, AudioAttributes.USAGE_NOTIFICATION_EVENT);
        } else if (riskLevel == RiskLevel.SUSPICIOUS || riskLevel == RiskLevel.NEEDS_ATTENTION) {
            vibrateWithPattern(context, PATTERN_WARNING, AMPLITUDE_WARNING, AudioAttributes.USAGE_NOTIFICATION_EVENT);
        } else {
            vibrateWithPattern(context, PATTERN_CLEAN, AMPLITUDE_CLEAN, AudioAttributes.USAGE_NOTIFICATION_EVENT);
        }
    }

    /**
     * Triggers distinct threat vibration (e.g. when malware is detected during a scan).
     */
    public static void triggerThreatVibration(Context context, boolean isCritical) {
        if (context == null) return;
        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isNotificationVibrationEnabled()) return;

        if (isCritical) {
            vibrateWithPattern(context, PATTERN_THREAT, AMPLITUDE_THREAT, AudioAttributes.USAGE_ALARM);
        } else {
            vibrateWithPattern(context, PATTERN_WARNING, AMPLITUDE_WARNING, AudioAttributes.USAGE_NOTIFICATION_EVENT);
        }
    }

    /**
     * Triggers a preview test vibration when user toggles vibration setting in app.
     */
    public static void triggerTestVibration(Context context) {
        vibrateWithPattern(context, PATTERN_THREAT, AMPLITUDE_THREAT, AudioAttributes.USAGE_NOTIFICATION_EVENT);
    }

    /**
     * Executes the hardware vibration using modern VibratorManager (API 31+) or Vibrator.
     */
    private static void vibrateWithPattern(Context context, long[] timings, int[] amplitudes, int usage) {
        try {
            Vibrator vibrator = getVibrator(context);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(usage)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect;
                if (vibrator.hasAmplitudeControl() && amplitudes != null && amplitudes.length == timings.length) {
                    effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                } else {
                    effect = VibrationEffect.createWaveform(timings, -1);
                }
                vibrator.vibrate(effect, audioAttributes);
            } else {
                vibrator.vibrate(timings, -1);
            }
        } catch (Exception ignored) {
            // Failsafe: avoid crashes on devices with restricted hardware access
        }
    }

    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                return vm.getDefaultVibrator();
            }
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
