// FILE: fragments/SalesHistoryFragment.java  (FINAL — full payment fields flow)
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

public class SalesHistoryFragment extends Fragment {

    private TextInputEditText    etSearch;
    private MaterialButton       btnDateFilter, btnClearFilter;
    private AutoCompleteTextView spinnerCompanyFilter;
    private TextView             tvSummary, tvTotalRevenue;
    private SwipeRefreshLayout   swipeRefresh;
    private RecyclerView         rvSales;
    private LinearLayout         layoutEmpty;
    private ProgressBar          progressBar;
    private Chip                 chipDateRange;

    private ApiService           apiService;
    private SalesAdapter         adapter;
    private final List<SaleRecord> salesList = new ArrayList<>();

    private String filterSearch    = "";
    private String filterStartDate = "";
    private String filterEndDate   = "";
    private int    currentPage     = 1;
    private boolean isLoading      = false;
    private boolean hasMorePages   = true;

    public static SalesHistoryFragment newInstance() { return new SalesHistoryFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup g, Bundle s) {
        return i.inflate(R.layout.fragment_sales_history, g, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiService.getInstance(requireContext());
        initViews(view);
        setupRV();
        setupSearch();
        swipeRefresh.setOnRefreshListener(this::refreshData);
        if (btnDateFilter  != null) btnDateFilter.setOnClickListener(v -> showDatePicker());
        if (btnClearFilter != null) btnClearFilter.setOnClickListener(v -> clearFilters());
        if (spinnerCompanyFilter != null) spinnerCompanyFilter.setOnItemClickListener((p,v,pos,id) -> {
            String sel = pos == 0 ? "" : (String) p.getItemAtPosition(pos);
            adapter.filter(sel.isEmpty() ? filterSearch : sel);
            updateSummaryBar();
        });
        loadSales(true);
    }

    private void initViews(View v) {
        etSearch            = v.findViewById(R.id.etSearchSales);
        btnDateFilter       = v.findViewById(R.id.btnDateFilter);
        btnClearFilter      = v.findViewById(R.id.btnClearFilter);
        spinnerCompanyFilter= v.findViewById(R.id.spinnerCompanyFilter);
        tvSummary           = v.findViewById(R.id.tvSalesSummary);
        tvTotalRevenue      = v.findViewById(R.id.tvSalesTotalRevenue);
        swipeRefresh        = v.findViewById(R.id.swipeRefresh);
        rvSales             = v.findViewById(R.id.rvSales);
        layoutEmpty         = v.findViewById(R.id.layoutEmpty);
        progressBar         = v.findViewById(R.id.progressBar);
        chipDateRange       = v.findViewById(R.id.chipDateRange);
        if (chipDateRange != null) chipDateRange.setVisibility(View.GONE);
    }

    private void setupRV() {
        adapter = new SalesAdapter(requireContext(), salesList);
        LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        rvSales.setLayoutManager(llm);
        rvSales.setAdapter(adapter);

        rvSales.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMorePages) return;
                if (llm.findLastVisibleItemPosition() >= llm.getItemCount() - 4) loadSales(false);
            }
        });

        // SaleRecord now carries all payment fields → no extra map needed
        adapter.setOnSaleClickListener(sale ->
                SaleDetailBottomSheet.newInstance(sale)
                        .show(getChildFragmentManager(), "SaleDetail"));
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){
                filterSearch = s.toString().trim();
                adapter.filter(filterSearch);
                updateSummaryBar();
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long,Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker().setTitleText("Filter by date").build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel.first == null || sel.second == null) return;
            filterStartDate = DateUtils.millisToApiDate(sel.first);
            filterEndDate   = DateUtils.millisToApiDate(sel.second);
            if (chipDateRange != null) {
                chipDateRange.setText(DateUtils.millisToDisplay(sel.first)
                        + " → " + DateUtils.millisToDisplay(sel.second));
                chipDateRange.setVisibility(View.VISIBLE);
            }
            if (btnClearFilter != null) btnClearFilter.setVisibility(View.VISIBLE);
            refreshData();
        });
        picker.show(getParentFragmentManager(), "DatePicker");
    }

    private void loadSales(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        if (reset) {
            currentPage = 1; hasMorePages = true;
            salesList.clear(); adapter.updateData(salesList);
            progressBar.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }

        apiService.getSalesHistory(filterSearch, filterStartDate, filterEndDate,
                currentPage, new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject response) {
                        if (!isAdded()) return;
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        try {
                            if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                                JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                                List<SaleRecord> page = new ArrayList<>();
                                for (int i = 0; i < data.length(); i++)
                                    page.add(SaleRecord.fromJSON(data.getJSONObject(i)));

                                if (reset) { salesList.clear(); salesList.addAll(page); adapter.updateData(salesList); updateCompanyDropdown(); }
                                else       { salesList.addAll(page); adapter.appendData(page); }

                                hasMorePages = data.length() == 20;
                                currentPage++;
                                updateSummaryBar();
                                updateEmptyState();

                                if (response.has("meta")) {
                                    JSONObject m = response.getJSONObject("meta");
                                    if (tvSummary != null)     tvSummary.setText(m.optInt("total_count", salesList.size()) + " sales");
                                    if (tvTotalRevenue != null) tvTotalRevenue.setText("₹ " +
                                            String.format(java.util.Locale.getDefault(), "%,.2f", m.optDouble("total_revenue", 0.0)));
                                }
                            } else {
                                Toast.makeText(requireContext(),
                                        response.optString(ApiConfig.KEY_MESSAGE,"Error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onError(String error) {
                        if (!isAdded()) return;
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void refreshData() { if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true); loadSales(true); }
    private void updateEmptyState() { if (layoutEmpty != null) layoutEmpty.setVisibility(salesList.isEmpty()?View.VISIBLE:View.GONE); }
    private void updateSummaryBar() { int n=adapter.getDisplayCount(); if(tvSummary!=null) tvSummary.setText(n+" sale"+(n==1?"":"s")+" shown"); }
    private void updateCompanyDropdown() {
        java.util.Set<String> s = new java.util.LinkedHashSet<>();
        s.add("All Companies");
        for (SaleRecord r : salesList) s.add(r.getCompanyName());
        if (spinnerCompanyFilter != null)
            spinnerCompanyFilter.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, new ArrayList<>(s)));
    }
    private void clearFilters() {
        filterStartDate=""; filterEndDate="";
        if (etSearch != null) etSearch.setText("");
        if (chipDateRange != null) chipDateRange.setVisibility(View.GONE);
        if (btnClearFilter != null) btnClearFilter.setVisibility(View.GONE);
        if (spinnerCompanyFilter != null) spinnerCompanyFilter.setText("All Companies", false);
        refreshData();
    }
}