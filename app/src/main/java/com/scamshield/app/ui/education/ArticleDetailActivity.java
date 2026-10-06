package com.scamshield.app.ui.education;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.scamshield.app.R;
import com.scamshield.app.data.model.EducationArticle;
import com.scamshield.app.data.repository.EducationRepository;

public class ArticleDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        String articleId = getIntent().getStringExtra("ARTICLE_ID");
        EducationArticle article = EducationRepository.getArticleById(articleId);

        if (article == null) {
            finish();
            return;
        }

        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvSubtitle = findViewById(R.id.tv_detail_subtitle);
        TextView tvSummary = findViewById(R.id.tv_detail_summary);
        TextView tvRedFlags = findViewById(R.id.tv_detail_red_flags);
        TextView tvWhatToDo = findViewById(R.id.tv_detail_what_to_do);
        TextView tvExample = findViewById(R.id.tv_detail_example);

        tvCategory.setText(article.getCategory());
        tvTitle.setText(article.getTitle());
        tvSubtitle.setText(article.getSubtitle());
        tvSummary.setText(article.getSummary());

        StringBuilder rf = new StringBuilder();
        for (String flag : article.getRedFlags()) {
            rf.append("• ").append(flag).append("\n\n");
        }
        tvRedFlags.setText(rf.toString().trim());

        StringBuilder wtd = new StringBuilder();
        for (String action : article.getWhatToDo()) {
            wtd.append("✓ ").append(action).append("\n\n");
        }
        tvWhatToDo.setText(wtd.toString().trim());

        tvExample.setText("\"" + article.getRealWorldExample() + "\"");
    }
}
