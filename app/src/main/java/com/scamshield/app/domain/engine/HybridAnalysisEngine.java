package com.scamshield.app.domain.engine;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.scamshield.app.data.local.PreferencesManager;
import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HybridAnalysisEngine implements ScamAnalysisEngine {

    private final Context context;
    private final LocalHeuristicEngine localEngine;
    private final RemoteAIEngine remoteEngine;
    private final PreferencesManager preferencesManager;

    public HybridAnalysisEngine(Context context) {
        this.context = context.getApplicationContext();
        this.localEngine = new LocalHeuristicEngine();
        this.remoteEngine = new RemoteAIEngine();
        this.preferencesManager = new PreferencesManager(this.context);
    }

    @Override
    public void analyze(String content, ScanType scanType, AnalysisCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            if (callback != null) callback.onError("Content cannot be empty.");
            return;
        }

        // Fast Layer 1 & 2: Local Heuristic Analysis
        ScamAnalysisResult localResult = localEngine.evaluateContent(content, scanType, true);

        // Check if user requested Offline-Only or if device has no network
        boolean userOfflinePref = preferencesManager.isOfflineOnlyEnabled();
        boolean isConnected = isNetworkAvailable();

        if (userOfflinePref || !isConnected) {
            // Return purely local heuristic result immediately
            if (callback != null) callback.onSuccess(localResult);
            return;
        }

        // Layer 3: Remote AI Analysis
        remoteEngine.analyze(content, scanType, new AnalysisCallback() {
            @Override
            public void onSuccess(ScamAnalysisResult remoteResult) {
                // Fuse local indicators with remote AI intelligence
                ScamAnalysisResult fusedResult = fuseResults(localResult, remoteResult);
                if (callback != null) callback.onSuccess(fusedResult);
            }

            @Override
            public void onError(String errorMessage) {
                // Graceful fallback to local heuristic on network / backend error
                if (callback != null) callback.onSuccess(localResult);
            }
        });
    }

    private ScamAnalysisResult fuseResults(ScamAnalysisResult local, ScamAnalysisResult remote) {
        // Take maximum calibrated risk score between local heuristics and AI
        int combinedScore = Math.max(local.getRiskScore(), remote.getRiskScore());
        RiskLevel combinedLevel = RiskLevel.fromScore(combinedScore);

        // Combine indicators without duplicates
        List<DetectedIndicator> combinedIndicators = new ArrayList<>(local.getIndicators());
        combinedIndicators.addAll(remote.getIndicators());

        // Deduplicate whyFlagged bullets
        Set<String> whyFlaggedSet = new LinkedHashSet<>(local.getWhyFlagged());
        whyFlaggedSet.addAll(remote.getWhyFlagged());

        // Deduplicate recommendations
        Set<String> recSet = new LinkedHashSet<>(local.getRecommendedActions());
        recSet.addAll(remote.getRecommendedActions());

        // Deduplicate human verification notes
        Set<String> notesSet = new LinkedHashSet<>(local.getHumanVerificationNotes());
        notesSet.addAll(remote.getHumanVerificationNotes());

        return new ScamAnalysisResult(
                UUID.randomUUID().toString(),
                local.getSubmittedContent(),
                local.getScanType(),
                combinedScore,
                combinedLevel,
                combinedIndicators,
                new ArrayList<>(whyFlaggedSet),
                new ArrayList<>(recSet),
                new ArrayList<>(notesSet),
                false, // Online fused result
                System.currentTimeMillis()
        );
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }
}
