package com.example.instabank;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";
    private EditText editTextFirstName, editTextMiddleName, editTextLastName, editTextEmail, editTextPhoneNumber;
    private EditText editTextAge, editTextBirthday, editTextAddress;
    private Button buttonRegister;
    private CheckBox checkBoxAgreement;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        checkBoxAgreement = findViewById(R.id.checkBoxAgreement);

        // Initialize Firebase Auth and Database reference
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextMiddleName = findViewById(R.id.editTextMiddleName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextAge = findViewById(R.id.editTextAge);
        editTextBirthday = findViewById(R.id.editTextBirthday);
        editTextAddress = findViewById(R.id.editTextAddress);
        buttonRegister = findViewById(R.id.buttonRegister);

        // Set click listener for birthday EditText to show DatePickerDialog
        editTextBirthday.setOnClickListener(v -> showDatePickerDialog());

        // Register button click listener
        buttonRegister.setOnClickListener(v -> {
            // Validate and proceed to OTP verification
            validateAndRegisterUser();
        });
    }

    private void showDatePickerDialog() {
        // Get current date to set as the default date in the picker
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show a DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                RegistrationActivity.this,
                (view, yearSelected, monthSelected, dayOfMonthSelected) -> {
                    // Format the selected date and set it in the EditText
                    String selectedDate = (monthSelected + 1) + "/" + dayOfMonthSelected + "/" + yearSelected;
                    editTextBirthday.setText(selectedDate);

                    // Calculate and set age based on selected date
                    try {
                        Date birthDate = new SimpleDateFormat("MM/dd/yyyy").parse(selectedDate);
                        int age = calculateAge(birthDate);
                        editTextAge.setText(String.valueOf(age));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                },
                year,
                month,
                day
        );
        datePickerDialog.show();
    }

    private int calculateAge(Date birthDate) {
        Calendar dob = Calendar.getInstance();
        dob.setTime(birthDate);
        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private void validateAndRegisterUser() {
        // Validate all required fields and highlight empty ones
        if (!validateAndHighlightEmptyField(editTextFirstName) ||
                !validateAndHighlightEmptyField(editTextLastName) ||
                !validateAndHighlightEmptyField(editTextEmail) ||
                !validateAndHighlightEmptyField(editTextPhoneNumber) ||
                !validateAndHighlightEmptyField(editTextAge) ||
                !validateAndHighlightEmptyField(editTextBirthday) ||
                !validateAndHighlightEmptyField(editTextAddress) ||
                !validateMiddleName()) {
            return; // Exit registration if any required field is empty or invalid
        }

        // Additional validations for each field
        // (e.g., name format, email format, phone number format, age restriction, etc.)

        registerUser();
    }

    // Helper method to validate and highlight empty fields
    private boolean validateAndHighlightEmptyField(EditText editText) {
        if (TextUtils.isEmpty(editText.getText().toString().trim())) {
            editText.setError("This field is required");
            return false;
        } else {
            editText.setError(null); // Clear the error
            return true;
        }
    }

    // Validate middle name for only letters
    private boolean validateMiddleName() {
        String middleName = editTextMiddleName.getText().toString().trim();
        if (TextUtils.isEmpty(middleName)) {
            return true; // Middle name is optional, so no validation needed if empty
        } else {
            if (!isValidName(middleName)) {
                editTextMiddleName.setError("Invalid middle name: Only letters are allowed");
                return false;
            } else {
                editTextMiddleName.setError(null); // Clear the error
                return true;
            }
        }
    }

    private void registerUser() {
        String firstName = editTextFirstName.getText().toString().trim();
        String middleName = editTextMiddleName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String age = editTextAge.getText().toString().trim();
        String birthday = editTextBirthday.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();

        // Validate agreement checkbox
        if (!checkBoxAgreement.isChecked()) {
            Toast.makeText(this, "You must agree to the terms to proceed.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate first name
        if (TextUtils.isEmpty(firstName)) {
            Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
            return;
        } else if (!isValidName(firstName)) {
            showInvalidNameDialog("Invalid first name: Only letters are allowed");
            return;
        }

        // Validate last name
        if (TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show();
            return;
        } else if (!isValidName(lastName)) {
            showInvalidNameDialog("Invalid last name: Only letters are allowed");
            return;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        } else if (!isValidEmail(email)) {
            new AlertDialog.Builder(this)
                    .setTitle("Invalid Email")
                    .setMessage("Invalid email format. Hint: example@gmail.com")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Validate phone number
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        } else if (!isValidPhoneNumber(phoneNumber)) {
            new AlertDialog.Builder(this)
                    .setTitle("Invalid Phone Number")
                    .setMessage("Invalid phone number format. Hint: 09XXXXXXXXX")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Validate age
        if (TextUtils.isEmpty(age)) {
            Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
            return;
        } else if (Integer.parseInt(age) < 18) {
            new AlertDialog.Builder(this)
                    .setTitle("Age Restriction")
                    .setMessage("You must be at least 18 years old to register.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Validate birthday
        if (TextUtils.isEmpty(birthday)) {
            Toast.makeText(this, "Please enter your birthday", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate address
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure phone number format starts with "+"
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
            phoneNumber = "+63" + phoneNumber; // Modify this according to your country code
        }

        // Check if the phone number already exists in the database
        usersRef.orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Phone number already exists, show a dialog
                    new AlertDialog.Builder(RegistrationActivity.this)
                            .setTitle("Phone Number Already Registered")
                            .setMessage("The phone number " + phoneNumber + " is already registered. Please use a different phone number.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    // Phone number does not exist, proceed with OTP verification
                    startOtpVerification(firstName, middleName, lastName, email, age, birthday, address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(RegistrationActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOtpVerification(String firstName, String middleName, String lastName, String email, String age, String birthday, String address) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        Log.d(TAG, "Verification completed");
                        // Automatically handle the code on instant verification
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "Verification failed", e);

                        if (e instanceof FirebaseTooManyRequestsException) {
                            // Too many requests
                            Toast.makeText(RegistrationActivity.this, "Too many requests. Please try again later.", Toast.LENGTH_SHORT).show();
                            buttonRegister.setEnabled(false);
                            new Handler().postDelayed(() -> buttonRegister.setEnabled(true), 30000); // 30 seconds cooldown
                        } else {
                            // Other errors
                            Toast.makeText(RegistrationActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        Intent otpIntent = new Intent(RegistrationActivity.this, OTPEntryActivity.class);
                        otpIntent.putExtra("verificationId", s);
                        otpIntent.putExtra("phoneNumber", phoneNumber);
                        otpIntent.putExtra("firstName", firstName);
                        otpIntent.putExtra("middleName", middleName);
                        otpIntent.putExtra("lastName", lastName);
                        otpIntent.putExtra("age", age);
                        otpIntent.putExtra("birthday", birthday);
                        otpIntent.putExtra("address", address);
                        otpIntent.putExtra("email", email);
                        startActivity(otpIntent);
                    }
                });
    }

    // Helper method to validate names (only letters)
    private boolean isValidName(String name) {
        // Allow letters, spaces, and "ñ"
        return Pattern.matches("[a-zA-ZñÑ\\s]+", name);
    }


    // Method to validate email format
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Helper method to validate phone number format (09XXXXXXXXX)
    private boolean isValidPhoneNumber(String phoneNumber) {
        return Pattern.matches("^09\\d{9}$", phoneNumber);
    }

    // Method to show dialog for invalid names
    private void showInvalidNameDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Invalid Name")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
