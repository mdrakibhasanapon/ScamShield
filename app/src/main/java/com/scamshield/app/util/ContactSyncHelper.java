package com.scamshield.app.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

import com.scamshield.app.data.local.AppDatabaseHelper;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.TrustedContact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContactSyncHelper {

    public static final String ACTION_CONTACTS_SYNCED = "com.scamshield.app.ACTION_CONTACTS_SYNCED";

    // Known Wangiri / high-cost international premium toll fraud prefixes
    private static final Set<String> HIGH_RISK_TOLL_PREFIXES = new HashSet<>(Arrays.asList(
            "+232", // Sierra Leone
            "+247", // Ascension Island
            "+269", // Comoros
            "+674", // Nauru
            "+675", // Papua New Guinea
            "+676", // Tonga
            "+677", // Solomon Islands
            "+688", // Tuvalu
            "+881", // Global Mobile Satellite System
            "+882"  // International Networks
    ));

    // Deceptive contact names commonly added by scammers/malware
    private static final String[] DECEPTIVE_CONTACT_KEYWORDS = {
            "fraud alert", "bank support", "kyc support", "otp verify",
            "security department", "irs agent", "gift card reward", "lottery claim"
    };

    public static boolean hasContactPermission(Context context) {
        if (context == null) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public static int syncContacts(Context context) {
        if (context == null || !hasContactPermission(context)) return 0;

        List<TrustedContact> contacts = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                long now = System.currentTimeMillis();

                while (!cursor.isAfterLast()) {
                    String id = idIdx != -1 ? cursor.getString(idIdx) : String.valueOf(contacts.size());
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "Unknown";
                    String phone = numIdx != -1 ? cursor.getString(numIdx) : "";

                    if (phone != null && !phone.trim().isEmpty()) {
                        String normalized = TrustedContact.normalizePhone(phone);
                        boolean isSuspicious = false;
                        String reason = null;

                        // Heuristic 1: Wangiri High-Cost Premium Rate Prefix
                        for (String prefix : HIGH_RISK_TOLL_PREFIXES) {
                            if (normalized.startsWith(prefix)) {
                                isSuspicious = true;
                                reason = "⚠️ High-Rate International Toll Destination (" + prefix + "). Associated with Wangiri one-ring callback fraud.";
                                break;
                            }
                        }

                        // Heuristic 2: Deceptive Institutional Contact Name
                        if (!isSuspicious && name != null) {
                            String lowerName = name.toLowerCase(Locale.ROOT);
                            for (String keyword : DECEPTIVE_CONTACT_KEYWORDS) {
                                if (lowerName.contains(keyword)) {
                                    isSuspicious = true;
                                    reason = "⚠️ Deceptive Contact Title: Claims institutional authority (" + keyword + "). Verify number independently.";
                                    break;
                                }
                            }
                        }

                        contacts.add(new TrustedContact(
                                id, name, phone, normalized, isSuspicious, reason, now
                        ));
                    }
                    cursor.moveToNext();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!contacts.isEmpty()) {
            AppDatabaseHelper db = AppDatabaseHelper.getInstance(context);
            db.insertOrUpdateContacts(contacts);

            PreferencesManager prefs = new PreferencesManager(context);
            prefs.setLastContactSyncTime(System.currentTimeMillis());
            prefs.setSyncedContactsCount(db.getTrustedContactCount());

            try {
                Intent syncIntent = new Intent(ACTION_CONTACTS_SYNCED);
                syncIntent.setPackage(context.getPackageName());
                context.sendBroadcast(syncIntent);
            } catch (Exception ignored) {}
        }

        return contacts.size();
    }

    public static boolean isTrustedContact(Context context, String senderQuery) {
        if (context == null || senderQuery == null || senderQuery.trim().isEmpty()) return false;
        AppDatabaseHelper db = AppDatabaseHelper.getInstance(context);
        return db.isSenderTrusted(senderQuery);
    }

    public static void populateSampleContactsIfEmpty(Context context) {
        if (context == null) return;
        AppDatabaseHelper db = AppDatabaseHelper.getInstance(context);
        if (db.getTrustedContactCount() > 0) return;

        long now = System.currentTimeMillis();
        List<TrustedContact> samples = new ArrayList<>();
        samples.add(new TrustedContact("sample_1", "Mom", "+1 (555) 014-4901", "+15550144901", false, null, now));
        samples.add(new TrustedContact("sample_2", "David Miller", "+1 (555) 018-8231", "+15550188231", false, null, now));
        samples.add(new TrustedContact("sample_3", "Sarah Jenkins", "+1 (555) 019-9432", "+15550199432", false, null, now));
        samples.add(new TrustedContact("sample_4", "Unknown Callback Toll", "+232 99 123456", "+23299123456", true, "⚠️ High-Rate International Toll Destination (+232). Associated with Wangiri one-ring callback fraud.", now));

        db.insertOrUpdateContacts(samples);
        PreferencesManager prefs = new PreferencesManager(context);
        prefs.setLastContactSyncTime(now);
        prefs.setSyncedContactsCount(samples.size());
    }
}
