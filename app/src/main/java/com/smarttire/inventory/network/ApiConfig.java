package com.smarttire.inventory.network;

public class ApiConfig {

    // Using HTTP to avoid potential SSL/Redirection 500 errors on shared hosting
    public static final String BASE_URL = "https://inventory.ecssofttech.com/inventory/api/";

    // ── Endpoints ───────────────────────────────────────────────────────────
    public static final String LOGIN              = BASE_URL + "login.php";
    public static final String ADD_COMPANY        = BASE_URL + "add_company.php";
    public static final String GET_COMPANIES      = BASE_URL + "get_companies.php";
    public static final String ADD_PRODUCT        = BASE_URL + "add_product.php";
    public static final String GET_PRODUCTS       = BASE_URL + "get_products.php";
    public static final String ADD_CUSTOMER       = BASE_URL + "add_customer.php";
    public static final String GET_CUSTOMERS      = BASE_URL + "get_customers.php";
    public static final String GET_CUSTOMER_DETAIL = BASE_URL + "get_customer_details.php";
    public static final String SELL_PRODUCT       = BASE_URL + "sell_product.php";
    public static final String GET_SALES_HISTORY   = BASE_URL + "get_sales_history.php";
    public static final String ADD_PAYMENT        = BASE_URL + "add_payment.php";
    public static final String GET_DASHBOARD      = BASE_URL + "get_dashboard.php";
    public static final String LOW_STOCK          = BASE_URL + "low_stock.php";
    public static final String GET_MONTHLY_SALES  = BASE_URL + "get_monthly_sales.php";

    // ── Config ───────────────────────────────────────────────────────────────
    public static final int REQUEST_TIMEOUT = 30000;
    public static final int WALK_IN_CUSTOMER_ID = 0;

    // ── Response keys ────────────────────────────────────────────────────────
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_DATA    = "data";

    // Low-stock threshold (must match backend)
    public static final int LOW_STOCK_THRESHOLD = 5;
}
