package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class AddTransactionActivity extends AppCompatActivity {

    EditText etAmount, etNote;
    Spinner spinnerType, spinnerCategory;
    Button btnSave;
    DBHelper db;
    long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSaveTransaction);

        db = new DBHelper(this);
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        userId = prefs.getLong("user_id", -1L);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.type_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        List<Category> categories = db.getCategories();
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Category c : categories) catAdapter.add(c.getName());
        spinnerCategory.setAdapter(catAdapter);

        btnSave.setOnClickListener(v -> {
            String amtStr = etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amtStr)) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amtStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isExpense = spinnerType.getSelectedItemPosition() == 1;

            int catIndex = spinnerCategory.getSelectedItemPosition();
            Integer catId = null;
            if (catIndex >= 0 && catIndex < categories.size()) {
                catId = categories.get(catIndex).getId();
            }

            String note = etNote.getText().toString().trim();
            long ts = System.currentTimeMillis();

            if (userId < 0) {
                Toast.makeText(this, "No user found. Please sign up/login.", Toast.LENGTH_SHORT).show();
                return;
            }

            long row = db.addTransaction(userId, catId, amount, isExpense, note, ts);
            if (row > 0) {
                Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
