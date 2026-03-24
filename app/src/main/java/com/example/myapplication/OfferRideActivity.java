package com.example.myapplication;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class OfferRideActivity extends AppCompatActivity implements OnMapReadyCallback {

    EditText etDriverName, etContactNumber, etFrom, etTo, etDate, etSeats, etFare; // ✅ ADDED etFare
    Button btnSubmit;

    MaterialCardView vehicleCar, vehicleBike;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    GoogleMap mMap;

    String selectedVehicle = "car";
    int maxSeats = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_ride);

        initViews();
        setupFirebase();
        setupMap();
        setupVehicleSelection();
        setupDatePicker();
        setupSubmitButton();
    }

    private void initViews() {
        etDriverName = findViewById(R.id.etDriverName);
        etContactNumber = findViewById(R.id.etContactNumber);
        etFrom = findViewById(R.id.etFrom);
        etTo = findViewById(R.id.etTo);
        etDate = findViewById(R.id.etDate);
        etSeats = findViewById(R.id.etSeats);
        etFare = findViewById(R.id.etFare); // ✅ NEW
        btnSubmit = findViewById(R.id.btnSubmit);

        vehicleCar = findViewById(R.id.vehicleCar);
        vehicleBike = findViewById(R.id.vehicleBike);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupVehicleSelection() {

        vehicleCar.setOnClickListener(v -> {
            selectedVehicle = "car";
            maxSeats = 4;
            etSeats.setHint("Seats (Max 4)");
            updateVehicleSelection();
        });

        vehicleBike.setOnClickListener(v -> {
            selectedVehicle = "bike";
            maxSeats = 2;
            etSeats.setHint("Seats (Max 2)");
            updateVehicleSelection();
        });

        updateVehicleSelection();
    }

    private void updateVehicleSelection() {

        vehicleCar.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        vehicleBike.setCardBackgroundColor(Color.parseColor("#F5F5F5"));

        if ("car".equals(selectedVehicle)) {
            vehicleCar.setCardBackgroundColor(Color.parseColor("#C8E6C9"));
        } else {
            vehicleBike.setCardBackgroundColor(Color.parseColor("#C8E6C9"));
        }
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();

            new DatePickerDialog(this, (view, year, month, day) -> {
                etDate.setText(day + "/" + (month + 1) + "/" + year);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> saveRide());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private LatLng getLatLngFromAddress(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void saveRide() {

        String driverName = etDriverName.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String from = etFrom.getText().toString().trim();
        String to = etTo.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String seatsText = etSeats.getText().toString().trim();
        String fareText = etFare.getText().toString().trim(); // ✅ NEW

        if (driverName.isEmpty() || contactNumber.isEmpty() || from.isEmpty() ||
                to.isEmpty() || date.isEmpty() || seatsText.isEmpty() || fareText.isEmpty()) {

            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int seats;
        int fare;

        try {
            seats = Integer.parseInt(seatsText);
            fare = Integer.parseInt(fareText);

            if (seats > maxSeats || seats < 1) {
                Toast.makeText(this, "Seats must be between 1-" + maxSeats, Toast.LENGTH_SHORT).show();
                return;
            }

            if (fare <= 0) {
                Toast.makeText(this, "Enter valid fare", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (Exception e) {
            Toast.makeText(this, "Enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng fromLatLng = getLatLngFromAddress(from);
        LatLng toLatLng = getLatLngFromAddress(to);

        if (fromLatLng == null || toLatLng == null) {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = getDirectionsUrl(fromLatLng, toLatLng);
        new FetchRouteTask().execute(url);

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> ride = new HashMap<>();
        ride.put("driverId", uid);
        ride.put("driverName", driverName);
        ride.put("contactNumber", contactNumber);
        ride.put("vehicleType", selectedVehicle);
        ride.put("from", from);
        ride.put("to", to);
        ride.put("fromLat", fromLatLng.latitude);
        ride.put("fromLng", fromLatLng.longitude);
        ride.put("toLat", toLatLng.latitude);
        ride.put("toLng", toLatLng.longitude);
        ride.put("date", date);
        ride.put("seats", seats);
        ride.put("fare", fare); // ✅ NEW
        ride.put("createdAt", new Date());

        db.collection("rides")
                .add(ride)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Ride Added ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving ride", Toast.LENGTH_SHORT).show();
                });
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + dest.latitude + "," + dest.longitude +
                "&mode=driving";
    }

    private class FetchRouteTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            return downloadUrl(urls[0]);
        }

        protected void onPostExecute(String result) {
            new ParseRouteTask().execute(result);
        }
    }

    private class ParseRouteTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            try {
                JSONObject jObject = new JSONObject(jsonData[0]);
                return new DirectionsJSONParser().parse(jObject);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            if (result == null || result.isEmpty()) return;

            mMap.clear();
            ArrayList<LatLng> points = new ArrayList<>();

            for (HashMap<String, String> point : result.get(0)) {
                points.add(new LatLng(
                        Double.parseDouble(point.get("lat")),
                        Double.parseDouble(point.get("lng"))
                ));
            }

            mMap.addPolyline(new PolylineOptions().addAll(points).width(10).color(Color.BLUE));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 12));
        }
    }

    private String downloadUrl(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) sb.append(line);

            return sb.toString();

        } catch (Exception e) {
            return "";
        }
    }
}