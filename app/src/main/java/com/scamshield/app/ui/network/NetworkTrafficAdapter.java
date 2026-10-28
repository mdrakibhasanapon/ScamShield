package com.scamshield.app.ui.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.scamshield.app.R;
import com.scamshield.app.data.model.NetworkTrafficItem;
import com.scamshield.app.data.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;

public class NetworkTrafficAdapter extends RecyclerView.Adapter<NetworkTrafficAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NetworkTrafficItem item);
    }

    private final List<NetworkTrafficItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public NetworkTrafficAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<NetworkTrafficItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network_traffic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAppIcon;
        private final TextView tvAppName;
        private final TextView tvPackageName;
        private final TextView tvRiskBadge;
        private final TextView tvDirectionBadge;
        private final TextView tvProtocolBadge;
        private final TextView tvTimestamp;
        private final TextView tvDestinationHost;
        private final TextView tvThreatReason;
        private final TextView tvRecommendedAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            tvRiskBadge = itemView.findViewById(R.id.tv_risk_badge);
            tvDirectionBadge = itemView.findViewById(R.id.tv_direction_badge);
            tvProtocolBadge = itemView.findViewById(R.id.tv_protocol_badge);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvDestinationHost = itemView.findViewById(R.id.tv_destination_host);
            tvThreatReason = itemView.findViewById(R.id.tv_threat_reason);
            tvRecommendedAction = itemView.findViewById(R.id.tv_recommended_action);
        }

        public void bind(NetworkTrafficItem item, OnItemClickListener listener) {
            Context context = itemView.getContext();

            tvAppName.setText(item.getAppName());
            tvPackageName.setText(item.getPackageName());

            // Try to load installed app icon
            try {
                PackageManager pm = context.getPackageManager();
                Drawable icon = pm.getApplicationIcon(item.getPackageName());
                ivAppIcon.setImageDrawable(icon);
                ivAppIcon.setImageTintList(null);
            } catch (Exception e) {
                ivAppIcon.setImageResource(R.drawable.ic_network_traffic);
                ivAppIcon.setColorFilter(ContextCompat.getColor(context, R.color.brand_primary));
            }

            // Risk badge
            tvRiskBadge.setText(item.getRiskScore() + "/100");
            if (item.getRiskLevel() == RiskLevel.HIGH_RISK) {
                tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_high);
            } else if (item.getRiskLevel() == RiskLevel.SUSPICIOUS) {
                tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_attention);
            } else {
                tvRiskBadge.setBackgroundResource(R.drawable.badge_risk_low);
            }

            // Direction badge
            if (item.getDirection() == NetworkTrafficItem.Direction.UPLOAD) {
                tvDirectionBadge.setText("⬆️ " + NetworkTrafficItem.formatBytes(item.getBytesUploaded()));
                tvDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_high));
            } else if (item.getDirection() == NetworkTrafficItem.Direction.DOWNLOAD) {
                tvDirectionBadge.setText("⬇️ " + NetworkTrafficItem.formatBytes(item.getBytesDownloaded()));
                tvDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
            } else {
                tvDirectionBadge.setText("🔁 " + NetworkTrafficItem.formatBytes(item.getTotalBytes()));
                tvDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary));
            }

            // Protocol
            tvProtocolBadge.setText(item.getProtocol());

            // Timestamp
            CharSequence relTime = DateUtils.getRelativeTimeSpanString(
                    item.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            tvTimestamp.setText(relTime);

            // Destination
            tvDestinationHost.setText("🌐 Destination: " + item.getDestinationHost() + " (" + item.getDestinationIp() + ":" + item.getDestinationPort() + ")");

            // Reason
            tvThreatReason.setText(item.getWhyFlagged());

            // Recommended Action
            if (item.getRecommendedAction() != null && !item.getRecommendedAction().isEmpty()) {
                tvRecommendedAction.setVisibility(View.VISIBLE);
                tvRecommendedAction.setText("Action: " + item.getRecommendedAction());
            } else {
                tvRecommendedAction.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}
