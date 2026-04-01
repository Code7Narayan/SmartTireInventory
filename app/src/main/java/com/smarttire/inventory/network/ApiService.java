// FILE: app/src/main/java/com/smarttire/inventory/network/ApiService.java  (REPLACE)
package com.smarttire.inventory.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton API service backed by Volley.
 * All callbacks are delivered on the main thread.
 */
public class ApiService {

    private static ApiService instance;
    private final RequestQueue requestQueue;

    private ApiService(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) instance = new ApiService(context);
        return instance;
    }

    // ── Callback interface ───────────────────────────────────────────────────

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    public void login(String username, String password, ApiCallback cb) {
        postRequest(ApiConfig.LOGIN, params -> {
            params.put("username", username);
            params.put("password", password);
        }, cb);
    }

    // ── Companies ────────────────────────────────────────────────────────────

    public void addCompany(String companyName, ApiCallback cb) {
        postRequest(ApiConfig.ADD_COMPANY, params -> {
            params.put("company_name", companyName);
        }, cb);
    }

    public void getCompanies(ApiCallback cb) {
        getRequest(ApiConfig.GET_COMPANIES, cb);
    }

    // ── Products ─────────────────────────────────────────────────────────────

    public void addProduct(int companyId, String tireType, String tireSize,
                           int quantity, double price, ApiCallback cb) {
        postRequest(ApiConfig.ADD_PRODUCT, params -> {
            params.put("company_id", String.valueOf(companyId));
            params.put("tire_type", tireType);
            params.put("tire_size", tireSize);
            params.put("quantity", String.valueOf(quantity));
            params.put("price", String.valueOf(price));
        }, cb);
    }

    public void getProducts(ApiCallback cb) {
        getRequest(ApiConfig.GET_PRODUCTS, cb);
    }

    // ── Sales ────────────────────────────────────────────────────────────────

    public void sellProduct(int productId, String companyName, String tireType,
                            String tireSize, int quantity, double unitPrice,
                            double totalPrice, ApiCallback cb) {
        postRequest(ApiConfig.SELL_PRODUCT, params -> {
            params.put("product_id", String.valueOf(productId));
            params.put("company_name", companyName);
            params.put("tire_type", tireType);
            params.put("tire_size", tireSize);
            params.put("quantity", String.valueOf(quantity));
            params.put("unit_price", String.valueOf(unitPrice));
            params.put("total_price", String.valueOf(totalPrice));
        }, cb);
    }

    // ── NEW: Sales History ────────────────────────────────────────────────────

    /**
     * Fetch paginated sales history with optional filters.
     *
     * @param search      search term (company / tire type / size), empty = all
     * @param startDate   "yyyy-MM-dd" or empty for no lower bound
     * @param endDate     "yyyy-MM-dd" or empty for no upper bound
     * @param page        1-based page number
     */
    public void getSalesHistory(String search, String startDate,
                                String endDate, int page, ApiCallback cb) {

        // Build query string
        String url = ApiConfig.GET_SALES_HISTORY
                + "?page=" + page
                + "&search=" + android.net.Uri.encode(search)
                + "&start_date=" + android.net.Uri.encode(startDate)
                + "&end_date=" + android.net.Uri.encode(endDate);

        getRequest(url, cb);
    }

    // ── NEW: Monthly Sales Chart Data ─────────────────────────────────────────

    /**
     * Returns the last {@code months} months of aggregated sales data
     * for the dashboard bar chart.
     */
    public void getMonthlySales(int months, ApiCallback cb) {
        String url = ApiConfig.GET_MONTHLY_SALES + "?months=" + months;
        getRequest(url, cb);
    }

    // ── Dashboard / Low Stock ─────────────────────────────────────────────────

    public void getDashboard(ApiCallback cb) {
        getRequest(ApiConfig.GET_DASHBOARD, cb);
    }

    public void getLowStock(ApiCallback cb) {
        getRequest(ApiConfig.LOW_STOCK, cb);
    }

    // ── Cancel ───────────────────────────────────────────────────────────────

    public void cancelAllRequests() {
        requestQueue.cancelAll(r -> true);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ParamBuilder {
        void build(Map<String, String> params);
    }

    private void postRequest(String url, ParamBuilder paramBuilder, ApiCallback cb) {
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> parseResponse(response, cb),
                error -> parseError(error, cb)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                paramBuilder.build(p);
                return p;
            }
        };
        enqueue(req);
    }

    private void getRequest(String url, ApiCallback cb) {
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> parseResponse(response, cb),
                error -> parseError(error, cb));
        enqueue(req);
    }

    private void parseResponse(String raw, ApiCallback cb) {
        try {
            cb.onSuccess(new JSONObject(raw));
        } catch (JSONException e) {
            cb.onError("Invalid response from server");
        }
    }

    private void parseError(com.android.volley.VolleyError error, ApiCallback cb) {
        String msg;
        if (error.networkResponse != null) {
            msg = "Server error " + error.networkResponse.statusCode;
        } else if (error.getMessage() != null) {
            msg = error.getMessage();
        } else {
            msg = "Connection error. Check your internet connection.";
        }
        cb.onError(msg);
    }

    private void enqueue(StringRequest req) {
        req.setRetryPolicy(new DefaultRetryPolicy(
                ApiConfig.REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(req);
    }
}
