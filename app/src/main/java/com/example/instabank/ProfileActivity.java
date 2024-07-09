package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhoneNumber, tvBirthday, tvAge, tvAddress;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvAge = findViewById(R.id.tvAge);
        tvAddress = findViewById(R.id.tvAddress);


        // Get current user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String phoneNumber = currentUser.getPhoneNumber();
            if (phoneNumber != null) {
                // Format phone number
                if (!phoneNumber.startsWith("+")) {
                    phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
                    phoneNumber = "+63" + phoneNumber; // Modify this according to your country code
                }

                // Reference to the users node in Firebase
                usersRef = FirebaseDatabase.getInstance().getReference().child("users");

                // Query the database to find the user by phone number
                usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        // Retrieve user details
                                        String firstName = snapshot.child("firstName").getValue(String.class);
                                        String middleName = snapshot.child("middleName").getValue(String.class);
                                        String lastName = snapshot.child("lastName").getValue(String.class);
                                        String email = snapshot.child("email").getValue(String.class);
                                        String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);
                                        String birthday = snapshot.child("birthday").getValue(String.class);
                                        String age = snapshot.child("age").getValue(String.class);
                                        String address = snapshot.child("address").getValue(String.class);

                                        // Display user details
                                        String fullName = firstName + " " + middleName + " " + lastName;
                                        tvName.setText(fullName);
                                        tvEmail.setText(email);
                                        tvPhoneNumber.setText(phoneNumber);
                                        tvBirthday.setText(birthday);
                                        tvAge.setText(age);
                                        tvAddress.setText(address);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle possible errors.
                            }
                        });
            }
        }

        TextView btnCustomerSupport = findViewById(R.id.btnCustomerSupport);
        btnCustomerSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, CustomerSupportActivity.class);
                startActivity(intent);
                // Optionally, you can finish the current activity if needed
                // finish();
            }
        });

        TextView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });

    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}