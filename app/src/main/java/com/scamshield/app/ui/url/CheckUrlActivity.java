package com.scamshield.app.ui.url;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

public class CheckUrlActivity extends AppCompatActivity {

    private TextInputEditText etUrl;
    private View cardUrlPreview;
    private TextView tvPreviewScheme;
    private TextView tvPreviewDomain;
    private TextView tvPreviewPath;
    private LinearProgressIndicator progressLoading;
    private Button btnAnalyze;
    private ScamAnalysisRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_url);

        repository = new ScamAnalysisRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etUrl = findViewById(R.id.et_url);
        cardUrlPreview = findViewById(R.id.card_url_preview);
        tvPreviewScheme = findViewById(R.id.tv_preview_scheme);
        tvPreviewDomain = findViewById(R.id.tv_preview_domain);
        tvPreviewPath = findViewById(R.id.tv_preview_path);
        progressLoading = findViewById(R.id.progress_loading);
        btnAnalyze = findViewById(R.id.btn_analyze_url);

        etUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateUrlPreview(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_paste_url).setOnClickListener(v -> pasteFromClipboard());
        findViewById(R.id.btn_clear_url).setOnClickListener(v -> etUrl.setText(""));

        // Samples
        findViewById(R.id.chip_sample_typosquat).setOnClickListener(v -> {
            etUrl.setText("http://paypa1-security-verification.top/login/verify");
        });

        findViewById(R.id.chip_sample_ip_url).setOnClickListener(v -> {
            etUrl.setText("http://192.168.1.105/auth/bank-security");
        });

        findViewById(R.id.chip_sample_shortener).setOnClickListener(v -> {
            etUrl.setText("https://bit.ly/update-parcel-fee");
        });

        // Pre-fill if intent extra provided
        String prefill = getIntent().getStringExtra("PREFILL_URL");
        if (prefill != null && !prefill.isEmpty()) {
            etUrl.setText(prefill);
        }

        btnAnalyze.setOnClickListener(v -> performAnalysis());
    }

    private void updateUrlPreview(String input) {
        if (input == null || input.trim().isEmpty()) {
            cardUrlPreview.setVisibility(View.GONE);
            return;
        }

        String urlStr = input.trim();
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            urlStr = "https://" + urlStr;
        }

        try {
            Uri uri = Uri.parse(urlStr);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "none";
            String host = uri.getHost() != null ? uri.getHost() : "unspecified";
            String path = uri.getPath() != null ? uri.getPath() : "/";

            tvPreviewScheme.setText(getString(R.string.link_scheme_label, scheme.toUpperCase()));
            tvPreviewDomain.setText(getString(R.string.link_host_label, host));
            tvPreviewPath.setText(getString(R.string.link_path_label, path));
            cardUrlPreview.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            cardUrlPreview.setVisibility(View.GONE);
        }
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) {
                    etUrl.setText(text);
                }
            }
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void performAnalysis() {
        String rawInput = etUrl.getText() != null ? etUrl.getText().toString() : "";
        String sanitized = SecuritySanitizer.sanitizeInput(rawInput);

        if (!SecuritySanitizer.isValidContent(sanitized)) {
            Toast.makeText(this, "Please enter a valid link or URL to analyze.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);

        repository.analyze(sanitized, ScanType.URL, new ScamAnalysisEngine.AnalysisCallback() {
            @Override
            public void onSuccess(ScamAnalysisResult result) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyze.setEnabled(true);

                Intent intent = new Intent(CheckUrlActivity.this, ResultActivity.class);
                intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyze.setEnabled(true);
                Toast.makeText(CheckUrlActivity.this, "URL analysis error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
