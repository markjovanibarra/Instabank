package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Executor;

public class PinInputActivity extends AppCompatActivity {

    private EditText editTextPin1, editTextPin2, editTextPin3, editTextPin4, editTextPin5, editTextPin6;
    private Button buttonSubmitPin, buttonUseBiometric;
    private String phoneNumber;

    private DatabaseReference usersRef;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_input);

        // Initialize Firebase Database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Retrieve phone number from intent extras
        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("phoneNumber");

        // Initialize views
        editTextPin1 = findViewById(R.id.editTextPin1);
        editTextPin2 = findViewById(R.id.editTextPin2);
        editTextPin3 = findViewById(R.id.editTextPin3);
        editTextPin4 = findViewById(R.id.editTextPin4);
        editTextPin5 = findViewById(R.id.editTextPin5);
        editTextPin6 = findViewById(R.id.editTextPin6);
        buttonSubmitPin = findViewById(R.id.buttonSubmitPin);
        buttonUseBiometric = findViewById(R.id.buttonUseBiometric);

        TextView tvForgotMPIN = findViewById(R.id.tv_forgot_mpin);
        tvForgotMPIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PinInputActivity.this, CustomerSupportActivity.class);
                startActivity(intent);
            }
        });
        TextView tv_help_center = findViewById(R.id.tv_help_center);
        tv_help_center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PinInputActivity.this, CustomerSupportActivity.class);
                startActivity(intent);
            }
        });

        // Set input type to numberPassword for hiding the PIN
        editTextPin1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editTextPin2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editTextPin3.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editTextPin4.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editTextPin5.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editTextPin6.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        // Add TextWatchers to handle automatic focus shift and clearing
        addPinTextWatchers();

        // Check if biometric is supported and enrolled
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("PinInputActivity", "App can authenticate using biometrics.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("PinInputActivity", "No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("PinInputActivity", "Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e("PinInputActivity", "The user hasn't associated any biometric credentials with their account.");
                break;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(PinInputActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.d("PinInputActivity", "Authentication error: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d("PinInputActivity", "Authentication succeeded!");
                // Authentication succeeded, proceed to the next activity
                navigateToNextActivity();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.d("PinInputActivity", "Authentication failed");
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for Instabank")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account PIN")
                .build();

        buttonUseBiometric.setOnClickListener(view -> biometricPrompt.authenticate(promptInfo));

        buttonSubmitPin.setOnClickListener(view -> {
            String pin1 = editTextPin1.getText().toString();
            String pin2 = editTextPin2.getText().toString();
            String pin3 = editTextPin3.getText().toString();
            String pin4 = editTextPin4.getText().toString();
            String pin5 = editTextPin5.getText().toString();
            String pin6 = editTextPin6.getText().toString();
            String enteredPin = pin1 + pin2 + pin3 + pin4 + pin5 + pin6;
            verifyPin(enteredPin);
        });
    }

    private void addPinTextWatchers() {
        editTextPin1.addTextChangedListener(new PinTextWatcher(editTextPin1, editTextPin2, null));
        editTextPin2.addTextChangedListener(new PinTextWatcher(editTextPin2, editTextPin3, editTextPin1));
        editTextPin3.addTextChangedListener(new PinTextWatcher(editTextPin3, editTextPin4, editTextPin2));
        editTextPin4.addTextChangedListener(new PinTextWatcher(editTextPin4, editTextPin5, editTextPin3));
        editTextPin5.addTextChangedListener(new PinTextWatcher(editTextPin5, editTextPin6, editTextPin4));
        editTextPin6.addTextChangedListener(new PinTextWatcher(editTextPin6, null, editTextPin5));
    }

    private class PinTextWatcher implements TextWatcher {

        private final EditText currentView;
        private final EditText nextView;
        private final EditText previousView;

        public PinTextWatcher(EditText currentView, EditText nextView, EditText previousView) {
            this.currentView = currentView;
            this.nextView = nextView;
            this.previousView = previousView;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() == 1 && nextView != null) {
                nextView.requestFocus();
            } else if (editable.length() == 0 && previousView != null) {
                clearSubsequentFields(currentView);
                previousView.requestFocus();
            }
        }

        private void clearSubsequentFields(EditText startView) {
            boolean clear = false;
            for (EditText pinField : new EditText[]{editTextPin1, editTextPin2, editTextPin3, editTextPin4, editTextPin5, editTextPin6}) {
                if (pinField == startView) {
                    clear = true;
                }
                if (clear) {
                    pinField.getText().clear();
                }
            }
        }
    }

    private void verifyPin(String enteredPin) {
        usersRef.child(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedPin = snapshot.child("pin").getValue(String.class);
                    if (storedPin != null && storedPin.equals(enteredPin)) {
                        // PIN is correct, proceed to the next activity
                        navigateToNextActivity();
                    } else {
                        // PIN is incorrect, show an error message and clear PIN fields
                        showErrorDialog("Incorrect PIN. Please try again.");
                        clearPinFields();
                    }
                } else {
                    showErrorDialog("User not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showErrorDialog("Failed to retrieve user data. Please try again.");
            }
        });
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void clearPinFields() {
        editTextPin1.getText().clear();
        editTextPin2.getText().clear();
        editTextPin3.getText().clear();
        editTextPin4.getText().clear();
        editTextPin5.getText().clear();
        editTextPin6.getText().clear();
        editTextPin1.requestFocus();
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(PinInputActivity.this, DashboardActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivity(intent);
        finish();
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
