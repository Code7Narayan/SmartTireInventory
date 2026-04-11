package com.smarttire.inventory.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.PaymentAdapter;
import com.smarttire.inventory.adapters.SalesAdapter;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONObject;

import java.util.ArrayList;

public class CustomerDetailActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvAddress, tvGst;
    private TextView tvTotalSales, tvTotalAmount, tvTotalPaid, tvTotalDue;
    private RecyclerView rvSales, rvPayments;
    private ProgressBar progressBar;
    private ImageButton btnEdit;
    private MaterialButton btnDelete;
    private MaterialToolbar toolbar;

    private ApiService api;
    private int customerId;

    private Customer currentCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        api = ApiService.getInstance(this);

        customerId = getIntent().getIntExtra("customer_id", 0);
        String name = getIntent().getStringExtra("customer_name");

        if (customerId == 0) {
            Toast.makeText(this, "Invalid Customer ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name != null ? name : "Customer Details");
        }

        // Views
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvGst = findViewById(R.id.tvGst);

        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalDue = findViewById(R.id.tvTotalDue);

        rvSales = findViewById(R.id.rvCustomerSales);
        rvPayments = findViewById(R.id.rvPayments);
        progressBar = findViewById(R.id.progressBar);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        rvSales.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setLayoutManager(new LinearLayoutManager(this));

        rvSales.setAdapter(new SalesAdapter(this, new ArrayList<>()));
        rvPayments.setAdapter(new PaymentAdapter(this, new ArrayList<>()));

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        loadDetails();
    }

    // 🔥 LOAD DETAILS
    private void loadDetails() {
        progressBar.setVisibility(View.VISIBLE);

        api.getCustomerDetails(customerId, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);

                try {
                    Log.d("DETAIL_RESPONSE", response.toString());

                    if (!response.optBoolean("success")) return;

                    JSONObject data = response.getJSONObject("data");
                    JSONObject cust = data.getJSONObject("customer");
                    JSONObject ledger = data.getJSONObject("ledger");

                    tvName.setText(cust.optString("name"));
                    tvPhone.setText(cust.optString("phone"));
                    tvAddress.setText(cust.optString("address"));
                    tvGst.setText("GST: " + cust.optString("gst_number"));

                    tvTotalSales.setText(String.valueOf(ledger.optInt("total_sales")));
                    tvTotalAmount.setText("₹ " + ledger.optDouble("total_amount"));
                    tvTotalPaid.setText("₹ " + ledger.optDouble("total_paid"));
                    tvTotalDue.setText("₹ " + ledger.optDouble("total_due"));

                    currentCustomer = new Customer();
                    currentCustomer.setId(customerId);
                    currentCustomer.setName(cust.optString("name"));
                    currentCustomer.setPhone(cust.optString("phone"));
                    currentCustomer.setAddress(cust.optString("address"));
                    currentCustomer.setGstNumber(cust.optString("gst_number"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✏️ EDIT
    private void showEditDialog() {
        if (currentCustomer == null) return;

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_customer, null);

        TextInputEditText etName = v.findViewById(R.id.editCustomerName);
        TextInputEditText etPhone = v.findViewById(R.id.editCustomerPhone);
        TextInputEditText etAddress = v.findViewById(R.id.editCustomerAddress);
        TextInputEditText etGst = v.findViewById(R.id.editCustomerGst);

        etName.setText(currentCustomer.getName());
        etPhone.setText(currentCustomer.getPhone());
        etAddress.setText(currentCustomer.getAddress());
        etGst.setText(currentCustomer.getGstNumber());

        new AlertDialog.Builder(this)
                .setTitle("Edit Customer")
                .setView(v)
                .setPositiveButton("Update", (d, w) -> performUpdate(
                        etName.getText().toString(),
                        etPhone.getText().toString(),
                        etAddress.getText().toString(),
                        etGst.getText().toString()
                ))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUpdate(String name, String phone, String address, String gst) {
        progressBar.setVisibility(View.VISIBLE);

        api.updateCustomer(customerId, name, phone, address, gst, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);

                Toast.makeText(CustomerDetailActivity.this,
                        response.optString("message"),
                        Toast.LENGTH_SHORT).show();

                if (response.optBoolean("success")) {
                    loadDetails();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🗑 DELETE
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (d, w) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete() {
        progressBar.setVisibility(View.VISIBLE);

        api.deleteCustomer(customerId, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);

                Toast.makeText(CustomerDetailActivity.this,
                        response.optString("message"),
                        Toast.LENGTH_SHORT).show();

                if (response.optBoolean("success")) {
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}