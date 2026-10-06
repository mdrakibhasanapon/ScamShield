package com.scamshield.app.ui.result;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.scamshield.app.R;
import com.scamshield.app.data.model.DetectedIndicator;

import java.util.List;

public class IndicatorAdapter extends RecyclerView.Adapter<IndicatorAdapter.ViewHolder> {

    private final List<DetectedIndicator> indicators;

    public IndicatorAdapter(List<DetectedIndicator> indicators) {
        this.indicators = indicators;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detected_indicator, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetectedIndicator indicator = indicators.get(position);
        holder.tvTitle.setText(indicator.getTitle());
        holder.tvExplanation.setText(indicator.getExplanation());
        holder.tvCategory.setText(indicator.getCategory().getDisplayName());

        // Severity Badge Styling
        int badgeColor;
        switch (indicator.getSeverity()) {
            case CRITICAL:
                badgeColor = R.color.severity_critical;
                holder.tvSeverity.setText("HIGH SEVERITY");
                break;
            case WARNING:
                badgeColor = R.color.severity_warning;
                holder.tvSeverity.setText("SUSPICIOUS");
                break;
            default:
                badgeColor = R.color.severity_info;
                holder.tvSeverity.setText("INFORMATIONAL");
                break;
        }
        holder.tvSeverity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), badgeColor));

        if (indicator.getEvidenceSnippet() != null && !indicator.getEvidenceSnippet().isEmpty()) {
            holder.tvEvidence.setVisibility(View.VISIBLE);
            holder.tvEvidence.setText("Detected pattern: \"" + indicator.getEvidenceSnippet() + "\"");
        } else {
            holder.tvEvidence.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return indicators.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSeverity;
        TextView tvCategory;
        TextView tvTitle;
        TextView tvExplanation;
        TextView tvEvidence;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeverity = itemView.findViewById(R.id.tv_indicator_severity);
            tvCategory = itemView.findViewById(R.id.tv_indicator_category);
            tvTitle = itemView.findViewById(R.id.tv_indicator_title);
            tvExplanation = itemView.findViewById(R.id.tv_indicator_explanation);
            tvEvidence = itemView.findViewById(R.id.tv_indicator_evidence);
        }
    }
}
