package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RealtimeCurrency extends AppCompatActivity {

    private EditText editTextAmount;
    private Spinner spinnerFromCurrency, spinnerToCurrency;
    private TextView textViewConvertedAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_currency);

        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency);
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency);
        textViewConvertedAmount = findViewById(R.id.textViewConvertedAmount);

        // Setup the spinner with currency options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currencies_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFromCurrency.setAdapter(adapter);
        spinnerToCurrency.setAdapter(adapter);

        // Set up text watcher for real-time updates
        editTextAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertCurrency();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
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
        // Set up item selected listeners for spinners
        spinnerFromCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                convertCurrency();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        spinnerToCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                convertCurrency();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void convertCurrency() {
        String amountStr = editTextAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            textViewConvertedAmount.setText("Converted Amount: ");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
        String toCurrency = spinnerToCurrency.getSelectedItem().toString();

        fetchExchangeRateAndConvert(amount, fromCurrency, toCurrency);
    }

    private void fetchExchangeRateAndConvert(double amount, String fromCurrency, String toCurrency) {
        new Thread(() -> {
            try {
                String apiKey = "5b49b5c4b9-4cd012f2e0-sfznvu"; // Replace with your FastForex API key
                String urlStr = "https://api.fastforex.io/fetch-one?from=" + fromCurrency + "&to=" + toCurrency + "&api_key=" + apiKey;
                URL url = new URL(urlStr);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                urlConnection.disconnect();

                JSONObject jsonObject = new JSONObject(content.toString());
                double exchangeRate = jsonObject.getJSONObject("result").getDouble(toCurrency);
                double convertedAmount = amount * exchangeRate;

                runOnUiThread(() -> textViewConvertedAmount.setText(String.format("Converted Amount: %.2f %s", convertedAmount, toCurrency)));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(RealtimeCurrency.this, "Failed to fetch exchange rate: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}