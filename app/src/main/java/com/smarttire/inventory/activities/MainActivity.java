// FILE: app/src/main/java/com/smarttire/inventory/activities/MainActivity.java  (UPDATED)
package com.smarttire.inventory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.fragments.DashboardFragment;
import com.smarttire.inventory.fragments.SalesHistoryFragment;
import com.smarttire.inventory.fragments.SellFragment;
import com.smarttire.inventory.fragments.StockFragment;
import com.smarttire.inventory.utils.SharedPrefManager;
import com.smarttire.inventory.utils.ThemeManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout         drawerLayout;
    private NavigationView       navigationView;
    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar      toolbar;
    private SharedPrefManager    prefManager;

    private int currentNavId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefManager = SharedPrefManager.getInstance(this);

        initViews();
        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment(), "Dashboard");
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
            currentNavId = R.id.nav_dashboard;
        }
    }

    private void initViews() {
        drawerLayout         = findViewById(R.id.drawerLayout);
        navigationView       = findViewById(R.id.navigationView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        toolbar              = findViewById(R.id.toolbar);

        View headerView  = navigationView.getHeaderView(0);
        TextView tvName  = headerView.findViewById(R.id.tvUserName);
        if (tvName != null) {
            tvName.setText(prefManager.getFullName());
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.white));
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentNavId) return true;

            Fragment f      = null;
            String   title  = "";

            if (id == R.id.nav_dashboard) {
                f = new DashboardFragment(); title = "Dashboard";
            } else if (id == R.id.nav_stock) {
                f = new StockFragment(); title = "Stock";
            } else if (id == R.id.nav_sell) {
                f = new SellFragment(); title = "Sell Product";
            } else if (id == R.id.nav_sales_history) {
                f = new SalesHistoryFragment(); title = "Sales History";
            }

            if (f != null) {
                currentNavId = id;
                loadFragment(f, title);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.drawer_dashboard) {
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);

        } else if (id == R.id.drawer_stock) {
            bottomNavigationView.setSelectedItemId(R.id.nav_stock);

        } else if (id == R.id.drawer_sell) {
            bottomNavigationView.setSelectedItemId(R.id.nav_sell);

        } else if (id == R.id.drawer_customers) {
            startActivity(new Intent(this, CustomerActivity.class));

        } else if (id == R.id.drawer_sales_history) {
            bottomNavigationView.setSelectedItemId(R.id.nav_sales_history);

        } else if (id == R.id.drawer_add_company) {
            startActivity(new Intent(this, AddCompanyActivity.class));

        } else if (id == R.id.drawer_add_stock) {
            startActivity(new Intent(this, AddStockActivity.class));

        } else if (id == R.id.drawer_theme) {
            ThemeManager.toggleTheme(this);

        } else if (id == R.id.drawer_logout) {
            showLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> performLogout())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void performLogout() {
        prefManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
