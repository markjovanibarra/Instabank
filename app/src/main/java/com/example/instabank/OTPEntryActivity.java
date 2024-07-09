package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OTPEntryActivity extends AppCompatActivity {

    private EditText editTextOTP;
    private Button buttonVerifyOTP;
    private TextView textViewPhoneNumber;
    private String verificationId;
    private String phoneNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String age;
    private String birthday;
    private String address;
    private String email;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_entry);

        // Initialize Firebase Auth and Database Reference
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Get intent extras
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        firstName = getIntent().getStringExtra("firstName");
        middleName = getIntent().getStringExtra("middleName");
        lastName = getIntent().getStringExtra("lastName");
        age = getIntent().getStringExtra("age");
        birthday = getIntent().getStringExtra("birthday");
        address = getIntent().getStringExtra("address");
        email = getIntent().getStringExtra("email");

        // Initialize views
        editTextOTP = findViewById(R.id.editTextOTP);
        buttonVerifyOTP = findViewById(R.id.buttonVerifyOTP);
        textViewPhoneNumber = findViewById(R.id.textViewPhoneNumber);

        // Display the phone number
        textViewPhoneNumber.setText(phoneNumber);

        // Verify OTP button click listener
        buttonVerifyOTP.setOnClickListener(v -> verifyOTP());
    }

    private void verifyOTP() {
        String otp = editTextOTP.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            showAlertDialog("Please enter OTP");
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, navigate to CreatePinActivity
                        navigateToCreatePin();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            showAlertDialog("Invalid OTP");
                        } else {
                            showAlertDialog("Sign-in failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    private void navigateToCreatePin() {
        Intent createPinIntent = new Intent(OTPEntryActivity.this, CreatePinActivity.class);
        createPinIntent.putExtra("firstName", firstName);
        createPinIntent.putExtra("middleName", middleName);
        createPinIntent.putExtra("lastName", lastName);
        createPinIntent.putExtra("age", age);
        createPinIntent.putExtra("birthday", birthday);
        createPinIntent.putExtra("address", address);
        createPinIntent.putExtra("email", email);
        createPinIntent.putExtra("phoneNumber", phoneNumber);
        startActivity(createPinIntent);
    }

    private void showAlertDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create();

        alertDialog.setOnShowListener(dialogInterface -> {
            TextView messageView = alertDialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setGravity(Gravity.CENTER);
            }
        });

        alertDialog.show();
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
