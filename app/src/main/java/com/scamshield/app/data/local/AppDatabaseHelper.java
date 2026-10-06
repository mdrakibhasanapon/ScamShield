package com.scamshield.app.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.scamshield.app.data.model.FeedbackItem;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScanHistoryItem;
import com.scamshield.app.data.model.ScanType;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "scamshield.db";
    private static final int DATABASE_VERSION = 1;

    // Table: Scan History
    public static final String TABLE_SCANS = "scan_history";
    public static final String COL_ID = "id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_SCAN_TYPE = "scan_type";
    public static final String COL_SNIPPET = "snippet";
    public static final String COL_RISK_SCORE = "risk_score";
    public static final String COL_RISK_LEVEL = "risk_level";

    // Table: Feedback
    public static final String TABLE_FEEDBACK = "user_feedback";
    public static final String COL_FEEDBACK_SCAN_ID = "scan_id";
    public static final String COL_FEEDBACK_RATING = "rating";
    public static final String COL_FEEDBACK_REASON = "reason";
    public static final String COL_FEEDBACK_TIMESTAMP = "timestamp";

    private static AppDatabaseHelper instance;

    public static synchronized AppDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AppDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createScansTable = "CREATE TABLE " + TABLE_SCANS + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_SCAN_TYPE + " TEXT, " +
                COL_SNIPPET + " TEXT, " +
                COL_RISK_SCORE + " INTEGER, " +
                COL_RISK_LEVEL + " TEXT)";
        db.execSQL(createScansTable);

        String createFeedbackTable = "CREATE TABLE " + TABLE_FEEDBACK + " (" +
                COL_FEEDBACK_SCAN_ID + " TEXT, " +
                COL_FEEDBACK_RATING + " TEXT, " +
                COL_FEEDBACK_REASON + " TEXT, " +
                COL_FEEDBACK_TIMESTAMP + " INTEGER)";
        db.execSQL(createFeedbackTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCANS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDBACK);
        onCreate(db);
    }

    public synchronized void insertScan(ScanHistoryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ID, item.getId());
        values.put(COL_TIMESTAMP, item.getTimestamp());
        values.put(COL_SCAN_TYPE, item.getScanType().name());
        values.put(COL_SNIPPET, item.getSnippet());
        values.put(COL_RISK_SCORE, item.getRiskScore());
        values.put(COL_RISK_LEVEL, item.getRiskLevel().name());
        db.insertWithOnConflict(TABLE_SCANS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized List<ScanHistoryItem> getAllScans() {
        return getScansInternal(null, null, "timestamp DESC", null);
    }

    public synchronized List<ScanHistoryItem> getRecentScans(int limit) {
        return getScansInternal(null, null, "timestamp DESC", String.valueOf(limit));
    }

    public synchronized List<ScanHistoryItem> getScansByRiskLevel(RiskLevel riskLevel) {
        return getScansInternal(COL_RISK_LEVEL + " = ?", new String[]{riskLevel.name()}, "timestamp DESC", null);
    }

    private List<ScanHistoryItem> getScansInternal(String selection, String[] selectionArgs, String orderBy, String limit) {
        List<ScanHistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SCANS, null, selection, selectionArgs, null, null, orderBy, limit);
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COL_ID);
                int timeIdx = cursor.getColumnIndexOrThrow(COL_TIMESTAMP);
                int typeIdx = cursor.getColumnIndexOrThrow(COL_SCAN_TYPE);
                int snipIdx = cursor.getColumnIndexOrThrow(COL_SNIPPET);
                int scoreIdx = cursor.getColumnIndexOrThrow(COL_RISK_SCORE);
                int levelIdx = cursor.getColumnIndexOrThrow(COL_RISK_LEVEL);

                do {
                    String id = cursor.getString(idIdx);
                    long time = cursor.getLong(timeIdx);
                    ScanType scanType = ScanType.MESSAGE;
                    try {
                        scanType = ScanType.valueOf(cursor.getString(typeIdx));
                    } catch (Exception ignored) {}
                    String snippet = cursor.getString(snipIdx);
                    int score = cursor.getInt(scoreIdx);
                    RiskLevel riskLevel = RiskLevel.fromScore(score);
                    try {
                        riskLevel = RiskLevel.valueOf(cursor.getString(levelIdx));
                    } catch (Exception ignored) {}

                    list.add(new ScanHistoryItem(id, time, scanType, snippet, score, riskLevel));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized boolean deleteScan(String id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SCANS, COL_ID + " = ?", new String[]{id}) > 0;
    }

    public synchronized void deleteAllScans() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SCANS, null, null);
    }

    public synchronized void insertFeedback(FeedbackItem feedback) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FEEDBACK_SCAN_ID, feedback.getScanId());
        values.put(COL_FEEDBACK_RATING, feedback.getRating().name());
        values.put(COL_FEEDBACK_REASON, feedback.getReason());
        values.put(COL_FEEDBACK_TIMESTAMP, feedback.getTimestamp());
        db.insert(TABLE_FEEDBACK, null, values);
    }
}
