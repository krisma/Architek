package com.example.krisma.architek.listeners;

import com.example.krisma.architek.MapsActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class MarkerClickListener {
    GoogleMap.OnMarkerClickListener onMarkerClickListener;

    public GoogleMap.OnMarkerClickListener getOnMarkerClickListener() {
        return onMarkerClickListener;
    }

    public MarkerClickListener(final MapsActivity mapsActivity) {
        this.onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker != null) {
                    marker.remove();
                    mapsActivity.getExpandMenu().collapse();
                }
                return true;
            }
        };
    }
}