package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.util.Log;
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

/**
 * Dashboard – shows KPI cards, low-stock list, and a monthly revenue bar chart.
 */
public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // ── Views ─────────────────────────────────────────────────────────────────
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalProducts, tvTotalStock, tvLowStock,
                     tvTotalCompanies, tvTodaySales, tvTotalRevenue;
    private TextView tvNoLowStock;
    private RecyclerView rvLowStock;
    private BarChart barChart;
    private CardView cardChartContainer;

    // ── State ─────────────────────────────────────────────────────────────────
    private ApiService apiService;
    private StockAdapter lowStockAdapter;
    private final List<Product> lowStockList = new ArrayList<>();

    /** Track how many parallel loads are pending so refresh stops at the right time. */
    private int pendingLoads = 0;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static DashboardFragment newInstance() { return new DashboardFragment(); }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initViews(View v) {
        swipeRefresh        = v.findViewById(R.id.swipeRefresh);
        tvTotalProducts     = v.findViewById(R.id.tvTotalProducts);
        tvTotalStock        = v.findViewById(R.id.tvTotalStock);
        tvLowStock          = v.findViewById(R.id.tvLowStock);
        tvTotalCompanies    = v.findViewById(R.id.tvTotalCompanies);
        tvTodaySales        = v.findViewById(R.id.tvTodaySales);
        tvTotalRevenue      = v.findViewById(R.id.tvTotalRevenue);
        tvNoLowStock        = v.findViewById(R.id.tvNoLowStock);
        rvLowStock          = v.findViewById(R.id.rvLowStock);
        barChart            = v.findViewById(R.id.barChart);
        cardChartContainer  = v.findViewById(R.id.cardChartContainer);
    }

    private void setupLowStockList() {
        lowStockAdapter = new StockAdapter(requireContext(), lowStockList);
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setAdapter(lowStockAdapter);
        rvLowStock.setNestedScrollingEnabled(false);
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadAll() {
        pendingLoads = 3;                 // dashboard + lowStock + monthlySales
        swipeRefresh.setRefreshing(true);
        loadDashboardStats();
        loadLowStock();
        loadMonthlyChart();
    }

    /** Decrement counter; stop spinner when all 3 calls complete. */
    private void onLoadComplete() {
        if (!isAdded()) return;
        pendingLoads--;
        if (pendingLoads <= 0) {
            swipeRefresh.setRefreshing(false);
        }
    }

    // ── Dashboard KPIs ────────────────────────────────────────────────────────

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
                Log.e(TAG, "Dashboard API Error: " + error);
            }
        });
    }

    private void bindStats(DashboardStatsModel s) {
        tvTotalProducts.setText(String.valueOf(s.getTotalProducts()));
        tvTotalStock.setText(String.valueOf(s.getTotalStock()));
        tvTotalCompanies.setText(String.valueOf(s.getTotalCompanies()));
        tvTodaySales.setText(String.valueOf(s.getTodaySalesCount()));
        tvTotalRevenue.setText(s.getFormattedTodayRevenue());

        tvLowStock.setText(String.valueOf(s.getLowStockCount()));
        if (s.hasLowStock()) {
            tvLowStock.setTextColor(requireContext().getColor(R.color.warning));
        } else {
            tvLowStock.setTextColor(requireContext().getColor(R.color.success));
        }
    }

    // ── Low Stock ─────────────────────────────────────────────────────────────

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

    // ── Monthly Chart ─────────────────────────────────────────────────────────

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
                Log.e(TAG, "Monthly Sales API Error (500 likely): " + error);
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
        return p;
    }
}
