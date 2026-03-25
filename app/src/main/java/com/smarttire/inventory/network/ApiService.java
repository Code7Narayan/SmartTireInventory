// FILE: network/ApiService.java
package com.smarttire.inventory.network;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ApiService {

    private static ApiService instance;
    private RequestQueue requestQueue;
    private Context context;

    private ApiService(Context context) {
        this.context = context.getApplicationContext();
        requestQueue = Volley.newRequestQueue(this.context);
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context);
        }
        return instance;
    }

    // Interface for API callbacks
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    // Login API
    public void login(String username, String password, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.LOGIN,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        addToRequestQueue(request);
    }

    // Add Company API
    public void addCompany(String companyName, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.ADD_COMPANY,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("company_name", companyName);
                return params;
            }
        };

        addToRequestQueue(request);
    }

    // Get Companies API
    public void getCompanies(final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET, ApiConfig.GET_COMPANIES,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback));

        addToRequestQueue(request);
    }

    // Add Product API
    public void addProduct(int companyId, String tireType, String tireSize,
                           int quantity, double price, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.ADD_PRODUCT,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("company_id", String.valueOf(companyId));
                params.put("tire_type", tireType);
                params.put("tire_size", tireSize);
                params.put("quantity", String.valueOf(quantity));
                params.put("price", String.valueOf(price));
                return params;
            }
        };

        addToRequestQueue(request);
    }

    // Get Products API
    public void getProducts(final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET, ApiConfig.GET_PRODUCTS,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback));

        addToRequestQueue(request);
    }

    // Sell Product API - UPDATED to include more fields required by backend
    public void sellProduct(int productId, String companyName, String tireType, String tireSize,
                           int quantity, double unitPrice, double totalPrice, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.SELL_PRODUCT,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("product_id", String.valueOf(productId));
                params.put("company_name", companyName);
                params.put("tire_type", tireType);
                params.put("tire_size", tireSize);
                params.put("quantity", String.valueOf(quantity));
                params.put("unit_price", String.valueOf(unitPrice));
                params.put("total_price", String.valueOf(totalPrice));
                return params;
            }
        };

        addToRequestQueue(request);
    }

    // Get Dashboard API
    public void getDashboard(final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET, ApiConfig.GET_DASHBOARD,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback));

        addToRequestQueue(request);
    }

    // Get Low Stock API
    public void getLowStock(final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET, ApiConfig.LOW_STOCK,
                response -> handleResponse(response, callback),
                error -> handleError(error, callback));

        addToRequestQueue(request);
    }

    // Handle successful response
    private void handleResponse(String response, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            callback.onSuccess(jsonObject);
        } catch (JSONException e) {
            callback.onError("Invalid response format");
        }
    }

    // Handle error response
    private void handleError(VolleyError error, ApiCallback callback) {
        String errorMessage;
        if (error.networkResponse != null) {
            errorMessage = "Server error: " + error.networkResponse.statusCode;
        } else if (error.getMessage() != null) {
            errorMessage = error.getMessage();
        } else {
            errorMessage = "Connection error. Please check your internet connection.";
        }
        callback.onError(errorMessage);
    }

    // Add request to queue with retry policy
    private void addToRequestQueue(StringRequest request) {
        request.setRetryPolicy(new DefaultRetryPolicy(
                ApiConfig.REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    // Cancel all pending requests
    public void cancelAllRequests() {
        requestQueue.cancelAll(request -> true);
    }
}
