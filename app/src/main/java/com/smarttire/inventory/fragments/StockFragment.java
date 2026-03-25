package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StockFragment extends Fragment {

    private TextInputEditText etSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvStock;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private ApiService apiService;
    private StockAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private List<Product> filteredList = new ArrayList<>();

    public StockFragment() {
        // Required empty public constructor
    }

    public static StockFragment newInstance() {
        return new StockFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSearch();

        apiService = ApiService.getInstance(requireContext());

        swipeRefresh.setOnRefreshListener(this::loadProducts);

        // Initial load
        loadProducts();
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvStock = view.findViewById(R.id.rvStock);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new StockAdapter(requireContext(), filteredList);
        rvStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStock.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            String query = text.toLowerCase();
            for (Product item : productList) {
                if (item.getCompanyName().toLowerCase().contains(query) ||
                    item.getTireSize().toLowerCase().contains(query) ||
                    item.getTireType().toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadProducts() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        layoutEmpty.setVisibility(View.GONE);

        apiService.getProducts(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    try {
                        if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                            JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                            productList.clear();
                            for (int i = 0; i < data.length(); i++) {
                                productList.add(parseProduct(data.getJSONObject(i)));
                            }
                            filter(etSearch.getText().toString());
                        } else {
                            Toast.makeText(requireContext(), response.getString(ApiConfig.KEY_MESSAGE), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product product = new Product();
        product.setId(obj.getInt("id"));
        product.setCompanyId(obj.getInt("company_id"));
        product.setCompanyName(obj.getString("company_name"));
        product.setTireType(obj.getString("tire_type"));
        product.setTireSize(obj.getString("tire_size"));
        product.setQuantity(obj.getInt("quantity"));
        product.setPrice(obj.getDouble("price"));
        return product;
    }
}
