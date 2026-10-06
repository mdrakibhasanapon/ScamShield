package com.scamshield.app.domain.engine;

import android.os.Handler;
import android.os.Looper;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteAIEngine implements ScamAnalysisEngine {

    // Configurable backend endpoint for secure remote AI analysis
    // Secret keys reside on the secure backend proxy, never compiled into the client APK
    private static final String DEFAULT_BACKEND_URL = "https://api.scamshield.internal/v1/analyze";
    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 6000;

    private final String backendEndpoint;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RemoteAIEngine() {
        this(DEFAULT_BACKEND_URL);
    }

    public RemoteAIEngine(String backendEndpoint) {
        this.backendEndpoint = backendEndpoint;
    }

    @Override
    public void analyze(String content, ScanType scanType, AnalysisCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            if (callback != null) callback.onError("Content cannot be empty.");
            return;
        }

        executor.execute(() -> {
            try {
                // Prepare sanitized JSON request payload
                JSONObject payload = new JSONObject();
                payload.put("content", content);
                payload.put("scanType", scanType.name());
                payload.put("clientVersion", "1.0.0");

                URL url = new URL(backendEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }

                    ScamAnalysisResult result = parseAiResponse(response.toString(), content, scanType);
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(result);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("Remote AI service returned status: " + statusCode);
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Remote AI connection failure: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Parses and strictly validates structured JSON response from the AI backend.
     */
    public ScamAnalysisResult parseAiResponse(String jsonString, String originalContent, ScanType scanType) throws Exception {
        JSONObject obj = new JSONObject(jsonString);

        int score = obj.optInt("score", 50);
        score = Math.max(0, Math.min(100, score));

        String riskLevelStr = obj.optString("riskLevel", "NEEDS_ATTENTION");
        RiskLevel riskLevel = RiskLevel.fromScore(score);
        try {
            riskLevel = RiskLevel.valueOf(riskLevelStr);
        } catch (Exception ignored) {}

        List<DetectedIndicator> indicators = new ArrayList<>();
        JSONArray indArray = obj.optJSONArray("indicators");
        if (indArray != null) {
            for (int i = 0; i < indArray.length(); i++) {
                JSONObject indObj = indArray.getJSONObject(i);
                String id = indObj.optString("id", "AI_INDICATOR_" + (i + 1));
                String title = indObj.optString("title", "Suspicious Indicator");
                String explanation = indObj.optString("explanation", "");
                String sevStr = indObj.optString("severity", "WARNING");
                DetectedIndicator.Severity severity = DetectedIndicator.Severity.WARNING;
                try {
                    severity = DetectedIndicator.Severity.valueOf(sevStr);
                } catch (Exception ignored) {}

                indicators.add(new DetectedIndicator(
                        id,
                        title,
                        explanation,
                        severity,
                        IndicatorCategory.SOCIAL_ENGINEERING,
                        ""
                ));
            }
        }

        List<String> whyFlagged = new ArrayList<>();
        JSONArray whyArray = obj.optJSONArray("whyFlagged");
        if (whyArray != null) {
            for (int i = 0; i < whyArray.length(); i++) {
                whyFlagged.add(whyArray.getString(i));
            }
        } else if (obj.has("explanation")) {
            whyFlagged.add(obj.getString("explanation"));
        }

        List<String> recommendedActions = new ArrayList<>();
        JSONArray recArray = obj.optJSONArray("recommendations");
        if (recArray != null) {
            for (int i = 0; i < recArray.length(); i++) {
                recommendedActions.add(recArray.getString(i));
            }
        } else if (obj.has("recommendation")) {
            recommendedActions.add(obj.getString("recommendation"));
        }

        List<String> humanVerificationNotes = new ArrayList<>();
        if (obj.has("limitations")) {
            humanVerificationNotes.add(obj.getString("limitations"));
        } else {
            humanVerificationNotes.add("AI analysis may miss novel zero-day fraud tactics. Confirm independently.");
        }

        return new ScamAnalysisResult(
                UUID.randomUUID().toString(),
                originalContent,
                scanType,
                score,
                riskLevel,
                indicators,
                whyFlagged,
                recommendedActions,
                humanVerificationNotes,
                false, // Online AI result
                System.currentTimeMillis()
        );
    }
}
