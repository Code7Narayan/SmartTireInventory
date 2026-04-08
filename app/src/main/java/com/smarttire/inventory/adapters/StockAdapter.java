// FILE: adapters/StockAdapter.java  (REDESIGNED — model-grouped, +qty, editable price, CRUD actions)
package com.smarttire.inventory.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Product;
import com.smarttire.inventory.network.ApiService;

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

        // Company
        h.tvCompanyName.setText(product.getCompanyName());

        // Model name — primary identifier
        String model = product.getModelName();
        if (model != null && !model.isEmpty()) {
            h.tvModelName.setVisibility(View.VISIBLE);
            h.tvModelName.setText(model);
        } else {
            h.tvModelName.setVisibility(View.VISIBLE);
            h.tvModelName.setText(product.getTireType()); // fallback to tire type
        }

        // Tire details
        h.tvTireDetails.setText(product.getTireType() + " • " + product.getTireSize());

        // Price
        h.tvPrice.setText(fmt.format(product.getPrice()));

        // Quantity with color coding
        h.tvQuantity.setText(String.valueOf(product.getQuantity()));
        h.tvQuantity.setTextColor(ContextCompat.getColor(ctx,
                product.isLowStock() ? R.color.error : R.color.text_primary));

        // Low stock warning
        h.ivLowStock.setVisibility(product.isLowStock() ? View.VISIBLE : View.GONE);

        // ── + button: add 1 quantity ──────────────────────────────────────────
        h.btnAddQty.setOnClickListener(v -> {
            showAddQtyDialog(product, h);
        });

        // ── Edit price ────────────────────────────────────────────────────────
        h.ivEditPrice.setOnClickListener(v -> showPriceEditDialog(product, h, fmt));
        h.tvPrice.setOnClickListener(v -> showPriceEditDialog(product, h, fmt));

        // ── Update button ─────────────────────────────────────────────────────
        h.btnUpdate.setOnClickListener(v -> showUpdateDialog(product, h, fmt));

        // ── Delete button ─────────────────────────────────────────────────────
        h.btnDelete.setOnClickListener(v -> showDeleteConfirmation(product, position));

        // ── Card click → detail ───────────────────────────────────────────────
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(product);
        });
    }

    // ── + Qty Dialog ──────────────────────────────────────────────────────────

    private void showAddQtyDialog(Product product, StockViewHolder h) {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Add Stock: " + product.getCompanyName());
        b.setMessage("Current quantity: " + product.getQuantity() + "\nHow many to add?");

        EditText et = new EditText(ctx);
        et.setHint("Quantity to add");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setText("1");
        b.setView(et);

        b.setPositiveButton("Add", (dialog, which) -> {
            String val = et.getText().toString().trim();
            if (val.isEmpty()) return;
            int qty;
            try { qty = Integer.parseInt(val); } catch (NumberFormatException e) { return; }
            if (qty <= 0) { Toast.makeText(ctx, "Enter a valid quantity", Toast.LENGTH_SHORT).show(); return; }

            ApiService.getInstance(ctx).updateProduct(product.getId(), "quantity_add", -1, qty,
                    new ApiService.ApiCallback() {
                        @Override public void onSuccess(JSONObject response) {
                            try {
                                if (response.getBoolean("success")) {
                                    product.setQuantity(product.getQuantity() + qty);
                                    h.tvQuantity.setText(String.valueOf(product.getQuantity()));
                                    h.tvQuantity.setTextColor(ContextCompat.getColor(ctx,
                                            product.isLowStock() ? R.color.error : R.color.text_primary));
                                    h.ivLowStock.setVisibility(product.isLowStock() ? View.VISIBLE : View.GONE);
                                    Toast.makeText(ctx, "+" + qty + " added", Toast.LENGTH_SHORT).show();
                                    if (changeListener != null) changeListener.onStockChanged();
                                } else {
                                    Toast.makeText(ctx, response.optString("message"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        @Override public void onError(String error) {
                            Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    // ── Price Edit Dialog ─────────────────────────────────────────────────────

    private void showPriceEditDialog(Product product, StockViewHolder h, NumberFormat fmt) {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Update Price");
        b.setMessage(product.getCompanyName() + " • " + product.getModelName());

        EditText et = new EditText(ctx);
        et.setHint("New price");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(String.valueOf(product.getPrice()));
        b.setView(et);

        b.setPositiveButton("Update", (dialog, which) -> {
            String val = et.getText().toString().trim();
            if (val.isEmpty()) return;
            double price;
            try { price = Double.parseDouble(val); } catch (NumberFormatException e) { return; }
            if (price <= 0) { Toast.makeText(ctx, "Enter a valid price", Toast.LENGTH_SHORT).show(); return; }

            ApiService.getInstance(ctx).updateProduct(product.getId(), "price", price, 0,
                    new ApiService.ApiCallback() {
                        @Override public void onSuccess(JSONObject response) {
                            try {
                                if (response.getBoolean("success")) {
                                    product.setPrice(price);
                                    h.tvPrice.setText(fmt.format(price));
                                    Toast.makeText(ctx, "Price updated", Toast.LENGTH_SHORT).show();
                                    if (changeListener != null) changeListener.onStockChanged();
                                } else {
                                    Toast.makeText(ctx, response.optString("message"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        @Override public void onError(String error) {
                            Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    // ── Update Dialog (price + qty) ───────────────────────────────────────────

    private void showUpdateDialog(Product product, StockViewHolder h, NumberFormat fmt) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(ctx);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etPrice = new EditText(ctx);
        etPrice.setHint("New price (leave empty to keep)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        EditText etQty = new EditText(ctx);
        etQty.setHint("Add quantity (leave empty to skip)");
        etQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etQty);

        new AlertDialog.Builder(ctx)
                .setTitle("Update: " + product.getCompanyName() + " • " + (product.getModelName().isEmpty() ? product.getTireType() : product.getModelName()))
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String priceStr = etPrice.getText().toString().trim();
                    String qtyStr   = etQty.getText().toString().trim();
                    double newPrice = priceStr.isEmpty() ? -1 : Double.parseDouble(priceStr);
                    int    addQty   = qtyStr.isEmpty()   ? 0  : Integer.parseInt(qtyStr);

                    ApiService.getInstance(ctx).updateProduct(product.getId(), "update", newPrice, addQty,
                            new ApiService.ApiCallback() {
                                @Override public void onSuccess(JSONObject response) {
                                    try {
                                        if (response.getBoolean("success")) {
                                            if (newPrice > 0) { product.setPrice(newPrice); h.tvPrice.setText(fmt.format(newPrice)); }
                                            if (addQty  > 0) {
                                                product.setQuantity(product.getQuantity() + addQty);
                                                h.tvQuantity.setText(String.valueOf(product.getQuantity()));
                                            }
                                            Toast.makeText(ctx, "Updated successfully", Toast.LENGTH_SHORT).show();
                                            if (changeListener != null) changeListener.onStockChanged();
                                        } else {
                                            Toast.makeText(ctx, response.optString("message"), Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                                @Override public void onError(String error) { Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show(); }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Delete Confirmation ───────────────────────────────────────────────────

    private void showDeleteConfirmation(Product product, int position) {
        new AlertDialog.Builder(ctx)
                .setTitle("Delete Product")
                .setMessage("Delete \"" + product.getDisplayName() + "\"?\n\nThis cannot be undone if the product has no sales.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiService.getInstance(ctx).updateProduct(product.getId(), "delete", -1, 0,
                            new ApiService.ApiCallback() {
                                @Override public void onSuccess(JSONObject response) {
                                    try {
                                        if (response.getBoolean("success")) {
                                            int pos = productList.indexOf(product);
                                            if (pos >= 0) {
                                                productList.remove(pos);
                                                productListFull.remove(product);
                                                notifyItemRemoved(pos);
                                            }
                                            Toast.makeText(ctx, "Product deleted", Toast.LENGTH_SHORT).show();
                                            if (changeListener != null) changeListener.onStockChanged();
                                        } else {
                                            Toast.makeText(ctx, response.optString("message"), Toast.LENGTH_LONG).show();
                                        }
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                                @Override public void onError(String error) { Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show(); }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public int getItemCount() { return productList.size(); }

    public void filter(String query) {
        productList.clear();
        if (query.isEmpty()) {
            productList.addAll(productListFull);
        } else {
            String q = query.toLowerCase().trim();
            for (Product p : productListFull) {
                if (p.getCompanyName().toLowerCase().contains(q)
                        || p.getTireType().toLowerCase().contains(q)
                        || p.getTireSize().toLowerCase().contains(q)
                        || p.getModelName().toLowerCase().contains(q)) {
                    productList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<Product> newProducts) {
        productList.clear();
        productList.addAll(newProducts);
        productListFull.clear();
        productListFull.addAll(newProducts);
        notifyDataSetChanged();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView     tvCompanyName, tvModelName, tvTireDetails, tvPrice, tvQuantity;
        ImageView    ivLowStock, ivEditPrice, btnDelete, btnUpdate;
        MaterialButton btnAddQty;
        CardView     cardView;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            if (itemView instanceof CardView) cardView = (CardView) itemView;
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvModelName   = itemView.findViewById(R.id.tvModelName);
            tvTireDetails = itemView.findViewById(R.id.tvTireDetails);
            tvPrice       = itemView.findViewById(R.id.tvPrice);
            tvQuantity    = itemView.findViewById(R.id.tvQuantity);
            ivLowStock    = itemView.findViewById(R.id.ivLowStock);
            ivEditPrice   = itemView.findViewById(R.id.ivEditPrice);
            btnDelete     = itemView.findViewById(R.id.btnDelete);
            btnUpdate     = itemView.findViewById(R.id.btnUpdate);
            btnAddQty     = itemView.findViewById(R.id.btnAddQty);
        }
    }
}