package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextPhoneNumber;
    private Button buttonLogin;
    private TextView textViewSignUp;

    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        mAuth = FirebaseAuth.getInstance();

        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.textViewSignUp);

        buttonLogin.setOnClickListener(v -> loginUser());
        // Set OnClickListener for Sign Up TextView
        textViewSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
            startActivity(intent);

        });
    }

    private void loginUser() {
        phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            showAlertDialog("Please enter your phone number");
            return;
        }

        if (!TextUtils.isDigitsOnly(phoneNumber.replaceAll("[^0-9]", ""))) {
            showAlertDialog("Phone number should not contain symbols or spaces");
            return;
        }

        if (!phoneNumber.startsWith("09")) {
            showAlertDialog("Phone number must start with 09");
            return;
        }

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
            phoneNumber = "+63" + phoneNumber;
        }

        usersRef.orderByChild("phoneNumber")
                .equalTo(phoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            initiateOtpVerification(phoneNumber);
                        } else {
                            showAlertDialog("Number is not registered in Instabank");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("LoginActivity", "Database error: " + databaseError.getMessage());
                        showAlertDialog("Database error: " + databaseError.getMessage());
                    }
                });
    }

    private void initiateOtpVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.w("LoginActivity", "onVerificationFailed", e);
                        showAlertDialog("Verification failed: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        Intent otpIntent = new Intent(LoginActivity.this, OTPLoginActivity.class);
                        otpIntent.putExtra("phoneNumber", phoneNumber);
                        otpIntent.putExtra("verificationId", verificationId);
                        startActivity(otpIntent);
                    }
                });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showAlertDialog("Authentication failed.");
                    }
                });
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
