package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

public class RideAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Map<String, Object>> rideList;

    public RideAdapter(Context context, ArrayList<Map<String, Object>> rideList) {
        this.context = context;
        this.rideList = rideList;
    }

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
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_ride, parent, false);
        }

        Map<String, Object> ride = rideList.get(position);

        TextView tvRoute = convertView.findViewById(R.id.tvRoute);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvSeats = convertView.findViewById(R.id.tvSeats);

        String from = (String) ride.get("from");
        String to = (String) ride.get("to");
        String date = (String) ride.get("date");
        Object seats = ride.get("seats");

        tvRoute.setText(from + " → " + to);
        tvDate.setText("Date: " + date);
        tvSeats.setText("Seats: " + seats);

        return convertView;
    }
}
