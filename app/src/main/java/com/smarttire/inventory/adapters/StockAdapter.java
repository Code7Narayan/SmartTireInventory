// FILE: adapters/StockAdapter.java  (OPTIMIZED — no server calls on main thread during bind,
//   DiffUtil support, stable IDs, efficient ViewHolder)
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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
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

    public interface OnItemClickListener    { void onItemClick(Product product); }
    public interface OnStockChangedListener { void onStockChanged(); }

    private final Context       ctx;
    // Adapter owns its own internal list — never hold a reference to the Fragment's list directly
    private final List<Product> items = new ArrayList<>();

    private OnItemClickListener    clickListener;
    private OnStockChangedListener changeListener;

    private static final NumberFormat INR_FMT =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public StockAdapter(Context ctx, List<Product> initialData) {
        this.ctx = ctx;
        this.items.addAll(initialData);
        // Stable IDs allow RecyclerView to animate changes correctly
        setHasStableIds(true);
    }

    public void setOnItemClickListener(OnItemClickListener l)     { this.clickListener  = l; }
    public void setOnStockChangedListener(OnStockChangedListener l){ this.changeListener = l; }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder h, int position) {
        // Use getAdapterPosition() inside click listeners — never capture `position` directly
        Product product = items.get(position);
        bindProduct(h, product);
    }

    /**
     * Called by RecyclerView for partial updates. Handles the "quantity changed" payload
     * so only the quantity TextView is redrawn — not the entire row.
     */
    @Override
    public void onBindViewHolder(@NonNull StockViewHolder h, int position,
                                 @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0) instanceof String) {
            String type = (String) payloads.get(0);
            Product product = items.get(position);
            if ("QTY".equals(type)) {
                updateQuantityViews(h, product);
                return;
            }
            if ("PRICE".equals(type)) {
                h.tvPrice.setText(INR_FMT.format(product.getPrice()));
                if (h.tvCostPrice != null)
                    h.tvCostPrice.setText("Cost: " + INR_FMT.format(product.getCostPrice()));
                return;
            }
        }
        super.onBindViewHolder(h, position, payloads);
    }

    private void bindProduct(@NonNull StockViewHolder h, Product product) {
        // Header
        String displayModel = product.getModelName().isEmpty()
                ? product.getTireType() : product.getModelName();
        h.tvModelName.setText(displayModel);
        h.tvCompanyName.setText(product.getCompanyName() + " | " + product.getTireSize());

        // Price & Stock
        h.tvPrice.setText(INR_FMT.format(product.getPrice()));
        if (h.tvCostPrice != null) {
            h.tvCostPrice.setText("Cost: " + INR_FMT.format(product.getCostPrice()));
        }
        updateQuantityViews(h, product);

        // ── Plus Button ──────────────────────────────────────────────────────
        h.btnPlus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            updateQuantityOnServer(items.get(pos), 1, h, pos);
        });

        // ── Minus Button ─────────────────────────────────────────────────────
        h.btnMinus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            Product p = items.get(pos);
            if (p.getQuantity() > 0) {
                updateQuantityOnServer(p, -1, h, pos);
            } else {
                Toast.makeText(ctx, "Stock cannot be negative", Toast.LENGTH_SHORT).show();
            }
        });

        // ── Cart Button ───────────────────────────────────────────────────────
        h.btnAddToCart.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            Product p = items.get(pos);
            if (p.getQuantity() <= 0) {
                Toast.makeText(ctx, "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }
            CartManager.getInstance().addItem(p);
            Toast.makeText(ctx, "Added to cart", Toast.LENGTH_SHORT).show();
            if (changeListener != null) changeListener.onStockChanged();
        });

        // ── Edit Cost Price ───────────────────────────────────────────────────
        if (h.btnEditCostPrice != null) {
            h.btnEditCostPrice.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                showEditDialog(items.get(pos), "cost_price", "Edit Cost Price", pos);
            });
        }

        // ── Card Click → Detail ───────────────────────────────────────────────
        h.itemView.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            if (clickListener != null) clickListener.onItemClick(items.get(pos));
        });
    }

    private void updateQuantityViews(@NonNull StockViewHolder h, Product product) {
        h.tvQuantity.setText("Stock: " + product.getQuantity());
        h.tvCurrentQty.setText(String.valueOf(product.getQuantity()));
        int stockColor = product.isLowStock() ? R.color.error : R.color.text_hint;
        h.tvQuantity.setTextColor(ContextCompat.getColor(ctx, stockColor));
        if (h.ivLowStock != null) {
            h.ivLowStock.setVisibility(product.isLowStock() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Sends +/- request to server WITHOUT blocking the UI.
     * Optimistically updates the local model before server responds.
     */
    private void updateQuantityOnServer(Product product, int delta,
                                        @NonNull StockViewHolder h, int pos) {
        // Optimistic UI update immediately (no lag)
        product.setQuantity(product.getQuantity() + delta);
        notifyItemChanged(pos, "QTY");

        ApiService.getInstance(ctx).updateProduct(
                product.getId(), "quantity_add", -1, delta,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (!response.optBoolean("success")) {
                            // Revert on failure
                            product.setQuantity(product.getQuantity() - delta);
                            notifyItemChanged(pos, "QTY");
                            Toast.makeText(ctx, response.optString("message", "Update failed"),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Sync with server value to stay accurate
                            int serverQty = response.optInt("new_quantity", product.getQuantity());
                            product.setQuantity(serverQty);
                            notifyItemChanged(pos, "QTY");
                            if (changeListener != null) changeListener.onStockChanged();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Revert optimistic update
                        product.setQuantity(product.getQuantity() - delta);
                        notifyItemChanged(pos, "QTY");
                        Toast.makeText(ctx, "Server error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDialog(Product product, String type, String title, int pos) {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(title);

        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf("price".equals(type) ? product.getPrice() : product.getCostPrice()));
        b.setView(input);

        b.setPositiveButton("Save", (dialog, which) -> {
            String raw = input.getText() != null ? input.getText().toString().trim() : "";
            if (raw.isEmpty()) return;
            double newVal;
            try { newVal = Double.parseDouble(raw); }
            catch (NumberFormatException e) {
                Toast.makeText(ctx, "Invalid value", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiService.getInstance(ctx).updateProduct(product.getId(), type, newVal, 0,
                    new ApiService.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            if (response.optBoolean("success")) {
                                if ("price".equals(type)) product.setPrice(newVal);
                                else product.setCostPrice(newVal);
                                notifyItemChanged(pos, "PRICE");
                                if (changeListener != null) changeListener.onStockChanged();
                            } else {
                                Toast.makeText(ctx,
                                        response.optString("message", "Update failed"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onError(String error) {
                            Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── Public update API ────────────────────────────────────────────────────

    /**
     * Full replace using DiffUtil for minimal, animated updates.
     * Safe to call from main thread; diff is computed inside (list is small enough).
     * For very large lists (500+), compute diff on background thread first.
     */
    public void updateData(List<Product> newProducts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ProductDiffCallback(items, newProducts));
        items.clear();
        items.addAll(newProducts);
        diffResult.dispatchUpdatesTo(this);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        final TextView  tvCompanyName, tvModelName, tvPrice, tvQuantity,
                tvCostPrice, tvCurrentQty;
        final View      ivLowStock;
        final ImageButton btnAddToCart, btnPlus, btnMinus, btnEditCostPrice;

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

    // ── DiffUtil Callback ────────────────────────────────────────────────────

    private static class ProductDiffCallback extends DiffUtil.Callback {
        private final List<Product> oldList;
        private final List<Product> newList;

        ProductDiffCallback(List<Product> oldList, List<Product> newList) {
            this.oldList = new ArrayList<>(oldList);
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int op, int np) {
            return oldList.get(op).getId() == newList.get(np).getId();
        }

        @Override
        public boolean areContentsTheSame(int op, int np) {
            Product o = oldList.get(op);
            Product n = newList.get(np);
            return o.getQuantity() == n.getQuantity()
                    && Double.compare(o.getPrice(), n.getPrice()) == 0
                    && Double.compare(o.getCostPrice(), n.getCostPrice()) == 0
                    && o.getModelName().equals(n.getModelName())
                    && o.getTireType().equals(n.getTireType())
                    && o.getTireSize().equals(n.getTireSize());
        }

        @Override
        @Nullable
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            Product o = oldList.get(oldItemPosition);
            Product n = newList.get(newItemPosition);
            if (o.getQuantity() != n.getQuantity()) return "QTY";
            if (Double.compare(o.getPrice(), n.getPrice()) != 0
                    || Double.compare(o.getCostPrice(), n.getCostPrice()) != 0) return "PRICE";
            return null;
        }
    }
}