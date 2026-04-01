// FILE: app/src/main/java/com/smarttire/inventory/models/SaleRecord.java
package com.smarttire.inventory.models;

import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents a single historical sale record from the sales table.
 * Used in SalesHistoryFragment.
 */
public class SaleRecord {

    private int    id;
    private int    productId;
    private String companyName;
    private String tireType;
    private String tireSize;
    private int    quantity;
    private double unitPrice;
    private double totalPrice;
    private String saleDate;  // raw ISO string from DB e.g. "2026-03-25 06:22:51"

    public SaleRecord() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int    getId()          { return id; }
    public void   setId(int id)    { this.id = id; }

    public int    getProductId()            { return productId; }
    public void   setProductId(int v)       { this.productId = v; }

    public String getCompanyName()          { return companyName; }
    public void   setCompanyName(String v)  { this.companyName = v; }

    public String getTireType()             { return tireType; }
    public void   setTireType(String v)     { this.tireType = v; }

    public String getTireSize()             { return tireSize; }
    public void   setTireSize(String v)     { this.tireSize = v; }

    public int    getQuantity()             { return quantity; }
    public void   setQuantity(int v)        { this.quantity = v; }

    public double getUnitPrice()            { return unitPrice; }
    public void   setUnitPrice(double v)    { this.unitPrice = v; }

    public double getTotalPrice()           { return totalPrice; }
    public void   setTotalPrice(double v)   { this.totalPrice = v; }

    public String getSaleDate()             { return saleDate; }
    public void   setSaleDate(String v)     { this.saleDate = v; }

    // ── Convenience ──────────────────────────────────────────────────────────

    public String getProductDisplayName() {
        return companyName + " - " + tireType + " (" + tireSize + ")";
    }

    public String getFormattedUnitPrice() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(unitPrice);
    }

    public String getFormattedTotalPrice() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(totalPrice);
    }

    /** Returns a user-friendly date like "25 Mar 2026  06:22" */
    public String getFormattedDate() {
        try {
            // Input: "2026-03-25 06:22:51"
            java.text.SimpleDateFormat inFmt  =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            java.text.SimpleDateFormat outFmt =
                    new java.text.SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
            java.util.Date date = inFmt.parse(saleDate);
            return date != null ? outFmt.format(date) : saleDate;
        } catch (Exception e) {
            return saleDate != null ? saleDate : "";
        }
    }

    /** Returns just the date portion "25 Mar 2026" for grouping headers. */
    public String getDateOnly() {
        try {
            java.text.SimpleDateFormat inFmt  =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            java.text.SimpleDateFormat outFmt =
                    new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            java.util.Date date = inFmt.parse(saleDate);
            return date != null ? outFmt.format(date) : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ── Factory from JSON ────────────────────────────────────────────────────

    public static SaleRecord fromJSON(JSONObject obj) throws Exception {
        SaleRecord s = new SaleRecord();
        s.id          = obj.getInt("id");
        s.productId   = obj.optInt("product_id", 0);
        s.companyName = obj.optString("company_name", "");
        s.tireType    = obj.optString("tire_type", "");
        s.tireSize    = obj.optString("tire_size", "");
        s.quantity    = obj.optInt("quantity", 0);
        s.unitPrice   = obj.optDouble("unit_price", 0.0);
        s.totalPrice  = obj.optDouble("total_price", 0.0);
        s.saleDate    = obj.optString("sale_date", "");
        return s;
    }
}
