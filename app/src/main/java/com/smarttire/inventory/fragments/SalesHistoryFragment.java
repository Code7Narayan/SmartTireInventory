// FILE: app/src/main/java/com/smarttire/inventory/fragments/SalesHistoryFragment.java
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the full chronological list of sales.
 *
 * Features:
 *  • Live search (company / tire type / size)
 *  • Date-range picker using MaterialDatePicker
 *  • Company filter chip
 *  • Pull-to-refresh
 *  • Summary bar (total sales count + revenue)
 */
public class SalesHistoryFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText       etSearch;
    private MaterialButton          btnDateFilter, btnClearFilter;
    private AutoCompleteTextView    spinnerCompanyFilter;
    private TextView                tvSummary, tvTotalRevenue;
    private SwipeRefreshLayout      swipeRefresh;
    private RecyclerView            rvSales;
    private LinearLayout            layoutEmpty;
    private ProgressBar             progressBar;
    private Chip                    chipDateRange;

    // ── State ─────────────────────────────────────────────────────────────────
    private ApiService              apiService;
    private SalesAdapter            adapter;
    private final List<SaleRecord>  salesList = new ArrayList<>();

    private String  filterSearch    = "";
    private String  filterStartDate = "";
    private String  filterEndDate   = "";
    private String  filterCompany   = "";
    private int     currentPage     = 1;
    private boolean isLoading       = false;
    private boolean hasMorePages    = true;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static SalesHistoryFragment newInstance() { return new SalesHistoryFragment(); }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sales_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiService.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupDateFilter();
        setupCompanyFilter();
        swipeRefresh.setOnRefreshListener(this::refreshData);
        loadSales(true);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initViews(View v) {
        etSearch         = v.findViewById(R.id.etSearchSales);
        btnDateFilter    = v.findViewById(R.id.btnDateFilter);
        btnClearFilter   = v.findViewById(R.id.btnClearFilter);
        spinnerCompanyFilter = v.findViewById(R.id.spinnerCompanyFilter);
        tvSummary        = v.findViewById(R.id.tvSalesSummary);
        tvTotalRevenue   = v.findViewById(R.id.tvSalesTotalRevenue);
        swipeRefresh     = v.findViewById(R.id.swipeRefresh);
        rvSales          = v.findViewById(R.id.rvSales);
        layoutEmpty      = v.findViewById(R.id.layoutEmpty);
        progressBar      = v.findViewById(R.id.progressBar);
        chipDateRange    = v.findViewById(R.id.chipDateRange);
    }

    private void setupRecyclerView() {
        adapter = new SalesAdapter(requireContext(), salesList);
        LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        rvSales.setLayoutManager(llm);
        rvSales.setAdapter(adapter);

        // Infinite scroll — load next page when near bottom
        rvSales.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMorePages) return;
                int total    = llm.getItemCount();
                int visible  = llm.findLastVisibleItemPosition();
                if (visible >= total - 4) loadSales(false);
            }
        });

        // Click → invoice dialog
        adapter.setOnSaleClickListener(sale ->
                SaleDetailBottomSheet.newInstance(sale)
                        .show(getChildFragmentManager(), "SaleDetail"));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterSearch = s.toString().trim();
                adapter.filter(filterSearch);
                updateSummaryBar();
            }
        });
    }

    private void setupDateFilter() {
        btnDateFilter.setOnClickListener(v -> showDateRangePicker());
        btnClearFilter.setOnClickListener(v -> clearFilters());
        if (chipDateRange != null) chipDateRange.setVisibility(View.GONE);
    }

    private void setupCompanyFilter() {
        // Company list is populated after first load via updateCompanyDropdown()
        spinnerCompanyFilter.setOnItemClickListener((parent, view, pos, id) -> {
            filterCompany = pos == 0 ? "" : (String) parent.getItemAtPosition(pos);
            refreshData();
        });
    }

    // ── Date Picker ───────────────────────────────────────────────────────────

    private void showDateRangePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Filter by date range")
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first == null || selection.second == null) return;
            filterStartDate = DateUtils.millisToApiDate(selection.first);
            filterEndDate   = DateUtils.millisToApiDate(selection.second);

            String label = DateUtils.millisToDisplay(selection.first)
                    + "  →  " + DateUtils.millisToDisplay(selection.second);
            if (chipDateRange != null) {
                chipDateRange.setText(label);
                chipDateRange.setVisibility(View.VISIBLE);
            }
            btnClearFilter.setVisibility(View.VISIBLE);
            refreshData();
        });

        picker.show(getParentFragmentManager(), "DateRangePicker");
    }

    // ── Load Data ─────────────────────────────────────────────────────────────

    private void loadSales(boolean reset) {
        if (isLoading) return;
        isLoading = true;

        if (reset) {
            currentPage  = 1;
            hasMorePages = true;
            salesList.clear();
            adapter.updateData(salesList);
            progressBar.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }

        apiService.getSalesHistory(filterSearch, filterStartDate, filterEndDate,
                currentPage, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                try {
                    if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                        List<SaleRecord> page = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            page.add(SaleRecord.fromJSON(data.getJSONObject(i)));
                        }

                        if (reset) {
                            salesList.clear();
                            salesList.addAll(page);
                            adapter.updateData(salesList);
                            updateCompanyDropdown();
                        } else {
                            salesList.addAll(page);
                            adapter.appendData(page);
                        }

                        hasMorePages = data.length() == 20; // page size = 20
                        currentPage++;
                        updateSummaryBar();
                        updateEmptyState();

                        // Parse totals from response metadata
                        if (response.has("meta")) {
                            JSONObject meta = response.getJSONObject("meta");
                            tvSummary.setText(meta.optInt("total_count", salesList.size()) + " sales");
                            tvTotalRevenue.setText("₹ " +
                                    String.format(java.util.Locale.getDefault(),
                                            "%,.2f", meta.optDouble("total_revenue", 0.0)));
                        }

                    } else {
                        Toast.makeText(requireContext(),
                                response.optString(ApiConfig.KEY_MESSAGE, "Error"),
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void refreshData() {
        if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);
        loadSales(true);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updateEmptyState() {
        layoutEmpty.setVisibility(salesList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateSummaryBar() {
        int count = adapter.getDisplayCount();
        tvSummary.setText(count + " sale" + (count == 1 ? "" : "s") + " shown");
    }

    private void updateCompanyDropdown() {
        java.util.Set<String> companies = new java.util.LinkedHashSet<>();
        companies.add("All Companies");
        for (SaleRecord s : salesList) companies.add(s.getCompanyName());
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>(companies));
        spinnerCompanyFilter.setAdapter(a);
    }

    private void clearFilters() {
        filterStartDate = "";
        filterEndDate   = "";
        filterCompany   = "";
        etSearch.setText("");
        if (chipDateRange != null) chipDateRange.setVisibility(View.GONE);
        btnClearFilter.setVisibility(View.GONE);
        spinnerCompanyFilter.setText("All Companies", false);
        refreshData();
    }
}
