package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    ImageView carImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        carImage = findViewById(R.id.carImage);

        // Start position (below screen)
        carImage.setTranslationY(800f);

        // Animate car moving up
        carImage.animate()
                .translationY(0f)
                .setDuration(1000)
                .start();

        // 🔥 Check login after delay
        new Handler().postDelayed(() -> {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                // ✅ Already logged in
                startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
            } else {
                // ❌ Not logged in
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            }

            finish();

        }, 2500);
    }
}