// FILE: activities/CustomerActivity.java  (ENHANCED — export all customers PDF)
package com.smarttire.inventory.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.CustomerAdapter;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.StockPdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomerActivity extends AppCompatActivity {

    private TextInputEditText   etSearch;
    private SwipeRefreshLayout  swipeRefresh;
    private RecyclerView        rvCustomers;
    private LinearLayout        layoutEmpty;
    private ProgressBar         progressBar;

    private ApiService          api;
    private CustomerAdapter     adapter;
    private final List<Customer> customerList = new ArrayList<>();

    private int     currentPage = 1;
    private boolean isLoading   = false;
    private boolean hasMore     = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        api = ApiService.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etSearch    = findViewById(R.id.etSearchCustomer);
        swipeRefresh= findViewById(R.id.swipeRefresh);
        rvCustomers = findViewById(R.id.rvCustomers);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        adapter = new CustomerAdapter(this, customerList);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rvCustomers.setLayoutManager(llm);
        rvCustomers.setAdapter(adapter);

        adapter.setOnItemClickListener(customer -> {
            Intent i = new Intent(this, CustomerDetailActivity.class);
            i.putExtra("customer_id",   customer.getId());
            i.putExtra("customer_name", customer.getName());
            startActivity(i);
        });

        // Infinite scroll
        rvCustomers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;
                if (llm.findLastVisibleItemPosition() >= llm.getItemCount() - 4)
                    loadCustomers(false);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                loadCustomers(true);
            }
        });

        swipeRefresh.setOnRefreshListener(() -> loadCustomers(true));

        FloatingActionButton fab = findViewById(R.id.fabAddCustomer);
        fab.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AddCustomerActivity.class), 100));

        loadCustomers(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Export All PDF")
                .setIcon(R.drawable.ic_pdf)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            exportAllCustomersPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK) loadCustomers(true);
    }

    private void loadCustomers(boolean reset) {
        if (isLoading) return;
        isLoading = true;

        String search = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";

        if (reset) {
            currentPage = 1; hasMore = true;
            customerList.clear(); adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.VISIBLE);
        }

        api.getCustomers(search, currentPage, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                try {
                    if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                        List<Customer> page = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++)
                            page.add(Customer.fromJSON(data.getJSONObject(i)));

                        if (reset) customerList.clear();
                        int start = customerList.size();
                        customerList.addAll(page);
                        if (reset) adapter.notifyDataSetChanged();
                        else adapter.notifyItemRangeInserted(start, page.size());

                        hasMore = data.length() == 30;
                        currentPage++;
                        layoutEmpty.setVisibility(customerList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override public void onError(String error) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CustomerActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Fetch ALL customers (report mode) then export PDF */
    private void exportAllCustomersPdf() {
        Toast.makeText(this, "Fetching all customers for report…", Toast.LENGTH_SHORT).show();
        api.getAllCustomersForReport(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                try {
                    if (!response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        Toast.makeText(CustomerActivity.this, "Failed to fetch data",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                    List<Customer> all = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++)
                        all.add(Customer.fromJSON(data.getJSONObject(i)));

                    if (all.isEmpty()) {
                        Toast.makeText(CustomerActivity.this, "No customer data",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        StockPdfGenerator gen = new StockPdfGenerator(CustomerActivity.this);
                        File pdf = gen.generateAllCustomersPdf(all);
                        runOnUiThread(() -> {
                            if (pdf != null) gen.openPdf(pdf);
                            else Toast.makeText(CustomerActivity.this,
                                    "PDF failed", Toast.LENGTH_SHORT).show();
                        });
                    }).start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override public void onError(String error) {
                Toast.makeText(CustomerActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}