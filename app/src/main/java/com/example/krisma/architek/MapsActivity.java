package com.example.krisma.architek;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.example.krisma.architek.deadreckoning.DeadReckoning;
import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.listeners.CameraChangedListener;
import com.example.krisma.architek.listeners.EditButtonClickListener;
import com.example.krisma.architek.listeners.FloorDownButtonClickListener;
import com.example.krisma.architek.listeners.FloorUpButtonClickListener;
import com.example.krisma.architek.listeners.MapClickListener;
import com.example.krisma.architek.listeners.MarkerClickListener;
import com.example.krisma.architek.listeners.MyLocationChangedListener;
import com.example.krisma.architek.tools.FB;
import com.example.krisma.architek.tools.OverlayHelper;
import com.facebook.Profile;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements LocationSource.OnLocationChangedListener, HeadingListener {
    final MarkerClickListener markerClickListener = new MarkerClickListener(this);
    final MyLocationChangedListener myLocationChangedListener = new MyLocationChangedListener(this);
    final MapClickListener mapClickListener = new MapClickListener(this);
    final CameraChangedListener cameraChangedListener = new CameraChangedListener(this);
    final FloorDownButtonClickListener floorDownButtonClickListener = new FloorDownButtonClickListener(this);
    final FloorUpButtonClickListener floorUpButtonClickListener = new FloorUpButtonClickListener(this);
    final EditButtonClickListener editButtonClickListener = new EditButtonClickListener(this);

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent i = new Intent(this, DeadReckoning.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        this.startService(i);

        // setup is done in the OnServiceConnected callback to ensure object-pointers
        setUpMapIfNeeded();
        setupGUI();

        // Facebook Profile
        if (Profile.getCurrentProfile() != null) {
            log.info("logged in as {} ({})", Profile.getCurrentProfile().getName(), Profile.getCurrentProfile().getLinkUri().toString());
            log.info("Friends : {}", FB.getMyFriends());
        }
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
        mMap.setOnCameraChangeListener(cameraChangedListener.getCameraListener());
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.setOnMyLocationChangeListener(myLocationChangedListener.getOnMyLocationChangeListener());
        mMap.setOnMapClickListener(mapClickListener.getOnMapClickListener());
        mMap.setOnMarkerClickListener(markerClickListener.getOnMarkerClickListener());
    }

    private void setupGUI() {
        ActionBar ab = getActionBar();
        if (ab != null) ab.hide();

        floorView = (TextView) findViewById(R.id.floorView);
        floorView.setShadowLayer(16, 4, 4, Color.BLACK);
        floorView.setVisibility(View.INVISIBLE);

        mPlusOneButton = (FloatingActionButton) findViewById(R.id.upButton);
        mPlusOneButton.setOnClickListener(floorUpButtonClickListener.getPlusButtonClickListener());
        mPlusOneButton.setVisibility(View.INVISIBLE);

        mMinusOneButton = (FloatingActionButton) findViewById(R.id.downButton);
        mMinusOneButton.setOnClickListener(floorDownButtonClickListener.getMinusButtonClickListener());
        mMinusOneButton.setVisibility(View.INVISIBLE);

        expandMenu = (FloatingActionsMenu) findViewById(R.id.right_menu);
        expandMenu.setSoundEffectsEnabled(true);

        FloatingActionButton editButton = (FloatingActionButton) findViewById(R.id.editButton);
        editButton.setOnClickListener(editButtonClickListener.getEditButtonClickListener());
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            deadReckoning = ((DeadReckoning.LocalBinder) service).getService();
            onDeadReckoningReady();
        }

        public void onServiceDisconnected(ComponentName className) {
            deadReckoning = null;
        }
    };

    private void onDeadReckoningReady(){
        deadReckoning.setActivity(MapsActivity.this);
        deadReckoning.getHeadingTracker().addListener(MapsActivity.this);
        deadReckoning.getLocationTracker().addListener(MapsActivity.this);
        mMap.setLocationSource(deadReckoning.getLocationTracker());
    }

    //endregion

    public void showFloorButtons(boolean showButtons) {
        if (showButtons) {
            mMinusOneButton.setVisibility(View.VISIBLE);
            mPlusOneButton.setVisibility(View.VISIBLE);
            floorView.setVisibility(View.VISIBLE);
        } else {
            mMinusOneButton.setVisibility(View.INVISIBLE);
            mPlusOneButton.setVisibility(View.INVISIBLE);
            floorView.setVisibility(View.INVISIBLE);
        }
    }

    private void showTravelledPath() {
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE);
        for (int z = 0; z < pointsOnRoute.size(); z++) {
            LatLng point = pointsOnRoute.get(z);
            options.add(point);
        }
        line = mMap.addPolyline(options);
    }

    @Override
    public void onLocationChanged(Location location) {

        currentLocation = location;

        if (lastLocation == null) {
            lastLocation = currentLocation;
        }

        if (currentLocation.distanceTo(lastLocation) > 1) {
            lastLocation = currentLocation;
            pointsOnRoute.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));

            //showTravelledPath();
        }
    }

    @Override
    public void onHeadingChange(float heading) {

        // used to rotate the map according to heading

        /*
        if (mMap == null) return;

        Location loc = mMap.getMyLocation();


        if (loc == null) return;

        CameraPosition position = new CameraPosition.Builder().target(new LatLng(loc.getLatitude(), loc.getLongitude()))
                .zoom(20f)
                .bearing(heading)
                .tilt(0)
                .build();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        */
    }

    //region Getters and Setters
    public void setCurrentBounds(LatLngBounds currentBounds) {
        this.currentBounds = currentBounds;
    }

    public LatLngBounds getCurrentBounds() {
        return currentBounds;
    }

    public DeadReckoning getDeadReckoning() {
        return deadReckoning;
    }

    public GoogleMap getMap() {
        return mMap;
    }

    public OverlayHelper getOverlayHelper() {
        return overlayHelper;
    }

    public TextView getFloorView() {
        return floorView;
    }

    public FloatingActionsMenu getExpandMenu() {
        return expandMenu;
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }

    public GoogleMap getmMap() {
        return mMap;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public LatLng getLastPosition() {
        return lastPosition;
    }

    public FloatingActionButton getmMinusOneButton() {
        return mMinusOneButton;
    }

    public long getLastOverlayDetect() {
        return lastOverlayDetect;
    }

    public FloatingActionButton getmPlusOneButton() {
        return mPlusOneButton;
    }

    public void setLastOverlayDetect(long lastOverlayDetect) {
        this.lastOverlayDetect = lastOverlayDetect;
    }
    //endregion

    //region Fields and Constants
    private static final Logger log = LoggerFactory.getLogger(MapsActivity.class);
    public final OverlayHelper overlayHelper = new OverlayHelper(this);

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private LatLng lastPosition = new LatLng(0, 0);
    private DeadReckoning deadReckoning;
    private boolean firstLoad = true;
    private Marker marker;
    private long lastOverlayDetect = System.currentTimeMillis();
    private LatLngBounds currentBounds;

    private FloatingActionsMenu expandMenu;
    private FloatingActionButton mMinusOneButton;
    private FloatingActionButton mPlusOneButton;
    private TextView floorView;

    private Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    private Location currentLocation;

    private List<LatLng> pointsOnRoute = new ArrayList<>();

    private Polyline line;

    //endregion
}