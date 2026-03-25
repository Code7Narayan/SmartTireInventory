// FILE: activities/MainActivity.java
package com.smarttire.inventory.activities;

import android.content.DialogInterface;
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
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.smarttire.inventory.R;
import com.smarttire.inventory.fragments.DashboardFragment;
import com.smarttire.inventory.fragments.SellFragment;
import com.smarttire.inventory.fragments.StockFragment;
import com.smarttire.inventory.utils.SharedPrefManager;
import com.smarttire.inventory.utils.ThemeManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;

    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefManager = SharedPrefManager.getInstance(this);

        initViews();
        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        toolbar = findViewById(R.id.toolbar);

        // Update navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        tvUserName.setText(prefManager.getFullName());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_dashboard, R.string.nav_dashboard);

        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.white));
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
                toolbar.setTitle(R.string.nav_dashboard);
            } else if (itemId == R.id.nav_stock) {
                fragment = new StockFragment();
                toolbar.setTitle(R.string.nav_stock);
            } else if (itemId == R.id.nav_sell) {
                fragment = new SellFragment();
                toolbar.setTitle(R.string.nav_sell);
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.drawer_dashboard) {
            loadFragment(new DashboardFragment());
            toolbar.setTitle(R.string.nav_dashboard);
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);

        } else if (itemId == R.id.drawer_stock) {
            loadFragment(new StockFragment());
            toolbar.setTitle(R.string.nav_stock);
            bottomNavigationView.setSelectedItemId(R.id.nav_stock);

        } else if (itemId == R.id.drawer_sell) {
            loadFragment(new SellFragment());
            toolbar.setTitle(R.string.nav_sell);
            bottomNavigationView.setSelectedItemId(R.id.nav_sell);

        } else if (itemId == R.id.drawer_add_company) {
            startActivity(new Intent(this, AddCompanyActivity.class));

        } else if (itemId == R.id.drawer_add_stock) {
            startActivity(new Intent(this, AddStockActivity.class));

        } else if (itemId == R.id.drawer_theme) {
            toggleTheme();

        } else if (itemId == R.id.drawer_logout) {
            showLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void toggleTheme() {
        boolean isDark = ThemeManager.isDarkTheme(this);
        String themeMessage = isDark ? "Switching to Light Theme" : "Switching to Dark Theme";
        Toast.makeText(this, themeMessage, Toast.LENGTH_SHORT).show();
        ThemeManager.toggleTheme(this);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> performLogout())
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