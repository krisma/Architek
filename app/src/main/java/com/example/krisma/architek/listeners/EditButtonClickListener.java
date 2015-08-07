package com.example.krisma.architek.listeners;

import android.view.View;

import com.example.krisma.architek.MapsActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class EditButtonClickListener {
    View.OnClickListener editButtonClickListener;

    public View.OnClickListener getEditButtonClickListener() {
        return editButtonClickListener;
    }

    public EditButtonClickListener(final MapsActivity mapsActivity) {
        this.editButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraPosition position = new CameraPosition.Builder().target(new LatLng(37.871223, -122.259060))
                        .zoom(18f)
                        .bearing(0)
                        .tilt(0)
                        .build();
                mapsActivity.getmMap().moveCamera(CameraUpdateFactory.newCameraPosition(position));
            }
        };
    }
}