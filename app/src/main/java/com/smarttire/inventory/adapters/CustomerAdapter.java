// FILE: adapters/CustomerAdapter.java
package com.smarttire.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarttire.inventory.R;
import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.utils.MessageHelper;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.VH> {

    public interface OnItemClickListener { void onItemClick(Customer c); }

    private final Context        ctx;
    private final List<Customer> list;
    private OnItemClickListener  listener;

    public CustomerAdapter(Context ctx, List<Customer> list) {
        this.ctx  = ctx;
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_customer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Customer c = list.get(pos);
        h.tvName.setText(c.getName());
        h.tvPhone.setText(c.getPhone());
        h.tvSales.setText("Sales: " + c.getTotalSales());
        h.tvDue.setText(c.getFormattedDue());
        h.tvDue.setTextColor(ContextCompat.getColor(ctx,
                c.hasDue() ? R.color.warning : R.color.success));
        
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(c); });

        h.btnMessage.setOnClickListener(v -> {
            MessageHelper.sendCustomerSummarySms(ctx, c, c.getTotalAmount(), c.getTotalPaid(), c.getTotalDue());
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvSales, tvDue;
        ImageButton btnMessage;
        VH(@NonNull View v) {
            super(v);
            tvName  = v.findViewById(R.id.tvCustomerName);
            tvPhone = v.findViewById(R.id.tvCustomerPhone);
            tvSales = v.findViewById(R.id.tvCustomerSales);
            tvDue   = v.findViewById(R.id.tvCustomerDue);
            btnMessage = v.findViewById(R.id.btnSendMessage);
        }
    }
}