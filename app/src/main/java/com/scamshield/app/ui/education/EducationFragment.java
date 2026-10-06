package com.scamshield.app.ui.education;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scamshield.app.R;
import com.scamshield.app.data.model.EducationArticle;
import com.scamshield.app.data.repository.EducationRepository;

import java.util.List;

public class EducationFragment extends Fragment implements EducationAdapter.OnArticleClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_education, container, false);

        RecyclerView rvEducation = view.findViewById(R.id.rv_education);
        rvEducation.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<EducationArticle> articles = EducationRepository.getArticles();
        EducationAdapter adapter = new EducationAdapter(articles, this);
        rvEducation.setAdapter(adapter);

        return view;
    }

    @Override
    public void onArticleClick(EducationArticle article) {
        Intent intent = new Intent(requireContext(), ArticleDetailActivity.class);
        intent.putExtra("ARTICLE_ID", article.getId());
        startActivity(intent);
    }
}
