package com.scamshield.app.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.domain.engine.LocalHeuristicEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to read and score existing SMS messages from the device inbox.
 * Allows instant verification of ScamShield's scoring capabilities.
 */
public class SmsInboxHelper {

    private static final String TAG = "SmsInboxHelper";

    public static List<ScamAnalysisResult> scanRecentInbox(Context context, int limit) {
        List<ScamAnalysisResult> results = new ArrayList<>();
        if (context == null) return results;

        LocalHeuristicEngine engine = new LocalHeuristicEngine();
        HistoryRepository historyRepository = new HistoryRepository(context);

        ContentResolver cr = context.getContentResolver();
        Uri inboxUri = Uri.parse("content://sms/inbox");

        try (Cursor cursor = cr.query(
                inboxUri,
                new String[]{"_id", "address", "body", "date"},
                null,
                null,
                "date DESC LIMIT " + limit)) {

            if (cursor != null && cursor.moveToFirst()) {
                int addressIdx = cursor.getColumnIndex("address");
                int bodyIdx = cursor.getColumnIndex("body");

                do {
                    String sender = addressIdx != -1 ? cursor.getString(addressIdx) : "Unknown Sender";
                    String body = bodyIdx != -1 ? cursor.getString(bodyIdx) : "";

                    if (body != null && !body.trim().isEmpty()) {
                        ScamAnalysisResult result = engine.evaluateContent(body, ScanType.MESSAGE, true);
                        historyRepository.saveScan(result);
                        results.add(result);
                    }
                } while (cursor.moveToNext());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Missing READ_SMS permission to access inbox: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error querying SMS inbox: " + e.getMessage(), e);
        }

        return results;
    }
}
