// FILE: models/Payment.java
package com.smarttire.inventory.models;

import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.Locale;

public class Payment {
    private int    id;
    private int    saleId;
    private int    customerId;
    private double amountPaid;
    private String paymentMode;
    private String note;
    private String paymentDate;

    public Payment() {}

    public int    getId()           { return id; }
    public void   setId(int v)      { this.id = v; }
    public int    getSaleId()       { return saleId; }
    public void   setSaleId(int v)  { this.saleId = v; }
    public int    getCustomerId()   { return customerId; }
    public void   setCustomerId(int v){ this.customerId = v; }
    public double getAmountPaid()   { return amountPaid; }
    public void   setAmountPaid(double v){ this.amountPaid = v; }
    public String getPaymentMode()  { return paymentMode; }
    public void   setPaymentMode(String v){ this.paymentMode = v; }
    public String getNote()         { return note; }
    public void   setNote(String v) { this.note = v; }
    public String getPaymentDate()  { return paymentDate; }
    public void   setPaymentDate(String v){ this.paymentDate = v; }

    public String getFormattedAmount() {
        return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(amountPaid);
    }

    public static Payment fromJSON(JSONObject o) throws Exception {
        Payment p     = new Payment();
        p.id          = o.getInt("id");
        p.saleId      = o.optInt("sale_id", 0);
        p.customerId  = o.optInt("customer_id", 0);
        p.amountPaid  = o.optDouble("amount_paid", 0);
        p.paymentMode = o.optString("payment_mode","cash");
        p.note        = o.optString("note","");
        p.paymentDate = o.optString("payment_date","");
        return p;
    }
}