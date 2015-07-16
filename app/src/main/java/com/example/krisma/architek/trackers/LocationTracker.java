package com.example.krisma.architek.trackers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.LocationSource;

import java.util.ArrayList;
import java.util.List;

public class LocationTracker implements LocationListener, LocationSource {

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0;

    // Declaring a Location Manager
    protected LocationManager locationManager;
    private Location currentLocation;

    public LocationTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(LocationTracker.this);
        }
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    @Override
    public void onLocationChanged(Location location) {
        Location lastLocation = this.location;

        updateListeners(location);

        this.location = location;

        float accuracyChange = lastLocation.getAccuracy() - location.getAccuracy();

        float acc = location.hasAccuracy() ? location.getAccuracy() : 0;
        Log.d("GPSTracker", getLatitude() + " : " + getLongitude() + " acc: " + acc + " change:" + accuracyChange);

        if (accuracyChange < -10) {
            Log.w("GPSTracker", "Went indoors - Change to Dead Reckoning!");
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(TRANSITION_ACTION));
        }


    }

    public static final String TRANSITION_ACTION = "TRANSITION_TO_INDOOR_ACTION";

    private void updateListeners(Location location) {
        for (OnLocationChangedListener listener : listeners) {
            listener.onLocationChanged(location);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private List<OnLocationChangedListener> listeners = new ArrayList<>();

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        if (onLocationChangedListener != null) {
            listeners.add(onLocationChangedListener);
        }
    }

    @Override
    public void deactivate() {
        listeners = new ArrayList<>();
    }

    public void addListener(OnLocationChangedListener listener) {
        this.listeners.add(listener);
    }

    public Location getCurrentLocation(){
        return location;
    }
}