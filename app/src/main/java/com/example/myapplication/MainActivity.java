package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnHamburger;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 👇 Make 100 % sure which layout is being loaded
        setContentView(R.layout.activity_welcome);
        Log.d("DebugCheck", "setContentView called for activity_welcome");

        drawerLayout = findViewById(R.id.drawer_layout);
        btnHamburger = findViewById(R.id.btnHamburger);

        if (btnHamburger == null) {
            Log.e("DebugCheck", "btnHamburger is NULL — layout mismatch!");
        } else {
            Log.d("DebugCheck", "btnHamburger found, attaching listener...");
            btnHamburger.setOnClickListener(v ->
                    Toast.makeText(MainActivity.this, "Hamburger clicked!", Toast.LENGTH_SHORT).show()
            );
        }
    }


}
