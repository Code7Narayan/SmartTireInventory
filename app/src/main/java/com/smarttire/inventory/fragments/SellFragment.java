// FILE: fragments/SellFragment.java  (ERP VERSION — FULL REPLACEMENT)
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.models.Sale;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.PdfGenerator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────────
    private AutoCompleteTextView    spinnerProduct, spinnerCustomer, spinnerPayMode;
    private CardView                cardProductInfo, cardCustomerCredit;
    private TextView                tvAvailableStock, tvUnitPrice, tvTotalPrice;
    private TextView                tvCustomerDue;
    private TextInputLayout         tilQuantity, tilPaid;
    private TextInputEditText       etQuantity, etPaidAmount, etCustomerName, etGstNumber;
    private MaterialButton          btnSell, btnAddCustomer, btnGenerateInvoice, btnShareInvoice;
    private LinearProgressIndicator progressBar;
    private LinearLayout            layoutInvoiceActions;

    // ── State ─────────────────────────────────────────────────────────────────
    private ApiService       api;
    private List<Product>    productList  = new ArrayList<>();
    private List<Customer>   customerList = new ArrayList<>();
    private Product          selectedProduct;
    private Customer         selectedCustomer;
    private Sale             lastSale;
    private File             lastPdfFile;

    public static SellFragment newInstance() { return new SellFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiService.getInstance(requireContext());
        initViews(view);
        setupListeners();
        loadProducts();
        loadCustomers();
    }

    private void initViews(View v) {
        spinnerProduct      = v.findViewById(R.id.spinnerProduct);
        spinnerCustomer     = v.findViewById(R.id.spinnerCustomer);
        spinnerPayMode      = v.findViewById(R.id.spinnerPaymentMode);
        cardProductInfo     = v.findViewById(R.id.cardProductInfo);
        cardCustomerCredit  = v.findViewById(R.id.cardCustomerCredit);
        tvAvailableStock    = v.findViewById(R.id.tvAvailableStock);
        tvUnitPrice         = v.findViewById(R.id.tvUnitPrice);
        tvTotalPrice        = v.findViewById(R.id.tvTotalPrice);
        tvCustomerDue       = v.findViewById(R.id.tvCustomerDue);
        tilQuantity         = v.findViewById(R.id.tilQuantity);
        tilPaid             = v.findViewById(R.id.tilPaid);
        etQuantity          = v.findViewById(R.id.etQuantity);
        etPaidAmount        = v.findViewById(R.id.etPaidAmount);
        etCustomerName      = v.findViewById(R.id.etCustomerName);
        etGstNumber         = v.findViewById(R.id.etGstNumber);
        btnSell             = v.findViewById(R.id.btnSell);
        btnAddCustomer      = v.findViewById(R.id.btnAddCustomer);
        btnGenerateInvoice  = v.findViewById(R.id.btnGenerateInvoice);
        btnShareInvoice     = v.findViewById(R.id.btnShareInvoice);
        progressBar         = v.findViewById(R.id.progressBar);
        layoutInvoiceActions= v.findViewById(R.id.layoutInvoiceActions);

        // Payment modes
        String[] modes = {"cash","upi","card","bank_transfer","cheque"};
        spinnerPayMode.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, modes));
        spinnerPayMode.setText(modes[0], false);

        if (layoutInvoiceActions != null) layoutInvoiceActions.setVisibility(View.GONE);
    }

    private void setupListeners() {
        spinnerProduct.setOnItemClickListener((p, v, pos, id) -> {
            selectedProduct = productList.get(pos);
            updateProductInfo();
        });

        spinnerCustomer.setOnItemClickListener((p, v, pos, id) -> {
            selectedCustomer = customerList.get(pos);
            updateCustomerInfo();
        });

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                calculateTotal();
            }
        });

        btnAddCustomer.setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        requireContext(),
                        com.smarttire.inventory.activities.AddCustomerActivity.class)));

        btnSell.setOnClickListener(v -> performSell());
        if (btnGenerateInvoice != null) btnGenerateInvoice.setOnClickListener(v -> openInvoice());
        if (btnShareInvoice    != null) btnShareInvoice.setOnClickListener(v -> shareInvoice());
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadProducts() {
        showLoading(true);
        api.getProducts(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject r) {
                if (!isAdded()) return;
                showLoading(false);
                try {
                    if (r.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                        productList.clear();
                        for (int i = 0; i < data.length(); i++)
                            productList.add(parseProduct(data.getJSONObject(i)));
                        spinnerProduct.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, productList));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String err) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCustomers() {
        api.getCustomers("", 1, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject r) {
                if (!isAdded()) return;
                try {
                    if (r.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                        customerList.clear();
                        for (int i = 0; i < data.length(); i++)
                            customerList.add(Customer.fromJSON(data.getJSONObject(i)));
                        spinnerCustomer.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, customerList));
                        // Default: Walk-in
                        if (!customerList.isEmpty()) {
                            selectedCustomer = customerList.get(0);
                            spinnerCustomer.setText(customerList.get(0).toString(), false);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String err) { /* non-critical */ }
        });
    }

    // ── UI updates ────────────────────────────────────────────────────────────

    private void updateProductInfo() {
        if (selectedProduct == null) { cardProductInfo.setVisibility(View.GONE); return; }
        cardProductInfo.setVisibility(View.VISIBLE);
        tvAvailableStock.setText(String.valueOf(selectedProduct.getQuantity()));
        tvUnitPrice.setText(selectedProduct.getFormattedPrice());
        calculateTotal();
    }

    private void updateCustomerInfo() {
        if (selectedCustomer == null || !selectedCustomer.hasDue()) {
            cardCustomerCredit.setVisibility(View.GONE); return;
        }
        cardCustomerCredit.setVisibility(View.VISIBLE);
        tvCustomerDue.setText("Existing due: " + selectedCustomer.getFormattedDue());
    }

    private void calculateTotal() {
        if (selectedProduct == null) { tvTotalPrice.setText("₹0.00"); return; }
        String qs = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
        if (!qs.isEmpty()) {
            try {
                double total = Integer.parseInt(qs) * selectedProduct.getPrice();
                tvTotalPrice.setText(
                        NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(total));
                return;
            } catch (NumberFormatException ignored) {}
        }
        tvTotalPrice.setText("₹0.00");
    }

    // ── Sell ──────────────────────────────────────────────────────────────────

    private void performSell() {
        tilQuantity.setError(null); tilPaid.setError(null);

        if (selectedProduct == null) {
            Toast.makeText(requireContext(), "Select a product", Toast.LENGTH_SHORT).show(); return;
        }

        String qs = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
        if (qs.isEmpty()) { tilQuantity.setError("Required"); return; }

        int quantity;
        try { quantity = Integer.parseInt(qs); }
        catch (NumberFormatException e) { tilQuantity.setError("Invalid"); return; }
        if (quantity <= 0)                        { tilQuantity.setError("Must be > 0"); return; }
        if (quantity > selectedProduct.getQuantity()) {
            tilQuantity.setError("Only " + selectedProduct.getQuantity() + " available"); return;
        }

        double totalPrice = quantity * selectedProduct.getPrice();
        String ps = etPaidAmount.getText() != null ? etPaidAmount.getText().toString().trim() : "";
        double paidAmount = ps.isEmpty() ? totalPrice : Double.parseDouble(ps); // full if blank

        int customerId = selectedCustomer != null
                ? selectedCustomer.getId() : ApiConfig.WALK_IN_CUSTOMER_ID;
        String mode  = spinnerPayMode.getText().toString();
        String gst   = etGstNumber.getText() != null ? etGstNumber.getText().toString().trim() : "";

        showLoading(true);

        api.sellProduct(selectedProduct.getId(), customerId, quantity,
                paidAmount, mode, gst, new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject r) {
                        if (!isAdded()) return;
                        showLoading(false);
                        try {
                            if (r.getBoolean(ApiConfig.KEY_SUCCESS)) {
                                JSONObject data = r.optJSONObject(ApiConfig.KEY_DATA);
                                if (data != null) {
                                    lastSale = new Sale();
                                    lastSale.setSaleId(data.optInt("sale_id"));
                                    lastSale.setCompanyName(data.optString("company_name"));
                                    lastSale.setTireType(data.optString("tire_type"));
                                    lastSale.setTireSize(data.optString("tire_size"));
                                    lastSale.setQuantity(data.optInt("quantity_sold"));
                                    lastSale.setUnitPrice(data.optDouble("unit_price"));
                                    lastSale.setTotalPrice(data.optDouble("total_price"));
                                    lastSale.setRemainingStock(data.optInt("remaining_stock"));

                                    String remaining = data.optString("remaining_amount","0");
                                    generatePdfInBackground();
                                }

                                if (layoutInvoiceActions != null)
                                    layoutInvoiceActions.setVisibility(View.VISIBLE);

                                Snackbar.make(requireView(), "Sale recorded!", Snackbar.LENGTH_LONG)
                                        .setAction("Invoice", v -> openInvoice()).show();
                                clearForm();
                                loadProducts();
                                loadCustomers();
                            } else {
                                Toast.makeText(requireContext(),
                                        r.optString(ApiConfig.KEY_MESSAGE,"Sale failed"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onError(String err) {
                        if (!isAdded()) return;
                        showLoading(false);
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Invoice ───────────────────────────────────────────────────────────────

    private void generatePdfInBackground() {
        if (lastSale == null) return;
        String customer = etCustomerName.getText() != null
                ? etCustomerName.getText().toString().trim() : "";
        String gst = etGstNumber.getText() != null
                ? etGstNumber.getText().toString().trim() : "";
        new Thread(() -> {
            PdfGenerator gen = new PdfGenerator(requireContext(), lastSale)
                    .setCustomerName(customer).setGstNumber(gst);
            lastPdfFile = gen.generateInvoice();
        }).start();
    }

    private void openInvoice() {
        if (lastSale == null) return;
        if (lastPdfFile != null && lastPdfFile.exists()) {
            new PdfGenerator(requireContext(), lastSale).openPdf(lastPdfFile);
        } else {
            String c = etCustomerName.getText() != null ? etCustomerName.getText().toString() : "";
            String g = etGstNumber.getText()    != null ? etGstNumber.getText().toString()    : "";
            PdfGenerator gen = new PdfGenerator(requireContext(), lastSale)
                    .setCustomerName(c).setGstNumber(g);
            lastPdfFile = gen.generateInvoice();
            if (lastPdfFile != null) gen.openPdf(lastPdfFile);
        }
    }

    private void shareInvoice() {
        if (lastSale == null || lastPdfFile == null) return;
        new PdfGenerator(requireContext(), lastSale).sharePdf(lastPdfFile);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearForm() {
        spinnerProduct.setText("", false);
        etQuantity.setText("");
        etPaidAmount.setText("");
        tvTotalPrice.setText("₹0.00");
        cardProductInfo.setVisibility(View.GONE);
        cardCustomerCredit.setVisibility(View.GONE);
        selectedProduct = null;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSell.setEnabled(!show);
    }

    private Product parseProduct(JSONObject o) throws Exception {
        Product p = new Product();
        p.setId(o.getInt("id"));
        p.setCompanyId(o.optInt("company_id"));
        p.setCompanyName(o.getString("company_name"));
        p.setTireType(o.getString("tire_type"));
        p.setTireSize(o.getString("tire_size"));
        p.setQuantity(o.getInt("quantity"));
        p.setPrice(o.getDouble("price"));
        return p;
    }
}