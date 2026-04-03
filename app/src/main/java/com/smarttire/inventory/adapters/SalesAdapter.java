// FILE: adapters/SalesAdapter.java  (UPDATED — payment status badge, paid/remaining row)
package com.smarttire.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.SaleRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Sales History screen.
 * UPDATED: Shows payment status badge (PAID / PARTIAL / UNPAID) and
 *          a paid/remaining breakdown row when the sale is not fully paid.
 */
public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SaleViewHolder> {

    public interface OnSaleClickListener {
        void onSaleClick(SaleRecord sale);
    }

    private final Context           ctx;
    private final List<SaleRecord>  displayList;
    private final List<SaleRecord>  masterList;
    private       OnSaleClickListener listener;

    public SalesAdapter(Context ctx, List<SaleRecord> data) {
        this.ctx         = ctx;
        this.masterList  = new ArrayList<>(data);
        this.displayList = new ArrayList<>(data);
    }

    public void setOnSaleClickListener(OnSaleClickListener l) { this.listener = l; }

    @NonNull @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_sale, parent, false);
        return new SaleViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder h, int pos) {
        SaleRecord sale = displayList.get(pos);

        h.tvSaleId.setText("Sale #" + sale.getId());
        h.tvCompany.setText(sale.getCompanyName());
        h.tvTireDetails.setText(sale.getTireType() + " — " + sale.getTireSize());
        h.tvQuantity.setText("Qty: " + sale.getQuantity());
        h.tvUnitPrice.setText(sale.getFormattedUnitPrice() + " / unit");
        h.tvTotalPrice.setText(sale.getFormattedTotalPrice());
        h.tvDate.setText(sale.getFormattedDate());

        // ── Payment status badge ──────────────────────────────────────────────
        if (h.tvPaymentStatus != null) {
            h.tvPaymentStatus.setText(sale.getPaymentStatusLabel().toUpperCase());
            int colorRes;
            switch (sale.getPaymentStatus().toLowerCase()) {
                case "partial": colorRes = R.color.warning; break;
                case "unpaid":  colorRes = R.color.error;   break;
                default:        colorRes = R.color.success; break;
            }
            h.tvPaymentStatus.setTextColor(ContextCompat.getColor(ctx, colorRes));
        }

        // ── Paid / Remaining row (only when not fully paid) ───────────────────
        if (h.layoutPaymentDetail != null) {
            boolean hasDue = sale.getRemainingAmount() > 0.005;
            h.layoutPaymentDetail.setVisibility(hasDue ? View.VISIBLE : View.GONE);
            if (hasDue) {
                if (h.tvPaid      != null) h.tvPaid.setText(sale.getFormattedPaidAmount());
                if (h.tvRemaining != null) h.tvRemaining.setText(sale.getFormattedRemainingAmount());
            }
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSaleClick(sale);
        });
    }

    @Override public int getItemCount() { return displayList.size(); }

    // ── Data management ───────────────────────────────────────────────────────

    /** Replace all data and reset filter. */
    public void updateData(List<SaleRecord> newData) {
        masterList.clear();  masterList.addAll(newData);
        displayList.clear(); displayList.addAll(newData);
        notifyDataSetChanged();
    }

    /** Append paginated results. */
    public void appendData(List<SaleRecord> more) {
        int start = displayList.size();
        masterList.addAll(more);
        displayList.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    /** Local filter by search query. Empty string resets. */
    public void filter(String query) {
        displayList.clear();
        if (query == null || query.isEmpty()) {
            displayList.addAll(masterList);
        } else {
            String q = query.toLowerCase().trim();
            for (SaleRecord s : masterList) {
                if (s.getCompanyName().toLowerCase().contains(q)
                        || s.getTireType().toLowerCase().contains(q)
                        || s.getTireSize().toLowerCase().contains(q)
                        || s.getCustomerNameSnap().toLowerCase().contains(q)) {
                    displayList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getDisplayCount() { return displayList.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    public static class SaleViewHolder extends RecyclerView.ViewHolder {
        final TextView      tvSaleId, tvCompany, tvTireDetails,
                tvQuantity, tvUnitPrice, tvTotalPrice, tvDate;
        final TextView      tvPaymentStatus, tvPaid, tvRemaining;
        final LinearLayout  layoutPaymentDetail;

        public SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSaleId            = itemView.findViewById(R.id.tvSaleId);
            tvCompany           = itemView.findViewById(R.id.tvSaleCompany);
            tvTireDetails       = itemView.findViewById(R.id.tvSaleTireDetails);
            tvQuantity          = itemView.findViewById(R.id.tvSaleQuantity);
            tvUnitPrice         = itemView.findViewById(R.id.tvSaleUnitPrice);
            tvTotalPrice        = itemView.findViewById(R.id.tvSaleTotalPrice);
            tvDate              = itemView.findViewById(R.id.tvSaleDate);
            tvPaymentStatus     = itemView.findViewById(R.id.tvSalePaymentStatus);
            layoutPaymentDetail = itemView.findViewById(R.id.layoutPaymentDetail);
            tvPaid              = itemView.findViewById(R.id.tvSalePaid);
            tvRemaining         = itemView.findViewById(R.id.tvSaleRemaining);
        }
    }
}