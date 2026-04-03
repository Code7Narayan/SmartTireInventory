// FILE: utils/PdfGenerator.java  (FULLY FIXED — Task 3, 4, 5)
// Shows: Total Amount, Paid Amount, Remaining Amount, Payment Status badge
// Works for ALL companies (dynamic company name from Sale object)
// Customer name printed ONCE — no duplication
package com.smarttire.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.smarttire.inventory.models.Sale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfGenerator {

    private final Context context;
    private final Sale    sale;

    private String customerName = "";
    private String gstNumber    = "";

    private static final int W      = 595;
    private static final int H      = 842;
    private static final int MARGIN = 40;

    private static final int C_PRIMARY    = Color.parseColor("#D32F2F");
    private static final int C_PRIMARY_DK = Color.parseColor("#B71C1C");
    private static final int C_TEXT       = Color.parseColor("#212121");
    private static final int C_TEXT_SEC   = Color.parseColor("#757575");
    private static final int C_DIVIDER    = Color.parseColor("#E0E0E0");
    private static final int C_BG_ROW     = Color.parseColor("#FFEBEE");
    private static final int C_SUCCESS    = Color.parseColor("#388E3C");
    private static final int C_WARNING    = Color.parseColor("#E65100");
    private static final int C_WHITE      = Color.WHITE;

    public PdfGenerator(Context context, Sale sale) {
        this.context = context;
        this.sale    = sale;
    }

    public PdfGenerator setCustomerName(String name) {
        this.customerName = name != null ? name.trim() : "";
        return this;
    }

    public PdfGenerator setGstNumber(String gst) {
        this.gstNumber = gst != null ? gst.trim() : "";
        return this;
    }

    public File generateInvoice() {
        PdfDocument doc  = new PdfDocument();
        PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(W, H, 1).create());
        drawPage(page.getCanvas());
        doc.finishPage(page);
        File file = save(doc);
        doc.close();
        return file;
    }

    public void openPdf(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT).show();
        }
    }

    public void sharePdf(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Invoice — " + sale.getInvoiceNumber());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Invoice"));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawPage(Canvas c) {
        Paint p   = new Paint(Paint.ANTI_ALIAS_FLAG);
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String now = new SimpleDateFormat("dd-MM-yyyy  HH:mm", Locale.getDefault()).format(new Date());

        // Header
        p.setColor(C_PRIMARY);
        c.drawRect(0, 0, W, 90, p);

        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(22); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY", W / 2f, 36, p);

        p.setTypeface(Typeface.DEFAULT); p.setTextSize(9.5f);
        c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad.", W / 2f, 54, p);
        c.drawText("Mob: 9860808003", W / 2f, 70, p);

        int y = 108;

        // Title
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(18); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("SALES INVOICE", W / 2f, y, p);
        p.setColor(C_PRIMARY); p.setStrokeWidth(2);
        c.drawLine(MARGIN, y + 6, W - MARGIN, y + 6, p);
        y += 26;

        // Invoice meta
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(10);
        p.setTextAlign(Paint.Align.LEFT); p.setColor(C_TEXT_SEC);
        c.drawText("Invoice No:", MARGIN, y, p);
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(sale.getInvoiceNumber(), MARGIN + 68, y, p);
        p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Date:  " + now, W - MARGIN, y, p);
        y += 20;

        // Customer row — ONCE, skipped for walk-in
        boolean hasCustomer = !customerName.isEmpty()
                && !"Walk-in Customer".equalsIgnoreCase(customerName)
                && !"Walk-in".equalsIgnoreCase(customerName);

        if (hasCustomer || !gstNumber.isEmpty()) {
            p.setTextAlign(Paint.Align.LEFT);
            if (hasCustomer) {
                p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT);
                c.drawText("Bill To:", MARGIN, y, p);
                p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(customerName, MARGIN + 55, y, p);
            }
            if (!gstNumber.isEmpty()) {
                p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT);
                p.setTextAlign(Paint.Align.RIGHT);
                c.drawText("GST:  " + gstNumber, W - MARGIN, y, p);
            }
            y += 20;
        }
        y += 6;

        // Table header
        p.setColor(C_BG_ROW);
        c.drawRect(MARGIN, y - 14, W - MARGIN, y + 10, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(10.5f);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText("PRODUCT DETAILS", MARGIN + 8, y, p);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("QTY", 340, y, p);
        c.drawText("RATE", 420, y, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("AMOUNT", W - MARGIN - 4, y, p);
        y += 22;

        p.setColor(C_DIVIDER); p.setStrokeWidth(1);
        c.drawLine(MARGIN, y, W - MARGIN, y, p);
        y += 20;

        // Product row — dynamic company name (ALL companies work)
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(11); p.setTextAlign(Paint.Align.LEFT);
        c.drawText(sale.getCompanyName(), MARGIN + 8, y, p);
        y += 17;
        p.setTypeface(Typeface.DEFAULT); p.setColor(C_TEXT_SEC);
        c.drawText(sale.getTireType() + "  –  " + sale.getTireSize(), MARGIN + 8, y, p);

        int cy = y - 8;
        p.setColor(C_TEXT); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(String.valueOf(sale.getQuantity()), 340, cy, p);
        c.drawText(fmt.format(sale.getUnitPrice()), 420, cy, p);
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(fmt.format(sale.getTotalPrice()), W - MARGIN - 4, cy, p);
        y += 22;

        p.setColor(C_DIVIDER); p.setStrokeWidth(1);
        c.drawLine(MARGIN, y, W - MARGIN, y, p);
        y += 20;

        // GST breakdown
        if (!gstNumber.isEmpty()) {
            double taxable = sale.getTotalPrice() / 1.18;
            double gstVal  = sale.getTotalPrice() - taxable;
            p.setTypeface(Typeface.DEFAULT); p.setTextSize(10); p.setColor(C_TEXT_SEC);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText("Sub-total (excl. 18% GST)", MARGIN + 8, y, p);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(fmt.format(taxable), W - MARGIN - 4, y, p);
            y += 17;
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText("CGST 9%  +  SGST 9%", MARGIN + 8, y, p);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(fmt.format(gstVal), W - MARGIN - 4, y, p);
            y += 17;
            p.setColor(C_DIVIDER); c.drawLine(MARGIN, y, W - MARGIN, y, p);
            y += 14;
        }

        // Grand total bar
        p.setColor(C_PRIMARY_DK);
        c.drawRoundRect(new RectF(MARGIN, y - 14, W - MARGIN, y + 22), 6, 6, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(13);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText("GRAND TOTAL", MARGIN + 12, y + 8, p);
        p.setTextSize(15); p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(fmt.format(sale.getTotalPrice()), W - MARGIN - 10, y + 8, p);
        y += 42;

        // ── PAYMENT SUMMARY (TASK 3 CORE FIX) ────────────────────────────────
        y = drawPaymentSummary(c, p, fmt, y);

        // Remaining stock note
        if (sale.getRemainingStock() > 0) {
            y += 8;
            p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText("Stock remaining after sale:  " + sale.getRemainingStock() + " units",
                    MARGIN, y, p);
        }

        // Footer
        y = H - 72;
        p.setColor(C_DIVIDER); p.setStrokeWidth(1);
        c.drawLine(MARGIN, y, W - MARGIN, y, p);
        y += 20;
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(12);
        c.drawText("Thank you for your business!", W / 2f, y, p);
        y += 18;
        p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9);
        c.drawText("Computer-generated invoice. No signature required.", W / 2f, y, p);

        // Watermark
        p.setColor(Color.parseColor("#0AD32F2F"));
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(72);
        p.setTextAlign(Paint.Align.CENTER);
        c.save(); c.rotate(-40, W / 2f, H / 2f);
        c.drawText("ANAND TIRE", W / 2f, H / 2f, p);
        c.restore();
    }

    /**
     * TASK 3 — Payment Summary Block
     * Draws three rows: Total Amount | Paid Amount | Remaining Amount
     * with colour-coded status badge.
     *
     * Format (matches requirement):
     *   Customer: [name]   Total: ₹7000   Paid: ₹5000   Remaining: ₹2000
     */
    private int drawPaymentSummary(Canvas c, Paint p, NumberFormat fmt, int y) {
        // Block background
        p.setColor(Color.parseColor("#F5F5F5"));
        c.drawRect(MARGIN, y, W - MARGIN, y + 82, p);

        // Section header
        p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(9.5f); p.setTextAlign(Paint.Align.LEFT);
        c.drawText("PAYMENT SUMMARY", MARGIN + 8, y + 14, p);

        p.setColor(C_DIVIDER); p.setStrokeWidth(0.8f);
        c.drawLine(MARGIN, y + 18, W - MARGIN, y + 18, p);

        // Row 1 — Total Amount
        drawPayRow(c, p, fmt, "Total Amount", sale.getTotalPrice(), C_TEXT, y + 34, false);

        // Row 2 — Paid Amount (green)
        drawPayRow(c, p, fmt, "Paid Amount", sale.getPaidAmount(), C_SUCCESS, y + 52, false);

        // Row 3 — Remaining Amount (orange/red if > 0, green if 0)
        boolean hasDue = sale.getRemainingAmount() > 0.005;
        drawPayRow(c, p, fmt, "Remaining Amount", sale.getRemainingAmount(),
                hasDue ? C_WARNING : C_SUCCESS, y + 70, hasDue);

        // Status badge (top-right)
        String statusLabel = sale.getPaymentStatusLabel().toUpperCase();
        int badgeColor = switch (sale.getPaymentStatus().toLowerCase()) {
            case "partial" -> C_WARNING;
            case "unpaid"  -> Color.parseColor("#C62828");
            default        -> C_SUCCESS;
        };
        p.setColor(badgeColor);
        c.drawRoundRect(new RectF(W - MARGIN - 68, y + 4, W - MARGIN - 4, y + 20), 8, 8, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(8.5f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(statusLabel, W - MARGIN - 36, y + 15, p);

        // Block bottom border
        p.setColor(C_DIVIDER); p.setStrokeWidth(0.8f);
        c.drawLine(MARGIN, y + 82, W - MARGIN, y + 82, p);

        return y + 94;
    }

    private void drawPayRow(Canvas c, Paint p, NumberFormat fmt,
                            String label, double value, int colour, int y, boolean highlight) {
        if (highlight) {
            p.setColor(Color.parseColor("#FFF3E0"));
            c.drawRect(MARGIN, y - 13, W - MARGIN, y + 5, p);
        }
        p.setColor(C_TEXT_SEC); p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(10); p.setTextAlign(Paint.Align.LEFT);
        c.drawText(label + ":", MARGIN + 8, y, p);

        p.setColor(colour);
        p.setTypeface(highlight ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(fmt.format(value), W - MARGIN - 8, y, p);
    }

    private File save(PdfDocument doc) {
        String fileName = "Invoice_" + sale.getSaleId() + "_" +
                System.currentTimeMillis() + ".pdf";
        File dir;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            dir = new File(context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), "Invoices");
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "SmartTire/Invoices");
        }
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            doc.writeTo(fos);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}