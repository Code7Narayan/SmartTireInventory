// FILE: network/ApiService.java
package com.smarttire.inventory.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiService {

    private static final String TAG = "ApiService";
    private static ApiService instance;
    private final RequestQueue queue;

    private ApiService(Context ctx) {
        queue = Volley.newRequestQueue(ctx.getApplicationContext());
    }

    public static synchronized ApiService getInstance(Context ctx) {
        if (instance == null) instance = new ApiService(ctx);
        return instance;
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    @FunctionalInterface
    private interface ParamBuilder { void build(Map<String, String> p); }

    public void login(String username, String password, ApiCallback cb) {
        post(ApiConfig.LOGIN, p -> { 
            p.put("username", username); 
            p.put("password", password); 
        }, cb);
    }

    public void addCompany(String companyName, ApiCallback cb) {
        post(ApiConfig.ADD_COMPANY, p -> {
            p.put("company_name", companyName);
        }, cb);
    }

    public void getCompanies(ApiCallback cb) {
        get(ApiConfig.GET_COMPANIES, cb);
    }

    public void addProduct(int companyId, String tireType, String tireSize, String modelName, int quantity, double price, ApiCallback cb) {
        post(ApiConfig.ADD_PRODUCT, p -> {
            p.put("company_id", String.valueOf(companyId));
            p.put("tire_type", tireType);
            p.put("tire_size", tireSize);
            p.put("model_name", modelName);
            p.put("quantity", String.valueOf(quantity));
            p.put("price", String.valueOf(price));
        }, cb);
    }

    public void getProducts(ApiCallback cb) {
        get(ApiConfig.GET_PRODUCTS, cb);
    }

    public void getProducts(String companyId, ApiCallback cb) {
        String url = ApiConfig.GET_PRODUCTS + "?company_id=" + companyId;
        get(url, cb);
    }

    public void getProductsSorted(String model, int companyId, String size, String sort, ApiCallback cb) {
        String url = ApiConfig.GET_PRODUCTS + "?model=" + model + "&company_id=" + companyId + "&size=" + size + "&sort=" + sort;
        get(url, cb);
    }

    public void getProductDetail(int productId, ApiCallback cb) {
        String url = ApiConfig.GET_PRODUCT_DETAIL + "?product_id=" + productId;
        get(url, cb);
    }

    public void addCustomer(String name, String phone, String address, String gst, ApiCallback cb) {
        post(ApiConfig.ADD_CUSTOMER, p -> {
            p.put("name", name);
            p.put("phone", phone);
            p.put("address", address);
            p.put("gst", gst);
        }, cb);
    }

    public void getCustomers(ApiCallback cb) {
        get(ApiConfig.GET_CUSTOMERS, cb);
    }

    public void getCustomers(String search, int page, ApiCallback cb) {
        String url = ApiConfig.GET_CUSTOMERS + "?search=" + search + "&page=" + page;
        get(url, cb);
    }

    public void getAllCustomersForReport(ApiCallback cb) {
        get(ApiConfig.GET_ALL_CUSTOMERS_REPORT, cb);
    }

    public void getCustomerDetails(int customerId, ApiCallback cb) {
        String url = ApiConfig.GET_CUSTOMER_DETAIL + "?customer_id=" + customerId;
        get(url, cb);
    }

    public void sellProduct(int productId, int customerId, int qty, double paid, String mode, String gst, ApiCallback cb) {
        post(ApiConfig.SELL_PRODUCT, p -> {
            p.put("product_id", String.valueOf(productId));
            p.put("customer_id", String.valueOf(customerId));
            p.put("quantity", String.valueOf(qty));
            p.put("paid_amount", String.valueOf(paid));
            p.put("payment_mode", mode);
            p.put("gst", gst);
        }, cb);
    }

    public void getSalesHistory(ApiCallback cb) {
        get(ApiConfig.GET_SALES_HISTORY, cb);
    }

    public void getSalesHistory(String search, String start, String end, int customerId, ApiCallback cb) {
        String url = ApiConfig.GET_SALES_HISTORY + "?search=" + search + "&start_date=" + start + "&end_date=" + end + "&customer_id=" + customerId;
        get(url, cb);
    }

    public void getSalesHistory(String search, String start, String end, int customerId, String status, int page, ApiCallback cb) {
        String url = ApiConfig.GET_SALES_HISTORY + "?search=" + search + "&start_date=" + start + "&end_date=" + end + "&customer_id=" + customerId + "&status=" + status + "&page=" + page;
        get(url, cb);
    }

    public void addPayment(int saleId, double amount, String mode, String note, ApiCallback cb) {
        post(ApiConfig.ADD_PAYMENT, p -> {
            p.put("sale_id", String.valueOf(saleId));
            p.put("amount", String.valueOf(amount));
            p.put("payment_mode", mode);
            p.put("note", note);
        }, cb);
    }

    public void getDashboard(ApiCallback cb) {
        get(ApiConfig.GET_DASHBOARD, cb);
    }

    public void getDashboardData(ApiCallback cb) {
        get(ApiConfig.GET_DASHBOARD_DATA, cb);
    }

    public void getDashboardAnalytics(ApiCallback cb) {
        get(ApiConfig.GET_DASHBOARD_ANALYTICS, cb);
    }

    public void getLowStock(ApiCallback cb) {
        get(ApiConfig.LOW_STOCK, cb);
    }

    public void getMonthlySales(ApiCallback cb) {
        get(ApiConfig.GET_MONTHLY_SALES, cb);
    }

    public void getMonthlySales(int months, ApiCallback cb) {
        String url = ApiConfig.GET_MONTHLY_SALES + "?months=" + months;
        get(url, cb);
    }

    private void post(String url, ParamBuilder pb, ApiCallback cb) {
        StringRequest req = new StringRequest(Request.Method.POST, url,
                r -> parse(r, cb), e -> parseError(e, cb)) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                pb.build(p);
                return p;
            }
        };
        enqueue(req);
    }

    private void get(String url, ApiCallback cb) {
        enqueue(new StringRequest(Request.Method.GET, url,
                r -> parse(r, cb), e -> parseError(e, cb)));
    }

    private void parse(String raw, ApiCallback cb) {
        Log.d(TAG, "Response: " + raw);
        try { cb.onSuccess(new JSONObject(raw)); }
        catch (JSONException e) { 
            Log.e(TAG, "JSON Parse Error: " + e.getMessage() + " | Raw: " + raw);
            cb.onError("Invalid server response"); 
        }
    }

    private void parseError(com.android.volley.VolleyError e, ApiCallback cb) {
        String message = "Connection error";
        if (e.networkResponse != null) {
            try {
                String body = new String(e.networkResponse.data, "UTF-8");
                Log.e(TAG, "HTTP " + e.networkResponse.statusCode + ": " + body);
                message = "Server error " + e.networkResponse.statusCode;
            } catch (Exception ex) { 
                Log.e(TAG, "Error body parse failed", ex); 
            }
        } else if (e instanceof com.android.volley.NoConnectionError) {
            message = "No internet connection";
        } else if (e instanceof com.android.volley.TimeoutError) {
            message = "Connection timed out";
        } else if (e.getMessage() != null) {
            Log.e(TAG, "Volley Error: " + e.getMessage());
            message = e.getMessage();
        }
        cb.onError(message);
    }

    private void enqueue(StringRequest req) {
        req.setRetryPolicy(new DefaultRetryPolicy(ApiConfig.REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(req);
    }
}
