package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class RideMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    double fromLat, fromLng, toLat, toLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_map);

        fromLat = getIntent().getDoubleExtra("fromLat", 0);
        fromLng = getIntent().getDoubleExtra("fromLng", 0);
        toLat = getIntent().getDoubleExtra("toLat", 0);
        toLng = getIntent().getDoubleExtra("toLng", 0);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        LatLng from = new LatLng(fromLat, fromLng);
        LatLng to = new LatLng(toLat, toLng);

        mMap.addMarker(new MarkerOptions().position(from).title("From"));
        mMap.addMarker(new MarkerOptions().position(to).title("To"));

        mMap.addPolyline(new PolylineOptions()
                .add(from, to)
                .width(8)
                .color(Color.BLUE));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(from, 10));
    }
}