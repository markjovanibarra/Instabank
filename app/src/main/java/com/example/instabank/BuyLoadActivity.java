package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class BuyLoadActivity extends AppCompatActivity {

    private EditText editTextPhoneNumber, editTextAmount;
    private Spinner spinnerSim;
    private Button buttonPay;
    private DatabaseReference transactionsRef;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buyload);

        // Initialize Firebase
        transactionsRef = FirebaseDatabase.getInstance().getReference().child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("transactions");
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize UI elements
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerSim = findViewById(R.id.spinnerSim);
        buttonPay = findViewById(R.id.buttonPay);

        // Populate SIM spinner
        ArrayAdapter<CharSequence> simAdapter = ArrayAdapter.createFromResource(this,
                R.array.sim_options, android.R.layout.simple_spinner_item);
        simAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSim.setAdapter(simAdapter);

        buttonPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = editTextPhoneNumber.getText().toString().trim();
                String amountStr = editTextAmount.getText().toString().trim();

                if (!isValidPhoneNumber(phoneNumber)) {
                    showDialogWarning("Invalid Phone Number", "Please enter a valid 11-digit phone number starting with '09'.");
                    return;
                }

                if (amountStr.isEmpty()) {
                    showDialogWarning("Missing Amount", "Please enter amount to load.");
                    return;
                }

                double amount = Double.parseDouble(amountStr);

                if (amount < 5 || amount > 1000) {
                    showDialogWarning("Invalid Amount", "Please enter an amount between 100 and 1000.");
                    return;
                }

                // Get selected SIM name
                String selectedSim = spinnerSim.getSelectedItem().toString();

                // Check if user has sufficient balance
                usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                        if (currentBalance != null && currentBalance >= amount) {
                                            // Sufficient balance, proceed with payment
                                            String referenceId = generateReferenceId();
                                            recordTransaction("Regular Load", amount, selectedSim, referenceId);
                                            showReceipt("Regular Load", phoneNumber, amount, selectedSim, referenceId);
                                            updateBalance(-amount); // Deduct amount from balance

                                            // Calculate and add rewards
                                            double rewardAmount = amount * 0.02;
                                            addRewardsToUser(rewardAmount);

                                            Toast.makeText(BuyLoadActivity.this, "Purchased: Regular Load Amount: ₱" + amount, Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Insufficient balance
                                            showDialogWarning("Insufficient Balance", "Your current balance is not sufficient to make this purchase.");
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(BuyLoadActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });




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

    // Method to validate phone number
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Check if it starts with "09" and is exactly 11 digits
        return phoneNumber.matches("^09\\d{9}$");
    }

    // Method to show a warning dialog
    private void showDialogWarning(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Method to record transaction in Firebase
    private void recordTransaction(String type, double amount, String telco, String referenceId) {
        if (currentUser != null) {
            String phoneNumber = currentUser.getPhoneNumber();
            String accountNumber = editTextPhoneNumber.getText().toString().trim(); // Get user inputted account number

            if (phoneNumber != null && !accountNumber.isEmpty()) {
                // Record transaction with server timestamp
                Transaction transaction = new Transaction("Buy A Load", accountNumber, -amount, type + " (" + telco + ")", System.currentTimeMillis(), referenceId);
                transactionsRef.push().setValue(transaction);
            } else {
                Toast.makeText(BuyLoadActivity.this, "Account number is required", Toast.LENGTH_SHORT).show();
            }
        }
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

    // Method to update user balance in Firebase
    private void updateBalance(double changeAmount) {
        if (currentUser != null) {
            usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                    if (currentBalance != null) {
                                        double newBalance = currentBalance + changeAmount;
                                        snapshot.getRef().child("balance").setValue(newBalance)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // Balance updated successfully
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(BuyLoadActivity.this, "Failed to update balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(BuyLoadActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Method to generate a unique reference ID
    private String generateReferenceId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder referenceId = new StringBuilder();
        Random rnd = new Random();
        while (referenceId.length() < 8) { // length of the reference ID
            int index = (int) (rnd.nextFloat() * chars.length());
            referenceId.append(chars.charAt(index));
        }
        return referenceId.toString();
    }

    // Method to add rewards to user
    private void addRewardsToUser(double rewardAmount) {
        if (currentUser != null) {
            usersRef.child(currentUser.getUid()).child("rewards")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Double currentRewards = snapshot.getValue(Double.class);
                            if (currentRewards == null) {
                                currentRewards = 0.0;
                            }
                            double newRewards = currentRewards + rewardAmount;
                            snapshot.getRef().setValue(newRewards)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Rewards updated successfully
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(BuyLoadActivity.this, "Failed to update rewards: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(BuyLoadActivity.this, "Failed to update rewards: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
