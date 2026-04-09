package com.smarttire.inventory.models;

import java.text.NumberFormat;
import java.util.Locale;

public class Product {
    private int    id;
    private int    companyId;
    private String companyName;
    private String modelName;
    private String tireType;
    private String tireSize;
    private int    quantity;
    private double price;       // Used as Selling Price
    private double costPrice;   // Added to match requirements
    
    // Cart related fields
    private int     cartQuantity = 0;
    private boolean isInCart     = false;

    public Product() {}

    // Getters / setters
    public int    getId()            { return id; }
    public void   setId(int v)       { this.id = v; }
    public int    getCompanyId()     { return companyId; }
    public void   setCompanyId(int v){ this.companyId = v; }
    public String getCompanyName()   { return companyName != null ? companyName : ""; }
    public void   setCompanyName(String v){ this.companyName = v; }
    public String getModelName()     { return modelName != null ? modelName : ""; }
    public void   setModelName(String v)  { this.modelName = v; }
    public String getTireType()      { return tireType != null ? tireType : ""; }
    public void   setTireType(String v)  { this.tireType = v; }
    public String getTireSize()      { return tireSize != null ? tireSize : ""; }
    public void   setTireSize(String v)  { this.tireSize = v; }
    public int    getQuantity()      { return quantity; }
    public void   setQuantity(int v) { this.quantity = v; }
    public double getPrice()         { return price; }
    public void   setPrice(double v) { this.price = v; }
    public double getCostPrice()     { return costPrice; }
    public void   setCostPrice(double v) { this.costPrice = v; }

    public int     getCartQuantity()        { return cartQuantity; }
    public void    setCartQuantity(int v)   { this.cartQuantity = v; }
    public boolean isInCart()               { return isInCart; }
    public void    setInCart(boolean v)     { this.isInCart = v; }

    public String getFormattedPrice() {
        return formatINR(price);
    }
    
    public String getFormattedCostPrice() {
        return formatINR(costPrice);
    }

    private String formatINR(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(amount);
    }

    public String getDisplayName() {
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : tireType;
        return companyName + " " + model + " (" + tireSize + ")";
    }

    public boolean isLowStock() { return quantity < 5; }

    @Override public String toString() { return getDisplayName(); }
}