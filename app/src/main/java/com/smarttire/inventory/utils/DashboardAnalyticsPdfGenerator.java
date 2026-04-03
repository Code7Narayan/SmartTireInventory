// FILE: utils/DashboardAnalyticsPdfGenerator.java  (Task 2)
// Generates a multi-page analytics PDF:
//   Page 1 — KPI summary, best sellers, company revenue
//   Page 2+ — Stock-wise revenue detail table
//   Last section — Stock added vs sold
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
import java.util.List;
import java.util.Locale;

public class DashboardAnalyticsPdfGenerator {

    private final Context       ctx;
    private final AnalyticsData data;

    private static final int W      = 595;
    private static final int H      = 842;
    private static final int MARGIN = 36;

    private static final int C_PRIMARY = Color.parseColor("#D32F2F");
    private static final int C_DARK    = Color.parseColor("#B71C1C");
    private static final int C_TEXT    = Color.parseColor("#212121");
    private static final int C_SEC     = Color.parseColor("#757575");
    private static final int C_DIV    = Color.parseColor("#E0E0E0");
    private static final int C_GREEN   = Color.parseColor("#388E3C");
    private static final int C_ORANGE  = Color.parseColor("#E65100");
    private static final int C_WHITE   = Color.WHITE;
    private static final int C_BG_ALT  = Color.parseColor("#FAFAFA");

    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en","IN"));
    private final String now = new SimpleDateFormat("dd-MM-yyyy  HH:mm", Locale.getDefault())
            .format(new Date());
    private int pageNo = 0;

    public DashboardAnalyticsPdfGenerator(Context ctx, AnalyticsData data) {
        this.ctx  = ctx;
        this.data = data;
    }

    public File generate() {
        PdfDocument doc = new PdfDocument();

        // ── PAGE 1: KPIs + Best Sellers + Company Revenue ──────────────────────
        addPage1(doc);

        // ── PAGE 2+: Stock-wise Revenue ────────────────────────────────────────
        addStockRevenuePages(doc);

        // ── LAST PAGE: Stock Added vs Sold ─────────────────────────────────────
        addStockSummaryPage(doc);

        File file = save(doc);
        doc.close();
        return file;
    }

    // ── PAGE 1 ────────────────────────────────────────────────────────────────

    private void addPage1(PdfDocument doc) {
        PdfDocument.Page pg = startPage(doc);
        Canvas c = pg.getCanvas();
        Paint  p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int y = drawHeader(c, p, "BUSINESS ANALYTICS REPORT");

        // ── KPI cards row ─────────────────────────────────────────────────────
        y = drawSectionTitle(c, p, "Key Performance Indicators", y);
        int colW = (W - MARGIN * 2 - 8) / 3;

        drawKpiCard(c, p, MARGIN,             y, colW, "Total Revenue",
                fmt.format(data.totalRevenue), C_PRIMARY);
        drawKpiCard(c, p, MARGIN + colW + 4,  y, colW, "Total Collected",
                fmt.format(data.totalCollected), C_GREEN);
        drawKpiCard(c, p, MARGIN + colW*2+8,  y, colW, "Outstanding",
                fmt.format(data.totalOutstanding), C_ORANGE);
        y += 72;

        drawKpiCard(c, p, MARGIN,             y, colW, "Today Revenue",
                fmt.format(data.todayRevenue), C_TEXT);
        drawKpiCard(c, p, MARGIN + colW + 4,  y, colW, "Month Revenue",
                fmt.format(data.monthRevenue), C_TEXT);
        drawKpiCard(c, p, MARGIN + colW*2+8,  y, colW, "Stock Added / Sold",
                data.totalStockAdded + " / " + data.totalStockSold, C_TEXT);
        y += 78;

        // ── Best Sellers ──────────────────────────────────────────────────────
        y = drawSectionTitle(c, p, "Best-Selling Products (by Quantity Sold)", y);
        y = drawTableHeader(c, p, y, new String[]{"Product","Qty Sold","Revenue","Sales"},
                new int[]{48, 14, 24, 10});

        for (int i = 0; i < Math.min(5, data.bestSelling.size()); i++) {
            AnalyticsData.StockRevenueItem item = data.bestSelling.get(i);
            if (i % 2 == 0) { p.setColor(C_BG_ALT); c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p); }
            p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
            p.setTextAlign(Paint.Align.LEFT);
            drawTrunc(c, p, item.displayName(), MARGIN+4, y, 250);
            c.drawText(String.valueOf(item.qtySold),   MARGIN+270, y, p);
            p.setColor(C_GREEN);
            c.drawText(item.formattedRevenue(),        MARGIN+350, y, p);
            p.setColor(C_SEC);
            c.drawText(item.saleCount + " sales",      MARGIN+475, y, p);
            p.setColor(C_DIV); p.setStrokeWidth(0.5f);
            c.drawLine(MARGIN,y+5,W-MARGIN,y+5,p);
            y += 18;
        }
        y += 12;

