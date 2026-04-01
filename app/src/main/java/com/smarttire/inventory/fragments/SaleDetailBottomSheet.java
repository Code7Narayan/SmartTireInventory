// FILE: app/src/main/java/com/smarttire/inventory/fragments/SaleDetailBottomSheet.java
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Sale;
import com.smarttire.inventory.models.SaleRecord;
import com.smarttire.inventory.utils.PdfGenerator;

import java.io.File;

/**
 * Modal bottom sheet shown when the user taps a sale in the history list.
 * Displays sale details and lets the user regenerate the PDF invoice.
 */
public class SaleDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SALE_ID       = "sale_id";
    private static final String ARG_COMPANY       = "company";
    private static final String ARG_TIRE_TYPE     = "tire_type";
    private static final String ARG_TIRE_SIZE     = "tire_size";
    private static final String ARG_QTY           = "qty";
    private static final String ARG_UNIT_PRICE    = "unit_price";
    private static final String ARG_TOTAL_PRICE   = "total_price";
    private static final String ARG_DATE          = "date";

    public static SaleDetailBottomSheet newInstance(SaleRecord sale) {
        Bundle args = new Bundle();
        args.putInt(ARG_SALE_ID,     sale.getId());
        args.putString(ARG_COMPANY,  sale.getCompanyName());
        args.putString(ARG_TIRE_TYPE, sale.getTireType());
        args.putString(ARG_TIRE_SIZE, sale.getTireSize());
        args.putInt(ARG_QTY,         sale.getQuantity());
        args.putDouble(ARG_UNIT_PRICE,  sale.getUnitPrice());
        args.putDouble(ARG_TOTAL_PRICE, sale.getTotalPrice());
        args.putString(ARG_DATE,     sale.getFormattedDate());
        SaleDetailBottomSheet sheet = new SaleDetailBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sale_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        int    saleId     = args.getInt(ARG_SALE_ID);
        String company    = args.getString(ARG_COMPANY, "");
        String tireType   = args.getString(ARG_TIRE_TYPE, "");
        String tireSize   = args.getString(ARG_TIRE_SIZE, "");
        int    qty        = args.getInt(ARG_QTY);
        double unitPrice  = args.getDouble(ARG_UNIT_PRICE);
        double totalPrice = args.getDouble(ARG_TOTAL_PRICE);
        String date       = args.getString(ARG_DATE, "");

        // ── Bind views ────────────────────────────────────────────────────────
        ((TextView) view.findViewById(R.id.tvDetailSaleId)).setText("Sale #" + saleId);
        ((TextView) view.findViewById(R.id.tvDetailCompany)).setText(company);
        ((TextView) view.findViewById(R.id.tvDetailTireInfo)).setText(tireType + " — " + tireSize);
        ((TextView) view.findViewById(R.id.tvDetailQty)).setText("Qty: " + qty);
        ((TextView) view.findViewById(R.id.tvDetailUnitPrice)).setText(
                formatCurrency(unitPrice) + " / unit");
        ((TextView) view.findViewById(R.id.tvDetailTotalPrice)).setText(formatCurrency(totalPrice));
        ((TextView) view.findViewById(R.id.tvDetailDate)).setText(date);

        // ── Invoice button ────────────────────────────────────────────────────
        MaterialButton btnInvoice = view.findViewById(R.id.btnDetailGenerateInvoice);
        btnInvoice.setOnClickListener(v -> generateInvoice(
                saleId, company, tireType, tireSize, qty, unitPrice, totalPrice));
    }

    private void generateInvoice(int saleId, String company, String tireType,
                                 String tireSize, int qty, double unitPrice, double totalPrice) {
        Sale sale = new Sale();
        sale.setSaleId(saleId);
        sale.setCompanyName(company);
        sale.setTireType(tireType);
        sale.setTireSize(tireSize);
        sale.setQuantity(qty);
        sale.setUnitPrice(unitPrice);
        sale.setTotalPrice(totalPrice);
        sale.setRemainingStock(0); // historical — stock may have changed

        try {
            PdfGenerator gen = new PdfGenerator(requireContext(), sale);
            File pdf = gen.generateInvoice();
            if (pdf != null) {
                gen.openPdf(pdf);
            } else {
                Toast.makeText(requireContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Could not generate PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatCurrency(double value) {
        return java.text.NumberFormat.getCurrencyInstance(
                new java.util.Locale("en", "IN")).format(value);
    }
}
