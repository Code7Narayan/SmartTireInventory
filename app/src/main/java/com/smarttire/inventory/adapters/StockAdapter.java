// FILE: adapters/StockAdapter.java  (ENHANCED — model name, low stock red, click support)
package com.smarttire.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private Context context;
    private List<Product> productList;
    private List<Product> productListFull;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public StockAdapter(Context context, List<Product> productList) {
        this.context         = context;
        this.productList     = productList;
        this.productListFull = new ArrayList<>(productList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Product product = productList.get(position);
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        holder.tvCompanyName.setText(product.getCompanyName());

        // Model name — show only if non-empty (TASK 1)
        String model = product.getModelName();
        if (model != null && !model.isEmpty()) {
            holder.tvModelName.setVisibility(View.VISIBLE);
            holder.tvModelName.setText(model);
        } else {
            holder.tvModelName.setVisibility(View.GONE);
        }

        holder.tvTireDetails.setText(product.getTireType() + " - " + product.getTireSize());
        holder.tvPrice.setText(fmt.format(product.getPrice()));
        holder.tvQuantity.setText(String.valueOf(product.getQuantity()));

        // Low stock highlight (TASK 1 & 5 — RED)
        if (product.isLowStock()) {
            holder.ivLowStock.setVisibility(View.VISIBLE);
            holder.tvQuantity.setTextColor(ContextCompat.getColor(context, R.color.error));
            // Red tint on card background for low stock
            if (holder.cardView != null) {
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.card_background));
            }
            // Use a subtle red border effect via elevation + tint
            holder.tvQuantity.setTextColor(ContextCompat.getColor(context, R.color.error));
        } else {
            holder.ivLowStock.setVisibility(View.GONE);
            holder.tvQuantity.setTextColor(ContextCompat.getColor(context, R.color.success));
            if (holder.cardView != null) {
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.card_background));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(product);
        });
    }

    @Override
    public int getItemCount() { return productList.size(); }

    public void filter(String query) {
        productList.clear();
        if (query.isEmpty()) {
            productList.addAll(productListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Product product : productListFull) {
                if (product.getCompanyName().toLowerCase().contains(lowerCaseQuery) ||
                        product.getTireType().toLowerCase().contains(lowerCaseQuery) ||
                        product.getTireSize().toLowerCase().contains(lowerCaseQuery) ||
                        product.getModelName().toLowerCase().contains(lowerCaseQuery)) {
                    productList.add(product);
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
        TextView  tvCompanyName, tvModelName, tvTireDetails, tvPrice, tvQuantity;
        ImageView ivLowStock;
        CardView  cardView;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find card view if present
            if (itemView instanceof CardView) cardView = (CardView) itemView;

            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvModelName   = itemView.findViewById(R.id.tvModelName);
            tvTireDetails = itemView.findViewById(R.id.tvTireDetails);
            tvPrice       = itemView.findViewById(R.id.tvPrice);
            tvQuantity    = itemView.findViewById(R.id.tvQuantity);
            ivLowStock    = itemView.findViewById(R.id.ivLowStock);
        }
    }
}