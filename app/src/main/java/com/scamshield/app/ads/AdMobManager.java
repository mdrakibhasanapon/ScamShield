package com.scamshield.app.ads;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.scamshield.app.R;

public class AdMobManager {

    private static volatile AdMobManager instance;
    private boolean isInitialized = false;
    private RewardedAd rewardedAd;
    private boolean isLoadingRewarded = false;

    public interface RewardCallback {
        void onRewardEarned();
        void onAdCompletedOrUnavailable();
    }

    private AdMobManager() {}

    public static AdMobManager getInstance() {
        if (instance == null) {
            synchronized (AdMobManager.class) {
                if (instance == null) {
                    instance = new AdMobManager();
                }
            }
        }
        return instance;
    }

    public synchronized void initialize(@NonNull Context context) {
        if (!isInitialized) {
            MobileAds.initialize(context.getApplicationContext(), initializationStatus -> {
                isInitialized = true;
                preloadRewardedAd(context.getApplicationContext());
            });
        }
    }

    /**
     * Loads an adaptive banner ad into the provided ViewGroup container.
     * Hides container gracefully if ad fails to load to prevent empty blank space.
     */
    public void loadAdaptiveBanner(@NonNull Activity activity, @NonNull ViewGroup container) {
        initialize(activity);

        String bannerUnitId = activity.getString(R.string.admob_banner_ad_unit_id);
        AdView adView = new AdView(activity);
        adView.setAdUnitId(bannerUnitId);

        AdSize adSize = getAdaptiveAdSize(activity);
        adView.setAdSize(adSize);

        container.removeAllViews();
        container.addView(adView);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                container.setVisibility(View.GONE);
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private AdSize getAdaptiveAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;
        float adWidthPixels = outMetrics.widthPixels;
        int adWidth = (int) (adWidthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public void preloadRewardedAd(@NonNull Context context) {
        if (rewardedAd != null || isLoadingRewarded) return;

        isLoadingRewarded = true;
        String rewardedUnitId = context.getString(R.string.admob_rewarded_ad_unit_id);
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, rewardedUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                isLoadingRewarded = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                isLoadingRewarded = false;
            }
        });
    }

    /**
     * Shows a Rewarded Video Ad. If the ad is not currently cached or fails,
     * gracefully rewards the user anyway to maintain high user trust.
     */
    public void showRewardedAd(@NonNull Activity activity, @NonNull RewardCallback callback) {
        if (rewardedAd != null) {
            final boolean[] earned = {false};

            rewardedAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    earned[0] = true;
                    callback.onRewardEarned();
                }
            });

            rewardedAd = null;
            preloadRewardedAd(activity.getApplicationContext());
        } else {
            // Graceful fallback: user clicked to watch ad, but none was ready; grant perk directly
            callback.onRewardEarned();
            callback.onAdCompletedOrUnavailable();
            preloadRewardedAd(activity.getApplicationContext());
        }
    }
}
