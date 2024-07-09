package com.example.instabank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class BillsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills); // Ensure to create activity_bills.xml layout file

        Button btnMeralco = findViewById(R.id.btnMeralco);
        Button btnMaynilad = findViewById(R.id.btnMaynilad);
        Button btnPLDT = findViewById(R.id.btnPLDT);
        Button btnSkyCable = findViewById(R.id.btnSkyCable);
        Button btnNBI = findViewById(R.id.btnNBI);
        Button btnsss = findViewById(R.id.btnsss);
        Button btnBIR = findViewById(R.id.btnBIR);
        Button btnpagibig = findViewById(R.id.btnpagibig);

        btnMeralco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Meralco payment activity
                Intent intent = new Intent(BillsActivity.this, Meralco.class);
                startActivity(intent);
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
        btnMaynilad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, Maynilad.class));
            }
        });

        btnPLDT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, PLDT.class));
            }
        });

        btnSkyCable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, SkyCable.class));
            }
        });

        btnNBI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, ComingSoonActivity.class));
            }
        });

        btnsss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, ComingSoonActivity.class));
            }
        });

        btnBIR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, ComingSoonActivity.class));
            }
        });

        btnpagibig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BillsActivity.this, ComingSoonActivity.class));
            }
        });
    }
}
