package com.example.instabank;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ComingSoonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coming_soon);

        // Initialize UI elements
        TextView textViewComingSoon = findViewById(R.id.textViewComingSoon);
        TextView textViewDescription = findViewById(R.id.textViewDescription);

        // Set text programmatically (optional)
        textViewComingSoon.setText("We're Working on It");
        textViewDescription.setText("We're currently working on it and we are happy to serve you.");
    }
}