package com.smarttire.inventory.models;

import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;

public class Customer {

    private int id;
    private String name;
    private String phone;
    private String address;
    private String gstNumber;

    private int totalSales;
    private double totalAmount;
    private double totalPaid;
    private double totalDue;

    public Customer() {}

    public Customer(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    // ── Getters / Setters ─────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public int getTotalSales() { return totalSales; }
    public void setTotalSales(int totalSales) { this.totalSales = totalSales; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }

    public double getTotalDue() { return totalDue; }
    public void setTotalDue(double totalDue) { this.totalDue = totalDue; }

    // ── Helper Methods ─────────────────────────────

    public boolean hasDue() {
        return totalDue > 0.0;
    }

    public String getFormattedDue() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(totalDue);
    }

    public String getFormattedTotalAmount() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(totalAmount);
    }

    // ── JSON Parsing (SAFE + FIXED) ─────────────────

    public static Customer fromJSON(JSONObject o) {
        Customer c = new Customer();

        try {
            // ID handling (flexible)
            if (o.has("id")) c.id = o.optInt("id");
            else if (o.has("customer_id")) c.id = o.optInt("customer_id");

            c.name        = o.optString("name", "");
            c.phone       = o.optString("phone", "");
            c.address     = o.optString("address", "");
            c.gstNumber   = o.optString("gst_number", "");

            c.totalSales  = o.optInt("total_sales", 0);
            c.totalAmount = o.optDouble("total_amount", 0);
            c.totalPaid   = o.optDouble("total_paid", 0);
            c.totalDue    = o.optDouble("total_due", 0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return c;
    }

    @Override
    public String toString() {
        return name + " (" + phone + ")";
    }
}