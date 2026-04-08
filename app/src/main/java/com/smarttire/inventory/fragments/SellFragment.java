// FILE: fragments/SellFragment.java  (UPDATED — selling price field + compact UI)
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

    private AutoCompleteTextView    spinnerProduct, spinnerCustomer, spinnerPayMode;
    private CardView                cardProductInfo, cardCustomerCredit;
    private TextView                tvAvailableStock, tvUnitPrice, tvTotalPrice, tvRemainingAmount;
    private TextView                tvCustomerDue;
    private TextInputLayout         tilQuantity, tilPaid, tilSellingPrice;
    private TextInputEditText       etQuantity, etPaidAmount, etGstNumber, etSellingPrice;
    private MaterialButton          btnSell, btnAddCustomer, btnGenerateInvoice, btnShareInvoice;
    private LinearProgressIndicator progressBar;
    private LinearLayout            layoutInvoiceActions;

    private ApiService     api;
    private List<Product>  productList  = new ArrayList<>();
    private List<Customer> customerList = new ArrayList<>();
    private Product        selectedProduct;
    private Customer       selectedCustomer;
    private Sale           lastSale;
    private File           lastPdfFile;

    private int    preselectProductId = -1;

    public static SellFragment newInstance() { return new SellFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup g, Bundle s) {
        return i.inflate(R.layout.fragment_sell, g, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiService.getInstance(requireContext());
        if (getActivity() != null)
            preselectProductId = getActivity().getIntent().getIntExtra("preselect_product_id", -1);

        initViews(view);
        setupListeners();
        loadProducts();
        loadCustomers();
    }

    private void initViews(View v) {
        spinnerProduct     = v.findViewById(R.id.spinnerProduct);
        spinnerCustomer    = v.findViewById(R.id.spinnerCustomer);
        spinnerPayMode     = v.findViewById(R.id.spinnerPaymentMode);
        cardProductInfo    = v.findViewById(R.id.cardProductInfo);
        cardCustomerCredit = v.findViewById(R.id.cardCustomerCredit);
        tvAvailableStock   = v.findViewById(R.id.tvAvailableStock);
        tvUnitPrice        = v.findViewById(R.id.tvUnitPrice);
        tvTotalPrice       = v.findViewById(R.id.tvTotalPrice);
        tvRemainingAmount  = v.findViewById(R.id.tvRemainingAmount);
        tvCustomerDue      = v.findViewById(R.id.tvCustomerDue);
        tilQuantity        = v.findViewById(R.id.tilQuantity);
        tilPaid            = v.findViewById(R.id.tilPaid);
        tilSellingPrice    = v.findViewById(R.id.tilSellingPrice);
        etQuantity         = v.findViewById(R.id.etQuantity);
        etPaidAmount       = v.findViewById(R.id.etPaidAmount);
        etGstNumber        = v.findViewById(R.id.etGstNumber);
        etSellingPrice     = v.findViewById(R.id.etSellingPrice);
        btnSell            = v.findViewById(R.id.btnSell);
        btnAddCustomer     = v.findViewById(R.id.btnAddCustomer);
        btnGenerateInvoice = v.findViewById(R.id.btnGenerateInvoice);
        btnShareInvoice    = v.findViewById(R.id.btnShareInvoice);
        progressBar        = v.findViewById(R.id.progressBar);
        layoutInvoiceActions = v.findViewById(R.id.layoutInvoiceActions);

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
        etQuantity.addTextChangedListener(simpleWatcher(() -> { calculateTotal(); validatePaid(); }));
        // Selling price change → recalculate total
        etSellingPrice.addTextChangedListener(simpleWatcher(() -> { calculateTotal(); validatePaid(); }));
        etPaidAmount.addTextChangedListener(simpleWatcher(this::validatePaid));

        btnAddCustomer.setOnClickListener(v -> startActivity(
                new android.content.Intent(requireContext(),
                        com.smarttire.inventory.activities.AddCustomerActivity.class)));
        btnSell.setOnClickListener(v -> performSell());
        if (btnGenerateInvoice != null) btnGenerateInvoice.setOnClickListener(v -> openInvoice());
        if (btnShareInvoice    != null) btnShareInvoice.setOnClickListener(v -> shareInvoice());
    }

    // ── Load ──────────────────────────────────────────────────────────────────

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
                        if (preselectProductId > 0) {
                            for (int i = 0; i < productList.size(); i++) {
                                if (productList.get(i).getId() == preselectProductId) {
                                    selectedProduct = productList.get(i);
                                    spinnerProduct.setText(selectedProduct.toString(), false);
                                    updateProductInfo();
                                    preselectProductId = -1;
                                    break;
                                }
                            }
                        }
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
                        if (!customerList.isEmpty()) {
                            selectedCustomer = customerList.get(0);
                            spinnerCustomer.setText(customerList.get(0).toString(), false);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(String err) {}
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updateProductInfo() {
        if (selectedProduct == null) { cardProductInfo.setVisibility(View.GONE); return; }
        cardProductInfo.setVisibility(View.VISIBLE);
        tvAvailableStock.setText(String.valueOf(selectedProduct.getQuantity()));
        tvUnitPrice.setText(inr(selectedProduct.getPrice()));
        // Pre-fill selling price with base price
        if (txt(etSellingPrice).isEmpty()) {
            etSellingPrice.setText(String.valueOf((int) selectedProduct.getPrice()));
        }
        calculateTotal();
    }

    private void updateCustomerInfo() {
        if (selectedCustomer == null || !selectedCustomer.hasDue()) {
            if (tvCustomerDue != null) tvCustomerDue.setVisibility(View.GONE); return;
        }
        if (tvCustomerDue != null) {
            tvCustomerDue.setVisibility(View.VISIBLE);
            tvCustomerDue.setText("Due: " + selectedCustomer.getFormattedDue());
        }
    }

    /** Uses selling price if set, otherwise falls back to base price */
    private double getEffectivePrice() {
        String sp = txt(etSellingPrice);
        if (!sp.isEmpty()) {
            try { return Double.parseDouble(sp); } catch (NumberFormatException ignored) {}
        }
        return selectedProduct != null ? selectedProduct.getPrice() : 0;
    }

    private void calculateTotal() {
        if (selectedProduct == null) { tvTotalPrice.setText("₹0.00"); return; }
        String qs = txt(etQuantity);
        if (!qs.isEmpty()) {
            try {
                double price = getEffectivePrice();
                tvTotalPrice.setText(inr(Integer.parseInt(qs) * price));
                return;
            } catch (NumberFormatException ignored) {}
        }
        tvTotalPrice.setText("₹0.00");
    }

    private void validatePaid() {
        if (selectedProduct == null || tilPaid == null) return;
        String qs = txt(etQuantity);
        if (qs.isEmpty()) return;
        double total;
        try { total = Integer.parseInt(qs) * getEffectivePrice(); }
        catch (NumberFormatException e) { return; }

        String ps = txt(etPaidAmount);
        if (ps.isEmpty()) {
            tilPaid.setError(null);
            if (tvRemainingAmount != null) tvRemainingAmount.setText("Remaining: ₹0.00 (Full)");
            return;
        }
        double paid;
        try { paid = Double.parseDouble(ps); }
        catch (NumberFormatException e) { tilPaid.setError("Invalid"); return; }

        if (paid > total + 0.005) {
            tilPaid.setError("Cannot exceed total " + inr(total));
            if (tvRemainingAmount != null) tvRemainingAmount.setText("Remaining: —");
        } else {
            tilPaid.setError(null);
            if (tvRemainingAmount != null) tvRemainingAmount.setText("Remaining: " + inr(total - paid));
        }
    }

    // ── Sell ──────────────────────────────────────────────────────────────────

    private void performSell() {
        if (tilQuantity != null) tilQuantity.setError(null);
        if (tilPaid     != null) tilPaid.setError(null);
        if (tilSellingPrice != null) tilSellingPrice.setError(null);

        if (selectedProduct == null) {
            Toast.makeText(requireContext(), "Select a product", Toast.LENGTH_SHORT).show(); return;
        }

        String qs = txt(etQuantity);
        if (qs.isEmpty()) { if(tilQuantity!=null) tilQuantity.setError("Required"); return; }

        int qty;
        try { qty = Integer.parseInt(qs); }
        catch (NumberFormatException e) { if(tilQuantity!=null) tilQuantity.setError("Invalid"); return; }

        if (qty <= 0) { if(tilQuantity!=null) tilQuantity.setError("Must be > 0"); return; }
        if (qty > selectedProduct.getQuantity()) {
            if(tilQuantity!=null) tilQuantity.setError("Only " + selectedProduct.getQuantity() + " available");
            return;
        }

        // Selling price (admin-entered, falls back to base price)
        double sellingPrice = getEffectivePrice();
        if (sellingPrice <= 0) {
            if(tilSellingPrice!=null) tilSellingPrice.setError("Enter selling price");
            return;
        }

        double total = qty * sellingPrice;
        String ps    = txt(etPaidAmount);
        double paid;

        if (ps.isEmpty()) {
            paid = total;
        } else {
            try { paid = Double.parseDouble(ps); }
            catch (NumberFormatException e) {
                if(tilPaid!=null) tilPaid.setError("Invalid"); return;
            }
            if (paid < 0) { if(tilPaid!=null) tilPaid.setError("Cannot be negative"); return; }
            if (paid > total + 0.005) {
                if(tilPaid!=null) tilPaid.setError("Cannot exceed " + inr(total)); return;
            }
        }

        int    custId = selectedCustomer != null ? selectedCustomer.getId() : ApiConfig.WALK_IN_CUSTOMER_ID;
        String mode   = spinnerPayMode.getText().toString();
        String gst    = txt(etGstNumber);

        // Temporarily override product price for this sale if selling price differs
        double originalPrice = selectedProduct.getPrice();
        if (Math.abs(sellingPrice - originalPrice) > 0.01) {
            selectedProduct.setPrice(sellingPrice);
        }

        showLoading(true);
        api.sellProduct(selectedProduct.getId(), custId, qty, paid, mode, gst,
                new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject r) {
                        if (!isAdded()) return;
                        showLoading(false);
                        // Restore original price
                        selectedProduct.setPrice(originalPrice);
                        try {
                            if (r.getBoolean(ApiConfig.KEY_SUCCESS)) {
                                JSONObject d = r.optJSONObject(ApiConfig.KEY_DATA);
                                if (d != null) {
                                    lastSale = new Sale();
                                    lastSale.setSaleId(d.optInt("sale_id"));
                                    lastSale.setCompanyName(d.optString("company_name"));
                                    lastSale.setTireType(d.optString("tire_type"));
                                    lastSale.setTireSize(d.optString("tire_size"));
                                    lastSale.setQuantity(d.optInt("quantity_sold"));
                                    lastSale.setUnitPrice(sellingPrice);  // use selling price
                                    lastSale.setTotalPrice(total);
                                    lastSale.setRemainingStock(d.optInt("remaining_stock"));
                                    lastSale.setPaidAmount(d.optDouble("paid_amount", paid));
                                    lastSale.setRemainingAmount(d.optDouble("remaining_amount", total - paid));
                                    lastSale.setPaymentStatus(d.optString("payment_status", "paid"));
                                    String custName = selectedCustomer != null ? selectedCustomer.getName() : "";
                                    lastSale.setCustomerName(custName);
                                    generatePdfBackground(gst, custName);
                                }
                                if (layoutInvoiceActions != null) layoutInvoiceActions.setVisibility(View.VISIBLE);
                                Snackbar.make(requireView(), "Sale recorded!", Snackbar.LENGTH_LONG)
                                        .setAction("Invoice", v -> openInvoice()).show();
                                clearForm();
                                loadProducts();
                                loadCustomers();
                            } else {
                                Toast.makeText(requireContext(),
                                        r.optString(ApiConfig.KEY_MESSAGE,"Sale failed"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onError(String err) {
                        if (!isAdded()) return;
                        showLoading(false);
                        selectedProduct.setPrice(originalPrice);
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private void generatePdfBackground(String gst, String custName) {
        if (lastSale == null) return;
        new Thread(() -> {
            lastPdfFile = new PdfGenerator(requireContext(), lastSale)
                    .setCustomerName(custName).setGstNumber(gst).generateInvoice();
        }).start();
    }

    private void openInvoice() {
        if (lastSale == null) return;
        if (lastPdfFile != null && lastPdfFile.exists()) {
            new PdfGenerator(requireContext(), lastSale).openPdf(lastPdfFile);
        } else {
            PdfGenerator gen = new PdfGenerator(requireContext(), lastSale)
                    .setCustomerName(lastSale.getCustomerName()).setGstNumber(txt(etGstNumber));
            lastPdfFile = gen.generateInvoice();
            if (lastPdfFile != null) gen.openPdf(lastPdfFile);
        }
    }

    private void shareInvoice() {
        if (lastSale == null || lastPdfFile == null) return;
        new PdfGenerator(requireContext(), lastSale).sharePdf(lastPdfFile);
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void clearForm() {
        spinnerProduct.setText("", false);
        etQuantity.setText(""); etPaidAmount.setText(""); etSellingPrice.setText("");
        tvTotalPrice.setText("₹0.00");
        if (tvRemainingAmount != null) tvRemainingAmount.setText("Remaining: ₹0.00");
        cardProductInfo.setVisibility(View.GONE);
        if (cardCustomerCredit != null) cardCustomerCredit.setVisibility(View.GONE);
        if (tvCustomerDue != null) tvCustomerDue.setVisibility(View.GONE);
        if (tilPaid != null) tilPaid.setError(null);
        selectedProduct = null;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSell.setEnabled(!show);
    }

    private String txt(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }

    private String inr(double v) {
        return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(v);
    }

    private Product parseProduct(JSONObject o) throws Exception {
        Product p = new Product();
        p.setId(o.getInt("id"));
        p.setCompanyId(o.optInt("company_id"));
        p.setCompanyName(o.getString("company_name"));
        p.setTireType(o.getString("tire_type"));
        p.setTireSize(o.optString("tire_size",""));
        p.setQuantity(o.getInt("quantity"));
        p.setPrice(o.getDouble("price"));
        p.setModelName(o.optString("model_name",""));
        return p;
    }

    private TextWatcher simpleWatcher(Runnable action) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){ action.run(); }
        };
    }
}