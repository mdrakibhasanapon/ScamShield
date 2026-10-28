package com.scamshield.app.ui.scan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.scamshield.app.R;
import com.scamshield.app.domain.scanner.ScannedThreatItem;
import com.scamshield.app.domain.scanner.ThreatSeverity;
import com.scamshield.app.domain.scanner.ThreatType;

import java.util.List;

public class ThreatItemAdapter extends RecyclerView.Adapter<ThreatItemAdapter.ViewHolder> {

    public interface OnThreatActionListener {
        void onActionClick(ScannedThreatItem item, int position);
    }

    private final List<ScannedThreatItem> items;
    private final OnThreatActionListener listener;

    public ThreatItemAdapter(List<ScannedThreatItem> items, OnThreatActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scanned_threat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScannedThreatItem item = items.get(position);
        Context context = holder.itemView.getContext();
        PackageManager pm = context.getPackageManager();

        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());
        holder.tvThreatName.setText(item.getThreatName());
        holder.tvDesc.setText(item.getDescription());

        // Severity Badge
        ThreatSeverity severity = item.getSeverity();
        holder.tvBadge.setText(severity.getLabel().toUpperCase() + " (" + item.getRiskScore() + "/100)");
        holder.tvBadge.setTextColor(ContextCompat.getColor(context, severity.getColorRes()));
        holder.tvBadge.setBackgroundColor(ContextCompat.getColor(context, severity.getBgRes()));

        // Icon Loading
        if (item.getType() == ThreatType.APP && item.getPackageName() != null) {
            try {
                Drawable appIcon = pm.getApplicationIcon(item.getPackageName());
                holder.ivIcon.setImageDrawable(appIcon);
            } catch (Exception e) {
                holder.ivIcon.setImageResource(R.drawable.ic_shield_check);
            }
        } else if (item.getType() == ThreatType.FILE) {
            holder.ivIcon.setImageResource(R.drawable.ic_action_screenshot);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, severity.getColorRes()));
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_nav_settings);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, severity.getColorRes()));
        }

        // Reasons bullets
        holder.layoutReasons.removeAllViews();
        if (item.getReasons() != null && !item.getReasons().isEmpty()) {
            for (String reason : item.getReasons()) {
                TextView bullet = new TextView(context);
                bullet.setText("• " + reason);
                bullet.setTextSize(12);
                bullet.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_light));
                bullet.setPadding(0, 2, 0, 2);
                holder.layoutReasons.addView(bullet);
            }
            holder.layoutReasons.setVisibility(View.VISIBLE);
        } else {
            holder.layoutReasons.setVisibility(View.GONE);
        }

        // Action button or Resolved state
        if (item.isResolved()) {
            holder.btnAction.setVisibility(View.GONE);
            holder.tvResolved.setVisibility(View.VISIBLE);
            holder.cardThreat.setStrokeColor(ContextCompat.getColor(context, R.color.risk_low));
        } else {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.tvResolved.setVisibility(View.GONE);
            holder.cardThreat.setStrokeColor(ContextCompat.getColor(context, severity.getColorRes()));

            if (item.getType() == ThreatType.APP) {
                holder.btnAction.setText("Uninstall App");
                holder.btnAction.setIconResource(R.drawable.ic_delete_trash);
                holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.risk_high));
                holder.btnAction.setIconTintResource(R.color.risk_high);
            } else if (item.getType() == ThreatType.FILE) {
                holder.btnAction.setText("Delete File");
                holder.btnAction.setIconResource(R.drawable.ic_delete_trash);
                holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.risk_high));
                holder.btnAction.setIconTintResource(R.color.risk_high);
            } else {
                holder.btnAction.setText("Fix in Settings");
                holder.btnAction.setIconResource(R.drawable.ic_nav_settings);
                holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
                holder.btnAction.setIconTintResource(R.color.brand_primary);
            }

            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(item, holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardThreat;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvBadge;
        TextView tvThreatName;
        TextView tvDesc;
        LinearLayout layoutReasons;
        TextView tvResolved;
        MaterialButton btnAction;

        ViewHolder(View itemView) {
            super(itemView);
            cardThreat = itemView.findViewById(R.id.card_threat_item);
            ivIcon = itemView.findViewById(R.id.iv_threat_icon);
            tvTitle = itemView.findViewById(R.id.tv_threat_title);
            tvSubtitle = itemView.findViewById(R.id.tv_threat_subtitle);
            tvBadge = itemView.findViewById(R.id.tv_threat_badge);
            tvThreatName = itemView.findViewById(R.id.tv_threat_name);
            tvDesc = itemView.findViewById(R.id.tv_threat_desc);
            layoutReasons = itemView.findViewById(R.id.layout_threat_reasons);
            tvResolved = itemView.findViewById(R.id.tv_threat_resolved);
            btnAction = itemView.findViewById(R.id.btn_threat_action);
        }
    }
}
