package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    TextView tvTotalIncome, tvTotalExpense, tvNet;
    Button btnAddTxn;
    RecyclerView rvRecent;
    DBHelper dbHelper;
    TransactionAdapter adapter;
    long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNet = findViewById(R.id.tvNet);
        btnAddTxn = findViewById(R.id.btnAddTxn);
        rvRecent = findViewById(R.id.rvRecent);

        dbHelper = new DBHelper(this);
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        userId = prefs.getLong("user_id", -1L);

        btnAddTxn.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, AddTransactionActivity.class)));

        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this);
        rvRecent.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    private void refreshDashboard() {
        if (userId < 0) return;

        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        long fromTs = start.getTimeInMillis();

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999);
        long toTs = end.getTimeInMillis();

        double net = dbHelper.getNetProfit(userId, fromTs, toTs);

        List<Transaction> list = dbHelper.getTransactions(userId, fromTs, toTs);
        double income = 0.0, expense = 0.0;
        for (Transaction t : list) {
            if (t.isExpense()) expense += t.getAmount();
            else income += t.getAmount();
        }
        tvTotalIncome.setText(String.format("Income: %.2f", income));
        tvTotalExpense.setText(String.format("Expense: %.2f", expense));
        tvNet.setText(String.format("Net: %.2f", net));

        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        List<Transaction> recent = dbHelper.getTransactions(userId, thirtyDaysAgo, System.currentTimeMillis());
        adapter.setTransactions(recent);
    }
}
