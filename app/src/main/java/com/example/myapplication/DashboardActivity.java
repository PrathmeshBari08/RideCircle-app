package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.view.Gravity;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    FusedLocationProviderClient locationClient;

    ImageView btnLocation, menuBtn;
    DrawerLayout drawerLayout;

    Button btnOfferRide, btnFindRide;

    TextView menuHistory, menuLogout, menuPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        menuBtn = findViewById(R.id.menuBtn);
        btnLocation = findViewById(R.id.btnLocation);
        btnOfferRide = findViewById(R.id.btnOfferRide);
        btnFindRide = findViewById(R.id.btnFindRide);

        menuHistory = findViewById(R.id.menuHistory);
        menuLogout = findViewById(R.id.menuLogout);
        menuPayment = findViewById(R.id.menuPayment);

        // 🔥 Payment
        menuPayment.setOnClickListener(v -> {
            startActivity(new Intent(this, PaymentActivity.class));
            drawerLayout.closeDrawer(Gravity.START);
        });

        // 🔥 History
        menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            drawerLayout.closeDrawer(Gravity.START);
        });

        // 🔥 Logout
        menuLogout.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            drawerLayout.closeDrawer(Gravity.START);
        });

        // 🔥 Navigation buttons
        btnOfferRide.setOnClickListener(v ->
                startActivity(new Intent(this, OfferRideActivity.class)));

        btnFindRide.setOnClickListener(v ->
                startActivity(new Intent(this, FindRideActivity.class)));

        menuBtn.setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.START));

        // 🔥 Location client
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 🔥 Delay location button action
        btnLocation.setOnClickListener(v ->
                new Handler().postDelayed(this::getCurrentLocation, 500));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 🔥 DELAYED LOCATION LOAD (FIX ANR)
        new Handler().postDelayed(this::getCurrentLocation, 800);
    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }

        locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener(location -> {

            if (location != null && mMap != null) {

                LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(userLoc).title("My Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15));

            } else {
                Toast.makeText(this, "Turn ON GPS", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            new Handler().postDelayed(this::getCurrentLocation, 300);
        } else {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
        }
    }
}