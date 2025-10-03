package com.example.myapplication;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.util.ULocale;
import android.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "micro_data.db";
    private static final int DB_VERSION = 1;

    public static final String T_CATEGORIES = "categories";
    public static final String T_TRANSACTIONS = "transactions";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategories = "CREATE TABLE " + T_CATEGORIES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, type TEXT);";
        String createTrans = "CREATE TABLE " + T_TRANSACTIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, category_id INTEGER, amount REAL NOT NULL, is_expense INTEGER NOT NULL, note TEXT, timestamp INTEGER NOT NULL);";
        db.execSQL(createCategories);
        db.execSQL(createTrans);

        // Prepopulate realistic small-business categories (supplies, transport, rent, wages, sales, marketing, utilities, equipment, fees, misc)
        String[] cats = {"Sales","Supplies","Transport","Rent","Wages","Utilities","Marketing","Equipment","Fees","Misc"};
        for (String c : cats) {
            ContentValues cv = new ContentValues();
            cv.put("name", c);
            cv.put("type", c.equals("Sales") ? "income" : "expense");
            db.insert(T_CATEGORIES, null, cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + T_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + T_CATEGORIES);
        onCreate(db);
    }

    // categories
    public List<Category> getCategories() {
        List<Category> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_CATEGORIES, null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            out.add(new Category(c.getInt(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("name")), c.getString(c.getColumnIndexOrThrow("type"))));
        }
        c.close();
        return out;
    }

    public long addCategory(String name, String type) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("type", type);
        return db.insert(T_CATEGORIES, null, cv);
    }

    // transactions
    public long addTransaction(long userId, Integer categoryId, double amount, boolean isExpense, String note, long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        if (categoryId != null) cv.put("category_id", categoryId);
        cv.put("amount", amount);
        cv.put("is_expense", isExpense ? 1 : 0);
        cv.put("note", note);
        cv.put("timestamp", timestamp);
        return db.insert(T_TRANSACTIONS, null, cv);
    }

    public List<Transaction> getTransactions(long userId, long fromTs, long toTs) {
        List<Transaction> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_TRANSACTIONS, null, "user_id=? AND timestamp>=? AND timestamp<=?", new String[]{String.valueOf(userId), String.valueOf(fromTs), String.valueOf(toTs)}, null, null, "timestamp DESC");
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow("id"));
            int uid = c.getInt(c.getColumnIndexOrThrow("user_id"));
            Integer catId = c.isNull(c.getColumnIndexOrThrow("category_id")) ? null : c.getInt(c.getColumnIndexOrThrow("category_id"));
            double amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
            boolean isExp = c.getInt(c.getColumnIndexOrThrow("is_expense")) == 1;
            String note = c.getString(c.getColumnIndexOrThrow("note"));
            long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            out.add(new Transaction(id, uid, catId, amount, isExp, note, ts));
        }
        c.close();
        return out;
    }

    // analytics:
    public double getNetProfit(long userId, long fromTs, long toTs) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT SUM(CASE WHEN is_expense=1 THEN -amount ELSE amount END) as net FROM " + T_TRANSACTIONS + " WHERE user_id=? AND timestamp>=? AND timestamp<=?";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(fromTs), String.valueOf(toTs)});
        double net = 0.0;
        if (c.moveToFirst()) {
            net = c.isNull(c.getColumnIndexOrThrow("net")) ? 0.0 : c.getDouble(c.getColumnIndexOrThrow("net"));
        }
        c.close();
        return net;
    }

    public Map<String, Double> getExpensesByCategory(long userId, long fromTs, long toTs) {
        Map<String, Double> out = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT c.name as category, SUM(t.amount) as total FROM " + T_TRANSACTIONS + " t LEFT JOIN " + T_CATEGORIES + " c ON t.category_id=c.id WHERE t.user_id=? AND t.is_expense=1 AND t.timestamp>=? AND t.timestamp<=? GROUP BY c.name ORDER BY total DESC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(fromTs), String.valueOf(toTs)});
        while (c.moveToNext()) {
            String cat = c.getString(c.getColumnIndexOrThrow("category"));
            if (cat == null) cat = "Uncategorized";
            double total = c.isNull(c.getColumnIndexOrThrow("total")) ? 0.0 : c.getDouble(c.getColumnIndexOrThrow("total"));
            out.put(cat, total);
        }
        c.close();
        return out;
    }

    public List<Pair<Long, Double>> getMonthlyNet(long userId, long startTs, long endTs) {
        List<Pair<Long, Double>> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT strftime('%Y-%m', datetime(timestamp/1000,'unixepoch')) as ym, SUM(CASE WHEN is_expense=1 THEN -amount ELSE amount END) as net FROM " + T_TRANSACTIONS + " WHERE user_id=? AND timestamp>=? AND timestamp<=? GROUP BY ym ORDER BY ym ASC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(startTs), String.valueOf(endTs)});
        while (c.moveToNext()) {
            String ym = c.getString(c.getColumnIndexOrThrow("ym")); // "2025-07"
            double net = c.isNull(c.getColumnIndexOrThrow("net")) ? 0.0 : c.getDouble(c.getColumnIndexOrThrow("net"));
            String[] parts = ym.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]); // 1..12
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            out.add(new Pair<>(cal.getTimeInMillis(), net));
        }
        c.close();
        return out;
    }


}
