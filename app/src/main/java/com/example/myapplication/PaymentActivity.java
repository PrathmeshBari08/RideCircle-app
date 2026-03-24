package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    Button btnAddUpi, btnAddCard, btnAddDebit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        btnAddUpi = findViewById(R.id.btnAddUpi);
        btnAddCard = findViewById(R.id.btnAddCard);
        btnAddDebit = findViewById(R.id.btnAddDebit);

        btnAddUpi.setOnClickListener(v -> showInputDialog("Enter UPI ID"));
        btnAddCard.setOnClickListener(v -> showInputDialog("Enter Card Number"));
        btnAddDebit.setOnClickListener(v -> showInputDialog("Enter Debit Card Number"));
    }

    private void showInputDialog(String title) {

        EditText input = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {

                    String value = input.getText().toString();

                    if (!value.isEmpty()) {
                        Toast.makeText(this, "Saved: " + value, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Empty input", Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}