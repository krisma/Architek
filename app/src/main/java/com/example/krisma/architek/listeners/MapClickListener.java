package com.example.krisma.architek.listeners;

import com.example.krisma.architek.MapsActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapClickListener {
    GoogleMap.OnMapClickListener onMapClickListener;

    public GoogleMap.OnMapClickListener getOnMapClickListener() {
        return onMapClickListener;
    }

    public MapClickListener(final MapsActivity mapsActivity) {
        this.onMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mapsActivity.getMarker() != null) {
                    mapsActivity.getMarker().remove();
                }
                mapsActivity.setMarker(mapsActivity.getmMap().addMarker(new MarkerOptions().position(latLng)));
                mapsActivity.getmMap().animateCamera(CameraUpdateFactory.newLatLng(latLng), new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        mapsActivity.getExpandMenu().expand();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
            }
        };
    }
}