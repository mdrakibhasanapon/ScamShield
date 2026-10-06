package com.scamshield.app.ui.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.scamshield.app.ui.MainActivity;
import com.scamshield.app.ui.message.CheckMessageActivity;
import com.scamshield.app.ui.screenshot.ScreenshotAnalysisActivity;
import com.scamshield.app.ui.url.CheckUrlActivity;

public class ShareReceiverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            } else if (type.startsWith("image/")) {
                handleSendImage(intent);
            } else {
                fallbackToMain("Unsupported content type shared.");
            }
        } else {
            fallbackToMain(null);
        }
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null && !sharedText.trim().isEmpty()) {
            String trimmed = sharedText.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www.")) {
                Intent urlIntent = new Intent(this, CheckUrlActivity.class);
                urlIntent.putExtra("PREFILL_URL", trimmed);
                startActivity(urlIntent);
            } else {
                Intent msgIntent = new Intent(this, CheckMessageActivity.class);
                msgIntent.putExtra("PREFILL_CONTENT", trimmed);
                startActivity(msgIntent);
            }
        } else {
            fallbackToMain("No text found in shared content.");
        }
        finish();
    }

    private void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            Intent screenIntent = new Intent(this, ScreenshotAnalysisActivity.class);
            screenIntent.putExtra("IMAGE_URI", imageUri.toString());
            startActivity(screenIntent);
        } else {
            fallbackToMain("Could not read shared image stream.");
        }
        finish();
    }

    private void fallbackToMain(String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
