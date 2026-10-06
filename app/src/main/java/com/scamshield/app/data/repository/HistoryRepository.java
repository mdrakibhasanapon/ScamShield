package com.scamshield.app.data.repository;

import android.content.Context;

import com.scamshield.app.data.local.AppDatabaseHelper;
import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.FeedbackItem;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScanHistoryItem;
import com.scamshield.app.data.model.ScamAnalysisResult;

import java.util.Collections;
import java.util.List;

public class HistoryRepository {

    private final AppDatabaseHelper dbHelper;
    private final PreferencesManager preferencesManager;

    public HistoryRepository(Context context) {
        this.dbHelper = AppDatabaseHelper.getInstance(context);
        this.preferencesManager = new PreferencesManager(context);
    }

    public void saveScan(ScamAnalysisResult result) {
        if (!preferencesManager.isSaveHistoryEnabled()) {
            return;
        }

        ScanHistoryItem item = new ScanHistoryItem(
                result.getId(),
                result.getTimestamp(),
                result.getScanType(),
                result.getSnippet(120),
                result.getRiskScore(),
                result.getRiskLevel()
        );
        dbHelper.insertScan(item);
    }

    public List<ScanHistoryItem> getAllScans() {
        if (!preferencesManager.isSaveHistoryEnabled()) {
            return Collections.emptyList();
        }
        return dbHelper.getAllScans();
    }

    public List<ScanHistoryItem> getRecentScans(int limit) {
        if (!preferencesManager.isSaveHistoryEnabled()) {
            return Collections.emptyList();
        }
        return dbHelper.getRecentScans(limit);
    }

    public List<ScanHistoryItem> getScansByRisk(RiskLevel riskLevel) {
        if (!preferencesManager.isSaveHistoryEnabled()) {
            return Collections.emptyList();
        }
        return dbHelper.getScansByRiskLevel(riskLevel);
    }

    public boolean deleteScan(String id) {
        return dbHelper.deleteScan(id);
    }

    public void deleteAllScans() {
        dbHelper.deleteAllScans();
    }

    public void saveFeedback(FeedbackItem feedback) {
        dbHelper.insertFeedback(feedback);
    }
}
