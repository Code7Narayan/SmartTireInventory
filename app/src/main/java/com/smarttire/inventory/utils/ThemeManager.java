// FILE: utils/ThemeManager.java
package com.smarttire.inventory.utils;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    public static void applyTheme(Context context) {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(context);
        if (prefManager.isDarkTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static void toggleTheme(Context context) {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(context);
        boolean isDark = prefManager.isDarkTheme();
        prefManager.setDarkTheme(!isDark);

        if (!isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static boolean isDarkTheme(Context context) {
        return SharedPrefManager.getInstance(context).isDarkTheme();
    }

    public static void setDarkTheme(Context context, boolean isDark) {
        SharedPrefManager.getInstance(context).setDarkTheme(isDark);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}