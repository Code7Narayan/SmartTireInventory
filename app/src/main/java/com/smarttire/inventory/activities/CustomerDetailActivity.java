// FILE: activities/CustomerDetailActivity.java (UPDATED — Fixed Invalid Sale ID error by finding a valid sale)
package com.smarttire.inventory.activities;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.adapters.PaymentAdapter;
import com.smarttire.inventory.adapters.SalesAdapter;
import com.smarttire.inventory.fragments.SaleDetailBottomSheet;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.models.Payment;
import com.smarttire.inventory.models.SaleRecord;
import com.smarttire.inventory.network.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends AppCompatActivity {

    private RecyclerView rvMain;
    private ProgressBar progressBar;
    private ApiService api;
    private int customerId;

    private SalesAdapter salesAdapter;
    private PaymentAdapter paymentAdapter;
    private HeaderAdapter headerAdapter;
    private SectionTitleAdapter salesTitle, paymentTitle;
    private Customer currentCustomer;
    private List<SaleRecord> currentSalesList = new ArrayList<>();

    private static final NumberFormat INR_FMT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        api = ApiService.getInstance(this);
        customerId = getIntent().getIntExtra("customer_id", 0);

        if (customerId == 0) {
            Toast.makeText(this, "Invalid Customer ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        loadDetails();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String name = getIntent().getStringExtra("customer_name");
            getSupportActionBar().setTitle(name != null ? name : "Customer Detail");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvMain = findViewById(R.id.rvCustomerDetailMain);
        progressBar = findViewById(R.id.progressBar);

        headerAdapter = new HeaderAdapter();
        salesTitle = new SectionTitleAdapter("Purchase History");
        salesAdapter = new SalesAdapter(this, new ArrayList<>());
        salesAdapter.setOnSaleClickListener(sale -> {
            SaleDetailBottomSheet.newInstance(sale, sale.getCustomerNameSnap(), sale.getGstNumber())
                    .show(getSupportFragmentManager(), "SaleDetail");
        });
        paymentTitle = new SectionTitleAdapter("Payment History");
        paymentAdapter = new PaymentAdapter(this, new ArrayList<>());

        ConcatAdapter concatAdapter = new ConcatAdapter(
                headerAdapter,
                salesTitle,
                salesAdapter,
                paymentTitle,
                paymentAdapter
        );

        rvMain.setLayoutManager(new LinearLayoutManager(this));
        rvMain.setAdapter(concatAdapter);
    }

    private void loadDetails() {
        progressBar.setVisibility(View.VISIBLE);
        api.getCustomerDetails(customerId, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                if (!response.optBoolean("success")) {
                    Toast.makeText(CustomerDetailActivity.this, 
                        response.optString("message", "Error loading details"), Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject data = response.getJSONObject("data");
                    
                    JSONObject cust = data.getJSONObject("customer");
                    JSONObject ledger = data.getJSONObject("ledger");
                    headerAdapter.update(cust, ledger);
                    
                    currentCustomer = new Customer();
                    currentCustomer.setId(customerId);
                    currentCustomer.setName(cust.optString("name"));
                    currentCustomer.setPhone(cust.optString("phone"));
                    currentCustomer.setAddress(cust.optString("address"));
                    currentCustomer.setGstNumber(cust.optString("gst_number"));

                    // Update sales list
                    currentSalesList.clear();
                    JSONArray salesArr = data.optJSONArray("sales");
                    if (salesArr != null) {
                        for (int i = 0; i < salesArr.length(); i++) {
                            currentSalesList.add(SaleRecord.fromJSON(salesArr.getJSONObject(i)));
                        }
                    }
                    salesAdapter.updateData(currentSalesList);
                    salesTitle.setVisible(!currentSalesList.isEmpty());

                    List<Payment> payments = new ArrayList<>();
                    JSONArray payArr = data.optJSONArray("payments");
                    if (payArr != null) {
                        for (int i = 0; i < payArr.length(); i++) {
                            payments.add(Payment.fromJSON(payArr.getJSONObject(i)));
                        }
                    }
                    paymentAdapter.updateData(payments);
                    paymentTitle.setVisible(!payments.isEmpty());

                } catch (Exception e) {
                    Log.e("CustomerDetail", "Parse error", e);
                    Toast.makeText(CustomerDetailActivity.this, "Data parsing error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Inner Adapters ---

    class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.VH> {
        private JSONObject cust, ledger;

        void update(JSONObject cust, JSONObject ledger) {
            this.cust = cust;
            this.ledger = ledger;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.view_customer_detail_header, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            if (cust == null || ledger == null) return;
            
            h.tvName.setText(cust.optString("name"));
            h.tvPhone.setText(cust.optString("phone"));
            h.tvAddress.setText(cust.optString("address"));
            h.tvGst.setText("GST: " + (cust.optString("gst_number").isEmpty() ? "—" : cust.optString("gst_number")));

            h.tvTotalSales.setText(String.valueOf(ledger.optInt("total_sales")));
            h.tvTotalAmount.setText(INR_FMT.format(ledger.optDouble("total_amount")));
            h.tvTotalPaid.setText(INR_FMT.format(ledger.optDouble("total_paid")));
            h.tvTotalDue.setText(INR_FMT.format(ledger.optDouble("total_due")));

            double due = ledger.optDouble("total_due", 0);
            h.btnPayNow.setVisibility(due > 0.01 ? View.VISIBLE : View.GONE);
            h.btnPayNow.setOnClickListener(v -> showQuickPaymentDialog());
            
            h.btnEdit.setOnClickListener(v -> showEditDialog());
            h.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        }

        @Override public int getItemCount() { return 1; }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvAddress, tvGst, tvTotalSales, tvTotalAmount, tvTotalPaid, tvTotalDue;
            ImageButton btnEdit;
            MaterialButton btnDelete, btnPayNow;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvPhone = v.findViewById(R.id.tvPhone);
                tvAddress = v.findViewById(R.id.tvAddress);
                tvGst = v.findViewById(R.id.tvGst);
                tvTotalSales = v.findViewById(R.id.tvTotalSales);
                tvTotalAmount = v.findViewById(R.id.tvTotalAmount);
                tvTotalPaid = v.findViewById(R.id.tvTotalPaid);
                tvTotalDue = v.findViewById(R.id.tvTotalDue);
                btnEdit = v.findViewById(R.id.btnEdit);
                btnDelete = v.findViewById(R.id.btnDelete);
                btnPayNow = v.findViewById(R.id.btnPayNow);
            }
        }
    }

    static class SectionTitleAdapter extends RecyclerView.Adapter<SectionTitleAdapter.VH> {
        private final String title;
        private boolean visible = false;
        SectionTitleAdapter(String t) { this.title = t; }
        void setVisible(boolean v) { this.visible = v; notifyDataSetChanged(); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            TextView tv = new TextView(p.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
            tv.setPadding(48, 48, 48, 16);
            tv.setTextSize(16);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(ContextCompat.getColor(p.getContext(), R.color.text_primary));
            return new VH(tv);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) { ((TextView)h.itemView).setText(title); }
        @Override public int getItemCount() { return visible ? 1 : 0; }
        static class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
    }

    // --- Actions ---

    private void showQuickPaymentDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Record Payment");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount");
        b.setView(input);
        b.setPositiveButton("Pay Cash", (d, w) -> processPayment(input.getText().toString(), "Cash"));
        b.setNeutralButton("Pay Online", (d, w) -> processPayment(input.getText().toString(), "Online"));
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void processPayment(String amountStr, String mode) {
        if (amountStr.isEmpty()) return;
        double amt = Double.parseDouble(amountStr);
        if (amt <= 0) return;

        // Find a sale ID with balance to record payment against
        int targetSaleId = 0;
        for (SaleRecord s : currentSalesList) {
            if (s.getRemainingAmount() > 0.01) {
                targetSaleId = s.getId();
                break;
            }
        }

        // Fallback to most recent sale if none have balance but sales exist
        if (targetSaleId == 0 && !currentSalesList.isEmpty()) {
            targetSaleId = currentSalesList.get(0).getId();
        }

        if (targetSaleId == 0) {
            Toast.makeText(this, "No sales found to record payment", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        // Using addPayment with a valid saleId found from the list
        api.addPayment(targetSaleId, amt, mode, "General customer payment", new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, response.optString("message"), Toast.LENGTH_SHORT).show();
                if (response.optBoolean("success")) loadDetails();
            }
            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog() {
        if (currentCustomer == null) return;
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_customer, null);
        TextInputEditText etName = v.findViewById(R.id.editCustomerName);
        TextInputEditText etPhone = v.findViewById(R.id.editCustomerPhone);
        TextInputEditText etAddress = v.findViewById(R.id.editCustomerAddress);
        TextInputEditText etGst = v.findViewById(R.id.editCustomerGst);

        etName.setText(currentCustomer.getName());
        etPhone.setText(currentCustomer.getPhone());
        etAddress.setText(currentCustomer.getAddress());
        etGst.setText(currentCustomer.getGstNumber());

        new AlertDialog.Builder(this)
                .setTitle("Edit Customer")
                .setView(v)
                .setPositiveButton("Update", (d, w) -> performUpdate(
                        etName.getText().toString(),
                        etPhone.getText().toString(),
                        etAddress.getText().toString(),
                        etGst.getText().toString()
                ))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUpdate(String name, String phone, String address, String gst) {
        progressBar.setVisibility(View.VISIBLE);
        api.updateCustomer(customerId, name, phone, address, gst, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, response.optString("message"), Toast.LENGTH_SHORT).show();
                if (response.optBoolean("success")) loadDetails();
            }
            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete this customer? This will also remove their sales history.")
                .setPositiveButton("Delete", (d, w) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete() {
        progressBar.setVisibility(View.VISIBLE);
        api.deleteCustomer(customerId, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, response.optString("message"), Toast.LENGTH_SHORT).show();
                if (response.optBoolean("success")) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
