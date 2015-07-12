package com.example.krisma.architek;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);

        mMap.setOnMyLocationChangeListener(locationListener);
        mMap.setOnCameraChangeListener();


        LatLng NEWARK = new LatLng(37.871305, -122.259165);// North east corner
        GroundOverlayOptions newarkMap = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.wheeler1))
                .position(NEWARK, 65f, 58f);
        GroundOverlay groundOverlay = mMap.addGroundOverlay(newarkMap);
        groundOverlay.setBearing(-16);


    }

    private Location currentLocation;
    private boolean setUp;
    private LatLng building = new LatLng(37.871270, -122.259111);
    private LatLng buildingX = new LatLng(37.870979, -122.259416);
    private LatLng buildingY = new LatLng(37.871640, -122.258892);
    private LatLngBounds wheelerBound = new LatLngBounds(buildingX, buildingY);
    private int numberOfFloors = 4;

    GoogleMap.OnMyLocationChangeListener locationListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            currentLocation = location;
            if(!setUp){
                centerTest();
                setUp = true;
            }
        }
    };

    GoogleMap.OnCameraChangeListener cameraListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            LatLng cameraCenter = cameraPosition.target;
            if (wheelerBound.contains(cameraCenter)) {
                for (int i=0; i<numberOfFloors; i++) {

                };
            };
        }
    };

    private void centerTest(){
        CameraPosition position = new CameraPosition.Builder().target(new LatLng(37.871270, -122.259111))
                .zoom(18f)
                .bearing(0)
                .tilt(0)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
    }



    private void centerOnUser(){
        CameraPosition position = new CameraPosition.Builder().target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                .zoom(18f)
                .bearing(0)
                .tilt(0)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
    }

}
