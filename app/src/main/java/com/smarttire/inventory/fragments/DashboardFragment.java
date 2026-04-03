// FILE: fragments/DashboardFragment.java  (MAJOR UPGRADE — Tasks 1 & 2)
package com.smarttire.inventory.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.google.android.material.button.MaterialButton;
import com.smarttire.inventory.R;
import com.smarttire.inventory.activities.CustomerDetailActivity;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.AnalyticsData;
import com.smarttire.inventory.models.DashboardStatsModel;
import com.smarttire.inventory.models.MonthlyStats;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.ChartHelper;
import com.smarttire.inventory.utils.DashboardAnalyticsPdfGenerator;
import com.smarttire.inventory.utils.NotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // ── Standard KPI views ────────────────────────────────────────────────────
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalProducts, tvTotalStock, tvLowStock,
            tvTotalRevenue, tvTotalCustomers, tvTotalOutstanding;
    private TextView tvNoLowStock;
    private RecyclerView rvLowStock, rvTopDebtors;
    private BarChart     barChart;
    private CardView     cardChartContainer;

    // ── Analytics views (new) ─────────────────────────────────────────────────
    private CardView     cardAnalytics;
    private TextView     tvAnalyticsTotalRevenue, tvAnalyticsTotalCollected,
            tvAnalyticsOutstanding, tvAnalyticsTodayRev,
            tvAnalyticsMonthRev, tvAnalyticsStockMovement;
    private TextView     tvBestSellerName, tvBestSellerQty, tvBestSellerRevenue;
    private RecyclerView rvStockRevenue;
    private MaterialButton btnExportAnalytics;

    // ── Adapters & data ───────────────────────────────────────────────────────
    private ApiService   apiService;
    private StockAdapter lowStockAdapter;
    private final List<Product> lowStockList  = new ArrayList<>();

    private AnalyticsData latestAnalytics = null;

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
        setupLists();
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

        // Analytics section
        cardAnalytics              = v.findViewById(R.id.cardAnalytics);
        tvAnalyticsTotalRevenue    = v.findViewById(R.id.tvAnalyticsTotalRevenue);
        tvAnalyticsTotalCollected  = v.findViewById(R.id.tvAnalyticsTotalCollected);
        tvAnalyticsOutstanding     = v.findViewById(R.id.tvAnalyticsOutstanding);
        tvAnalyticsTodayRev        = v.findViewById(R.id.tvAnalyticsTodayRev);
        tvAnalyticsMonthRev        = v.findViewById(R.id.tvAnalyticsMonthRev);
        tvAnalyticsStockMovement   = v.findViewById(R.id.tvAnalyticsStockMovement);
        tvBestSellerName           = v.findViewById(R.id.tvBestSellerName);
        tvBestSellerQty            = v.findViewById(R.id.tvBestSellerQty);
        tvBestSellerRevenue        = v.findViewById(R.id.tvBestSellerRevenue);
        rvStockRevenue             = v.findViewById(R.id.rvStockRevenue);
        btnExportAnalytics         = v.findViewById(R.id.btnExportAnalytics);

        if (rvTopDebtors != null)
            rvTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (rvStockRevenue != null) {
            rvStockRevenue.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvStockRevenue.setNestedScrollingEnabled(false);
        }

        if (btnExportAnalytics != null)
            btnExportAnalytics.setOnClickListener(v2 -> exportAnalyticsPdf());
    }

    private void setupLists() {
        lowStockAdapter = new StockAdapter(requireContext(), lowStockList);
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setAdapter(lowStockAdapter);
        rvLowStock.setNestedScrollingEnabled(false);
    }

    // ── Load all ──────────────────────────────────────────────────────────────

    private void loadAll() {
        pendingLoads = 4;
        swipeRefresh.setRefreshing(true);
        loadDashboardStats();
        loadLowStock();
        loadMonthlyChart();
        loadAnalytics();      // NEW
    }

    private void onLoadComplete() {
        if (!isAdded()) return;
        if (--pendingLoads <= 0) swipeRefresh.setRefreshing(false);
    }

    // ── Standard dashboard stats ──────────────────────────────────────────────

    private void loadDashboardStats() {
        apiService.getDashboard(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject data = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (data != null) {
                            DashboardStatsModel stats = DashboardStatsModel.fromJSON(data);
                            bindStats(stats);
                            JSONArray debtors = data.optJSONArray("top_debtors");
                            if (debtors != null && rvTopDebtors != null) bindDebtors(debtors);
                        }
                    }
                } catch (Exception e) { Log.e(TAG, "Stats error", e); }
            }
            @Override public void onError(String error) {
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
        tvLowStock.setTextColor(ContextCompat.getColor(requireContext(),
                s.hasLowStock() ? R.color.warning : R.color.success));
    }

    private void bindDebtors(JSONArray arr) {
        try {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
            List<DebtorItem> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new DebtorItem(o.getInt("id"), o.optString("name"),
                        o.optString("phone"), o.optDouble("due")));
            }
            TopDebtorAdapter a = new TopDebtorAdapter(requireContext(), list);
            rvTopDebtors.setAdapter(a);
        } catch (Exception e) { Log.e(TAG, "Debtors error", e); }
    }

    // ── Low stock ─────────────────────────────────────────────────────────────

    private void loadLowStock() {
        apiService.getLowStock(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        lowStockList.clear();
                        if (data != null)
                            for (int i = 0; i < data.length(); i++)
                                lowStockList.add(parseProduct(data.getJSONObject(i)));
                        lowStockAdapter.notifyDataSetChanged();
                        tvNoLowStock.setVisibility(lowStockList.isEmpty() ? View.VISIBLE : View.GONE);
                        if (!lowStockList.isEmpty())
                            NotificationHelper.showLowStockAlert(requireContext(), lowStockList);
                    }
                } catch (Exception e) { Log.e(TAG, "LowStock error", e); }
            }
            @Override public void onError(String error) { if (!isAdded()) return; onLoadComplete(); }
        });
    }

    // ── Monthly chart ─────────────────────────────────────────────────────────

    private void loadMonthlyChart() {
        apiService.getMonthlySales(6, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        List<MonthlyStats> monthly = new ArrayList<>();
                        if (data != null)
                            for (int i = 0; i < data.length(); i++)
                                monthly.add(MonthlyStats.fromJSON(data.getJSONObject(i)));
                        if (!monthly.isEmpty()) {
                            cardChartContainer.setVisibility(View.VISIBLE);
                            ChartHelper.applyMonthlyRevenueChart(barChart, monthly, requireContext());
                        } else cardChartContainer.setVisibility(View.GONE);
                    } else cardChartContainer.setVisibility(View.GONE);
                } catch (Exception e) { cardChartContainer.setVisibility(View.GONE); }
            }
            @Override public void onError(String e) { if (!isAdded()) return; onLoadComplete(); }
        });
    }

    // ── Analytics (NEW — Task 1) ──────────────────────────────────────────────

    private void loadAnalytics() {
        apiService.getDashboardAnalytics(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject d = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (d != null) {
                            latestAnalytics = AnalyticsData.fromJSON(d);
                            bindAnalytics(latestAnalytics);
                        }
                    }
                } catch (Exception e) { Log.e(TAG, "Analytics error", e); }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                onLoadComplete();
                Log.e(TAG, "Analytics load error: " + error);
            }
        });
    }

    private void bindAnalytics(AnalyticsData a) {
        if (cardAnalytics != null) cardAnalytics.setVisibility(View.VISIBLE);

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

        setText(tvAnalyticsTotalRevenue,   fmt.format(a.totalRevenue));
        setText(tvAnalyticsTotalCollected, fmt.format(a.totalCollected));
        setText(tvAnalyticsOutstanding,    fmt.format(a.totalOutstanding));
        setText(tvAnalyticsTodayRev,       fmt.format(a.todayRevenue));
        setText(tvAnalyticsMonthRev,       fmt.format(a.monthRevenue));
        setText(tvAnalyticsStockMovement,  "Added: " + a.totalStockAdded +
                "  |  Sold: " + a.totalStockSold);

        // Best seller
        if (!a.bestSelling.isEmpty()) {
            AnalyticsData.StockRevenueItem best = a.bestSelling.get(0);
            setText(tvBestSellerName,    best.displayName());
            setText(tvBestSellerQty,     best.qtySold + " units sold");
            setText(tvBestSellerRevenue, fmt.format(best.revenue));
        }

        // Stock-wise revenue list
        if (rvStockRevenue != null && !a.stockWiseRevenue.isEmpty()) {
            StockRevenueAdapter adapter = new StockRevenueAdapter(requireContext(),
                    a.stockWiseRevenue);
            rvStockRevenue.setAdapter(adapter);
        }
    }

    // ── Export analytics PDF (Task 2) ─────────────────────────────────────────

    private void exportAnalyticsPdf() {
        if (latestAnalytics == null) {
            Toast.makeText(requireContext(), "Analytics not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Generating analytics PDF…", Toast.LENGTH_SHORT).show();
        AnalyticsData snap = latestAnalytics;
        new Thread(() -> {
            DashboardAnalyticsPdfGenerator gen =
                    new DashboardAnalyticsPdfGenerator(requireContext(), snap);
            File pdf = gen.generate();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pdf != null) gen.openPdf(pdf);
                    else Toast.makeText(requireContext(), "PDF failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setText(TextView tv, String s) {
        if (tv != null) tv.setText(s);
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product p = new Product();
        p.setId(obj.optInt("id"));
        p.setCompanyName(obj.optString("company_name"));
        p.setTireType(obj.optString("tire_type"));
        p.setTireSize(obj.optString("tire_size"));
        p.setQuantity(obj.optInt("quantity"));
        p.setPrice(obj.optDouble("price"));
        p.setModelName(obj.optString("model_name",""));
        return p;
    }

    // ── Stock Revenue Adapter ─────────────────────────────────────────────────

    static class StockRevenueAdapter extends RecyclerView.Adapter<StockRevenueAdapter.VH> {
        private final android.content.Context ctx;
        private final List<AnalyticsData.StockRevenueItem> list;

        StockRevenueAdapter(android.content.Context ctx,
                            List<AnalyticsData.StockRevenueItem> list) {
            this.ctx = ctx; this.list = list;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.item_stock_revenue, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AnalyticsData.StockRevenueItem item = list.get(pos);
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

            h.tvRank.setText(String.valueOf(pos + 1));
            h.tvProduct.setText(item.displayName());
            h.tvQtySold.setText(item.qtySold + " sold");
            h.tvRevenue.setText(item.formattedRevenue());
            if (h.tvOutstanding != null) {
                h.tvOutstanding.setVisibility(item.outstanding > 0.005 ? View.VISIBLE : View.GONE);
                h.tvOutstanding.setText("Due: " + fmt.format(item.outstanding));
            }
        }

        @Override public int getItemCount() { return Math.min(list.size(), 15); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvRank, tvProduct, tvQtySold, tvRevenue, tvOutstanding;
            VH(@NonNull android.view.View v) {
                super(v);
                tvRank        = v.findViewById(R.id.tvRevRank);
                tvProduct     = v.findViewById(R.id.tvRevProduct);
                tvQtySold     = v.findViewById(R.id.tvRevQtySold);
                tvRevenue     = v.findViewById(R.id.tvRevRevenue);
                tvOutstanding = v.findViewById(R.id.tvRevOutstanding);
            }
        }
    }

    // ── Debtors adapter (inline) ──────────────────────────────────────────────

    static class DebtorItem {
        int id; String name, phone; double due;
        DebtorItem(int id, String name, String phone, double due) {
            this.id=id; this.name=name; this.phone=phone; this.due=due;
        }
    }

    static class TopDebtorAdapter extends RecyclerView.Adapter<TopDebtorAdapter.VH> {
        private final android.content.Context ctx;
        private final List<DebtorItem> list;

        TopDebtorAdapter(android.content.Context ctx, List<DebtorItem> list) {
            this.ctx=ctx; this.list=list;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.item_debtor, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DebtorItem d = list.get(pos);
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
            h.tvName.setText(d.name);
            h.tvPhone.setText(d.phone);
            h.tvDue.setText(fmt.format(d.due));
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(ctx, CustomerDetailActivity.class);
                i.putExtra("customer_id", d.id);
                i.putExtra("customer_name", d.name);
                ctx.startActivity(i);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvDue;
            VH(@NonNull android.view.View v) {
                super(v);
                tvName  = v.findViewById(R.id.tvDebtorName);
                tvPhone = v.findViewById(R.id.tvDebtorPhone);
                tvDue   = v.findViewById(R.id.tvDebtorDue);
            }
        }
    }
}