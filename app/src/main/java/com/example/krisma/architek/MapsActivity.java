package com.example.krisma.architek;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.krisma.architek.deadreckoning.DeadReckoning;
import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.deadreckoning.utils.Helper;
import com.facebook.Profile;
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

public class MapsActivity extends FragmentActivity implements LocationSource.OnLocationChangedListener, HeadingListener{

    //region Fields and Constants

    private static final Logger log = LoggerFactory.getLogger(MapsActivity.class);

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GroundOverlay groundOverlay;
    private JSONArray coordinatesHash;
    private LatLng lastPosition = new LatLng(0, 0);
    private int currentOverlayId;
    private int currentFloorNumbers;
    private int currentFloor = 0;
    private LatLng currentBuildingLocation;
    private JSONArray currentBuildingMaps;
    private DeadReckoning deadReckoning;
    private URL currentOverlayURL;
    private Map<LatLng, GroundOverlay> overlaysHash = new HashMap<>();
    private boolean firstLoad = true;
    AsyncDrawNextFloor drawNextFloorAsyncTask;
    private JSONObject currentBuilding;
    HashMap<URL, Bitmap> floorplans = new HashMap<>();


    // UI ELEMENTS
    private FloatingActionButton editButton;
    private FloatingActionsMenu expandMenu;
    private FloatingActionButton mMinusOneButton;
    private FloatingActionButton mPlusOneButton;
    private TextView floorView;
    private boolean indoor;
    private Bitmap currentOverlayBitmap;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Intent i = new Intent(this, DeadReckoning.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        this.startService(i);

        // setup is done in the OnServiceConnected callback to ensure object-pointers
        setUpMapIfNeeded();
        setupGUI();

        // Facebook Profile
        log.info("logged in as {} ({})", Profile.getCurrentProfile().getName(), Profile.getCurrentProfile().getLinkUri().toString());
        log.info("Friends : {}", FB.getMyFriends());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

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
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.setOnMyLocationChangeListener(onMyLocationChangeListener);
        mMap.setOnMapClickListener(onMapClickListener);
        mMap.setOnMarkerClickListener(onMarkerClickListener);
    }

    private void setupGUI() {
        getActionBar().hide();

        floorView = (TextView) findViewById(R.id.floorView);
        floorView.setShadowLayer(16, 4, 4, Color.BLACK);
        floorView.setVisibility(View.INVISIBLE);

        mPlusOneButton = (FloatingActionButton) findViewById(R.id.upButton);
        mPlusOneButton.setOnClickListener(plusButtonClickListener);
        mPlusOneButton.setVisibility(View.INVISIBLE);

        mMinusOneButton = (FloatingActionButton) findViewById(R.id.downButton);
        mMinusOneButton.setOnClickListener(minusButtonClickListener);
        mMinusOneButton.setVisibility(View.INVISIBLE);

        expandMenu = (FloatingActionsMenu) findViewById(R.id.right_menu);
        expandMenu.setSoundEffectsEnabled(true);

        editButton = (FloatingActionButton) findViewById(R.id.editButton);
        editButton.setOnClickListener(editButtonClickListener);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            deadReckoning = ((DeadReckoning.LocalBinder)service).getService();
            deadReckoning.setActivity(MapsActivity.this);
            deadReckoning.getHeadingTracker().addListener(MapsActivity.this);
            deadReckoning.getLocationTracker().addListener(MapsActivity.this);
            mMap.setLocationSource(deadReckoning.getLocationTracker());
        }

