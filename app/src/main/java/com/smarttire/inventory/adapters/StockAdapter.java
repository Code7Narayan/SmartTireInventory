// FILE: adapters/StockAdapter.java
package com.smarttire.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        this.context = context;
        this.productList = productList;
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

        // Company name
        holder.tvCompanyName.setText(product.getCompanyName());

        // Tire details
        String details = product.getTireType() + " - " + product.getTireSize();
        holder.tvTireDetails.setText(details);

        // Price
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        holder.tvPrice.setText(format.format(product.getPrice()));

        // Quantity
        holder.tvQuantity.setText(String.valueOf(product.getQuantity()));

        // Low stock indicator
        if (product.isLowStock()) {
            holder.ivLowStock.setVisibility(View.VISIBLE);
            holder.tvQuantity.setTextColor(ContextCompat.getColor(context, R.color.warning));
        } else {
            holder.ivLowStock.setVisibility(View.GONE);
            holder.tvQuantity.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // Filter products by search query
    public void filter(String query) {
        productList.clear();

        if (query.isEmpty()) {
            productList.addAll(productListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Product product : productListFull) {
                if (product.getCompanyName().toLowerCase().contains(lowerCaseQuery) ||
                        product.getTireType().toLowerCase().contains(lowerCaseQuery) ||
                        product.getTireSize().toLowerCase().contains(lowerCaseQuery)) {
                    productList.add(product);
                }
            }
        }

        notifyDataSetChanged();
    }

    // Update data
    public void updateData(List<Product> newProducts) {
        productList.clear();
        productList.addAll(newProducts);
        productListFull.clear();
        productListFull.addAll(newProducts);
        notifyDataSetChanged();
    }

    // ViewHolder class
    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompanyName, tvTireDetails, tvPrice, tvQuantity;
        ImageView ivLowStock;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvTireDetails = itemView.findViewById(R.id.tvTireDetails);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            ivLowStock = itemView.findViewById(R.id.ivLowStock);
        }
    }
}