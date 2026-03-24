package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class HistoryActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<Map<String, Object>> rideData = new ArrayList<>();

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.listView);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadRides();
    }

    private void loadRides() {

        String uid = mAuth.getCurrentUser().getUid();

        rideData.clear();

        // 🔥 YOUR RIDES
        db.collection("rides")
                .whereEqualTo("driverId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("type", "your");
                        data.put("docId", doc.getId());
                        rideData.add(data);
                    }

                    loadBookedRides(uid);
                });
    }

    private void loadBookedRides(String uid) {

        db.collection("bookings")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot bookingDoc : queryDocumentSnapshots) {

                        String rideId = bookingDoc.getString("rideId");

                        if (rideId == null) continue;

                        db.collection("rides")
                                .document(rideId)
                                .get()
                                .addOnSuccessListener(rideDoc -> {

                                    if (rideDoc.exists()) {

                                        Map<String, Object> data = rideDoc.getData();
                                        data.put("type", "booked");
                                        data.put("bookingId", bookingDoc.getId()); // 🔥 important
                                        data.put("rideId", rideId);

                                        rideData.add(data);
                                        listView.setAdapter(new CustomAdapter());
                                    }
                                });
                    }

                    listView.setAdapter(new CustomAdapter());
                });
    }

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rideData.size();
        }

        @Override
        public Object getItem(int position) {
            return rideData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Map<String, Object> ride = rideData.get(position);

            if (convertView == null) {
                LinearLayout layout = new LinearLayout(HistoryActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(30, 30, 30, 30);
                layout.setBackgroundColor(0xFFFFFFFF);

                TextView type = new TextView(HistoryActivity.this);
                TextView route = new TextView(HistoryActivity.this);
                TextView date = new TextView(HistoryActivity.this);

                Button actionBtn = new Button(HistoryActivity.this);

                layout.addView(type);
                layout.addView(route);
                layout.addView(date);
                layout.addView(actionBtn);

                convertView = layout;
            }

            LinearLayout layout = (LinearLayout) convertView;

            TextView type = (TextView) layout.getChildAt(0);
            TextView route = (TextView) layout.getChildAt(1);
            TextView date = (TextView) layout.getChildAt(2);
            Button actionBtn = (Button) layout.getChildAt(3);

            String from = ride.get("from").toString();
            String to = ride.get("to").toString();
            String dateStr = ride.get("date").toString();
            String rideType = ride.get("type").toString();

            route.setText(from + " → " + to);
            date.setText(dateStr);

            if (rideType.equals("your")) {

                type.setText("🟢 Your Ride");
                actionBtn.setText("Delete Ride");
                actionBtn.setBackgroundColor(0xFFFF4D4D);

                actionBtn.setOnClickListener(v -> {

                    String docId = ride.get("docId").toString();

                    db.collection("rides")
                            .document(docId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                Toast.makeText(HistoryActivity.this,
                                        "Ride Deleted", Toast.LENGTH_SHORT).show();

                                rideData.remove(position);
                                notifyDataSetChanged();
                            });
                });

            } else {

                type.setText("🔵 Booked Ride");
                actionBtn.setText("Cancel Booking");
                actionBtn.setBackgroundColor(0xFFFF9800);

                actionBtn.setOnClickListener(v -> {

                    new AlertDialog.Builder(HistoryActivity.this)
                            .setTitle("Cancel Booking")
                            .setMessage("Are you sure you want to cancel?")
                            .setPositiveButton("Yes", (dialog, which) -> {

                                String bookingId = ride.get("bookingId").toString();
                                String rideId = ride.get("rideId").toString();

                                // 🔥 Delete booking
                                db.collection("bookings")
                                        .document(bookingId)
                                        .delete();

                                // 🔥 Increase seat back
                                db.collection("rides")
                                        .document(rideId)
                                        .get()
                                        .addOnSuccessListener(doc -> {

                                            if (doc.exists()) {

                                                int seats = doc.getLong("seats").intValue();

                                                db.collection("rides")
                                                        .document(rideId)
                                                        .update("seats", seats + 1);
                                            }
                                        });

                                Toast.makeText(HistoryActivity.this,
                                        "Booking Cancelled", Toast.LENGTH_SHORT).show();

                                rideData.remove(position);
                                notifyDataSetChanged();

                            })
                            .setNegativeButton("No", null)
                            .show();
                });
            }

            return convertView;
        }
    }
}