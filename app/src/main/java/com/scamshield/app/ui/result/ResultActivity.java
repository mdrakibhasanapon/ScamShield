package com.scamshield.app.ui.result;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.scamshield.app.R;
import com.scamshield.app.data.model.FeedbackItem;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.ui.widget.RiskScoreView;
import com.scamshield.app.util.ShareUtils;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT = "extra_scam_analysis_result";

    private ScamAnalysisResult result;
    private HistoryRepository historyRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        historyRepository = new HistoryRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        result = (ScamAnalysisResult) getIntent().getSerializableExtra(EXTRA_RESULT);
        if (result == null) {
            Toast.makeText(this, "No assessment data available.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindAssessmentData();
    }

    private void bindAssessmentData() {
        // Hero & Risk Score
        View layoutHero = findViewById(R.id.layout_assessment_hero);
        ImageView ivRiskIcon = findViewById(R.id.iv_risk_icon);
        TextView tvRiskTitle = findViewById(R.id.tv_risk_title);
        RiskScoreView riskScoreView = findViewById(R.id.risk_score_view);
        TextView tvRiskSummary = findViewById(R.id.tv_risk_summary);
        TextView tvOfflineBadge = findViewById(R.id.tv_offline_badge);

        int riskColor = ContextCompat.getColor(this, result.getRiskLevel().getColorResId());
        int riskBgColor = ContextCompat.getColor(this, result.getRiskLevel().getBackgroundColorResId());

        layoutHero.setBackgroundColor(riskBgColor);
        ivRiskIcon.setImageResource(result.getRiskLevel().getIconResId());
        ivRiskIcon.setColorFilter(riskColor);
        tvRiskTitle.setText(result.getRiskLevel().getTitle());
        tvRiskTitle.setTextColor(riskColor);
        tvRiskSummary.setText(result.getRiskLevel().getDescription());

        riskScoreView.setRiskData(result.getRiskScore(), result.getRiskLevel());

        if (result.isOfflineResult()) {
            tvOfflineBadge.setVisibility(View.VISIBLE);
        } else {
            tvOfflineBadge.setVisibility(View.GONE);
        }

        // Snippet
        TextView tvTypeLabel = findViewById(R.id.tv_analyzed_type_label);
        TextView tvSnippet = findViewById(R.id.tv_analyzed_snippet);
        tvTypeLabel.setText(result.getScanType().getLabel());
        tvSnippet.setText(result.getSnippet(240));

        // Indicators RecyclerView
        RecyclerView rvIndicators = findViewById(R.id.rv_indicators);
        rvIndicators.setLayoutManager(new LinearLayoutManager(this));
        IndicatorAdapter indicatorAdapter = new IndicatorAdapter(result.getIndicators());
        rvIndicators.setAdapter(indicatorAdapter);

        // Recommended Actions
        TextView tvRecommended = findViewById(R.id.tv_recommended_actions);
        StringBuilder recSb = new StringBuilder();
        for (String action : result.getRecommendedActions()) {
            recSb.append("✓ ").append(action).append("\n\n");
        }
        tvRecommended.setText(recSb.toString().trim());

        // Human Verification Notes
        TextView tvHumanNotes = findViewById(R.id.tv_human_verification);
        StringBuilder hvSb = new StringBuilder();
        for (String note : result.getHumanVerificationNotes()) {
            hvSb.append("• ").append(note).append("\n\n");
        }
        tvHumanNotes.setText(hvSb.toString().trim());

        // Feedback Buttons (Section 12: False Positive & Feedback Handling)
        Button btnHelpful = findViewById(R.id.btn_feedback_helpful);
        Button btnIncorrect = findViewById(R.id.btn_feedback_incorrect);
        Button btnNotSure = findViewById(R.id.btn_feedback_notsure);

        btnHelpful.setOnClickListener(v -> submitFeedback(FeedbackItem.Rating.HELPFUL, ""));
        btnNotSure.setOnClickListener(v -> submitFeedback(FeedbackItem.Rating.NOT_SURE, ""));
        btnIncorrect.setOnClickListener(v -> showIncorrectFeedbackDialog());

        // Action Buttons
        findViewById(R.id.btn_share_result).setOnClickListener(v -> {
            ShareUtils.shareAssessmentResult(this, result);
        });

        findViewById(R.id.btn_check_another).setOnClickListener(v -> finish());

        // 1. Load Adaptive Bottom Banner
        android.widget.FrameLayout bannerContainer = findViewById(R.id.result_banner_ad_container);
        if (bannerContainer != null) {
            com.scamshield.app.ads.AdMobManager.getInstance().loadAdaptiveBanner(this, bannerContainer);
        }

        // 2. Setup Opt-in Rewarded Ad for Deep Cloud AI Verification
        Button btnWatchRewarded = findViewById(R.id.btn_watch_rewarded_ad);
        TextView tvRewardedStatus = findViewById(R.id.tv_rewarded_status);

        if (btnWatchRewarded != null) {
            btnWatchRewarded.setOnClickListener(v -> {
                btnWatchRewarded.setEnabled(false);
                Toast.makeText(this, R.string.rewarded_ad_loading, Toast.LENGTH_SHORT).show();

                com.scamshield.app.ads.AdMobManager.getInstance().showRewardedAd(this, new com.scamshield.app.ads.AdMobManager.RewardCallback() {
                    @Override
                    public void onRewardEarned() {
                        runOnUiThread(() -> {
                            Toast.makeText(ResultActivity.this, R.string.rewarded_ad_completed, Toast.LENGTH_LONG).show();
                            if (tvRewardedStatus != null) {
                                tvRewardedStatus.setText("✓ Deep Cloud AI scan completed: Risk rating verified with multi-model consensus.");
                                tvRewardedStatus.setTextColor(ContextCompat.getColor(ResultActivity.this, R.color.risk_low));
                            }
                            btnWatchRewarded.setText("Deep Cloud AI Verified ✓");
                            btnWatchRewarded.setBackgroundTintList(ContextCompat.getColorStateList(ResultActivity.this, R.color.risk_low));
                        });
                    }

                    @Override
                    public void onAdCompletedOrUnavailable() {
                        runOnUiThread(() -> {
                            if (btnWatchRewarded != null) {
                                btnWatchRewarded.setEnabled(true);
                            }
                        });
                    }
                });
            });
        }
    }

    private void submitFeedback(FeedbackItem.Rating rating, String reason) {
        FeedbackItem feedback = new FeedbackItem(result.getId(), rating, reason, System.currentTimeMillis());
        historyRepository.saveFeedback(feedback);
        Toast.makeText(this, R.string.feedback_thanks, Toast.LENGTH_SHORT).show();
    }

    private void showIncorrectFeedbackDialog() {
        String[] options = {
                getString(R.string.feedback_option_too_harsh),
                getString(R.string.feedback_option_missed_scam),
                getString(R.string.feedback_option_confusing)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.feedback_dialog_title)
                .setItems(options, (dialog, which) -> {
                    submitFeedback(FeedbackItem.Rating.INCORRECT, options[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
