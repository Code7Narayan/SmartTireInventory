// FILE: adapters/StockAdapter.java  (UPDATED to include direct stock addition)
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
    private final List<Product> items = new ArrayList<>();

    private OnItemClickListener    clickListener;
    private OnStockChangedListener changeListener;

    private static final NumberFormat INR_FMT =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public StockAdapter(Context ctx, List<Product> initialData) {
        this.ctx = ctx;
        this.items.addAll(initialData);
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
        Product product = items.get(position);
        bindProduct(h, product);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder h, int position,
                                 @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0) instanceof String) {
            String type = (String) payloads.get(0);
            Product product = items.get(position);
            if ("QTY".equals(type) || "PRICE".equals(type)) {
                updatePriceAndQuantityViews(h, product);
                return;
            }
        }
        super.onBindViewHolder(h, position, payloads);
    }

    private void bindProduct(@NonNull StockViewHolder h, Product product) {
        String displayModel = product.getModelName().isEmpty()
                ? product.getTireType() : product.getModelName();
        h.tvModelName.setText(displayModel);
        h.tvCompanyName.setText(product.getCompanyName() + " | " + product.getTireSize());

        updatePriceAndQuantityViews(h, product);

        h.btnPlus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            updateQuantityOnServer(items.get(pos), 1, h, pos);
        });

        h.btnMinus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Product p = items.get(pos);
            if (p.getQuantity() > 0) {
                updateQuantityOnServer(p, -1, h, pos);
            } else {
                Toast.makeText(ctx, "Stock cannot be negative", Toast.LENGTH_SHORT).show();
            }
        });

        h.btnEditQty.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            showAddStockDialog(items.get(pos), h, pos);
        });

        h.btnAddToCart.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Product p = items.get(pos);
            if (p.getQuantity() <= 0) {
                Toast.makeText(ctx, "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }
            CartManager.getInstance().addItem(p);
            Toast.makeText(ctx, "Added to cart", Toast.LENGTH_SHORT).show();
            if (changeListener != null) changeListener.onStockChanged();
        });

        if (h.btnEditCostPrice != null) {
            h.btnEditCostPrice.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                showEditDialog(items.get(pos), "cost_price", "Edit Cost Price", pos);
            });
        }

        h.itemView.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (clickListener != null) clickListener.onItemClick(items.get(pos));
        });
    }

    private void updatePriceAndQuantityViews(@NonNull StockViewHolder h, Product product) {
        // Show Total Stock Value (Qty * Cost Price) instead of selling price
        double totalValue = product.getQuantity() * product.getCostPrice();
        h.tvPrice.setText("Total: " + INR_FMT.format(totalValue));
        
        // Show Unit Cost Price
        if (h.tvCostPrice != null) {
            h.tvCostPrice.setText("Unit: " + INR_FMT.format(product.getCostPrice()));
        }

        h.tvQuantity.setText("Stock: " + product.getQuantity());
        h.tvCurrentQty.setText(String.valueOf(product.getQuantity()));
        
        int stockColor = product.isLowStock() ? R.color.error : R.color.text_hint;
        h.tvQuantity.setTextColor(ContextCompat.getColor(ctx, stockColor));
        if (h.ivLowStock != null) {
            h.ivLowStock.setVisibility(product.isLowStock() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateQuantityOnServer(Product product, int delta,
                                        @NonNull StockViewHolder h, int pos) {
        product.setQuantity(product.getQuantity() + delta);
        updatePriceAndQuantityViews(h, product);

        ApiService.getInstance(ctx).updateProduct(
                product.getId(), "quantity_add", -1, delta,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (!response.optBoolean("success")) {
                            product.setQuantity(product.getQuantity() - delta);
                            updatePriceAndQuantityViews(h, product);
                            Toast.makeText(ctx, response.optString("message", "Update failed"),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            int serverQty = response.optInt("new_quantity", product.getQuantity());
                            product.setQuantity(serverQty);
                            updatePriceAndQuantityViews(h, product);
                            if (changeListener != null) changeListener.onStockChanged();
                        }
                    }
                    @Override
                    public void onError(String error) {
                        product.setQuantity(product.getQuantity() - delta);
                        updatePriceAndQuantityViews(h, product);
                        Toast.makeText(ctx, "Server error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddStockDialog(Product product, StockViewHolder h, int pos) {
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Add Stock Quantity");

        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter quantity to add");
        b.setView(input);

        b.setPositiveButton("Add", (dialog, which) -> {
            String raw = input.getText() != null ? input.getText().toString().trim() : "";
            if (raw.isEmpty()) return;
            try {
                int delta = Integer.parseInt(raw);
                if (delta > 0) {
                    updateQuantityOnServer(product, delta, h, pos);
                } else if (delta < 0) {
                    Toast.makeText(ctx, "Use minus button to reduce stock", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(ctx, "Invalid quantity", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
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
                                Toast.makeText(ctx, response.optString("message", "Update failed"),
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

    public void updateData(List<Product> newProducts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ProductDiffCallback(items, newProducts));
        items.clear();
        items.addAll(newProducts);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        final TextView  tvCompanyName, tvModelName, tvPrice, tvQuantity,
                tvCostPrice, tvCurrentQty;
        final View      ivLowStock;
        final ImageButton btnAddToCart, btnPlus, btnMinus, btnEditCostPrice, btnEditQty;

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
            btnEditQty       = itemView.findViewById(R.id.btnEditQty);
        }
    }

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