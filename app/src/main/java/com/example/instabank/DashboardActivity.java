package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView textViewWelcome, textViewBalance;
    private ImageButton switchCurrencyIcon;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private ValueEventListener balanceListener;
    private double pesoBalance = 0.0;
    private double usdBalance = 0.0;
    private boolean displayInUSD = false;

    private ListView listViewTransactions;
    private DatabaseReference transactionsRef;
    private List<TransactionHistory.Transaction> transactionList;
    private ArrayAdapter<String> adapter;
    private List<String> transactionDisplayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        textViewWelcome = findViewById(R.id.textViewWelcome);
        textViewBalance = findViewById(R.id.textViewBalance);
        switchCurrencyIcon = findViewById(R.id.switch_currency_icon);
        listViewTransactions = findViewById(R.id.listViewTransactions);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            initializeBalance();
        } else {
            textViewWelcome.setText("Welcome User!");
        }

        switchCurrencyIcon.setOnClickListener(v -> {
            displayInUSD = !displayInUSD;
            displayBalance();
            updateCurrencyIcon();
        });

        transactionList = new ArrayList<>();
        transactionDisplayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, transactionDisplayList);
        listViewTransactions.setAdapter(adapter);

        if (currentUser != null) {
            transactionsRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(currentUser.getUid())
                    .child("transactions");
            loadTransactions(); // Load initial transactions
        }

        setupButtonClicks(); // Move this call here
    }

    private void initializeBalance() {
        String phoneNumber = currentUser.getPhoneNumber();
        if (phoneNumber != null) {
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
                phoneNumber = "+63" + phoneNumber;
            }

            usersRef = FirebaseDatabase.getInstance().getReference().child("users");

            usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    String firstName = snapshot.child("firstName").getValue(String.class);
                                    if (firstName != null) {
                                        textViewWelcome.setText("Welcome " + firstName + "!");
                                    }

                                    Double balance = snapshot.child("balance").getValue(Double.class);
                                    if (balance == null) {
                                        snapshot.getRef().child("balance").setValue(500.00);
                                        balance = 500.00;
                                    }

                                    Double usdBal = snapshot.child("usdBalance").getValue(Double.class);
                                    if (usdBal == null) {
                                        usdBal = 0.00;
                                    }

                                    pesoBalance = balance;
                                    usdBalance = usdBal;

                                    displayBalance();

                                    setupBalanceListener(snapshot.getRef());
                                    break;
                                }
                            } else {
                                textViewWelcome.setText("Welcome User!");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(DashboardActivity.this, "Failed to get user information", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadTransactions() {
        if (transactionsRef != null) {
            transactionsRef.orderByChild("timestamp").limitToLast(4).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    transactionList.clear();
                    transactionDisplayList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String accountNumber = snapshot.child("accountNumber").getValue(String.class);
                        Double amount = snapshot.child("amount").getValue(Double.class);
                        String source = snapshot.child("source").getValue(String.class);
                        Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                        if (accountNumber != null && amount != null && source != null && timestamp != null) {
                            TransactionHistory.Transaction transaction = new TransactionHistory.Transaction(
                                    accountNumber, "", amount, source, timestamp, ""
                            );
                            transactionList.add(transaction);
                        }
                    }

                    // Sort transactions by timestamp (newest first)
                    Collections.sort(transactionList, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));

                    // Clear display list before adding new items
                    transactionDisplayList.clear();

                    // Add up to 3 recent transactions to the display list
                    int count = 0;
                    for (TransactionHistory.Transaction transaction : transactionList) {
                        if (count >= 4) {
                            break; // Limit to 3 transactions
                        }
                        String dateTime = getFormattedDateTime(transaction.getTimestamp());
                        String transactionInfo = dateTime +     "                    " + (transaction.getAmount() >= 0 ? "    +  " : "    -  ") + Math.abs(transaction.getAmount());
                        transactionDisplayList.add(transactionInfo);
                        count++;
                    }

                    adapter.notifyDataSetChanged(); // Notify adapter of changes
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(DashboardActivity.this, "Failed to load transactions: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private String getFormattedDateTime(long timestamp) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    private void displayBalance() {
        if (displayInUSD) {
            textViewBalance.setText(String.format(Locale.getDefault(), " $%.2f", usdBalance));
        } else {
            textViewBalance.setText(String.format(Locale.getDefault(), " ₱%.2f", pesoBalance));
        }
    }

    private void updateCurrencyIcon() {
        if (displayInUSD) {
            switchCurrencyIcon.setImageResource(R.drawable.money_off);
        } else {
            switchCurrencyIcon.setImageResource(R.drawable.money_on);
        }
    }

    private void setupBalanceListener(DatabaseReference userRef) {
        balanceListener = userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double pesoBal = dataSnapshot.child("balance").getValue(Double.class);
                Double usdBal = dataSnapshot.child("usdBalance").getValue(Double.class);

                if (pesoBal != null) {
                    pesoBalance = pesoBal;
                }
                if (usdBal != null) {
                    usdBalance = usdBal;
                }
                displayBalance();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DashboardActivity", "Failed to read balance value.", databaseError.toException());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (balanceListener != null && usersRef != null) {
            usersRef.removeEventListener(balanceListener);
        }
    }

    private void setupButtonClicks() {
        findViewById(R.id.buttonOption1).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, Etransfer.class)));
        findViewById(R.id.btn_rewards).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, RewardsActivity.class)));
        findViewById(R.id.btn_pay).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, BillsActivity.class)));
        findViewById(R.id.btn_load).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, BuyLoadActivity.class)));
        findViewById(R.id.buttonSavings1).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, SavingsActivity.class)));
        findViewById(R.id.btn_savings1).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, RealtimeCurrency.class)));
        findViewById(R.id.btn_cashin).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, Cashin.class)));
        findViewById(R.id.btn_profile).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));
        findViewById(R.id.buttonPay).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, InstabankPay.class)));
        findViewById(R.id.buttonOption2).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, TransactionHistory.class)));
        findViewById(R.id.btn_home).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, DashboardActivity.class)));
        findViewById(R.id.buttonConvertPage).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ConvertActivity.class)));
        findViewById(R.id.btnSeeAll).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, TransactionHistory.class)));

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Do you want to exit the app?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity(); // Close all activities of the app
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
