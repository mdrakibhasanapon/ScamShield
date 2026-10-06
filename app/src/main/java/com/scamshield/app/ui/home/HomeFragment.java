package com.scamshield.app.ui.home;

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
import com.scamshield.app.data.model.ScanHistoryItem;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.ui.history.HistoryAdapter;
import com.scamshield.app.ui.message.CheckMessageActivity;
import com.scamshield.app.ui.qr.QrScanActivity;
import com.scamshield.app.ui.screenshot.ScreenshotAnalysisActivity;
import com.scamshield.app.ui.url.CheckUrlActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements HistoryAdapter.OnItemClickListener {

    private HistoryRepository historyRepository;
    private RecyclerView rvRecentScans;
    private View layoutEmptyScans;
    private HistoryAdapter historyAdapter;
    private final List<ScanHistoryItem> recentScansList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        historyRepository = new HistoryRepository(requireContext());

        rvRecentScans = view.findViewById(R.id.rv_recent_scans);
        layoutEmptyScans = view.findViewById(R.id.layout_empty_scans);

        rvRecentScans.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new HistoryAdapter(recentScansList, this);
        rvRecentScans.setAdapter(historyAdapter);

        // Action Buttons Setup
        view.findViewById(R.id.card_action_message).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CheckMessageActivity.class));
        });

        view.findViewById(R.id.card_action_link).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CheckUrlActivity.class));
        });

        view.findViewById(R.id.card_action_screenshot).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ScreenshotAnalysisActivity.class));
        });

        view.findViewById(R.id.card_action_qr).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), QrScanActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentScans();
    }

    private void loadRecentScans() {
        recentScansList.clear();
        List<ScanHistoryItem> items = historyRepository.getRecentScans(5);
        if (items.isEmpty()) {
            layoutEmptyScans.setVisibility(View.VISIBLE);
            rvRecentScans.setVisibility(View.GONE);
        } else {
            layoutEmptyScans.setVisibility(View.GONE);
            rvRecentScans.setVisibility(View.VISIBLE);
            recentScansList.addAll(items);
            historyAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(ScanHistoryItem item) {
        // Can open message or link checker pre-filled or review details
        Intent intent = new Intent(requireContext(), CheckMessageActivity.class);
        intent.putExtra("PREFILL_CONTENT", item.getSnippet());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(ScanHistoryItem item, int position) {
        historyRepository.deleteScan(item.getId());
        loadRecentScans();
    }
}
