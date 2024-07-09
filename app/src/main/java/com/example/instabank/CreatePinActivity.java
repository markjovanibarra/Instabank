package com.example.instabank;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreatePinActivity extends AppCompatActivity {

    private EditText editTextPin;
    private EditText editTextConfirmPin;
    private Button buttonSubmitPin;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private String firstName;
    private String middleName;
    private String lastName;
    private String age;
    private String birthday;
    private String address;
    private String email;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pin);

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Retrieve all necessary data from intent extras
        Intent intent = getIntent();
        firstName = intent.getStringExtra("firstName");
        middleName = intent.getStringExtra("middleName");
        lastName = intent.getStringExtra("lastName");
        age = intent.getStringExtra("age");
        birthday = intent.getStringExtra("birthday");
        address = intent.getStringExtra("address");
        email = intent.getStringExtra("email");
        phoneNumber = intent.getStringExtra("phoneNumber");

        editTextPin = findViewById(R.id.editTextPin);
        editTextConfirmPin = findViewById(R.id.editTextConfirmPin);
        buttonSubmitPin = findViewById(R.id.buttonSubmitPin);

        buttonSubmitPin.setOnClickListener(v -> submitPin());
    }

    private void submitPin() {
        String pin = editTextPin.getText().toString().trim();
        String confirmPin = editTextConfirmPin.getText().toString().trim();

        if (TextUtils.isEmpty(pin) || TextUtils.isEmpty(confirmPin)) {
            showAlertDialog("Please enter PIN and confirm PIN");
            return;
        }

        if (!pin.equals(confirmPin)) {
            showAlertDialog("PINs do not match");
            return;
        }

        if (pin.length() != 6) {
            showAlertDialog("PIN must be 6 digits");
            return;
        }

        // Proceed to insert data into Firebase Realtime Database
        insertDataIntoDatabase(pin);
    }

    private void insertDataIntoDatabase(String pin) {
        // Create a new User object with all the collected data
        User user = new User(firstName, middleName, lastName, age, birthday, address, email, phoneNumber, pin);

        // Insert the user data into the database under "users" node with the userId
        usersRef.child(phoneNumber).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showRegistrationSuccessDialog();
                    } else {
                        showAlertDialog("Failed to register user: " + task.getException().getMessage());
                    }
                });
    }

    private void showRegistrationSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Account created successfully")
                .setPositiveButton("Open Account", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Pass phone number to PinInputActivity for login
                        Intent intent = new Intent(CreatePinActivity.this, PinInputActivity.class);
                        intent.putExtra("phoneNumber", phoneNumber);
                        startActivity(intent);
                    }
                })
                .setCancelable(false) // Prevent dismissing dialog by tapping outside
                .show();
    }

    private void showAlertDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
