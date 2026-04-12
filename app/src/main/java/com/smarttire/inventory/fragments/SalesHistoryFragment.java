// FILE: fragments/SalesHistoryFragment.java
// FIXED: JSON parsing was broken — it tried to unwrap a nested "data" object from inside
//        the already-extracted data, causing an empty list even when records exist.
//        The API returns: { "success": true, "data": [...] }
//        The old code did: response.get("data") → then AGAIN tried ((JSONObject) raw).optJSONArray("data")
//        which is wrong when "data" is already a JSONArray at the top level.
//
//        Correct approach: response.optJSONArray("data") directly.

package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.SalesAdapter;
import com.smarttire.inventory.models.SaleRecord;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.DateUtils;
import com.smarttire.inventory.utils.SalesHistoryPdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesHistoryFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView       rvSales;
    private ProgressBar        progressBar;
    private View               layoutEmpty;
    private TextView           tvSummary, tvTotalRevenue, tvTotalDue;
    private TextInputEditText  etSearch;
    private MaterialButton     btnDateFilter, btnExport;
    private Chip               chipDateRange;

    private ApiService   api;
    private SalesAdapter adapter;
    private final List<SaleRecord> salesList = new ArrayList<>();

    private String startDate = "", endDate = "";
    private double currentTotalRevenue = 0;
    private double currentTotalDue     = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sales_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiService.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupSearch();

        swipeRefresh.setOnRefreshListener(this::loadSales);
        btnDateFilter.setOnClickListener(v -> showDatePicker());
        btnExport.setOnClickListener(v -> exportPdf());

        loadSales();
    }

    private void initViews(View v) {
        swipeRefresh   = v.findViewById(R.id.swipeRefresh);
        rvSales        = v.findViewById(R.id.rvSales);
        progressBar    = v.findViewById(R.id.progressBar);
        layoutEmpty    = v.findViewById(R.id.layoutEmpty);
        tvSummary      = v.findViewById(R.id.tvSalesSummary);
        tvTotalDue     = v.findViewById(R.id.tvSalesTotalDue);
        tvTotalRevenue = v.findViewById(R.id.tvSalesTotalRevenue);
        etSearch       = v.findViewById(R.id.etSearchSales);
        btnDateFilter  = v.findViewById(R.id.btnDateFilter);
        btnExport      = v.findViewById(R.id.btnExportSales);
        chipDateRange  = v.findViewById(R.id.chipDateRange);

        chipDateRange.setOnCloseIconClickListener(chipView -> {
            startDate = "";
            endDate   = "";
            chipDateRange.setVisibility(View.GONE);
            loadSales();
        });
    }

    private void setupRecyclerView() {
        adapter = new SalesAdapter(requireContext(), salesList);
        adapter.setOnSaleClickListener(sale -> {
            SaleDetailBottomSheet sheet = SaleDetailBottomSheet.newInstance(sale);
            sheet.show(getChildFragmentManager(), "sale_detail");
        });
        rvSales.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSales.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.filter(s.toString());
                updateSummaryFromAdapter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateSummaryFromAdapter() {
        double totalRev = 0, totalDue = 0;
        List<SaleRecord> filtered = adapter.getDisplayList();
        for (SaleRecord s : filtered) {
            totalRev += s.getTotalPrice();
            totalDue += s.getRemainingAmount();
        }
        currentTotalRevenue = totalRev;
        currentTotalDue     = totalDue;

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvSummary.setText(filtered.size() + " Sales");
        tvTotalRevenue.setText(fmt.format(totalRev));
        tvTotalDue.setText(fmt.format(totalDue));
    }

    private void loadSales() {
        if (!isAdded()) return;
        swipeRefresh.setRefreshing(true);
        progressBar.setVisibility(View.VISIBLE);

        api.getSalesHistory("", startDate, endDate, 0, "", 1, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("API_RESPONSE", "getSalesHistory → " + response.toString());
                if (!isAdded()) return;

                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                try {
                    if (!response.optBoolean("success", false)) {
                        String msg = response.optString("message", "Failed to load sales");
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        layoutEmpty.setVisibility(salesList.isEmpty() ? View.VISIBLE : View.GONE);
                        return;
                    }

                    // FIXED: The API returns { "success": true, "data": [...] }
                    // "data" is directly a JSONArray — do NOT try to re-unwrap it.
                    JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                    if (data == null) {
                        // Defensive: if somehow data is a JSONObject, try to get "data" inside it
                        JSONObject dataObj = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (dataObj != null) {
                            data = dataObj.optJSONArray("data");
                        }
                    }
                    if (data == null) {
                        data = new JSONArray(); // empty — nothing to show
                    }

                    salesList.clear();
                    currentTotalRevenue = 0;
                    currentTotalDue     = 0;

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.optJSONObject(i);
                        if (obj != null) {
                            SaleRecord record = SaleRecord.fromJSON(obj);
                            salesList.add(record);
                            currentTotalRevenue += record.getTotalPrice();
                            currentTotalDue     += record.getRemainingAmount();
                        }
                    }

                    adapter.updateData(salesList);

                    NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                    tvSummary.setText(salesList.size() + " Sales");
                    tvTotalRevenue.setText(fmt.format(currentTotalRevenue));
                    tvTotalDue.setText(fmt.format(currentTotalDue));

                    // Use server-side totals from meta if available (more accurate)
                    JSONObject meta = response.optJSONObject(ApiConfig.KEY_META);
                    if (meta != null) {
                        double serverRevenue = meta.optDouble("total_revenue", currentTotalRevenue);
                        double serverDue     = meta.optDouble("total_outstanding", currentTotalDue);
                        tvTotalRevenue.setText(fmt.format(serverRevenue));
                        tvTotalDue.setText(fmt.format(serverDue));
                    }

                    layoutEmpty.setVisibility(salesList.isEmpty() ? View.VISIBLE : View.GONE);

                } catch (Exception e) {
                    Log.e("SalesHistory", "Parsing error", e);
                    Toast.makeText(requireContext(), "Data error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Filter by Date")
                        .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = DateUtils.millisToApiDate(selection.first);
            endDate   = DateUtils.millisToApiDate(selection.second);
            chipDateRange.setText(startDate + " - " + endDate);
            chipDateRange.setVisibility(View.VISIBLE);
            loadSales();
        });
        picker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void exportPdf() {
        List<SaleRecord> listToExport = adapter.getDisplayList();
        if (listToExport.isEmpty()) {
            Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        String range = startDate.isEmpty() ? "All Time" : (startDate + " to " + endDate);
        SalesHistoryPdfGenerator gen = new SalesHistoryPdfGenerator(requireContext());
        File file = gen.generate(listToExport, range, currentTotalRevenue, currentTotalDue);
        if (file != null) gen.openPdf(file);
        else Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
    }
}