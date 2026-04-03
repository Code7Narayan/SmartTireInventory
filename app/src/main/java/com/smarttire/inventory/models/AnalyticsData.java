// FILE: models/AnalyticsData.java
package com.smarttire.inventory.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parsed result from get_dashboard_data.php */
public class AnalyticsData {

    // KPIs
    public double totalRevenue;
    public double totalCollected;
    public double totalOutstanding;
    public double todayRevenue;
    public double monthRevenue;
    public int    totalSalesCount;
    public int    totalStockAdded;
    public int    totalStockSold;

    // Lists
    public final List<StockRevenueItem> stockWiseRevenue  = new ArrayList<>();
    public final List<StockRevenueItem> bestSelling       = new ArrayList<>();
    public final List<StockSummaryItem> stockAddedSummary = new ArrayList<>();
    public final List<CompanyRevItem>   companyRevenue    = new ArrayList<>();

    // ── Inner models ──────────────────────────────────────────────────────────

    public static class StockRevenueItem {
        public int    productId;
        public String companyName;
        public String modelName;
        public String tireType;
        public String tireSize;
        public double unitPrice;
        public int    currentStock;
        public int    qtySold;
        public double revenue;
        public double collected;
        public double outstanding;
        public int    saleCount;

        public String displayName() {
            String m = (modelName != null && !modelName.isEmpty()) ? modelName + " " : "";
            return companyName + " " + m + tireType + " (" + tireSize + ")";
        }

        public String formattedRevenue() {
            return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(revenue);
        }
    }

    public static class StockSummaryItem {
        public int    productId;
        public String companyName;
        public String modelName;
        public String tireType;
        public String tireSize;
        public int    currentStock;
        public int    totalAdded;
        public int    totalSold;

        public String displayName() {
            String m = (modelName != null && !modelName.isEmpty()) ? modelName + " " : "";
            return companyName + " " + m + tireType + " (" + tireSize + ")";
        }
    }

    public static class CompanyRevItem {
        public String companyName;
        public double revenue;
        public int    qtySold;
        public int    saleCount;

        public String formattedRevenue() {
            return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(revenue);
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AnalyticsData fromJSON(JSONObject d) throws Exception {
        AnalyticsData a = new AnalyticsData();

        a.totalRevenue    = d.optDouble("total_revenue",    0);
        a.totalCollected  = d.optDouble("total_collected",  0);
        a.totalOutstanding= d.optDouble("total_outstanding",0);
        a.todayRevenue    = d.optDouble("today_revenue",    0);
        a.monthRevenue    = d.optDouble("month_revenue",    0);
        a.totalSalesCount = d.optInt("total_sales_count",   0);
        a.totalStockAdded = d.optInt("total_stock_added",   0);
        a.totalStockSold  = d.optInt("total_stock_sold",    0);

        JSONArray swr = d.optJSONArray("stock_wise_revenue");
        if (swr != null) for (int i = 0; i < swr.length(); i++)
            a.stockWiseRevenue.add(parseStockRev(swr.getJSONObject(i)));

        JSONArray bs = d.optJSONArray("best_selling");
        if (bs != null) for (int i = 0; i < bs.length(); i++)
            a.bestSelling.add(parseStockRev(bs.getJSONObject(i)));

        JSONArray sas = d.optJSONArray("stock_added_summary");
        if (sas != null) for (int i = 0; i < sas.length(); i++)
            a.stockAddedSummary.add(parseStockSum(sas.getJSONObject(i)));

        JSONArray cr = d.optJSONArray("company_revenue");
        if (cr != null) for (int i = 0; i < cr.length(); i++)
            a.companyRevenue.add(parseCompanyRev(cr.getJSONObject(i)));

        return a;
    }

    private static StockRevenueItem parseStockRev(JSONObject o) throws Exception {
        StockRevenueItem s = new StockRevenueItem();
        s.productId   = o.optInt("product_id");
        s.companyName = o.optString("company_name","");
        s.modelName   = o.optString("model_name","");
        s.tireType    = o.optString("tire_type","");
        s.tireSize    = o.optString("tire_size","");
        s.unitPrice   = o.optDouble("unit_price",0);
        s.currentStock= o.optInt("current_stock",0);
        s.qtySold     = o.optInt("qty_sold", o.optInt("total_qty_sold",0));
        s.revenue     = o.optDouble("revenue", o.optDouble("total_revenue",0));
        s.collected   = o.optDouble("collected",0);
        s.outstanding = o.optDouble("outstanding",0);
        s.saleCount   = o.optInt("sale_count",0);
        return s;
    }

    private static StockSummaryItem parseStockSum(JSONObject o) throws Exception {
        StockSummaryItem s = new StockSummaryItem();
        s.productId    = o.optInt("product_id");
        s.companyName  = o.optString("company_name","");
        s.modelName    = o.optString("model_name","");
        s.tireType     = o.optString("tire_type","");
        s.tireSize     = o.optString("tire_size","");
        s.currentStock = o.optInt("current_stock",0);
        s.totalAdded   = o.optInt("total_added",0);
        s.totalSold    = o.optInt("total_sold",0);
        return s;
    }

    private static CompanyRevItem parseCompanyRev(JSONObject o) throws Exception {
        CompanyRevItem c = new CompanyRevItem();
        c.companyName = o.optString("company_name","");
        c.revenue     = o.optDouble("revenue",0);
        c.qtySold     = o.optInt("qty_sold",0);
        c.saleCount   = o.optInt("sale_count",0);
        return c;
    }
}