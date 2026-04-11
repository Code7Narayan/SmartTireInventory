// FILE: utils/SalesHistoryPdfGenerator.java
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

import com.smarttire.inventory.models.SaleRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesHistoryPdfGenerator {

    private final Context ctx;
    private static final int W      = 595;
    private static final int H      = 842;
    private static final int MARGIN = 36;

    private static final int C_PRIMARY = Color.parseColor("#D32F2F");
    private static final int C_DARK    = Color.parseColor("#B71C1C");
    private static final int C_TEXT    = Color.parseColor("#212121");
    private static final int C_SEC     = Color.parseColor("#757575");
    private static final int C_DIV     = Color.parseColor("#E0E0E0");
    private static final int C_GREEN   = Color.parseColor("#388E3C");
    private static final int C_ORANGE  = Color.parseColor("#E65100");
    private static final int C_WHITE   = Color.WHITE;
    private static final int C_BG      = Color.parseColor("#FAFAFA");

    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

    public SalesHistoryPdfGenerator(Context ctx) { this.ctx = ctx; }

    public File generate(List<SaleRecord> sales, String dateRange, double totalRevenue, double totalDue) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page pg = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, 1).create());
        Canvas c = pg.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int y = drawHeader(c, p, dateRange);

        // Summary Cards
        y = drawSectionTitle(c, p, "History Summary", y);
        int cw = (W - MARGIN * 2 - 8) / 3;
        drawStatCard(c, p, MARGIN,           y, cw, "Total Sales", String.valueOf(sales.size()), C_PRIMARY);
        drawStatCard(c, p, MARGIN + cw + 4,  y, cw, "Total Due",   fmt.format(totalDue),         C_ORANGE);
        drawStatCard(c, p, MARGIN + cw*2+8,  y, cw, "Total Revenue", fmt.format(totalRevenue),   C_GREEN);
        y += 70;

        // Sales List
        y = drawSectionTitle(c, p, "Detailed Records", y);
        if (sales == null || sales.isEmpty()) {
            p.setColor(C_SEC); p.setTextSize(9f); p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(Typeface.DEFAULT);
            c.drawText("No sales records to display", MARGIN + 8, y + 14, p);
        } else {
            y = drawTableHeader(c, p, y, new String[]{"Customer / Product","Qty","Amount","Due","Status"}, new int[]{45,6,15,15,15});
            for (int i = 0; i < sales.size() && y < H - 80; i++) {
                SaleRecord s = sales.get(i);
                String customer = s.getCustomerNameSnap().isEmpty() ? "Walk-in" : s.getCustomerNameSnap();
                String product  = s.getCompanyName() + " " + s.getTireSize();
                String qty      = String.valueOf(s.getQuantity());
                String amount   = fmt.format(s.getTotalPrice());
                String due      = fmt.format(s.getRemainingAmount());
                String status   = s.getPaymentStatusLabel().toUpperCase();

                if (i % 2 == 0) { p.setColor(C_BG); c.drawRect(MARGIN, y-11, W-MARGIN, y+16, p); }
                
                // First line: Customer
                p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(8.5f);
                p.setTextAlign(Paint.Align.LEFT);
                drawTrunc(c, p, customer, MARGIN+4, y, 250);
                
                // Second line: Product
                p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(7.5f);
                drawTrunc(c, p, product, MARGIN+4, y+10, 250);

                p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(8.5f);
                c.drawText(qty, MARGIN+272, y+4, p);
                
                p.setColor(C_GREEN);
                c.drawText(amount, MARGIN+310, y+4, p);
                
                p.setColor(s.getRemainingAmount() > 0 ? C_ORANGE : C_GREEN);
                c.drawText(due, MARGIN+400, y+4, p);
                
                int statusColor = "partial".equalsIgnoreCase(status) ? C_ORANGE :
                        "unpaid".equalsIgnoreCase(status) ? C_PRIMARY : C_GREEN;
                p.setColor(statusColor); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(status, MARGIN+490, y+4, p);
                
                p.setColor(C_DIV); p.setStrokeWidth(0.5f);
                c.drawLine(MARGIN, y+14, W-MARGIN, y+14, p);
                y += 28;
            }
        }

        drawFooter(c, p);
        doc.finishPage(pg);
        File file = save(doc, "SalesHistory_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
        doc.close();
        return file;
    }

    private int drawHeader(Canvas c, Paint p, String dateRange) {
        p.setColor(C_PRIMARY); c.drawRect(0, 0, W, 88, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(20f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY", W/2f, 34, p);
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad  |  Mob: 9860808003", W/2f, 52, p);

        int y = 100;
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(15f);
        c.drawText("SALES HISTORY REPORT", W/2f, y, p);
        p.setColor(C_PRIMARY); p.setStrokeWidth(2);
        c.drawLine(MARGIN, y+6, W-MARGIN, y+6, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Range: " + (dateRange.isEmpty() ? "All Time" : dateRange), W-MARGIN, y+20, p);
        return y + 34;
    }

    private int drawSectionTitle(Canvas c, Paint p, String t, int y) {
        y += 6;
        p.setColor(Color.parseColor("#FFEBEE"));
        c.drawRect(MARGIN, y-13, W-MARGIN, y+5, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(10.5f); p.setTextAlign(Paint.Align.LEFT);
        c.drawText(t, MARGIN+6, y, p);
        return y + 16;
    }

    private void drawStatCard(Canvas c, Paint p, int x, int y, int w, String label, String value, int vc) {
        p.setColor(Color.parseColor("#F5F5F5")); p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(x, y, x+w, y+55), 6, 6, p);
        p.setColor(vc); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(13f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(value, x+w/2f, y+28, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        c.drawText(label, x+w/2f, y+46, p);
    }

    private int drawTableHeader(Canvas c, Paint p, int y, String[] headers, int[] pcts) {
        p.setColor(C_DARK); c.drawRect(MARGIN, y-13, W-MARGIN, y+6, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(9.5f);
        p.setTextAlign(Paint.Align.LEFT);
        int avail = W - MARGIN*2, xPos = MARGIN+4;
        for (int i = 0; i < headers.length; i++) {
            c.drawText(headers[i], xPos, y, p);
            xPos += avail * pcts[i] / 100;
        }
        return y + 18;
    }

    private void drawTrunc(Canvas c, Paint p, String text, int x, int y, int maxW) {
        if (text == null) text = "—";
        if (p.measureText(text) <= maxW) { c.drawText(text, x, y, p); return; }
        while (p.measureText(text + "…") > maxW && text.length() > 1)
            text = text.substring(0, text.length()-1);
        c.drawText(text + "…", x, y, p);
    }

    private void drawFooter(Canvas c, Paint p) {
        p.setColor(C_DIV); p.setStrokeWidth(1);
        c.drawLine(MARGIN, H-48, W-MARGIN, H-48, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(10f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("Thank you — Anand Tire Inventory", W/2f, H-30, p);
    }

    private File save(PdfDocument doc, String name) {
        String fn = name + "_" + System.currentTimeMillis() + ".pdf";
        File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fn);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            doc.writeTo(fos); return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public void openPdf(File file) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName()+".provider", file);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {}
    }
}