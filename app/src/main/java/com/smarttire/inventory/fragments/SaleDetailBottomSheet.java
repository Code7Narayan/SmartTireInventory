// FILE: fragments/SaleDetailBottomSheet.java  (COMPLETE — paid/remaining in UI and PDF)
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Sale;
import com.smarttire.inventory.models.SaleRecord;
import com.smarttire.inventory.utils.PdfGenerator;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class SaleDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SALE_ID      = "sale_id";
    private static final String ARG_COMPANY      = "company";
    private static final String ARG_TIRE_TYPE    = "tire_type";
    private static final String ARG_TIRE_SIZE    = "tire_size";
    private static final String ARG_QTY          = "qty";
    private static final String ARG_UNIT_PRICE   = "unit_price";
    private static final String ARG_TOTAL_PRICE  = "total_price";
    private static final String ARG_TOTAL_AMOUNT = "total_amount";
    private static final String ARG_PAID         = "paid_amount";
    private static final String ARG_REMAINING    = "remaining_amount";
    private static final String ARG_STATUS       = "payment_status";
    private static final String ARG_DATE         = "date";
    private static final String ARG_CUSTOMER     = "customer";
    private static final String ARG_GST          = "gst";

    public static SaleDetailBottomSheet newInstance(SaleRecord sale) {
        return newInstance(sale, sale.getCustomerNameSnap(), sale.getGstNumber());
    }

    public static SaleDetailBottomSheet newInstance(SaleRecord sale,
                                                    String customerSnap, String gst) {
        Bundle args = new Bundle();
        args.putInt(ARG_SALE_ID,      sale.getId());
        args.putString(ARG_COMPANY,   sale.getCompanyName());
        args.putString(ARG_TIRE_TYPE, sale.getTireType());
        args.putString(ARG_TIRE_SIZE, sale.getTireSize());
        args.putInt(ARG_QTY,          sale.getQuantity());
        args.putDouble(ARG_UNIT_PRICE,   sale.getUnitPrice());
        args.putDouble(ARG_TOTAL_PRICE,  sale.getTotalPrice());
        args.putDouble(ARG_TOTAL_AMOUNT, sale.getTotalAmount() > 0
                ? sale.getTotalAmount() : sale.getTotalPrice());
        args.putDouble(ARG_PAID,         sale.getPaidAmount());
        args.putDouble(ARG_REMAINING,    sale.getRemainingAmount());
        args.putString(ARG_STATUS,       sale.getPaymentStatus());
        args.putString(ARG_DATE,         sale.getFormattedDate());
        args.putString(ARG_CUSTOMER,     customerSnap != null ? customerSnap : "");
        args.putString(ARG_GST,          gst          != null ? gst          : "");
        SaleDetailBottomSheet s = new SaleDetailBottomSheet();
        s.setArguments(args);
        return s;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.bottom_sheet_sale_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle a = requireArguments();

        int    saleId      = a.getInt(ARG_SALE_ID);
        String company     = a.getString(ARG_COMPANY, "");
        String tireType    = a.getString(ARG_TIRE_TYPE, "");
        String tireSize    = a.getString(ARG_TIRE_SIZE, "");
        int    qty         = a.getInt(ARG_QTY);
        double unitPrice   = a.getDouble(ARG_UNIT_PRICE);
        double totalPrice  = a.getDouble(ARG_TOTAL_PRICE);
        double totalAmount = a.getDouble(ARG_TOTAL_AMOUNT, totalPrice);
        double paidAmount  = a.getDouble(ARG_PAID, totalPrice);
        double remaining   = a.getDouble(ARG_REMAINING, 0);
        String status      = a.getString(ARG_STATUS, "paid");
        String date        = a.getString(ARG_DATE, "");
        String customer    = a.getString(ARG_CUSTOMER, "");
        String gst         = a.getString(ARG_GST, "");

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Existing views
        setText(view, R.id.tvDetailSaleId,     "Sale #" + saleId);
        setText(view, R.id.tvDetailCompany,    company);
        setText(view, R.id.tvDetailTireInfo,   tireType + " — " + tireSize);
        setText(view, R.id.tvDetailQty,        "Qty: " + qty);
        setText(view, R.id.tvDetailUnitPrice,  fmt.format(unitPrice) + " / unit");
        setText(view, R.id.tvDetailTotalPrice, fmt.format(totalPrice));
        setText(view, R.id.tvDetailDate,       date);

        // Payment summary — safe bind (only sets if view exists)
        bindOptional(view, R.id.tvDetailTotalAmount,
                "Total Amount:  " + fmt.format(totalAmount), R.color.text_primary);
        bindOptional(view, R.id.tvDetailPaidAmount,
                "Paid:  " + fmt.format(paidAmount),          R.color.success);
        bindOptional(view, R.id.tvDetailRemaining,
                "Remaining:  " + fmt.format(remaining),
                remaining > 0.005 ? R.color.warning : R.color.success);

        // Status badge
        TextView tvStatus = view.findViewById(R.id.tvDetailPaymentStatus);
        if (tvStatus != null) {
            tvStatus.setText(statusLabel(status));
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor(status)));
        }

        // Customer
        TextView tvCust = view.findViewById(R.id.tvDetailCustomer);
        if (tvCust != null) {
            boolean walkin = customer.isEmpty() || "Walk-in Customer".equalsIgnoreCase(customer);
            tvCust.setVisibility(walkin ? View.GONE : View.VISIBLE);
            if (!walkin) tvCust.setText("Customer: " + customer);
        }

        // PDF button — passes ALL payment fields
        MaterialButton btn = view.findViewById(R.id.btnDetailGenerateInvoice);
        if (btn != null) {
            btn.setOnClickListener(v -> generateInvoice(
                    saleId, company, tireType, tireSize, qty,
                    unitPrice, totalPrice, totalAmount, paidAmount, remaining, status, customer, gst));
        }
    }

    private void generateInvoice(int saleId, String company, String tireType, String tireSize,
                                 int qty, double unitPrice, double totalPrice, double totalAmount,
                                 double paidAmount, double remaining, String status,
                                 String customer, String gst) {
        Sale sale = new Sale();
        sale.setSaleId(saleId);
        sale.setCompanyName(company);   // dynamic — all companies work
        sale.setTireType(tireType);
        sale.setTireSize(tireSize);
        sale.setQuantity(qty);
        sale.setUnitPrice(unitPrice);
        sale.setTotalPrice(totalPrice);
        sale.setRemainingStock(0);
        // Payment fields so PdfGenerator draws the payment summary block
        sale.setPaidAmount(paidAmount);
        sale.setRemainingAmount(remaining);
        sale.setPaymentStatus(status);
        sale.setCustomerName(customer);

        try {
            PdfGenerator gen = new PdfGenerator(requireContext(), sale)
                    .setCustomerName(customer)   // printed ONCE
                    .setGstNumber(gst);
            File pdf = gen.generateInvoice();
            if (pdf != null) gen.openPdf(pdf);
            else Toast.makeText(requireContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void bindOptional(View root, int id, String text, int colorRes) {
        TextView tv = root.findViewById(id);
        if (tv == null) return;
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        tv.setVisibility(View.VISIBLE);
    }

    private String statusLabel(String s) {
        if (s == null) return "PAID";
        return switch (s.toLowerCase()) {
            case "partial" -> "PARTIAL";
            case "unpaid"  -> "UNPAID";
            default        -> "PAID";
        };
    }

    private int statusColor(String s) {
        if (s == null) return R.color.success;
        return switch (s.toLowerCase()) {
            case "partial" -> R.color.warning;
            case "unpaid"  -> R.color.error;
            default        -> R.color.success;
        };
    }
}