package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.Random;

public class RewardsActivity extends AppCompatActivity {

    private TextView textViewRewardsAmount;
    private EditText editTextAddAmount;
    private Button buttonAddToBalance;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private ValueEventListener rewardsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        textViewRewardsAmount = findViewById(R.id.textViewRewardsAmount);
        editTextAddAmount = findViewById(R.id.editTextAddAmount);
        buttonAddToBalance = findViewById(R.id.buttonAddToBalance);

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



        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Fetch and display rewards in real-time
        attachRewardsListener();

        // Set click listener for Add to Balance button
        buttonAddToBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToUserBalance();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rewardsListener != null) {
            usersRef.removeEventListener(rewardsListener);
        }
    }

    private void attachRewardsListener() {
        rewardsListener = usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Double rewardsAmount = snapshot.child("rewards").getValue(Double.class);
                            if (rewardsAmount == null) {
                                rewardsAmount = 0.0;
                            }
                            textViewRewardsAmount.setText(String.format(" ₱%.2f", rewardsAmount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(RewardsActivity.this, "Failed to load rewards", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToUserBalance() {
        String amountString = editTextAddAmount.getText().toString().trim();

        if (amountString.isEmpty()) {
            editTextAddAmount.setError("Amount is required");
            editTextAddAmount.requestFocus();
            return;
        }

        double amount = Double.parseDouble(amountString);

        if (amount < 100) {
            showDialogWarning("Minimum Amount Required", "You must add at least ₱100 in rewards to your balance.");
            return;
        }

        if (amount > 1000) {
            showDialogWarning("Exceeded Maximum Amount", "You can only add up to ₱1000 in rewards to your balance.");
            return;
        }

        if (currentUser != null) {
            usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);

                                Double rewardsBalance = snapshot.child("rewards").getValue(Double.class);
                                if (rewardsBalance == null) {
                                    rewardsBalance = 0.0;
                                }

                                if (rewardsBalance >= amount) {
                                    // Deduct from rewards balance
                                    double newRewardsBalance = rewardsBalance - amount;
                                    snapshot.getRef().child("rewards").setValue(newRewardsBalance)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    // Proceed to add amount to user's balance
                                                    Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                                    if (currentBalance == null) {
                                                        currentBalance = 0.0;
                                                    }

                                                    double newBalance = currentBalance + amount;
                                                    // Update balance in Firebase
                                                    snapshot.getRef().child("balance").setValue(newBalance)
                                                            .addOnCompleteListener(balanceTask -> {
                                                                if (balanceTask.isSuccessful()) {
                                                                    String referenceId = generateReferenceId();
                                                                    recordTransactions(phoneNumber, "Rewards", amount, referenceId);

                                                                    // Calculate and add rewards (if any)
                                                                    double rewardAmount = amount * 0.02;
                                                                    if (rewardAmount > 0) {

                                                                    }

                                                                    Toast.makeText(RewardsActivity.this, "Balance updated successfully", Toast.LENGTH_SHORT).show();

                                                                    // Show the receipt
                                                                    showReceipts("Rewards Transferred to Current Balance", phoneNumber, amount, "Rewards", referenceId);

                                                                    // Clear input field
                                                                    editTextAddAmount.setText("");

                                                                } else {
                                                                    Toast.makeText(RewardsActivity.this, "Failed to update balance: " + balanceTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(RewardsActivity.this, "Failed to deduct from rewards: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    showInsufficientBalanceDialog();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(RewardsActivity.this, "Failed to load rewards balance: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Method to show a warning dialog
    private void showDialogWarning(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }


    // Method to generate a unique reference ID
    private String generateReferenceId() {
        Random random = new Random();
        long referenceId = 100000000000L + (long)(random.nextDouble() * 900000000000L); // Ensure a 12-digit random number
        return String.valueOf(referenceId);
    }

    // Method to record transaction
    private void recordTransactions(String phoneNumber, String source, double amount, String referenceId) {
        // Create a new transaction entry in Firebase with server timestamp
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.getUid())
                .child("transactions");

        String transactionId = transactionsRef.push().getKey();
        if (transactionId != null) {
            long timestamp = System.currentTimeMillis(); // Current timestamp
            Transaction transaction = new Transaction("Rewards Transfered to Current Balance", phoneNumber, amount, source, timestamp, referenceId);
            transactionsRef.child(transactionId).setValue(transaction);
        }
    }

    // Method to show receipt or transaction details
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

    private void showInsufficientBalanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Insufficient Balance");
        builder.setMessage("Your current balance is insufficient for this transaction.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }


}
