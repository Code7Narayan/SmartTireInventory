// FILE: fragments/SellFragment.java (ULTRA STABILITY FIX - Realme/Oppo Focus Fix)
package com.smarttire.inventory.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarttire.inventory.R;
import com.smarttire.inventory.activities.AddCustomerActivity;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiConfig;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.CartManager;
import com.smarttire.inventory.utils.KeyboardUtils;
import com.smarttire.inventory.utils.MessageHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellFragment extends Fragment {

    private AutoCompleteTextView spinnerCustomer, spinnerProduct, spinnerPayMode;
    private TextView tvTotalPrice, tvCustomerDue;
    private TextInputEditText etPaidAmount, etRemainingAmount;
    private RecyclerView rvCart;
    private MaterialButton btnAddProduct, btnPlaceOrder, btnAddNewCustomer;

    private ApiService api;
    private final List<Product> productList = new ArrayList<>();
    private final List<Customer> customerList = new ArrayList<>();
    private Product selectedProduct;
    private Customer selectedCustomer;
    
    private CartAdapter cartAdapter;
    private boolean isUpdatingAmounts = false;

    private List<Product> currentOrderItems;
    private double currentOrderTotal, currentOrderPaid;

    // Handler to debounce total calculation and avoid layout jitters while typing
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = this::performUpdateUI;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiService.getInstance(requireContext());
        initViews(view);
        setupCart();
        setupSearching();
        setupAmountCalculation();
        
        btnAddProduct.setOnClickListener(v -> addToCart());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
        btnAddNewCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddCustomerActivity.class);
            startActivity(intent);
        });
    }

    private void initViews(View v) {
        spinnerCustomer = v.findViewById(R.id.spinnerCustomer);
        spinnerProduct = v.findViewById(R.id.spinnerProduct);
        spinnerPayMode = v.findViewById(R.id.spinnerPaymentMode);
        tvTotalPrice = v.findViewById(R.id.tvTotalPrice);
        tvCustomerDue = v.findViewById(R.id.tvCustomerDue);
        etPaidAmount = v.findViewById(R.id.etPaidAmount);
        etRemainingAmount = v.findViewById(R.id.etRemainingAmount);
        rvCart = v.findViewById(R.id.rvCart);
        btnAddProduct = v.findViewById(R.id.btnAddProduct);
        btnPlaceOrder = v.findViewById(R.id.btnPlaceOrder);
        btnAddNewCustomer = v.findViewById(R.id.btnAddNewCustomer);

        String[] modes = {"Cash", "UPI", "Card", "Credit"};
        spinnerPayMode.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, modes));
    }

    private void setupCart() {
        cartAdapter = new CartAdapter(new CartAdapter.OnCartListener() {
            @Override
            public void onCartUpdated() {
                // Use debounce to prevent RecyclerView from re-binding while user is typing
                updateHandler.removeCallbacks(updateRunnable);
                updateHandler.postDelayed(updateRunnable, 100); 
            }

            @Override
            public void onItemRemoved() {
                performUpdateUI();
                cartAdapter.updateItems(CartManager.getInstance().getItems());
            }
        });
        
        cartAdapter.updateItems(CartManager.getInstance().getItems());
        
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        rvCart.setLayoutManager(lm);
        rvCart.setItemAnimator(null); // CRITICAL: Disable animations to prevent view detachment on Realme
        rvCart.setHasFixedSize(true);
        rvCart.setAdapter(cartAdapter);
        performUpdateUI();
    }

    private void setupSearching() {
        spinnerCustomer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (s != null && s.length() >= 1) searchCustomers(s.toString());
            }
        });

        spinnerCustomer.setOnItemClickListener((parent, view, position, id) -> {
            selectedCustomer = (Customer) parent.getItemAtPosition(position);
            if(selectedCustomer != null && selectedCustomer.hasDue()){
                tvCustomerDue.setVisibility(View.VISIBLE);
                tvCustomerDue.setText("Outstanding: " + selectedCustomer.getFormattedDue());
            } else {
                tvCustomerDue.setVisibility(View.GONE);
            }
            KeyboardUtils.hideKeyboard(requireActivity());
        });

        spinnerProduct.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (s != null && s.length() >= 1) searchProducts(s.toString());
            }
        });

        spinnerProduct.setOnItemClickListener((parent, view, position, id) -> {
            selectedProduct = (Product) parent.getItemAtPosition(position);
            KeyboardUtils.hideKeyboard(requireActivity());
        });
    }

    private void setupAmountCalculation() {
        etPaidAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingAmounts) return;
                calculateRemaining();
            }
        });
    }

    private void calculateRemaining() {
        double total = CartManager.getInstance().getTotal();
        double paid = 0;
        try {
            String text = etPaidAmount.getText().toString();
            if (!text.isEmpty()) paid = Double.parseDouble(text);
        } catch (Exception ignored) {}
        etRemainingAmount.setText(String.format(Locale.getDefault(), "%.2f", Math.max(0, total - paid)));
    }

    private void searchCustomers(final String query) {
        api.getCustomers(query, 1, new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject r) {
                try {
                    JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                    customerList.clear();
                    for(int i=0; i<data.length(); i++) customerList.add(Customer.fromJSON(data.getJSONObject(i)));
                    
                    if (isAdded() && getContext() != null) {
                        ArrayAdapter<Customer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, customerList);
                        spinnerCustomer.setAdapter(adapter);
                        if (!query.isEmpty()) adapter.getFilter().filter(query);
                        spinnerCustomer.showDropDown();
                    }
                } catch(Exception ignored){}
            }
            @Override public void onError(String e){}
        });
    }

    private void searchProducts(final String query) {
        api.getProducts(new ApiService.ApiCallback() {
            @Override public void onSuccess(JSONObject r) {
                try {
                    JSONArray data = r.getJSONArray(ApiConfig.KEY_DATA);
                    productList.clear();
                    for(int i=0; i<data.length(); i++) {
                        Product p = parseProduct(data.getJSONObject(i));
                        if (p.getDisplayName().toLowerCase().contains(query.toLowerCase())) {
                            productList.add(p);
                        }
                    }
                    if (isAdded() && getContext() != null) {
                        ArrayAdapter<Product> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, productList);
                        spinnerProduct.setAdapter(adapter);
                        if (!query.isEmpty()) adapter.getFilter().filter(query);
                        spinnerProduct.showDropDown();
                    }
                } catch(Exception ignored){}
            }
            @Override public void onError(String e){}
        });
    }

    private void addToCart() {
        if(selectedProduct == null) return;
        
        Product p = new Product();
        p.setId(selectedProduct.getId());
        p.setCompanyName(selectedProduct.getCompanyName());
        p.setModelName(selectedProduct.getModelName());
        p.setTireSize(selectedProduct.getTireSize());
        p.setTireType(selectedProduct.getTireType());
        // Default selling price is set to cost price as per current workflow
        p.setPrice(selectedProduct.getCostPrice()); 
        p.setCostPrice(selectedProduct.getCostPrice());
        p.setQuantity(selectedProduct.getQuantity());

        CartManager.getInstance().addItem(p);
        cartAdapter.updateItems(CartManager.getInstance().getItems());
        performUpdateUI();
        spinnerProduct.setText("");
        selectedProduct = null;
        KeyboardUtils.hideKeyboard(requireActivity());
    }

    private void performUpdateUI() {
        if (isUpdatingAmounts) return;
        isUpdatingAmounts = true;
        
        double total = CartManager.getInstance().getTotal();
        tvTotalPrice.setText(NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(total));
        
        if (!etPaidAmount.hasFocus()) {
            etPaidAmount.setText(String.format(Locale.getDefault(), "%.2f", total));
            etRemainingAmount.setText("0.00");
        } else {
            calculateRemaining();
        }
        isUpdatingAmounts = false;
    }

    private void placeOrder() {
        if(selectedCustomer == null) {
            Toast.makeText(requireContext(), "Please select a customer", Toast.LENGTH_SHORT).show();
            return;
        }
        if(CartManager.getInstance().getItems().isEmpty()){
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        for (Product p : CartManager.getInstance().getItems()) {
            if (p.getPrice() <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid amount for " + p.getDisplayName(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String payMode = spinnerPayMode.getText().toString();
        currentOrderTotal = CartManager.getInstance().getTotal();
        currentOrderPaid = 0;
        try {
            String text = etPaidAmount.getText().toString();
            if (!text.isEmpty()) currentOrderPaid = Double.parseDouble(text);
        } catch (Exception ignored) {}

        currentOrderItems = new ArrayList<>(CartManager.getInstance().getItems());
        KeyboardUtils.hideKeyboard(requireActivity());
        processNextItem(currentOrderItems, 0, currentOrderPaid, payMode);
    }

    private void processNextItem(final List<Product> items, final int index, final double remainingPaid, final String payMode) {
        if (!isAdded()) return;

        if (index >= items.size()) {
            Toast.makeText(requireContext(), "Order Placed Successfully", Toast.LENGTH_SHORT).show();
            MessageHelper.sendOrderSms(requireContext(), selectedCustomer, items, currentOrderTotal, currentOrderPaid);
            CartManager.getInstance().clear();
            cartAdapter.updateItems(new ArrayList<>());
            performUpdateUI();
            spinnerCustomer.setText("");
            selectedCustomer = null;
            return;
        }

        final Product p = items.get(index);
        double itemTotal = p.getPrice() * p.getCartQuantity();
        
        // Distribute paid amount among items correctly
        final double itemPaidAmount = Math.min(remainingPaid, itemTotal);

        // Send 'itemPaidAmount' as the 'paid_amount' for this specific item/sale entry
        api.sellProduct(p.getId(), selectedCustomer.getId(), p.getCartQuantity(), p.getPrice(), itemPaidAmount, payMode, "", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                if (response.optBoolean("success")) {
                    // Update remainingPaid by subtracting what was already allocated to this item
                    double newlyRemaining = Math.max(0, remainingPaid - itemPaidAmount);
                    processNextItem(items, index + 1, newlyRemaining, payMode);
                } else {
                    Toast.makeText(requireContext(), "Error selling " + p.getDisplayName() + ": " + response.optString("message"), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Server error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Product parseProduct(JSONObject o) throws Exception {
        Product p = new Product();
        p.setId(o.getInt("id"));
        p.setCompanyName(o.getString("company_name"));
        p.setModelName(o.optString("model_name",""));
        p.setTireSize(o.optString("tire_size",""));
        p.setPrice(o.getDouble("price"));
        p.setCostPrice(o.optDouble("cost_price", 0.0));
        p.setQuantity(o.getInt("quantity"));
        return p;
    }

    private static class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
        interface OnCartListener {
            void onCartUpdated();
            void onItemRemoved();
        }

        private List<Product> items = new ArrayList<>();
        private final OnCartListener listener;

        CartAdapter(OnCartListener listener) {
            this.listener = listener;
        }

        void updateItems(List<Product> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_cart, p, false), listener);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Product p = items.get(pos);
            h.bind(p);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvQty;
            EditText etCostPrice;
            ImageButton btnPlus, btnMinus, btnRemove;
            Product boundProduct;
            boolean isBinding = false;

            VH(View v, final OnCartListener listener) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                etCostPrice = v.findViewById(R.id.etItemCostPrice);
                tvQty = v.findViewById(R.id.tvQuantity);
                btnPlus = v.findViewById(R.id.btnPlus);
                btnMinus = v.findViewById(R.id.btnMinus);
                btnRemove = v.findViewById(R.id.btnRemove);

                etCostPrice.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                    @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                    @Override public void afterTextChanged(Editable s) {
                        if (isBinding || boundProduct == null) return;
                        try {
                            String input = s.toString();
                            double val = input.isEmpty() ? 0 : Double.parseDouble(input);
                            boundProduct.setPrice(val);
                        } catch (Exception e) {
                            boundProduct.setPrice(0);
                        }
                        listener.onCartUpdated();
                    }
                });

                btnPlus.setOnClickListener(view -> {
                    if (boundProduct != null) {
                        boundProduct.setCartQuantity(boundProduct.getCartQuantity() + 1);
                        tvQty.setText(String.valueOf(boundProduct.getCartQuantity()));
                        listener.onCartUpdated();
                    }
                });

                btnMinus.setOnClickListener(view -> {
                    if (boundProduct != null && boundProduct.getCartQuantity() > 1) {
                        boundProduct.setCartQuantity(boundProduct.getCartQuantity() - 1);
                        tvQty.setText(String.valueOf(boundProduct.getCartQuantity()));
                        listener.onCartUpdated();
                    }
                });

                btnRemove.setOnClickListener(view -> {
                    if (boundProduct != null) {
                        CartManager.getInstance().removeItem(boundProduct.getId());
                        listener.onItemRemoved();
                    }
                });
            }

            void bind(Product p) {
                this.isBinding = true;
                this.boundProduct = p;
                tvName.setText(p.getDisplayName());
                tvQty.setText(String.valueOf(p.getCartQuantity()));
                
                String priceStr = String.valueOf(p.getPrice());
                if (!etCostPrice.getText().toString().equals(priceStr)) {
                    etCostPrice.setText(priceStr);
                }
                this.isBinding = false;
            }
        }
    }
}