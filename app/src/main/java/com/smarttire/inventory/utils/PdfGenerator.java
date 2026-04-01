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

/**
 * Generates a professional A4 PDF invoice.
 * Now supports optional customer name and GST fields.
 */
public class PdfGenerator {

    private final Context context;
    private final Sale    sale;
    private       String  customerName = "";
    private       String  gstNumber    = "";

    // ── Page dimensions (A4 @ 72 DPI) ────────────────────────────────────────
    private static final int W = 595;
    private static final int H = 842;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_PRIMARY    = Color.parseColor("#D32F2F");
    private static final int C_PRIMARY_DK = Color.parseColor("#B71C1C");
    private static final int C_TEXT       = Color.parseColor("#212121");
    private static final int C_TEXT_SEC   = Color.parseColor("#757575");
    private static final int C_DIVIDER    = Color.parseColor("#E0E0E0");
    private static final int C_BG_HEADER  = Color.parseColor("#FFEBEE");
    private static final int C_WHITE      = Color.WHITE;

    // ── Margins ───────────────────────────────────────────────────────────────
    private static final int MARGIN = 40;

    public PdfGenerator(Context context, Sale sale) {
        this.context = context;
        this.sale    = sale;
    }

    /** Optional: set customer name that will appear on the invoice. */
    public PdfGenerator setCustomerName(String name) {
        this.customerName = name != null ? name.trim() : "";
        return this;
    }

