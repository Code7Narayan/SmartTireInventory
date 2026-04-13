// FILE: activities/StockDetailActivity.java (UPDATED - Simplified UI)
package com.smarttire.inventory.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
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
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Company;
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

    private TextView        tvCompany, tvModel, tvAddedDate;
    private TextView        tvTotalAdded, tvTotalSold, tvCurrentStock;
    private MaterialButton  btnSell, btnExportPdf, btnDelete;
    private ImageButton     btnEdit;
    private RecyclerView    rvTransactions;
    private ProgressBar     progressBar;
    private LinearLayout    layoutContent;

    private ApiService api;
    private int        productId;
    private JSONObject productData;
    private final List<Company> companyList = new ArrayList<>();

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
        tvTotalAdded   = findViewById(R.id.tvDetailTotalAdded);
        tvTotalSold    = findViewById(R.id.tvDetailTotalSold);
        tvCurrentStock = findViewById(R.id.tvDetailCurrentStock);
        tvAddedDate    = findViewById(R.id.tvDetailAddedDate);
        btnSell        = findViewById(R.id.btnDetailSell);
        btnExportPdf   = findViewById(R.id.btnDetailExportPdf);
        btnDelete      = findViewById(R.id.btnDetailDelete);
        btnEdit        = findViewById(R.id.btnDetailEdit);
        rvTransactions = findViewById(R.id.rvDetailTransactions);
        progressBar    = findViewById(R.id.progressBar);
        layoutContent  = findViewById(R.id.layoutContent);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setNestedScrollingEnabled(false);

        if (btnDelete != null) btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        if (btnEdit != null)   btnEdit.setOnClickListener(v -> showEditDialog());

        loadDetail();
        loadCompanies();
    }

    private void loadDetail() {
        progressBar.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);

        api.getProductDetail(productId, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (!response.optBoolean("success")) return;

                    JSONObject data = response.optJSONObject("data");
                    if (data == null) return;

                    productData = data;
                    bindUI(data);
                    layoutContent.setVisibility(View.VISIBLE);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void bindUI(JSONObject data) {
        int totalAdded      = data.optInt("total_added", 0);
        int totalSold       = data.optInt("total_sold", 0);
        int currentStock    = data.optInt("quantity", data.optInt("current_stock", 0));
        JSONArray txList    = data.optJSONArray("transactions");

        tvCompany.setText(data.optString("company_name", "—"));
        tvModel.setText(data.optString("model_name", "—"));
        tvTotalAdded.setText(String.valueOf(totalAdded));
        tvTotalSold.setText(String.valueOf(totalSold));
        tvCurrentStock.setText(String.valueOf(currentStock));

        if (currentStock < 5) tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.warning));
        else tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.success));

        tvAddedDate.setText(formatDate(data.optString("created_at", "")));

        List<String[]> txRows = new ArrayList<>();
        if (txList != null) {
            for (int i = 0; i < txList.length(); i++) {
                JSONObject tx = txList.optJSONObject(i);
                if (tx != null) {
                    txRows.add(new String[]{
                            tx.optString("type", ""),
                            String.valueOf(tx.optInt("quantity", 0)),
                            tx.optString("note", ""),
                            formatDate(tx.optString("created_at", ""))
                    });
                }
            }
        }
        rvTransactions.setAdapter(new StockTransactionAdapter(this, txRows));

        btnSell.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_sell", true);
            intent.putExtra("preselect_product_id", productId);
            // Simplified summary for selection
            intent.putExtra("preselect_product_name", data.optString("company_name") + " | " + data.optString("model_name"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnExportPdf.setOnClickListener(v -> exportPdf(data, totalAdded, totalSold, currentStock, txRows));
    }

    private void showEditDialog() {
        if (productData == null) return;
        try {
            JSONObject p = productData;
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_stock, null);
            b.setView(v);

            AutoCompleteTextView spinCompany = v.findViewById(R.id.editStockCompany);
            TextInputEditText etModel        = v.findViewById(R.id.editStockModel);

            // Setup spinners
            ArrayAdapter<Company> cAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, companyList);
            spinCompany.setAdapter(cAdapter);

            // Set current values
            spinCompany.setText(p.optString("company_name"), false);
            etModel.setText(p.optString("model_name"));

            b.setTitle("Edit Product Details");
            b.setPositiveButton("Update", (dialog, which) -> {
                // Keep original values for fields removed from UI
                String type = p.optString("tire_type");
                String size = p.optString("tire_size");
                double price = p.optDouble("price", 0);
                double cost = p.optDouble("cost_price", 0);
                
                performUpdate(spinCompany.getText().toString(), type, etModel.getText().toString(), size, String.valueOf(price), String.valueOf(cost));
            });
            b.setNegativeButton("Cancel", null);
            b.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void performUpdate(String company, String type, String model, String size, String price, String cost) {
        int cid = 0;
        for (Company c : companyList) if (c.getName().equals(company)) { cid = c.getId(); break; }
        if (cid == 0) { Toast.makeText(this, "Select a valid company", Toast.LENGTH_SHORT).show(); return; }

        progressBar.setVisibility(View.VISIBLE);
        api.updateProductFull(productId, cid, type, size, model, Double.parseDouble(price), Double.parseDouble(cost), new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                if (response.optBoolean("success")) {
                    Toast.makeText(StockDetailActivity.this, "Product updated", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else Toast.makeText(StockDetailActivity.this, response.optString("message"), Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String error) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void loadCompanies() {
        api.getCompanies(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                try {
                    if (response.optBoolean("success")) {
                        JSONArray dataArr = response.optJSONArray("data");
                        if (dataArr != null) {
                            companyList.clear();
                            for (int i = 0; i < dataArr.length(); i++) {
                                JSONObject obj = dataArr.getJSONObject(i);
                                companyList.add(new Company(obj.getInt("id"), obj.getString("name")));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            @Override public void onError(String error) {}
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure? This will PERMANENTLY delete this product along with all its sales history and payments.")
                .setPositiveButton("Delete Everything", (dialog, which) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete() {
        progressBar.setVisibility(View.VISIBLE);
        api.updateProduct(productId, "delete", -1, 0, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                if (response.optBoolean("success")) {
                    Toast.makeText(StockDetailActivity.this, "Product deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else Toast.makeText(StockDetailActivity.this, response.optString("message"), Toast.LENGTH_LONG).show();
            }
            @Override public void onError(String error) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void exportPdf(JSONObject product, int totalAdded, int totalSold, int currentStock, List<String[]> txRows) {
        try {
            StockPdfGenerator gen = new StockPdfGenerator(this);
            // We pass original values for PDF if needed, or update PDF generator as well. 
            // For now, keeping the call signature as is but passing existing data.
            java.io.File pdf = gen.generateStockDetailPdf(product.optString("company_name"), product.optString("model_name"), product.optString("tire_type"), product.optString("tire_size"), product.optDouble("price", 0), totalAdded, totalSold, currentStock, txRows);
            if (pdf != null) gen.openPdf(pdf);
        } catch (Exception e) { Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
    }


    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
            Date d = in.parse(raw);
            return d != null ? out.format(d) : raw;
        } catch (Exception e) { return raw; }
    }

    static class StockTransactionAdapter extends RecyclerView.Adapter<StockTransactionAdapter.VH> {
        private final android.content.Context ctx;
        private final List<String[]> list;
        StockTransactionAdapter(android.content.Context ctx, List<String[]> list) { this.ctx = ctx; this.list = list; }
        @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(android.view.LayoutInflater.from(ctx).inflate(R.layout.item_stock_transaction, parent, false));
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            String[] row = list.get(pos);
            boolean isIn = "IN".equals(row[0]);
            h.tvType.setText(isIn ? "▲ IN" : "▼ OUT");
            h.tvType.setTextColor(ContextCompat.getColor(ctx, isIn ? R.color.success : R.color.error));
            h.tvQty.setText("Qty: " + row[1]);
            h.tvNote.setText(row[2]);
            h.tvDate.setText(row[3]);
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvType, tvQty, tvNote, tvDate;
            VH(android.view.View v) { super(v); tvType = v.findViewById(R.id.tvTxType); tvQty = v.findViewById(R.id.tvTxQty); tvNote = v.findViewById(R.id.tvTxNote); tvDate = v.findViewById(R.id.tvTxDate); }
        }
    }
}
