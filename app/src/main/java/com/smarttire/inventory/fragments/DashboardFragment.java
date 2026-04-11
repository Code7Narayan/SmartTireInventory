// FILE: fragments/DashboardFragment.java  (UPDATED — Fixed Analytics Keys and Graph Rendering)
package com.smarttire.inventory.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.smarttire.inventory.models.DashboardStatsModel;
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

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalStock, tvDailyRevenue, tvRevenueLabel;
    private TextView tvDailyAdded, tvDailySold;
    private TextView tvTotalCustomers, tvTotalOutstanding;
    private TextView tvDashboardDate, tvBarAddedCount, tvBarSoldCount;
    private TextView tvNoLowStock;

    private View barAdded, barSold;
    private MaterialButton btnPickDate, btnExport, btnChartFilter;
    private View cardTotalStock, cardRevenue, cardAddedStock, cardSoldStock, cardCustomers, cardOutstanding;

    private RecyclerView rvLowStock, rvTopDebtors, rvMostSold;
    private BarChart     barChartMonthly;

    private ApiService  api;
    private StockAdapter lowStockAdapter, mostSoldAdapter;
    private final List<Product> lowStockList = new ArrayList<>();
    private final List<Product> mostSoldList = new ArrayList<>();

    private String selectedDate = DateUtils.today();
    private int    dailyAdded  = 0, dailySold = 0;
    private double dailyRevenue = 0;
    private JSONArray cachedAddDetail = null, cachedSellDetail = null;
    
    private String currentChartEntity = "revenue"; 

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
        lowStockAdapter = new StockAdapter(requireContext(), lowStockList);
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setAdapter(lowStockAdapter);
        rvLowStock.setNestedScrollingEnabled(false);

        mostSoldAdapter = new StockAdapter(requireContext(), mostSoldList);
        rvMostSold.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMostSold.setAdapter(mostSoldAdapter);
        rvMostSold.setNestedScrollingEnabled(false);

        rvTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTopDebtors.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        if (cardTotalStock != null) cardTotalStock.setOnClickListener(v -> navigateTo(R.id.nav_stock));
        if (cardRevenue != null) cardRevenue.setOnClickListener(v -> navigateTo(R.id.nav_sales_history));
        if (cardAddedStock != null) cardAddedStock.setOnClickListener(v -> navigateTo(R.id.nav_stock));
        if (cardSoldStock != null) cardSoldStock.setOnClickListener(v -> navigateTo(R.id.nav_sales_history));
        if (cardCustomers != null) cardCustomers.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.smarttire.inventory.activities.CustomerActivity.class));
        });
        if (cardOutstanding != null) cardOutstanding.setOnClickListener(v -> navigateTo(R.id.nav_sales_history));
    }

    private void navigateTo(int navId) {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            com.google.android.material.bottomnavigation.BottomNavigationView bnv = main.findViewById(R.id.bottomNavigationView);
            if (bnv != null) bnv.setSelectedItemId(navId);
        }
    }

    private void setupChart() {
        if (barChartMonthly == null) return;
        barChartMonthly.getDescription().setEnabled(false);
        barChartMonthly.setDrawGridBackground(false);
        barChartMonthly.setDrawBarShadow(false);
        barChartMonthly.setHighlightFullBarEnabled(false);
        
        XAxis xAxis = barChartMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        barChartMonthly.getAxisRight().setEnabled(false);
        barChartMonthly.getLegend().setEnabled(true);
    }

    private void loadAll() {
        swipeRefresh.setRefreshing(true);
        loadDashboardStats();
        loadDailyAnalytics();
        loadLowStock();
        loadMonthlyAnalytics();
    }

    private void loadDashboardStats() {
        api.getDashboard(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject data = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (data != null) {
                            DashboardStatsModel stats = DashboardStatsModel.fromJSON(data);
                            if (tvTotalStock     != null) tvTotalStock.setText(String.valueOf(stats.getTotalStock()));
                            if (tvTotalCustomers != null) tvTotalCustomers.setText(String.valueOf(stats.getTotalCustomers()));
                            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
                            if (tvTotalOutstanding != null) tvTotalOutstanding.setText(fmt.format(stats.getTotalOutstanding()));
                            JSONArray debtors = data.optJSONArray("top_debtors");
                            if (debtors != null) bindDebtors(debtors);
                            JSONArray mostSold = data.optJSONArray("most_sold");
                            if (mostSold != null) bindMostSold(mostSold);
                        }
                    }
                } catch (Exception e) { Log.e(TAG, "Stats error", e); }
            }
            @Override public void onError(String error) {}
        });
    }

    private void loadDailyAnalytics() {
        api.getDailyAnalytics(selectedDate, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject d = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (d != null) {
                            // Key correction: fallback to total_stock_added/total_stock_sold if total_added/total_sold is missing
                            dailyAdded   = d.optInt("total_added", d.optInt("total_stock_added", 0));
                            dailySold    = d.optInt("total_sold",  d.optInt("total_stock_sold", 0));
                            dailyRevenue = d.optDouble("revenue", 0);
                            cachedAddDetail  = d.optJSONArray("add_detail");
                            cachedSellDetail = d.optJSONArray("sell_detail");
                            bindDailyStats();
                        }
                    }
                } catch (Exception e) { Log.e(TAG, "Daily error", e); }
            }
            @Override public void onError(String error) { if (!isAdded()) return; swipeRefresh.setRefreshing(false); }
        });
    }

    private void bindDailyStats() {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
        if (tvDailyAdded != null) tvDailyAdded.setText(String.valueOf(dailyAdded));
        if (tvDailySold  != null) tvDailySold.setText(String.valueOf(dailySold));
        if (tvDailyRevenue != null) tvDailyRevenue.setText(fmt.format(dailyRevenue));

        int max = Math.max(dailyAdded, Math.max(dailySold, 1));
        if (tvBarAddedCount != null) tvBarAddedCount.setText(String.valueOf(dailyAdded));
        if (tvBarSoldCount  != null) tvBarSoldCount.setText(String.valueOf(dailySold));

        if (barAdded != null) {
            barAdded.post(() -> {
                if (barAdded.getParent() == null) return;
                int totalWidth = ((ViewGroup)barAdded.getParent()).getWidth();
                if (totalWidth <= 0) return;
                ViewGroup.LayoutParams lp = barAdded.getLayoutParams();
                lp.width = (int)(totalWidth * ((float)dailyAdded / max));
                barAdded.setLayoutParams(lp);
            });
        }
        if (barSold != null) {
            barSold.post(() -> {
                if (barSold.getParent() == null) return;
                int totalWidth = ((ViewGroup)barSold.getParent()).getWidth();
                if (totalWidth <= 0) return;
                ViewGroup.LayoutParams lp = barSold.getLayoutParams();
                lp.width = (int)(totalWidth * ((float)dailySold / max));
                barSold.setLayoutParams(lp);
            });
        }
    }

    private void loadLowStock() {
        api.getLowStock(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        lowStockList.clear();
                        if (data != null)
                            for (int i = 0; i < data.length(); i++)
                                lowStockList.add(parseProduct(data.getJSONObject(i)));
                        lowStockAdapter.notifyDataSetChanged();
                        if (tvNoLowStock != null)
                            tvNoLowStock.setVisibility(lowStockList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } catch (Exception e) {}
            }
            @Override public void onError(String error) {}
        });
    }

    private void loadMonthlyAnalytics() {
        api.getMonthlySales(6, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded() || barChartMonthly == null) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.optJSONArray(ApiConfig.KEY_DATA);
                        if (data != null) bindBarChartData(data);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String error) {}
        });
    }

    private void showChartMenu(View v) {
        PopupMenu menu = new PopupMenu(requireContext(), v);
        menu.getMenu().add("Total Revenue");
        menu.getMenu().add("Sales Quantity");
        menu.getMenu().add("Stock Added");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            btnChartFilter.setText(title);
            switch (title) {
                case "Total Revenue": currentChartEntity = "revenue"; break;
                case "Sales Quantity": currentChartEntity = "sales_qty"; break;
                case "Stock Added": currentChartEntity = "stock_added"; break;
            }
            loadMonthlyAnalytics();
            return true;
        });
        menu.show();
    }

    private void bindBarChartData(JSONArray data) throws Exception {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        String labelText = "Data";
        int barColor = Color.parseColor("#4CAF50");

        if (currentChartEntity.equals("revenue")) {
            labelText = "Revenue (₹)";
            barColor = Color.parseColor("#4CAF50");
        } else if (currentChartEntity.equals("sales_qty")) {
            labelText = "Quantity Sold";
            barColor = Color.parseColor("#FF9800");
        } else if (currentChartEntity.equals("stock_added")) {
            labelText = "Stock Added";
            barColor = Color.parseColor("#2196F3");
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.getJSONObject(i);
            float val = 0;
            if (currentChartEntity.equals("revenue")) {
                val = (float) obj.optDouble("revenue", obj.optDouble("total_revenue", 0));
            } else if (currentChartEntity.equals("sales_qty")) {
                // Key correction: check multiple possible keys from API
                val = (float) obj.optInt("total_sold", obj.optInt("total_qty_sold", obj.optInt("total_sold_qty", 0)));
            } else if (currentChartEntity.equals("stock_added")) {
                // Key correction: check multiple possible keys from API
                val = (float) obj.optInt("total_added", obj.optInt("total_stock_added", obj.optInt("total_added_qty", 0)));
            }
            
            entries.add(new BarEntry(i, val));
            labels.add(obj.optString("month_name", "Month"));
        }

        BarDataSet set = new BarDataSet(entries, labelText);
        set.setColor(barColor);
        set.setValueTextSize(10f);
        set.setDrawValues(true);

        BarData barData = new BarData(set);
        barData.setBarWidth(0.6f);

        barChartMonthly.setData(barData);
        barChartMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartMonthly.getXAxis().setLabelCount(labels.size());
        barChartMonthly.animateY(1000);
        barChartMonthly.invalidate();
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            selectedDate = DateUtils.millisToApiDate(millis);
            updateDateLabel();
            swipeRefresh.setRefreshing(true);
            loadDailyAnalytics();
        });
        picker.show(getParentFragmentManager(), "DatePicker");
    }

    private void updateDateLabel() {
        String label = selectedDate.equals(DateUtils.today()) ? "Today" : selectedDate;
        if (tvDashboardDate != null) tvDashboardDate.setText(label);
        if (tvRevenueLabel != null) tvRevenueLabel.setText(selectedDate.equals(DateUtils.today()) ? "Today Revenue" : "Revenue");
    }

    private void showExportMenu(View v) {
        PopupMenu menu = new PopupMenu(requireContext(), v);
        menu.getMenu().add("Export Daily Report (" + selectedDate + ")");
        menu.getMenu().add("Select Date & Export");
        menu.getMenu().add("Export All-Time Analytics");

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.startsWith("Export Daily Report")) {
                exportDailyPdf();
            } else if (title.equals("Select Date & Export")) {
                showDatePickerForExport();
            } else if (title.equals("Export All-Time Analytics")) {
                exportAllTimeAnalytics();
            }
            return true;
        });
        menu.show();
    }

    private void showDatePickerForExport() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date for Report")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            String date = DateUtils.millisToApiDate(millis);
            fetchAndExportDaily(date);
        });
        picker.show(getParentFragmentManager(), "ExportDatePicker");
    }

    private void fetchAndExportDaily(String date) {
        Toast.makeText(requireContext(), "Fetching data for " + date + "...", Toast.LENGTH_SHORT).show();
        api.getDailyAnalytics(date, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject d = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (d != null) {
                            int added = d.optInt("total_added", d.optInt("total_stock_added", 0));
                            int sold = d.optInt("total_sold", d.optInt("total_stock_sold", 0));
                            double rev = d.optDouble("revenue", 0);
                            JSONArray addDet = d.optJSONArray("add_detail");
                            JSONArray sellDet = d.optJSONArray("sell_detail");

                            new Thread(() -> {
                                try {
                                    DailyReportPdfGenerator gen = new DailyReportPdfGenerator(requireContext());
                                    File pdf = gen.generate(date, added, sold, rev, addDet, sellDet);
                                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                        if (pdf != null) gen.openPdf(pdf);
                                    });
                                } catch (Exception e) {}
                            }).start();
                        }
                    }
                } catch (Exception e) {}
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportDailyPdf() {
        Toast.makeText(requireContext(), "Generating daily report…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                DailyReportPdfGenerator gen = new DailyReportPdfGenerator(requireContext());
                File pdf = gen.generate(selectedDate, dailyAdded, dailySold, dailyRevenue, cachedAddDetail, cachedSellDetail);
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (pdf != null) gen.openPdf(pdf);
                    else Toast.makeText(requireContext(), "PDF failed", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) { if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()); }
        }).start();
    }

    private void exportAllTimeAnalytics() {
        Toast.makeText(requireContext(), "Fetching all-time analytics...", Toast.LENGTH_SHORT).show();
        api.getDashboardAnalytics(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                try {
                    if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONObject dataObj = response.optJSONObject(ApiConfig.KEY_DATA);
                        if (dataObj != null) {
                            AnalyticsData data = AnalyticsData.fromJSON(dataObj);
                            generateAnalyticsPdf(data);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to load analytics data", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "AllTime Error", e);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateAnalyticsPdf(AnalyticsData data) {
        new Thread(() -> {
            try {
                DashboardAnalyticsPdfGenerator gen = new DashboardAnalyticsPdfGenerator(requireContext(), data);
                File pdf = gen.generate();
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (pdf != null) gen.openPdf(pdf);
                    else Toast.makeText(requireContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void bindDebtors(JSONArray arr) {
        try {
            List<DebtorItem> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new DebtorItem(o.getInt("id"), o.optString("name"), o.optString("phone"), o.optDouble("due")));
            }
            rvTopDebtors.setAdapter(new TopDebtorAdapter(requireContext(), list));
        } catch (Exception e) {}
    }

    private void bindMostSold(JSONArray arr) {
        try {
            mostSoldList.clear();
            for (int i = 0; i < arr.length(); i++) mostSoldList.add(parseProduct(arr.getJSONObject(i)));
            mostSoldAdapter.notifyDataSetChanged();
        } catch (Exception e) {}
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product p = new Product();
        p.setId(obj.optInt("id"));
        p.setCompanyName(obj.optString("company_name"));
        p.setTireType(obj.optString("tire_type"));
        p.setTireSize(obj.optString("tire_size"));
        p.setQuantity(obj.optInt("quantity", 0));
        p.setPrice(obj.optDouble("price", 0));
        p.setModelName(obj.optString("model_name",""));
        return p;
    }

    static class DebtorItem {
        int id; String name, phone; double due;
        DebtorItem(int id, String n, String p, double d) { this.id=id; name=n; phone=p; due=d; }
    }

    static class TopDebtorAdapter extends RecyclerView.Adapter<TopDebtorAdapter.VH> {
        private final android.content.Context ctx;
        private final List<DebtorItem> list;
        TopDebtorAdapter(android.content.Context c, List<DebtorItem> l) { ctx=c; list=l; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_debtor, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            DebtorItem d = list.get(pos);
            h.tvName.setText(d.name); h.tvPhone.setText(d.phone);
            h.tvDue.setText(NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(d.due));
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(ctx, CustomerDetailActivity.class);
                i.putExtra("customer_id", d.id); ctx.startActivity(i);
            });
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvDue;
            VH(View v) { super(v); tvName=v.findViewById(R.id.tvDebtorName);
                tvPhone=v.findViewById(R.id.tvDebtorPhone); tvDue=v.findViewById(R.id.tvDebtorDue); }
        }
    }
}
