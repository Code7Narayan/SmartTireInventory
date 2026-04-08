// FILE: network/ApiService.java  (UPDATED — updateProduct + getDailyAnalytics added)
package com.smarttire.inventory.network;

import android.content.Context;
import android.net.Uri;
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

    // ── Auth ──────────────────────────────────────────────────────────────────
    public void login(String username, String password, ApiCallback cb) {
        post(ApiConfig.LOGIN, p -> { p.put("username",username); p.put("password",password); }, cb);
    }

    // ── Companies ─────────────────────────────────────────────────────────────
    public void addCompany(String name, ApiCallback cb) {
        post(ApiConfig.ADD_COMPANY, p -> p.put("company_name", name), cb);
    }
    public void getCompanies(ApiCallback cb) { get(ApiConfig.GET_COMPANIES, cb); }

    // ── Products ──────────────────────────────────────────────────────────────
    public void addProduct(int companyId, String tireType, String tireSize,
                           String modelName, int quantity, double price, ApiCallback cb) {
        post(ApiConfig.ADD_PRODUCT, p -> {
            p.put("company_id", String.valueOf(companyId));
            p.put("tire_type",  tireType);
            p.put("tire_size",  tireSize);
            p.put("model_name", modelName);
            p.put("quantity",   String.valueOf(quantity));
            p.put("price",      String.valueOf(price));
        }, cb);
    }

    public void getProducts(ApiCallback cb) { get(ApiConfig.GET_PRODUCTS, cb); }

    public void getProducts(String modelName, int companyId, String size, ApiCallback cb) {
        get(ApiConfig.GET_PRODUCTS
                + "?model_name=" + Uri.encode(modelName)
                + "&company_id=" + companyId
                + "&size="       + Uri.encode(size), cb);
    }

    public void getProductsSorted(String modelName, int companyId,
                                  String size, String sort, ApiCallback cb) {
        get(ApiConfig.GET_PRODUCTS
                + "?model_name=" + Uri.encode(modelName)
                + "&company_id=" + companyId
                + "&size="       + Uri.encode(size)
                + "&sort="       + Uri.encode(sort), cb);
    }

    public void getProductDetail(int productId, ApiCallback cb) {
        get(ApiConfig.GET_PRODUCTS + "?detail_id=" + productId, cb);
    }

    /**
     * NEW: Update product — action can be 'price', 'quantity_add', 'update', 'delete'
     * Pass -1 for price/addQty when not applicable.
     */
    public void updateProduct(int productId, String action, double price, int addQty, ApiCallback cb) {
        post(ApiConfig.UPDATE_PRODUCT, p -> {
            p.put("product_id", String.valueOf(productId));
            p.put("action",     action);
            if (price > 0)   p.put("price",   String.valueOf(price));
            if (addQty > 0)  p.put("add_qty", String.valueOf(addQty));
        }, cb);
    }

    // ── Customers ─────────────────────────────────────────────────────────────
    public void addCustomer(String name, String phone, String address,
                            String gst, ApiCallback cb) {
        post(ApiConfig.ADD_CUSTOMER, p -> {
            p.put("name",       name);
            p.put("phone",      phone);
            p.put("address",    address);
            p.put("gst_number", gst);
        }, cb);
    }

    public void getCustomers(String search, int page, ApiCallback cb) {
        get(ApiConfig.GET_CUSTOMERS
                + "?search=" + Uri.encode(search)
                + "&page="   + page, cb);
    }

    public void getAllCustomersForReport(ApiCallback cb) {
        get(ApiConfig.GET_CUSTOMERS + "?report=1", cb);
    }

    public void getCustomerDetails(int customerId, ApiCallback cb) {
        get(ApiConfig.GET_CUSTOMER_DETAIL + "?customer_id=" + customerId, cb);
    }

    // ── Sales ─────────────────────────────────────────────────────────────────
    public void sellProduct(int productId, int customerId, int quantity,
                            double paidAmount, String paymentMode,
                            String gstNumber, ApiCallback cb) {
        post(ApiConfig.SELL_PRODUCT, p -> {
            p.put("product_id",   String.valueOf(productId));
            p.put("customer_id",  String.valueOf(customerId));
            p.put("quantity",     String.valueOf(quantity));
            p.put("paid_amount",  String.valueOf(paidAmount));
            p.put("payment_mode", paymentMode);
            p.put("gst_number",   gstNumber);
        }, cb);
    }

    public void getSalesHistory(String search, String startDate, String endDate,
                                int page, ApiCallback cb) {
        get(ApiConfig.GET_SALES_HISTORY
                + "?search="     + Uri.encode(search)
                + "&start_date=" + Uri.encode(startDate)
                + "&end_date="   + Uri.encode(endDate)
                + "&page="       + page, cb);
    }

    public void getSalesHistory(String search, String startDate, String endDate,
                                int customerId, String status, int page, ApiCallback cb) {
        get(ApiConfig.GET_SALES_HISTORY
                + "?search="      + Uri.encode(search)
                + "&start_date="  + Uri.encode(startDate)
                + "&end_date="    + Uri.encode(endDate)
                + "&customer_id=" + customerId
                + "&status="      + Uri.encode(status)
                + "&page="        + page, cb);
    }

    // ── Payments ──────────────────────────────────────────────────────────────
    public void addPayment(int saleId, double amount, String mode, String note, ApiCallback cb) {
        post(ApiConfig.ADD_PAYMENT, p -> {
            p.put("sale_id",      String.valueOf(saleId));
            p.put("amount_paid",  String.valueOf(amount));
            p.put("payment_mode", mode);
            p.put("note",         note);
        }, cb);
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────
    public void getDashboard(ApiCallback cb)          { get(ApiConfig.GET_DASHBOARD, cb); }
    public void getLowStock(ApiCallback cb)           { get(ApiConfig.LOW_STOCK, cb); }
    public void getDashboardAnalytics(ApiCallback cb) { get(ApiConfig.GET_DASHBOARD_DATA, cb); }

    public void getMonthlySales(int months, ApiCallback cb) {
        get(ApiConfig.GET_MONTHLY_SALES + "?months=" + months, cb);
    }

    /**
     * NEW: Daily analytics for a specific date (yyyy-MM-dd).
     * Returns total added/sold/revenue for that date with transaction details.
     */
    public void getDailyAnalytics(String date, ApiCallback cb) {
        get(ApiConfig.GET_DAILY_ANALYTICS + "?date=" + Uri.encode(date), cb);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────
    public void cancelAll() { queue.cancelAll(r -> true); }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

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
        try { cb.onSuccess(new JSONObject(raw)); }
        catch (JSONException e) { cb.onError("Invalid server response"); }
    }

    private void parseError(com.android.volley.VolleyError e, ApiCallback cb) {
        if (e.networkResponse != null) {
            try {
                String body = new String(e.networkResponse.data, "UTF-8");
                Log.e(TAG, "HTTP " + e.networkResponse.statusCode + ": " + body);
            } catch (Exception ex) { Log.e(TAG, "Error body parse failed", ex); }
            cb.onError("Server error " + e.networkResponse.statusCode);
        } else if (e.getMessage() != null) {
            Log.e(TAG, "Volley: " + e.getMessage());
            cb.onError(e.getMessage());
        } else {
            cb.onError("Connection error. Check internet.");
        }
    }

    private void enqueue(StringRequest req) {
        req.setRetryPolicy(new DefaultRetryPolicy(ApiConfig.REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(req);
    }
}