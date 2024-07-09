package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Etransfer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.etransfer);

        // Find buttons in etransfer.xml
        Button buttonGcash = findViewById(R.id.buttonGcash);
        Button buttonPaypal = findViewById(R.id.buttonPaypal);
        Button buttonPaymaya = findViewById(R.id.buttonPaymaya);
        Button buttonShopee = findViewById(R.id.buttonShopee);
        Button buttonBDO = findViewById(R.id.buttonBDO);
        Button buttonBPI = findViewById(R.id.buttonBPI);
        Button buttonSecurityBank = findViewById(R.id.buttonSecurityBank);
        Button buttonTransactions = findViewById(R.id.buttonTransactions);
        Button buttonLandBank = findViewById(R.id.buttonLandBank);
        Button btn_home = findViewById(R.id.btn_home);
        Button btn_profile = findViewById(R.id.btn_profile);

        buttonTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to TransactionHistoryActivity
                Intent intent = new Intent(getApplicationContext(), TransactionHistory.class);
                startActivity(intent);
            }
        });

        btn_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to DashboardActivity
                Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(intent);
            }
        });

        btn_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ProfileActivity
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });


        // Set click listeners for each button
        buttonGcash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Gcash button click
                // Example: Navigate to Gcash transfer functionality or activity
                // Replace ExampleActivity.class with your intended activity or functionality
                Intent intent = new Intent(Etransfer.this, GcashPay.class);
                startActivity(intent);
            }
        });

        buttonPaypal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle PayPal button click
                // Example: Navigate to PayPal transfer functionality or activity
                // Replace ExampleActivity.class with your intended activity or functionality
                Intent intent = new Intent(Etransfer.this, PaypalPay.class);
                startActivity(intent);
            }
        });

        buttonPaymaya.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle PayMaya button click
                // Example: Navigate to PayMaya transfer functionality or activity
                // Replace ExampleActivity.class with your intended activity or functionality
                Intent intent = new Intent(Etransfer.this, PaymayaPay.class);
                startActivity(intent);
            }
        });

        buttonShopee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Shopee button click
                // Example: Navigate to Shopee transfer functionality or activity
                // Replace ExampleActivity.class with your intended activity or functionality
                Intent intent = new Intent(Etransfer.this, ShopeePay.class);
                startActivity(intent);
            }
        });

        buttonBDO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Etransfer.this, ComingSoonActivity.class));
            }
        });

        buttonBPI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Etransfer.this, ComingSoonActivity.class));
            }
        });

        buttonSecurityBank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Etransfer.this, ComingSoonActivity.class));
            }
        });

        buttonLandBank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Etransfer.this, ComingSoonActivity.class));
            }
        });


    }
}
