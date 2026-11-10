package com.scamshield.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.util.NotificationHelper;

public class CallSafetyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        PreferencesManager preferencesManager = new PreferencesManager(context);
        if (!preferencesManager.isCallSafetyShieldEnabled()) return;

        String action = intent.getAction();
        if ("android.intent.action.PHONE_STATE".equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // Post high-priority security advisory for incoming phone call
                NotificationHelper.showSecurityAlertNotification(
                        context,
                        "📞 Incoming Call Security Alert",
                        "⚠️ Unknown Caller: Official bank and government agents will NEVER ask for your secret PIN or OTP codes over the phone.",
                        true
                );
            }
        }
    }
}
