package com.scamshield.app.data.repository;

import android.content.Context;

import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.domain.engine.HybridAnalysisEngine;
import com.scamshield.app.domain.engine.ScamAnalysisEngine;

public class ScamAnalysisRepository {

    private final ScamAnalysisEngine engine;
    private final HistoryRepository historyRepository;

    public ScamAnalysisRepository(Context context) {
        this.engine = new HybridAnalysisEngine(context);
        this.historyRepository = new HistoryRepository(context);
    }

    public void analyze(String content, ScanType scanType, ScamAnalysisEngine.AnalysisCallback callback) {
        engine.analyze(content, scanType, new ScamAnalysisEngine.AnalysisCallback() {
            @Override
            public void onSuccess(ScamAnalysisResult result) {
                // Auto-save to local history if enabled
                historyRepository.saveScan(result);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) callback.onError(errorMessage);
            }
        });
    }
}
