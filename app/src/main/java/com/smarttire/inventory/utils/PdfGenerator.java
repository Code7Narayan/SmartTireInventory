// FILE: utils/PdfGenerator.java
package com.smarttire.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private Context context;
    private Sale sale;

    // Page dimensions (A4 in pixels at 72 DPI)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    // Colors
    private static final int COLOR_PRIMARY = Color.parseColor("#D32F2F");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#212121");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#757575");
    private static final int COLOR_DIVIDER = Color.parseColor("#E0E0E0");

    public PdfGenerator(Context context, Sale sale) {
        this.context = context;
        this.sale = sale;
    }

    public File generateInvoice() {
        PdfDocument document = new PdfDocument();

        // Create page info
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                PAGE_WIDTH, PAGE_HEIGHT, 1).create();

        // Start page
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Draw content
        drawInvoiceContent(canvas);

        // Finish page
        document.finishPage(page);

        // Save document
        File pdfFile = savePdfDocument(document);

        // Close document
        document.close();

        return pdfFile;
    }

    private void drawInvoiceContent(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int yPosition = 50;

        // ===== HEADER SECTION =====
        // Shop Name
        paint.setColor(COLOR_PRIMARY);
        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SMART TIRE INVENTORY", PAGE_WIDTH / 2, yPosition, paint);

        yPosition += 25;

        // Shop Address
        paint.setColor(COLOR_TEXT_SECONDARY);
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        canvas.drawText("123 Main Street, City, State - 123456", PAGE_WIDTH / 2, yPosition, paint);

        yPosition += 15;
        canvas.drawText("Phone: +91 1234567890 | Email: info@smarttire.com", PAGE_WIDTH / 2, yPosition, paint);

        yPosition += 20;

        // Divider line
        paint.setColor(COLOR_PRIMARY);
        paint.setStrokeWidth(2);
        canvas.drawLine(40, yPosition, PAGE_WIDTH - 40, yPosition, paint);

        yPosition += 30;

        // ===== INVOICE TITLE =====
        paint.setColor(COLOR_TEXT_PRIMARY);
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("SALES INVOICE", PAGE_WIDTH / 2, yPosition, paint);

        yPosition += 35;

        // ===== INVOICE DETAILS =====
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(11);
        paint.setFakeBoldText(false);

        // Invoice Number
        paint.setColor(COLOR_TEXT_SECONDARY);
        canvas.drawText("Invoice No:", 40, yPosition, paint);
        paint.setColor(COLOR_TEXT_PRIMARY);
        paint.setFakeBoldText(true);
        canvas.drawText(sale.getInvoiceNumber(), 120, yPosition, paint);

        // Date (right aligned)
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(COLOR_TEXT_SECONDARY);
        paint.setFakeBoldText(false);
        canvas.drawText("Date:", PAGE_WIDTH - 150, yPosition, paint);
        paint.setColor(COLOR_TEXT_PRIMARY);
        paint.setFakeBoldText(true);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        canvas.drawText(sdf.format(new Date()), PAGE_WIDTH - 40, yPosition, paint);

        yPosition += 40;

        // ===== TABLE HEADER =====
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(COLOR_PRIMARY);
        paint.setTextSize(11);
        paint.setFakeBoldText(true);

        // Draw table header background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#FFEBEE"));
        canvas.drawRect(40, yPosition - 15, PAGE_WIDTH - 40, yPosition + 8, bgPaint);

        // Table headers
        canvas.drawText("PRODUCT DETAILS", 50, yPosition, paint);
        canvas.drawText("QTY", 320, yPosition, paint);
        canvas.drawText("RATE", 380, yPosition, paint);
        canvas.drawText("AMOUNT", 470, yPosition, paint);

        yPosition += 25;

        // Divider
        paint.setColor(COLOR_DIVIDER);
        paint.setStrokeWidth(1);
        canvas.drawLine(40, yPosition, PAGE_WIDTH - 40, yPosition, paint);

        yPosition += 25;

        // ===== TABLE CONTENT =====
        paint.setColor(COLOR_TEXT_PRIMARY);
        paint.setFakeBoldText(false);
        paint.setTextSize(11);

        // Product name
        paint.setFakeBoldText(true);
        canvas.drawText(sale.getCompanyName(), 50, yPosition, paint);
        yPosition += 18;

        paint.setFakeBoldText(false);
        paint.setColor(COLOR_TEXT_SECONDARY);
        canvas.drawText(sale.getTireType() + " - " + sale.getTireSize(), 50, yPosition, paint);

        // Quantity
        paint.setColor(COLOR_TEXT_PRIMARY);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(sale.getQuantity()), 335, yPosition - 9, paint);

        // Rate
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        canvas.drawText(format.format(sale.getUnitPrice()), 410, yPosition - 9, paint);

        // Amount
        paint.setFakeBoldText(true);
        canvas.drawText(format.format(sale.getTotalPrice()), 510, yPosition - 9, paint);

        yPosition += 30;

        // Divider
        paint.setColor(COLOR_DIVIDER);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawLine(40, yPosition, PAGE_WIDTH - 40, yPosition, paint);

        yPosition += 30;

        // ===== TOTAL SECTION =====
        // Draw total background
        bgPaint.setColor(COLOR_PRIMARY);
        canvas.drawRect(300, yPosition - 15, PAGE_WIDTH - 40, yPosition + 20, bgPaint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("GRAND TOTAL:", 320, yPosition + 5, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(format.format(sale.getTotalPrice()), PAGE_WIDTH - 50, yPosition + 5, paint);

        yPosition += 60;

        // ===== REMAINING STOCK INFO =====
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(COLOR_TEXT_SECONDARY);
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        canvas.drawText("Remaining Stock: " + sale.getRemainingStock() + " units", 40, yPosition, paint);

        yPosition += 80;

        // ===== FOOTER =====
        paint.setColor(COLOR_DIVIDER);
        canvas.drawLine(40, yPosition, PAGE_WIDTH - 40, yPosition, paint);

        yPosition += 25;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(COLOR_PRIMARY);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("Thank you for your business!", PAGE_WIDTH / 2, yPosition, paint);

        yPosition += 20;

        paint.setColor(COLOR_TEXT_SECONDARY);
        paint.setTextSize(9);
        paint.setFakeBoldText(false);
        canvas.drawText("This is a computer generated invoice.", PAGE_WIDTH / 2, yPosition, paint);

        // ===== WATERMARK (Optional) =====
        paint.setColor(Color.parseColor("#20D32F2F"));
        paint.setTextSize(60);
        paint.setFakeBoldText(true);
        canvas.save();
        canvas.rotate(-45, PAGE_WIDTH / 2, PAGE_HEIGHT / 2);
        canvas.drawText("SMART TIRE", PAGE_WIDTH / 2 - 120, PAGE_HEIGHT / 2, paint);
        canvas.restore();
    }

    private File savePdfDocument(PdfDocument document) {
        String fileName = "Invoice_" + sale.getSaleId() + "_" + System.currentTimeMillis() + ".pdf";

        File directory;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Invoices");
        } else {
            directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "SmartTire/Invoices");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show();
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
            intent.putExtra(Intent.EXTRA_SUBJECT, "Invoice - " + sale.getInvoiceNumber());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Share Invoice"));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing PDF", Toast.LENGTH_SHORT).show();
        }
    }
}