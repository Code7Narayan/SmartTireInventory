// FILE: activities/LoginActivity.java
package com.smarttire.inventory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smarttire.inventory.R;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.KeyboardUtils;
import com.smarttire.inventory.utils.SharedPrefManager;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progressBar;

    private SharedPrefManager prefManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize managers
        prefManager = SharedPrefManager.getInstance(this);
        apiService = ApiService.getInstance(this);

        // Check if already logged in
        if (prefManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        // Clear previous errors
        tilUsername.setError(null);
        tilPassword.setError(null);

        // Get input values
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (username.isEmpty()) {
            tilUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Hide keyboard on attempt
        KeyboardUtils.hideKeyboard(this);

        // Show loading
        showLoading(true);

        // Make API call
        apiService.login(username, password, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                showLoading(false);
                try {
                    boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);
                    String message = response.getString(ApiConfig.KEY_MESSAGE);

                    if (success) {
                        JSONObject data = response.getJSONObject(ApiConfig.KEY_DATA);
                        int userId = data.getInt("user_id");
                        String uname = data.getString("username");
                        String fullName = data.getString("full_name");

                        // Save session
                        prefManager.saveUserSession(userId, uname, fullName);

                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}