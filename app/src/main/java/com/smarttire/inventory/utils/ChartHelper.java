// FILE: app/src/main/java/com/smarttire/inventory/utils/ChartHelper.java
package com.smarttire.inventory.utils;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.smarttire.inventory.R;
import com.smarttire.inventory.models.MonthlyStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralised helper that styles and populates MPAndroidChart BarCharts.
 * Call {@link #applyMonthlyRevenueChart} after the API returns monthly data.
 */
public final class ChartHelper {

    private ChartHelper() {}

    /**
     * Configures a BarChart with monthly revenue data.
     *
     * @param chart      The BarChart view from the layout
     * @param monthlyData List of MonthlyStats from the backend
     * @param ctx        Context for resolving theme colors
     */
    public static void applyMonthlyRevenueChart(BarChart chart,
                                                List<MonthlyStats> monthlyData,
                                                Context ctx) {
        if (chart == null || monthlyData == null || monthlyData.isEmpty()) return;

        // ── Build entries ────────────────────────────────────────────────────
        List<BarEntry> entries   = new ArrayList<>();
        List<String>   labels    = new ArrayList<>();
        int            idx       = 0;
        for (MonthlyStats s : monthlyData) {
            entries.add(new BarEntry(idx, (float) s.getRevenue()));
            labels.add(s.getMonthLabel());
            idx++;
        }

        // ── DataSet styling ──────────────────────────────────────────────────
        BarDataSet dataSet = new BarDataSet(entries, "Monthly Revenue (₹)");
        dataSet.setColors(
                Color.parseColor("#D32F2F"),
                Color.parseColor("#E53935"),
                Color.parseColor("#EF5350"),
                Color.parseColor("#F44336"),
                Color.parseColor("#C62828"),
                Color.parseColor("#B71C1C")
        );
        dataSet.setValueTextColor(Color.parseColor("#212121"));
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                float v = barEntry.getY();
                if (v >= 100_000) return String.format("₹%.1fL", v / 100_000);
                if (v >= 1_000)   return String.format("₹%.1fK", v / 1_000);
                return String.format("₹%.0f", v);
            }
        });

        // ── Chart global settings ────────────────────────────────────────────
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chart.setData(barData);

        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.getDescription().setEnabled(false);
        chart.setFitBars(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.animateY(800);

        // ── X Axis ───────────────────────────────────────────────────────────
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.parseColor("#757575"));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        // ── Y Axis ───────────────────────────────────────────────────────────
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.parseColor("#757575"));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                if (value >= 100_000) return "₹" + (int)(value / 100_000) + "L";
                if (value >= 1_000)   return "₹" + (int)(value / 1_000) + "K";
                return "₹" + (int)value;
            }
        });
        chart.getAxisRight().setEnabled(false);

        // ── Legend ───────────────────────────────────────────────────────────
        Legend legend = chart.getLegend();
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#212121"));

        chart.invalidate();
    }
}
