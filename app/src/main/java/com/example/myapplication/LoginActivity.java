package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    EditText phoneInput;
    Button btnContinue;

    FirebaseAuth mAuth;
    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        phoneInput = findViewById(R.id.phoneInput);
        btnContinue = findViewById(R.id.btnContinue);

        mAuth = FirebaseAuth.getInstance();

        btnContinue.setOnClickListener(view -> {
            String phone = phoneInput.getText().toString().trim();

            if (phone.length() != 10) {
                Toast.makeText(this, "Enter valid number", Toast.LENGTH_SHORT).show();
                return;
            }

            phone = "+91" + phone;

            sendOTP(phone);
        });
    }

    // 🔥 ADD THIS METHOD (IMPORTANT)
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User already logged in → go to Dashboard
            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void sendOTP(String phoneNumber) {

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(
                        com.google.firebase.auth.PhoneAuthCredential credential) {
                    // Auto verification (optional)
                }

                @Override
                public void onVerificationFailed(
                        com.google.firebase.FirebaseException e) {

                    Toast.makeText(LoginActivity.this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(String verId,
                                       PhoneAuthProvider.ForceResendingToken token) {

                    verificationId = verId;

                    String phone = phoneInput.getText().toString().trim();
                    phone = "+91" + phone;

                    // 🔥 Move to OTP screen
                    Intent intent = new Intent(LoginActivity.this, OtpActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("verificationId", verificationId);
                    startActivity(intent);
                }
            };
}