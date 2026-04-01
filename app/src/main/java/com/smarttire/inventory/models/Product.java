// FILE: models/Product.java  (ERP VERSION — REPLACE)
package com.smarttire.inventory.models;

import java.text.NumberFormat;
import java.util.Locale;

public class Product {
    private int    id;
    private int    companyId;
    private String companyName;
    private String modelName;   // ← NEW
    private String tireType;
    private String tireSize;
    private int    quantity;
    private double price;

    public Product() {}

    // Getters / setters
    public int    getId()            { return id; }
    public void   setId(int v)       { this.id = v; }
    public int    getCompanyId()     { return companyId; }
    public void   setCompanyId(int v){ this.companyId = v; }
    public String getCompanyName()   { return companyName; }
    public void   setCompanyName(String v){ this.companyName = v; }
    public String getModelName()     { return modelName != null ? modelName : ""; }
    public void   setModelName(String v)  { this.modelName = v; }
    public String getTireType()      { return tireType; }
    public void   setTireType(String v)  { this.tireType = v; }
    public String getTireSize()      { return tireSize; }
    public void   setTireSize(String v)  { this.tireSize = v; }
    public int    getQuantity()      { return quantity; }
    public void   setQuantity(int v) { this.quantity = v; }
    public double getPrice()         { return price; }
    public void   setPrice(double v) { this.price = v; }

    public String getFormattedPrice() {
        return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(price);
    }

    public String getDisplayName() {
        String model = (modelName != null && !modelName.isEmpty()) ? modelName + " — " : "";
        return companyName + " | " + model + tireType + " (" + tireSize + ")";
    }

    public boolean isLowStock() { return quantity < 5; }

    @Override public String toString() { return getDisplayName(); }
}