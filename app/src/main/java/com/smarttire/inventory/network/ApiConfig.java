// FILE: network/ApiConfig.java
package com.smarttire.inventory.network;

public class ApiConfig {

    // Base URL - Change this to your server IP
    // For Emulator use: 10.0.2.2
    // For Real Device use: Your computer's local IP (e.g., 192.168.1.100)
    private static final String BASE_URL = "https://inventory.ecssofttech.com/inventory/api/";

    // API EndpointsString url = "https://inventory.ecssofttech.com/inventory/api/";
    public static final String LOGIN = BASE_URL + "login.php";
    public static final String ADD_COMPANY = BASE_URL + "add_company.php";
    public static final String GET_COMPANIES = BASE_URL + "get_companies.php";
    public static final String ADD_PRODUCT = BASE_URL + "add_product.php";
    public static final String GET_PRODUCTS = BASE_URL + "get_products.php";
    public static final String SELL_PRODUCT = BASE_URL + "sell_product.php";
    public static final String GET_DASHBOARD = BASE_URL + "get_dashboard.php";
    public static final String LOW_STOCK = BASE_URL + "low_stock.php";

    // Request timeout in milliseconds
    public static final int REQUEST_TIMEOUT = 30000;

    // Response keys
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_DATA = "data";
}