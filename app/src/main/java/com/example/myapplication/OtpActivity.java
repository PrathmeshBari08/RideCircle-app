package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class OtpActivity extends AppCompatActivity {

    TextView txtPhone;
    EditText otpInput;
    Button btnVerify;

    String phone, verificationId;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        txtPhone = findViewById(R.id.txtPhone);
        otpInput = findViewById(R.id.otpInput);
        btnVerify = findViewById(R.id.btnVerify);

        mAuth = FirebaseAuth.getInstance();

        phone = getIntent().getStringExtra("phone");
        verificationId = getIntent().getStringExtra("verificationId");

        // ✅ Safety check
        if (phone == null || verificationId == null) {
            Toast.makeText(this, "Something went wrong ❌", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtPhone.setText(phone);

        btnVerify.setOnClickListener(v -> {
            String otp = otpInput.getText().toString().trim();

            if (otp.length() != 6) {
                Toast.makeText(this, "Enter valid OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            btnVerify.setEnabled(false); // 🔥 prevent multiple clicks
            verifyOTP(otp);
        });
    }

    private void verifyOTP(String otp) {

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, otp);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {

                            String uid = user.getUid();
                            String phone = user.getPhoneNumber();

                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("phone", phone);
                            userMap.put("createdAt", FieldValue.serverTimestamp());

                            db.collection("users")
                                    .document(uid)
                                    .set(userMap, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {

                                        Toast.makeText(this, "Login Successful ✅", Toast.LENGTH_SHORT).show();

                                        // 🔥 CLEAR BACK STACK (BEST PRACTICE)
                                        Intent intent = new Intent(OtpActivity.this, DashboardActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);

                                        finish(); // extra safety
                                    })
                                    .addOnFailureListener(e -> {
                                        btnVerify.setEnabled(true); // 🔥 re-enable button
                                        Toast.makeText(this, "Error saving user", Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            btnVerify.setEnabled(true);
                        }

                    } else {
                        btnVerify.setEnabled(true);
                        Toast.makeText(this, "Wrong OTP ❌", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}