// FILE: fragments/StockFragment.java  (ERP VERSION — FULL REPLACEMENT)
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.Company;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StockFragment extends Fragment {

    private TextInputEditText   etSearch, etModelFilter, etSizeFilter;
    private AutoCompleteTextView spinnerCompanyFilter;
    private SwipeRefreshLayout  swipeRefresh;
    private RecyclerView        rvStock;
    private LinearLayout        layoutEmpty;
    private ProgressBar         progressBar;

    private ApiService          api;
    private StockAdapter        adapter;
    private final List<Product> productList  = new ArrayList<>();
    private final List<Product> filteredList = new ArrayList<>();
    private final List<Company> companyList  = new ArrayList<>();

    private int    filterCompanyId  = 0;
    private String filterModel      = "";
    private String filterSize       = "";

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
        etSearch          = v.findViewById(R.id.etSearch);
        etModelFilter     = v.findViewById(R.id.etModelFilter);
        etSizeFilter      = v.findViewById(R.id.etSizeFilter);
        spinnerCompanyFilter = v.findViewById(R.id.spinnerCompanyFilter);
        swipeRefresh      = v.findViewById(R.id.swipeRefresh);
        rvStock           = v.findViewById(R.id.rvStock);
        layoutEmpty       = v.findViewById(R.id.layoutEmpty);
        progressBar       = v.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new StockAdapter(requireContext(), filteredList);
        rvStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStock.setAdapter(adapter);
    }

    private void setupSearch() {
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyLocalFilter();
            }
        };
        etSearch.addTextChangedListener(tw);
    }

    private void setupFilters() {
        // Model filter — triggers server-side reload on text change
        etModelFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterModel = s.toString().trim();
                loadProducts(); // server-side
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

        spinnerCompanyFilter.setOnItemClickListener((parent, v, pos, id) -> {
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

        api.getProducts(filterModel, filterCompanyId, filterSize,
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

    /** Secondary local filter for the free-text search bar */
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

    private Product parseProduct(JSONObject o) throws Exception {
        Product p = new Product();
        p.setId(o.getInt("id"));
        p.setCompanyId(o.optInt("company_id"));
        p.setCompanyName(o.getString("company_name"));
        p.setModelName(o.optString("model_name",""));
        p.setTireType(o.getString("tire_type"));
        p.setTireSize(o.getString("tire_size"));
        p.setQuantity(o.getInt("quantity"));
        p.setPrice(o.getDouble("price"));
        return p;
    }
}