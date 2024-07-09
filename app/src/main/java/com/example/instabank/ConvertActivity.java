package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConvertActivity extends AppCompatActivity {

    private static final String TAG = "ConvertActivity";

    private TextView textViewCurrentBalance, textViewCurrentUSDBalance, textViewConvertedAmount;
    private EditText editTextAmount;
    private Spinner spinnerConversionType;
    private Button buttonConvert;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private double currentBalance;
    private double currentUSDBalance;
    private double exchangeRateToUSD;
    private double exchangeRateToPHP;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert);

        // Initialize views
        textViewCurrentBalance = findViewById(R.id.textViewCurrentBalance);
        textViewCurrentUSDBalance = findViewById(R.id.textViewCurrentUSDBalance);
        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerConversionType = findViewById(R.id.spinnerConversionType);
        buttonConvert = findViewById(R.id.buttonConvert);
        textViewConvertedAmount = findViewById(R.id.textViewConvertedAmount);

        // Initialize spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.conversion_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerConversionType.setAdapter(adapter);

        // Initialize Firebase Database reference
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            fetchUserBalance();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

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

        // Fetch exchange rates
        fetchExchangeRates();

        // Handle convert button click
        buttonConvert.setOnClickListener(v -> {
            String amountStr = editTextAmount.getText().toString().trim();

            // Validate amount format (allow dots, reject other symbols)
            if (!amountStr.matches("^\\d*\\.?\\d+$")) {
                showDialogWarning("Invalid Amount", "Please enter a valid amount. Use digits and optionally a decimal point.");
                return;
            }

            // Validate minimum amount
            double amount = Double.parseDouble(amountStr);
            if (amount < 1) {
                showDialogWarning("Invalid Amount", "Minimum amount allowed is 100.");
                return;
            }

            // Validate maximum amount
            if (amount > 50000) {
                showDialogWarning("Invalid Amount", "Maximum amount allowed is 50,000.");
                return;
            }

            // Continue with conversion logic
            if (!amountStr.isEmpty()) {
                String conversionType = spinnerConversionType.getSelectedItem().toString();

                if (conversionType.equals("Peso to USD")) {
                    if (amount <= currentBalance) {
                        double convertedAmount = convertPHPToUSD(amount);
                        textViewConvertedAmount.setText("Converted Amount: $" + String.format("%.2f", convertedAmount));
                        updateBalanceInDatabase(amount, convertedAmount, true);
                    } else {
                        showDialogWarning("Insufficient Balance", "You do not have enough balance to complete this transaction.");
                    }
                } else if (conversionType.equals("USD to Peso")) {
                    if (amount <= currentUSDBalance) {
                        double convertedAmount = convertUSDToPHP(amount);
                        textViewConvertedAmount.setText("Converted Amount: ₱" + String.format("%.2f", convertedAmount));
                        updateBalanceInDatabase(amount, convertedAmount, false);
                    } else {
                        showDialogWarning("Insufficient USD Balance", "You do not have enough USD balance to complete this transaction.");
                    }
                }
            } else {
                showDialogWarning("Invalid Amount", "Enter a valid amount.");
            }
        });
    }

    // Fetch user balance from Firebase
    private void fetchUserBalance() {
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
                                    Double balance = snapshot.child("balance").getValue(Double.class);
                                    if (balance == null) {
                                        snapshot.getRef().child("balance").setValue(500.00);
                                        balance = 500.00;
                                    }
                                    currentBalance = balance;
                                    textViewCurrentBalance.setText("Current Balance: ₱" + String.format("%.2f", currentBalance));

                                    Double usdBalance = snapshot.child("usdBalance").getValue(Double.class);
                                    if (usdBalance == null) {
                                        usdBalance = 0.00;
                                        snapshot.getRef().child("usdBalance").setValue(usdBalance); // Ensure usdBalance is initialized if null
                                    }
                                    currentUSDBalance = usdBalance;
                                    textViewCurrentUSDBalance.setText("Current USD Balance: $" + String.format("%.2f", currentUSDBalance));
                                    Log.d(TAG, "Current USD Balance: " + currentUSDBalance); // Log to check currentUSDBalance
                                    break;
                                }
                            } else {
                                Log.e(TAG, "User with phone number not found");
                                Toast.makeText(ConvertActivity.this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "DatabaseError: " + databaseError.getMessage());
                            Toast.makeText(ConvertActivity.this, "Failed to get balance", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e(TAG, "Phone number is null");
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Fetch exchange rates for both USD to PHP and PHP to USD
    private void fetchExchangeRates() {
        String url = "https://api.exchangerate-api.com/v4/latest/PHP";

        // Fetch PHP to USD exchange rate
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ConvertActivity.this, "Failed to fetch exchange rate", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        exchangeRateToUSD = jsonResponse.getJSONObject("rates").getDouble("USD");
                        exchangeRateToPHP = 1 / exchangeRateToUSD; // Calculate reverse rate
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(ConvertActivity.this, "Failed to parse exchange rate", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    // Convert PHP to USD using the fetched exchange rate
    private double convertPHPToUSD(double amountInPHP) {
        return round(amountInPHP * exchangeRateToUSD, 2);
    }

    // Convert USD to PHP using the fetched exchange rate
    private double convertUSDToPHP(double amountInUSD) {
        return round(amountInUSD * exchangeRateToPHP, 2);
    }

    // Round a value to specified number of decimal places
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    // Update balance in Firebase Database
    private void updateBalanceInDatabase(double originalAmount, double convertedAmount, boolean isUSDConversion) {
        String phoneNumber = currentUser.getPhoneNumber();
        if (phoneNumber != null) {
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
                phoneNumber = "+63" + phoneNumber; // Modify this according to your country code
            }

            DatabaseReference userRef = usersRef.child(phoneNumber);

            if (isUSDConversion) {
                currentBalance -= originalAmount;
                currentUSDBalance += convertedAmount;
                userRef.child("balance").setValue(currentBalance);
                userRef.child("usdBalance").setValue(currentUSDBalance);
            } else {
                currentUSDBalance -= originalAmount;
                currentBalance += convertedAmount;
                userRef.child("usdBalance").setValue(currentUSDBalance);
                userRef.child("balance").setValue(currentBalance);
            }

            // Update UI
            textViewCurrentBalance.setText("Current Balance: ₱" + String.format("%.2f", currentBalance));
            textViewCurrentUSDBalance.setText("Current USD Balance: $" + String.format("%.2f", currentUSDBalance));

            Toast.makeText(this, "Balance updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Phone number is null");
            Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show();
        }
    }

    // Show warning dialog
    private void showDialogWarning(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
