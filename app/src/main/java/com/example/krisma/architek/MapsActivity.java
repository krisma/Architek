package com.example.krisma.architek;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.krisma.architek.deadreckoning.DeadReckoning;
import com.example.krisma.architek.deadreckoning.trackers.listeners.FloorListener;
import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.deadreckoning.trackers.LocationTracker;
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
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener, LocationSource.OnLocationChangedListener, HeadingListener, FloorListener {

    private static final Logger log = LoggerFactory.getLogger(MapsActivity.class);

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
    private JSONObject currentBuilding;


    // UI ELEMENTS
    private FloatingActionButton editButton;
    private FloatingActionsMenu expandMenu;
    private FloatingActionButton mMinusOneButton;
    private FloatingActionButton mPlusOneButton;
    private TextView floorView;
    private RelativeLayout mapLayout;

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

    private void drawOverlaysNearby() {
        Log.i("Call", "Called: drawOverlaysNearby");
        for (int i = 0; i < coordinatesHash.length(); i++) {
            JSONObject thisBuilding = null;
            try {
                thisBuilding = coordinatesHash.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new AsyncDrawDefaultFloor(thisBuilding).execute();
        }
        ;
    }

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

            transitionToIndoor();
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

        Intent i= new Intent(this, DeadReckoning.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        this.startService(i);


        // setup is done in the OnServiceConnected callback to ensure object-pointers
        setUpMapIfNeeded();
        setupGUI();


    }

    private void setupGUI() {
        getActionBar().hide();

        mapLayout = (RelativeLayout) findViewById(R.id.mapLayout);
        mapLayout.setDrawingCacheEnabled(true);

        //Find the buttons
        mPlusOneButton = (FloatingActionButton) findViewById(R.id.upButton);
        mMinusOneButton = (FloatingActionButton) findViewById(R.id.downButton);

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

        // TODO: Hide FLoor Buttons

    }

    public int getColorOnMapFromScreenPosition(Point pos) {
        mapLayout.buildDrawingCache();

        Bitmap bitmap = mapLayout.getDrawingCache();

        int pixel = bitmap.getPixel(pos.x, pos.y);

        return pixel;
    }

    private void setOverlayImage(int floor) {
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

    private void setUpMap() {

        mMap.setMyLocationEnabled(true);
        mMap.setOnCameraChangeListener(cameraListener);
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

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            deadReckoning = ((DeadReckoning.LocalBinder)service).getService();
            deadReckoning.setMapsActivity(MapsActivity.this);
            deadReckoning.getHeadingTracker().addListener(MapsActivity.this);
            deadReckoning.getLocationTracker().addListener(MapsActivity.this);
            mMap.setLocationSource(deadReckoning.getLocationTracker());
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            deadReckoning = null;
        }
    };

    public void onFloorChanged(int currentFloor) {
        this.currentFloor = currentFloor;
        overlayCurrentFloor();
        LocalBroadcastManager.getInstance(MapsActivity.this).sendBroadcast(new Intent(LocationTracker.TRANSITION_ACTION));
    }

    public void overlayCurrentFloor() {
        if (drawNextFloorAsyncTask != null && !drawNextFloorAsyncTask.isCancelled()) {
            drawNextFloorAsyncTask.cancel(true);
        }
        drawNextFloorAsyncTask = new AsyncDrawNextFloor();
        drawNextFloorAsyncTask.execute();
    }

    public GoogleMap getMmap() {
        return mMap;
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
                //bmp = BitmapFactory.decodeResource(getResources(), R.drawable.wheeler11_edged_test_coords);
                floorplans.put(url, bmp);
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

                log.info("From Server || H: {}, W: {}, B: {}", height, width, bearing);

                GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(result))
                        .position(anchor, width, height);

                groundOverlay = mMap.addGroundOverlay(overlayOptions);
                groundOverlay.setBearing(bearing);

                transitionToIndoor();

                deadReckoning.setGroundOverlay(groundOverlay);
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

    public JSONObject getCurrentBuilding() {
        return currentBuilding;
    }

    HashMap<URL, Bitmap> floorplans = new HashMap<>();

    Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    Location currentLocation;

    List<LatLng> pointsOnRoute = new ArrayList<>();
    Polyline line;

    public GroundOverlay getCurrentOverlayObject() {
        return groundOverlay;
    }

    public URL getCurrentOverlayURL() {
        return currentOverlayURL;
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
            if (result != null) {
                GroundOverlay toSetImage = overlaysHash.get(currentBuildingLocation);
                toSetImage.setImage(BitmapDescriptorFactory.fromBitmap(result));
                transitionToIndoor();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    public void transitionToIndoor(){
        deadReckoning.transitionToIndoor();
        mMap.setLocationSource(deadReckoning);
    }

    public void transtitionToOutdoor(){
        deadReckoning.setParticleSet(null);
        mMap.setLocationSource(deadReckoning.getLocationTracker());
    }

    //region Map Listeners
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker != null) {
            marker.remove();
            expandMenu.collapse();
        }
        return true;
    }

    private long lastOverlayDetect = System.currentTimeMillis();
    private GoogleMap.OnCameraChangeListener cameraListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            if (lastMoveFarFarEnough(cameraPosition.target)) {
                new AsyncUpdateCoordinatesHash(cameraPosition.target).execute();

            }

            if (lastMoveFarEnough(cameraPosition.target)) {
                if (coordinatesHash != null && System.currentTimeMillis() > lastOverlayDetect + 4000) {
                    lastOverlayDetect = System.currentTimeMillis();
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

    //endregion

    //region Tracker Listeners
    Marker marker;
    @Override
    public void onLocationChanged(Location location) {

        if(location.getProvider().equals("DEAD_RECKONING")){
            marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .title("DR"));
        }


        currentLocation = location;

        if (lastLocation == null) {
            lastLocation = currentLocation;
        }

        if (currentLocation.distanceTo(lastLocation) > 1) {
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

    @Override
    public void onHeadingChange(float heading) {

        if (mMap == null) return;

        Location loc = mMap.getMyLocation();


        if (loc == null) return;

        CameraPosition position = new CameraPosition.Builder().target(new LatLng(loc.getLatitude(), loc.getLongitude()))
                .zoom(18f)
                .bearing(heading)
                .tilt(0)
                .build();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
    }
    //endregion
}