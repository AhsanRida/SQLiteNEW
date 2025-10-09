package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout with the Drawer and Toolbar
        setContentView(R.layout.activity_welcome);

        // --- Initialize Toolbar and Drawer ---
        Toolbar mytoolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mytoolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Enable the hamburger menu icon toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                mytoolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Handle menu item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_transactions) {
                Intent intent = new Intent(MainActivity.this, TransactionsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_archive) {
                Intent intent = new Intent(MainActivity.this, ArchiveActivity.class);
                startActivity(intent);
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // --- Optional: Route user if not logged in ---
        route();
    }

    private void route() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long userId = prefs.getLong("user_id", -1L);

        Intent intent;
        if (userId > 0) {
            // User already logged in → show main content
            // (You can stay in MainActivity or go to WelcomeActivity)
            intent = new Intent(MainActivity.this, WelcomeActivity.class);
        } else {
            // Not logged in → go to login
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu); // main_menu is the name of your menu resource file
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.activity_) {
//            // Handle settings action
//            return true;
//        } else if (item.getItemId() == R.id.action_share) {
//            // Handle share action
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
