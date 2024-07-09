package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class Cashin extends AppCompatActivity {

    private EditText editTextAmount;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cashin);

        editTextAmount = findViewById(R.id.editTextAmount);
        Button buttonConfirm = findViewById(R.id.buttonConfirm);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = editTextAmount.getText().toString().trim();
                if (!amountStr.isEmpty()) {
                    // Validate amount input
                    if (isValidAmount(amountStr)) {
                        double amount = Double.parseDouble(amountStr);

                        // Validate maximum amount ($50,000) and minimum amount ($10)
                        if (amount <= 50000.0 && amount >= 10.0) {
                            // Proceed with cash-in operation
                            if (currentUser != null) {
                                String phoneNumber = currentUser.getPhoneNumber();
                                if (phoneNumber != null) {
                                    usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (dataSnapshot.exists()) {
                                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                            Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                                            if (currentBalance != null) {
                                                                double newBalance = currentBalance + amount;
                                                                // Update balance in Firebase
                                                                snapshot.getRef().child("balance").setValue(newBalance)
                                                                        .addOnCompleteListener(task -> {
                                                                            if (task.isSuccessful()) {
                                                                                // Generate a unique reference ID
                                                                                String referenceId = generateReferenceId();

                                                                                // Record transaction with server timestamp
                                                                                recordTransaction(phoneNumber, amount, referenceId);

                                                                                // Calculate and add rewards
                                                                                double rewardAmount = amount * 0.02;
                                                                                addRewardsToUser(rewardAmount);

                                                                                // Show the receipt
                                                                                showReceipt("Cash In", phoneNumber, amount, "Cash In", referenceId);

                                                                                // Finish activity
                                                                                finish();
                                                                            } else {
                                                                                showToast("Failed to update balance: " + task.getException().getMessage());
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    } else {
                                                        showToast("User not found");
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    showToast("Failed to update balance: " + databaseError.getMessage());
                                                }
                                            });
                                }
                            }
                        } else if (amount < 10.0) {
                            // Show error if amount is below $10 using AlertDialog
                            showMinAmountExceededDialog();
                        } else {
                            // Show error if amount exceeds $50,000 using AlertDialog
                            showMaxAmountExceededDialog();
                        }
                    } else {
                        showInvalidAmountDialog();
                    }
                } else {
                    editTextAmount.setError("Please enter an amount");
                }
            }
        });
    }

    private void showMaxAmountExceededDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Maximum Amount Exceeded");
        builder.setMessage("Maximum cash-in amount is ₱50,000.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showMinAmountExceededDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Minimum Amount Required");
        builder.setMessage("Minimum cash-in amount is ₱10.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private boolean isValidAmount(String amountStr) {
        // Validate amount input for numeric values only and no symbols
        boolean isValid = amountStr.matches("^\\d*\\.?\\d*$") && !amountStr.matches(".*[!@#$%^&*()_=+{}|;:\"<>,/?\\[\\]\\\\].*");

        // Additional validation: Close if dot is entered
        if (amountStr.contains(".")) {
            isValid = false;
        }

        return isValid;
    }

    private void showInvalidAmountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invalid Amount");
        builder.setMessage("Please enter a valid numeric amount without symbols.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private String generateReferenceId() {
        Random random = new Random();
        long referenceId = 100000000000L + (long)(random.nextDouble() * 900000000000L); // Ensure a 12-digit random number
        return String.valueOf(referenceId);
    }

    private void recordTransaction(String phoneNumber, double amount, String referenceId) {
        // Create a new transaction entry in Firebase with server timestamp
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.getUid())
                .child("transactions");

        String transactionId = transactionsRef.push().getKey();
        if (transactionId != null) {
            long timestamp = System.currentTimeMillis(); // Current timestamp
            Transaction transaction = new Transaction("Cash In", phoneNumber, amount, "Cash In", timestamp, referenceId);
            transactionsRef.child(transactionId).setValue(transaction);
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

    private void addRewardsToUser(double rewardAmount) {
        usersRef.orderByChild("phoneNumber").equalTo(currentUser.getPhoneNumber())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Double currentRewards = snapshot.child("rewards").getValue(Double.class);
                            if (currentRewards == null) {
                                currentRewards = 0.0;
                            }
                            double newRewards = currentRewards + rewardAmount;
                            snapshot.getRef().child("rewards").setValue(newRewards).addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    showToast("Failed to update rewards: " + task.getException().getMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Failed to update rewards: " + error.getMessage());
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(Cashin.this, message, Toast.LENGTH_SHORT).show();
    }
}
