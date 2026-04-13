// FILE: activities/AddStockActivity.java (UPDATED - Simplified for new Backend)
package com.smarttire.inventory.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Company;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.KeyboardUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddStockActivity extends AppCompatActivity {

    private MaterialToolbar      toolbar;
    private TextInputLayout      tilCompany, tilModelName, tilQuantity, tilCostPrice, tilTotalCost;
    private AutoCompleteTextView spinnerCompany;
    private TextInputEditText    etModelName, etQuantity, etCostPrice, etTotalCost;
    private MaterialButton       btnSaveStock;
    private LinearProgressIndicator progressBar;

    private ApiService   apiService;
    private List<Company> companyList = new ArrayList<>();
    private Company       selectedCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stock);

        apiService = ApiService.getInstance(this);
        initViews();
        setupToolbar();
        setupListeners();
        loadCompanies();
    }

    private void initViews() {
        toolbar        = findViewById(R.id.toolbar);
        tilCompany     = findViewById(R.id.tilCompany);
        tilModelName   = findViewById(R.id.tilModelName);
        tilQuantity    = findViewById(R.id.tilQuantity);
        tilCostPrice   = findViewById(R.id.tilCostPrice);
        tilTotalCost   = findViewById(R.id.tilTotalCost);
        
        spinnerCompany = findViewById(R.id.spinnerCompany);
        etModelName    = findViewById(R.id.etModelName);
        etQuantity     = findViewById(R.id.etQuantity);
        etCostPrice    = findViewById(R.id.etCostPrice);
        etTotalCost    = findViewById(R.id.etTotalCost);
        
        btnSaveStock   = findViewById(R.id.btnSaveStock);
        progressBar    = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        spinnerCompany.setOnItemClickListener((parent, view, position, id) -> {
                selectedCompany = companyList.get(position);
                KeyboardUtils.hideKeyboard(this);
        });

        TextWatcher calculationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                calculateTotal();
            }
        };

        etQuantity.addTextChangedListener(calculationWatcher);
        etCostPrice.addTextChangedListener(calculationWatcher);

        btnSaveStock.setOnClickListener(v -> saveStock());
    }

    private void calculateTotal() {
        try {
            String qStr = etQuantity.getText().toString();
            String cStr = etCostPrice.getText().toString();
            if (!qStr.isEmpty() && !cStr.isEmpty()) {
                double qty = Double.parseDouble(qStr);
                double cost = Double.parseDouble(cStr);
                double total = qty * cost;
                etTotalCost.setText(String.format(Locale.getDefault(), "%.2f", total));
            } else {
                etTotalCost.setText("");
            }
        } catch (Exception e) {
            etTotalCost.setText("");
        }
    }

    private void loadCompanies() {
        showLoading(true);
        apiService.getCompanies(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                showLoading(false);
                try {
                    if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                        companyList.clear();
                        int mrfIndex = -1;
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            Company c = new Company(obj.getInt("id"), obj.getString("name"));
                            companyList.add(c);
                            if (c.getName().equalsIgnoreCase("MRF")) {
                                mrfIndex = i;
                            }
                        }
                        ArrayAdapter<Company> adapter = new ArrayAdapter<>(
                                AddStockActivity.this,
                                android.R.layout.simple_dropdown_item_1line,
                                companyList);
                        spinnerCompany.setAdapter(adapter);

                        // Auto-select MRF if found
                        if (mrfIndex != -1) {
                            selectedCompany = companyList.get(mrfIndex);
                            spinnerCompany.setText(selectedCompany.getName(), false);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(AddStockActivity.this, "Error loading companies", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onError(String error) {
                showLoading(false);
                Toast.makeText(AddStockActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStock() {
        if (tilCompany   != null) tilCompany.setError(null);
        if (tilModelName != null) tilModelName.setError(null);
        if (tilQuantity  != null) tilQuantity.setError(null);
        if (tilCostPrice != null) tilCostPrice.setError(null);

        if (selectedCompany == null) {
            if (tilCompany != null) tilCompany.setError("Please select a company");
            return;
        }

        String modelName = etModelName.getText() != null
                ? etModelName.getText().toString().trim() : "";
        if (modelName.isEmpty()) {
            if (tilModelName != null) tilModelName.setError("Model name is required");
            etModelName.requestFocus();
            return;
        }

        String quantityStr = etQuantity.getText() != null
                ? etQuantity.getText().toString().trim() : "";
        if (quantityStr.isEmpty()) {
            if (tilQuantity != null) tilQuantity.setError("Quantity is required");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            if (tilQuantity != null) tilQuantity.setError("Invalid quantity");
            return;
        }

        String costPriceStr = etCostPrice.getText() != null ? etCostPrice.getText().toString().trim() : "";
        if (costPriceStr.isEmpty()) {
            if (tilCostPrice != null) tilCostPrice.setError("Cost price is required");
            return;
        }
        
        double costPrice;
        try {
            costPrice = Double.parseDouble(costPriceStr);
        } catch (NumberFormatException e) {
            if (tilCostPrice != null) tilCostPrice.setError("Invalid cost price");
            return;
        }

        showLoading(true);
        KeyboardUtils.hideKeyboard(this);
        // We pass costPrice as both 'price' and 'cost_price' to satisfy backend validation
        // since selling price (price) was removed from the UI.
        apiService.addProduct(selectedCompany.getId(), "General", modelName,
                modelName, quantity, costPrice, costPrice, new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject response) {
                        showLoading(false);
                        if (response.optBoolean(ApiConfig.KEY_SUCCESS)) {
                            Toast.makeText(AddStockActivity.this, "Stock added successfully!", Toast.LENGTH_SHORT).show();
                            clearForm();
                        } else {
                            Toast.makeText(AddStockActivity.this, response.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(AddStockActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearForm() {
        etModelName.setText("");
        etQuantity.setText("");
        etCostPrice.setText("");
        etTotalCost.setText("");
    }

    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnSaveStock != null)
            btnSaveStock.setEnabled(!show);
    }
}