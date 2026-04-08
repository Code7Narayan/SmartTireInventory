// FILE: utils/DailyReportPdfGenerator.java
// Generates a clean one-page PDF for daily stock added/sold report.
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DailyReportPdfGenerator {

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

    public DailyReportPdfGenerator(Context ctx) { this.ctx = ctx; }

    public File generate(String date, int totalAdded, int totalSold, double revenue,
                         JSONArray addDetail, JSONArray sellDetail) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page pg = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, 1).create());
        Canvas c = pg.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int y = drawHeader(c, p, date);

        // Summary Cards
        y = drawSectionTitle(c, p, "Daily Summary", y);
        int cw = (W - MARGIN * 2 - 8) / 3;
        drawStatCard(c, p, MARGIN,           y, cw, "Added Stock",  String.valueOf(totalAdded), C_PRIMARY);
        drawStatCard(c, p, MARGIN + cw + 4,  y, cw, "Sold Stock",   String.valueOf(totalSold),  C_ORANGE);
        drawStatCard(c, p, MARGIN + cw*2+8,  y, cw, "Revenue",      fmt.format(revenue),         C_GREEN);
        y += 70;

        // Added Stock Detail
        y = drawSectionTitle(c, p, "Stock Added", y);
        if (addDetail == null || addDetail.length() == 0) {
            p.setColor(C_SEC); p.setTextSize(9f); p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(Typeface.DEFAULT);
            c.drawText("No stock added on this date", MARGIN + 8, y + 14, p);
            y += 26;
        } else {
            y = drawTableHeader(c, p, y, new String[]{"Product","Qty","Time"}, new int[]{70,12,15});
            for (int i = 0; i < addDetail.length() && y < H - 150; i++) {
                try {
                    JSONObject o = addDetail.getJSONObject(i);
                    String name = o.optString("company_name") + " | " + o.optString("tire_type")
                            + " (" + o.optString("tire_size") + ")";
                    String qty  = String.valueOf(o.optInt("qty_added"));
                    String time = o.optString("created_at", "");
                    if (time.length() >= 16) time = time.substring(11, 16);

                    if (i % 2 == 0) { p.setColor(C_BG); c.drawRect(MARGIN, y-11, W-MARGIN, y+5, p); }
                    p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(8.5f);
                    p.setTextAlign(Paint.Align.LEFT);
                    drawTrunc(c, p, name, MARGIN+4, y, 340);
                    p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD);
                    c.drawText(qty, MARGIN+350, y, p);
                    p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT);
                    c.drawText(time, MARGIN+440, y, p);
                    p.setColor(C_DIV); p.setStrokeWidth(0.5f);
                    c.drawLine(MARGIN, y+4, W-MARGIN, y+4, p);
                    y += 17;
                } catch (Exception ignored) {}
            }
            y += 8;
        }

        // Sold Stock Detail
        y = drawSectionTitle(c, p, "Sales Made", y);
        if (sellDetail == null || sellDetail.length() == 0) {
            p.setColor(C_SEC); p.setTextSize(9f); p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(Typeface.DEFAULT);
            c.drawText("No sales on this date", MARGIN + 8, y + 14, p);
            y += 26;
        } else {
            y = drawTableHeader(c, p, y, new String[]{"Product","Qty","Amount","Status"}, new int[]{48,8,20,16});
            for (int i = 0; i < sellDetail.length() && y < H - 80; i++) {
                try {
                    JSONObject o = sellDetail.getJSONObject(i);
                    String name   = o.optString("company_name") + " | " + o.optString("tire_type")
                            + " (" + o.optString("tire_size") + ")";
                    String qty    = String.valueOf(o.optInt("quantity"));
                    String amount = fmt.format(o.optDouble("total_amount"));
                    String status = o.optString("payment_status","paid").toUpperCase();

                    if (i % 2 == 0) { p.setColor(C_BG); c.drawRect(MARGIN, y-11, W-MARGIN, y+5, p); }
                    p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT); p.setTextSize(8.5f);
                    p.setTextAlign(Paint.Align.LEFT);
                    drawTrunc(c, p, name, MARGIN+4, y, 260);
                    p.setColor(C_ORANGE); p.setTypeface(Typeface.DEFAULT_BOLD);
                    c.drawText(qty, MARGIN+272, y, p);
                    p.setColor(C_GREEN);
                    c.drawText(amount, MARGIN+310, y, p);
                    int statusColor = "partial".equals(status.toLowerCase()) ? C_ORANGE :
                            "unpaid".equals(status.toLowerCase()) ? C_PRIMARY : C_GREEN;
                    p.setColor(statusColor);
                    c.drawText(status, MARGIN+440, y, p);
                    p.setColor(C_DIV); p.setStrokeWidth(0.5f);
                    c.drawLine(MARGIN, y+4, W-MARGIN, y+4, p);
                    y += 17;
                } catch (Exception ignored) {}
            }
        }

        drawFooter(c, p);
        doc.finishPage(pg);
        File file = save(doc, "DailyReport_" + date.replace("-",""));
        doc.close();
        return file;
    }

    private int drawHeader(Canvas c, Paint p, String date) {
        p.setColor(C_PRIMARY); c.drawRect(0, 0, W, 88, p);
        p.setColor(C_WHITE); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(20f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY", W/2f, 34, p);
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad  |  Mob: 9860808003", W/2f, 52, p);

        int y = 100;
        p.setColor(C_TEXT); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(15f);
        c.drawText("DAILY STOCK REPORT", W/2f, y, p);
        p.setColor(C_PRIMARY); p.setStrokeWidth(2);
        c.drawLine(MARGIN, y+6, W-MARGIN, y+6, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Date: " + date + "   Generated: " +
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), W-MARGIN, y+20, p);
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
        p.setTextSize(14f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(value, x+w/2f, y+28, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(9f);
        c.drawText(label, x+w/2f, y+46, p);
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

    private void drawFooter(Canvas c, Paint p) {
        p.setColor(C_DIV); p.setStrokeWidth(1);
        c.drawLine(MARGIN, H-48, W-MARGIN, H-48, p);
        p.setColor(C_PRIMARY); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(10f);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("Thank you — Anand Tire Inventory", W/2f, H-30, p);
        p.setColor(C_SEC); p.setTypeface(Typeface.DEFAULT); p.setTextSize(8f);
        c.drawText("System-generated daily report.", W/2f, H-14, p);
    }

    private File save(PdfDocument doc, String name) {
        String fn = name + "_" + System.currentTimeMillis() + ".pdf";
        File dir;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Invoices");
        else
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SmartTire/Invoices");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fn);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            doc.writeTo(fos); return file;
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
        } catch (Exception e) { Toast.makeText(ctx, "No PDF viewer", Toast.LENGTH_SHORT).show(); }
    }
}