package com.scamshield.app.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.scamshield.app.R;
import com.scamshield.app.data.model.RiskLevel;
import com.scamshield.app.data.model.ScanHistoryItem;
import com.scamshield.app.data.repository.HistoryRepository;
import com.scamshield.app.ui.message.CheckMessageActivity;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnItemClickListener {

    private HistoryRepository historyRepository;
    private RecyclerView rvHistory;
    private View layoutEmpty;
    private HistoryAdapter adapter;
    private final List<ScanHistoryItem> currentList = new ArrayList<>();
    private RiskLevel activeFilter = null; // null = all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyRepository = new HistoryRepository(requireContext());
        rvHistory = view.findViewById(R.id.rv_history);
        layoutEmpty = view.findViewById(R.id.layout_history_empty);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(currentList, this);
        rvHistory.setAdapter(adapter);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_filters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.contains(R.id.chip_filter_all)) {
                activeFilter = null;
            } else if (checkedIds.contains(R.id.chip_filter_high)) {
                activeFilter = RiskLevel.HIGH_RISK;
            } else if (checkedIds.contains(R.id.chip_filter_suspicious)) {
                activeFilter = RiskLevel.SUSPICIOUS;
            } else if (checkedIds.contains(R.id.chip_filter_low)) {
                activeFilter = RiskLevel.LOW_RISK;
            }
            loadHistory();
        });

        view.findViewById(R.id.btn_clear_all).setOnClickListener(v -> showClearAllConfirmation());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        currentList.clear();
        List<ScanHistoryItem> results;
        if (activeFilter == null) {
            results = historyRepository.getAllScans();
        } else {
            results = historyRepository.getScansByRisk(activeFilter);
        }

        if (results.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            currentList.addAll(results);
            adapter.notifyDataSetChanged();
        }
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_clear_history_title)
                .setMessage(R.string.dialog_clear_history_msg)
                .setPositiveButton("Clear All", (dialog, which) -> {
                    historyRepository.deleteAllScans();
                    loadHistory();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onItemClick(ScanHistoryItem item) {
        Intent intent = new Intent(requireContext(), CheckMessageActivity.class);
        intent.putExtra("PREFILL_CONTENT", item.getSnippet());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(ScanHistoryItem item, int position) {
        historyRepository.deleteScan(item.getId());
        loadHistory();
    }
}
