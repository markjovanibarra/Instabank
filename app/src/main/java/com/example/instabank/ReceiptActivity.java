package com.example.instabank;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ReceiptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_receipt);

        // Get data from the intent
        String name = getIntent().getStringExtra("name");
        String accountNumber = getIntent().getStringExtra("accountNumber");
        double amount = getIntent().getDoubleExtra("amount", 0);
        String source = getIntent().getStringExtra("source");
        long transactionTimestamp = getIntent().getLongExtra("timestamp", 0); // Use long for timestamp
        String referenceId = getIntent().getStringExtra("referenceId"); // Get unique reference ID

        // Initialize UI elements
        TextView textViewName = findViewById(R.id.textViewName);
        TextView textViewAccountNumber = findViewById(R.id.textViewAccountNumber);
        TextView textViewAmount = findViewById(R.id.textViewAmount);
        TextView textViewDateTime = findViewById(R.id.textViewDateTime);
        TextView textViewSource = findViewById(R.id.textViewSource);
        TextView textViewReferenceId = findViewById(R.id.textViewReferenceId); // Add TextView for reference ID
        Button buttonClose = findViewById(R.id.buttonClose);

        // Set data to UI elements
        textViewName.setText(name);
        textViewAccountNumber.setText("Acc no: " + accountNumber);
        textViewAmount.setText("Amount: ₱" + amount);
        textViewSource.setText("Source: " + source);
        textViewDateTime.setText("Transaction Date: " + getFormattedDateTime(transactionTimestamp));
        textViewReferenceId.setText("Reference ID: " + referenceId); // Set reference ID

        // Set click listener for Close button
        buttonClose.setOnClickListener(v -> finish());  // Close the activity when the Close button is clicked
    }

    private String getFormattedDateTime(long timestamp) {
        if (timestamp <= 0) {
            return ""; // Handle invalid timestamp
        }

        // Convert timestamp to date format
        Date date = new Date(timestamp);
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault()); // Set local time zone
        return dateFormat.format(date);
    }
}
