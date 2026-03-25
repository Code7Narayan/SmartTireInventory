// FILE: activities/AddCompanyActivity.java
package com.smarttire.inventory.activities;

import android.os.Bundle;
import android.view.View;
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
import org.json.JSONObject;

public class AddCompanyActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilCompanyName;
    private TextInputEditText etCompanyName;
    private MaterialButton btnSaveCompany;
    private LinearProgressIndicator progressBar;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_company);

        apiService = ApiService.getInstance(this);

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tilCompanyName = findViewById(R.id.tilCompanyName);
        etCompanyName = findViewById(R.id.etCompanyName);
        btnSaveCompany = findViewById(R.id.btnSaveCompany);
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

    private void setupListeners() {
        btnSaveCompany.setOnClickListener(v -> saveCompany());
    }

    private void saveCompany() {
        // Clear previous error
        tilCompanyName.setError(null);

        // Get input
        String companyName = etCompanyName.getText().toString().trim();

        // Validate
        if (companyName.isEmpty()) {
            tilCompanyName.setError("Company name is required");
            etCompanyName.requestFocus();
            return;
        }

        if (companyName.length() < 2) {
            tilCompanyName.setError("Company name must be at least 2 characters");
            etCompanyName.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Make API call
        apiService.addCompany(companyName, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                showLoading(false);
                try {
                    boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);
                    String message = response.getString(ApiConfig.KEY_MESSAGE);

                    if (success) {
                        Toast.makeText(AddCompanyActivity.this,
                                "Company added successfully!", Toast.LENGTH_SHORT).show();
                        etCompanyName.setText("");

                        // Optionally go back
                        // finish();
                    } else {
                        Toast.makeText(AddCompanyActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(AddCompanyActivity.this,
                            "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(AddCompanyActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveCompany.setEnabled(!show);
        etCompanyName.setEnabled(!show);
    }
}