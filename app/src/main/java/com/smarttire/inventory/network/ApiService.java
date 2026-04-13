// FILE: network/ApiService.java
package com.smarttire.inventory.network;

import android.content.Context;
import android.util.Log;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
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
        post(ApiConfig.LOGIN, p -> { p.put("username", username); p.put("password", password); }, cb);
    }

    // ── Companies ─────────────────────────────────────────────────────────────
    public void addCompany(String name, ApiCallback cb) {
        post(ApiConfig.ADD_COMPANY, p -> p.put("company_name", name), cb);
    }
    public void getCompanies(ApiCallback cb) { get(ApiConfig.GET_COMPANIES, cb); }

    // ── Products ──────────────────────────────────────────────────────────────
    public void addProduct(int companyId, String tireType, String tireSize,
                           String modelName, int quantity, double price, double costPrice, ApiCallback cb) {
        post(ApiConfig.ADD_PRODUCT, p -> {
            p.put("company_id", String.valueOf(companyId));
            p.put("tire_type",  tireType);
            p.put("tire_size",  tireSize);
            p.put("model_name", modelName);
            p.put("quantity",   String.valueOf(quantity));
            p.put("price",      String.valueOf(price));
            p.put("cost_price", String.valueOf(costPrice));
        }, cb);
    }

    // GET — no body params needed; returns all products
    public void getProducts(ApiCallback cb) { get(ApiConfig.GET_PRODUCTS, cb); }

    public void getProductsSorted(String m, int cid, String s, String sort, ApiCallback cb) {
        post(ApiConfig.GET_PRODUCTS, p -> {
            if (m != null && !m.isEmpty())    p.put("model_name", m);
            if (cid > 0)                       p.put("company_id", String.valueOf(cid));
            if (s != null && !s.isEmpty())    p.put("size", s);
            if (sort != null && !sort.isEmpty()) p.put("sort", sort);
        }, cb);
    }

    public void getProductDetail(int productId, ApiCallback cb) {
        post(ApiConfig.GET_PRODUCTS, p -> p.put("detail_id", String.valueOf(productId)), cb);
    }

    public void updateProduct(int id, String action, double price, int addQty, ApiCallback cb) {
        post(ApiConfig.UPDATE_PRODUCT, p -> {
            p.put("product_id", String.valueOf(id));
            
            String serverAction = action;
            if ("price".equals(action) || "cost_price".equals(action)) {
                serverAction = "update";
            }
            p.put("action", serverAction);

            if (price != -1) {
                String key = "cost_price".equals(action) ? "cost_price" : "price";
                p.put(key, String.valueOf(price));
            }
            if (addQty != 0) p.put("add_qty", String.valueOf(addQty));
        }, cb);
    }

    public void updateProductFull(int id, int cid, String type, String size, String model,
                                  double price, double cost, ApiCallback cb) {
        post(ApiConfig.UPDATE_PRODUCT, p -> {
            p.put("product_id",  String.valueOf(id));
            p.put("action",      "update");
            p.put("company_id",  String.valueOf(cid));
            p.put("tire_type",   type);
            p.put("tire_size",   size);
            p.put("model_name",  model);
            p.put("price",       String.valueOf(price));
            p.put("cost_price",  String.valueOf(cost));
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

    public void updateCustomer(int id, String name, String phone, String address,
                               String gst, ApiCallback cb) {
        post(ApiConfig.UPDATE_CUSTOMER, p -> {
            p.put("customer_id", String.valueOf(id));
            p.put("name",        name);
            p.put("phone",       phone);
            p.put("address",     address);
            p.put("gst_number",  gst);
        }, cb);
    }

    public void deleteCustomer(int id, ApiCallback cb) {
        post(ApiConfig.DELETE_CUSTOMER, p -> p.put("customer_id", String.valueOf(id)), cb);
    }

    public void getCustomers(String search, int page, ApiCallback cb) {
        String url = ApiConfig.GET_CUSTOMERS
                + "?search=" + (search != null ? search : "")
                + "&page=" + page;
        get(url, cb);
    }

    public void getAllCustomersForReport(ApiCallback cb) {
        get(ApiConfig.GET_CUSTOMERS + "?report=1", cb);
    }

    public void getCustomerDetails(int customerId, ApiCallback cb) {
        get(ApiConfig.GET_CUSTOMER_DETAIL + "?customer_id=" + customerId, cb);
    }

    // ── Sales ─────────────────────────────────────────────────────────────────
    public void sellProduct(int pid, int cid, int q, double price, double paid, String mode,
                            String gst, ApiCallback cb) {
        post(ApiConfig.SELL_PRODUCT, p -> {
            p.put("product_id",   String.valueOf(pid));
            p.put("customer_id",  String.valueOf(cid));
            p.put("quantity",     String.valueOf(q));
            p.put("price",        String.valueOf(price));
            p.put("paid_amount",  String.valueOf(paid));
            p.put("payment_mode", mode);
            p.put("gst_number",   gst);
        }, cb);
    }

    public void getSalesHistory(String search, String startDate, String endDate,
                                int customerId, String status, int page, ApiCallback cb) {
        post(ApiConfig.GET_SALES_HISTORY, p -> {
            p.put("page",        String.valueOf(page));
            p.put("search",      search != null ? search : "");
            p.put("start_date",  startDate != null ? startDate : "");
            p.put("end_date",    endDate != null ? endDate : "");
            p.put("customer_id", String.valueOf(customerId));
            p.put("status",      status != null ? status : "");
        }, cb);
    }

    // ── Payments ──────────────────────────────────────────────────────────────
    public void addPayment(int saleId, double amount, String mode, String note, ApiCallback cb) {
        post(ApiConfig.ADD_PAYMENT, p -> {
            p.put("sale_id",      String.valueOf(saleId));
            p.put("amount_paid",  String.valueOf(amount));
            p.put("payment_mode", mode);
            p.put("note",         note != null ? note : "");
        }, cb);
    }

    public void addCustomerPayment(int customerId, double amount, String mode, String note, ApiCallback cb) {
        post(ApiConfig.ADD_PAYMENT, p -> {
            p.put("customer_id",  String.valueOf(customerId));
            p.put("amount_paid",  String.valueOf(amount));
            p.put("payment_mode", mode);
            p.put("note",         note != null ? note : "");
            p.put("sale_id",      "0");
        }, cb);
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────
    public void getDashboard(ApiCallback cb)          { get(ApiConfig.GET_DASHBOARD, cb); }
    public void getLowStock(ApiCallback cb)           { get(ApiConfig.LOW_STOCK, cb); }
    public void getDashboardAnalytics(ApiCallback cb) { get(ApiConfig.GET_DASHBOARD_DATA, cb); }

    public void getMonthlySales(int months, ApiCallback cb) {
        get(ApiConfig.GET_MONTHLY_SALES + "?months=" + months, cb);
    }

    public void getDailyAnalytics(String date, ApiCallback cb) {
        String safeDate = (date != null && !date.isEmpty()) ? date : "";
        get(ApiConfig.GET_DAILY_ANALYTICS + "?date=" + safeDate, cb);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private void post(String url, ParamBuilder pb, ApiCallback cb) {
        Map<String, String> params = new HashMap<>();
        pb.build(params);
        Log.d(TAG, "POST " + url + " | params=" + params.toString());

        StringRequest req = new StringRequest(Request.Method.POST, url,
                r -> parse(r, url, cb),
                e -> parseError(e, cb)) {
            @Override
            protected Map<String, String> getParams() { return params; }
        };
        req.setTag(TAG);
        enqueue(req);
    }

    private void get(String url, ApiCallback cb) {
        Log.d(TAG, "GET " + url);
        StringRequest req = new StringRequest(Request.Method.GET, url,
                r -> parse(r, url, cb),
                e -> parseError(e, cb));
        req.setTag(TAG);
        enqueue(req);
    }

    private void parse(String raw, String url, ApiCallback cb) {
        Log.d("API_RESPONSE", url + " → " + raw);
        try { cb.onSuccess(new JSONObject(raw)); }
        catch (JSONException e) {
            Log.e(TAG, "JSON parse error for " + url + ": " + raw);
            cb.onError("Invalid server response");
        }
    }

    private void parseError(VolleyError e, ApiCallback cb) {
        String msg = "Network error";
        if (e.networkResponse != null) {
            int status = e.networkResponse.statusCode;
            msg = "Server error: " + status;
            try {
                String body = new String(e.networkResponse.data, StandardCharsets.UTF_8);
                Log.e(TAG, "Error body: " + body);
                if (body.contains("message")) {
                    JSONObject obj = new JSONObject(body);
                    msg = obj.optString("message", msg);
                }
            } catch (Exception ignored) {}
        } else if (e instanceof com.android.volley.TimeoutError) {
            msg = "Connection timeout";
        } else if (e instanceof com.android.volley.NoConnectionError) {
            msg = "No internet connection";
        }
        Log.e(TAG, "Volley error: " + msg, e);
        cb.onError(msg);
    }

    private void enqueue(StringRequest req) {
        req.setRetryPolicy(new DefaultRetryPolicy(ApiConfig.REQUEST_TIMEOUT, 1, 1f));
        queue.add(req);
    }
}