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
import java.util.Locale;
import android.util.Pair;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;

public class WelcomeActivity extends AppCompatActivity {

    TextView tvTotalIncome, tvTotalExpense, tvNet, tvNetWorth;
    Button btnAddTxn;
    RecyclerView rvRecent;
    DBHelper dbHelper;
    TransactionAdapter adapter;
    long userId;

    LineChart chartMonthlyNet;
    PieChart chartCategoryPie;
    SimpleDateFormat monthFmt = new SimpleDateFormat("MMM yy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNet = findViewById(R.id.tvNet);
        tvNetWorth = findViewById(R.id.tvNetWorth);
        btnAddTxn = findViewById(R.id.btnAddTxn);
        rvRecent = findViewById(R.id.rvRecent);

        chartMonthlyNet = findViewById(R.id.chartMonthlyNet);
        chartCategoryPie = findViewById(R.id.chartCategoryPie);

        dbHelper = new DBHelper(this);
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        userId = prefs.getLong("user_id", -1L);

        btnAddTxn.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, AddTransactionActivity.class)));

        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this);
        rvRecent.setAdapter(adapter);

        initCharts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();

        loadCharts();
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
        Calendar prevStart = Calendar.getInstance();
        prevStart.add(Calendar.MONTH, -1);
        prevStart.set(Calendar.DAY_OF_MONTH, 1);
        prevStart.set(Calendar.HOUR_OF_DAY, 0);
        prevStart.set(Calendar.MINUTE, 0);
        prevStart.set(Calendar.SECOND, 0);
        prevStart.set(Calendar.MILLISECOND, 0);

        Calendar prevEnd = Calendar.getInstance();
        prevEnd.add(Calendar.MONTH, -1);
        prevEnd.set(Calendar.DAY_OF_MONTH, prevEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        prevEnd.set(Calendar.HOUR_OF_DAY, 23);
        prevEnd.set(Calendar.MINUTE, 59);
        prevEnd.set(Calendar.SECOND, 59);
        prevEnd.set(Calendar.MILLISECOND, 999);

        double prevNet = dbHelper.getNetProfit(userId, prevStart.getTimeInMillis(), prevEnd.getTimeInMillis());
        double momPct = MathEcon.percentageIncrease(prevNet, net);
        tvTotalIncome.setText(String.format(Locale.getDefault(), "Income: %.2f", income));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "Expense: %.2f", expense));
        tvNet.setText(String.format(
                Locale.getDefault(),
                "Net: %.2f  (MoM: %.1f%%)",
                net, momPct));
        // ----- Assets & Liabilities from SharedPreferences (example keys) -----
        SharedPreferences prefsAL = getSharedPreferences("finance_inputs", MODE_PRIVATE);

// Example: read saved values (default to 0 if not set)
        double cash        = Double.longBitsToDouble(prefsAL.getLong("assets_cash",        Double.doubleToLongBits(0)));
        double investments = Double.longBitsToDouble(prefsAL.getLong("assets_investments", Double.doubleToLongBits(0)));
        double property    = Double.longBitsToDouble(prefsAL.getLong("assets_property",    Double.doubleToLongBits(0)));

        double loans       = Double.longBitsToDouble(prefsAL.getLong("liab_loans",         Double.doubleToLongBits(0)));
        double mortgage    = Double.longBitsToDouble(prefsAL.getLong("liab_mortgage",      Double.doubleToLongBits(0)));
        double bills       = Double.longBitsToDouble(prefsAL.getLong("liab_bills",         Double.doubleToLongBits(0)));

// Totals via MathEcon helpers
        double totalAssets      = MathEcon.totalAssets(cash, investments, property);
        double totalLiabilities = MathEcon.totalLiabilities(loans, mortgage, bills);
        double netWorth         = totalAssets - totalLiabilities;

// Show it
        if (tvNetWorth != null) {
            tvNetWorth.setText(String.format(Locale.getDefault(), "Net Worth: %.2f", netWorth));
        }

        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        List<Transaction> recent = dbHelper.getTransactions(userId, thirtyDaysAgo, System.currentTimeMillis());
        adapter.setTransactions(recent);


}

    // =======================
    // chart helpers
    // =======================
    private void initCharts() {
        if (chartMonthlyNet != null) {
            chartMonthlyNet.getAxisRight().setEnabled(false);
            chartMonthlyNet.getDescription().setEnabled(false);
            chartMonthlyNet.getLegend().setForm(Legend.LegendForm.LINE);
            chartMonthlyNet.getXAxis().setGranularity(1f);
        }

        if (chartCategoryPie != null) {
            Description d = new Description();
            d.setText("");
            chartCategoryPie.setDescription(d);
            chartCategoryPie.setUsePercentValues(false);
            chartCategoryPie.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        }
    }

    private void loadCharts() {
        if (userId < 0) return;

        // 1) Monthly Net (last 6 months) with 3-mo moving average
        List<Pair<Long, Double>> months = MathEcon.monthlyNetSeries(dbHelper, userId, 6);
        List<Pair<Long, Double>> ma = MathEcon.movingAverage(months, 3);

        if (chartMonthlyNet != null) {
            List<Entry> netEntries = new java.util.ArrayList<>();
            List<Entry> maEntries  = new java.util.ArrayList<>();

            for (int i = 0; i < months.size(); i++) {
                netEntries.add(new Entry(i, months.get(i).second.floatValue()));
                maEntries.add(new Entry(i, ma.get(i).second.floatValue()));
            }

            LineDataSet dsNet = new LineDataSet(netEntries, "Monthly Net");
            dsNet.setCircleRadius(3f);
            dsNet.setLineWidth(2.2f);

            LineDataSet dsMA = new LineDataSet(maEntries, "3-mo Avg");
            dsMA.setCircleRadius(0f);
            dsMA.setLineWidth(2.0f);
            dsMA.setDrawCircles(false);
            dsMA.enableDashedLine(10f, 6f, 0f);

            chartMonthlyNet.setData(new LineData(dsNet, dsMA));
            chartMonthlyNet.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    int idx = Math.max(0, Math.min(months.size() - 1, (int) value));
                    return monthFmt.format(new java.util.Date(months.get(idx).first));
                }
            });

            chartMonthlyNet.invalidate();
        }

        // 2) Pie: current month expenses by category
        if (chartCategoryPie != null) {
            List<Pair<String, Double>> cats = MathEcon.currentMonthCategoryExpenseModel(dbHelper, userId);
            List<PieEntry> pieEntries = new java.util.ArrayList<>();
            for (Pair<String, Double> p : cats) {
                if (p.second > 0) pieEntries.add(new PieEntry(p.second.floatValue(), p.first));
            }
            PieDataSet pds = new PieDataSet(pieEntries, "Expenses by Category");
            pds.setSliceSpace(2f);
            chartCategoryPie.setData(new PieData(pds));
            chartCategoryPie.invalidate();
        }
    }
}
