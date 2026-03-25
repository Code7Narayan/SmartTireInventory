// FILE: utils/SharedPrefManager.java
package com.smarttire.inventory.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "SmartTirePrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_IS_DARK_THEME = "isDarkTheme";

    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // Login session management
    public void saveUserSession(int userId, String username, String fullName) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    public String getFullName() {
        return sharedPreferences.getString(KEY_FULL_NAME, "");
    }

    public void logout() {
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_FULL_NAME);
        editor.apply();
    }

    // Theme management
    public void setDarkTheme(boolean isDark) {
        editor.putBoolean(KEY_IS_DARK_THEME, isDark);
        editor.apply();
    }

    public boolean isDarkTheme() {
        return sharedPreferences.getBoolean(KEY_IS_DARK_THEME, false);
    }

    // Clear all preferences
    public void clearAll() {
        editor.clear();
        editor.apply();
    }
}