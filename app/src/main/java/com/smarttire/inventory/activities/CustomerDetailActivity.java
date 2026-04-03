// FILE: activities/CustomerDetailActivity.java  (FINAL — all payment fields, PDF receipt)
package com.smarttire.inventory.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.PaymentAdapter;
import com.smarttire.inventory.adapters.SalesAdapter;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.models.Payment;
import com.smarttire.inventory.models.SaleRecord;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.StockPdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends AppCompatActivity {

    private TextView       tvName, tvPhone, tvAddress, tvGst;
    private TextView       tvTotalSales, tvTotalAmount, tvTotalPaid, tvTotalDue;
    private RecyclerView   rvSales, rvPayments;
    private ProgressBar    progressBar;
    private MaterialButton btnPayNow;

    private ApiService api;
    private int        customerId;
    private double     currentDue, cachedTotalAmount, cachedTotalPaid;
    private Customer   currentCustomer;

    private final List<String[]> cachedSales    = new ArrayList<>();
    private final List<String[]> cachedPayments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        api = ApiService.getInstance(this);

        customerId = getIntent().getIntExtra("customer_id", 0);
        String customerName = getIntent().getStringExtra("customer_name");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(customerName != null ? customerName : "Customer");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvName        = findViewById(R.id.tvName);
        tvPhone       = findViewById(R.id.tvPhone);
        tvAddress     = findViewById(R.id.tvAddress);
        tvGst         = findViewById(R.id.tvGst);
        tvTotalSales  = findViewById(R.id.tvTotalSales);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalPaid   = findViewById(R.id.tvTotalPaid);
        tvTotalDue    = findViewById(R.id.tvTotalDue);
        rvSales       = findViewById(R.id.rvCustomerSales);
        rvPayments    = findViewById(R.id.rvPayments);
        progressBar   = findViewById(R.id.progressBar);
        btnPayNow     = findViewById(R.id.btnPayNow);

        rvSales.setLayoutManager(new LinearLayoutManager(this));
        rvSales.setNestedScrollingEnabled(false);
        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setNestedScrollingEnabled(false);

        btnPayNow.setOnClickListener(v -> showPaymentScreen());
        loadDetails();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Export PDF")
                .setIcon(R.drawable.ic_pdf)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) { exportCustomerPdf(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void loadDetails() {
        progressBar.setVisibility(View.VISIBLE);
        api.getCustomerDetails(customerId, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (!response.getBoolean(ApiConfig.KEY_SUCCESS)) return;
                    JSONObject data    = response.getJSONObject(ApiConfig.KEY_DATA);
                    JSONObject cust    = data.getJSONObject("customer");
                    JSONObject ledger  = data.getJSONObject("ledger");
                    JSONArray  sales   = data.getJSONArray("sales");
                    JSONArray  payments= data.getJSONArray("payments");

                    NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

                    tvName.setText(cust.optString("name"));
                    tvPhone.setText(cust.optString("phone"));
                    tvAddress.setText(cust.optString("address","—"));
                    String gst = cust.optString("gst_number","");
                    tvGst.setText("GST: " + (gst.isEmpty() ? "—" : gst));

                    currentCustomer = new Customer();
                    currentCustomer.setId(customerId);
                    currentCustomer.setName(cust.optString("name"));
                    currentCustomer.setPhone(cust.optString("phone"));
                    currentCustomer.setAddress(cust.optString("address",""));
                    currentCustomer.setGstNumber(gst);

                    tvTotalSales.setText(String.valueOf(ledger.optInt("total_sales")));
                    cachedTotalAmount = ledger.optDouble("total_amount");
                    cachedTotalPaid   = ledger.optDouble("total_paid");
                    currentDue        = ledger.optDouble("total_due");

                    tvTotalAmount.setText(fmt.format(cachedTotalAmount));
                    tvTotalPaid.setText(fmt.format(cachedTotalPaid));
                    tvTotalDue.setText(fmt.format(currentDue));
                    tvTotalDue.setTextColor(ContextCompat.getColor(
                            CustomerDetailActivity.this,
                            currentDue > 0 ? R.color.warning : R.color.success));

                    btnPayNow.setVisibility(currentDue > 0 ? View.VISIBLE : View.GONE);

                    // Sales — full payment fields via SaleRecord.fromJSON
                    List<SaleRecord> saleList = new ArrayList<>();
                    cachedSales.clear();
                    for (int i = 0; i < sales.length(); i++) {
                        SaleRecord sr = SaleRecord.fromJSON(sales.getJSONObject(i));
                        saleList.add(sr);
                        // PDF row: date | product | qty | total | paid | remaining | status
                        cachedSales.add(new String[]{
                                sr.getFormattedDate(),
                                sr.getProductDisplayName(),
                                String.valueOf(sr.getQuantity()),
                                sr.getFormattedTotalPrice(),
                                sr.getFormattedPaidAmount(),
                                sr.getFormattedRemainingAmount(),
                                sr.getPaymentStatus()
                        });
                    }
                    SalesAdapter sa = new SalesAdapter(CustomerDetailActivity.this, saleList);
                    rvSales.setAdapter(sa);

                    // Payments
                    List<Payment> payList = new ArrayList<>();
                    cachedPayments.clear();
                    for (int i = 0; i < payments.length(); i++) {
                        Payment py = Payment.fromJSON(payments.getJSONObject(i));
                        payList.add(py);
                        cachedPayments.add(new String[]{
                                py.getPaymentDate(),
                                py.getFormattedAmount(),
                                py.getPaymentMode().toUpperCase(),
                                py.getNote() != null && !py.getNote().isEmpty()
                                        ? py.getNote() : "—"
                        });
                    }
                    PaymentAdapter pa = new PaymentAdapter(CustomerDetailActivity.this, payList);
                    rvPayments.setAdapter(pa);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CustomerDetailActivity.this,
                            "Error loading details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportCustomerPdf() {
        if (currentCustomer == null) {
            Toast.makeText(this, "Data not loaded yet", Toast.LENGTH_SHORT).show(); return;
        }
        Toast.makeText(this, "Generating receipt PDF…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            StockPdfGenerator gen = new StockPdfGenerator(this);
            File pdf = gen.generateCustomerReportPdf(
                    currentCustomer,
                    cachedTotalAmount, cachedTotalPaid, currentDue,
                    cachedSales, cachedPayments);
            runOnUiThread(() -> {
                if (pdf != null) gen.openPdf(pdf);
                else Toast.makeText(this, "PDF failed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void showPaymentScreen() {
        android.content.Intent i = new android.content.Intent(this, PaymentActivity.class);
        i.putExtra("customer_id",   customerId);
        i.putExtra("customer_name", tvName.getText().toString());
        i.putExtra("current_due",   currentDue);
        startActivityForResult(i, 200);
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 200 && res == RESULT_OK) loadDetails();
    }
}