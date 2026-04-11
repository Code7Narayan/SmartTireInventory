// FILE: utils/DashboardAnalyticsPdfGenerator.java (UPDATED — Alignment and Formatting Fix)
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

import com.smarttire.inventory.models.AnalyticsData;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardAnalyticsPdfGenerator {

    private final Context       ctx;
    private final AnalyticsData data;

    private static final int W      = 595;
    private static final int H      = 842;
    private static final int MARGIN = 40;

    private static final int C_PRIMARY = Color.parseColor("#D32F2F");
    private static final int C_DARK    = Color.parseColor("#B71C1C");
    private static final int C_TEXT    = Color.parseColor("#212121");
    private static final int C_SEC     = Color.parseColor("#757575");
    private static final int C_DIV     = Color.parseColor("#E0E0E0");
    private static final int C_GREEN   = Color.parseColor("#388E3C");
    private static final int C_ORANGE  = Color.parseColor("#E65100");
    private static final int C_WHITE   = Color.WHITE;
    private static final int C_BG_ALT  = Color.parseColor("#F9F9F9");

    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
    private final String now = new SimpleDateFormat("dd-MM-yyyy  HH:mm", Locale.getDefault()).format(new Date());

    public DashboardAnalyticsPdfGenerator(Context ctx, AnalyticsData data) {
        this.ctx  = ctx;
        this.data = data;
    }

    public File generate() {
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page pg = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, 1).create());
        Canvas c = pg.getCanvas();
        Paint  p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int y = drawHeader(c, p, "DASHBOARD ANALYTICS REPORT");

        // ── 1. KPI SUMMARY SECTION ───────────────────────────────────────────
        y = drawSectionTitle(c, p, "Performance Summary", y);
        int cardW = (W - (MARGIN * 2) - 16) / 3;
        
        drawKpiCard(c, p, MARGIN,               y, cardW, "Total Revenue", fmt.format(data.totalRevenue), C_PRIMARY);
        drawKpiCard(c, p, MARGIN + cardW + 8,   y, cardW, "Total Collected", fmt.format(data.totalCollected), C_GREEN);
        drawKpiCard(c, p, MARGIN + (cardW * 2) + 16, y, cardW, "Total Outstanding", fmt.format(data.totalOutstanding), C_ORANGE);
        y += 85;

        // ── 2. STOCK FLOW SUMMARY ────────────────────────────────────────────
        y = drawSectionTitle(c, p, "Inventory Flow", y);
        drawKpiCard(c, p, MARGIN,               y, cardW, "Stock Added", String.valueOf(data.totalStockAdded), C_TEXT);
        drawKpiCard(c, p, MARGIN + cardW + 8,   y, cardW, "Stock Sold", String.valueOf(data.totalStockSold), C_TEXT);
        drawKpiCard(c, p, MARGIN + (cardW * 2) + 16, y, cardW, "Net Change", String.valueOf(data.totalStockAdded - data.totalStockSold), C_TEXT);
        y += 95;

        // ── 3. PRODUCT BREAKDOWN TABLE ───────────────────────────────────────
        y = drawSectionTitle(c, p, "Product Wise Details", y);
        y = drawTableHeader(c, p, y, 
            new String[]{"Product Description", "Sold", "Revenue", "Due Status"}, 
            new int[]{45, 10, 20, 25});

        if (data.stockWiseRevenue != null) {
            int limit = Math.min(data.stockWiseRevenue.size(), 20); // Show top 20 to fit one page
            for (int i = 0; i < limit; i++) {
                AnalyticsData.StockRevenueItem item = data.stockWiseRevenue.get(i);
                if (i % 2 == 0) { p.setColor(C_BG_ALT); c.drawRect(MARGIN, y - 13, W - MARGIN, y + 5, p); }
                
                p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
                p.setTextAlign(Paint.Align.LEFT);
                drawTrunc(c, p, item.displayName(), MARGIN + 6, y, 250);
                
                c.drawText(String.valueOf(item.qtySold), MARGIN + 270, y, p);
                
                p.setColor(C_GREEN); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(fmt.format(item.revenue), MARGIN + 330, y, p);
                
                p.setColor(item.outstanding > 0 ? C_ORANGE : C_GREEN);
                p.setTextSize(8f);
                String dueTxt = item.outstanding > 0 ? "Due: " + fmt.format(item.outstanding) : "Fully Paid";
                c.drawText(dueTxt, MARGIN + 450, y, p);
                
                p.setColor(C_DIV); p.setStrokeWidth(0.5f);
                c.drawLine(MARGIN, y + 8, W - MARGIN, y + 8, p);
                y += 20;
                
                if (y > H - 100) break; // Simple overflow check
            }
        }

        drawFooter(c, p);
        doc.finishPage(pg);
        File file = save(doc);
        doc.close();
        return file;
    }

    private int drawHeader(Canvas c, Paint p, String title) {
        p.setColor(C_PRIMARY); c.drawRect(0, 0, W, 90, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(22f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY", W / 2f, 38, p);
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(10f);
        c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad  |  Mob: 9860808003", W / 2f, 58, p);

        int y = 110;
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(16f);
        c.drawText(title, W / 2f, y, p);
        p.setColor(C_PRIMARY); p.setStrokeWidth(2.5f);
        c.drawLine(MARGIN, y + 8, W - MARGIN, y + 8, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Report Date: " + now, W - MARGIN, y + 22, p);
        return y + 40;
    }

    private int drawSectionTitle(Canvas c, Paint p, String title, int y) {
        y += 10;
        p.setColor(Color.parseColor("#FFF1F1"));
        c.drawRect(MARGIN, y - 15, W - MARGIN, y + 8, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(11f); p.setTextAlign(Paint.Align.LEFT);
        c.drawText(title.toUpperCase(), MARGIN + 10, y, p);
        return y + 25;
    }

    private void drawKpiCard(Canvas c, Paint p, int x, int y, int w, String label, String value, int vc) {
        p.setColor(Color.WHITE); p.setStyle(Paint.Style.FILL);
        p.setShadowLayer(4, 0, 2, Color.LTGRAY);
        RectF rect = new RectF(x, y, x + w, y + 65);
        c.drawRoundRect(rect, 10, 10, p);
        p.clearShadowLayer();
        
        p.setColor(C_DIV); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1f);
        c.drawRoundRect(rect, 10, 10, p);
        
        p.setStyle(Paint.Style.FILL);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(label, x + w / 2f, y + 22, p);
        
        p.setColor(vc); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(12.5f);
        c.drawText(value, x + w / 2f, y + 48, p);
    }

    private int drawTableHeader(Canvas c, Paint p, int y, String[] headers, int[] pcts) {
        p.setColor(C_DARK); c.drawRect(MARGIN, y - 15, W - MARGIN, y + 10, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(10f);
        p.setTextAlign(Paint.Align.LEFT);
        int avail = W - MARGIN * 2, xPos = MARGIN + 8;
        for (int i = 0; i < headers.length; i++) {
            c.drawText(headers[i], xPos, y, p);
            xPos += (avail * pcts[i]) / 100;
        }
        return y + 25;
    }

    private void drawTrunc(Canvas c, Paint p, String text, int x, int y, int maxW) {
        if (text == null) text = "—";
        if (p.measureText(text) <= maxW) { c.drawText(text, x, y, p); return; }
        while (p.measureText(text + "…") > maxW && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        c.drawText(text + "…", x, y, p);
    }

    private void drawFooter(Canvas c, Paint p) {
        p.setColor(C_DIV); p.setStrokeWidth(1.5f);
        c.drawLine(MARGIN, H - 60, W - MARGIN, H - 60, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(11f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("Thank you — Anand Tire Inventory Management System", W / 2f, H - 40, p);
    }

    private File save(PdfDocument doc) {
        String name = "Dashboard_Analytics_" + System.currentTimeMillis() + ".pdf";
        File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, name);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            doc.writeTo(fos); return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public void openPdf(File file) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", file);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {}
    }
}