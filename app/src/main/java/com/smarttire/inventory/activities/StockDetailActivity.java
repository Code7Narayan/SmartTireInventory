// FILE: activities/StockDetailActivity.java
package com.smarttire.inventory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.smarttire.inventory.R;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.StockPdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID   = "product_id";
    public static final String EXTRA_PRODUCT_NAME = "product_name";

    private TextView        tvCompany, tvModel, tvTireType, tvTireSize, tvPrice;
    private TextView        tvTotalAdded, tvTotalSold, tvCurrentStock, tvAddedDate;
    private MaterialButton  btnSell, btnExportPdf;
    private RecyclerView    rvTransactions;
    private ProgressBar     progressBar;
    private LinearLayout    layoutContent;

    private ApiService api;
    private int        productId;
    private JSONObject productData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        api       = ApiService.getInstance(this);
        productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(getIntent().getStringExtra(EXTRA_PRODUCT_NAME));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvCompany      = findViewById(R.id.tvDetailCompany);
        tvModel        = findViewById(R.id.tvDetailModel);
        tvTireType     = findViewById(R.id.tvDetailTireType);
        tvTireSize     = findViewById(R.id.tvDetailTireSize);
        tvPrice        = findViewById(R.id.tvDetailPrice);
        tvTotalAdded   = findViewById(R.id.tvDetailTotalAdded);
        tvTotalSold    = findViewById(R.id.tvDetailTotalSold);
        tvCurrentStock = findViewById(R.id.tvDetailCurrentStock);
        tvAddedDate    = findViewById(R.id.tvDetailAddedDate);
        btnSell        = findViewById(R.id.btnDetailSell);
        btnExportPdf   = findViewById(R.id.btnDetailExportPdf);
        rvTransactions = findViewById(R.id.rvDetailTransactions);
        progressBar    = findViewById(R.id.progressBar);
        layoutContent  = findViewById(R.id.layoutContent);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setNestedScrollingEnabled(false);

        loadDetail();
    }

    private void loadDetail() {
        progressBar.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);

        api.getProductDetail(productId, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (!response.getBoolean(ApiConfig.KEY_SUCCESS)) return;
                    JSONObject data = response.getJSONObject(ApiConfig.KEY_DATA);
                    productData     = data;
                    bindUI(data);
                    layoutContent.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(StockDetailActivity.this,
                            "Error loading detail", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StockDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUI(JSONObject data) throws Exception {
        JSONObject product  = data.getJSONObject("product");
        int totalAdded      = data.getInt("total_added");
        int totalSold       = data.getInt("total_sold");
        int currentStock    = data.getInt("current_stock");
        JSONArray txList    = data.getJSONArray("transactions");

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        tvCompany.setText(product.optString("company_name", "—"));
        tvModel.setText(product.optString("model_name", "—"));
        tvTireType.setText(product.optString("tire_type", "—"));
        tvTireSize.setText(product.optString("tire_size", "—"));
        tvPrice.setText(fmt.format(product.optDouble("price", 0)));
        tvTotalAdded.setText(String.valueOf(totalAdded));
        tvTotalSold.setText(String.valueOf(totalSold));
        tvCurrentStock.setText(String.valueOf(currentStock));

        // Color code current stock
        if (currentStock < 5) {
            tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.warning));
        } else {
            tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.success));
        }

        // Format added date
        String createdAt = product.optString("created_at", "");
        tvAddedDate.setText(formatDate(createdAt));

        // Transaction history adapter
        List<String[]> txRows = new ArrayList<>();
        for (int i = 0; i < txList.length(); i++) {
            JSONObject tx = txList.getJSONObject(i);
            String type   = tx.optString("type", "");
            String qty    = String.valueOf(tx.optInt("quantity", 0));
            String note   = tx.optString("note", "");
            String date   = formatDate(tx.optString("created_at", ""));
            txRows.add(new String[]{type, qty, note, date});
        }
        StockTransactionAdapter adapter = new StockTransactionAdapter(this, txRows);
        rvTransactions.setAdapter(adapter);

        // Buttons
        btnSell.setOnClickListener(v -> {
            // Open MainActivity → SellFragment with product pre-selected
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_sell", true);
            intent.putExtra("preselect_product_id",   productId);
            intent.putExtra("preselect_product_name",
                    product.optString("company_name") + " | " +
                            product.optString("model_name") + " " +
                            product.optString("tire_type") + " (" +
                            product.optString("tire_size") + ")");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnExportPdf.setOnClickListener(v -> exportPdf(product, totalAdded, totalSold, currentStock, txRows));
    }

    private void exportPdf(JSONObject product, int totalAdded, int totalSold,
                           int currentStock, List<String[]> txRows) {
        try {
            StockPdfGenerator gen = new StockPdfGenerator(this);
            java.io.File pdf = gen.generateStockDetailPdf(
                    product.optString("company_name"),
                    product.optString("model_name"),
                    product.optString("tire_type"),
                    product.optString("tire_size"),
                    product.optDouble("price", 0),
                    totalAdded, totalSold, currentStock,
                    txRows
            );
            if (pdf != null) gen.openPdf(pdf);
            else Toast.makeText(this, "PDF generation failed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
            Date d = in.parse(raw);
            return d != null ? out.format(d) : raw;
        } catch (Exception e) {
            return raw;
        }
    }

    // ── Inner adapter for transaction list ────────────────────────────────────
    static class StockTransactionAdapter
            extends RecyclerView.Adapter<StockTransactionAdapter.VH> {

        private final android.content.Context ctx;
        private final List<String[]> list; // [type, qty, note, date]

        StockTransactionAdapter(android.content.Context ctx, List<String[]> list) {
            this.ctx  = ctx;
            this.list = list;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.item_stock_transaction, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            String[] row = list.get(pos);
            boolean isIn = "IN".equals(row[0]);
            h.tvType.setText(isIn ? "▲ IN" : "▼ OUT");
            h.tvType.setTextColor(ContextCompat.getColor(ctx,
                    isIn ? R.color.success : R.color.error));
            h.tvQty.setText("Qty: " + row[1]);
            h.tvNote.setText(row[2]);
            h.tvDate.setText(row[3]);
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvType, tvQty, tvNote, tvDate;
            VH(android.view.View v) {
                super(v);
                tvType = v.findViewById(R.id.tvTxType);
                tvQty  = v.findViewById(R.id.tvTxQty);
                tvNote = v.findViewById(R.id.tvTxNote);
                tvDate = v.findViewById(R.id.tvTxDate);
            }
        }
    }
}