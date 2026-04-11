// FILE: fragments/DashboardFragment.java (FINAL FIX — addressing checklist)
package com.smarttire.inventory.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.smarttire.inventory.R;
import com.smarttire.inventory.activities.CustomerDetailActivity;
import com.smarttire.inventory.activities.MainActivity;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.AnalyticsData;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.DailyReportPdfGenerator;
import com.smarttire.inventory.utils.DashboardAnalyticsPdfGenerator;
import com.smarttire.inventory.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // ── Views ────────────────────────────────────────────────────────────────
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalStock, tvDailyRevenue, tvRevenueLabel;
    private TextView tvDailyAdded, tvDailySold;
    private TextView tvTotalCustomers, tvTotalOutstanding;
    private TextView tvDashboardDate, tvBarAddedCount, tvBarSoldCount;
    private TextView tvNoLowStock;

    private View barAdded, barSold;
    private MaterialButton btnPickDate, btnExport, btnChartFilter;
    private View cardTotalStock, cardRevenue, cardAddedStock, cardSoldStock,
            cardCustomers, cardOutstanding;

    private RecyclerView rvLowStock, rvTopDebtors, rvMostSold;
    private BarChart     barChartMonthly;

    // ── Data / Adapters ──────────────────────────────────────────────────────
    private ApiService  api;
    private StockAdapter lowStockAdapter, mostSoldAdapter;
    private TopDebtorAdapter debtorAdapter;

    private String selectedDate  = DateUtils.today();
    private int    dailyAdded    = 0, dailySold = 0;
    private double dailyRevenue  = 0;
    private JSONArray cachedAddDetail  = null;
    private JSONArray cachedSellDetail = null;
    private String currentChartEntity = "revenue";

    private final ExecutorService executor   = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());
    private int pendingLoads = 0;

    private static final NumberFormat INR_FMT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static DashboardFragment newInstance() { return new DashboardFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiService.getInstance(requireContext());
        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        setupChart();
        swipeRefresh.setOnRefreshListener(this::loadAll);
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnExport.setOnClickListener(this::showExportMenu);
        btnChartFilter.setOnClickListener(this::showChartMenu);
        updateDateLabel();
        loadAll();
    }

    @Override public void onDestroyView() { super.onDestroyView(); executor.shutdown(); }

    private void initViews(View v) {
        swipeRefresh       = v.findViewById(R.id.swipeRefresh);
        tvTotalStock       = v.findViewById(R.id.tvTotalStock);
        tvDailyRevenue     = v.findViewById(R.id.tvDailyRevenue);
        tvRevenueLabel     = v.findViewById(R.id.tvRevenueLabel);
        tvDailyAdded       = v.findViewById(R.id.tvDailyAdded);
        tvDailySold        = v.findViewById(R.id.tvDailySold);
        tvTotalCustomers   = v.findViewById(R.id.tvTotalCustomers);
        tvTotalOutstanding = v.findViewById(R.id.tvTotalOutstanding);
        tvDashboardDate    = v.findViewById(R.id.tvDashboardDate);
        tvBarAddedCount    = v.findViewById(R.id.tvBarAddedCount);
        tvBarSoldCount     = v.findViewById(R.id.tvBarSoldCount);
        barAdded           = v.findViewById(R.id.barAdded);
        barSold            = v.findViewById(R.id.barSold);
        btnPickDate        = v.findViewById(R.id.btnPickDate);
        btnExport          = v.findViewById(R.id.btnExportDailyPdf);
        btnChartFilter     = v.findViewById(R.id.btnChartFilter);
        rvLowStock         = v.findViewById(R.id.rvLowStock);
        rvTopDebtors       = v.findViewById(R.id.rvTopDebtors);
        rvMostSold         = v.findViewById(R.id.rvMostSold);
        barChartMonthly    = v.findViewById(R.id.barChartMonthly);
        tvNoLowStock       = v.findViewById(R.id.tvNoLowStock);
        cardTotalStock     = v.findViewById(R.id.cardTotalStock);
        cardRevenue        = v.findViewById(R.id.cardRevenue);
        cardAddedStock     = v.findViewById(R.id.cardAddedStock);
        cardSoldStock      = v.findViewById(R.id.cardSoldStock);
        cardCustomers      = v.findViewById(R.id.cardCustomers);
        cardOutstanding    = v.findViewById(R.id.cardOutstanding);
    }

    private void setupRecyclerViews() {
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setNestedScrollingEnabled(false);
        lowStockAdapter = new StockAdapter(requireContext(), new ArrayList<>());
        rvLowStock.setAdapter(lowStockAdapter);

        rvMostSold.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMostSold.setNestedScrollingEnabled(false);
        mostSoldAdapter = new StockAdapter(requireContext(), new ArrayList<>());
        rvMostSold.setAdapter(mostSoldAdapter);

        rvTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTopDebtors.setNestedScrollingEnabled(false);
        debtorAdapter = new TopDebtorAdapter(requireContext(), new ArrayList<>());
        rvTopDebtors.setAdapter(debtorAdapter);
    }

    private void setupClickListeners() {
        if (cardTotalStock   != null) cardTotalStock.setOnClickListener(v -> navigateTo(R.id.nav_stock));
        if (cardRevenue      != null) cardRevenue.setOnClickListener(v -> navigateTo(R.id.nav_sales_history));
        if (cardAddedStock   != null) cardAddedStock.setOnClickListener(v -> navigateTo(R.id.nav_stock));
        if (cardSoldStock    != null) cardSoldStock.setOnClickListener(v -> navigateTo(R.id.nav_sales_history));
        if (cardCustomers    != null) cardCustomers.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.smarttire.inventory.activities.CustomerActivity.class)));
        if (cardOutstanding != null) cardOutstanding.setOnClickListener(v -> navigateTo(R.id.nav_sales_history));
    }

    private void navigateTo(int navId) {
        if (getActivity() instanceof MainActivity) {
            com.google.android.material.bottomnavigation.BottomNavigationView bnv = getActivity().findViewById(R.id.bottomNavigationView);
            if (bnv != null) bnv.setSelectedItemId(navId);
        }
    }

    private void setupChart() {
        if (barChartMonthly == null) return;
        barChartMonthly.getDescription().setEnabled(false);
        barChartMonthly.getAxisRight().setEnabled(false);
        XAxis xAxis = barChartMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void loadAll() {
        pendingLoads = 4;
        swipeRefresh.setRefreshing(true);
        loadDashboardStats();
        loadDailyAnalytics();
        loadLowStock();
        loadMonthlyAnalytics();
    }

    private void onLoadComplete() {
        if (!isAdded()) return;
        pendingLoads--;
        if (pendingLoads <= 0) swipeRefresh.setRefreshing(false);
    }

    private void loadDashboardStats() {
        api.getDashboard(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean("success")) {
                        JSONObject data = response.optJSONObject("data");
                        if (data == null) return;

                        // ✅ Set TextViews properly
                        tvTotalStock.setText(String.valueOf(data.optInt("total_stock", 0)));
                        tvTotalCustomers.setText(String.valueOf(data.optInt("total_customers", 0)));
                        tvTotalOutstanding.setText(INR_FMT.format(data.optDouble("total_outstanding", 0)));

                        // Debtors
                        JSONArray debtors = data.optJSONArray("top_debtors");
                        if (debtors != null) bindDebtorsAsync(debtors);

                        // Most Sold
                        JSONArray mostSold = data.optJSONArray("most_sold");
                        if (mostSold != null) bindMostSoldAsync(mostSold);
                    }
                } catch (Exception e) { Log.e(TAG, "Dashboard parse", e); }
            }
            @Override public void onError(String error) { onLoadComplete(); }
        });
    }

    private void loadDailyAnalytics() {
        api.getDailyAnalytics(selectedDate, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject d = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (d == null) return;
                        dailyAdded    = d.optInt("total_added", d.optInt("total_stock_added", 0));
                        dailySold     = d.optInt("total_sold",  d.optInt("total_stock_sold", 0));
                        dailyRevenue  = d.optDouble("revenue", 0);
                        cachedAddDetail  = d.optJSONArray("add_detail");
                        cachedSellDetail = d.optJSONArray("sell_detail");
                        bindDailyStats();
                    }
                } catch (Exception e) { Log.e(TAG, "Daily error", e); }
            }
            @Override public void onError(String error) { onLoadComplete(); }
        });
    }

    private void loadLowStock() {
        api.getLowStock(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                onLoadComplete();
                executor.execute(() -> {
                    List<Product> parsed = new ArrayList<>();
                    try {
                        if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                            JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                            if (data != null) {
                                for (int i = 0; i < data.length(); i++) parsed.add(parseProduct(data.getJSONObject(i)));
                            }
                        }
                    } catch (Exception e) { Log.e(TAG, "Low stock parse", e); }
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        // ✅ Clear & Notify via adapter method
                        lowStockAdapter.updateData(parsed);
                        if (tvNoLowStock != null) tvNoLowStock.setVisibility(parsed.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                });
            }
            @Override public void onError(String error) { onLoadComplete(); }
        });
    }

    private void loadMonthlyAnalytics() {
        api.getMonthlySales(6, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded() || barChartMonthly == null) return;
                onLoadComplete();
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        if (data != null) bindBarChartData(data);
                    }
                } catch (Exception e) { Log.e(TAG, "Monthly parse", e); }
            }
            @Override public void onError(String error) { onLoadComplete(); }
        });
    }

    private void bindDailyStats() {
        if (tvDailyAdded   != null) tvDailyAdded.setText(String.valueOf(dailyAdded));
        if (tvDailySold    != null) tvDailySold.setText(String.valueOf(dailySold));
        if (tvDailyRevenue != null) tvDailyRevenue.setText(INR_FMT.format(dailyRevenue));
        if (tvBarAddedCount != null) tvBarAddedCount.setText(String.valueOf(dailyAdded));
        if (tvBarSoldCount  != null) tvBarSoldCount.setText(String.valueOf(dailySold));
        if (barAdded != null) barAdded.post(() -> updateProgressBar(barAdded, dailyAdded));
        if (barSold  != null) barSold.post(() -> updateProgressBar(barSold, dailySold));
    }

    private void updateProgressBar(View bar, int value) {
        if (!isAdded() || bar.getParent() == null) return;
        int max = Math.max(dailyAdded, Math.max(dailySold, 1));
        int parentWidth = ((View) bar.getParent()).getWidth();
        if (parentWidth <= 0) return;
        ViewGroup.LayoutParams lp = bar.getLayoutParams();
        lp.width = (int) (parentWidth * ((float) value / max));
        bar.setLayoutParams(lp);
    }

    private void bindDebtorsAsync(JSONArray arr) {
        executor.execute(() -> {
            List<DebtorItem> list = new ArrayList<>();
            try {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new DebtorItem(o.getInt("id"), o.optString("name"), o.optString("phone"), o.optDouble("due")));
                }
            } catch (Exception e) { Log.e(TAG, "Debtor parse", e); }
            mainHandler.post(() -> {
                if (!isAdded()) return;
                // ✅ Clear & Notify via adapter method
                debtorAdapter.updateData(list);
            });
        });
    }

    private void bindMostSoldAsync(JSONArray arr) {
        executor.execute(() -> {
            List<Product> parsed = new ArrayList<>();
            try {
                for (int i = 0; i < arr.length(); i++) parsed.add(parseProduct(arr.getJSONObject(i)));
            } catch (Exception e) { Log.e(TAG, "MostSold parse", e); }
            mainHandler.post(() -> {
                if (!isAdded()) return;
                // ✅ Clear & Notify via adapter method
                mostSoldAdapter.updateData(parsed);
            });
        });
    }

    private void bindBarChartData(JSONArray data) throws Exception {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String>   labels  = new ArrayList<>();
        int barColor; String labelText;
        switch (currentChartEntity) {
            case "sales_qty":   labelText = "Quantity Sold";  barColor = Color.parseColor("#FF9800"); break;
            case "stock_added": labelText = "Stock Added";    barColor = Color.parseColor("#2196F3"); break;
            default:            labelText = "Revenue (₹)";    barColor = Color.parseColor("#4CAF50"); break;
        }
        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.getJSONObject(i);
            float val = 0;
            switch (currentChartEntity) {
                case "revenue":     val = (float) obj.optDouble("revenue", obj.optDouble("total_revenue", 0)); break;
                case "sales_qty":   val = obj.optInt("total_sold", obj.optInt("total_qty_sold", 0)); break;
                case "stock_added": val = obj.optInt("total_added", obj.optInt("total_stock_added", 0)); break;
            }
            entries.add(new BarEntry(i, val));
            labels.add(obj.optString("month", obj.optString("month_name", "")));
        }
        BarDataSet set = new BarDataSet(entries, labelText);
        set.setColor(barColor);
        barChartMonthly.setData(new BarData(set));
        barChartMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartMonthly.animateY(800);
        barChartMonthly.invalidate();
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Date").build();
        picker.addOnPositiveButtonClickListener(millis -> {
            selectedDate = DateUtils.millisToApiDate(millis);
            updateDateLabel();
            swipeRefresh.setRefreshing(true);
            pendingLoads = 1;
            loadDailyAnalytics();
        });
        picker.show(getParentFragmentManager(), "DatePicker");
    }

    private void updateDateLabel() {
        String label = selectedDate.equals(DateUtils.today()) ? "Today" : selectedDate;
        if (tvDashboardDate != null) tvDashboardDate.setText(label);
        if (tvRevenueLabel  != null) tvRevenueLabel.setText(selectedDate.equals(DateUtils.today()) ? "Today Revenue" : "Revenue");
    }

    private void showChartMenu(View v) {
        PopupMenu menu = new PopupMenu(requireContext(), v);
        menu.getMenu().add("Total Revenue"); menu.getMenu().add("Sales Quantity"); menu.getMenu().add("Stock Added");
        menu.setOnMenuItemClickListener(item -> {
            btnChartFilter.setText(item.getTitle());
            switch (item.getTitle().toString()) {
                case "Total Revenue":   currentChartEntity = "revenue";     break;
                case "Sales Quantity":  currentChartEntity = "sales_qty";   break;
                case "Stock Added":     currentChartEntity = "stock_added"; break;
            }
            pendingLoads = 1; loadMonthlyAnalytics();
            return true;
        });
        menu.show();
    }

    private void showExportMenu(View v) {
        PopupMenu menu = new PopupMenu(requireContext(), v);
        menu.getMenu().add("Export Daily Report (" + selectedDate + ")");
        menu.getMenu().add("Select Date & Export");
        menu.getMenu().add("Export All-Time Analytics");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.startsWith("Export Daily Report")) exportDailyPdf();
            else if ("Select Date & Export".equals(title)) showDatePickerForExport();
            else if ("Export All-Time Analytics".equals(title)) exportAllTimeAnalytics();
            return true;
        });
        menu.show();
    }

    private void showDatePickerForExport() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Date for Report").build();
        picker.addOnPositiveButtonClickListener(millis -> fetchAndExportDaily(DateUtils.millisToApiDate(millis)));
        picker.show(getParentFragmentManager(), "ExportDatePicker");
    }

    private void fetchAndExportDaily(String date) {
        api.getDailyAnalytics(date, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject d = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (d == null) return;
                        generateDailyPdfAsync(date, d.optInt("total_added", 0), d.optInt("total_sold", 0), d.optDouble("revenue", 0), d.optJSONArray("add_detail"), d.optJSONArray("sell_detail"));
                    }
                } catch (Exception e) { Log.e(TAG, "Export fetch error", e); }
            }
            @Override public void onError(String error) { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void exportDailyPdf() { generateDailyPdfAsync(selectedDate, dailyAdded, dailySold, dailyRevenue, cachedAddDetail, cachedSellDetail); }

    private void generateDailyPdfAsync(String date, int added, int sold, double rev, JSONArray addDet, JSONArray sellDet) {
        executor.execute(() -> {
            DailyReportPdfGenerator gen = new DailyReportPdfGenerator(requireContext());
            File pdf = gen.generate(date, added, sold, rev, addDet, sellDet);
            mainHandler.post(() -> { if (isAdded() && pdf != null) gen.openPdf(pdf); });
        });
    }

    private void exportAllTimeAnalytics() {
        api.getDashboardAnalytics(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        AnalyticsData data = AnalyticsData.fromJSON(response.optJSONObject(ApiConfig.KEY_DATA));
                        executor.execute(() -> {
                            DashboardAnalyticsPdfGenerator gen = new DashboardAnalyticsPdfGenerator(requireContext(), data);
                            File pdf = gen.generate();
                            mainHandler.post(() -> { if (isAdded() && pdf != null) gen.openPdf(pdf); });
                        });
                    }
                } catch (Exception e) { Log.e(TAG, "AllTime export error", e); }
            }
            @Override public void onError(String error) { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show(); }
        });
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product p = new Product();
        p.setId(obj.optInt("id"));
        p.setCompanyName(obj.optString("company_name"));
        p.setTireType(obj.optString("tire_type"));
        p.setTireSize(obj.optString("tire_size"));
        p.setQuantity(obj.optInt("quantity", 0));
        p.setPrice(obj.optDouble("price", 0));
        p.setModelName(obj.optString("model_name", ""));
        return p;
    }

    static class DebtorItem {
        final int id; final String name, phone; final double due;
        DebtorItem(int id, String n, String p, double d) { this.id = id; name = n; phone = p; due = d; }
    }

    static class TopDebtorAdapter extends RecyclerView.Adapter<TopDebtorAdapter.VH> {
        private final android.content.Context ctx;
        private final List<DebtorItem> list = new ArrayList<>();
        TopDebtorAdapter(android.content.Context c, List<DebtorItem> initial) { ctx = c; list.addAll(initial); }
        void updateData(List<DebtorItem> newData) {
            list.clear(); // ✅ Clear before adding
            list.addAll(newData);
            notifyDataSetChanged(); // ✅ Notify adapter
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_debtor, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            DebtorItem d = list.get(pos);
            h.tvName.setText(d.name); h.tvPhone.setText(d.phone);
            h.tvDue.setText(INR_FMT.format(d.due));
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(ctx, CustomerDetailActivity.class);
                i.putExtra("customer_id", d.id); ctx.startActivity(i);
            });
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView tvName, tvPhone, tvDue;
            VH(View v) { super(v); tvName = v.findViewById(R.id.tvDebtorName); tvPhone = v.findViewById(R.id.tvDebtorPhone); tvDue = v.findViewById(R.id.tvDebtorDue); }
        }
    }
}
