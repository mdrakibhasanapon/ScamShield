package com.scamshield.app.service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationManagerCompat;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.domain.engine.LocalHeuristicEngine;
import com.scamshield.app.util.NotificationHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class UniversalMessageListenerService extends NotificationListenerService {

    public static final String ACTION_CHAT_SCANNED = "com.scamshield.app.ACTION_CHAT_SCANNED";
    public static final String EXTRA_APP_NAME = "extra_app_name";
    public static final String EXTRA_SENDER = "extra_sender";
    public static final String EXTRA_MESSAGE = "extra_message";

    // Known messaging applications
    private static final Map<String, String> KNOWN_MESSAGING_APPS = new HashMap<>();
    static {
        KNOWN_MESSAGING_APPS.put("com.whatsapp", "WhatsApp");
        KNOWN_MESSAGING_APPS.put("com.whatsapp.w4b", "WhatsApp Business");
        KNOWN_MESSAGING_APPS.put("org.telegram.messenger", "Telegram");
        KNOWN_MESSAGING_APPS.put("org.telegram.plus", "Telegram Plus");
        KNOWN_MESSAGING_APPS.put("org.thunderdog.challegram", "Telegram X");
        KNOWN_MESSAGING_APPS.put("com.facebook.orca", "Messenger");
        KNOWN_MESSAGING_APPS.put("com.facebook.mlite", "Messenger Lite");
        KNOWN_MESSAGING_APPS.put("org.thoughtcrime.securesms", "Signal");
        KNOWN_MESSAGING_APPS.put("com.imo.android.imoim", "IMO");
        KNOWN_MESSAGING_APPS.put("com.imo.android.imoimbeta", "IMO Beta");
        KNOWN_MESSAGING_APPS.put("com.discord", "Discord");
        KNOWN_MESSAGING_APPS.put("com.viber.voip", "Viber");
        KNOWN_MESSAGING_APPS.put("com.skype.raider", "Skype");
        KNOWN_MESSAGING_APPS.put("com.skype.m2", "Skype Lite");
        KNOWN_MESSAGING_APPS.put("com.tencent.mm", "WeChat");
        KNOWN_MESSAGING_APPS.put("jp.naver.line.android", "LINE");
        KNOWN_MESSAGING_APPS.put("com.snapchat.android", "Snapchat");
        KNOWN_MESSAGING_APPS.put("com.instagram.android", "Instagram");
        KNOWN_MESSAGING_APPS.put("com.google.android.apps.messaging", "Google Messages");
        KNOWN_MESSAGING_APPS.put("com.samsung.android.messaging", "Samsung Messages");
        KNOWN_MESSAGING_APPS.put("com.android.mms", "Messages");
    }

    // Deduplication cache: Key (pkg + sender + message) -> timestamp
    private static final int MAX_CACHE_SIZE = 120;
    private static final long DEDUP_WINDOW_MS = 8000; // 8 seconds
    private static final Map<String, Long> processedMessagesCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Long>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String pkg = sbn.getPackageName();
        if (pkg == null) return;

        // Never intercept notifications from ScamShield itself to prevent infinite notification loops
        String myPkg = getPackageName();
        if (pkg.equals(myPkg) || pkg.startsWith("com.scamshield.app")) {
            return;
        }

        // Ignore internal system framework alerts
        if ("android".equals(pkg) || "com.android.systemui".equals(pkg)) {
            return;
        }

        PreferencesManager prefs = new PreferencesManager(this);
        if (!prefs.isUniversalChatWatchdogEnabled()) {
            return;
        }

        Notification notification = sbn.getNotification();

        // Filter out ongoing background calls or sync services if no chat text
        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0 &&
                (notification.category == null || !Notification.CATEGORY_MESSAGE.equals(notification.category))) {
            return;
        }

        boolean isKnownApp = KNOWN_MESSAGING_APPS.containsKey(pkg);
        String category = notification.category;
        boolean isMessageCategory = Notification.CATEGORY_MESSAGE.equals(category) ||
                Notification.CATEGORY_SOCIAL.equals(category);

        Bundle extras = notification.extras;
        boolean hasMessageExtras = extras != null && (
                extras.containsKey(Notification.EXTRA_MESSAGES) ||
                extras.containsKey(Notification.EXTRA_BIG_TEXT) ||
                extras.containsKey(Notification.EXTRA_TEXT)
        );

        if (!isKnownApp && !isMessageCategory && !hasMessageExtras) {
            return;
        }

        String appName = resolveAppName(pkg);
        MessagePayload payload = extractMessagePayload(extras);
        if (payload == null || payload.text == null || payload.text.trim().length() < 3) {
            return;
        }

        // Deduplicate rapid notification refreshes for the exact same message
        String dedupKey = pkg + "|" + payload.sender + "|" + payload.text.trim();
        long now = System.currentTimeMillis();
        Long lastSeen = processedMessagesCache.get(dedupKey);
        if (lastSeen != null && (now - lastSeen) < DEDUP_WINDOW_MS) {
            return;
        }
        processedMessagesCache.put(dedupKey, now);

        // Run on-device heuristic scoring and trigger alerts
        processMessage(this, appName, payload.sender, payload.text.trim());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // No action needed on dismiss
    }

    public static void processMessage(Context context, String appName, String sender, String messageText) {
        if (context == null || messageText == null || messageText.trim().isEmpty()) return;

        LocalHeuristicEngine engine = new LocalHeuristicEngine();
        ScamAnalysisResult result = engine.evaluateContent(messageText, ScanType.CHAT_MESSAGE, true);

        // Save scan to local history
        try {
            HistoryRepository repository = new HistoryRepository(context);
            repository.saveScan(result);
        } catch (Exception ignored) {}

        // Dispatch alert notification with phone vibration if risk detected
        NotificationHelper.showChatScanNotification(context, appName, sender, messageText, result);

        // Broadcast to active UI screens for immediate dashboard refresh
        try {
            Intent updateIntent = new Intent(ACTION_CHAT_SCANNED);
            updateIntent.setPackage(context.getPackageName());
            updateIntent.putExtra(EXTRA_APP_NAME, appName);
            updateIntent.putExtra(EXTRA_SENDER, sender);
            updateIntent.putExtra(EXTRA_MESSAGE, messageText);
            context.sendBroadcast(updateIntent);
        } catch (Exception ignored) {}
    }

    public static MessagePayload extractMessagePayload(Bundle extras) {
        if (extras == null) return null;

        // 1. Extract Sender / Title
        String sender = "Unknown Sender";
        CharSequence convoTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
        CharSequence bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);

        if (convoTitle != null && convoTitle.length() > 0) {
            sender = convoTitle.toString();
        } else if (bigTitle != null && bigTitle.length() > 0) {
            sender = bigTitle.toString();
        } else if (title != null && title.length() > 0) {
            sender = title.toString();
        }

        // 2. Extract Message Text
        String text = null;

        // Check MessagingStyle historical messages bundle first
        Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messages != null && messages.length > 0) {
            for (int i = messages.length - 1; i >= 0; i--) {
                Parcelable p = messages[i];
                if (p instanceof Bundle) {
                    Bundle b = (Bundle) p;
                    CharSequence msgText = b.getCharSequence("text");
                    CharSequence msgSender = b.getCharSequence("sender");
                    if (msgText != null && msgText.length() > 0) {
                        text = msgText.toString();
                        if (msgSender != null && msgSender.length() > 0) {
                            sender = msgSender.toString();
                        }
                        break;
                    }
                }
            }
        }

        // Fallback to BigText then Text
        if (text == null || text.trim().isEmpty()) {
            CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if (bigText != null && bigText.length() > 0) {
                text = bigText.toString();
            } else {
                CharSequence normalText = extras.getCharSequence(Notification.EXTRA_TEXT);
                if (normalText != null && normalText.length() > 0) {
                    text = normalText.toString();
                }
            }
        }

        if (text == null) return null;

        // Filter out status messages like "Checking for new messages", "WhatsApp Web is active", etc.
        String lower = text.toLowerCase();
        if (lower.startsWith("checking for new") ||
                lower.contains("web is currently active") ||
                lower.startsWith("incoming voice call") ||
                lower.startsWith("incoming video call") ||
                lower.equals("missed call") ||
                lower.equals("typing…") ||
                lower.equals("typing...")) {
            return null;
        }

        return new MessagePayload(sender, text);
    }

    private String resolveAppName(String pkg) {
        String name = KNOWN_MESSAGING_APPS.get(pkg);
        if (name != null) return name;

        try {
            PackageManager pm = getPackageManager();
            CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (Exception ignored) {}

        return "Messaging App";
    }

    public static boolean isNotificationAccessGranted(Context context) {
        if (context == null) return false;
        String packageName = context.getPackageName();
        Set<String> enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context);
        return enabledPackages.contains(packageName);
    }

    public static void openNotificationAccessSettings(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static void simulateIncomingChatScan(Context context, String appName, String sender, String messageText) {
        processMessage(context, appName, sender, messageText);
    }

    public static class MessagePayload {
        public final String sender;
        public final String text;

        public MessagePayload(String sender, String text) {
            this.sender = sender != null ? sender : "Unknown";
            this.text = text != null ? text : "";
        }
    }
}
