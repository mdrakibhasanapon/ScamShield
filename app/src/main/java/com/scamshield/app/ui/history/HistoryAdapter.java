package com.scamshield.app.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.scamshield.app.R;
import com.scamshield.app.data.model.ScanHistoryItem;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ScanHistoryItem item);
        void onDeleteClick(ScanHistoryItem item, int position);
    }

    private final List<ScanHistoryItem> items;
    private final OnItemClickListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public HistoryAdapter(List<ScanHistoryItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanHistoryItem item = items.get(position);

        holder.tvSnippet.setText(item.getSnippet());
        holder.tvType.setText(item.getScanType().getLabel());
        holder.tvDate.setText(dateFormat.format(new Date(item.getTimestamp())));
        holder.tvScore.setText(item.getRiskScore() + "/100");

        holder.tvRiskBadge.setText(item.getRiskLevel().getTitle());
        int color = ContextCompat.getColor(holder.itemView.getContext(), item.getRiskLevel().getColorResId());
        holder.tvRiskBadge.setTextColor(color);
        holder.ivRiskIcon.setImageResource(item.getRiskLevel().getIconResId());
        holder.ivRiskIcon.setColorFilter(color);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRiskIcon;
        TextView tvRiskBadge;
        TextView tvScore;
        TextView tvType;
        TextView tvDate;
        TextView tvSnippet;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRiskIcon = itemView.findViewById(R.id.iv_history_risk_icon);
            tvRiskBadge = itemView.findViewById(R.id.tv_history_risk_badge);
            tvScore = itemView.findViewById(R.id.tv_history_score);
            tvType = itemView.findViewById(R.id.tv_history_type);
            tvDate = itemView.findViewById(R.id.tv_history_date);
            tvSnippet = itemView.findViewById(R.id.tv_history_snippet);
            btnDelete = itemView.findViewById(R.id.btn_history_delete);
        }
    }
}
