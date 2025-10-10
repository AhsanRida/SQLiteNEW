package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class TransactionsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnHamburger;
    private NavigationView navView;
    private Button btnAddTxn2;
    private RecyclerView rvRecent;
    private LineChart chartMonthlyNet;

    private DBHelper dbHelper;
    private TransactionAdapter adapter;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        drawerLayout = findViewById(R.id.drawer_layout);
        btnHamburger = findViewById(R.id.btnHamburger);
        navView = findViewById(R.id.nav_view);
        btnAddTxn2 = findViewById(R.id.btnAddTxn2);
        rvRecent = findViewById(R.id.rvRecent);
        chartMonthlyNet = findViewById(R.id.chartMonthlyNet);

        dbHelper = new DBHelper(this);
        userId = getSharedPreferences("app_prefs", MODE_PRIVATE).getLong("user_id", -1L);

        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this);
        rvRecent.setAdapter(adapter);

        initChart();
        setupHamburgerAndDrawer();

        btnAddTxn2.setOnClickListener(v ->
                startActivity(new Intent(this, AddTransactionActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
        loadChartData();
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
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
            } else if (id == R.id.nav_transactions) {
                // Already here
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadTransactions() {
        if (userId < 0) return;
        List<Transaction> recent = dbHelper.getTransactions(userId, 0, System.currentTimeMillis());
        adapter.setTransactions(recent);
    }

    private void initChart() {
        chartMonthlyNet.getAxisRight().setEnabled(false);
        chartMonthlyNet.getDescription().setEnabled(false);
        chartMonthlyNet.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.LINE);
        chartMonthlyNet.getXAxis().setGranularity(1f);
    }

    private void loadChartData() {
        // Populate chartMonthlyNet if desired
        //
    }
}