    /** Optional: set GST number that will appear on the invoice. */
    public PdfGenerator setGstNumber(String gst) {
        this.gstNumber = gst != null ? gst.trim() : "";
        return this;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public File generateInvoice() {
        PdfDocument doc  = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(W, H, 1).create();
        PdfDocument.Page page     = doc.startPage(info);

        draw(page.getCanvas());

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
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
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
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    "Invoice — " + sale.getInvoiceNumber());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Invoice"));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void draw(Canvas c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String now = new SimpleDateFormat("dd-MM-yyyy  HH:mm", Locale.getDefault())
                .format(new Date());

        int y = MARGIN;

        // ── Header background strip ───────────────────────────────────────────
        p.setColor(C_PRIMARY);
        c.drawRect(0, 0, W, 90, p);

        // ── Shop name ─────────────────────────────────────────────────────────
        p.setColor(C_WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(22);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY", W / 2f, 38, p);

        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(9.5f);
        c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad.", W / 2f, 56, p);
        c.drawText("Mob no 9860808003", W / 2f, 72, p);

        y = 110;

        // ── Invoice title ─────────────────────────────────────────────────────
        p.setColor(C_TEXT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(18);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("SALES INVOICE", W / 2f, y, p);

        // ── Red underline ─────────────────────────────────────────────────────
        p.setColor(C_PRIMARY);
        p.setStrokeWidth(2);
        c.drawLine(MARGIN, y + 6, W - MARGIN, y + 6, p);
        y += 28;

        // ── Invoice meta row ──────────────────────────────────────────────────
        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(10);
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(C_TEXT_SEC);
        c.drawText("Invoice No:", MARGIN, y, p);
        p.setColor(C_TEXT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(sale.getInvoiceNumber(), MARGIN + 65, y, p);

        p.setColor(C_TEXT_SEC);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Date:  " + now, W - MARGIN, y, p);

        y += 20;

        // ── Customer / GST row (optional) ─────────────────────────────────────
        if (!customerName.isEmpty() || !gstNumber.isEmpty()) {
            p.setTextAlign(Paint.Align.LEFT);
            if (!customerName.isEmpty()) {
                p.setColor(C_TEXT_SEC);
                p.setTypeface(Typeface.DEFAULT);
                c.drawText("Customer:", MARGIN, y, p);
                p.setColor(C_TEXT);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(customerName, MARGIN + 60, y, p);
            }
            if (!gstNumber.isEmpty()) {
                p.setColor(C_TEXT_SEC);
                p.setTypeface(Typeface.DEFAULT);
                p.setTextAlign(Paint.Align.RIGHT);
                c.drawText("GST:  " + gstNumber, W - MARGIN, y, p);
            }
            y += 20;
        }

        y += 8;

        // ── Table header ──────────────────────────────────────────────────────
        p.setColor(C_BG_HEADER);
        c.drawRect(MARGIN, y - 14, W - MARGIN, y + 10, p);

        p.setColor(C_PRIMARY);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(10.5f);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText("PRODUCT DETAILS", MARGIN + 8, y, p);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("QTY",    340, y, p);
        c.drawText("RATE",   420, y, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("AMOUNT", W - MARGIN - 4, y, p);

        y += 22;

        // ── Divider ───────────────────────────────────────────────────────────
        p.setColor(C_DIVIDER);
        p.setStrokeWidth(1);
        c.drawLine(MARGIN, y, W - MARGIN, y, p);
        y += 20;

        // ── Product row ───────────────────────────────────────────────────────
        p.setColor(C_TEXT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(11);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(sale.getCompanyName(), MARGIN + 8, y, p);

        y += 17;
        p.setTypeface(Typeface.DEFAULT);
        p.setColor(C_TEXT_SEC);
        c.drawText(sale.getTireType() + "  –  " + sale.getTireSize(), MARGIN + 8, y, p);

        // Right-column values
        int rowCenterY = y - 8;
        p.setColor(C_TEXT);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(String.valueOf(sale.getQuantity()), 340, rowCenterY, p);
        c.drawText(fmt.format(sale.getUnitPrice()),    420, rowCenterY, p);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(fmt.format(sale.getTotalPrice()), W - MARGIN - 4, rowCenterY, p);

        y += 22;

        // ── Divider ───────────────────────────────────────────────────────────
        p.setColor(C_DIVIDER);
        p.setStrokeWidth(1);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawLine(MARGIN, y, W - MARGIN, y, p);
        y += 20;

        // ── GST calculation rows (18% GST breakdown if gstNumber provided) ────
        if (!gstNumber.isEmpty()) {
            double taxableVal = sale.getTotalPrice() / 1.18;
            double gstVal     = sale.getTotalPrice() - taxableVal;

            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(10);
            p.setColor(C_TEXT_SEC);
            c.drawText("Sub-total (excl. GST)", MARGIN + 8, y, p);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(fmt.format(taxableVal), W - MARGIN - 4, y, p);
            y += 18;

            p.setTextAlign(Paint.Align.LEFT);
            c.drawText("GST @ 18%", MARGIN + 8, y, p);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(fmt.format(gstVal), W - MARGIN - 4, y, p);
            y += 18;

            p.setColor(C_DIVIDER);
            c.drawLine(MARGIN, y, W - MARGIN, y, p);
            y += 14;
        }

        // ── Grand total bar ───────────────────────────────────────────────────
        p.setColor(C_PRIMARY_DK);
        RectF totalRect = new RectF(MARGIN, y - 14, W - MARGIN, y + 22);
        c.drawRoundRect(totalRect, 6, 6, p);

        p.setColor(C_WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(13);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText("GRAND TOTAL", MARGIN + 12, y + 8, p);
        p.setTextSize(15);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(fmt.format(sale.getTotalPrice()), W - MARGIN - 10, y + 8, p);
        y += 50;

        // ── Remaining stock info ──────────────────────────────────────────────
        if (sale.getRemainingStock() > 0) {
            p.setColor(C_TEXT_SEC);
            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(9);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText("Remaining stock after this sale:  " +
                    sale.getRemainingStock() + " units", MARGIN, y, p);
            y += 20;
        }

        // ── Footer ────────────────────────────────────────────────────────────
        y = H - 70;
        p.setColor(C_DIVIDER);
        p.setStrokeWidth(1);
        c.drawLine(MARGIN, y, W - MARGIN, y, p);
        y += 20;

        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(C_PRIMARY);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(12);
        c.drawText("Thank you for your business!", W / 2f, y, p);
        y += 18;

        p.setColor(C_TEXT_SEC);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(9);
        c.drawText("This is a computer-generated invoice. No signature required.", W / 2f, y, p);

        // ── Subtle watermark ──────────────────────────────────────────────────
        p.setColor(Color.parseColor("#10D32F2F"));
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(72);
        p.setTextAlign(Paint.Align.CENTER);
        c.save();
        c.rotate(-40, W / 2f, H / 2f);
        c.drawText("ANAND TIRE", W / 2f, H / 2f, p);
        c.restore();
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

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
