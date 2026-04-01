// FILE: app/src/main/java/com/smarttire/inventory/fragments/DashboardFragment.java (UPDATED)
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.DashboardStatsModel;
import com.smarttire.inventory.models.MonthlyStats;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.ChartHelper;
import com.smarttire.inventory.utils.NotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalProducts, tvTotalStock, tvLowStock,
                     tvTotalRevenue, tvTotalCustomers, tvTotalOutstanding;
    private TextView tvNoLowStock;
    private RecyclerView rvLowStock, rvTopDebtors;
    private BarChart barChart;
    private CardView cardChartContainer;

    private ApiService apiService;
    private StockAdapter lowStockAdapter;
    private final List<Product> lowStockList = new ArrayList<>();

    private int pendingLoads = 0;

    public static DashboardFragment newInstance() { return new DashboardFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiService.getInstance(requireContext());
        initViews(view);
        setupLowStockList();
        swipeRefresh.setOnRefreshListener(this::loadAll);
        loadAll();
    }

    private void initViews(View v) {
        swipeRefresh        = v.findViewById(R.id.swipeRefresh);
        tvTotalProducts     = v.findViewById(R.id.tvTotalProducts);
        tvTotalStock        = v.findViewById(R.id.tvTotalStock);
        tvLowStock          = v.findViewById(R.id.tvLowStock);
        tvTotalRevenue      = v.findViewById(R.id.tvTotalRevenue);
        tvTotalCustomers    = v.findViewById(R.id.tvTotalCustomers);
        tvTotalOutstanding  = v.findViewById(R.id.tvTotalOutstanding);
        tvNoLowStock        = v.findViewById(R.id.tvNoLowStock);
        rvLowStock          = v.findViewById(R.id.rvLowStock);
        rvTopDebtors        = v.findViewById(R.id.rvTopDebtors);
        barChart            = v.findViewById(R.id.barChart);
        cardChartContainer  = v.findViewById(R.id.cardChartContainer);

        rvTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupLowStockList() {
        lowStockAdapter = new StockAdapter(requireContext(), lowStockList);
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setAdapter(lowStockAdapter);
        rvLowStock.setNestedScrollingEnabled(false);
    }

    private void loadAll() {
        pendingLoads = 3;
        swipeRefresh.setRefreshing(true);
        loadDashboardStats();
        loadLowStock();
        loadMonthlyChart();
    }

    private void onLoadComplete() {
        if (!isAdded()) return;
        pendingLoads--;
        if (pendingLoads <= 0) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void loadDashboardStats() {
        apiService.getDashboard(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject data = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (data != null) {
                            DashboardStatsModel stats = DashboardStatsModel.fromJSON(data);
                            bindStats(stats);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing dashboard stats", e);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                onLoadComplete();
            }
        });
    }

    private void bindStats(DashboardStatsModel s) {
        tvTotalProducts.setText(String.valueOf(s.getTotalProducts()));
        tvTotalStock.setText(String.valueOf(s.getTotalStock()));
        tvTotalRevenue.setText(s.getFormattedTodayRevenue());
        tvTotalCustomers.setText(String.valueOf(s.getTotalCustomers()));
        tvTotalOutstanding.setText(s.getFormattedTotalOutstanding());

        tvLowStock.setText(String.valueOf(s.getLowStockCount()));
        if (s.hasLowStock()) {
            tvLowStock.setTextColor(requireContext().getColor(R.color.warning));
        } else {
            tvLowStock.setTextColor(requireContext().getColor(R.color.success));
        }
    }

    private void loadLowStock() {
        apiService.getLowStock(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        lowStockList.clear();
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                lowStockList.add(parseProduct(data.getJSONObject(i)));
                            }
                        }
                        lowStockAdapter.notifyDataSetChanged();
                        tvNoLowStock.setVisibility(lowStockList.isEmpty() ? View.VISIBLE : View.GONE);

                        if (!lowStockList.isEmpty()) {
                            NotificationHelper.showLowStockAlert(requireContext(), lowStockList);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing low stock", e);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                onLoadComplete();
            }
        });
    }

    private void loadMonthlyChart() {
        apiService.getMonthlySales(6, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        List<MonthlyStats> monthly = new ArrayList<>();
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                monthly.add(MonthlyStats.fromJSON(data.getJSONObject(i)));
                            }
                        }
                        if (!monthly.isEmpty()) {
                            cardChartContainer.setVisibility(View.VISIBLE);
                            ChartHelper.applyMonthlyRevenueChart(barChart, monthly, requireContext());
                        } else {
                            cardChartContainer.setVisibility(View.GONE);
                        }
                    } else {
                        cardChartContainer.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing monthly sales", e);
                    cardChartContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                onLoadComplete();
                if (cardChartContainer != null) {
                    cardChartContainer.setVisibility(View.GONE);
                }
            }
        });
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product p = new Product();
        p.setId(obj.optInt("id"));
        p.setCompanyName(obj.optString("company_name"));
        p.setTireType(obj.optString("tire_type"));
        p.setTireSize(obj.optString("tire_size"));
        p.setQuantity(obj.optInt("quantity"));
        p.setPrice(obj.optDouble("price"));
        p.setModelName(obj.optString("model_name"));
        return p;
    }
}
