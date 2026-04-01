// FILE: activities/CustomerDetailActivity.java
package com.smarttire.inventory.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.PaymentAdapter;
import com.smarttire.inventory.adapters.SalesAdapter;
import com.smarttire.inventory.models.Payment;
import com.smarttire.inventory.models.SaleRecord;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends AppCompatActivity {

    private TextView      tvName, tvPhone, tvAddress, tvGst;
    private TextView      tvTotalSales, tvTotalAmount, tvTotalPaid, tvTotalDue;
    private RecyclerView  rvSales, rvPayments;
    private ProgressBar   progressBar;
    private MaterialButton btnPayNow;

    private ApiService    api;
    private int           customerId;
    private double        currentDue;

    @Override protected void onCreate(Bundle savedInstanceState) {
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

        btnPayNow.setOnClickListener(v -> showPaymentDialog());

        loadDetails();
    }

    private void loadDetails() {
        progressBar.setVisibility(View.VISIBLE);
        api.getCustomerDetails(customerId, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (!response.getBoolean(ApiConfig.KEY_SUCCESS)) return;
                    JSONObject data     = response.getJSONObject(ApiConfig.KEY_DATA);
                    JSONObject customer = data.getJSONObject("customer");
                    JSONObject ledger   = data.getJSONObject("ledger");
                    JSONArray  sales    = data.getJSONArray("sales");
                    JSONArray  payments = data.getJSONArray("payments");

                    NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

                    tvName.setText(customer.optString("name"));
                    tvPhone.setText(customer.optString("phone"));
                    tvAddress.setText(customer.optString("address","—"));
                    tvGst.setText("GST: " + customer.optString("gst_number","—"));

                    tvTotalSales.setText(String.valueOf(ledger.optInt("total_sales")));
                    tvTotalAmount.setText(fmt.format(ledger.optDouble("total_amount")));
                    tvTotalPaid.setText(fmt.format(ledger.optDouble("total_paid")));
                    currentDue = ledger.optDouble("total_due");
                    tvTotalDue.setText(fmt.format(currentDue));
                    tvTotalDue.setTextColor(getColor(currentDue > 0
                            ? R.color.warning : R.color.success));

                    btnPayNow.setVisibility(currentDue > 0 ? View.VISIBLE : View.GONE);

                    // Bind sales
                    List<SaleRecord> saleList = new ArrayList<>();
                    for (int i = 0; i < sales.length(); i++)
                        saleList.add(SaleRecord.fromJSON(sales.getJSONObject(i)));
                    SalesAdapter sa = new SalesAdapter(CustomerDetailActivity.this, saleList);
                    rvSales.setAdapter(sa);

                    // Bind payments
                    List<Payment> payList = new ArrayList<>();
                    for (int i = 0; i < payments.length(); i++)
                        payList.add(Payment.fromJSON(payments.getJSONObject(i)));
                    PaymentAdapter pa = new PaymentAdapter(CustomerDetailActivity.this, payList);
                    rvPayments.setAdapter(pa);

                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPaymentDialog() {
        // Find the oldest unpaid/partial sale for this customer, then open PaymentActivity
        android.content.Intent intent = new android.content.Intent(this, PaymentActivity.class);
        intent.putExtra("customer_id",   customerId);
        intent.putExtra("customer_name", tvName.getText().toString());
        intent.putExtra("current_due",   currentDue);
        startActivityForResult(intent, 200);
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 200 && res == RESULT_OK) loadDetails(); // refresh after payment
    }
}