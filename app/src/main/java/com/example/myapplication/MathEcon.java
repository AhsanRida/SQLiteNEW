package com.example.myapplication;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.*;
import java.time.temporal.ChronoUnit;


/**
 * MathEcon = analytics & simple forecasting utilities for micro-business finance.
 * - Aggregations (monthly net, category breakdowns)
 * - KPIs (income, expense, net, margin)
 * - Moving averages
 * - Linear regression (trend, R^2) + naive next-month forecast
 * - Burn rate (avg daily expense) & cash runway estimator
 *
 * Keep this class UI-agnostic; return primitives & Lists. Your Activities decide how to render.
 */
public final class MathEcon {

    private MathEcon() {}

    /* =========================
     * Time Window Helpers
     * ========================= */
    public static long startOfMonthMillis(int year, int month1to12) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month1to12 - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long endOfMonthMillis(int year, int month1to12) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month1to12 - 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    /** Returns an array of [startMillis, endMillis] for the last N months including current month. */
    public static List<Pair<Long, Long>> lastNMonthsWindows(int n) {
        List<Pair<Long, Long>> out = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH) + 1; // 1..12
        for (int i = n - 1; i >= 0; i--) {
            int m = curMonth - i;
            int y = curYear;
            while (m <= 0) { m += 12; y -= 1; }
            long s = startOfMonthMillis(y, m);
            long e = endOfMonthMillis(y, m);
            out.add(new Pair<>(s, e));
        }
        return out;
    }

    /* =========================
     * Core KPIs
     * ========================= */

    /** Returns income, expense, net for a period. */
    public static Totals totalsForPeriod(DBHelper db, long userId, long fromTs, long toTs) {
        List<Transaction> txns = db.getTransactions(userId, fromTs, toTs);
        double income = 0, expense = 0;
        for (Transaction t : txns) {
            if (t.isExpense()) expense += t.getAmount();
            else income += t.getAmount();
        }
        double net = income - expense;
        double margin = (income > 1e-9) ? (net / income) : 0.0;
        return new Totals(income, expense, net, margin);
    }

    public static class Totals {
        public final double income, expense, net, profitMargin;
        public Totals(double income, double expense, double net, double profitMargin) {
            this.income = income; this.expense = expense; this.net = net; this.profitMargin = profitMargin;
        }
        @Override public String toString() {
            return String.format(Locale.getDefault(),
                    "Income=%.2f, Expense=%.2f, Net=%.2f, Margin=%.1f%%",
                    income, expense, net, 100*profitMargin);
        }
    }

    /** Returns monthly net series (month-start millis, net) for last N months, ascending by month. */
    public static List<Pair<Long, Double>> monthlyNetSeries(DBHelper db, long userId, int lastNMonths) {
        // DBHelper already has getMonthlyNet(startTs, endTs). Just compute a window large enough.
        List<Pair<Long, Long>> windows = lastNMonthsWindows(lastNMonths);
        long startTs = windows.get(0).first;
        long endTs = windows.get(windows.size() - 1).second;

        // getMonthlyNet returns month-start millis paired with net for months that have data.
        List<Pair<Long, Double>> raw = db.getMonthlyNet(userId, startTs, endTs);

        // Fill missing months with 0 to keep the chart continuous
        Map<Long, Double> map = new LinkedHashMap<>();
        for (Pair<Long, Long> w : windows) {
            map.put(monthKey(w.first), 0.0);
        }
        for (Pair<Long, Double> p : raw) {
            map.put(monthKey(p.first), p.second);
        }
        List<Pair<Long, Double>> out = new ArrayList<>();
        for (Map.Entry<Long, Double> e : map.entrySet()) out.add(new Pair<>(e.getKey(), e.getValue()));
        // Already in chronological order due to insertion order.
        return out;
    }

    private static long monthKey(long monthStartMillis) {
        // normalize to 00:00:00.000 of the 1st (safety)
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(monthStartMillis);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Expense totals by category for a period, sorted desc. */
    public static List<Pair<String, Double>> expenseByCategoryList(DBHelper db, long userId, long fromTs, long toTs) {
        Map<String, Double> map = db.getExpensesByCategory(userId, fromTs, toTs);
        List<Pair<String, Double>> list = new ArrayList<>();
        for (Map.Entry<String, Double> e : map.entrySet()) list.add(new Pair<>(e.getKey(), e.getValue()));
        Collections.sort(list, (a, b) -> Double.compare(b.second, a.second));
        return list;
    }

    /* =========================
     * Moving Average
     * ========================= */

    /** Simple moving average across monthly net; window >= 1. Returns same x with smoothed y. */
    public static List<Pair<Long, Double>> movingAverage(List<Pair<Long, Double>> series, int window) {
        if (window <= 1 || series.isEmpty()) return new ArrayList<>(series);
        List<Pair<Long, Double>> out = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < series.size(); i++) {
            sum += series.get(i).second;
            if (i >= window) sum -= series.get(i - window).second;
            double avg = (i >= window - 1) ? sum / window : sum / (i + 1);
            out.add(new Pair<>(series.get(i).first, avg));
        }
        return out;
    }

    /* =========================
     * Linear Regression & Forecast
     * ========================= */

    /** Fit y = a + b*x over the series. x = month index (0..n-1). */
    public static RegressionResult regressMonthlyNet(List<Pair<Long, Double>> series) {
        int n = series.size();
        if (n == 0) return new RegressionResult(0, 0, 0);
        // x: 0..n-1 ; y: net
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, sumYY = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = series.get(i).second;
            sumX += x; sumY += y; sumXY += x*y; sumXX += x*x; sumYY += y*y;
        }
        double denom = (n*sumXX - sumX*sumX);
        double b = (denom == 0) ? 0 : (n*sumXY - sumX*sumY) / denom; // slope
        double a = (sumY - b*sumX) / n; // intercept

        // R^2
        double ssTot = sumYY - (sumY*sumY)/n;
        double ssRes = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = series.get(i).second;
            double yhat = a + b*x;
            ssRes += (y - yhat) * (y - yhat);
        }
        double r2 = (ssTot <= 1e-12) ? 0 : 1 - ssRes/ssTot;
        return new RegressionResult(a, b, r2);
    }

    public static class RegressionResult {
        public final double interceptA, slopeB, r2;
        public RegressionResult(double a, double b, double r2) {
            this.interceptA = a; this.slopeB = b; this.r2 = r2;
        }
        @Override public String toString() {
            return String.format(Locale.getDefault(), "y = %.2f + %.2f*x (R²=%.3f)", interceptA, slopeB, r2);
        }
    }

    /** Naive forecast of next month’s net using regression. */
    public static double forecastNextMonthNet(List<Pair<Long, Double>> monthlyNetSeries) {
        int n = monthlyNetSeries.size();
        if (n == 0) return 0;
        RegressionResult rr = regressMonthlyNet(monthlyNetSeries);
        double nextX = n; // next month index
        return rr.interceptA + rr.slopeB*nextX;
    }

    /* =========================
     * Burn Rate & Runway
     * ========================= */

    /** Average daily expense over the last N days. */
    public static double averageDailyBurn(DBHelper db, long userId, int lastNDays) {
        long now = System.currentTimeMillis();
        long from = now - lastNDays * 24L * 60L * 60L * 1000L;

        List<Transaction> txns = db.getTransactions(userId, from, now);
        double totalExpense = 0;
        for (Transaction t : txns) if (t.isExpense()) totalExpense += t.getAmount();
        return totalExpense / Math.max(1, lastNDays);
    }

    /** Estimated runway in days given currentBalance and avg daily burn. */
    public static double cashRunwayDays(double currentBalance, double avgDailyBurn) {
        if (avgDailyBurn <= 1e-9) return Double.POSITIVE_INFINITY;
        return Math.max(0, currentBalance / avgDailyBurn);
    }

    /* =========================
     * Convenience for current month/an easy pie model
     * ========================= */

    /** Returns a simple model of "label -> value" for pie use. */
    public static List<Pair<String, Double>> currentMonthCategoryExpenseModel(DBHelper db, long userId) {
        Calendar s = Calendar.getInstance();
        s.set(Calendar.DAY_OF_MONTH, 1);
        s.set(Calendar.HOUR_OF_DAY, 0);
        s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0);
        s.set(Calendar.MILLISECOND, 0);
        Calendar e = Calendar.getInstance();
        e.set(Calendar.HOUR_OF_DAY, 23);
        e.set(Calendar.MINUTE, 59);
        e.set(Calendar.SECOND, 59);
        e.set(Calendar.MILLISECOND, 999);
        return expenseByCategoryList(db, userId, s.getTimeInMillis(), e.getTimeInMillis());
    }
    // -----------------------
