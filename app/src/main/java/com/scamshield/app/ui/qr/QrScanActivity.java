package com.scamshield.app.ui.qr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.scamshield.app.R;
import com.scamshield.app.data.model.ScamAnalysisResult;
import com.scamshield.app.data.model.ScanType;
import com.scamshield.app.data.repository.ScamAnalysisRepository;
import com.scamshield.app.domain.engine.ScamAnalysisEngine;
import com.scamshield.app.ui.result.ResultActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScanActivity extends AppCompatActivity {

    private PreviewView cameraPreviewView;
    private TextView tvCameraStatus;
    private Button btnGrantCamera;
    private Button btnPickQrImage;
    private View cardQrDestination;
    private TextView tvQrDestinationLabel;
    private TextView tvQrDestinationUrl;
    private TextView tvQrResolvedUrl;
    private TextView tvQrDomain;
    private Button btnAnalyzeQrUrl;
    private Button btnOpenInBrowser;
    private Button btnScanAgain;
    private LinearProgressIndicator progressLoading;

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private BarcodeScanner barcodeScanner;
    private volatile boolean isQrDetected = false;
    private String currentDecodedContent = "";
    private String resolvedFinalUrl = null;
    private boolean isContentWebUrl = false;
    private ScamAnalysisRepository repository;

    // Camera Permission Contract
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    btnGrantCamera.setVisibility(View.GONE);
                    tvCameraStatus.setText("Align QR Code within frame");
                    startCamera();
                } else {
                    btnGrantCamera.setVisibility(View.VISIBLE);
                    tvCameraStatus.setText("Camera permission required for live scanner");
                    Toast.makeText(this, "Camera permission denied. You can pick a QR image or grant permission.", Toast.LENGTH_LONG).show();
                }
            }
    );

    // Pick QR Code image from device gallery
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleQrImageSelected
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        repository = new ScamAnalysisRepository(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        cameraPreviewView = findViewById(R.id.camera_preview_view);
        tvCameraStatus = findViewById(R.id.tv_camera_status);
        btnGrantCamera = findViewById(R.id.btn_grant_camera);
        btnPickQrImage = findViewById(R.id.btn_pick_qr_image);
        cardQrDestination = findViewById(R.id.card_qr_destination);
        tvQrDestinationLabel = findViewById(R.id.tv_qr_destination_label);
        tvQrDestinationUrl = findViewById(R.id.tv_qr_destination_url);
        tvQrResolvedUrl = findViewById(R.id.tv_qr_resolved_url);
        tvQrDomain = findViewById(R.id.tv_qr_domain);
        btnAnalyzeQrUrl = findViewById(R.id.btn_analyze_qr_url);
        btnOpenInBrowser = findViewById(R.id.btn_open_in_browser);
        btnScanAgain = findViewById(R.id.btn_scan_again);
        progressLoading = findViewById(R.id.progress_loading);

        btnGrantCamera.setOnClickListener(v -> requestCameraPermission());
        btnPickQrImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        View btnSimulate = findViewById(R.id.btn_simulate_qr);
        if (btnSimulate != null) {
            btnSimulate.setOnClickListener(v -> loadDecodedQrCode(getString(R.string.sample_qr_url)));
        }

        btnAnalyzeQrUrl.setOnClickListener(v -> analyzeQrDestination());

        btnOpenInBrowser.setOnClickListener(v -> {
            String targetToOpen = resolvedFinalUrl != null ? resolvedFinalUrl : currentDecodedContent;
            if (isContentWebUrl && !targetToOpen.isEmpty()) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetToOpen));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not launch browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "This QR code contains non-web content.", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(v -> resetScanner());
        }

        // Initialize Camera
        checkAndStartCamera();
    }

    private void checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            btnGrantCamera.setVisibility(View.GONE);
            startCamera();
        } else {
            btnGrantCamera.setVisibility(View.VISIBLE);
            tvCameraStatus.setText("Camera permission required for live scanner");
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                tvCameraStatus.setText("Camera init error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            @ExperimentalGetImage
            public void analyze(@NonNull ImageProxy imageProxy) {
                Image mediaImage = imageProxy.getImage();
                if (mediaImage != null && !isQrDetected) {
                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                    barcodeScanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    String rawValue = barcode.getRawValue();
                                    if (rawValue != null && !rawValue.trim().isEmpty() && !isQrDetected) {
                                        isQrDetected = true;
                                        runOnUiThread(() -> loadDecodedQrCode(rawValue.trim()));
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Frame decode failure ignored
                            })
                            .addOnCompleteListener(task -> imageProxy.close());
                } else {
                    imageProxy.close();
                }
            }
        });

        try {
            provider.unbindAll();
            provider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            tvCameraStatus.setText("Align QR Code within frame");
        } catch (Exception e) {
            tvCameraStatus.setText("Camera binding failed: " + e.getMessage());
        }
    }

    private void handleQrImageSelected(@Nullable Uri uri) {
        if (uri == null) return;

        progressLoading.setVisibility(View.VISIBLE);
        try {
            InputImage inputImage = InputImage.fromFilePath(this, uri);
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        progressLoading.setVisibility(View.GONE);
                        boolean found = false;
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.trim().isEmpty()) {
                                isQrDetected = true;
                                loadDecodedQrCode(rawValue.trim());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            Toast.makeText(this, "No QR code found in selected image.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to decode QR image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to open image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetScanner() {
        isQrDetected = false;
        currentDecodedContent = "";
        resolvedFinalUrl = null;
        isContentWebUrl = false;
        cardQrDestination.setVisibility(View.GONE);
        if (tvQrResolvedUrl != null) {
            tvQrResolvedUrl.setVisibility(View.GONE);
        }
        tvCameraStatus.setText("Align QR Code within frame");
    }

    private void loadDecodedQrCode(String rawCode) {
        this.currentDecodedContent = rawCode;
        this.resolvedFinalUrl = null;
        this.isContentWebUrl = isWebUrl(rawCode);

        tvQrDestinationUrl.setText(rawCode);
        if (tvQrResolvedUrl != null) {
            tvQrResolvedUrl.setVisibility(View.GONE);
        }

        if (isContentWebUrl) {
            if (tvQrDestinationLabel != null) {
                tvQrDestinationLabel.setText(getString(R.string.qr_destination_label));
            }
            btnOpenInBrowser.setVisibility(View.VISIBLE);
            btnAnalyzeQrUrl.setText(getString(R.string.btn_analyze_qr));

            try {
                Uri uri = Uri.parse(rawCode.startsWith("http") ? rawCode : "https://" + rawCode);
                String host = uri.getHost() != null ? uri.getHost() : "Web link";
                tvQrDomain.setText(getString(R.string.qr_domain_label, host));
            } catch (Exception e) {
                tvQrDomain.setText("Web link destination");
            }

            // Asynchronously resolve safe HTTP redirection (e.g. dynamic QR code generator / shortlink)
            resolveSafeRedirectAsync(rawCode);

        } else if (rawCode.startsWith("WIFI:") || rawCode.startsWith("wifi:")) {
            if (tvQrDestinationLabel != null) {
                tvQrDestinationLabel.setText("Wi-Fi Setup QR:");
            }
            tvQrDomain.setText("Local Wi-Fi Network Credentials");
            btnOpenInBrowser.setVisibility(View.GONE);
            btnAnalyzeQrUrl.setText("Analyze Wi-Fi Setup");
        } else {
            if (tvQrDestinationLabel != null) {
                tvQrDestinationLabel.setText("QR Text Content:");
            }
            tvQrDomain.setText("Plain Text (" + rawCode.length() + " chars)");
            btnOpenInBrowser.setVisibility(View.GONE);
            btnAnalyzeQrUrl.setText("Analyze Text for Scams");
        }

        cardQrDestination.setVisibility(View.VISIBLE);
        tvCameraStatus.setText("QR Code detected!");
        Toast.makeText(this, "QR Code scanned! Review destination before proceeding.", Toast.LENGTH_SHORT).show();
    }

    private boolean isWebUrl(String text) {
        if (text == null) return false;
        String lower = text.trim().toLowerCase(Locale.US);
        return lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("www.") ||
                (lower.contains(".") && !lower.contains(" ") && lower.indexOf('.') < lower.length() - 2);
    }

    private void resolveSafeRedirectAsync(String targetUrl) {
        String safeUrl = targetUrl.startsWith("http") ? targetUrl : "https://" + targetUrl;

        networkExecutor.execute(() -> {
            try {
                URL url = new URL(safeUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(3500);
                conn.setReadTimeout(3500);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) ScamShield/1.0");
                conn.getResponseCode();

                String finalUrlStr = conn.getURL().toString();
                conn.disconnect();

                if (finalUrlStr != null && !finalUrlStr.equalsIgnoreCase(safeUrl)) {
                    runOnUiThread(() -> {
                        resolvedFinalUrl = finalUrlStr;
                        if (tvQrResolvedUrl != null) {
                            tvQrResolvedUrl.setText(String.format(Locale.US, "↳ Target: %s", finalUrlStr));
                            tvQrResolvedUrl.setVisibility(View.VISIBLE);
                        }
                        try {
                            Uri finalUri = Uri.parse(finalUrlStr);
                            if (finalUri.getHost() != null) {
                                tvQrDomain.setText(getString(R.string.qr_domain_label, finalUri.getHost()));
                            }
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {
                // If HEAD fails or network unavailable, non-blocking
            }
        });
    }

    private void analyzeQrDestination() {
        if (currentDecodedContent.isEmpty()) return;

        // Use resolved final destination URL if redirection occurred, otherwise original content
        String analysisTarget = (isContentWebUrl && resolvedFinalUrl != null) ? resolvedFinalUrl : currentDecodedContent;
        ScanType scanType = isContentWebUrl ? ScanType.QR_CODE : ScanType.MESSAGE;

        btnAnalyzeQrUrl.setEnabled(false);
        progressLoading.setVisibility(View.VISIBLE);

        repository.analyze(analysisTarget, scanType, new ScamAnalysisEngine.AnalysisCallback() {
            @Override
            public void onSuccess(ScamAnalysisResult result) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyzeQrUrl.setEnabled(true);
                Intent intent = new Intent(QrScanActivity.this, ResultActivity.class);
                intent.putExtra(ResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage) {
                progressLoading.setVisibility(View.GONE);
                btnAnalyzeQrUrl.setEnabled(true);
                Toast.makeText(QrScanActivity.this, "QR analysis error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}
