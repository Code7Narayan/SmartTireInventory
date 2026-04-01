// FILE: app/src/main/java/com/smarttire/inventory/utils/NotificationHelper.java
package com.smarttire.inventory.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.smarttire.inventory.R;
import com.smarttire.inventory.activities.MainActivity;
import com.smarttire.inventory.models.Product;

import java.util.List;

/**
 * Centralised helper for all in-app notifications.
 *
 * Channels:
 *   CHANNEL_LOW_STOCK  – warnings when product quantity < 5
 *   CHANNEL_SALES      – daily sales summary
 */
public final class NotificationHelper {

    public static final String CHANNEL_LOW_STOCK = "low_stock_channel";
    public static final String CHANNEL_SALES     = "sales_channel";

    private static final int NOTIF_ID_LOW_STOCK  = 1001;
    private static final int NOTIF_ID_SALES      = 1002;

    private NotificationHelper() {}

    // ── Channel registration (call once from Application.onCreate) ───────────

    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel lowStockChannel = new NotificationChannel(
                    CHANNEL_LOW_STOCK,
                    "Low Stock Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            lowStockChannel.setDescription("Notifies when product stock falls below 5 units");

            NotificationChannel salesChannel = new NotificationChannel(
                    CHANNEL_SALES,
                    "Sales Summaries",
                    NotificationManager.IMPORTANCE_DEFAULT);
            salesChannel.setDescription("Daily and weekly sales summary");

            nm.createNotificationChannel(lowStockChannel);
            nm.createNotificationChannel(salesChannel);
        }
    }

    // ── Low Stock ────────────────────────────────────────────────────────────

    /**
     * Shows a single grouped notification for all low-stock products.
     * Call after loading the low-stock list from the API.
     */
    public static void showLowStockAlert(Context ctx, List<Product> lowStockItems) {
        if (lowStockItems == null || lowStockItems.isEmpty()) return;

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, flags);

        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Product p : lowStockItems) {
            if (shown++ > 0) sb.append("\n");
            sb.append("• ").append(p.getDisplayName())
              .append(" — ").append(p.getQuantity()).append(" left");
            if (shown >= 5) { sb.append("\n+").append(lowStockItems.size() - 5).append(" more"); break; }
        }

        String title = lowStockItems.size() == 1
                ? "⚠ 1 product is running low!"
                : "⚠ " + lowStockItems.size() + " products running low!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_LOW_STOCK)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(lowStockItems.get(0).getDisplayName())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sb.toString()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);  // Don't re-buzz on every load

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID_LOW_STOCK, builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS permission not granted (Android 13+)
        }
    }

    /** Cancel the low-stock notification (e.g., after restocking). */
    public static void cancelLowStockAlert(Context ctx) {
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID_LOW_STOCK);
    }

    // ── Sales Summary ────────────────────────────────────────────────────────

    /**
     * Shows a sales summary notification.
     *
     * @param salesCount  number of sales today
     * @param revenue     total revenue today (formatted string e.g. "₹12,500")
     */
    public static void showSalesSummary(Context ctx, int salesCount, String revenue) {
        Intent intent = new Intent(ctx, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, flags);

        String body = salesCount + " sales recorded today  •  Revenue: " + revenue;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_SALES)
                .setSmallIcon(R.drawable.ic_sell)
                .setContentTitle("Today's Sales Summary")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID_SALES, builder.build());
        } catch (SecurityException ignored) {}
    }
}
