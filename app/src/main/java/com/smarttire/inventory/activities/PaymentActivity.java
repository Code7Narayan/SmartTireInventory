// FILE: activities/PaymentActivity.java
package com.smarttire.inventory.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smarttire.inventory.R;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private TextInputLayout           tilAmount, tilSale;
    private TextInputEditText         etAmount, etNote;
    private AutoCompleteTextView      spinnerSale, spinnerMode;
    private TextView                  tvDueLabel;
    private MaterialButton            btnRecord;
    private LinearProgressIndicator   progressBar;

    private ApiService  api;
    private int         customerId;
    private double      currentDue;

    // sale_id → display string mapping
    private List<Integer> saleIds    = new ArrayList<>();
    private List<String>  saleLabels = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        api = ApiService.getInstance(this);

        customerId = getIntent().getIntExtra("customer_id", 0);
        String customerName = getIntent().getStringExtra("customer_name");
        currentDue          = getIntent().getDoubleExtra("current_due", 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Record Payment");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tilAmount   = findViewById(R.id.tilAmount);
        tilSale     = findViewById(R.id.tilSale);
        etAmount    = findViewById(R.id.etAmount);
        etNote      = findViewById(R.id.etNote);
        spinnerSale = findViewById(R.id.spinnerSale);
        spinnerMode = findViewById(R.id.spinnerPaymentMode);
        tvDueLabel  = findViewById(R.id.tvDueLabel);
        btnRecord   = findViewById(R.id.btnRecordPayment);
        progressBar = findViewById(R.id.progressBar);

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
        tvDueLabel.setText("Outstanding: " + fmt.format(currentDue));

        // Payment mode spinner
        String[] modes = {"cash","upi","card","bank_transfer","cheque"};
        spinnerMode.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes));
        spinnerMode.setText(modes[0], false);

        // Load pending sales for this customer
        loadPendingSales();

        btnRecord.setOnClickListener(v -> recordPayment());
    }

    private void loadPendingSales() {
        progressBar.setVisibility(View.VISIBLE);
        api.getSalesHistory("", "", "", customerId, "partial,unpaid", 1,
                new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            if (!response.getBoolean(ApiConfig.KEY_SUCCESS)) return;
                            JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                            saleIds.clear(); saleLabels.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject o = data.getJSONObject(i);
                                saleIds.add(o.getInt("id"));
                                saleLabels.add("Sale #" + o.getInt("id")
                                        + " — Due: ₹" + String.format(Locale.getDefault(),"%.2f",
                                        o.getDouble("remaining_amount")));
                            }
                            spinnerSale.setAdapter(new ArrayAdapter<>(PaymentActivity.this,
                                    android.R.layout.simple_dropdown_item_1line, saleLabels));
                            if (!saleLabels.isEmpty()) spinnerSale.setText(saleLabels.get(0), false);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PaymentActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordPayment() {
        tilAmount.setError(null);
        String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        if (amtStr.isEmpty()) { tilAmount.setError("Enter amount"); return; }

        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) { tilAmount.setError("Invalid amount"); return; }
        if (amount <= 0) { tilAmount.setError("Amount must be > 0"); return; }

        // Identify selected sale
        int selectedPos = saleLabels.indexOf(spinnerSale.getText().toString());
        if (selectedPos < 0 || saleIds.isEmpty()) {
            Toast.makeText(this, "Select a sale", Toast.LENGTH_SHORT).show(); return;
        }
        int saleId = saleIds.get(selectedPos);
        String mode = spinnerMode.getText().toString();
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        progressBar.setVisibility(View.VISIBLE);
        btnRecord.setEnabled(false);

        api.addPayment(saleId, amount, mode, note, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnRecord.setEnabled(true);
                try {
                    boolean ok  = response.getBoolean(ApiConfig.KEY_SUCCESS);
                    String  msg = response.getString(ApiConfig.KEY_MESSAGE);
                    Toast.makeText(PaymentActivity.this, msg, Toast.LENGTH_SHORT).show();
                    if (ok) { setResult(Activity.RESULT_OK); finish(); }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnRecord.setEnabled(true);
                Toast.makeText(PaymentActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}