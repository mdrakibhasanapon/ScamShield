package com.scamshield.app.domain.engine;

import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;

public interface ScamAnalysisEngine {

    interface AnalysisCallback {
        void onSuccess(ScamAnalysisResult result);
        void onError(String errorMessage);
    }

    void analyze(String content, ScanType scanType, AnalysisCallback callback);
}