// Basic finance formulas
// -----------------------

    /** Percentage Loss = (Purchase Price - Sale Price) / Purchase Price * 100 */
    public static double percentageLoss(double purchasePrice, double salePrice) {
        if (purchasePrice == 0) return 0; // avoid /0; define as 0 by convention
        return ((purchasePrice - salePrice) / purchasePrice) * 100.0;
    }

    /** Percentage Increase = ((Final Value - Initial Value) / Initial Value) * 100 */
    public static double percentageIncrease(double initialValue, double finalValue) {
        if (initialValue == 0) return (finalValue > 0 ? 100.0 : 0.0); // define convention for 0→positive
        return ((finalValue - initialValue) / initialValue) * 100.0;
    }

    /** Sum helper that ignores nulls and NaNs. */
    private static double safeSum(double... values) {
        double sum = 0.0;
        if (values == null) return 0.0;
        for (double v : values) {
            if (!Double.isNaN(v) && !Double.isInfinite(v)) sum += v;
        }
        return sum;
    }

    /** Total Liabilities = Loans + Mortgage + Bills + ... */
    public static double totalLiabilities(double... items) {
        return safeSum(items);
    }

    /** Total Assets = Cash + Investments + Property + ... */
    public static double totalAssets(double... items) {
        return safeSum(items);
    }
    public static String fmtPct(double v) { return String.format(java.util.Locale.getDefault(), "%.1f%%", v); }
    public static String fmtMoney(double v) { return String.format(java.util.Locale.getDefault(), "%.2f", v); }

}
