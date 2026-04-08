// FILE: activities/AddStockActivity.java  (UPDATED — tire size field removed from UI)
package com.smarttire.inventory.activities;

import android.os.Bundle;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddStockActivity extends AppCompatActivity {

    private MaterialToolbar      toolbar;
    private TextInputLayout      tilCompany, tilTireType, tilModelName, tilQuantity, tilPrice;
    private AutoCompleteTextView spinnerCompany, spinnerTireType;
    private TextInputEditText    etModelName, etQuantity, etPrice;
    // etTireSize is kept as a hidden view for backend compatibility — always set to model name
    private MaterialButton       btnSaveStock;
    private LinearProgressIndicator progressBar;

    private ApiService   apiService;
    private List<Company> companyList   = new ArrayList<>();
    private Company       selectedCompany;
    private String        selectedTireType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stock);

        apiService = ApiService.getInstance(this);
        initViews();
        setupToolbar();
        setupTireTypeSpinner();
        setupListeners();
        loadCompanies();
    }

    private void initViews() {
        toolbar       = findViewById(R.id.toolbar);
        tilCompany    = findViewById(R.id.tilCompany);
        tilTireType   = findViewById(R.id.tilTireType);
        tilModelName  = findViewById(R.id.tilModelName);
        tilQuantity   = findViewById(R.id.tilQuantity);
        tilPrice      = findViewById(R.id.tilPrice);
        spinnerCompany  = findViewById(R.id.spinnerCompany);
        spinnerTireType = findViewById(R.id.spinnerTireType);
        etModelName   = findViewById(R.id.etModelName);
        etQuantity    = findViewById(R.id.etQuantity);
        etPrice       = findViewById(R.id.etPrice);
        btnSaveStock  = findViewById(R.id.btnSaveStock);
        progressBar   = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupTireTypeSpinner() {
        String[] tireTypes = getResources().getStringArray(R.array.tire_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tireTypes);
        spinnerTireType.setAdapter(adapter);
        spinnerTireType.setOnItemClickListener((parent, view, position, id) ->
                selectedTireType = tireTypes[position]);
    }

    private void setupListeners() {
        spinnerCompany.setOnItemClickListener((parent, view, position, id) ->
                selectedCompany = companyList.get(position));
        btnSaveStock.setOnClickListener(v -> saveStock());
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
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            companyList.add(new Company(obj.getInt("id"), obj.getString("name")));
                        }
                        ArrayAdapter<Company> adapter = new ArrayAdapter<>(
                                AddStockActivity.this,
                                android.R.layout.simple_dropdown_item_1line,
                                companyList);
                        spinnerCompany.setAdapter(adapter);
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
        // Clear previous errors
        if (tilCompany   != null) tilCompany.setError(null);
        if (tilTireType  != null) tilTireType.setError(null);
        if (tilModelName != null) tilModelName.setError(null);
        if (tilQuantity  != null) tilQuantity.setError(null);
        if (tilPrice     != null) tilPrice.setError(null);

        if (selectedCompany == null) {
            if (tilCompany != null) tilCompany.setError("Please select a company");
            return;
        }
        if (selectedTireType == null || selectedTireType.isEmpty()) {
            if (tilTireType != null) tilTireType.setError("Please select tire type");
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
            etQuantity.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                if (tilQuantity != null) tilQuantity.setError("Quantity must be > 0");
                return;
            }
        } catch (NumberFormatException e) {
            if (tilQuantity != null) tilQuantity.setError("Invalid quantity");
            return;
        }

        String priceStr = etPrice.getText() != null
                ? etPrice.getText().toString().trim() : "";
        if (priceStr.isEmpty()) {
            if (tilPrice != null) tilPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                if (tilPrice != null) tilPrice.setError("Price must be > 0");
                return;
            }
        } catch (NumberFormatException e) {
            if (tilPrice != null) tilPrice.setError("Invalid price");
            return;
        }

        // Use model name as tire size for backend compatibility (tire size field removed from UI)
        // This keeps the backend addProduct.php working without schema changes.
        String tireSize = modelName;

        showLoading(true);
        apiService.addProduct(selectedCompany.getId(), selectedTireType, tireSize,
                modelName, quantity, price, new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject response) {
                        showLoading(false);
                        try {
                            boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);
                            String  message = response.getString(ApiConfig.KEY_MESSAGE);
                            if (success) {
                                Toast.makeText(AddStockActivity.this,
                                        "Stock added successfully!", Toast.LENGTH_SHORT).show();
                                clearForm();
                            } else {
                                Toast.makeText(AddStockActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AddStockActivity.this,
                                    "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(AddStockActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearForm() {
        spinnerCompany.setText("", false);
        spinnerTireType.setText("", false);
        etModelName.setText("");
        etQuantity.setText("");
        etPrice.setText("");
        selectedCompany  = null;
        selectedTireType = null;
    }

    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnSaveStock != null)
            btnSaveStock.setEnabled(!show);
    }
}