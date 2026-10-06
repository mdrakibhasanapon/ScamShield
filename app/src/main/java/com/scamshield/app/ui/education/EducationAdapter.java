package com.scamshield.app.ui.education;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.scamshield.app.R;
import com.scamshield.app.data.model.EducationArticle;

import java.util.List;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.ViewHolder> {

    public interface OnArticleClickListener {
        void onArticleClick(EducationArticle article);
    }

    private final List<EducationArticle> articles;
    private final OnArticleClickListener listener;

    public EducationAdapter(List<EducationArticle> articles, OnArticleClickListener listener) {
        this.articles = articles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_education_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EducationArticle article = articles.get(position);
        holder.tvTitle.setText(article.getTitle());
        holder.tvSubtitle.setText(article.getSubtitle());
        holder.tvCategory.setText(article.getCategory());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onArticleClick(article);
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvTitle;
        TextView tvSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_edu_category);
            tvTitle = itemView.findViewById(R.id.tv_edu_title);
            tvSubtitle = itemView.findViewById(R.id.tv_edu_subtitle);
        }
    }
}
