package com.example.instabank;

public class Transaction {
    private String name;
    private String accountNumber;
    private double amount;
    private String source;
    private long timestamp;
    private String referenceId;

    public Transaction(String name, String accountNumber, double amount, String source, long timestamp, String referenceId) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.source = source;
        this.timestamp = timestamp;
        this.referenceId = referenceId;
    }

    public String getName() {
        return name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getAmount() {
        return amount;
    }

    public String getSource() {
        return source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
