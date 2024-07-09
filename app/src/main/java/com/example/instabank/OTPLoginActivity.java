package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTPLoginActivity extends AppCompatActivity {

    private static final String TAG = "OTPLoginActivity";
    private EditText editTextOTP;
    private TextView textViewPhoneNumber;
    private Button buttonVerifyOTP;

    private String verificationId;
    private String phoneNumber;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Retrieve phone number and verificationId from intent extras
        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("phoneNumber");
        verificationId = intent.getStringExtra("verificationId");

        // Initialize views
        editTextOTP = findViewById(R.id.editTextOTP);
        textViewPhoneNumber = findViewById(R.id.textViewPhoneNumber);
        buttonVerifyOTP = findViewById(R.id.buttonVerifyOTP);

        // Display the phone number
        textViewPhoneNumber.setText(phoneNumber);

        // Verify OTP button click listener
        buttonVerifyOTP.setOnClickListener(v -> verifyOTP());
    }

    private void verifyOTP() {
        String otp = editTextOTP.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            showAlertDialog("Please enter the OTP");
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // OTP verification successful, proceed to PIN input activity
                        Intent pinIntent = new Intent(OTPLoginActivity.this, PinInputActivity.class);
                        pinIntent.putExtra("phoneNumber", phoneNumber);
                        startActivity(pinIntent);
                        finish();
                    } else {
                        // OTP verification failed
                        Log.e(TAG, "OTP verification failed: " + task.getException().getMessage());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            showAlertDialog("Invalid OTP");
                        } else {
                            showAlertDialog("OTP verification failed");
                        }
                    }
                });
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
    protected void onStart() {
        super.onStart();
        // Trigger the OTP send process on start
        sendOTP();
    }

    private void sendOTP() {
        // Ensure phoneNumber is effectively final
        final String finalPhoneNumber = phoneNumber;

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                finalPhoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        Log.d(TAG, "Verification completed");
                        // Automatically handle the code on instant verification
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "Verification failed", e);
                        Toast.makeText(OTPLoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationId = s; // Save the verification id for later use
                    }
                });
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
