package com.example.krisma.architek.deadreckoning;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.deadreckoning.particlefilter.Move;
import com.example.krisma.architek.deadreckoning.particlefilter.ParticleSet;
import com.example.krisma.architek.deadreckoning.particlefilter.Pose;
import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.deadreckoning.trackers.HeadingTracker;
import com.example.krisma.architek.deadreckoning.trackers.LocationTracker;
import com.example.krisma.architek.deadreckoning.trackers.listeners.MoveListener;
import com.example.krisma.architek.deadreckoning.trackers.MovementTracker;
import com.example.krisma.architek.deadreckoning.utils.Mapper;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smp on 14/07/15.
 */
public class DeadReckoning extends Service implements MoveListener, HeadingListener, LocationSource {

    //region Fields and Constants
    private Context mContext;
    private Mapper mapper;
    private static final Logger log = LoggerFactory.getLogger(DeadReckoning.class);

    // Trackers
    private LocationTracker locationTracker;
    private MovementTracker movementTracker;
    private double heading;


    private ParticleSet particleSet;
    private HeadingTracker headingTracker;
    private GroundOverlay overlay;
    private MapsActivity activity;

    //endregion

    //region Initialization
    public DeadReckoning() {

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        if (onLocationChangedListener != null) {
            onLocationChangedListeners.add(onLocationChangedListener);
        }
    }

    @Override
    public void deactivate() {
        onLocationChangedListeners = new ArrayList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }


    public class LocalBinder extends Binder {
        public DeadReckoning getService() {
            return DeadReckoning.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;

        LocalBroadcastManager.getInstance(mContext).registerReceiver(transitionReceiver, new IntentFilter(LocationTracker.TRANSITION_ACTION));

        setupHeadingTracker();
        setupLocationTracker();
        setupMovementTracker();

        return Service.START_NOT_STICKY;
    }

    private void setupLocationTracker() {
        locationTracker = new LocationTracker(mContext);
        locationTracker.addListener(headingTracker);
    }

    private void setupHeadingTracker() {
        headingTracker = new HeadingTracker(mContext);
        headingTracker.addListener(this);
    }

    private void setupMovementTracker() {
        movementTracker = new MovementTracker(mContext);
        movementTracker.addMoveListener(this);
    }

    //endregion

    //region Event Callbacks
    @Override
    public void onMove(final Move m) {

        if (particleSet != null) {
            Move move = m;
            move.setHeading(heading);

            int tickDistance = 5;

            float distanceLeft = move.getDistanceTraveled();

            while (distanceLeft - tickDistance > tickDistance) {
                Move tmp = move;
                tmp.setDistance(tickDistance);
                particleSet.updateParticles(tmp);
                distanceLeft = distanceLeft - tickDistance;
            }

            Move tmp = move;
            tmp.setDistance(distanceLeft);
            particleSet.updateParticles(tmp);

            Log.d("Move", "Moved " + move.getDistanceTraveled() + " in heading: " + move.getHeading());

            // Transform back to Google Map -- Show location on Map by updating onLocationChangedListeners --> MapsActivity included
            Pose averagePose = particleSet.getAveragePose();
            LatLng newPos = mapper.pointToRotatedLatLng(new Point((int) averagePose.getY(), (int) averagePose.getX()));

            Location loc = new Location("DEAD_RECKONING");
            loc.setLatitude(newPos.latitude);
            loc.setLongitude(newPos.longitude);

            updateListeners(loc);
        }

    }

    @Override
    public void onHeadingChange(float heading) {
        this.heading = heading;
    }

    //endregion

    //region Transitioning

    private void retryTransition() {
        Log.w("Transition", "Location Was Null! Retrying in 2 seconds!");
        new Handler().postDelayed(transitionToIndoorRunnable, 4000);
    }

