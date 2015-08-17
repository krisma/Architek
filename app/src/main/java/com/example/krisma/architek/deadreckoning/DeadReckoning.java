package com.example.krisma.architek.deadreckoning;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.krisma.architek.DebugActivity;
import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.deadreckoning.particlefilter.Move;
import com.example.krisma.architek.deadreckoning.particlefilter.ParticleSet;
import com.example.krisma.architek.deadreckoning.particlefilter.Pose;
import com.example.krisma.architek.deadreckoning.trackers.HeadingTracker;
import com.example.krisma.architek.deadreckoning.trackers.LocationTracker;
import com.example.krisma.architek.deadreckoning.trackers.MovementTracker;
import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.deadreckoning.trackers.listeners.MoveListener;
import com.example.krisma.architek.deadreckoning.utils.Mapper;
import com.example.krisma.architek.storing.model.Trip;
import com.example.krisma.architek.tools.OverlayHelper;
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

    private static final Logger log = LoggerFactory.getLogger(DeadReckoning.class);
    boolean indoor = false;
    boolean DEBUGGING = false;

    //region Fields and Constants
    private Context mContext;
    private Mapper mapper;

    // Trackers
    private LocationTracker locationTracker;
    private MovementTracker movementTracker;
    private double heading;
    private ParticleSet particleSet;
    private HeadingTracker headingTracker;
    private GroundOverlay overlay;
    private MapsActivity mapsActivity;
    private Location drLocation;

    //endregion
    private DebugActivity debugActivity;
    private Trip trip;
    private Runnable transitionToIndoorRunnable = new Runnable() {
        @Override
        public void run() {
            transitionToIndoor();
        }
    };
    private List<OnLocationChangedListener> onLocationChangedListeners = new ArrayList<>();

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

    public void setPosition(LatLng position) {
        Point point = mapper.latLngToRotatedPoint(position);
        particleSet = new ParticleSet(mContext, 1000, mapsActivity.getOverlayHelper().getCurrentOverlayBitmap(), point.x, point.y);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;

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

        if (particleSet == null && DEBUGGING) {
            particleSet = debugActivity.getOIV().getParticleSet();
        }

        if (particleSet != null) {
            float d = m.getDistanceTraveled();
            double h = heading;

            Log.d("Move", "Moved " + d + " in heading: " + h);

            int tickDistance = 2;

            while (d - tickDistance > tickDistance) {
                Move tmp = new Move(d, h);
                tmp.setDistance(tickDistance);
                particleSet.updateParticles(tmp);
                d = d - tickDistance;
            }

            Move tmp = new Move(d, h);
            tmp.setDistance(d);
            particleSet.updateParticles(tmp);

            if (DEBUGGING) {
                debugActivity.getOIV().displayParticles();
            } else {
                // Transform back to Google Map -- Show location on Map by updating onLocationChangedListeners --> MapsActivity included
                Pose averagePose = particleSet.getPose();
                LatLng newPos = mapper.pointToLatLng(new Point((int) averagePose.getX(), (int) averagePose.getY()));

                Location loc = new Location("DEAD_RECKONING");
                loc.setLatitude(newPos.latitude);
                loc.setLongitude(newPos.longitude);

                updateListeners(loc);
                trip.addPosition(newPos);

                //showParticlesOnMap();
            }
        }
    }

    public void showParticlesOnMap() {
        if (particleSet != null && particleSet.getParticles().length > 0) {
            List<LatLng> locs = new ArrayList<>();

            for (int i = 0; i < particleSet.getParticles().length; i++) {
                Pose p = particleSet.getParticles()[i].getPose();
                locs.add(mapper.pointToLatLng(new Point((int) p.getX(), (int) p.getY())));
            }
            mapsActivity.overlayHelper.showParticles(locs, mapsActivity);
        } else {
            log.warn("showParticlesOnMap error! null or empty particle set.");
        }
    }

    @Override
    public void onHeadingChanged(float heading) {
        this.heading = heading;
    }

    private void retryTransition() {
        Log.w("Transition", "Location Was Null! Retrying in 4 seconds!");
        new Handler().postDelayed(transitionToIndoorRunnable, 4000);
    }

    public void transitionToIndoor() {
        OverlayHelper helper = mapsActivity.getOverlayHelper();

        trip = new Trip.Builder()
                .setStartPos(locationTracker.getCurrentLatLng())
                .setStartTime(System.currentTimeMillis())
                .setBuildingPos(helper.getCurrentBuildingLocation())
                .createTrip();

        if (!DEBUGGING) {
            Location location;

            if (indoor && drLocation != null) {
                // already indoor, rely on dead reckoning pos
                location = drLocation;
                log.info("Using DR location for transition");
            } else {
                location = locationTracker.getCurrentLocation();
                log.info("Using Fused (GPS) location for transition");

            }

            indoor = true;
            Log.d("Transition", "Started");

            if (location == null || helper.getCurrentOverlayURL() == null || overlay == null) {
                retryTransition();
                Log.d("Transition", "Null");

            } else {
                Log.d("Transition", "In progress");

                Bitmap bitmap;
                try {
                    bitmap = helper.getCurrentOverlayBitmap();

                    mapper = new Mapper(                                                // Mapper responsible of coordinate transformations, needs 2 common points
                            new Point(bitmap.getWidth() / 2, bitmap.getHeight() / 2),   // Image Center
                            overlay.getPosition(),                                      // Overlay Center
                            getCorners(),                                               // Overlay Corners
                            overlay.getBearing());                                      // Overlay rotation/bearing

                    // Initialise Particle Set on user location on Image
                    LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                    Point startPoint = mapper.latlngToPoint(loc);

                    log.info("Start location is {} on image.", startPoint);
                    particleSet = new ParticleSet(mContext, 1000, bitmap, startPoint.x, startPoint.y);

                    //region Old Debugging
/*                    try {
                        LatLng C1 = new LatLng(corners.getJSONArray("coordinate1").getDouble(0), corners.getJSONArray("coordinate1").getDouble(1));
                        LatLng C2 = new LatLng(corners.getJSONArray("coordinate2").getDouble(0), corners.getJSONArray("coordinate2").getDouble(1));
                        LatLng C3 = new LatLng(corners.getJSONArray("coordinate3").getDouble(0), corners.getJSONArray("coordinate3").getDouble(1));
                        LatLng C4 = new LatLng(corners.getJSONArray("coordinate4").getDouble(0), corners.getJSONArray("coordinate4").getDouble(1));

                        log.info("Top Left (C1): {}, Top Right (C2): {}, Bottom Right (C3): {}, Bottom Left (C4): {}", C1, C2, C3, C4);


                        mapper = new Mapper(iCenter, oCenter, corners, bearing);

                        if (DEBUGGING) {
                            LatLng boundSW = overlay.getBounds().southwest;
                            LatLng rotatedBoundSW = new LatLng(mapper.rotateLatitudeAround(boundSW.latitude, bearing, oCenter), mapper.rotateLongitudeAround(boundSW.longitude, bearing, oCenter));
                            LatLng boundNE = overlay.getBounds().northeast;
                            LatLng rotatedBoundNE = new LatLng(mapper.rotateLatitudeAround(boundNE.latitude, bearing, oCenter), mapper.rotateLongitudeAround(boundNE.longitude, bearing, oCenter));

                            log.info("Overlay Width : {} - Height : {}", overlay.getWidth(), overlay.getHeight());

                            LatLng testCoord = new LatLng(37.871174, -122.258860);
                            mapsActivity.getMap().addMarker(new MarkerOptions()
                                    .position(testCoord)
                                    .title("Test"));

                            LatLng testCoord2 = new LatLng(37.871528, -122.258858);
                            mapsActivity.getMap().addMarker(new MarkerOptions()
                                    .position(testCoord2)
                                    .title("Tes2"));

                            //region Logging
                            log.info("Test Point estimated to {} on image", mapper.latLngToRotatedPoint(testCoord));
                            log.info("Test Point 2 estimated to {} on image", mapper.latLngToRotatedPoint(testCoord2));
                            log.info("Center Test {}", mapper.latLngToRotatedPoint(oCenter));


                            log.info("Bound C4 on Map {}", boundSW);
                            log.info("Rotated Bound C4 on Map {}", rotatedBoundSW);
                            log.info("Bound C2 on Map {}", boundNE);
                            log.info("Rotated Bound C2 on Map {}", rotatedBoundNE);
                            log.info("Rotated North East on Image: {}", mapper.latlngToPoint(rotatedBoundNE));
                            log.info("Rotated South West on Image: {}", mapper.latlngToPoint(rotatedBoundSW));

                            log.info("Corners {}", corners);
                            log.info("Bearing {}", bearing);

                            log.info("Bitmap Width : {} - Height : {}", bitmap.getWidth(), bitmap.getHeight()); // 950 × 800

                            log.info("Center on Map {}", oCenter);
                            log.info("North West on Map {}", C1);
                            log.info("North East on Map {}", C2);
                            log.info("South East on Map {}", C3);
                            log.info("South West on Map {}", C4);

                            log.info("Center on Image: {}", mapper.latlngToPoint(oCenter));
                            log.info("North West on Image: {}", mapper.latlngToPoint(C1));
                            log.info("North East on Image: {}", mapper.latlngToPoint(C2));
                            log.info("South East on Image: {}", mapper.latlngToPoint(C3));
                            log.info("South West on Image: {}", mapper.latlngToPoint(C4));

                            log.info("North West Back and Forth : {}", mapper.pointToLatLng(mapper.latlngToPoint(C1)));
                            log.info("Rotation back and forth : {}", mapper.pointToRotatedLatLng(mapper.latLngToRotatedPoint(boundSW)));
                            //endregion
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        log.warn("something died");
                    }*/
                    //endregion

                } catch (JSONException e) {
                    e.printStackTrace();
                    log.error("Failed to initialize mapper");
                }
            }
        }
    }

    //endregion

    //region Utilities

    private JSONObject getCorners() {
        JSONObject corners = null;
        try {
            corners = mapsActivity.getOverlayHelper().getCurrentBuilding().getJSONObject("fourCoordinates");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return corners;
    }

    public void setGroundOverlay(GroundOverlay overlay) {
        this.overlay = overlay;
    }

    public void setActivity(Activity activity) {
        if (activity instanceof MapsActivity) {
            this.mapsActivity = (MapsActivity) activity;
        } else if (activity instanceof DebugActivity) {
            this.debugActivity = (DebugActivity) activity;
        }
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
        this.drLocation = location;
        for (OnLocationChangedListener listener : onLocationChangedListeners) {
            listener.onLocationChanged(location);
        }
    }

    public class LocalBinder extends Binder {
        public DeadReckoning getService() {
            return DeadReckoning.this;
        }
    }

    //endregion

}