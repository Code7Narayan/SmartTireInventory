package com.smarttire.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.models.Product;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MessageHelper {

    private static final String SHOP_NAME = "Annand Tires";
    private static final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static void sendOrderSms(Context context, Customer customer, List<Product> items, double total, double paid) {
        if (customer == null || customer.getPhone() == null || customer.getPhone().isEmpty()) {
            Toast.makeText(context, "Customer phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(customer.getName()).append(",\n");
        sb.append("Thank you for your purchase at ").append(SHOP_NAME).append(".\n\n");
        sb.append("Order Details:\n");
        
        for (Product item : items) {
            sb.append("- ").append(item.getDisplayName())
              .append(" x").append(item.getCartQuantity())
              .append(": ").append(fmt.format(item.getPrice() * item.getCartQuantity())).append("\n");
        }

        sb.append("\nTotal: ").append(fmt.format(total));
        sb.append("\nPaid: ").append(fmt.format(paid));
        double due = total - paid;
        if (due > 0) {
            sb.append("\nRemaining: ").append(fmt.format(due));
        }
        sb.append("\n\nThank you!");

        openSmsApp(context, customer.getPhone(), sb.toString());
    }

    public static void sendCustomerSummarySms(Context context, Customer customer, double totalAmount, double totalPaid, double totalDue) {
        if (customer == null || customer.getPhone() == null || customer.getPhone().isEmpty()) {
            Toast.makeText(context, "Customer phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Dear " + customer.getName() + ",\n" +
                "Statement from " + SHOP_NAME + ":\n" +
                "Total Purchase: " + fmt.format(totalAmount) + "\n" +
                "Total Paid: " + fmt.format(totalPaid) + "\n" +
                "Remaining Balance: " + fmt.format(totalDue) + "\n\n" +
                "Please clear your dues. Thank you!";

        openSmsApp(context, customer.getPhone(), message);
    }

    public static void sendDetailedStatementSms(Context context, Customer customer, double totalAmount, double totalPaid, double totalDue, List<String[]> sales) {
        if (customer == null || customer.getPhone() == null || customer.getPhone().isEmpty()) {
            Toast.makeText(context, "Customer phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(customer.getName()).append(",\n");
        sb.append("Detailed Statement from ").append(SHOP_NAME).append(":\n\n");
        
        if (sales != null && !sales.isEmpty()) {
            sb.append("Recent Purchases:\n");
            int count = 0;
            for (String[] sale : sales) {
                // date | product | qty | total | paid | remaining | status
                sb.append("- ").append(sale[0]).append(": ").append(sale[1]).append(" (").append(sale[3]).append(")\n");
                count++;
                if (count >= 5) break; // Limit to 5 recent items to avoid too long SMS
            }
            sb.append("\n");
        }

        sb.append("Total Amount: ").append(fmt.format(totalAmount)).append("\n");
        sb.append("Total Paid: ").append(fmt.format(totalPaid)).append("\n");
        sb.append("Balance Due: ").append(fmt.format(totalDue)).append("\n\n");
        sb.append("Thank you!");

        openSmsApp(context, customer.getPhone(), sb.toString());
    }

    private static void openSmsApp(Context context, String phone, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:" + phone));
            intent.putExtra("sms_body", message);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open SMS app", Toast.LENGTH_SHORT).show();
        }
    }
}
