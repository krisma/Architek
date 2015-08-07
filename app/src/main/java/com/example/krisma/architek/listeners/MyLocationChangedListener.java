package com.example.krisma.architek.listeners;

import android.location.Location;

import com.example.krisma.architek.MapsActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MyLocationChangedListener {
    GoogleMap.OnMyLocationChangeListener onMyLocationChangeListener;

    public GoogleMap.OnMyLocationChangeListener getOnMyLocationChangeListener() {
        return onMyLocationChangeListener;
    }

    public MyLocationChangedListener(final MapsActivity mapsActivity) {
        this.onMyLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (mapsActivity.isFirstLoad()) {
                    CameraPosition position = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(18f)
                            .bearing(0)
                            .tilt(0)
                            .build();
                    mapsActivity.getmMap().moveCamera(CameraUpdateFactory.newCameraPosition(position));
                    mapsActivity.setFirstLoad(false);
                }
            }
        };
    }
}