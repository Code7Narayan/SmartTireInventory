// FILE: models/Sale.java  (UPDATED — paidAmount, remainingAmount, paymentStatus, customerName)
package com.smarttire.inventory.models;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents a completed sale record.
 * UPDATED: Added paidAmount, remainingAmount, paymentStatus, customerName
 *          so PdfGenerator can show the full financial picture.
 */
public class Sale {

    private int    saleId;
    private String companyName;
    private String tireType;
    private String tireSize;
    private int    quantity;
    private double unitPrice;
    private double totalPrice;
    private int    remainingStock;
    private String saleDate;

    // ── NEW payment fields (Task 3 / Task 5) ─────────────────────────────────
    private double paidAmount      = 0.0;
    private double remainingAmount = 0.0;
    private String paymentStatus   = "paid";   // paid | partial | unpaid
    private String customerName    = "";

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int    getSaleId()             { return saleId; }
    public void   setSaleId(int v)        { this.saleId = v; }

    public String getCompanyName()        { return companyName; }
    public void   setCompanyName(String v){ this.companyName = v; }

    public String getTireType()           { return tireType; }
    public void   setTireType(String v)   { this.tireType = v; }

    public String getTireSize()           { return tireSize; }
    public void   setTireSize(String v)   { this.tireSize = v; }

    public int    getQuantity()           { return quantity; }
    public void   setQuantity(int v)      { this.quantity = v; }

    public double getUnitPrice()          { return unitPrice; }
    public void   setUnitPrice(double v)  { this.unitPrice = v; }

    public double getTotalPrice()         { return totalPrice; }
    public void   setTotalPrice(double v) { this.totalPrice = v; }

    public int    getRemainingStock()     { return remainingStock; }
    public void   setRemainingStock(int v){ this.remainingStock = v; }

    public String getSaleDate()           { return saleDate; }
    public void   setSaleDate(String v)   { this.saleDate = v; }

    // ── Payment fields ────────────────────────────────────────────────────────

    public double getPaidAmount()               { return paidAmount; }
    public void   setPaidAmount(double v)       { this.paidAmount = v; }

    public double getRemainingAmount()          { return remainingAmount; }
    public void   setRemainingAmount(double v)  { this.remainingAmount = v; }

    public String getPaymentStatus()            { return paymentStatus; }
    public void   setPaymentStatus(String v)    { this.paymentStatus = v != null ? v : "paid"; }

    public String getCustomerName()             { return customerName; }
    public void   setCustomerName(String v)     { this.customerName = v != null ? v.trim() : ""; }

    // ── Convenience ───────────────────────────────────────────────────────────

    public boolean isPartiallyPaid() {
        return "partial".equalsIgnoreCase(paymentStatus) || remainingAmount > 0.005;
    }

    public boolean isFullyPaid() {
        return "paid".equalsIgnoreCase(paymentStatus) && remainingAmount < 0.005;
    }

    public boolean isUnpaid() {
        return "unpaid".equalsIgnoreCase(paymentStatus) || paidAmount < 0.005;
    }

    public String getInvoiceNumber() {
        return "INV-" + saleId + "-" + System.currentTimeMillis() % 10000;
    }

    // ── Formatted helpers ─────────────────────────────────────────────────────

    private static NumberFormat fmt() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    public String getFormattedTotalPrice()     { return fmt().format(totalPrice); }
    public String getFormattedUnitPrice()      { return fmt().format(unitPrice); }
    public String getFormattedPaidAmount()     { return fmt().format(paidAmount); }
    public String getFormattedRemainingAmount(){ return fmt().format(remainingAmount); }

    public String getPaymentStatusLabel() {
        return switch (paymentStatus.toLowerCase()) {
            case "partial" -> "Partial";
            case "unpaid"  -> "Unpaid";
            default        -> "Paid";
        };
    }
}