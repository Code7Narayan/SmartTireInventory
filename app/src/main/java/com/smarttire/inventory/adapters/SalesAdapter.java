// FILE: app/src/main/java/com/smarttire/inventory/adapters/SalesAdapter.java
package com.smarttire.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.SaleRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Sales History screen.
 * Supports item-click for invoice generation and live search filtering.
 */
public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SaleViewHolder> {

    public interface OnSaleClickListener {
        void onSaleClick(SaleRecord sale);
    }

    private final Context             ctx;
    private final List<SaleRecord>    displayList;   // currently shown
    private final List<SaleRecord>    masterList;    // full unfiltered copy
    private       OnSaleClickListener listener;

    public SalesAdapter(Context ctx, List<SaleRecord> data) {
        this.ctx         = ctx;
        this.masterList  = new ArrayList<>(data);
        this.displayList = new ArrayList<>(data);
    }

    public void setOnSaleClickListener(OnSaleClickListener l) { this.listener = l; }

    // ── RecyclerView overrides ────────────────────────────────────────────────

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_sale, parent, false);
        return new SaleViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder h, int position) {
        SaleRecord sale = displayList.get(position);

        h.tvSaleId.setText("Sale #" + sale.getId());
        h.tvCompany.setText(sale.getCompanyName());
        h.tvTireDetails.setText(sale.getTireType() + " — " + sale.getTireSize());
        h.tvQuantity.setText("Qty: " + sale.getQuantity());
        h.tvUnitPrice.setText(sale.getFormattedUnitPrice() + " / unit");
        h.tvTotalPrice.setText(sale.getFormattedTotalPrice());
        h.tvDate.setText(sale.getFormattedDate());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSaleClick(sale);
        });
    }

    @Override
    public int getItemCount() { return displayList.size(); }

    // ── Data management ───────────────────────────────────────────────────────

    /** Replace all data and reset filter. */
    public void updateData(List<SaleRecord> newData) {
        masterList.clear();
        masterList.addAll(newData);
        displayList.clear();
        displayList.addAll(newData);
        notifyDataSetChanged();
    }

    /** Append a page of results (pagination). */
    public void appendData(List<SaleRecord> moreData) {
        int start = displayList.size();
        masterList.addAll(moreData);
        displayList.addAll(moreData);
        notifyItemRangeInserted(start, moreData.size());
    }

    /** Filter locally by query string. Pass empty to reset. */
    public void filter(String query) {
        displayList.clear();
        if (query == null || query.isEmpty()) {
            displayList.addAll(masterList);
        } else {
            String q = query.toLowerCase().trim();
            for (SaleRecord s : masterList) {
                if (s.getCompanyName().toLowerCase().contains(q)
                        || s.getTireType().toLowerCase().contains(q)
                        || s.getTireSize().toLowerCase().contains(q)) {
                    displayList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getDisplayCount() { return displayList.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    public static class SaleViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSaleId, tvCompany, tvTireDetails,
                tvQuantity, tvUnitPrice, tvTotalPrice, tvDate;

        public SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSaleId     = itemView.findViewById(R.id.tvSaleId);
            tvCompany    = itemView.findViewById(R.id.tvSaleCompany);
            tvTireDetails = itemView.findViewById(R.id.tvSaleTireDetails);
            tvQuantity   = itemView.findViewById(R.id.tvSaleQuantity);
            tvUnitPrice  = itemView.findViewById(R.id.tvSaleUnitPrice);
            tvTotalPrice = itemView.findViewById(R.id.tvSaleTotalPrice);
            tvDate       = itemView.findViewById(R.id.tvSaleDate);
        }
    }
}