        // ── Company Revenue ────────────────────────────────────────────────────
        y = drawSectionTitle(c, p, "Revenue by Company", y);
        y = drawTableHeader(c, p, y, new String[]{"Company","Revenue","Qty Sold"},
                new int[]{40, 35, 20});

        for (int i = 0; i < Math.min(8, data.companyRevenue.size()); i++) {
            AnalyticsData.CompanyRevItem co = data.companyRevenue.get(i);
            if (i % 2 == 0) { p.setColor(C_BG_ALT); c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p); }
            p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
            p.setTextAlign(Paint.Align.LEFT);
            drawTrunc(c, p, co.companyName, MARGIN+4, y, 200);
            p.setColor(C_GREEN); c.drawText(co.formattedRevenue(), MARGIN+210, y, p);
            p.setColor(C_TEXT);  c.drawText(co.qtySold + " units", MARGIN+410, y, p);
            p.setColor(C_DIV); p.setStrokeWidth(0.5f);
            c.drawLine(MARGIN,y+5,W-MARGIN,y+5,p);
            y += 18;
        }

        drawFooter(c, p, ++pageNo);
        doc.finishPage(pg);
    }

    // ── Stock-wise Revenue pages ──────────────────────────────────────────────

    private void addStockRevenuePages(PdfDocument doc) {
        if (data.stockWiseRevenue.isEmpty()) return;
        final int perPage = 26;
        int total = data.stockWiseRevenue.size();
        int pages = (int) Math.ceil(total / (double) perPage);

        for (int pg = 0; pg < pages; pg++) {
            PdfDocument.Page page = startPage(doc);
            Canvas c = page.getCanvas();
            Paint  p = new Paint(Paint.ANTI_ALIAS_FLAG);

            int y = drawHeader(c, p, "STOCK-WISE REVENUE DETAIL");

            // Page subtitle
            p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText("Page " + (pg+2) + " — showing " + (pg*perPage+1) + "–" +
                    Math.min((pg+1)*perPage, total) + " of " + total, W-MARGIN, y-8, p);

            y = drawTableHeader(c, p, y,
                    new String[]{"Product","Price","Sold","Revenue","Collected","Due"},
                    new int[]{36, 10, 7, 17, 16, 10});

            int start = pg * perPage;
            int end   = Math.min(start + perPage, total);
            for (int i = start; i < end; i++) {
                AnalyticsData.StockRevenueItem item = data.stockWiseRevenue.get(i);
                if (i % 2 == 0) { p.setColor(C_BG_ALT); c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p); }

                p.setTextAlign(Paint.Align.LEFT); p.setColor(C_TEXT);
                p.setTypeface(Typeface.DEFAULT); p.setTextSize(8.5f);
                drawTrunc(c, p, item.displayName(), MARGIN+4, y, 190);
                c.drawText(fmt.format(item.unitPrice),    MARGIN+205, y, p);
                c.drawText(String.valueOf(item.qtySold),  MARGIN+265, y, p);
                p.setColor(C_GREEN);
                c.drawText(fmt.format(item.revenue),      MARGIN+300, y, p);
                p.setColor(C_TEXT);
                c.drawText(fmt.format(item.collected),    MARGIN+390, y, p);
                p.setColor(item.outstanding > 0 ? C_ORANGE : C_GREEN);
                c.drawText(fmt.format(item.outstanding),  MARGIN+470, y, p);

                p.setColor(C_DIV); p.setStrokeWidth(0.5f);
                c.drawLine(MARGIN,y+5,W-MARGIN,y+5,p);
                y += 17;
            }

            // Totals row on last page
            if (pg == pages - 1) {
                y += 8;
                p.setColor(C_DARK);
                c.drawRect(MARGIN, y-13, W-MARGIN, y+6, p);
                p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(9.5f);
                p.setTextAlign(Paint.Align.LEFT);
                c.drawText("TOTALS", MARGIN+4, y, p);
                c.drawText(fmt.format(data.totalRevenue),     MARGIN+300, y, p);
                c.drawText(fmt.format(data.totalCollected),   MARGIN+390, y, p);
                p.setColor(Color.parseColor("#FFCC80"));
                c.drawText(fmt.format(data.totalOutstanding), MARGIN+470, y, p);
            }

            drawFooter(c, p, ++pageNo);
            doc.finishPage(page);
        }
    }

    // ── Stock Added vs Sold page ──────────────────────────────────────────────

    private void addStockSummaryPage(PdfDocument doc) {
        if (data.stockAddedSummary.isEmpty()) return;
        PdfDocument.Page pg = startPage(doc);
        Canvas c = pg.getCanvas();
        Paint  p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int y = drawHeader(c, p, "STOCK ADDED vs SOLD SUMMARY");

        // Aggregate banner
        int bw = (W - MARGIN*2 - 8) / 3;
        drawKpiCard(c, p, MARGIN,          y, bw, "Total Added", String.valueOf(data.totalStockAdded), C_GREEN);
        drawKpiCard(c, p, MARGIN+bw+4,     y, bw, "Total Sold",  String.valueOf(data.totalStockSold),  C_PRIMARY);
        drawKpiCard(c, p, MARGIN+bw*2+8,   y, bw, "In Stock",
                String.valueOf(data.totalStockAdded - data.totalStockSold), C_TEXT);
        y += 78;

        y = drawTableHeader(c, p, y,
                new String[]{"Product","Added","Sold","In Stock"},
                new int[]{55, 15, 15, 12});

        int limit = Math.min(data.stockAddedSummary.size(), 30);
        for (int i = 0; i < limit; i++) {
            AnalyticsData.StockSummaryItem item = data.stockAddedSummary.get(i);
            if (i % 2 == 0) { p.setColor(C_BG_ALT); c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p); }
            p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
            p.setTextAlign(Paint.Align.LEFT);
            drawTrunc(c, p, item.displayName(), MARGIN+4, y, 290);
            p.setColor(C_GREEN); c.drawText(String.valueOf(item.totalAdded),  MARGIN+320, y, p);
            p.setColor(C_PRIMARY); c.drawText(String.valueOf(item.totalSold), MARGIN+400, y, p);
            p.setColor(item.currentStock < 5 ? C_ORANGE : C_TEXT);
            p.setTypeface(item.currentStock < 5 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            c.drawText(String.valueOf(item.currentStock), MARGIN+480, y, p);
            p.setColor(C_DIV); p.setStrokeWidth(0.5f);
            c.drawLine(MARGIN,y+5,W-MARGIN,y+5,p);
            y += 18;
        }

        drawFooter(c, p, ++pageNo);
        doc.finishPage(pg);
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private PdfDocument.Page startPage(PdfDocument doc) {
        return doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo + 1).create());
    }

    private int drawHeader(Canvas c, Paint p, String title) {
        p.setColor(C_PRIMARY); c.drawRect(0, 0, W, 88, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(20f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY", W/2f, 34, p);
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad  |  Mob: 9860808003", W/2f, 52, p);

        int y = 100;
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(15f);
        c.drawText(title, W/2f, y, p);
        p.setColor(C_PRIMARY); p.setStrokeWidth(2);
        c.drawLine(MARGIN, y+6, W-MARGIN, y+6, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Generated: " + now, W-MARGIN, y+20, p);
        return y + 34;
    }

    private int drawSectionTitle(Canvas c, Paint p, String title, int y) {
        y += 6;
        p.setColor(Color.parseColor("#FFEBEE"));
        c.drawRect(MARGIN, y-13, W-MARGIN, y+5, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(10.5f); p.setTextAlign(Paint.Align.LEFT);
        c.drawText(title, MARGIN+6, y, p);
        return y + 16;
    }

    private void drawKpiCard(Canvas c, Paint p, int x, int y, int w,
                             String label, String value, int valueColor) {
        p.setColor(Color.parseColor("#F5F5F5")); p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(x, y, x+w, y+60), 6, 6, p);
        p.setColor(valueColor); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(13f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(value, x+w/2f, y+32, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        c.drawText(label, x+w/2f, y+50, p);
        p.setStyle(Paint.Style.FILL);
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
        p.setTextAlign(Paint.Align.LEFT);
        if (p.measureText(text) <= maxW) { c.drawText(text, x, y, p); return; }
        while (p.measureText(text + "…") > maxW && text.length() > 1)
            text = text.substring(0, text.length()-1);
        c.drawText(text + "…", x, y, p);
    }

    private void drawFooter(Canvas c, Paint p, int page) {
        p.setColor(C_DIV); p.setStrokeWidth(1);
        c.drawLine(MARGIN, H-48, W-MARGIN, H-48, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(10f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("Anand Tire Inventory — Analytics Report", W/2f, H-30, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(8.5f);
        c.drawText("Page " + page, W/2f, H-14, p);
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

    private File save(PdfDocument doc) {
        String name = "Analytics_" + new SimpleDateFormat("yyyyMMdd_HHmm",
                Locale.getDefault()).format(new Date()) + ".pdf";
        File dir;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Invoices");
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "SmartTire/Invoices");
        }
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, name);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            doc.writeTo(fos);
            return file;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public void openPdf(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(ctx, "PDF not found", Toast.LENGTH_SHORT).show(); return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName()+".provider", file);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(ctx, "No PDF viewer", Toast.LENGTH_SHORT).show();
        }
    }
}