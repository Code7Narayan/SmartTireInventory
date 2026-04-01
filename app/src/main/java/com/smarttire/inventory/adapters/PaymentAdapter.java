// FILE: adapters/PaymentAdapter.java
package com.smarttire.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Payment;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.VH> {

    private final Context       ctx;
    private final List<Payment> list;

    public PaymentAdapter(Context ctx, List<Payment> list) {
        this.ctx  = ctx;
        this.list = list;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_payment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Payment p = list.get(pos);
        h.tvAmount.setText(p.getFormattedAmount());
        h.tvMode.setText(p.getPaymentMode().toUpperCase());
        h.tvDate.setText(p.getPaymentDate());
        h.tvNote.setText(p.getNote() != null && !p.getNote().isEmpty()
                ? p.getNote() : "—");
        h.tvSaleRef.setText("Sale #" + p.getSaleId());
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAmount, tvMode, tvDate, tvNote, tvSaleRef;
        VH(@NonNull View v) {
            super(v);
            tvAmount  = v.findViewById(R.id.tvPayAmount);
            tvMode    = v.findViewById(R.id.tvPayMode);
            tvDate    = v.findViewById(R.id.tvPayDate);
            tvNote    = v.findViewById(R.id.tvPayNote);
            tvSaleRef = v.findViewById(R.id.tvPaySaleRef);
        }
    }
}