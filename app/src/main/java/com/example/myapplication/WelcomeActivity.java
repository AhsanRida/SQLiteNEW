package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.util.Pair;
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

    private DrawerLayout drawerLayout;
    private ImageButton btnHamburger;
    private NavigationView navView;

    private TextView tvTotalIncome, tvTotalExpense, tvNet, tvNetWorth;
    private Button btnAddTxn;
    private RecyclerView rvRecent;
    private DBHelper dbHelper;
    private TransactionAdapter adapter;
    private long userId;

    private LineChart chartMonthlyNet;
    private PieChart chartCategoryPie;
    private SimpleDateFormat monthFmt = new SimpleDateFormat("MMM yy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        drawerLayout = findViewById(R.id.drawer_layout);
        btnHamburger = findViewById(R.id.btnHamburger);
        navView = findViewById(R.id.nav_view);

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

        btnAddTxn.setOnClickListener(v -> startActivity(new Intent(this, AddTransactionActivity.class)));

        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this);
        rvRecent.setAdapter(adapter);

        initCharts();
        setupHamburgerAndDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
        loadCharts();
    }

    private void setupHamburgerAndDrawer() {
        btnHamburger.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_home) {
                // Already on WelcomeActivity
            } else if (id == R.id.nav_transactions) {
                startActivity(new Intent(this, TransactionsActivity.class));
                finish();
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // -------------------- DASHBOARD METHODS --------------------
    private void refreshDashboard() {
        if (userId < 0) return;

        // Fetch transactions and calculate totals
        List<Transaction> list = dbHelper.getTransactions(userId, 0, System.currentTimeMillis());
        double income = 0.0, expense = 0.0;
        for (Transaction t : list) {
            if (t.isExpense()) expense += t.getAmount();
            else income += t.getAmount();
        }
        double net = income - expense;

        tvTotalIncome.setText(String.format(Locale.getDefault(), "Income: %.2f", income));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "Expense: %.2f", expense));
        tvNet.setText(String.format(Locale.getDefault(), "Net: %.2f", net));

        rvRecent.setAdapter(adapter);
        adapter.setTransactions(list);
    }

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
