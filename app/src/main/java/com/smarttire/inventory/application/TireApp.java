// FILE: app/src/main/java/com/smarttire/inventory/application/TireApp.java  (REPLACE)
package com.smarttire.inventory.application;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.smarttire.inventory.utils.NotificationHelper;
import com.smarttire.inventory.utils.ThemeManager;

public class TireApp extends Application {

    private static TireApp instance;
    private RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply saved theme before any Activity starts
        ThemeManager.applyTheme(this);

        // Register notification channels (safe to call multiple times)
        NotificationHelper.createChannels(this);
    }

    public static synchronized TireApp getInstance() {
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return requestQueue;
    }
}
