package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalProducts, tvTotalStock, tvLowStock,
            tvTotalCompanies, tvTodaySales, tvTotalRevenue;
    private TextView tvNoLowStock;
    private RecyclerView rvLowStock;
    private CardView cardLowStock;

    private ApiService apiService;
    private StockAdapter lowStockAdapter;
    private List<Product> lowStockList = new ArrayList<>();

    public DashboardFragment() {}

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();

        apiService = ApiService.getInstance(requireContext());
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);

        loadDashboardData();
    }

    private void initViews(View view) {
        swipeRefresh       = view.findViewById(R.id.swipeRefresh);
        tvTotalProducts    = view.findViewById(R.id.tvTotalProducts);
        tvTotalStock       = view.findViewById(R.id.tvTotalStock);
        tvLowStock         = view.findViewById(R.id.tvLowStock);
        tvTotalCompanies   = view.findViewById(R.id.tvTotalCompanies);
        tvTodaySales       = view.findViewById(R.id.tvTodaySales);
        tvTotalRevenue     = view.findViewById(R.id.tvTotalRevenue);
        tvNoLowStock       = view.findViewById(R.id.tvNoLowStock);
        rvLowStock         = view.findViewById(R.id.rvLowStock);
        cardLowStock       = view.findViewById(R.id.cardLowStock);
    }

    private void setupRecyclerView() {
        lowStockAdapter = new StockAdapter(requireContext(), lowStockList);
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setAdapter(lowStockAdapter);
    }

    private void loadDashboardData() {
        swipeRefresh.setRefreshing(true);

        // Load statistics
        apiService.getDashboard(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    try {
                        if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                            JSONObject data = response.getJSONObject(ApiConfig.KEY_DATA);
                            updateUI(data);
                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString(ApiConfig.KEY_MESSAGE, "Error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load low stock items
        apiService.getLowStock(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    swipeRefresh.setRefreshing(false);
                    try {
                        if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                            JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                            lowStockList.clear();
                            for (int i = 0; i < data.length(); i++) {
                                lowStockList.add(parseProduct(data.getJSONObject(i)));
                            }
                            lowStockAdapter.notifyDataSetChanged();
                            tvNoLowStock.setVisibility(
                                    lowStockList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    private void updateUI(JSONObject data) throws Exception {
        tvTotalProducts.setText(String.valueOf(data.optInt("total_products", 0)));
        tvTotalStock.setText(String.valueOf(data.optInt("total_stock", 0)));
        tvLowStock.setText(String.valueOf(data.optInt("low_stock_count", 0)));
        tvTotalCompanies.setText(String.valueOf(data.optInt("total_companies", 0)));
        tvTodaySales.setText(String.valueOf(data.optInt("today_sales_count", 0)));

        // ── FIX: accept either key name the server might send ──────────────
        double revenue = 0.0;
        if (data.has("today_revenue")) {
            revenue = data.getDouble("today_revenue");
        } else if (data.has("today_sales_amount")) {
            revenue = data.getDouble("today_sales_amount");
        } else if (data.has("total_revenue")) {
            revenue = data.getDouble("total_revenue");
        }
        // ───────────────────────────────────────────────────────────────────

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvTotalRevenue.setText(fmt.format(revenue));
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product product = new Product();
        product.setId(obj.optInt("id"));
        product.setCompanyName(obj.optString("company_name"));
        product.setTireType(obj.optString("tire_type"));
        product.setTireSize(obj.optString("tire_size"));
        product.setQuantity(obj.optInt("quantity"));
        product.setPrice(obj.optDouble("price"));
        return product;
    }
}