// FILE: fragments/SellFragment.java (MODERN CART & CHECKOUT LOGIC)
package com.smarttire.inventory.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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

    // Track order summary for messaging
    private List<Product> currentOrderItems;
    private double currentOrderTotal, currentOrderPaid;

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
        cartAdapter = new CartAdapter(CartManager.getInstance().getItems(), new CartAdapter.OnCartListener() {
            @Override
            public void onQuantityChanged() {
                updateUI();
            }

            @Override
            public void onItemRemoved() {
                updateUI();
                cartAdapter.updateItems(CartManager.getInstance().getItems());
            }
        });
        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCart.setAdapter(cartAdapter);
        updateUI();
    }

    private void setupSearching() {
        // Customer Search
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
        });

        // Product Search
        spinnerProduct.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (s != null && s.length() >= 1) searchProducts(s.toString());
            }
        });

        spinnerProduct.setOnItemClickListener((parent, view, position, id) -> {
            selectedProduct = (Product) parent.getItemAtPosition(position);
        });
    }

    private void setupAmountCalculation() {
        etPaidAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingAmounts) return;
                isUpdatingAmounts = true;
                calculateRemaining();
                isUpdatingAmounts = false;
            }
        });

        etRemainingAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingAmounts) return;
                isUpdatingAmounts = true;
                calculatePaid();
                isUpdatingAmounts = false;
            }
        });
    }

    private void calculateRemaining() {
        double total = CartManager.getInstance().getTotal();
        double paid = 0;
        try {
            Editable text = etPaidAmount.getText();
            if (text != null) paid = Double.parseDouble(text.toString());
        } catch (Exception ignored) {}
        etRemainingAmount.setText(String.valueOf(Math.max(0, total - paid)));
    }

    private void calculatePaid() {
        double total = CartManager.getInstance().getTotal();
        double remaining = 0;
        try {
            Editable text = etRemainingAmount.getText();
            if (text != null) remaining = Double.parseDouble(text.toString());
        } catch (Exception ignored) {}
        etPaidAmount.setText(String.valueOf(Math.max(0, total - remaining)));
    }

    private void searchCustomers(String query) {
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

    private void searchProducts(String query) {
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
        p.setPrice(selectedProduct.getPrice());
        p.setCostPrice(selectedProduct.getCostPrice());
        p.setQuantity(selectedProduct.getQuantity());

        CartManager.getInstance().addItem(p);
        cartAdapter.updateItems(CartManager.getInstance().getItems());
        updateUI();
        spinnerProduct.setText("");
        selectedProduct = null;
    }

    private void updateUI() {
        double total = CartManager.getInstance().getTotal();
        tvTotalPrice.setText(NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(total));
        etPaidAmount.setText(String.valueOf((int)total));
        etRemainingAmount.setText("0");
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

        String payMode = spinnerPayMode.getText().toString();
        currentOrderTotal = CartManager.getInstance().getTotal();
        currentOrderPaid = 0;
        try {
            Editable text = etPaidAmount.getText();
            if (text != null) currentOrderPaid = Double.parseDouble(text.toString());
        } catch (Exception ignored) {}

        currentOrderItems = new ArrayList<>(CartManager.getInstance().getItems());
        processNextItem(currentOrderItems, 0, currentOrderPaid, payMode);
    }

    private void processNextItem(List<Product> items, int index, double remainingPaid, String payMode) {
        if (!isAdded()) return;

        if (index >= items.size()) {
            Toast.makeText(requireContext(), "Order Placed Successfully", Toast.LENGTH_SHORT).show();
            
            // Send SMS notification
            MessageHelper.sendOrderSms(requireContext(), selectedCustomer, items, currentOrderTotal, currentOrderPaid);

            CartManager.getInstance().clear();
            cartAdapter.updateItems(new ArrayList<>());
            updateUI();
            spinnerCustomer.setText("");
            selectedCustomer = null;
            return;
        }

        Product p = items.get(index);
        double itemTotal = p.getPrice() * p.getCartQuantity();
        double currentPaid = Math.min(remainingPaid, itemTotal);

        api.sellProduct(p.getId(), selectedCustomer.getId(), p.getCartQuantity(), currentPaid, payMode, "", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                if (response.optBoolean("success")) {
                    processNextItem(items, index + 1, Math.max(0, remainingPaid - currentPaid), payMode);
                } else {
                    Toast.makeText(requireContext(), "Error selling " + p.getDisplayName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
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
            void onQuantityChanged();
            void onItemRemoved();
        }

        private List<Product> items;
        private final OnCartListener listener;

        CartAdapter(List<Product> items, OnCartListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void updateItems(List<Product> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_cart, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Product p = items.get(pos);
            h.tvName.setText(p.getDisplayName());
            h.tvCost.setText(String.format("Cost: %s", p.getFormattedCostPrice()));
            h.tvSell.setText(String.format("Sell: %s", p.getFormattedPrice()));
            h.tvQty.setText(String.valueOf(p.getCartQuantity()));

            h.btnPlus.setOnClickListener(v -> {
                p.setCartQuantity(p.getCartQuantity() + 1);
                h.tvQty.setText(String.valueOf(p.getCartQuantity()));
                listener.onQuantityChanged();
            });

            h.btnMinus.setOnClickListener(v -> {
                if (p.getCartQuantity() > 1) {
                    p.setCartQuantity(p.getCartQuantity() - 1);
                    h.tvQty.setText(String.valueOf(p.getCartQuantity()));
                    listener.onQuantityChanged();
                }
            });

            h.btnRemove.setOnClickListener(v -> {
                CartManager.getInstance().removeItem(p.getId());
                listener.onItemRemoved();
            });

            h.btnEditPrice.setOnClickListener(v -> {
                AlertDialog.Builder b = new AlertDialog.Builder(v.getContext());
                b.setTitle("Edit Selling Price");
                
                final EditText input = new EditText(v.getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText(String.valueOf(p.getPrice()));
                b.setView(input);

                b.setPositiveButton("OK", (dialog, which) -> {
                    try {
                        double newPrice = Double.parseDouble(input.getText().toString());
                        p.setPrice(newPrice);
                        h.tvSell.setText(String.format("Sell: %s", p.getFormattedPrice()));
                        listener.onQuantityChanged();
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
                    }
                });
                b.setNegativeButton("Cancel", null);
                b.show();
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCost, tvSell, tvQty;
            ImageButton btnPlus, btnMinus, btnRemove, btnEditPrice;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvCost = v.findViewById(R.id.tvCostPrice);
                tvSell = v.findViewById(R.id.tvSellPrice);
                tvQty = v.findViewById(R.id.tvQuantity);
                btnPlus = v.findViewById(R.id.btnPlus);
                btnMinus = v.findViewById(R.id.btnMinus);
                btnRemove = v.findViewById(R.id.btnRemove);
                btnEditPrice = v.findViewById(R.id.btnEditPrice);
            }
        }
    }
}
