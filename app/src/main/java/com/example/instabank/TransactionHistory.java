package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TransactionHistory extends AppCompatActivity {

    private ListView listViewTransactions;
    private DatabaseReference transactionsRef;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private List<Transaction> transactionList;
    private TransactionAdapter adapter; // Custom adapter
    private String userFirstName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transactionhistory);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            transactionsRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(currentUser.getUid())
                    .child("transactions");
            usersRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(currentUser.getUid());
            fetchUserFirstName();
        }

        // Initialize UI elements
        listViewTransactions = findViewById(R.id.listViewTransactions);

        // Initialize transaction list and adapter
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(transactionList);
        listViewTransactions.setAdapter(adapter);

        // Load transactions from Firebase
        loadTransactions();

        // Set item click listener
        listViewTransactions.setOnItemClickListener((parent, view, position, id) -> {
            Transaction transaction = transactionList.get(position);
            Intent intent = new Intent(TransactionHistory.this, ReceiptActivity.class);
            intent.putExtra("name", transaction.getName());
            intent.putExtra("accountNumber", transaction.getAccountNumber());
            intent.putExtra("amount", transaction.getAmount());
            intent.putExtra("source", transaction.getSource());
            intent.putExtra("timestamp", transaction.getTimestamp());
            intent.putExtra("referenceId", transaction.getReferenceId()); // Include reference ID
            startActivity(intent);
        });
    }

    private void fetchUserFirstName() {
        if (usersRef != null) {
            usersRef.child("firstName").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userFirstName = dataSnapshot.getValue(String.class);
                    if (userFirstName == null) {
                        userFirstName = "Unknown"; // Default value if first name is not found
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(TransactionHistory.this, "Failed to load user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadTransactions() {
        if (transactionsRef != null) {
            transactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    transactionList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String accountNumber = snapshot.child("accountNumber").getValue(String.class);
                        Double amount = snapshot.child("amount").getValue(Double.class);
                        String source = snapshot.child("source").getValue(String.class);
                        Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        String referenceId = snapshot.child("referenceId").getValue(String.class); // Retrieve referenceId

                        if (name == null || name.isEmpty()) {
                            name = userFirstName;
                        }

                        if (accountNumber != null && amount != null && source != null && timestamp != null && referenceId != null) {
                            Transaction transaction = new Transaction(name, accountNumber, amount, source, timestamp, referenceId);
                            transactionList.add(transaction);
                        }
                    }

                    // Sort transactions by timestamp (most recent first)
                    Collections.sort(transactionList, new Comparator<Transaction>() {
                        @Override
                        public int compare(Transaction t1, Transaction t2) {
                            return Long.compare(t2.getTimestamp(), t1.getTimestamp());
                        }
                    });

                    // Update adapter data
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(TransactionHistory.this, "Failed to load transactions: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Custom adapter for transactions
    private class TransactionAdapter extends ArrayAdapter<Transaction> {

        public TransactionAdapter(List<Transaction> transactions) {
            super(TransactionHistory.this, R.layout.list_item_transaction, transactions);
        }


        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_transaction, parent, false);
            }

            Transaction transaction = getItem(position);
            if (transaction == null) {
                return convertView;
            }

            // Bind data to views in list item layout
            TextView textViewDate = convertView.findViewById(R.id.textViewDate);
            TextView textViewTime = convertView.findViewById(R.id.textViewTime);
            TextView textViewName = convertView.findViewById(R.id.textViewName);
            TextView textViewAmount = convertView.findViewById(R.id.textViewAmount);

            // Format date and time
            String formattedDate = getFormattedDate(transaction.getTimestamp());
            String formattedTime = getFormattedTime(transaction.getTimestamp());

            // Set values to views
            textViewDate.setText(formattedDate);
            textViewTime.setText(formattedTime);
            textViewName.setText(transaction.getName());

            // Display amount with a plus sign (+) for positive amounts
            if (transaction.getAmount() >= 0) {
                textViewAmount.setText(String.format(Locale.getDefault(), "+%.2f", transaction.getAmount()));
                textViewAmount.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_green_dark));
            } else {
                // Negative amount (deducted)
                textViewAmount.setText(String.format(Locale.getDefault(), "%.2f", transaction.getAmount()));
                textViewAmount.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
            }

            return convertView;
        }

    }

    private String getFormattedDate(long timestamp) {
        if (timestamp <= 0) {
            return ""; // Handle invalid timestamp
        }

        // Convert timestamp to date format
        Date date = new Date(timestamp);
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault()); // Set local time zone
        return dateFormat.format(date);
    }

    private String getFormattedTime(long timestamp) {
        if (timestamp <= 0) {
            return ""; // Handle invalid timestamp
        }

        // Convert timestamp to time format
        Date date = new Date(timestamp);
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getDefault()); // Set local time zone
        return timeFormat.format(date);
    }

    // Transaction model class
    public static class Transaction {
        private String name;
        private String accountNumber;
        private double amount;
        private String source;
        private long timestamp;
        private String referenceId; // Add referenceId field

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
}