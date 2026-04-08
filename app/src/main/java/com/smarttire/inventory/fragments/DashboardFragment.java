// FILE: fragments/DashboardFragment.java  (REDESIGNED — clean daily insights dashboard)
package com.smarttire.inventory.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.smarttire.inventory.R;
import com.smarttire.inventory.activities.CustomerDetailActivity;
import com.smarttire.inventory.adapters.StockAdapter;
import com.smarttire.inventory.models.DashboardStatsModel;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.DailyReportPdfGenerator;
import com.smarttire.inventory.utils.DateUtils;
import com.smarttire.inventory.utils.NotificationHelper;
import com.smarttire.inventory.utils.StockPdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // KPI views
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalStock, tvDailyRevenue, tvRevenueLabel;
    private TextView tvDailyAdded, tvDailySold;
    private TextView tvTotalCustomers, tvTotalOutstanding;
    private TextView tvSelectedDate, tvDashboardDate, tvComparisonDate;
    private TextView tvNoLowStock, tvNoAddedStock, tvNoSoldStock;
    private TextView tvBarAddedCount, tvBarSoldCount;

    // Comparison bars
    private View barAdded, barSold;

    // Buttons
    private MaterialButton btnPickDate, btnExportDailyPdf;

    // Lists
    private RecyclerView rvLowStock, rvTopDebtors, rvDailyAdded, rvDailySold;

    private ApiService  api;
    private StockAdapter lowStockAdapter;
    private final List<Product> lowStockList = new ArrayList<>();

    // State
    private String selectedDate = DateUtils.today();
    private int    dailyAdded  = 0;
    private int    dailySold   = 0;
    private double dailyRevenue = 0;
    private JSONArray cachedAddDetail  = null;
    private JSONArray cachedSellDetail = null;

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
        swipeRefresh.setOnRefreshListener(this::loadAll);
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnExportDailyPdf.setOnClickListener(v -> exportDailyPdf());
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
        tvSelectedDate     = v.findViewById(R.id.tvSelectedDate);
        tvDashboardDate    = v.findViewById(R.id.tvDashboardDate);
        tvComparisonDate   = v.findViewById(R.id.tvComparisonDate);
        tvNoLowStock       = v.findViewById(R.id.tvNoLowStock);
        tvNoAddedStock     = v.findViewById(R.id.tvNoAddedStock);
        tvNoSoldStock      = v.findViewById(R.id.tvNoSoldStock);
        tvBarAddedCount    = v.findViewById(R.id.tvBarAddedCount);
        tvBarSoldCount     = v.findViewById(R.id.tvBarSoldCount);
        barAdded           = v.findViewById(R.id.barAdded);
        barSold            = v.findViewById(R.id.barSold);
        btnPickDate        = v.findViewById(R.id.btnPickDate);
        btnExportDailyPdf  = v.findViewById(R.id.btnExportDailyPdf);
        rvLowStock         = v.findViewById(R.id.rvLowStock);
        rvTopDebtors       = v.findViewById(R.id.rvTopDebtors);
        rvDailyAdded       = v.findViewById(R.id.rvDailyAdded);
        rvDailySold        = v.findViewById(R.id.rvDailySold);
    }

    private void setupRecyclerViews() {
        lowStockAdapter = new StockAdapter(requireContext(), lowStockList);
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setAdapter(lowStockAdapter);
        rvLowStock.setNestedScrollingEnabled(false);

        rvTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTopDebtors.setNestedScrollingEnabled(false);

        rvDailyAdded.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDailyAdded.setNestedScrollingEnabled(false);

        rvDailySold.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDailySold.setNestedScrollingEnabled(false);
    }

    private void loadAll() {
        swipeRefresh.setRefreshing(true);
        loadDashboardStats();
        loadDailyAnalytics();
        loadLowStock();
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
                        }
                    }
                } catch (Exception e) { Log.e(TAG, "Stats error", e); }
            }
            @Override public void onError(String error) { if (!isAdded()) return; }
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
                            dailyAdded   = d.optInt("total_added", 0);
                            dailySold    = d.optInt("total_sold",  0);
                            dailyRevenue = d.optDouble("revenue", 0);
                            cachedAddDetail  = d.optJSONArray("add_detail");
                            cachedSellDetail = d.optJSONArray("sell_detail");
                            bindDailyData(d);
                        }
                    }
                } catch (Exception e) { Log.e(TAG, "Daily error", e); }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Daily analytics error: " + error);
            }
        });
    }

    private void bindDailyData(JSONObject d) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

        if (tvDailyAdded != null) tvDailyAdded.setText(String.valueOf(dailyAdded));
        if (tvDailySold  != null) tvDailySold.setText(String.valueOf(dailySold));
        if (tvDailyRevenue != null) tvDailyRevenue.setText(fmt.format(dailyRevenue));

        int max = Math.max(dailyAdded, Math.max(dailySold, 1));
        if (tvBarAddedCount != null) tvBarAddedCount.setText(String.valueOf(dailyAdded));
        if (tvBarSoldCount  != null) tvBarSoldCount.setText(String.valueOf(dailySold));

        if (barAdded != null) {
            barAdded.post(() -> {
                ViewGroup parent = (ViewGroup) barAdded.getParent();
                int totalWidth = parent.getWidth();
                int addedWidth = (int) (totalWidth * ((float) dailyAdded / max));
                android.view.ViewGroup.LayoutParams lp = barAdded.getLayoutParams();
                lp.width = Math.max(addedWidth, 4);
                barAdded.setLayoutParams(lp);
            });
        }
        if (barSold != null) {
            barSold.post(() -> {
                ViewGroup parent = (ViewGroup) barSold.getParent();
                int totalWidth = parent.getWidth();
                int soldWidth = (int) (totalWidth * ((float) dailySold / max));
                android.view.ViewGroup.LayoutParams lp = barSold.getLayoutParams();
                lp.width = Math.max(soldWidth, 4);
                barSold.setLayoutParams(lp);
            });
        }

        try {
            JSONArray addArr = d.optJSONArray("add_detail");
            List<DailyStockItem> addList = new ArrayList<>();
            if (addArr != null) {
                for (int i = 0; i < addArr.length(); i++) {
                    JSONObject o = addArr.getJSONObject(i);
                    addList.add(new DailyStockItem(
                            o.optString("company_name"),
                            o.optString("model_name"),
                            o.optString("tire_type"),
                            o.optString("tire_size"),
                            o.optInt("qty_added"),
                            o.optString("created_at"),
                            true
                    ));
                }
            }
            DailyStockAdapter addAdapter = new DailyStockAdapter(requireContext(), addList);
            rvDailyAdded.setAdapter(addAdapter);
            if (tvNoAddedStock != null)
                tvNoAddedStock.setVisibility(addList.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) { Log.e(TAG, "Add detail bind error", e); }

        try {
            JSONArray sellArr = d.optJSONArray("sell_detail");
            List<DailyStockItem> sellList = new ArrayList<>();
            if (sellArr != null) {
                for (int i = 0; i < sellArr.length(); i++) {
                    JSONObject o = sellArr.getJSONObject(i);
                    sellList.add(new DailyStockItem(
                            o.optString("company_name"),
                            "",
                            o.optString("tire_type"),
                            o.optString("tire_size"),
                            o.optInt("quantity"),
                            o.optString("sale_date"),
                            false
                    ));
                }
            }
            DailyStockAdapter sellAdapter = new DailyStockAdapter(requireContext(), sellList);
            rvDailySold.setAdapter(sellAdapter);
            if (tvNoSoldStock != null)
                tvNoSoldStock.setVisibility(sellList.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) { Log.e(TAG, "Sell detail bind error", e); }
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
                        if (!lowStockList.isEmpty())
                            NotificationHelper.showLowStockAlert(requireContext(), lowStockList);
                    }
                } catch (Exception e) { Log.e(TAG, "LowStock error", e); }
            }
            @Override public void onError(String error) {}
        });
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
        String today = DateUtils.today();
        String label = selectedDate.equals(today) ? "Today" : selectedDate;
        if (tvSelectedDate    != null) tvSelectedDate.setText(label);
        if (tvDashboardDate   != null) tvDashboardDate.setText(label);
        if (tvComparisonDate  != null) tvComparisonDate.setText(label);
        if (tvRevenueLabel    != null) tvRevenueLabel.setText(selectedDate.equals(today) ? "Today Revenue" : "Revenue");
    }

    private void exportDailyPdf() {
        Toast.makeText(requireContext(), "Generating daily report…", Toast.LENGTH_SHORT).show();
        final String date  = selectedDate;
        final int    added = dailyAdded;
        final int    sold  = dailySold;
        final double rev   = dailyRevenue;
        final JSONArray addDetail  = cachedAddDetail;
        final JSONArray sellDetail = cachedSellDetail;

        new Thread(() -> {
            try {
                DailyReportPdfGenerator gen = new DailyReportPdfGenerator(requireContext());
                File pdf = gen.generate(date, added, sold, rev, addDetail, sellDetail);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pdf != null) gen.openPdf(pdf);
                        else Toast.makeText(requireContext(), "PDF failed", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void bindDebtors(JSONArray arr) {
        try {
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

    static class DailyStockItem {
        String company, model, tireType, tireSize, date;
        int qty;
        boolean isAdded;
        DailyStockItem(String co, String mo, String tt, String ts, int q, String d, boolean ia) {
            company=co; model=mo; tireType=tt; tireSize=ts; qty=q; date=d; isAdded=ia;
        }
    }

    static class DailyStockAdapter extends RecyclerView.Adapter<DailyStockAdapter.VH> {
        private final android.content.Context ctx;
        private final List<DailyStockItem> list;

        DailyStockAdapter(android.content.Context ctx, List<DailyStockItem> list) {
            this.ctx = ctx; this.list = list;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.item_daily_stock, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DailyStockItem item = list.get(pos);
            h.tvProduct.setText(item.company +
                    (item.model.isEmpty() ? "" : " • " + item.model) +
                    " | " + item.tireType + " (" + item.tireSize + ")");
            h.tvQty.setText(String.valueOf(item.qty));
            h.tvDate.setText(item.date.length() >= 16 ? item.date.substring(11, 16) : item.date);
            h.tvType.setText(item.isAdded ? "▲" : "▼");
            h.tvType.setTextColor(androidx.core.content.ContextCompat.getColor(ctx,
                    item.isAdded ? R.color.primary : R.color.warning));
            h.tvQty.setTextColor(androidx.core.content.ContextCompat.getColor(ctx,
                    item.isAdded ? R.color.primary : R.color.warning));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvProduct, tvQty, tvDate, tvType;
            VH(@NonNull android.view.View v) {
                super(v);
                tvProduct = v.findViewById(R.id.tvDailyProduct);
                tvQty     = v.findViewById(R.id.tvDailyQty);
                tvDate    = v.findViewById(R.id.tvDailyTime);
                tvType    = v.findViewById(R.id.tvDailyType);
            }
        }
    }

    static class DebtorItem {
        int id; String name, phone; double due;
        DebtorItem(int id, String n, String p, double d) { this.id=id; name=n; phone=p; due=d; }
    }

    static class TopDebtorAdapter extends RecyclerView.Adapter<TopDebtorAdapter.VH> {
        private final android.content.Context ctx;
        private final List<DebtorItem> list;

        TopDebtorAdapter(android.content.Context c, List<DebtorItem> l) { ctx=c; list=l; }

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
            android.widget.TextView tvName, tvPhone, tvDue;
            VH(@NonNull android.view.View v) {
                super(v);
                tvName  = v.findViewById(R.id.tvDebtorName);
                tvPhone = v.findViewById(R.id.tvDebtorPhone);
                tvDue   = v.findViewById(R.id.tvDebtorDue);
            }
        }
    }
}