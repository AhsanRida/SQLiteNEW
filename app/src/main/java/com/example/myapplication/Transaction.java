package com.example.myapplication;

public class Transaction {
    private int id;
    private int userId;
    private Integer categoryId;
    private double amount;
    private boolean isExpense;
    private String note;
    private long timestamp;
    public Transaction(int id, int userId, Integer categoryId, double amount, boolean isExpense, String note, long timestamp) {
        this.id=id; this.userId=userId; this.categoryId=categoryId; this.amount=amount; this.isExpense=isExpense; this.note=note; this.timestamp=timestamp;
    }
    public int getId(){ return id; }
    public int getUserId(){ return userId; }
    public Integer getCategoryId(){ return categoryId; }
    public double getAmount(){ return amount; }
    public boolean isExpense(){ return isExpense; }
    public String getNote(){ return note; }
    public long getTimestamp(){ return timestamp; }
}