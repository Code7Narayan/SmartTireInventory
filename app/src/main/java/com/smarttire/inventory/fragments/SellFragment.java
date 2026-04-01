// FILE: app/src/main/java/com/smarttire/inventory/fragments/SellFragment.java  (REPLACE)
package com.smarttire.inventory.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

/**
 * Sell Product screen – upgraded with:
 *  • Optional customer name field
 *  • Optional GST number field
 *  • Snackbar confirmations
 *  • Proper invoice button visibility management
 */
public class SellFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────────
    private AutoCompleteTextView    spinnerProduct;
    private CardView                cardProductInfo;
    private TextView                tvAvailableStock, tvUnitPrice, tvTotalPrice;
    private TextInputLayout         tilQuantity;
    private TextInputEditText       etQuantity, etCustomerName, etGstNumber;
    private MaterialButton          btnSell, btnGenerateInvoice, btnShareInvoice;
    private LinearProgressIndicator progressBar;

    // ── State ─────────────────────────────────────────────────────────────────
    private ApiService       apiService;
    private List<Product>    productList   = new ArrayList<>();
    private Product          selectedProduct;
    private Sale             lastSale      = null;
    private File             lastPdfFile   = null;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static SellFragment newInstance() { return new SellFragment(); }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiService.getInstance(requireContext());
        initViews(view);
        setupListeners();
        loadProducts();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initViews(View v) {
        spinnerProduct      = v.findViewById(R.id.spinnerProduct);
        cardProductInfo     = v.findViewById(R.id.cardProductInfo);
        tvAvailableStock    = v.findViewById(R.id.tvAvailableStock);
        tvUnitPrice         = v.findViewById(R.id.tvUnitPrice);
        tvTotalPrice        = v.findViewById(R.id.tvTotalPrice);
        tilQuantity         = v.findViewById(R.id.tilQuantity);
        etQuantity          = v.findViewById(R.id.etQuantity);
        etCustomerName      = v.findViewById(R.id.etCustomerName);
        etGstNumber         = v.findViewById(R.id.etGstNumber);
        btnSell             = v.findViewById(R.id.btnSell);
        btnGenerateInvoice  = v.findViewById(R.id.btnGenerateInvoice);
        btnShareInvoice     = v.findViewById(R.id.btnShareInvoice);
        progressBar         = v.findViewById(R.id.progressBar);

        // Hidden until a sale succeeds
        if (btnGenerateInvoice != null) btnGenerateInvoice.setVisibility(View.GONE);
        if (btnShareInvoice    != null) btnShareInvoice.setVisibility(View.GONE);
    }

    private void setupListeners() {
        spinnerProduct.setOnItemClickListener((parent, view, pos, id) -> {
            selectedProduct = (Product) parent.getItemAtPosition(pos);
            updateProductInfo();
        });

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                calculateTotal();
            }
        });

        btnSell.setOnClickListener(v -> performSell());

        if (btnGenerateInvoice != null) {
            btnGenerateInvoice.setOnClickListener(v -> openInvoice());
        }
        if (btnShareInvoice != null) {
            btnShareInvoice.setOnClickListener(v -> shareInvoice());
        }
    }

    // ── Product loading ───────────────────────────────────────────────────────

    private void loadProducts() {
        showLoading(true);
        apiService.getProducts(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                showLoading(false);
                try {
                    if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                        JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                        productList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            productList.add(parseProduct(data.getJSONObject(i)));
                        }
                        setupProductSpinner();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupProductSpinner() {
        ArrayAdapter<Product> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, productList);
        spinnerProduct.setAdapter(adapter);
    }

    // ── UI updates ────────────────────────────────────────────────────────────

    private void updateProductInfo() {
        if (selectedProduct != null) {
            cardProductInfo.setVisibility(View.VISIBLE);
            tvAvailableStock.setText(String.valueOf(selectedProduct.getQuantity()));
            tvUnitPrice.setText(selectedProduct.getFormattedPrice());
            calculateTotal();
        } else {
            cardProductInfo.setVisibility(View.GONE);
        }
    }

    private void calculateTotal() {
        if (selectedProduct == null) { tvTotalPrice.setText("₹0.00"); return; }
        String qtyStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
        if (!qtyStr.isEmpty()) {
            try {
                int qty = Integer.parseInt(qtyStr);
                double total = qty * selectedProduct.getPrice();
                tvTotalPrice.setText(
                        NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(total));
                return;
            } catch (NumberFormatException ignored) {}
        }
        tvTotalPrice.setText("₹0.00");
    }

    // ── Sell action ───────────────────────────────────────────────────────────

    private void performSell() {
        tilQuantity.setError(null);

        if (selectedProduct == null) {
            Toast.makeText(requireContext(), "Please select a product", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etQuantity.getText() != null ?
                etQuantity.getText().toString().trim() : "";
        if (qtyStr.isEmpty()) { tilQuantity.setError("Quantity is required"); return; }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            tilQuantity.setError("Enter a valid number"); return;
        }

        if (quantity <= 0) { tilQuantity.setError("Quantity must be > 0"); return; }
        if (quantity > selectedProduct.getQuantity()) {
            tilQuantity.setError("Not enough stock. Available: " +
                    selectedProduct.getQuantity());
            return;
        }

        showLoading(true);

        double unitPrice  = selectedProduct.getPrice();
        double totalPrice = quantity * unitPrice;

        apiService.sellProduct(
                selectedProduct.getId(),
                selectedProduct.getCompanyName(),
                selectedProduct.getTireType(),
                selectedProduct.getTireSize(),
                quantity, unitPrice, totalPrice,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (!isAdded()) return;
                        showLoading(false);
                        try {
                            boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);
                            if (success) {
                                JSONObject data = response.optJSONObject(ApiConfig.KEY_DATA);
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
                                }

                                // Pre-generate PDF immediately for instant open/share
                                generatePdfInBackground();

                                // Show invoice buttons
                                if (btnGenerateInvoice != null)
                                    btnGenerateInvoice.setVisibility(View.VISIBLE);
                                if (btnShareInvoice != null)
                                    btnShareInvoice.setVisibility(View.VISIBLE);

                                // Snackbar with action
                                View root = requireView();
                                Snackbar.make(root, "Sale recorded successfully!",
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Open Invoice", v -> openInvoice())
                                        .show();

                                clearForm();
                                loadProducts(); // refresh stock quantities

                            } else {
                                Toast.makeText(requireContext(),
                                        response.optString(ApiConfig.KEY_MESSAGE, "Sale failed"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        showLoading(false);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Invoice actions ───────────────────────────────────────────────────────

    private void generatePdfInBackground() {
        if (lastSale == null) return;
        String customer = etCustomerName != null && etCustomerName.getText() != null
                ? etCustomerName.getText().toString().trim() : "";
        String gst = etGstNumber != null && etGstNumber.getText() != null
                ? etGstNumber.getText().toString().trim() : "";

        new Thread(() -> {
            PdfGenerator gen = new PdfGenerator(requireContext(), lastSale)
                    .setCustomerName(customer)
                    .setGstNumber(gst);
            lastPdfFile = gen.generateInvoice();
        }).start();
    }

    private void openInvoice() {
        if (lastSale == null) return;
        if (lastPdfFile != null && lastPdfFile.exists()) {
            new PdfGenerator(requireContext(), lastSale).openPdf(lastPdfFile);
        } else {
            // Regenerate if not ready yet
            String customer = etCustomerName != null && etCustomerName.getText() != null
                    ? etCustomerName.getText().toString().trim() : "";
            String gst = etGstNumber != null && etGstNumber.getText() != null
                    ? etGstNumber.getText().toString().trim() : "";
            PdfGenerator gen = new PdfGenerator(requireContext(), lastSale)
                    .setCustomerName(customer)
                    .setGstNumber(gst);
            lastPdfFile = gen.generateInvoice();
            if (lastPdfFile != null) gen.openPdf(lastPdfFile);
            else Toast.makeText(requireContext(), "PDF generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareInvoice() {
        if (lastSale == null || lastPdfFile == null) {
            Toast.makeText(requireContext(), "Generate invoice first", Toast.LENGTH_SHORT).show();
            return;
        }
        new PdfGenerator(requireContext(), lastSale).sharePdf(lastPdfFile);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearForm() {
        spinnerProduct.setText("", false);
        etQuantity.setText("");
        tvTotalPrice.setText("₹0.00");
        cardProductInfo.setVisibility(View.GONE);
        selectedProduct = null;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSell.setEnabled(!show);
        spinnerProduct.setEnabled(!show);
    }

    private Product parseProduct(JSONObject obj) throws Exception {
        Product p = new Product();
        p.setId(obj.getInt("id"));
        p.setCompanyId(obj.optInt("company_id"));
        p.setCompanyName(obj.getString("company_name"));
        p.setTireType(obj.getString("tire_type"));
        p.setTireSize(obj.getString("tire_size"));
        p.setQuantity(obj.getInt("quantity"));
        p.setPrice(obj.getDouble("price"));
        return p;
    }
}
