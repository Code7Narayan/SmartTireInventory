// FILE: activities/AddCustomerActivity.java
package com.smarttire.inventory.activities;

import android.app.Activity;
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
import com.smarttire.inventory.utils.KeyboardUtils;

import org.json.JSONObject;

public class AddCustomerActivity extends AppCompatActivity {

    private TextInputLayout     tilName, tilPhone, tilAddress;
    private TextInputEditText   etName, etPhone, etAddress, etGst;
    private MaterialButton      btnSave;
    private LinearProgressIndicator progressBar;
    private ApiService          api;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);
        api = ApiService.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tilName      = findViewById(R.id.tilName);
        tilPhone     = findViewById(R.id.tilPhone);
        tilAddress   = findViewById(R.id.tilAddress);
        etName       = findViewById(R.id.etName);
        etPhone      = findViewById(R.id.etPhone);
        etAddress    = findViewById(R.id.etAddress);
        etGst        = findViewById(R.id.etGst);
        btnSave      = findViewById(R.id.btnSaveCustomer);
        progressBar  = findViewById(R.id.progressBar);

        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        tilName.setError(null); tilPhone.setError(null);

        String name    = etName.getText() != null  ? etName.getText().toString().trim()  : "";
        String phone   = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String gst     = etGst.getText() != null   ? etGst.getText().toString().trim()  : "";

        if (name.length() < 2) { tilName.setError("Enter a valid name"); return; }
        if (!phone.matches("[6-9]\\d{9}")) { tilPhone.setError("Enter a valid 10-digit phone"); return; }

        KeyboardUtils.hideKeyboard(this);
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        api.addCustomer(name, phone, address, gst, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                try {
                    boolean ok = response.getBoolean(ApiConfig.KEY_SUCCESS);
                    String msg = response.getString(ApiConfig.KEY_MESSAGE);
                    Toast.makeText(AddCustomerActivity.this, msg, Toast.LENGTH_SHORT).show();
                    if (ok) { setResult(Activity.RESULT_OK); finish(); }
                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddCustomerActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}