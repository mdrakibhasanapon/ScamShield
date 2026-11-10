package com.scamshield.app.ui.privacy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.scamshield.app.R;
import com.scamshield.app.data.model.AppPrivacyProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppPrivacyAdapter extends RecyclerView.Adapter<AppPrivacyAdapter.ViewHolder> {

    public interface OnActionListener {
        void onManageApp(AppPrivacyProfile profile);
        void onUninstallApp(AppPrivacyProfile profile);
    }

    private final List<AppPrivacyProfile> profiles = new ArrayList<>();
    private final OnActionListener listener;
    private final ExecutorService iconExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AppPrivacyAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<AppPrivacyProfile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) {
            profiles.addAll(newProfiles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_privacy, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppPrivacyProfile profile = profiles.get(position);
        Context context = holder.itemView.getContext();
        PackageManager pm = context.getPackageManager();

        holder.tvAppName.setText(profile.getAppName());

        String sourceText = profile.isSystemApp()
                ? "Verified System App"
                : (profile.isSideloaded() ? "Sideloaded / Unknown Store" : "Store Installed");
        holder.tvPkgName.setText(profile.getPackageName() + " • " + sourceText);

        // Async load icon
        holder.ivAppIcon.setImageResource(R.drawable.ic_shield_check);
        iconExecutor.execute(() -> {
            try {
                Drawable icon = pm.getApplicationIcon(profile.getPackageName());
                mainHandler.post(() -> holder.ivAppIcon.setImageDrawable(icon));
            } catch (Exception ignored) {}
        });

        // Risk Badge
        if (profile.isPotentialSpyware()) {
            holder.tvRiskBadge.setText("🚨 Spyware Threat");
            holder.tvRiskBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_high));
            holder.tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_high);
        } else {
            switch (profile.getRiskSeverity()) {
                case CRITICAL:
                case HIGH:
                    holder.tvRiskBadge.setText("⚠️ High Privilege");
                    holder.tvRiskBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_high));
                    holder.tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_high);
                    break;
                case MEDIUM:
                    holder.tvRiskBadge.setText("Suspicious");
                    holder.tvRiskBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_suspicious));
                    holder.tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_attention);
                    break;
                case LOW:
                    holder.tvRiskBadge.setText("Elevated");
                    holder.tvRiskBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
                    holder.tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_attention);
                    break;
                default:
                    holder.tvRiskBadge.setText("Verified");
                    holder.tvRiskBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_low));
                    holder.tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_low);
                    break;
            }
        }

        // Permission Pills
        holder.pillMic.setVisibility(profile.hasMicrophone() ? View.VISIBLE : View.GONE);
        holder.pillLocation.setVisibility(profile.hasLocation() ? View.VISIBLE : View.GONE);
        holder.pillContacts.setVisibility(profile.hasContacts() ? View.VISIBLE : View.GONE);
        holder.pillCamera.setVisibility(profile.hasCamera() ? View.VISIBLE : View.GONE);
        holder.pillAudio.setVisibility(profile.hasAudioPhone() ? View.VISIBLE : View.GONE);

        // Explanation Reason
        if (!profile.getRiskReasons().isEmpty()) {
            holder.tvRiskReason.setText(profile.getRiskReasons().get(0));
            holder.tvRiskReason.setVisibility(View.VISIBLE);
        } else {
            holder.tvRiskReason.setVisibility(View.GONE);
        }

        // Manage App Button
        holder.btnManageApp.setOnClickListener(v -> {
            if (listener != null) {
                listener.onManageApp(profile);
            } else {
                openAppDetails(context, profile.getPackageName());
            }
        });

        // Uninstall Button
        if (profile.isSystemApp()) {
            holder.btnUninstallApp.setVisibility(View.GONE);
        } else {
            holder.btnUninstallApp.setVisibility(View.VISIBLE);
            holder.btnUninstallApp.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUninstallApp(profile);
                } else {
                    uninstallApp(context, profile.getPackageName());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    private void openAppDetails(Context context, String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", packageName, null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open app settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uninstallApp(Context context, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.fromParts("package", packageName, null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot prompt uninstall: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivAppIcon;
        final TextView tvAppName;
        final TextView tvPkgName;
        final TextView tvRiskBadge;
        final TextView pillMic;
        final TextView pillLocation;
        final TextView pillContacts;
        final TextView pillCamera;
        final TextView pillAudio;
        final TextView tvRiskReason;
        final MaterialButton btnManageApp;
        final MaterialButton btnUninstallApp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvPkgName = itemView.findViewById(R.id.tv_pkg_name);
            tvRiskBadge = itemView.findViewById(R.id.tv_risk_badge);
            pillMic = itemView.findViewById(R.id.pill_mic);
            pillLocation = itemView.findViewById(R.id.pill_location);
            pillContacts = itemView.findViewById(R.id.pill_contacts);
            pillCamera = itemView.findViewById(R.id.pill_camera);
            pillAudio = itemView.findViewById(R.id.pill_audio);
            tvRiskReason = itemView.findViewById(R.id.tv_risk_reason);
            btnManageApp = itemView.findViewById(R.id.btn_manage_app);
            btnUninstallApp = itemView.findViewById(R.id.btn_uninstall_app);
        }
    }
}
