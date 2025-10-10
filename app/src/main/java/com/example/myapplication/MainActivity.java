package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnHamburger;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        drawerLayout = findViewById(R.id.drawer_layout);
        btnHamburger = findViewById(R.id.btnHamburger);
        navView = findViewById(R.id.nav_view);

        if (drawerLayout == null || btnHamburger == null || navView == null) {
            Log.e("DebugCheck", "❌ One or more views are NULL — check your layout IDs!");
            return;
        }

        // ✅ Open drawer when hamburger clicked
        btnHamburger.setOnClickListener(v -> {
            Log.d("DebugCheck", "Hamburger clicked → opening drawer");
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // ✅ Handle navigation item clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Log.d("DebugCheck", "Menu clicked: " + getResources().getResourceEntryName(id));

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_transactions) {
                Log.d("DebugCheck", "Opening TransactionsActivity...");
                Intent intent = new Intent(MainActivity.this, TransactionsActivity.class);
                startActivity(intent);

            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
}
