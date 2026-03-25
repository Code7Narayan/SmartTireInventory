// FILE: activities/AddStockActivity.java
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

    private MaterialToolbar toolbar;
    private TextInputLayout tilCompany, tilTireType, tilTireSize, tilQuantity, tilPrice;
    private AutoCompleteTextView spinnerCompany, spinnerTireType;
    private TextInputEditText etTireSize, etQuantity, etPrice;
    private MaterialButton btnSaveStock;
    private LinearProgressIndicator progressBar;

    private ApiService apiService;
    private List<Company> companyList = new ArrayList<>();
    private Company selectedCompany;
    private String selectedTireType;

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
        toolbar = findViewById(R.id.toolbar);
        tilCompany = findViewById(R.id.tilCompany);
        tilTireType = findViewById(R.id.tilTireType);
        tilTireSize = findViewById(R.id.tilTireSize);
        tilQuantity = findViewById(R.id.tilQuantity);
        tilPrice = findViewById(R.id.tilPrice);
        spinnerCompany = findViewById(R.id.spinnerCompany);
        spinnerTireType = findViewById(R.id.spinnerTireType);
        etTireSize = findViewById(R.id.etTireSize);
        etQuantity = findViewById(R.id.etQuantity);
        etPrice = findViewById(R.id.etPrice);
        btnSaveStock = findViewById(R.id.btnSaveStock);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupTireTypeSpinner() {
        String[] tireTypes = getResources().getStringArray(R.array.tire_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tireTypes);
        spinnerTireType.setAdapter(adapter);

        spinnerTireType.setOnItemClickListener((parent, view, position, id) -> {
            selectedTireType = tireTypes[position];
        });
    }

    private void setupListeners() {
        spinnerCompany.setOnItemClickListener((parent, view, position, id) -> {
            selectedCompany = companyList.get(position);
        });

        btnSaveStock.setOnClickListener(v -> saveStock());
    }

    private void loadCompanies() {
        showLoading(true);

        apiService.getCompanies(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                showLoading(false);
                try {
                    boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);

                    if (success) {
                        JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                        companyList.clear();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            Company company = new Company(
                                    obj.getInt("id"),
                                    obj.getString("name")
                            );
                            companyList.add(company);
                        }

                        setupCompanySpinner();
                    }
                } catch (Exception e) {
                    Toast.makeText(AddStockActivity.this,
                            "Error loading companies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(AddStockActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCompanySpinner() {
        ArrayAdapter<Company> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, companyList);
        spinnerCompany.setAdapter(adapter);
    }

    private void saveStock() {
        // Clear errors
        tilCompany.setError(null);
        tilTireType.setError(null);
        tilTireSize.setError(null);
        tilQuantity.setError(null);
        tilPrice.setError(null);

        // Validate inputs
        if (selectedCompany == null) {
            tilCompany.setError("Please select a company");
            return;
        }

        if (selectedTireType == null || selectedTireType.isEmpty()) {
            tilTireType.setError("Please select tire type");
            return;
        }

        String tireSize = etTireSize.getText().toString().trim();
        if (tireSize.isEmpty()) {
            tilTireSize.setError("Tire size is required");
            etTireSize.requestFocus();
            return;
        }

        String quantityStr = etQuantity.getText().toString().trim();
        if (quantityStr.isEmpty()) {
            tilQuantity.setError("Quantity is required");
            etQuantity.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                tilQuantity.setError("Quantity must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            tilQuantity.setError("Invalid quantity");
            return;
        }

        String priceStr = etPrice.getText().toString().trim();
        if (priceStr.isEmpty()) {
            tilPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                tilPrice.setError("Price must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            tilPrice.setError("Invalid price");
            return;
        }

        // Show loading
        showLoading(true);

        // Make API call
        apiService.addProduct(selectedCompany.getId(), selectedTireType, tireSize,
                quantity, price, new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        showLoading(false);
                        try {
                            boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);
                            String message = response.getString(ApiConfig.KEY_MESSAGE);

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

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(AddStockActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearForm() {
        spinnerCompany.setText("", false);
        spinnerTireType.setText("", false);
        etTireSize.setText("");
        etQuantity.setText("");
        etPrice.setText("");
        selectedCompany = null;
        selectedTireType = null;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveStock.setEnabled(!show);
    }
}