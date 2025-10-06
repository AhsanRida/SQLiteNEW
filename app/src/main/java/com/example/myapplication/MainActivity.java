package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // your main layout XML


    }
    private void route() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long userId = prefs.getLong("user_id", -1L);

        Intent intent;
        if (userId > 0) {
            intent = new Intent(MainActivity.this, WelcomeActivity.class);
        } else {
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
