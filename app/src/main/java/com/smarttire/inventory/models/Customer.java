// FILE: models/Customer.java
package com.smarttire.inventory.models;

import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;

public class Customer {
    private int    id;
    private String name;
    private String phone;
    private String address;
    private String gstNumber;
    private int    totalSales;
    private double totalAmount;
    private double totalPaid;
    private double totalDue;

    public Customer() {}

    public Customer(int id, String name, String phone) {
        this.id    = id;
        this.name  = name;
        this.phone = phone;
    }

    // Getters / setters
    public int    getId()          { return id; }
    public void   setId(int v)     { this.id = v; }
    public String getName()        { return name; }
    public void   setName(String v){ this.name = v; }
    public String getPhone()       { return phone; }
    public void   setPhone(String v){ this.phone = v; }
    public String getAddress()     { return address; }
    public void   setAddress(String v){ this.address = v; }
    public String getGstNumber()   { return gstNumber; }
    public void   setGstNumber(String v){ this.gstNumber = v; }
    public int    getTotalSales()  { return totalSales; }
    public void   setTotalSales(int v){ this.totalSales = v; }
    public double getTotalAmount() { return totalAmount; }
    public void   setTotalAmount(double v){ this.totalAmount = v; }
    public double getTotalPaid()   { return totalPaid; }
    public void   setTotalPaid(double v){ this.totalPaid = v; }
    public double getTotalDue()    { return totalDue; }
    public void   setTotalDue(double v){ this.totalDue = v; }

    public boolean hasDue() { return totalDue > 0; }

    public String getFormattedDue() {
        return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(totalDue);
    }

    public String getFormattedTotalAmount() {
        return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(totalAmount);
    }

    /** Factory from JSON object in list response */
    public static Customer fromJSON(JSONObject o) throws Exception {
        Customer c = new Customer();
        c.id          = o.getInt("id");
        c.name        = o.optString("name","");
        c.phone       = o.optString("phone","");
        c.address     = o.optString("address","");
        c.gstNumber   = o.optString("gst_number","");
        c.totalSales  = o.optInt("total_sales", 0);
        c.totalAmount = o.optDouble("total_amount", 0);
        c.totalPaid   = o.optDouble("total_paid", 0);
        c.totalDue    = o.optDouble("total_due", 0);
        return c;
    }

    @Override public String toString() { return name + " (" + phone + ")"; }
}