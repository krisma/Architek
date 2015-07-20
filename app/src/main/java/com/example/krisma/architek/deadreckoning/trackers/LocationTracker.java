package com.example.krisma.architek.deadreckoning.trackers;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource;

import java.util.ArrayList;
import java.util.List;

public class LocationTracker implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, LocationSource {

    private final Context mContext;
    Location location;
    double latitude;
    double longitude;

    // Declaring a Location Manager
    protected LocationManager locationManager;
    private boolean mRequestingLocationUpdates;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    public LocationTracker(Context context) {
        this.mContext = context;

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mRequestingLocationUpdates = true;

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private LocationRequest getLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(2000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
        return mLocationRequest;
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, getLocationRequest(), this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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

        if(lastLocation != null && this.location != null) {
            float accuracyChange = lastLocation.getAccuracy() - location.getAccuracy();

            float acc = location.hasAccuracy() ? location.getAccuracy() : 0;
            Log.d("GPSTracker", getLatitude() + " : " + getLongitude() + " acc: " + acc + " change:" + accuracyChange);

            if (accuracyChange < -10) {
                Log.w("GPSTracker", "Went indoors - Change to Dead Reckoning!");
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(TRANSITION_ACTION));
            }
        }
    }

    public static final String TRANSITION_ACTION = "TRANSITION_TO_INDOOR_ACTION";

    private void updateListeners(Location location) {
        for (OnLocationChangedListener listener : listeners) {
            listener.onLocationChanged(location);
        }
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

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}