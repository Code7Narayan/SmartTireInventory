// FILE: fragments/StockFragment.java (Java 8 compatible)
package com.smarttire.inventory.fragments;

import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class StockFragment extends Fragment {

    private TextInputEditText    etSearch, etModelFilter, etSizeFilter;
    private AutoCompleteTextView spinnerCompanyFilter, spinnerSort;
    private SwipeRefreshLayout   swipeRefresh;
    private RecyclerView         rvStock;
    private LinearLayout         layoutEmpty;
    private ProgressBar          progressBar;
    private MaterialButton       btnExportPdf;

    private ApiService          api;
    private StockAdapter        adapter;
    private final List<Product> productList  = new ArrayList<>();
    private final List<Product> filteredList = new ArrayList<>();
    private final List<Company> companyList  = new ArrayList<>();

    private int    filterCompanyId = 0;
    private String filterModel     = "";
    private String filterSize      = "";
    private String currentSort     = "company"; // company|low_stock|date|name

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
        setupFilters();
        loadCompanies();
        swipeRefresh.setOnRefreshListener(this::loadProducts);
        loadProducts();
    }

    private void initViews(View v) {
        etSearch            = v.findViewById(R.id.etSearch);
        etModelFilter       = v.findViewById(R.id.etModelFilter);
        etSizeFilter        = v.findViewById(R.id.etSizeFilter);
        spinnerCompanyFilter= v.findViewById(R.id.spinnerCompanyFilter);
        spinnerSort         = v.findViewById(R.id.spinnerSort);
        swipeRefresh        = v.findViewById(R.id.swipeRefresh);
        rvStock             = v.findViewById(R.id.rvStock);
        layoutEmpty         = v.findViewById(R.id.layoutEmpty);
        progressBar         = v.findViewById(R.id.progressBar);
        btnExportPdf        = v.findViewById(R.id.btnExportStockPdf);

        // Sort options
        String[] sorts = {"Company (A-Z)", "Low Stock First", "Latest Added", "Model Name"};
        spinnerSort.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sorts));
        spinnerSort.setText(sorts[0], false);
        spinnerSort.setOnItemClickListener((parent, view12, pos, id) -> {
            switch (pos) {
                case 1:
                    currentSort = "low_stock";
                    break;
                case 2:
                    currentSort = "date";
                    break;
                case 3:
                    currentSort = "name";
                    break;
                default:
                    currentSort = "company";
                    break;
            }
            loadProducts();
        });

        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v2 -> exportAllStockPdf());
        }
    }

    private void setupRecyclerView() {
        adapter = new StockAdapter(requireContext(), filteredList);
        rvStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStock.setAdapter(adapter);

        // Click → Stock Detail
        adapter.setOnItemClickListener(product -> {
            Intent intent = new Intent(requireContext(), StockDetailActivity.class);
            intent.putExtra(StockDetailActivity.EXTRA_PRODUCT_ID,   product.getId());
            intent.putExtra(StockDetailActivity.EXTRA_PRODUCT_NAME, product.getDisplayName());
            startActivity(intent);
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyLocalFilter();
            }
        });
    }

    private void setupFilters() {
        etModelFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterModel = s.toString().trim();
                loadProducts();
            }
        });

        etSizeFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterSize = s.toString().trim();
                loadProducts();
            }
        });

        spinnerCompanyFilter.setOnItemClickListener((parent, view, pos, id) -> {
            filterCompanyId = pos == 0 ? 0 : companyList.get(pos - 1).getId();
            loadProducts();
        });
    }

    private void loadCompanies() {
        api.getCompanies(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject r) {
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
                        spinnerCompanyFilter.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, names));
                        spinnerCompanyFilter.setText(names.get(0), false);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String err) { /* non-critical */ }
        });
    }

    private void loadProducts() {
        if (!swipeRefresh.isRefreshing()) progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        api.getProductsSorted(filterModel, filterCompanyId, filterSize, currentSort,
                new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject r) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        try {
                            if (r.getBoolean(ApiConfig.KEY_SUCCESS)) {
                                JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                                productList.clear();
                                for (int i = 0; i < data.length(); i++)
                                    productList.add(parseProduct(data.getJSONObject(i)));
                                applyLocalFilter();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onError(String err) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyLocalFilter() {
        String query = etSearch.getText() != null
                ? etSearch.getText().toString().toLowerCase().trim() : "";
        filteredList.clear();
        for (Product p : productList) {
            if (query.isEmpty()
                    || p.getCompanyName().toLowerCase().contains(query)
                    || p.getTireType().toLowerCase().contains(query)
                    || p.getTireSize().toLowerCase().contains(query)
                    || p.getModelName().toLowerCase().contains(query)) {
                filteredList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void exportAllStockPdf() {
        if (productList.isEmpty()) {
            Toast.makeText(requireContext(), "No stock data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Generating PDF…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            StockPdfGenerator gen = new StockPdfGenerator(requireContext());
            File pdf = gen.generateAllStockPdf(new ArrayList<>(productList));
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pdf != null) gen.openPdf(pdf);
                    else Toast.makeText(requireContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
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
        return p;
    }
}
