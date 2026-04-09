// FILE: fragments/SalesHistoryFragment.java (CLEAN INDUSTRY STANDARD)
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesHistoryFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvSales;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private TextView tvSummary, tvTotalRevenue;
    private TextInputEditText etSearch;
    private MaterialButton btnDateFilter, btnExport;
    private Chip chipDateRange;

    private ApiService api;
    private SalesAdapter adapter;
    private final List<SaleRecord> salesList = new ArrayList<>();
    
    private String startDate = "", endDate = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        
        loadSales();
    }

    private void initViews(View v) {
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        rvSales = v.findViewById(R.id.rvSales);
        progressBar = v.findViewById(R.id.progressBar);
        layoutEmpty = v.findViewById(R.id.layoutEmpty);
        tvSummary = v.findViewById(R.id.tvSalesSummary);
        tvTotalRevenue = v.findViewById(R.id.tvSalesTotalRevenue);
        etSearch = v.findViewById(R.id.etSearchSales);
        btnDateFilter = v.findViewById(R.id.btnDateFilter);
        btnExport = v.findViewById(R.id.btnExportSales);
        chipDateRange = v.findViewById(R.id.chipDateRange);
        
        chipDateRange.setOnCloseIconClickListener(view -> {
            startDate = ""; endDate = "";
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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSales() {
        swipeRefresh.setRefreshing(true);
        api.getSalesHistory("", startDate, endDate, 1, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                try {
                    if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                        salesList.clear();
                        double totalRev = 0;
                        for (int i = 0; i < data.length(); i++) {
                            SaleRecord record = SaleRecord.fromJSON(data.getJSONObject(i));
                            salesList.add(record);
                            totalRev += record.getTotalPrice();
                        }
                        adapter.updateData(salesList);
                        tvSummary.setText(salesList.size() + " Sales");
                        tvTotalRevenue.setText(NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(totalRev));
                        layoutEmpty.setVisibility(salesList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Filter by Date")
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = DateUtils.millisToApiDate(selection.first);
            endDate = DateUtils.millisToApiDate(selection.second);
            chipDateRange.setText(startDate + " - " + endDate);
            chipDateRange.setVisibility(View.VISIBLE);
            loadSales();
        });
        picker.show(getParentFragmentManager(), "DATE_PICKER");
    }
}