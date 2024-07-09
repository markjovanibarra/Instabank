package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SavingsActivity extends AppCompatActivity {

    private static final String TAG = "SavingsActivity";

    private TextView textViewSavingsBalancePHP;
    private EditText editTextSavingsAmount;
    private Button buttonSaveAmount;
    private Button buttonTransferToCurrent;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private double savingsBalancePHP;
    private final double interestRate = 0.0001; // 0.01% interest rate
    private final long interestUpdateInterval = 60 * 1000; // 1 minute interval
    private Handler handler;
    private Runnable interestRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_start);

        // Initialize views
        textViewSavingsBalancePHP = findViewById(R.id.textViewSavingsBalancePHP);
        editTextSavingsAmount = findViewById(R.id.editTextSavingsAmount);
        buttonSaveAmount = findViewById(R.id.buttonSaveAmount);
        buttonTransferToCurrent = findViewById(R.id.buttonTransferToCurrent);

        // Initialize Firebase Database reference
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            fetchUserSavings();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        // Handle save amount button click
        buttonSaveAmount.setOnClickListener(v -> {
            String amountStr = editTextSavingsAmount.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                if (amount >= 100.0) {
                    saveAmount(amount);
                } else {
                    showAlertDialog("Invalid Amount", "The minimum amount to save is ₱100.");
                }
            } else {
                Toast.makeText(SavingsActivity.this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle transfer to current balance button click
        buttonTransferToCurrent.setOnClickListener(v -> transferToCurrentBalance());

        // Initialize handler for periodic interest update
        handler = new Handler();
        interestRunnable = new Runnable() {
            @Override
            public void run() {
                applyInterest(); // Apply interest every 1 minute
                handler.postDelayed(this, interestUpdateInterval);
            }
        };

        Button buttonTransactions = findViewById(R.id.buttonOption2);
        Button buttonHome = findViewById(R.id.btn_home);
        Button buttonProfile = findViewById(R.id.btn_profile);

        buttonTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to TransactionHistoryActivity
                Intent intent = new Intent(getApplicationContext(), TransactionHistory.class);
                startActivity(intent);
            }
        });

        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to DashboardActivity
                Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(intent);
            }
        });

        buttonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ProfileActivity
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start periodic interest update when activity is resumed
        handler.postDelayed(interestRunnable, interestUpdateInterval);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop periodic interest update when activity is paused
        handler.removeCallbacks(interestRunnable);
    }

    // Fetch user savings from Firebase
    private void fetchUserSavings() {
        String phoneNumber = currentUser.getPhoneNumber();
        if (phoneNumber != null) {
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
                phoneNumber = "+63" + phoneNumber; // Modify this according to your country code
            }

            usersRef = FirebaseDatabase.getInstance().getReference().child("users");
            usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    Double balancePHP = snapshot.child("savingsBalancePHP").getValue(Double.class);
                                    if (balancePHP == null) {
                                        balancePHP = 0.00;
                                        snapshot.getRef().child("savingsBalancePHP").setValue(balancePHP);
                                    }
                                    savingsBalancePHP = balancePHP;
                                    textViewSavingsBalancePHP.setText("₱" + String.format("%.2f", savingsBalancePHP));
                                    break;
                                }
                            } else {
                                Log.e(TAG, "User with phone number not found");
                                Toast.makeText(SavingsActivity.this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "DatabaseError: " + databaseError.getMessage());
                            Toast.makeText(SavingsActivity.this, "Failed to get savings", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e(TAG, "Phone number is null");
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Save amount to savings and update Firebase
    // Save amount to savings and update Firebase
    private void saveAmount(double amount) {
        // Check if amount exceeds the maximum limit (50,000 PHP)
        if (amount > 50000.0) {
            // Show dialog for exceeding maximum limit
            new AlertDialog.Builder(this)
                    .setTitle("Maximum Limit Exceeded")
                    .setMessage("You can only save up to ₱50,000.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        // Proceed with saving amount if within limit
        usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);
                            // Fetch current balance
                            Double currentBalance = snapshot.child("balance").getValue(Double.class);
                            if (currentBalance == null) {
                                currentBalance = 0.0;
                            }

                            // Check if sufficient balance in main balance
                            if (currentBalance >= amount) {
                                // Deduct amount from balance
                                double newBalance = currentBalance - amount;
                                snapshot.getRef().child("balance").setValue(newBalance)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                // Update savings balance
                                                savingsBalancePHP += amount;

                                                // Update savings balance in Firebase
                                                snapshot.getRef().child("savingsBalancePHP").setValue(savingsBalancePHP)
                                                        .addOnCompleteListener(task1 -> {
                                                            if (task1.isSuccessful()) {
                                                                runOnUiThread(() -> {
                                                                    textViewSavingsBalancePHP.setText(" ₱" + String.format("%.2f", savingsBalancePHP));
                                                                    Toast.makeText(SavingsActivity.this, "Amount saved successfully", Toast.LENGTH_SHORT).show();
                                                                    // Generate a unique reference ID
                                                                    String referenceId = generateReferenceId();
                                                                    recordTransaction(phoneNumber, "Savings", -amount, referenceId);
                                                                    // Show the receipt
                                                                    showReceipt("Savings", phoneNumber, amount, "Savings", referenceId);
                                                                });
                                                            } else {
                                                                Log.e(TAG, "Failed to save amount to savings: " + task1.getException());
                                                                Toast.makeText(SavingsActivity.this, "Failed to save amount to savings", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                Log.e(TAG, "Failed to deduct balance: " + task.getException());
                                                Toast.makeText(SavingsActivity.this, "Failed to deduct balance", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(SavingsActivity.this, "Insufficient balance", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "DatabaseError: " + databaseError.getMessage());
                        Toast.makeText(SavingsActivity.this, "Failed to deduct balance", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    // Transfer savings balance to current balance
    // Transfer savings balance to current balance with user input for amount
    // Transfer savings to current balance and update Firebase
    private void transferToCurrentBalance() {
        // Create an EditText for user input
        EditText input = new EditText(SavingsActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Set maximum length filter
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });

        // Create an AlertDialog to prompt for amount input
        new AlertDialog.Builder(SavingsActivity.this)
                .setTitle("Transfer to Current Balance")
                .setMessage("Enter amount to transfer")
                .setView(input)
                .setPositiveButton("Transfer", (dialog, which) -> {
                    String amountStr = input.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        if (amount >= 100.0 && amount <= 50000.0) {
                            if (amount <= savingsBalancePHP) {
                                // Proceed with the transfer logic
                                usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                    String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);

                                                    // Fetch current balance
                                                    Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                                    if (currentBalance == null) {
                                                        currentBalance = 0.0;
                                                    }

                                                    // Add savings balance to current balance
                                                    double newBalance = currentBalance + amount;

                                                    // Update current balance in Firebase
                                                    snapshot.getRef().child("balance").setValue(newBalance)
                                                            .addOnCompleteListener(task -> {
                                                                if (task.isSuccessful()) {
                                                                    String referenceId = generateReferenceId();
                                                                    recordTransactions(phoneNumber, "Savings Transfer", amount, referenceId);
                                                                    // Show the receipt
                                                                    showReceipts("Transferred to Current Balance", phoneNumber, amount, "Savings", referenceId);
                                                                    // Deduct transferred amount from savings balance
                                                                    savingsBalancePHP -= amount;

                                                                    // Update savings balance in Firebase
                                                                    snapshot.getRef().child("savingsBalancePHP").setValue(savingsBalancePHP)
                                                                            .addOnCompleteListener(task1 -> {
                                                                                if (task1.isSuccessful()) {
                                                                                    runOnUiThread(() -> {
                                                                                        textViewSavingsBalancePHP.setText("Savings Balance (PHP): ₱" + String.format("%.2f", savingsBalancePHP));
                                                                                    });
                                                                                } else {
                                                                                    Log.e(TAG, "Failed to update savings balance: " + task1.getException());
                                                                                    showAlertDialog("Error", "Failed to update savings balance.");
                                                                                }
                                                                            });
                                                                } else {
                                                                    Log.e(TAG, "Failed to update current balance: " + task.getException());
                                                                    showAlertDialog("Error", "Failed to update current balance.");
                                                                }
                                                            });
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                Log.e(TAG, "DatabaseError: " + databaseError.getMessage());
                                                showAlertDialog("Error", "Failed to transfer savings.");
                                            }
                                        });
                            } else {
                                showAlertDialog("Invalid Amount", "Invalid amount or insufficient savings balance.");
                            }
                        } else {
                            showAlertDialog("Invalid Amount", "Amount must be between 100 and 50,000 PHP.");
                        }
                    } else {
                        showAlertDialog("Invalid Amount", "Enter a valid amount.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    // Apply interest to savings balance
    private void applyInterest() {
        if (savingsBalancePHP > 0.00) {
            savingsBalancePHP += savingsBalancePHP * interestRate;

            Map<String, Object> updates = new HashMap<>();
            updates.put("savingsBalancePHP", round(savingsBalancePHP, 2));

            usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().updateChildren(updates).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        runOnUiThread(() -> {
                                            textViewSavingsBalancePHP.setText("Savings Balance (PHP): ₱" + String.format("%.2f", savingsBalancePHP));
                                            Toast.makeText(SavingsActivity.this, "Interest applied successfully", Toast.LENGTH_SHORT).show();
                                        });
                                    } else {
                                        Log.e(TAG, "Failed to apply interest: " + task.getException());
                                        Toast.makeText(SavingsActivity.this, "Failed to apply interest", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "DatabaseError: " + databaseError.getMessage());
                            Toast.makeText(SavingsActivity.this, "Failed to apply interest", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Helper method to round double values
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void recordTransaction(String phoneNumber, String source, double amount, String referenceId) {
        // Create a new transaction entry in Firebase with server timestamp
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.getUid())
                .child("transactions");

        String transactionId = transactionsRef.push().getKey();
        if (transactionId != null) {
            long timestamp = System.currentTimeMillis(); // Current timestamp
            Transaction transaction = new Transaction("Savings", phoneNumber, amount, source, timestamp, referenceId);
            transactionsRef.child(transactionId).setValue(transaction);
        }
    }

    private void recordTransactions(String phoneNumber, String source, double amount, String referenceId) {
        // Create a new transaction entry in Firebase with server timestamp
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.getUid())
                .child("transactions");

        String transactionId = transactionsRef.push().getKey();
        if (transactionId != null) {
            long timestamp = System.currentTimeMillis(); // Current timestamp
            Transaction transaction = new Transaction("Transfered to Current Balance", phoneNumber, amount, source, timestamp, referenceId);
            transactionsRef.child(transactionId).setValue(transaction);
        }
    }

    // Show an AlertDialog with a custom message
    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }





    private String generateReferenceId() {
        Random random = new Random();
        long referenceId = 100000000000L + (long)(random.nextDouble() * 900000000000L); // Ensure a 12-digit random number
        return String.valueOf(referenceId);
    }
    private void showReceipt(String name, String accountNumber, double amount, String source, String referenceId) {
        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("accountNumber", accountNumber);
        intent.putExtra("amount", amount);
        intent.putExtra("source", source);
        long timestamp = System.currentTimeMillis(); // Use current timestamp for the receipt
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("referenceId", referenceId);
        startActivity(intent);
    }

    private void showReceipts(String name, String accountNumber, double savingsBalancePHP, String source, String referenceId) {
        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("accountNumber", accountNumber);
        intent.putExtra("amount", savingsBalancePHP);
        intent.putExtra("source", source);
        long timestamp = System.currentTimeMillis(); // Use current timestamp for the receipt
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("referenceId", referenceId);
        startActivity(intent);
    }



}