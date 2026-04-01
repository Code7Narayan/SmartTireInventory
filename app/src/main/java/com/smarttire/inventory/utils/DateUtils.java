// FILE: app/src/main/java/com/smarttire/inventory/utils/DateUtils.java
package com.smarttire.inventory.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility methods for date parsing, formatting, and range calculations.
 */
public final class DateUtils {

    private static final String DB_FORMAT      = "yyyy-MM-dd HH:mm:ss";
    private static final String DISPLAY_FORMAT = "dd MMM yyyy";
    private static final String API_DATE_FORMAT = "yyyy-MM-dd";

    private DateUtils() {}

    /** Format a raw DB date string to a readable display format. */
    public static String formatDisplay(String dbDate) {
        if (dbDate == null || dbDate.isEmpty()) return "";
        try {
            SimpleDateFormat in  = new SimpleDateFormat(DB_FORMAT, Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault());
            Date d = in.parse(dbDate);
            return d != null ? out.format(d) : dbDate;
        } catch (Exception e) {
            return dbDate;
        }
    }

    /** Format a Date object as "dd MMM yyyy". */
    public static String formatDisplay(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault()).format(date);
    }

    /** Format a Date for use in API queries (yyyy-MM-dd). */
    public static String formatForApi(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault()).format(date);
    }

    /** Return today's date formatted for API. */
    public static String today() {
        return formatForApi(new Date());
    }

    /** Return the first day of the current month (yyyy-MM-dd). */
    public static String firstDayOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        return formatForApi(c.getTime());
    }

    /** Return a Date representing N days ago from today. */
    public static Date daysAgo(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -n);
        return c.getTime();
    }

    /** Return a Date representing the start of the current month. */
    public static Date startOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** Parse "yyyy-MM-dd" string to Date object. */
    public static Date parseApiDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault()).parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** Convert milliseconds (from MaterialDatePicker) to "yyyy-MM-dd" string. */
    public static String millisToApiDate(long millis) {
        return new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault()).format(new Date(millis));
    }

    /** Convert milliseconds to display format. */
    public static String millisToDisplay(long millis) {
        return new SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault()).format(new Date(millis));
    }

    /** Returns true if dbDate falls between startDate and endDate (inclusive). */
    public static boolean isBetween(String dbDate, Date startDate, Date endDate) {
        if (dbDate == null || startDate == null || endDate == null) return true;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(DB_FORMAT, Locale.getDefault());
            Date d = fmt.parse(dbDate);
            if (d == null) return true;
            return !d.before(startDate) && !d.after(endDate);
        } catch (Exception e) {
            return true;
        }
    }
}
