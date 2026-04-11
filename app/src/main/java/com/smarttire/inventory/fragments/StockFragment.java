// FILE: fragments/StockFragment.java  (OPTIMIZED — fixes ANR, RecyclerView lag, main thread blocking)
package com.smarttire.inventory.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.activities.StockDetailActivity;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.Company;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.StockPdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockFragment extends Fragment {

    private TextInputEditText    etSearch;
    private AutoCompleteTextView spinnerCompanyFilter;
    private SwipeRefreshLayout   swipeRefresh;
    private RecyclerView         rvStock;
    private LinearLayout         layoutEmpty;
    private ProgressBar          progressBar;
    private MaterialButton       btnExportPdf;

    private ApiService          api;
    private StockAdapter        adapter;

    // Master list (from server) + filtered list (displayed)
    private final List<Product> productList  = new ArrayList<>();
    private final List<Product> filteredList = new ArrayList<>();
    private final List<Company> companyList  = new ArrayList<>();

    private int filterCompanyId = 0;

    // Debounce search to avoid rapid adapter updates on every keystroke
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final int SEARCH_DEBOUNCE_MS = 300;

    // Background executor for PDF generation and heavy filtering
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static StockFragment newInstance() { return new StockFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiService.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupSearch();
        loadCompanies();
        swipeRefresh.setOnRefreshListener(this::loadProducts);
        loadProducts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel pending search debounce to avoid memory leaks
        if (searchDebounceRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
        }
        executor.shutdown();
    }

    private void initViews(View v) {
        etSearch             = v.findViewById(R.id.etSearch);
        spinnerCompanyFilter = v.findViewById(R.id.spinnerCompanyFilter);
        swipeRefresh         = v.findViewById(R.id.swipeRefresh);
        rvStock              = v.findViewById(R.id.rvStock);
        layoutEmpty          = v.findViewById(R.id.layoutEmpty);
        progressBar          = v.findViewById(R.id.progressBar);
        btnExportPdf         = v.findViewById(R.id.btnExportStockPdf);

        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v2 -> exportAllStockPdf());
        }
    }

    private void setupRecyclerView() {
        // Use a fixed size to avoid re-measuring the RecyclerView on every update
        rvStock.setHasFixedSize(true);

        // LinearLayoutManager with initial prefetch count for faster initial load
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setInitialPrefetchItemCount(8);
        rvStock.setLayoutManager(layoutManager);

        // Increase RecyclerView's recycled view pool for smoother scrolling
        rvStock.setRecycledViewPool(new RecyclerView.RecycledViewPool());
        rvStock.getRecycledViewPool().setMaxRecycledViews(0, 20);

        adapter = new StockAdapter(requireContext(), filteredList);
        rvStock.setAdapter(adapter);

        // Click → Stock Detail
        adapter.setOnItemClickListener(product -> {
            Intent intent = new Intent(requireContext(), StockDetailActivity.class);
            intent.putExtra(StockDetailActivity.EXTRA_PRODUCT_ID,   product.getId());
            intent.putExtra(StockDetailActivity.EXTRA_PRODUCT_NAME, product.getDisplayName());
            startActivity(intent);
        });

        adapter.setOnStockChangedListener(() -> {
            // No-op: lightweight callback, dashboard refreshes on resume
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                // Debounce: only filter after user stops typing for 300ms
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> applyFilterAsync(s.toString());
                searchDebounceHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void loadCompanies() {
        api.getCompanies(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject r) {
                if (!isAdded()) return;
                try {
                    if (r.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                        companyList.clear();
                        List<String> names = new ArrayList<>();
                        names.add("All Companies");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject o = data.getJSONObject(i);
                            companyList.add(new Company(o.getInt("id"), o.getString("name")));
                            names.add(o.getString("name"));
                        }
                        if (spinnerCompanyFilter != null) {
                            ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_dropdown_item_1line,
                                    names);
                            spinnerCompanyFilter.setAdapter(spinAdapter);
                            spinnerCompanyFilter.setText(names.get(0), false);
                            spinnerCompanyFilter.setOnItemClickListener((parent, view, pos, id) -> {
                                filterCompanyId = (pos == 0) ? 0 : companyList.get(pos - 1).getId();
                                String currentSearch = etSearch.getText() != null
                                        ? etSearch.getText().toString() : "";
                                applyFilterAsync(currentSearch);
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override public void onError(String err) {}
        });
    }

    private void loadProducts() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        layoutEmpty.setVisibility(View.GONE);

        api.getProductsSorted("", 0, "", "name", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject r) {
                if (!isAdded()) return;
                // Parse products on a background thread to avoid blocking the main thread
                executor.execute(() -> {
                    List<Product> newList = new ArrayList<>();
                    try {
                        if (r.optBoolean(ApiConfig.KEY_SUCCESS)) {
                            JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                            for (int i = 0; i < data.length(); i++) {
                                newList.add(parseProduct(data.getJSONObject(i)));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Apply initial filter off main thread
                    String currentSearch = etSearch.getText() != null
                            ? etSearch.getText().toString().toLowerCase().trim() : "";
                    List<Product> initialFiltered = filterProducts(newList, filterCompanyId, currentSearch);

                    // DiffUtil for smart, minimal adapter updates
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                            new ProductDiffCallback(filteredList, initialFiltered));

                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        productList.clear();
                        productList.addAll(newList);

                        filteredList.clear();
                        filteredList.addAll(initialFiltered);

                        diffResult.dispatchUpdatesTo(adapter);
                        layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                });
            }

            @Override
            public void onError(String err) {
                if (!isAdded()) return;
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Runs filtering on a background thread, then dispatches DiffUtil results on main thread.
     * This prevents jank when the product list is large.
     */
    private void applyFilterAsync(String rawQuery) {
        final String query = rawQuery.toLowerCase().trim();
        executor.execute(() -> {
            List<Product> filtered = filterProducts(productList, filterCompanyId, query);

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new ProductDiffCallback(filteredList, filtered));

            mainHandler.post(() -> {
                if (!isAdded()) return;
                filteredList.clear();
                filteredList.addAll(filtered);
                diffResult.dispatchUpdatesTo(adapter);
                layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    /** Pure function — safe to call from any thread. */
    private static List<Product> filterProducts(List<Product> source, int companyId, String query) {
        List<Product> result = new ArrayList<>();
        for (Product p : source) {
            if (companyId > 0 && p.getCompanyId() != companyId) continue;
            if (!query.isEmpty()) {
                boolean match = p.getCompanyName().toLowerCase().contains(query)
                        || p.getTireType().toLowerCase().contains(query)
                        || p.getTireSize().toLowerCase().contains(query)
                        || p.getModelName().toLowerCase().contains(query);
                if (!match) continue;
            }
            result.add(p);
        }
        return result;
    }

    private void exportAllStockPdf() {
        if (productList.isEmpty()) {
            Toast.makeText(requireContext(), "No stock data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Generating PDF…", Toast.LENGTH_SHORT).show();
        // Already background; no change needed here — keep consistent pattern
        executor.execute(() -> {
            StockPdfGenerator gen = new StockPdfGenerator(requireContext());
            File pdf = gen.generateAllStockPdf(new ArrayList<>(productList));
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (pdf != null) gen.openPdf(pdf);
                else Toast.makeText(requireContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private Product parseProduct(JSONObject o) throws Exception {
        Product p = new Product();
        p.setId(o.getInt("id"));
        p.setCompanyId(o.optInt("company_id"));
        p.setCompanyName(o.getString("company_name"));
        p.setModelName(o.optString("model_name", ""));
        p.setTireType(o.getString("tire_type"));
        p.setTireSize(o.getString("tire_size"));
        p.setQuantity(o.getInt("quantity"));
        p.setPrice(o.getDouble("price"));
        p.setCostPrice(o.optDouble("cost_price", 0.0));
        return p;
    }

    // ── DiffUtil Callback ────────────────────────────────────────────────────

    private static class ProductDiffCallback extends DiffUtil.Callback {
        private final List<Product> oldList;
        private final List<Product> newList;

        ProductDiffCallback(List<Product> oldList, List<Product> newList) {
            this.oldList = new ArrayList<>(oldList);
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId() == newList.get(newPos).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Product o = oldList.get(oldPos);
            Product n = newList.get(newPos);
            return o.getQuantity() == n.getQuantity()
                    && Double.compare(o.getPrice(), n.getPrice()) == 0
                    && Double.compare(o.getCostPrice(), n.getCostPrice()) == 0
                    && o.getModelName().equals(n.getModelName());
        }
    }
}