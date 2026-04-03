// FILE: models/SaleRecord.java  (UPDATED — paid, remaining, paymentStatus fields added)
package com.smarttire.inventory.models;

import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents a single historical sale record.
 * UPDATED: paidAmount, remainingAmount, paymentStatus now populated from API.
 *          These fields drive the payment summary in SaleDetailBottomSheet PDF.
 */
public class SaleRecord {

    private int    id;
    private int    productId;
    private int    customerId;
    private String companyName;
    private String tireType;
    private String tireSize;
    private int    quantity;
    private double unitPrice;
    private double totalPrice;
    private String saleDate;

    // ── Payment fields (Task 3/5) ─────────────────────────────────────────────
    private double totalAmount;
    private double paidAmount;
    private double remainingAmount;
    private String paymentStatus;    // "paid" | "partial" | "unpaid"
    private String customerNameSnap; // snapshot at time of sale
    private String gstNumber;

    public SaleRecord() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public int    getProductId()       { return productId; }
    public void   setProductId(int v)  { this.productId = v; }

    public int    getCustomerId()      { return customerId; }
    public void   setCustomerId(int v) { this.customerId = v; }

    public String getCompanyName()     { return companyName; }
    public void   setCompanyName(String v) { this.companyName = v; }

    public String getTireType()        { return tireType; }
    public void   setTireType(String v){ this.tireType = v; }

    public String getTireSize()        { return tireSize; }
    public void   setTireSize(String v){ this.tireSize = v; }

    public int    getQuantity()        { return quantity; }
    public void   setQuantity(int v)   { this.quantity = v; }

    public double getUnitPrice()       { return unitPrice; }
    public void   setUnitPrice(double v){ this.unitPrice = v; }

    public double getTotalPrice()      { return totalPrice; }
    public void   setTotalPrice(double v){ this.totalPrice = v; }

    public String getSaleDate()        { return saleDate; }
    public void   setSaleDate(String v){ this.saleDate = v; }

    public double getTotalAmount()     { return totalAmount; }
    public void   setTotalAmount(double v){ this.totalAmount = v; }

    public double getPaidAmount()      { return paidAmount; }
    public void   setPaidAmount(double v){ this.paidAmount = v; }

    public double getRemainingAmount() { return remainingAmount; }
    public void   setRemainingAmount(double v){ this.remainingAmount = v; }

    public String getPaymentStatus()   { return paymentStatus != null ? paymentStatus : "paid"; }
    public void   setPaymentStatus(String v){ this.paymentStatus = v; }

    public String getCustomerNameSnap(){ return customerNameSnap != null ? customerNameSnap : ""; }
    public void   setCustomerNameSnap(String v){ this.customerNameSnap = v; }

    public String getGstNumber()       { return gstNumber != null ? gstNumber : ""; }
    public void   setGstNumber(String v){ this.gstNumber = v; }

    // ── Convenience ───────────────────────────────────────────────────────────

    public boolean hasDue() { return remainingAmount > 0.005; }

    public boolean isFullyPaid() {
        return "paid".equalsIgnoreCase(paymentStatus) && remainingAmount < 0.005;
    }

    public String getProductDisplayName() {
        return companyName + " - " + tireType + " (" + tireSize + ")";
    }

    private static NumberFormat inrFmt() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    public String getFormattedUnitPrice()      { return inrFmt().format(unitPrice); }
    public String getFormattedTotalPrice()     { return inrFmt().format(totalPrice); }
    public String getFormattedTotalAmount()    { return inrFmt().format(totalAmount > 0 ? totalAmount : totalPrice); }
    public String getFormattedPaidAmount()     { return inrFmt().format(paidAmount); }
    public String getFormattedRemainingAmount(){ return inrFmt().format(remainingAmount); }

    public String getPaymentStatusLabel() {
        if (paymentStatus == null) return "Paid";
        return switch (paymentStatus.toLowerCase()) {
            case "partial" -> "Partial";
            case "unpaid"  -> "Unpaid";
            default        -> "Paid";
        };
    }

    /** "25 Mar 2026  06:22" */
    public String getFormattedDate() {
        try {
            java.text.SimpleDateFormat in  =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
            java.util.Date d = in.parse(saleDate);
            return d != null ? out.format(d) : saleDate;
        } catch (Exception e) { return saleDate != null ? saleDate : ""; }
    }

    /** "25 Mar 2026" — for grouping */
    public String getDateOnly() {
        try {
            java.text.SimpleDateFormat in  =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            java.util.Date d = in.parse(saleDate);
            return d != null ? out.format(d) : "";
        } catch (Exception e) { return ""; }
    }

    // ── Factory from JSON ─────────────────────────────────────────────────────

    public static SaleRecord fromJSON(JSONObject obj) throws Exception {
        SaleRecord s = new SaleRecord();
        s.id              = obj.getInt("id");
        s.productId       = obj.optInt("product_id", 0);
        s.customerId      = obj.optInt("customer_id", 0);
        s.companyName     = obj.optString("company_name", "");
        s.tireType        = obj.optString("tire_type", "");
        s.tireSize        = obj.optString("tire_size", "");
        s.quantity        = obj.optInt("quantity", 0);
        s.unitPrice       = obj.optDouble("unit_price", 0.0);
        s.totalPrice      = obj.optDouble("total_price", 0.0);
        s.saleDate        = obj.optString("sale_date", "");

        // Payment fields — always present in get_sales_history.php response
        s.totalAmount     = obj.optDouble("total_amount",     s.totalPrice);
        s.paidAmount      = obj.optDouble("paid_amount",      s.totalPrice);
        s.remainingAmount = obj.optDouble("remaining_amount", 0.0);
        s.paymentStatus   = obj.optString("payment_status",  "paid");
        s.customerNameSnap= obj.optString("customer_name_snap", "");
        s.gstNumber       = obj.optString("gst_number", "");
        return s;
    }
}