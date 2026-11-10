package com.scamshield.app.ui.emergency;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.scamshield.app.R;

public class EmergencyLockdownActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_lockdown);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Open network/airplane mode settings
        findViewById(R.id.btn_open_airplane_mode).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });

        // Direct phone dialing
        findViewById(R.id.btn_call_999).setOnClickListener(v -> dialNumber("999"));
        findViewById(R.id.btn_call_btrc).setOnClickListener(v -> dialNumber("13219"));
        findViewById(R.id.btn_call_bkash).setOnClickListener(v -> dialNumber("16247"));
        findViewById(R.id.btn_call_nagad).setOnClickListener(v -> dialNumber("16167"));
    }

    private void dialNumber(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
