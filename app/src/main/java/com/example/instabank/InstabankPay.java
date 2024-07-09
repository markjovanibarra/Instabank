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

public class InstabankPay extends AppCompatActivity {

    private EditText editTextPhoneNumber, editTextAmount;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private TextView textViewCurrentBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instabank_pay);

        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize UI elements
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextAmount = findViewById(R.id.editTextAmount);
        Button buttonPay = findViewById(R.id.buttonPay);
        textViewCurrentBalance = findViewById(R.id.textViewCurrentBalance);

        // Fetch and display current balance
        if (currentUser != null) {
            String currentUserPhoneNumber = currentUser.getPhoneNumber();
            if (currentUserPhoneNumber != null) {
                usersRef.orderByChild("phoneNumber").equalTo(currentUserPhoneNumber)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                        if (currentBalance != null) {
                                            textViewCurrentBalance.setText("Current Balance: ₱" + String.format("%.2f", currentBalance));
                                        } else {
                                            textViewCurrentBalance.setText("Current Balance: ₱0.00");
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(InstabankPay.this, "Failed to fetch balance: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }

        // Set click listener for Pay button
        buttonPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePayButtonClick();
            }
        });
    }


    private void handlePayButtonClick() {
        // Validate inputs
        String phoneNumberInput = editTextPhoneNumber.getText().toString().trim();
        String amountStringInput = editTextAmount.getText().toString().trim();

        if (phoneNumberInput.isEmpty()) {
            editTextPhoneNumber.setError("Phone number is required");
            editTextPhoneNumber.requestFocus();
            return;
        }

        if (amountStringInput.isEmpty()) {
            editTextAmount.setError("Amount is required");
            editTextAmount.requestFocus();
            return;
        }

        // Format phone number
        final String phoneNumber = formatPhoneNumber(phoneNumberInput);

        // Convert amount to double
        final double amount;
        try {
            amount = Double.parseDouble(amountStringInput);
        } catch (NumberFormatException e) {
            editTextAmount.setError("Invalid amount");
            editTextAmount.requestFocus();
            return;
        }

        // Validate minimum amount
        if (amount < 10) {
            showAlertDialog("Invalid Amount", "The minimum transfer amount is ₱10.");
            return;
        }

        // Validate maximum amount
        if (amount > 50000) {
            showAlertDialog("Invalid Amount", "The maximum transfer amount is ₱50,000.");
            return;
        }

        // Validate that the phone number is not the current user's phone number
        if (currentUser != null && currentUser.getPhoneNumber() != null && currentUser.getPhoneNumber().equals(phoneNumber)) {
            showExistingPhoneDialog();

            return;
        }

        // Check if the entered phone number exists in Firebase
        usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            handleTransaction(dataSnapshot, phoneNumber, amount);
                        } else {
                            showRegisteredPhoneDialog();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(InstabankPay.this, "Failed to process payment: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleTransaction(DataSnapshot dataSnapshot, String phoneNumber, double amount) {
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            Double receiverBalance = snapshot.child("balance").getValue(Double.class);
            if (receiverBalance != null) {
                updateCurrentUserBalance(amount, phoneNumber, snapshot);
            } else {
                Toast.makeText(InstabankPay.this, "Receiver's balance not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateCurrentUserBalance(double amount, String phoneNumber, DataSnapshot receiverSnapshot) {
        if (currentUser == null) return;
        String currentUserPhoneNumber = currentUser.getPhoneNumber();
        if (currentUserPhoneNumber == null) return;

        usersRef.orderByChild("phoneNumber").equalTo(currentUserPhoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                if (currentBalance != null && currentBalance >= amount) {
                                    double newBalance = currentBalance - amount;
                                    textViewCurrentBalance.setText("Current Balance: ₱" + String.format("%.2f", currentBalance));
                                    snapshot.getRef().child("balance").setValue(newBalance)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    String referenceId = generateReferenceId();
                                                    recordTransaction(phoneNumber, "Savings", amount, referenceId);
                                                    showReceipt("Send", phoneNumber, amount, "Savings", referenceId);
                                                    Toast.makeText(InstabankPay.this, "Payment of ₱" + amount + " successful", Toast.LENGTH_SHORT).show();
                                                    updateReceiverBalance(receiverSnapshot, amount, phoneNumber, referenceId);
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(InstabankPay.this, "Failed to deduct balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    showInsufficientBalanceDialog();
                                }
                            }
                        } else {
                            Toast.makeText(InstabankPay.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(InstabankPay.this, "Failed to deduct balance: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateReceiverBalance(DataSnapshot receiverSnapshot, double amount, String phoneNumber, String referenceId) {
        Double receiverBalance = receiverSnapshot.child("balance").getValue(Double.class);
        if (receiverBalance != null) {
            double newReceiverBalance = receiverBalance + amount;
            receiverSnapshot.getRef().child("balance").setValue(newReceiverBalance)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            recordReceiverTransaction(phoneNumber, "Receive", amount, referenceId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(InstabankPay.this, "Failed to add amount to receiver: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Format phone number if necessary
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", ""); // Remove leading zeros
            phoneNumber = "+63" + phoneNumber; // Modify this according to your country code
        }
        return phoneNumber;
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

    private String generateReferenceId() {
        Random random = new Random();
        long referenceId = 100000000000L + (long)(random.nextDouble() * 900000000000L); // Ensure a 12-digit random number
        return String.valueOf(referenceId);
    }

    private void recordTransaction(String accountNumber, String source, double amount, String referenceId) {
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.getUid())
                .child("transactions");

        String transactionId = transactionsRef.push().getKey();
        if (transactionId != null) {
            long timestamp = System.currentTimeMillis(); // Current timestamp
            Transaction transaction = new Transaction("Send", accountNumber, -amount, source, timestamp, referenceId);
            transactionsRef.child(transactionId).setValue(transaction);
        }
    }

    private void recordReceiverTransaction(String phoneNumber, String source, double amount, String referenceId) {
        usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String receiverUid = snapshot.getKey();

                                DatabaseReference receiverTransactionsRef = FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .child(receiverUid)
                                        .child("transactions");

                                String transactionId = receiverTransactionsRef.push().getKey();
                                if (transactionId != null) {
                                    long timestamp = System.currentTimeMillis(); // Current timestamp
                                    Transaction transaction = new Transaction("Receive", phoneNumber, amount, source, timestamp, referenceId);
                                    receiverTransactionsRef.child(transactionId).setValue(transaction);
                                }
                            }
                        } else {
                            Toast.makeText(InstabankPay.this, "Receiver not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(InstabankPay.this, "Failed to record transaction for receiver: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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

    private void showExistingPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invalid Operation");
        builder.setMessage("You cannot send money to your own phone number.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showRegisteredPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Phone Number");
        builder.setMessage("Phone number not registered");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

}
