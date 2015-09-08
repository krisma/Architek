package com.example.krisma.architek.tools;

import android.graphics.Bitmap;

import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.asynctasks.AsyncDrawDefaultFloor;
import com.example.krisma.architek.asynctasks.AsyncDrawNextFloor;
import com.example.krisma.architek.deadreckoning.utils.Helper;
import com.example.krisma.architek.deadreckoning.utils.Logger;
import com.example.krisma.architek.storing.model.Building;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 *  Overlaying Process
 *  1. Update Coordinate Bounds on Camera Update --> detectOverlay
 *  2. Find building around given location with detectOverlay()
 *  3. Focus the building found by detectOverlay
 */

public class OverlayHelper {

    private static final Logger log = new Logger(OverlayHelper.class);
    private Building building;

    public OverlayHelper(MapsActivity mapsActivity) {
        this.mapsActivity = mapsActivity;
    }

    //region Getters and Setters
    public void setBuilding(Building building){
        this.building = building;
    }

    public GroundOverlay getGroundOverlay() {
        return groundOverlay;
    }

    public void setGroundOverlay(GroundOverlay groundOverlay) {
        this.groundOverlay = groundOverlay;
    }

    public JSONArray getCoordinatesHash() {
        return coordinatesHash;
    }

    public void setCoordinatesHash(JSONArray coordinatesHash) {
        this.coordinatesHash = coordinatesHash;
    }

    public int getCurrentFloorNumbers() {
        return currentFloorNumbers;
    }

    public void setCurrentFloorNumbers(int currentFloorNumbers) {
        this.currentFloorNumbers = currentFloorNumbers;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public LatLng getCurrentBuildingLocation() {
        return currentBuildingLocation;
    }

    public void setCurrentBuildingLocation(LatLng currentBuildingLocation) {
        this.currentBuildingLocation = currentBuildingLocation;
    }

    public JSONArray getCurrentBuildingMaps() {
        return currentBuildingMaps;
    }

    public void setCurrentBuildingMaps(JSONArray currentBuildingMaps) {
        this.currentBuildingMaps = currentBuildingMaps;
    }

    public Bitmap getCurrentOverlayBitmap() {
        return currentOverlayBitmap;
    }

    public void setCurrentOverlayBitmap(Bitmap currentOverlayBitmap) {
        this.currentOverlayBitmap = currentOverlayBitmap;
    }

    public Building getCurrentBuilding() {
        return currentBuilding;
    }

    public void setCurrentBuilding(Building currentBuilding) {
        this.currentBuilding = currentBuilding;
    }

    public URL getCurrentOverlayURL() {
        return currentOverlayURL;
    }

    public void setCurrentOverlayURL(URL currentOverlayURL) {
        this.currentOverlayURL = currentOverlayURL;
    }

    public GroundOverlay getCurrentOverlayObject() {
        return groundOverlay;
    }

    public MapsActivity getMapsActivity() {
        return mapsActivity;
    }

    public Map<LatLng, GroundOverlay> getOverlaysHash() {
        return overlaysHash;
    }

    public HashMap<URL, Bitmap> getFloorplans() {
        return floorplans;
    }
    //endregion

    //region Methods
    public LatLng detectOverlay(LatLng currentLocation) {
        log.i("Called: detectOverlay");

        JSONArray buildings = getCoordinatesHash(); // latlng location for each building

        if(buildings != null) {
            for (int i = 0; i < buildings.length(); i++) {
                try {
                    LatLngBounds bounds = Helper.getBoundsFromJSONObject(buildings.getJSONObject(i).getJSONObject("twoCoordinates"));

                    log.i("coordinates : " + bounds.toString());

                    if (bounds.contains(currentLocation) && bounds != mapsActivity.getCurrentBounds()) {
                        mapsActivity.setCurrentBounds(bounds);
                        return bounds.getCenter();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public void transitionToIndoor() {
        if (!indoor) {
            mapsActivity.getDeadReckoning().setGroundOverlay(groundOverlay);
            mapsActivity.getDeadReckoning().transitionToIndoor();
            mapsActivity.getMap().setLocationSource(mapsActivity.getDeadReckoning());
            indoor = true;
        }
    }

    public void overlayCurrentFloor() {
        drawNextFloorAsyncTask = new AsyncDrawNextFloor(mapsActivity.getOverlayHelper());
        drawNextFloorAsyncTask.execute();
    }

    public void updateOverlays() {
        for (int i = 0; i < coordinatesHash.length(); i++) {
            try {
                JSONObject thisBuilding = coordinatesHash.getJSONObject(i);

                Building building = new Building(mapsActivity.getApplicationContext(), thisBuilding);


                LatLng toDraw = Helper.getLagLngFromLngLat(thisBuilding.getJSONArray("location"));
                if (overlaysHash.containsKey(toDraw)) {
                    continue;
                } else {
                    new AsyncDrawDefaultFloor(mapsActivity.getOverlayHelper(), building).execute();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void transtitionToOutdoor() {
        indoor = false;
        mapsActivity.getDeadReckoning().setParticleSet(null);
        mapsActivity.getMap().setLocationSource(mapsActivity.getDeadReckoning().getLocationTracker());
    }

    public boolean lastMoveFarEnough(LatLng currentLocation, LatLng last) {
        return Math.sqrt((currentLocation.latitude - last.latitude) *
                (currentLocation.latitude - last.latitude) +
                (currentLocation.longitude - last.longitude) *
                        (currentLocation.longitude - last.longitude)) > 0.0005;
    }

    public boolean lastMoveFarFarEnough(LatLng currentLocation, LatLng last) {
        return Math.sqrt((currentLocation.latitude - last.latitude) *
                (currentLocation.latitude - last.latitude) +
                (currentLocation.longitude - last.longitude) *
                        (currentLocation.longitude - last.longitude)) > 0.005;
    }

    public void changeFloor(int currentFloor) {
        setCurrentFloor(currentFloor);
        overlayCurrentFloor();
    }

    public void changeFloor() {
        mapsActivity.getDeadReckoning().setGroundOverlay(getGroundOverlay());
        mapsActivity.getDeadReckoning().transitionToIndoor();
        mapsActivity.getMap().setLocationSource(mapsActivity.getDeadReckoning());
    }

    public void showParticles(List<LatLng> locs, MapsActivity mapsActivity) {
        mOverlay.remove();
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(locs)
                .build();
        mOverlay = mapsActivity.getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));


//        for(Marker m : particles){
//            m.remove();
//        }
//
//        particles.clear();
//
//        for(LatLng l : locs){
//            particles.add(mMap.addMarker(new MarkerOptions()
//                    .position(l)
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.dot))));
//        }
    }

    //endregion

    //region Fields and Constants
    private final MapsActivity mapsActivity;
    GroundOverlay groundOverlay;
    JSONArray coordinatesHash;
    int currentFloorNumbers;
    int currentFloor = 0;
    LatLng currentBuildingLocation;
    JSONArray currentBuildingMaps;
    URL currentOverlayURL;
    Map<LatLng, GroundOverlay> overlaysHash = new HashMap<LatLng, GroundOverlay>();
    AsyncDrawNextFloor drawNextFloorAsyncTask;
    Building currentBuilding;
    HashMap<URL, Bitmap> floorplans = new HashMap<URL, Bitmap>();
    boolean indoor;
    Bitmap currentOverlayBitmap;
    private List<Marker> particles = new ArrayList<>();

    private TileOverlay mOverlay;




    //endregion
}