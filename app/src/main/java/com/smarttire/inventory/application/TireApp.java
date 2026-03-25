// FILE: application/TireApp.java
package com.smarttire.inventory.application;

import android.app.Application;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.smarttire.inventory.utils.ThemeManager;

public class TireApp extends Application {

    private static TireApp instance;
    private RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply saved theme on app start
        ThemeManager.applyTheme(this);
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