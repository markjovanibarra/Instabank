package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class Etransferpay extends AppCompatActivity {

    private EditText editTextAccountNumber, editTextAmount;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.etransferpay);

        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize UI elements
        editTextAccountNumber = findViewById(R.id.editTextAccountNumber);
        editTextAmount = findViewById(R.id.editTextAmount);
        Button buttonPay = findViewById(R.id.buttonPay);


        // Set click listener for Pay button
        buttonPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate inputs
                String accountNumber = editTextAccountNumber.getText().toString().trim();
                String amountString = editTextAmount.getText().toString().trim();

                if (accountNumber.isEmpty()) {
                    editTextAccountNumber.setError("Account number is required");
                    editTextAccountNumber.requestFocus();
                    return;
                }

                if (amountString.isEmpty()) {
                    editTextAmount.setError("Amount is required");
                    editTextAmount.requestFocus();
                    return;
                }

                // Convert amount to double
                double amount = Double.parseDouble(amountString);

                // Deduct amount from user's balance in Firebase
                if (currentUser != null) {
                    String phoneNumber = currentUser.getPhoneNumber();
                    if (phoneNumber != null) {
                        usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                Double currentBalance = snapshot.child("balance").getValue(Double.class);
                                                if (currentBalance != null) {
                                                    double newBalance = currentBalance - amount;
                                                    // Update balance in Firebase
                                                    snapshot.getRef().child("balance").setValue(newBalance)
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    // Update balance on dashboard after successful payment
                                                                    updateDashboardBalance(newBalance);
                                                                    Toast.makeText(Etransferpay.this, "Payment of $" + amount + " successful", Toast.LENGTH_SHORT).show();
                                                                    // Finish activity
                                                                    finish();
                                                                }

                                                                private void showReceipt(String accountNumber, double amount, String referenceNumber) {
                                                                    Intent intent = new Intent(Etransferpay.this, ReceiptActivity.class);
                                                                    intent.putExtra("accountNumber", accountNumber);
                                                                    intent.putExtra("amount", amount);
                                                                    intent.putExtra("referenceNumber", referenceNumber);
                                                                    startActivity(intent);
                                                                }

                                                                private void updateDashboardBalance(double newBalance) {
                                                                    Intent resultIntent = new Intent();
                                                                    resultIntent.putExtra("newBalance", newBalance);
                                                                    setResult(RESULT_OK, resultIntent);
                                                                }
                                                            })

                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Toast.makeText(Etransferpay.this, "Failed to deduct balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }
                                            }
                                        } else {
                                            Toast.makeText(Etransferpay.this, "User not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Toast.makeText(Etransferpay.this, "Failed to deduct balance: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            }
        });
    }
}
