package com.scamshield.app.ui.message;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.scamshield.app.R;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.data.repository.ScamAnalysisRepository;
import com.scamshield.app.domain.engine.ScamAnalysisEngine;
import com.scamshield.app.ui.result.ResultActivity;
import com.scamshield.app.util.SecuritySanitizer;

public class CheckMessageActivity extends AppCompatActivity {

    private TextInputEditText etMessage;
    private TextView tvCharCount;
    private LinearProgressIndicator progressLoading;
    private Button btnAnalyze;
    private ScamAnalysisRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_message);

        repository = new ScamAnalysisRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etMessage = findViewById(R.id.et_message);
        tvCharCount = findViewById(R.id.tv_char_count);
        progressLoading = findViewById(R.id.progress_loading);
        btnAnalyze = findViewById(R.id.btn_analyze_message);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                tvCharCount.setText(getString(R.string.char_count_format, length));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Paste from clipboard
        findViewById(R.id.btn_paste).setOnClickListener(v -> pasteFromClipboard());

        // Clear input
        findViewById(R.id.btn_clear_input).setOnClickListener(v -> etMessage.setText(""));

        // Realistic Samples
        findViewById(R.id.chip_sample_bank).setOnClickListener(v -> {
            etMessage.setText(getString(R.string.sample_bank_phish));
        });

        findViewById(R.id.chip_sample_delivery).setOnClickListener(v -> {
            etMessage.setText(getString(R.string.sample_delivery_scam));
        });

        findViewById(R.id.chip_sample_crypto).setOnClickListener(v -> {
            etMessage.setText(getString(R.string.sample_crypto_scam));
        });

        // Pre-fill if intent extra provided (e.g., from History or Share sheet)
        String prefill = getIntent().getStringExtra("PREFILL_CONTENT");
        if (prefill != null && !prefill.isEmpty()) {
            etMessage.setText(prefill);
        }

        btnAnalyze.setOnClickListener(v -> performAnalysis());
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) {
                    etMessage.setText(text);
                }
            }
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void performAnalysis() {
        String rawInput = etMessage.getText() != null ? etMessage.getText().toString() : "";
        String sanitized = SecuritySanitizer.sanitizeInput(rawInput);

        if (!SecuritySanitizer.isValidContent(sanitized)) {
            Toast.makeText(this, "Please paste or enter message content to analyze (at least 3 characters).", Toast.LENGTH_SHORT).show();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);

        repository.analyze(sanitized, ScanType.MESSAGE, new ScamAnalysisEngine.AnalysisCallback() {
            @Override
            public void onSuccess(ScamAnalysisResult result) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyze.setEnabled(true);

                Intent intent = new Intent(CheckMessageActivity.this, ResultActivity.class);
                intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyze.setEnabled(true);
                Toast.makeText(CheckMessageActivity.this, "Analysis error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