        public void onServiceDisconnected(ComponentName className) {
            deadReckoning = null;
        }
    };

    //endregion

    //region Floor Drawing Flow


    // 1. Update Coordinate Bounds on Camera Update --> detectOverlay
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
                        mMinusOneButton.setVisibility(View.INVISIBLE);
                        mPlusOneButton.setVisibility(View.INVISIBLE);
                        floorView.setVisibility(View.INVISIBLE);
                    }

                }

            }

        }
    };

    // 2. Find building around given location
    private LatLngBounds currentBounds;
    private LatLng detectOverlay(LatLng currentLocation) {
        Log.i("Called: detectOverlay", "Called: detectOverlay");
        for (int i = 0; i < coordinatesHash.length(); i++) {
            try {
                LatLngBounds bounds = Helper.getBoundsFromJSONObject(coordinatesHash.getJSONObject(i)
                        .getJSONObject("twoCoordinates"));
                Log.d("coordinates", bounds.toString());
                if (bounds.contains(currentLocation) && bounds != currentBounds) {
                    currentBounds = bounds;
                    return Helper.getLagLngFromLngLat(coordinatesHash.getJSONObject(i).getJSONArray("location"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    // 3. Focus the building found by detectOverlay
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
                jObject = new JSONObject(Helper.convertStreamToString(is));
            } catch (JSONException f) {
                f.printStackTrace();
            }
            ;
            Log.d("test", jObject.toString());

            currentBuildingMaps = jObject.getJSONArray("floors");
            log.info(currentBuildingMaps.toString());

            currentFloorNumbers = currentBuildingMaps.length();
            currentBuildingLocation = location;

            currentFloor = 0;
            floorView.setText(0 + "");
            floorView.invalidate();

            mMinusOneButton.setVisibility(View.VISIBLE);
            mPlusOneButton.setVisibility(View.VISIBLE);
            floorView.setVisibility(View.VISIBLE);

            overlayCurrentFloor();

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

    public void transitionToIndoor(){
        if(!indoor){
            deadReckoning.setGroundOverlay(groundOverlay);
            deadReckoning.transitionToIndoor();
            mMap.setLocationSource(deadReckoning);
            indoor = true;
        }
    }

    public void changeFloor(int currentFloor) {
        this.currentFloor = currentFloor;
        overlayCurrentFloor();
    }

    public void overlayCurrentFloor() {
//        if (drawNextFloorAsyncTask != null && !drawNextFloorAsyncTask.isCancelled()) {
//            drawNextFloorAsyncTask.cancel(true);
//        }
        drawNextFloorAsyncTask = new AsyncDrawNextFloor();
        drawNextFloorAsyncTask.execute();
    }

    private void updateOverlays() {
        for (int i = 0; i < coordinatesHash.length(); i++) {
            try {
                JSONObject thisBuilding = coordinatesHash.getJSONObject(i);
                LatLng toDraw = Helper.getLagLngFromLngLat(thisBuilding.getJSONArray("location"));
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


    //endregion

    //region Async Tasks
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
                    jObject = new JSONObject(Helper.convertStreamToString(is));
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

                setCurrentOverlayBitmap(result);
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

                deadReckoning.setGroundOverlay(groundOverlay);
                currentBuilding = thisBuilding;

                for(GroundOverlay g : overlaysHash.values()){
                    g.remove();
                }

                GroundOverlay temp = mMap.addGroundOverlay(overlayOptions);
                overlaysHash.put(anchor, temp);
                temp.setBearing(bearing);

                transitionToIndoor();
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
                for(GroundOverlay g : overlaysHash.values()){
                    g.remove();
                }

                GroundOverlay toSetImage = overlaysHash.get(currentBuildingLocation);
                toSetImage.setImage(BitmapDescriptorFactory.fromBitmap(result));

                transitionToIndoor();
                overlayCurrentFloor();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    //endregion

    //region Helper Methods
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

    //endregion

    //region Getters and Setters
    public GoogleMap getMmap() {
        return mMap;
    }

    public Bitmap getCurrentOverlayBitmap() {
        return currentOverlayBitmap;
    }

    public void setCurrentOverlayBitmap(Bitmap currentOverlayBitmap) {
        this.currentOverlayBitmap = currentOverlayBitmap;
    }

    public JSONObject getCurrentBuilding() {
        return currentBuilding;
    }

    public URL getCurrentOverlayURL() {
        return currentOverlayURL;
    }
    //endregion

    //region Dead Reckoning
    Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    Location currentLocation;

    List<LatLng> pointsOnRoute = new ArrayList<>();
    Polyline line;

    List<Marker> particles = new ArrayList<>();

    public void showParticles(List<LatLng> locs){

        for(Marker m : particles){
            m.remove();
        }

        particles.clear();

        for(LatLng l : locs){
            particles.add(mMap.addMarker(new MarkerOptions()
                    .position(l)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.dot))));
        }
    }

    //endregion

    //region Listeners
    Marker marker;

    GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker != null) {
                marker.remove();
                expandMenu.collapse();
            }
            return true;
        }
    };

    GoogleMap.OnMyLocationChangeListener onMyLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
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
    };

    GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
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
    };


    View.OnClickListener minusButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            currentFloor--; // TODO: should not exceed the minimum floor level
            floorView.setText(String.valueOf(currentFloor));

            changeFloor(currentFloor);
        }
    };

    View.OnClickListener plusButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (currentFloor < currentFloorNumbers) { // TODO: Should not exceed max floors
                currentFloor++;

                floorView.setText(String.valueOf(currentFloor));

                changeFloor(currentFloor);
            }
        }
    };

    View.OnClickListener editButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CameraPosition position = new CameraPosition.Builder().target(new LatLng(37.871223, -122.259060))
                    .zoom(18f)
                    .bearing(0)
                    .tilt(0)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
    };

    @Override
    public void onLocationChanged(Location location) {

        currentLocation = location;

        if (lastLocation == null) {
            lastLocation = currentLocation;
        }

        if (currentLocation.distanceTo(lastLocation) > 1) {
            lastLocation = currentLocation;
            pointsOnRoute.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));

            updatePath();


        }
    }

    private void updatePath() {
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE);
        for (int z = 0; z < pointsOnRoute.size(); z++) {
            LatLng point = pointsOnRoute.get(z);
            options.add(point);
        }
        line = mMap.addPolyline(options);
    }


    private float currentBearing = 0;
    @Override
    public void onHeadingChange(float heading) {

        if (mMap == null) return;

        Location loc = mMap.getMyLocation();


        if (loc == null) return;

            CameraPosition position = new CameraPosition.Builder().target(new LatLng(loc.getLatitude(), loc.getLongitude()))
                    .zoom(20f)
                    .bearing(heading)
                    .tilt(0)
                    .build();

        //mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));

    }
    //endregion

    //region Currently Unused
    public GroundOverlay getCurrentOverlayObject() {
        return groundOverlay;
    }

    public void changeFloor(){
        deadReckoning.setGroundOverlay(groundOverlay);
        deadReckoning.transitionToIndoor();
        mMap.setLocationSource(deadReckoning);
    }

    public void transtitionToOutdoor(){
        indoor = false;
        deadReckoning.setParticleSet(null);
        mMap.setLocationSource(deadReckoning.getLocationTracker());
    }
    //endregion
}

