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

public class SellFragment extends Fragment {

    private AutoCompleteTextView spinnerProduct;
    private CardView cardProductInfo;
    private TextView tvAvailableStock, tvUnitPrice, tvTotalPrice;
    private TextInputLayout tilQuantity;
    private TextInputEditText etQuantity;
    private MaterialButton btnSell, btnGenerateInvoice;
    private LinearProgressIndicator progressBar;

    private ApiService apiService;
    private List<Product> productList = new ArrayList<>();
    private Product selectedProduct;

    // Holds the last completed sale for invoice generation
    private Sale lastSale = null;

    public SellFragment() {}

    public static SellFragment newInstance() {
        return new SellFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        apiService = ApiService.getInstance(requireContext());

        setupListeners();
        loadProducts();
    }

    private void initViews(View view) {
        spinnerProduct     = view.findViewById(R.id.spinnerProduct);
        cardProductInfo    = view.findViewById(R.id.cardProductInfo);
        tvAvailableStock   = view.findViewById(R.id.tvAvailableStock);
        tvUnitPrice        = view.findViewById(R.id.tvUnitPrice);
        tvTotalPrice       = view.findViewById(R.id.tvTotalPrice);
        tilQuantity        = view.findViewById(R.id.tilQuantity);
        etQuantity         = view.findViewById(R.id.etQuantity);
        btnSell            = view.findViewById(R.id.btnSell);
        btnGenerateInvoice = view.findViewById(R.id.btnGenerateInvoice);
        progressBar        = view.findViewById(R.id.progressBar);

        // Invoice button hidden until a sale completes
        if (btnGenerateInvoice != null) {
            btnGenerateInvoice.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        spinnerProduct.setOnItemClickListener((parent, view, position, id) -> {
            selectedProduct = (Product) parent.getItemAtPosition(position);
            updateProductInfo();
        });

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSell.setOnClickListener(v -> performSell());

        // ── FIX: btnGenerateInvoice now has a click listener ──────────────
        if (btnGenerateInvoice != null) {
            btnGenerateInvoice.setOnClickListener(v -> {
                if (lastSale != null) {
                    try {
                        PdfGenerator generator = new PdfGenerator(requireContext(), lastSale);
                        File pdfFile = generator.generateInvoice();
                        if (pdfFile != null) {
                            generator.openPdf(pdfFile);
                        } else {
                            Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(),
                                "Could not generate invoice: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "No sale to generate invoice for", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadProducts() {
        showLoading(true);
        apiService.getProducts(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    showLoading(false);
                    try {
                        if (response.getBoolean(ApiConfig.KEY_SUCCESS)) {
                            JSONArray data = response.getJSONArray(ApiConfig.KEY_DATA);
                            productList.clear();
                            for (int i = 0; i < data.length(); i++) {
                                productList.add(parseProduct(data.getJSONObject(i)));
                            }
                            setupProductSpinner();
                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString(ApiConfig.KEY_MESSAGE, "Failed to load products"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    showLoading(false);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupProductSpinner() {
        ArrayAdapter<Product> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, productList);
        spinnerProduct.setAdapter(adapter);
    }

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
        if (selectedProduct != null) {
            String qtyStr = etQuantity.getText().toString().trim();
            if (!qtyStr.isEmpty()) {
                try {
                    int qty = Integer.parseInt(qtyStr);
                    double total = qty * selectedProduct.getPrice();
                    NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                    tvTotalPrice.setText(fmt.format(total));
                    return;
                } catch (NumberFormatException ignored) {}
            }
        }
        tvTotalPrice.setText("₹0.00");
    }

    private void performSell() {
        tilQuantity.setError(null);

        if (selectedProduct == null) {
            Toast.makeText(requireContext(), "Please select a product", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            tilQuantity.setError("Quantity is required");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            tilQuantity.setError("Enter a valid number");
            return;
        }

        if (quantity <= 0) {
            tilQuantity.setError("Quantity must be greater than 0");
            return;
        }

        if (quantity > selectedProduct.getQuantity()) {
            tilQuantity.setError("Not enough stock. Available: " + selectedProduct.getQuantity());
            return;
        }

        showLoading(true);

        double unitPrice = selectedProduct.getPrice();
        double totalPrice = quantity * unitPrice;

        apiService.sellProduct(
                selectedProduct.getId(),
                selectedProduct.getCompanyName(),
                selectedProduct.getTireType(),
                selectedProduct.getTireSize(),
                quantity,
                unitPrice,
                totalPrice,
                new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    showLoading(false);
                    try {
                        boolean success = response.getBoolean(ApiConfig.KEY_SUCCESS);
                        String  message = response.optString(ApiConfig.KEY_MESSAGE, "");

                        if (success) {
                            Toast.makeText(requireContext(),
                                    "Sale successful!", Toast.LENGTH_SHORT).show();

                            // Build Sale object for invoice
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

                                // Show invoice button after a successful sale
                                if (btnGenerateInvoice != null) {
                                    btnGenerateInvoice.setVisibility(View.VISIBLE);
                                }
                            }

                            clearForm();
                            loadProducts(); // Refresh stock quantities
                        } else {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(),
                                "Error reading response", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    showLoading(false);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

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
        Product product = new Product();
        product.setId(obj.getInt("id"));
        product.setCompanyId(obj.optInt("company_id"));
        product.setCompanyName(obj.getString("company_name"));
        product.setTireType(obj.getString("tire_type"));
        product.setTireSize(obj.getString("tire_size"));
        product.setQuantity(obj.getInt("quantity"));
        product.setPrice(obj.getDouble("price"));
        return product;
    }
}
