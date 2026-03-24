package com.example.myapplication;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.*;

public class FindRideActivity extends AppCompatActivity implements OnMapReadyCallback, PaymentResultListener {

    GoogleMap mMap;
    ListView listView;

    FirebaseFirestore db;

    ArrayList<Map<String, Object>> rideList = new ArrayList<>();
    ArrayList<String> docIds = new ArrayList<>();

    String currentRideId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_ride);

        listView = findViewById(R.id.listView);
        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        loadRides();
    }

    private void loadRides() {

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("bookings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(bookingSnapshots -> {

                    Set<String> bookedRideIds = new HashSet<>();

                    for (DocumentSnapshot booking : bookingSnapshots) {
                        String rideId = booking.getString("rideId");
                        if (rideId != null) bookedRideIds.add(rideId);
                    }

                    db.collection("rides")
                            .addSnapshotListener((value, error) -> {

                                if (value == null || mMap == null) return;

                                rideList.clear();
                                docIds.clear();
                                mMap.clear();

                                for (DocumentSnapshot doc : value.getDocuments()) {

                                    Map<String, Object> ride = doc.getData();
                                    if (ride == null) continue;

                                    String driverId = doc.getString("driverId");
                                    String docId = doc.getId();

                                    if (driverId != null && driverId.equals(currentUserId)) continue;
                                    if (bookedRideIds.contains(docId)) continue;

                                    rideList.add(ride);
                                    docIds.add(docId);

                                    Double lat = doc.getDouble("fromLat");
                                    Double lng = doc.getDouble("fromLng");

                                    if (lat == null || lng == null) continue;

                                    LatLng loc = new LatLng(lat, lng);

                                    String title = ride.get("from") + " → " + ride.get("to");

                                    mMap.addMarker(new MarkerOptions().position(loc).title(title));
                                }

                                listView.setAdapter(new CustomAdapter());
                            });
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rideList.size();
        }

        @Override
        public Object getItem(int position) {
            return rideList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Map<String, Object> ride = rideList.get(position);

            if (convertView == null) {

                LinearLayout layout = new LinearLayout(FindRideActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(30, 30, 30, 30);
                layout.setBackgroundColor(0xFFFFFFFF);

                TextView route = new TextView(FindRideActivity.this);
                route.setTextSize(18);

                TextView driver = new TextView(FindRideActivity.this);
                TextView phone = new TextView(FindRideActivity.this);
                TextView vehicle = new TextView(FindRideActivity.this);
                TextView seats = new TextView(FindRideActivity.this);
                TextView fare = new TextView(FindRideActivity.this);

                Button bookBtn = new Button(FindRideActivity.this);
                bookBtn.setText("Book Now");
                bookBtn.setBackgroundColor(0xFF4CAF50);
                bookBtn.setTextColor(0xFFFFFFFF);

                layout.addView(route);
                layout.addView(driver);
                layout.addView(phone);
                layout.addView(vehicle);
                layout.addView(seats);
                layout.addView(fare);
                layout.addView(bookBtn);

                convertView = layout;
            }

            LinearLayout layout = (LinearLayout) convertView;

            TextView route = (TextView) layout.getChildAt(0);
            TextView driver = (TextView) layout.getChildAt(1);
            TextView phone = (TextView) layout.getChildAt(2);
            TextView vehicle = (TextView) layout.getChildAt(3);
            TextView seats = (TextView) layout.getChildAt(4);
            TextView fare = (TextView) layout.getChildAt(5);
            Button bookBtn = (Button) layout.getChildAt(6);

            String from = ride.get("from").toString();
            String to = ride.get("to").toString();
            String driverName = ride.get("driverName").toString();
            String phoneNumber = ride.get("contactNumber").toString();
            String vehicleType = ride.get("vehicleType").toString();

            int seatCount = Integer.parseInt(ride.get("seats").toString());
            int fareValue = Integer.parseInt(ride.get("fare").toString());

            route.setText("📍 " + from + " → " + to);
            driver.setText("👤 Driver: " + driverName);
            phone.setText("📞 Phone: " + phoneNumber);
            vehicle.setText("🚗 Vehicle: " + vehicleType);
            seats.setText("💺 Seats: " + seatCount);
            fare.setText("💰 ₹" + fareValue);

            String docId = docIds.get(position);

            bookBtn.setOnClickListener(v -> showPaymentOptions(docId, fareValue, seatCount));

            return convertView;
        }
    }

    // 🔥 PAYMENT OPTIONS
    private void showPaymentOptions(String docId, int fare, int seats) {

        String[] options = {"Pay Online 💳", "Cash Payment 💵"};

        new AlertDialog.Builder(this)
                .setTitle("Select Payment")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        startPayment(fare, docId);
                    } else {
                        completeBooking(docId, fare);
                        Toast.makeText(this, "Booked with Cash", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // 💳 RAZORPAY
    private void startPayment(int fare, String rideId) {

        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_SUCmfzG5rqOy9Y");

        try {
            JSONObject options = new JSONObject();
            options.put("name", "RideCircle");
            options.put("description", "Ride Booking");
            options.put("currency", "INR");
            options.put("amount", fare * 100);

            checkout.open(this, options);

            currentRideId = rideId;

        } catch (Exception e) {
            Toast.makeText(this, "Payment error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String s) {
        Toast.makeText(this, "Payment Success", Toast.LENGTH_SHORT).show();
        completeBooking(currentRideId, 0);
    }

    @Override
    public void onPaymentError(int i, String s) {
        Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show();
    }

    private void completeBooking(String docId, int fare) {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("rides").document(docId).get().addOnSuccessListener(doc -> {

            int seats = doc.getLong("seats").intValue();

            Map<String, Object> booking = new HashMap<>();
            booking.put("rideId", docId);
            booking.put("userId", userId);
            booking.put("totalFare", fare);
            booking.put("timestamp", System.currentTimeMillis());

            db.collection("bookings").add(booking);

            db.collection("rides")
                    .document(docId)
                    .update("seats", seats - 1);
        });
    }
}