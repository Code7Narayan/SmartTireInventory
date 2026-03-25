// FILE: models/Sale.java
package com.smarttire.inventory.models;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Sale {
    private int saleId;
    private String companyName;
    private String tireType;
    private String tireSize;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private int remainingStock;
    private String saleDate;

    public Sale() {
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTireType() {
        return tireType;
    }

    public void setTireType(String tireType) {
        this.tireType = tireType;
    }

    public String getTireSize() {
        return tireSize;
    }

    public void setTireSize(String tireSize) {
        this.tireSize = tireSize;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(int remainingStock) {
        this.remainingStock = remainingStock;
    }

    public String getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(String saleDate) {
        this.saleDate = saleDate;
    }

    public String getFormattedTotalPrice() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(totalPrice);
    }

    public String getFormattedUnitPrice() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(unitPrice);
    }

    public String getInvoiceNumber() {
        return "INV-" + saleId + "-" + System.currentTimeMillis() % 10000;
    }
}