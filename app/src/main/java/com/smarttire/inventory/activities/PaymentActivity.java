// FILE: activities/PaymentActivity.java  (UPDATED — robust sale loading, clear validation)
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

    private TextInputLayout         tilAmount, tilSale;
    private TextInputEditText       etAmount, etNote;
    private AutoCompleteTextView    spinnerSale, spinnerMode;
    private TextView                tvDueLabel;
    private MaterialButton          btnRecord;
    private LinearProgressIndicator progressBar;

    private ApiService  api;
    private int         customerId;
    private double      currentDue;

    private final List<Integer> saleIds    = new ArrayList<>();
    private final List<String>  saleLabels = new ArrayList<>();
    private final List<Double>  saleDues   = new ArrayList<>();   // remaining per sale

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        api = ApiService.getInstance(this);

        customerId  = getIntent().getIntExtra("customer_id", 0);
        String customerName = getIntent().getStringExtra("customer_name");
        currentDue  = getIntent().getDoubleExtra("current_due", 0);

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

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvDueLabel.setText("Outstanding: " + fmt.format(currentDue));

        // Payment modes
        String[] modes = {"cash", "upi", "card", "bank_transfer", "cheque"};
        spinnerMode.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes));
        spinnerMode.setText(modes[0], false);

        // When sale is selected, pre-fill the amount field with the remaining due
        spinnerSale.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < saleDues.size()) {
                double due = saleDues.get(pos);
                etAmount.setText(String.format(Locale.getDefault(), "%.2f", due));
            }
        });

        loadPendingSales();
        btnRecord.setOnClickListener(v -> recordPayment());
    }

    private void loadPendingSales() {
        progressBar.setVisibility(View.VISIBLE);
        // Load ALL sales for this customer that have a remaining balance
        api.getSalesHistory("", "", "", customerId, "partial,unpaid", 1,
                new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            if (!response.getBoolean(ApiConfig.KEY_SUCCESS)) return;
                            JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                            saleIds.clear(); saleLabels.clear(); saleDues.clear();

                            NumberFormat fmt = NumberFormat.getCurrencyInstance(
                                    new Locale("en", "IN"));
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject o = data.getJSONObject(i);
                                double remaining = o.optDouble("remaining_amount", 0);
                                if (remaining <= 0) continue; // skip fully paid
                                int saleId = o.getInt("id");
                                saleIds.add(saleId);
                                saleDues.add(remaining);
                                saleLabels.add("Sale #" + saleId +
                                        " — Due: " + fmt.format(remaining) +
                                        " (" + o.optString("tire_size","") + ")");
                            }

                            spinnerSale.setAdapter(new ArrayAdapter<>(PaymentActivity.this,
                                    android.R.layout.simple_dropdown_item_1line, saleLabels));

                            if (!saleLabels.isEmpty()) {
                                spinnerSale.setText(saleLabels.get(0), false);
                                // Auto-fill amount with first sale's remaining due
                                etAmount.setText(String.format(Locale.getDefault(),
                                        "%.2f", saleDues.get(0)));
                            }

                            if (saleIds.isEmpty()) {
                                Toast.makeText(PaymentActivity.this,
                                        "No pending dues found for this customer.",
                                        Toast.LENGTH_LONG).show();
                                btnRecord.setEnabled(false);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(PaymentActivity.this,
                                    "Error loading sales", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PaymentActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordPayment() {
        tilAmount.setError(null);
        tilSale.setError(null);

        // Identify selected sale
        int selectedPos = saleLabels.indexOf(spinnerSale.getText().toString());
        if (selectedPos < 0 || saleIds.isEmpty()) {
            if (tilSale != null) tilSale.setError("Select a sale");
            else Toast.makeText(this, "Select a sale", Toast.LENGTH_SHORT).show();
            return;
        }

        String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        if (amtStr.isEmpty()) { tilAmount.setError("Enter amount"); return; }

        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) { tilAmount.setError("Invalid amount"); return; }
        if (amount <= 0) { tilAmount.setError("Amount must be > 0"); return; }

        // Validate: cannot exceed remaining due for this sale
        double saleRemaining = saleDues.get(selectedPos);
        if (amount > saleRemaining + 0.01) {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            tilAmount.setError("Cannot exceed sale due of " + fmt.format(saleRemaining));
            return;
        }

        int    saleId = saleIds.get(selectedPos);
        String mode   = spinnerMode.getText().toString();
        String note   = etNote.getText() != null ? etNote.getText().toString().trim() : "";

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