    BroadcastReceiver transitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transitionToIndoor();
        }
    };

    private Runnable transitionToIndoorRunnable = new Runnable() {
        @Override
        public void run() {
            transitionToIndoor();
        }
    };

    public void transitionToIndoor() {
        Location location = locationTracker.getCurrentLocation();
        Log.d("Transition", "Started");

        if (location == null || activity.getCurrentOverlayURL() == null) {
            retryTransition();
            Log.d("Transition", "Null");
        } else {
            Log.d("Transition", "In progress");
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(activity.getCurrentOverlayURL().openConnection().getInputStream());

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Transformation to the Image
            Point iCenter = new Point(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
            Point iNW = new Point(0, 0);

            LatLng oCenter = overlay.getPosition();

            log.info("Overlay Center: {}", oCenter);
            log.info("SW: {}", overlay.getBounds().southwest);

            JSONObject corners = getCorners(); // SE, NW, NE, SW

            try {
                LatLng SW = new LatLng(corners.getJSONArray("coordinate1").getDouble(0), corners.getJSONArray("coordinate1").getDouble(1));
                LatLng NE = new LatLng(corners.getJSONArray("coordinate2").getDouble(0), corners.getJSONArray("coordinate2").getDouble(1));
                LatLng NW = new LatLng(corners.getJSONArray("coordinate3").getDouble(0), corners.getJSONArray("coordinate3").getDouble(1));
                LatLng SE = new LatLng(corners.getJSONArray("coordinate4").getDouble(0), corners.getJSONArray("coordinate4").getDouble(1));

                float bearing = overlay.getBearing();
                mapper = new Mapper(iCenter,oCenter, NW, bearing);

                boolean debugging = false;
                if(debugging) {
                    LatLng boundSW = overlay.getBounds().southwest;
                    LatLng rotatedBoundSW = new LatLng(mapper.rotateLatitudeAround(boundSW.latitude, bearing, oCenter), mapper.rotateLongitudeAround(boundSW.longitude, bearing, oCenter));
                    LatLng boundNE = overlay.getBounds().northeast;
                    LatLng rotatedBoundNE = new LatLng(mapper.rotateLatitudeAround(boundNE.latitude, bearing, oCenter), mapper.rotateLongitudeAround(boundNE.longitude, bearing, oCenter));

                    log.info("Overlay Width : {} - Height : {}", overlay.getWidth(), overlay.getHeight());

                    LatLng testCoord = new LatLng(37.871174, -122.258860);
                    activity.getMmap().addMarker(new MarkerOptions()
                            .position(testCoord)
                            .title("Test"));

                    LatLng testCoord2 = new LatLng(37.871528, -122.258858);
                    activity.getMmap().addMarker(new MarkerOptions()
                            .position(testCoord2)
                            .title("Tes2"));

                    log.info("Test Point estimated to {} on image", mapper.latLngToRotatedPoint(testCoord));
                    log.info("Test Point 2 estimated to {} on image", mapper.latLngToRotatedPoint(testCoord2));
                    log.info("Center Test {}", mapper.latLngToRotatedPoint(oCenter));


                    log.info("Bound SW on Map {}", boundSW);
                    log.info("Rotated Bound SW on Map {}", rotatedBoundSW);
                    log.info("Bound NE on Map {}", boundNE);
                    log.info("Rotated Bound NE on Map {}", rotatedBoundNE);
                    log.info("Rotated North East on Image: {}", mapper.latlngToPoint(rotatedBoundNE));
                    log.info("Rotated South West on Image: {}", mapper.latlngToPoint(rotatedBoundSW));

                    log.info("Corners {}", corners);
                    log.info("Bearing {}", bearing);

                    log.info("Bitmap Width : {} - Height : {}", bitmap.getWidth(), bitmap.getHeight()); // 950 × 800

                    log.info("Center on Map {}", oCenter);
                    log.info("North West on Map {}", NW);
                    log.info("North East on Map {}", NE);
                    log.info("South East on Map {}", SE);
                    log.info("South West on Map {}", SW);

                    log.info("Center on Image: {}", mapper.latlngToPoint(oCenter));
                    log.info("North West on Image: {}", mapper.latlngToPoint(NW));
                    log.info("North East on Image: {}", mapper.latlngToPoint(NE));
                    log.info("South East on Image: {}", mapper.latlngToPoint(SE));
                    log.info("South West on Image: {}", mapper.latlngToPoint(SW));


                    log.info("North West Back and Forth : {}", mapper.pointToLatLng(mapper.latlngToPoint(NW)));

                    log.info("Rotation back and forth : {}", mapper.pointToRotatedLatLng(mapper.latLngToRotatedPoint(boundSW)));

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Initialise Particle Set on user location on Image
            Point startPoint = mapper.latLngToRotatedPoint(loc);
            particleSet = new ParticleSet(mContext, 500, bitmap, startPoint.x, startPoint.y);
        }
    }

    //endregion

    //region Utilities

    private JSONObject getCorners() {
        JSONObject corners = null;
        try {
            corners = activity.getCurrentBuilding().getJSONObject("fourCoordinates");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return corners;
    }

    public void setGroundOverlay(GroundOverlay overlay) {
        this.overlay = overlay;
    }

    public void setMapsActivity(MapsActivity activity) {
        this.activity = activity;
    }

    //endregion

    //region Getters and Setters

    public MovementTracker getMovementTracker() {
        return movementTracker;
    }

    public HeadingTracker getHeadingTracker() {
        return headingTracker;
    }

    public LocationTracker getLocationTracker() {
        return locationTracker;
    }

    public void setParticleSet(ParticleSet particleSet) {
        this.particleSet = particleSet;
    }


    //endregion

    //region Listener Interface
    public void addOnLocationChangedListener(OnLocationChangedListener listener) {
        this.onLocationChangedListeners.add(listener);
    }

    private void updateListeners(Location location) {
        for (OnLocationChangedListener listener : onLocationChangedListeners) {
            listener.onLocationChanged(location);
        }
    }

    private List<OnLocationChangedListener> onLocationChangedListeners = new ArrayList<>();

    //endregion

}