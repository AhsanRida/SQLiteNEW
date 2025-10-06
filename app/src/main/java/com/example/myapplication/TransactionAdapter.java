package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    private List<Transaction> transactions;
    private Context ctx;

    public TransactionAdapter(Context ctx) { this.ctx = ctx; }

    public void setTransactions(List<Transaction> txns) {
        this.transactions = txns;
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Transaction t = transactions.get(position);
        holder.title.setText((t.isExpense() ? "-" : "+") + String.format(Locale.getDefault(),"%.2f", t.getAmount()));
        String note = t.getNote() == null ? "" : t.getNote();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.sub.setText(note + " • " + sdf.format(new Date(t.getTimestamp())));
    }

    @Override
    public int getItemCount() { return transactions == null ? 0 : transactions.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub;
        VH(View v) {
            super(v);
            title = v.findViewById(android.R.id.text1);
            sub = v.findViewById(android.R.id.text2);
        }
    }
}
