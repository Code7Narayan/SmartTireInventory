// FILE: app/src/main/java/com/smarttire/inventory/models/DashboardStatsModel.java
package com.smarttire.inventory.models;

import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Immutable model holding all dashboard statistics.
 * Parsed from the /get_dashboard.php API response.
 */
public class DashboardStatsModel {

    private final int    totalProducts;
    private final int    totalStock;
    private final int    lowStockCount;
    private final int    totalCompanies;
    private final int    todaySalesCount;
    private final double todayRevenue;
    private final double totalRevenue;

    private DashboardStatsModel(Builder b) {
        this.totalProducts   = b.totalProducts;
        this.totalStock      = b.totalStock;
        this.lowStockCount   = b.lowStockCount;
        this.totalCompanies  = b.totalCompanies;
        this.todaySalesCount = b.todaySalesCount;
        this.todayRevenue    = b.todayRevenue;
        this.totalRevenue    = b.totalRevenue;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int    getTotalProducts()   { return totalProducts; }
    public int    getTotalStock()      { return totalStock; }
    public int    getLowStockCount()   { return lowStockCount; }
    public int    getTotalCompanies()  { return totalCompanies; }
    public int    getTodaySalesCount() { return todaySalesCount; }
    public double getTodayRevenue()    { return todayRevenue; }
    public double getTotalRevenue()    { return totalRevenue; }

    public boolean hasLowStock() { return lowStockCount > 0; }

    public String getFormattedTodayRevenue() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"))
                .format(todayRevenue);
    }

    public String getFormattedTotalRevenue() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"))
                .format(totalRevenue);
    }

    // ── Factory from JSON ────────────────────────────────────────────────────

    public static DashboardStatsModel fromJSON(JSONObject data) throws Exception {
        double todayRev = 0.0;
        if (data.has("today_revenue"))       todayRev = data.getDouble("today_revenue");
        else if (data.has("today_sales_amount")) todayRev = data.getDouble("today_sales_amount");

        return new Builder()
                .totalProducts(data.optInt("total_products", 0))
                .totalStock(data.optInt("total_stock", 0))
                .lowStockCount(data.optInt("low_stock_count", 0))
                .totalCompanies(data.optInt("total_companies", 0))
                .todaySalesCount(data.optInt("today_sales_count", 0))
                .todayRevenue(todayRev)
                .totalRevenue(data.optDouble("total_revenue", 0.0))
                .build();
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static class Builder {
        private int    totalProducts   = 0;
        private int    totalStock      = 0;
        private int    lowStockCount   = 0;
        private int    totalCompanies  = 0;
        private int    todaySalesCount = 0;
        private double todayRevenue    = 0.0;
        private double totalRevenue    = 0.0;

        public Builder totalProducts(int v)   { totalProducts = v;   return this; }
        public Builder totalStock(int v)      { totalStock = v;      return this; }
        public Builder lowStockCount(int v)   { lowStockCount = v;   return this; }
        public Builder totalCompanies(int v)  { totalCompanies = v;  return this; }
        public Builder todaySalesCount(int v) { todaySalesCount = v; return this; }
        public Builder todayRevenue(double v) { todayRevenue = v;    return this; }
        public Builder totalRevenue(double v) { totalRevenue = v;    return this; }

        public DashboardStatsModel build() { return new DashboardStatsModel(this); }
    }
}
