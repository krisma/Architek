package com.example.krisma.architek;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.krisma.architek.trackers.LocationTracker;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener, LocationSource.OnLocationChangedListener {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GroundOverlay groundOverlay;
    private JSONArray coordinatesHash;
    private LatLng lastPosition = new LatLng(0, 0);
    private int currentOverlayId;
    private int currentFloorNumbers;
    private int currentFloor;
    private LatLng currentBuildingLocation;
    private JSONArray currentBuildingMaps;
    private DeadReckoning deadReckoning;
    private URL currentOverlayURL;
    private Map<LatLng, GroundOverlay> overlaysHash = new HashMap<>();
    private boolean firstLoad = true;
    AsyncDrawNextFloor drawNextFloorAsyncTask;
    private Marker marker;

    // UI ELEMENTS
    private FloatingActionButton editButton;
    private FloatingActionsMenu expandMenu;
    private FloatingActionButton mMinusOneButton;
    private FloatingActionButton mPlusOneButton;
    private TextView floorView;

    private void resetOverlay() {

        groundOverlay.remove();
        currentFloor = 0;
        currentBuildingMaps = null;
        currentFloorNumbers = 0;
        currentBuildingLocation = null;
    }

    private LatLng getLagLngFromLngLat(JSONArray coordinate) {
        try {
            return new LatLng(coordinate.getDouble(1), coordinate.getDouble(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ;
        return null;
    }

    private LatLngBounds getBoundsFromJSONObject(JSONObject twoCoordinates) {
        LatLng sw = null;
        LatLng ne = null;
        try {
            JSONArray coordinatesw = twoCoordinates.getJSONArray("coordinatesw");
            JSONArray coordinatene = twoCoordinates.getJSONArray("coordinatene");
            sw = new LatLng(coordinatesw.getDouble(0), coordinatesw.getDouble(1));
            ne = new LatLng(coordinatene.getDouble(0), coordinatene.getDouble(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new LatLngBounds(sw, ne);
    }

    private void drawOverlaysNearby(){
        Log.i("Call", "Called: drawOverlaysNearby");
        for (int i=0; i<coordinatesHash.length(); i++) {
            JSONObject thisBuilding = null;
            try {
                thisBuilding = coordinatesHash.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new AsyncDrawDefaultFloor(thisBuilding).execute();
        };
    };

    private void setFocusBuilding(LatLng location) {
        Log.i("Call", "Called: setFocusBuilding");
        URL url;
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String token = getPrefs.getString("token", "");
        InputStream is = null;
        HttpURLConnection con = null;
        String param = String.valueOf(location.latitude) + "," + String.valueOf(location.longitude);

        try {
            url = new URL("https://architek-server.herokuapp.com/getbuildingmaps?" +
                    "location=" + param);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.connect();
            int responseCode = con.getResponseCode();
            is = con.getInputStream();
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(convertStreamToString(is));
            } catch (JSONException f) {
                f.printStackTrace();
            }
            ;
            Log.d("test", jObject.toString());
            currentBuildingMaps = jObject.getJSONArray("floors");
            currentFloor = 0;
            currentFloorNumbers = currentBuildingMaps.length();
            currentBuildingLocation = location;

            // TODO: Show FLoor Buttons


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                con.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ;

    private LatLng detectOverlay(LatLng currentLocation) {
        Log.i("Called: detectOverlay", "Called: detectOverlay");
        for (int i = 0; i < coordinatesHash.length(); i++) {
            try {
                LatLngBounds bounds = getBoundsFromJSONObject(coordinatesHash.getJSONObject(i)
                        .getJSONObject("twoCoordinates"));
                Log.d("coordinates", bounds.toString());
                if (bounds.contains(currentLocation)) {

                    return getLagLngFromLngLat(coordinatesHash.getJSONObject(i).getJSONArray("location"));
                }
                ;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ;
        return null;
    }

    private boolean lastMoveFarEnough(LatLng currentLocation) {
        return Math.sqrt((currentLocation.latitude - lastPosition.latitude) *
                (currentLocation.latitude - lastPosition.latitude) +
                (currentLocation.longitude - lastPosition.longitude) *
                        (currentLocation.longitude - lastPosition.longitude)) > 0.0005;
    }



    private boolean lastMoveFarFarEnough(LatLng currentLocation) {
        return Math.sqrt((currentLocation.latitude - lastPosition.latitude) *
                (currentLocation.latitude - lastPosition.latitude) +
                (currentLocation.longitude - lastPosition.longitude) *
                        (currentLocation.longitude - lastPosition.longitude)) > 0.005;
    }

    private void updateOverlays() {
        for (int i = 0; i < coordinatesHash.length(); i++) {
            try {
                JSONObject thisBuilding = coordinatesHash.getJSONObject(i);
                LatLng toDraw = getLagLngFromLngLat(thisBuilding.getJSONArray("location"));
                if (overlaysHash.containsKey(toDraw)) {
                    continue;
                } else {
                    new AsyncDrawDefaultFloor(thisBuilding).execute();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }


    public String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setUpMapIfNeeded();

        setupGUI();


        setUpMapIfNeeded();

        editButton = (FloatingActionButton) findViewById(R.id.editButton);
        editButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CameraPosition position = new CameraPosition.Builder().target(new LatLng(37.871223, -122.259060))
                        .zoom(18f)
                        .bearing(0)
                        .tilt(0)
                        .build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
            }
        });
    }
    private void setupGUI() {
        getActionBar().hide();

        //Find the buttons
        mPlusOneButton = (FloatingActionButton) findViewById(R.id.upButton);
        mMinusOneButton = (FloatingActionButton)  findViewById(R.id.downButton);

        // Find FloorView
        floorView = (TextView) findViewById(R.id.floorView);
        floorView.setShadowLayer(16, 4, 4, Color.BLACK);


        mPlusOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFloor < currentFloorNumbers) { // TODO: Should not exceed max floors
                    currentFloor++;

                    floorView.setText(String.valueOf(currentFloor));

                    onFloorChanged(currentFloor);
                }
            }
        });

        mMinusOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFloor--; // TODO: should not exceed the minimum floor level
                floorView.setText(String.valueOf(currentFloor));

                onFloorChanged(currentFloor);
            }
        });

        expandMenu = (FloatingActionsMenu) findViewById(R.id.right_menu);
        expandMenu.setSoundEffectsEnabled(true);

        // TODO: Hide FLoor Buttons


    }


    private void setOverlayImage(int floor){
        URL url = null;
        try {
            url = new URL(currentBuildingMaps.getJSONObject(floor).getString("map"));
            currentOverlayURL = url;
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            groundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(bmp));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        deadReckoning = new DeadReckoning(this, MapsActivity.this, mMap);

        mMap.setMyLocationEnabled(true);
        mMap.setOnCameraChangeListener(cameraListener);
        mMap.setLocationSource(deadReckoning.getLocationTracker());
        deadReckoning.getLocationTracker().addListener(this);

        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

//        LatLng NEWARK = new LatLng(37.871305, -122.259165);
//        GroundOverlayOptions newarkMap = new GroundOverlayOptions()
//                .image(BitmapDescriptorFactory.fromResource(R.drawable.wheeler1))
//                .position(NEWARK, 65f, 58f);
//        groundOverlay = mMap.addGroundOverlay(newarkMap);
//        groundOverlay.setBearing(-16);
//        BitmapDescriptor overlayBitmap = BitmapDescriptorFactory.fromResource(R.drawable.saintjean1);
//        currentOverlayId = R.drawable.saintjean1;
//        LatLng Saint = new LatLng(43.708827, 7.288305);
//        GroundOverlayOptions saintMap = new GroundOverlayOptions()
//                .image(BitmapDescriptorFactory.fromResource(currentOverlayId))
//                .position(Saint, 90f, 75f);
//        groundOverlay = mMap.addGroundOverlay(saintMap);
//        groundOverlay.setBearing(90);

        mMap.getUiSettings().setCompassEnabled(false);
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (firstLoad) {
                    CameraPosition position = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(18f)
                            .bearing(0)
                            .tilt(0)
                            .build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
                    firstLoad = false;
                }
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (marker != null) {
                    marker.remove();
                }
                marker = mMap.addMarker(new MarkerOptions().position(latLng));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng), new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        expandMenu.expand();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
            }
        });
        mMap.setOnMarkerClickListener(this);
    }

    private GoogleMap.OnCameraChangeListener cameraListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            if (lastMoveFarFarEnough(cameraPosition.target)) {
                new AsyncUpdateCoordinatesHash(cameraPosition.target).execute();

            }

            if (lastMoveFarEnough(cameraPosition.target)) {
                if (coordinatesHash != null) {
                    LatLng tmp = detectOverlay(cameraPosition.target);

                    if (tmp != null) {
                        setFocusBuilding(tmp);
                    } else {
                        // TODO: Hide FLoor Buttons
                    }

                }

            }

        }
    };


    public int getCurrentOverlayId() {
        return currentOverlayId;
    }

    private JSONObject currentBuilding;

    public void onFloorChanged(int currentFloor) {
        this.currentFloor = currentFloor;
        overlayCurrentFloor();
        LocalBroadcastManager.getInstance(MapsActivity.this).sendBroadcast(new Intent(LocationTracker.TRANSITION_ACTION));
    }

    public void overlayCurrentFloor(){
        if(drawNextFloorAsyncTask != null && !drawNextFloorAsyncTask.isCancelled()){
            drawNextFloorAsyncTask.cancel(true);
        }
        drawNextFloorAsyncTask = new AsyncDrawNextFloor();
        drawNextFloorAsyncTask.execute();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker != null) {
            marker.remove();
            expandMenu.collapse();
        }
        return true;
    }

    private class AsyncDrawDefaultFloor extends AsyncTask<String, Void, Bitmap> {

        private JSONObject thisBuilding = null;

        public AsyncDrawDefaultFloor(JSONObject thisBuilding) {
            this.thisBuilding = thisBuilding;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            URL url = null;
            Bitmap bmp = null;
            try {
                url = new URL(thisBuilding.getString("defaultfloor"));
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                floorplans.put(url,bmp);
                currentOverlayURL = url;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("test", url.toString());

            return bmp;

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            try {
                LatLng anchor = new LatLng((Double) thisBuilding.getJSONArray("location").get(1),
                        (Double) thisBuilding.getJSONArray("location").get(0));
                Float height = new Float(thisBuilding.getDouble("height"));
                Float width = new Float(thisBuilding.getDouble("width"));
                Float bearing = new Float(thisBuilding.getDouble("bearing"));
                Log.d("test", bearing.toString());
                GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(result))
                        .position(anchor, width, height);
                groundOverlay = mMap.addGroundOverlay(overlayOptions);
                groundOverlay.setBearing(bearing);

                currentBuilding = thisBuilding;

                GroundOverlay temp = mMap.addGroundOverlay(overlayOptions);
                overlaysHash.put(anchor, temp);
                temp.setBearing(bearing);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    private class AsyncUpdateCoordinatesHash extends AsyncTask<String, Void, JSONArray> {

        private final LatLng target;

        public AsyncUpdateCoordinatesHash(LatLng target) {
            this.target = target;
        }

        @Override
        protected JSONArray doInBackground(String... params) {
            URL url;
            HttpURLConnection con = null;
            JSONArray buildings = null;
            final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String token = getPrefs.getString("token", "");
            String coordinateParam = Double.toString(target.latitude) + "," + Double.toString(target.longitude);
            InputStream is = null;
            try {
                url = new URL("https://architek-server.herokuapp.com/getbuildingsnearby?" +
                        "coordinate=" + coordinateParam + "&" + "token=" + token);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.connect();
                int responseCode = con.getResponseCode();
                is = con.getInputStream();
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(convertStreamToString(is));
                } catch (JSONException f) {
                    f.printStackTrace();
                }
                ;
                buildings = jObject.getJSONArray("buildings");
                return buildings;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buildings;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            coordinatesHash = result;
            updateOverlays();
        }

        ;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

public JSONObject getCurrentBuilding(){
    return currentBuilding;
}


    //region Dead Reckoning

    HashMap<URL, Bitmap> floorplans = new HashMap<>();

    Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    Location currentLocation;

    List<LatLng> pointsOnRoute = new ArrayList<>();
    Polyline line;

    public GroundOverlay getCurrentOverlayObject() {
        return groundOverlay;
    }

    public URL getCurrentOverlayURL(){
        return currentOverlayURL;
    }


    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;

        if(lastLocation == null){
            lastLocation = currentLocation;
        }

        if(currentLocation.distanceTo(lastLocation) > 1){
            lastLocation = currentLocation;
            pointsOnRoute.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));


            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE);
            for (int z = 0; z < pointsOnRoute.size(); z++) {
                LatLng point = pointsOnRoute.get(z);
                options.add(point);
            }
            line = mMap.addPolyline(options);
        }
    }

    private class AsyncDrawNextFloor extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bmp = null;
            URL url = null;
            try {
                url = new URL(currentBuildingMaps.getJSONObject(currentFloor).getString("map")); // TODO: KRIS LOOK HERE -- NULL POINTER
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if(result != null) {
                GroundOverlay toSetImage = overlaysHash.get(currentBuildingLocation);
                toSetImage.setImage(BitmapDescriptorFactory.fromBitmap(result));
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}