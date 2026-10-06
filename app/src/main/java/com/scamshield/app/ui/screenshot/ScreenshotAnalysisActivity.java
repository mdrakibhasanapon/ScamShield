package com.scamshield.app.ui.screenshot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.scamshield.app.R;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.data.repository.ScamAnalysisRepository;
import com.scamshield.app.domain.engine.ScamAnalysisEngine;
import com.scamshield.app.ui.result.ResultActivity;
import com.scamshield.app.util.SecuritySanitizer;

import java.io.InputStream;

public class ScreenshotAnalysisActivity extends AppCompatActivity {

    private View cardImagePreview;
    private ImageView ivScreenshotPreview;
    private View cardOcrReview;
    private TextInputEditText etExtractedText;
    private View layoutBottomBar;
    private Button btnAnalyzeExtracted;
    private LinearProgressIndicator progressLoading;
    private ScamAnalysisRepository repository;
    private TextRecognizer textRecognizer;

    // Gallery Picker Contract
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleGalleryImageSelected
    );

    // Live Camera Photo Capture Contract
    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            this::handleCameraCapturedBitmap
    );

    // Camera Permission Contract
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    cameraLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Camera permission is required to photograph screenshots", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot_analysis);

        repository = new ScamAnalysisRepository(this);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        cardImagePreview = findViewById(R.id.card_image_preview);
        ivScreenshotPreview = findViewById(R.id.iv_screenshot_preview);
        cardOcrReview = findViewById(R.id.card_ocr_review);
        etExtractedText = findViewById(R.id.et_extracted_text);
        layoutBottomBar = findViewById(R.id.layout_bottom_bar);
        btnAnalyzeExtracted = findViewById(R.id.btn_analyze_extracted);
        progressLoading = findViewById(R.id.progress_loading);

        findViewById(R.id.btn_gallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        findViewById(R.id.btn_camera).setOnClickListener(v -> takePhotoWithCamera());

        View btnSample = findViewById(R.id.btn_sample_screenshot);
        if (btnSample != null) {
            btnSample.setOnClickListener(v -> loadSampleScreenshot());
        }

        btnAnalyzeExtracted.setOnClickListener(v -> performAnalysis());

        // Handle Image URI passed via Android Share sheet
        String shareUriStr = getIntent().getStringExtra("IMAGE_URI");
        if (shareUriStr != null) {
            handleGalleryImageSelected(Uri.parse(shareUriStr));
        }
    }

    private void takePhotoWithCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handleCameraCapturedBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null) return;

        ivScreenshotPreview.setImageBitmap(bitmap);
        cardImagePreview.setVisibility(View.VISIBLE);

        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        processOcrWithMlKit(inputImage);
    }

    private void handleGalleryImageSelected(@Nullable Uri uri) {
        if (uri == null) return;

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                ivScreenshotPreview.setImageBitmap(bitmap);
                cardImagePreview.setVisibility(View.VISIBLE);
            }

            InputImage inputImage = InputImage.fromFilePath(this, uri);
            processOcrWithMlKit(inputImage);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processOcrWithMlKit(InputImage image) {
        progressLoading.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.ocr_analyzing_image, Toast.LENGTH_SHORT).show();

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    progressLoading.setVisibility(View.GONE);
                    cardOcrReview.setVisibility(View.VISIBLE);
                    layoutBottomBar.setVisibility(View.VISIBLE);

                    String text = visionText.getText();
                    if (text == null || text.trim().isEmpty()) {
                        etExtractedText.setText("");
                        Toast.makeText(this, "No text detected in this image. You can type or paste message text manually.", Toast.LENGTH_LONG).show();
                    } else {
                        etExtractedText.setText(text.trim());
                        Toast.makeText(this, "Text extracted via ML Kit! Please review and edit before analyzing.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressLoading.setVisibility(View.GONE);
                    cardOcrReview.setVisibility(View.VISIBLE);
                    layoutBottomBar.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadSampleScreenshot() {
        cardImagePreview.setVisibility(View.VISIBLE);
        ivScreenshotPreview.setImageResource(R.drawable.ic_action_screenshot);
        cardOcrReview.setVisibility(View.VISIBLE);
        layoutBottomBar.setVisibility(View.VISIBLE);
        etExtractedText.setText("USPS Notice: Your delivery package #US9400123 has been suspended due to an unpaid customs redelivery charge of $2.30. Update your address and card information within 12 hours at https://bit.ly/usps-reschedule-fee");
        Toast.makeText(this, "Sample scam screenshot loaded. Review text and tap Analyze.", Toast.LENGTH_SHORT).show();
    }

    private void performAnalysis() {
        String rawInput = etExtractedText.getText() != null ? etExtractedText.getText().toString() : "";
        String sanitized = SecuritySanitizer.sanitizeInput(rawInput);

        if (!SecuritySanitizer.isValidContent(sanitized)) {
            Toast.makeText(this, "Please review or enter the extracted text before analyzing.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        btnAnalyzeExtracted.setEnabled(false);

        repository.analyze(sanitized, ScanType.SCREENSHOT, new ScamAnalysisEngine.AnalysisCallback() {
            @Override
            public void onSuccess(ScamAnalysisResult result) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyzeExtracted.setEnabled(true);

                Intent intent = new Intent(ScreenshotAnalysisActivity.this, ResultActivity.class);
                intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyzeExtracted.setEnabled(true);
                Toast.makeText(ScreenshotAnalysisActivity.this, "Analysis error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}
