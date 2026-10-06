package com.scamshield.app.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.scamshield.app.R;
import com.scamshield.app.ui.MainActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Preload Google Mobile Ads (AdMob) in background
        com.scamshield.app.ads.AdMobManager.getInstance().initialize(this);

        ImageView ivGraphic = findViewById(R.id.iv_splash_graphic);
        TextView tvStatus = findViewById(R.id.tv_splash_status);

        // Elegant fade-in entrance
        AlphaAnimation fadeIn = new AlphaAnimation(0.3f, 1.0f);
        fadeIn.setDuration(700);
        ivGraphic.startAnimation(fadeIn);

        // Real-time security engine status simulation
        handler.postDelayed(() -> {
            if (!isFinishing() && tvStatus != null) {
                tvStatus.setText("Defense modules active & ready");
            }
        }, 750);

        // Navigate cleanly to Main Dashboard
        navigateRunnable = () -> {
            if (!isFinishing()) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        };

        handler.postDelayed(navigateRunnable, 1400);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navigateRunnable != null) {
            handler.removeCallbacks(navigateRunnable);
        }
    }
}
