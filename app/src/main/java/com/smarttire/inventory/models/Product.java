// FILE: models/Product.java
package com.smarttire.inventory.models;

import java.text.NumberFormat;
import java.util.Locale;

public class Product {
    private int id;
    private int companyId;
    private String companyName;
    private String tireType;
    private String tireSize;
    private int quantity;
    private double price;

    public Product() {
    }

    public Product(int id, int companyId, String companyName, String tireType,
                   String tireSize, int quantity, double price) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.tireType = tireType;
        this.tireSize = tireSize;
        this.quantity = quantity;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getFormattedPrice() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(price);
    }

    public String getDisplayName() {
        return companyName + " - " + tireType + " (" + tireSize + ")";
    }

    public boolean isLowStock() {
        return quantity < 5;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}