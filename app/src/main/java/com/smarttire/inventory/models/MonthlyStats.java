// FILE: app/src/main/java/com/smarttire/inventory/models/MonthlyStats.java
package com.smarttire.inventory.models;

import org.json.JSONObject;

/**
 * Represents aggregated sales data for a single month.
 * Used to populate the BarChart on the Dashboard.
 */
public class MonthlyStats {

    private String monthLabel;   // e.g. "Jan", "Feb"
    private int    salesCount;
    private double revenue;

    public MonthlyStats() {}

    public MonthlyStats(String monthLabel, int salesCount, double revenue) {
        this.monthLabel  = monthLabel;
        this.salesCount  = salesCount;
        this.revenue     = revenue;
    }

    public String getMonthLabel()  { return monthLabel; }
    public int    getSalesCount()  { return salesCount; }
    public double getRevenue()     { return revenue; }

    public void setMonthLabel(String v)  { this.monthLabel = v; }
    public void setSalesCount(int v)     { this.salesCount = v; }
    public void setRevenue(double v)     { this.revenue = v; }

    public static MonthlyStats fromJSON(JSONObject obj) throws Exception {
        MonthlyStats m = new MonthlyStats();
        m.monthLabel = obj.optString("month", "");
        m.salesCount = obj.optInt("count", 0);
        m.revenue    = obj.optDouble("revenue", 0.0);
        return m;
    }
}
