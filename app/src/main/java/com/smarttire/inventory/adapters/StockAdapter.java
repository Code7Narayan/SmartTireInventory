// FILE: adapters/StockAdapter.java  (REFACTORED — Clean UI, Add to Cart, Edit Price/Qty)
package com.smarttire.inventory.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiService;
import com.smarttire.inventory.utils.CartManager;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    public interface OnItemClickListener   { void onItemClick(Product product); }
    public interface OnStockChangedListener { void onStockChanged(); }

    private final Context       ctx;
    private final List<Product> productList;
    private final List<Product> productListFull;
    private       OnItemClickListener    clickListener;
    private       OnStockChangedListener changeListener;

    public StockAdapter(Context ctx, List<Product> list) {
        this.ctx             = ctx;
        this.productList     = list;
        this.productListFull = new ArrayList<>(list);
    }

    public void setOnItemClickListener(OnItemClickListener l)     { this.clickListener  = l; }
    public void setOnStockChangedListener(OnStockChangedListener l){ this.changeListener = l; }

    @NonNull @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder h, int position) {
        Product product = productList.get(position);
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        h.tvModelName.setText(product.getModelName().isEmpty() ? product.getTireType() : product.getModelName());
        h.tvCompanyName.setText(product.getCompanyName() + " | " + product.getTireSize());

        // Price & Stock
        h.tvPrice.setText(fmt.format(product.getPrice()));
        h.tvQuantity.setText("Stock: " + product.getQuantity());
        h.tvCurrentQty.setText(String.valueOf(product.getQuantity()));
        
        // Cost Price
        if (h.tvCostPrice != null) {
            h.tvCostPrice.setText("Cost: " + fmt.format(product.getCostPrice()));
        }

        // Color coding for low stock
        int stockColor = product.isLowStock() ? R.color.error : R.color.text_hint;
        h.tvQuantity.setTextColor(ContextCompat.getColor(ctx, stockColor));
        h.ivLowStock.setVisibility(product.isLowStock() ? View.VISIBLE : View.GONE);

        // ── Plus Button Action ───────────────────────────────────────────────
        h.btnPlus.setOnClickListener(v -> {
            updateQuantityOnServer(product, 1, h);
        });

        // ── Minus Button Action ──────────────────────────────────────────────
        h.btnMinus.setOnClickListener(v -> {
            if (product.getQuantity() > 0) {
                updateQuantityOnServer(product, -1, h);
            } else {
                Toast.makeText(ctx, "Stock cannot be negative", Toast.LENGTH_SHORT).show();
            }
        });

        // ── Cart Action ───────────────────────────────────────────────────────
        h.btnAddToCart.setOnClickListener(v -> {
            if (product.getQuantity() <= 0) {
                Toast.makeText(ctx, "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }
            CartManager.getInstance().addItem(product);
            Toast.makeText(ctx, "Added to cart", Toast.LENGTH_SHORT).show();
            if (changeListener != null) changeListener.onStockChanged();
        });

        // ── Edit Cost Price Action ───────────────────────────────────────────
        h.btnEditCostPrice.setOnClickListener(v -> {
            showEditDialog(product, "cost_price", "Edit Cost Price", h, fmt);
        });

        // ── Card click → detail ───────────────────────────────────────────────
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(product);
        });
    }

    private void showEditDialog(Product product, String type, String title, StockViewHolder h, NumberFormat fmt) {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(title);
        
        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(type.equals("price") ? product.getPrice() : product.getCostPrice()));
        b.setView(input);

        b.setPositiveButton("Save", (dialog, which) -> {
            try {
                double newVal = Double.parseDouble(input.getText().toString());
                ApiService.getInstance(ctx).updateProduct(product.getId(), type, newVal, 0,
                        new ApiService.ApiCallback() {
                            @Override public void onSuccess(JSONObject response) {
                                if (response.optBoolean("success")) {
                                    if (type.equals("price")) {
                                        product.setPrice(newVal);
                                        h.tvPrice.setText(fmt.format(product.getPrice()));
                                    } else {
                                        product.setCostPrice(newVal);
                                        h.tvCostPrice.setText("Cost: " + fmt.format(product.getCostPrice()));
                                    }
                                    if (changeListener != null) changeListener.onStockChanged();
                                } else {
                                    Toast.makeText(ctx, response.optString("message", "Update failed"), Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override public void onError(String error) { Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show(); }
                        });
            } catch (Exception e) {
                Toast.makeText(ctx, "Invalid value", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void updateQuantityOnServer(Product product, int delta, StockViewHolder h) {
        ApiService.getInstance(ctx).updateProduct(product.getId(), "quantity_add", -1, delta,
                new ApiService.ApiCallback() {
                    @Override public void onSuccess(JSONObject response) {
                        if (response.optBoolean("success")) {
                            product.setQuantity(product.getQuantity() + delta);
                            h.tvQuantity.setText("Stock: " + product.getQuantity());
                            h.tvCurrentQty.setText(String.valueOf(product.getQuantity()));
                            
                            int stockColor = product.isLowStock() ? R.color.error : R.color.text_hint;
                            h.tvQuantity.setTextColor(ContextCompat.getColor(ctx, stockColor));
                            h.ivLowStock.setVisibility(product.isLowStock() ? View.VISIBLE : View.GONE);
                            
                            if (changeListener != null) changeListener.onStockChanged();
                        } else {
                            Toast.makeText(ctx, response.optString("message", "Update failed"), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onError(String error) { 
                        Toast.makeText(ctx, "Server error: " + error, Toast.LENGTH_SHORT).show(); 
                    }
                });
    }

    @Override public int getItemCount() { return productList.size(); }

    public void updateData(List<Product> newProducts) {
        productList.clear();
        productList.addAll(newProducts);
        productListFull.clear();
        productListFull.addAll(newProducts);
        notifyDataSetChanged();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompanyName, tvModelName, tvPrice, tvQuantity, tvCostPrice, tvCurrentQty;
        View ivLowStock;
        ImageButton btnAddToCart, btnPlus, btnMinus, btnEditCostPrice;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyName    = itemView.findViewById(R.id.tvCompanyName);
            tvModelName      = itemView.findViewById(R.id.tvModelName);
            tvPrice          = itemView.findViewById(R.id.tvPrice);
            tvQuantity       = itemView.findViewById(R.id.tvQuantity);
            tvCostPrice      = itemView.findViewById(R.id.tvCostPrice);
            tvCurrentQty     = itemView.findViewById(R.id.tvCurrentQty);
            ivLowStock       = itemView.findViewById(R.id.ivLowStock);
            btnAddToCart     = itemView.findViewById(R.id.btnAddToCart);
            btnPlus          = itemView.findViewById(R.id.btnPlus);
            btnMinus         = itemView.findViewById(R.id.btnMinus);
            btnEditCostPrice = itemView.findViewById(R.id.btnEditCostPrice);
        }
    }
}