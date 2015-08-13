package com.example.krisma.architek;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.example.krisma.architek.deadreckoning.DeadReckoning;
import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.listeners.CameraChangedListener;
import com.example.krisma.architek.listeners.PictureButtonClickListener;
import com.example.krisma.architek.listeners.FloorDownButtonClickListener;
import com.example.krisma.architek.listeners.FloorUpButtonClickListener;
import com.example.krisma.architek.listeners.MapClickListener;
import com.example.krisma.architek.listeners.MarkerClickListener;
import com.example.krisma.architek.listeners.MyLocationChangedListener;
import com.example.krisma.architek.tools.FB;
import com.example.krisma.architek.tools.OverlayHelper;
import com.example.krisma.architek.vision.FloorplanProcessor;
import com.facebook.Profile;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements LocationSource.OnLocationChangedListener, HeadingListener {

    //region Initialization

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent i = new Intent(this, DeadReckoning.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        this.startService(i);

        // setup is done in the OnServiceConnected callback to ensure object-pointers
        setupListeners();
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

    private void setupListeners(){
        markerClickListener = new MarkerClickListener(this);
        myLocationChangedListener = new MyLocationChangedListener(this);
        mapClickListener = new MapClickListener(this);
        cameraChangedListener = new CameraChangedListener(this);
        floorDownButtonClickListener = new FloorDownButtonClickListener(this);
        floorUpButtonClickListener = new FloorUpButtonClickListener(this);
        pictureButtonClickListener = new PictureButtonClickListener(this);
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        //mMap.setOnCameraChangeListener(cameraChangedListener.getCameraListener());
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
        editButton.setOnClickListener(pictureButtonClickListener.getPicButtonClickListener());
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
        log.debug("Dead Reckoning Ready.");
        deadReckoning.setActivity(MapsActivity.this);
        deadReckoning.getHeadingTracker().addListener(MapsActivity.this);
        deadReckoning.getLocationTracker().addListener(MapsActivity.this);
        mMap.setLocationSource(deadReckoning.getLocationTracker());
    }

    public void setupCameraListener(CameraPosition cameraPosition) {
        mMap.setOnCameraChangeListener(cameraChangedListener.getCameraListener());
        cameraChangedListener.updateBuildings(cameraPosition);
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
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");



                    // Convert to Matrix
                    //Mat imgMAT = new Mat (imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC1);
                    Mat tmp = new Mat();
                    Utils.bitmapToMat(imageBitmap, tmp);

                    // Detect Edges
                    FloorplanProcessor processor = new FloorplanProcessor();
                    Mat edgedMAT = processor.detectEdges(tmp);

                    // Convert to Bitmap
                    Bitmap edgedBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(edgedMAT, edgedBitmap);

                    Intent intent = new Intent(MapsActivity.this, PreviewActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("bitmap", edgedBitmap);
                    MapsActivity.this.startActivity(intent);
                }
            });
        }
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
    public void onHeadingChanged(float heading) {

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

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    log.info("OpenCV loaded successfully");

                } break;


                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


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
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private DeadReckoning deadReckoning;
    private boolean firstLoad = true;

    private long lastOverlayDetect = System.currentTimeMillis();
    private LatLngBounds currentBounds;

    private FloatingActionsMenu expandMenu;
    private FloatingActionButton mMinusOneButton;
    private FloatingActionButton mPlusOneButton;
    private TextView floorView;

    private LatLng lastPosition = new LatLng(0, 0);
    private Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    private Location currentLocation;

    private List<LatLng> pointsOnRoute = new ArrayList<>();
    private Marker marker;
    private Polyline line;

    private MarkerClickListener markerClickListener;
    private MyLocationChangedListener myLocationChangedListener;
    private MapClickListener mapClickListener;
    private CameraChangedListener cameraChangedListener;
    private FloorDownButtonClickListener floorDownButtonClickListener;
    private FloorUpButtonClickListener floorUpButtonClickListener;
    private PictureButtonClickListener pictureButtonClickListener;





    //endregion
}