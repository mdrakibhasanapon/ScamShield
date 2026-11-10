package com.scamshield.app.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.scamshield.app.R;
import com.scamshield.app.data.model.TrustedContact;

import java.util.ArrayList;
import java.util.List;

public class TrustedContactAdapter extends RecyclerView.Adapter<TrustedContactAdapter.ViewHolder> {

    private final List<TrustedContact> contacts = new ArrayList<>();

    public void updateList(List<TrustedContact> newContacts) {
        contacts.clear();
        if (newContacts != null) {
            contacts.addAll(newContacts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trusted_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrustedContact contact = contacts.get(position);
        Context context = holder.itemView.getContext();

        holder.tvAvatarInitials.setText(contact.getInitials());
        holder.tvContactName.setText(contact.getName());
        holder.tvContactPhone.setText(contact.getPhoneNumber());

        if (contact.isSuspicious()) {
            holder.tvContactBadge.setText("⚠️ Toll Risk");
            holder.tvContactBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_high));
            holder.tvContactBadge.setBackgroundResource(R.drawable.badge_risk_high);
            holder.tvAvatarInitials.setBackgroundResource(R.drawable.badge_risk_high);
            holder.tvAvatarInitials.setTextColor(ContextCompat.getColor(context, R.color.risk_high));

            holder.tvSuspiciousReason.setText(contact.getSuspiciousReason());
            holder.tvSuspiciousReason.setVisibility(View.VISIBLE);
        } else {
            holder.tvContactBadge.setText("✓ Trusted Safe");
            holder.tvContactBadge.setTextColor(ContextCompat.getColor(context, R.color.risk_low));
            holder.tvContactBadge.setBackgroundResource(R.drawable.badge_risk_low);
            holder.tvAvatarInitials.setBackgroundResource(R.drawable.badge_risk_low);
            holder.tvAvatarInitials.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));

            holder.tvSuspiciousReason.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            try {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
                context.startActivity(dialIntent);
            } catch (Exception e) {
                Toast.makeText(context, contact.getName() + ": " + contact.getPhoneNumber(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatarInitials;
        final TextView tvContactName;
        final TextView tvContactPhone;
        final TextView tvContactBadge;
        final TextView tvSuspiciousReason;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitials = itemView.findViewById(R.id.tv_avatar_initials);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            tvContactPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvContactBadge = itemView.findViewById(R.id.tv_contact_badge);
            tvSuspiciousReason = itemView.findViewById(R.id.tv_suspicious_reason);
        }
    }
}
