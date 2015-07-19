package com.example.krisma.architek;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.krisma.architek.particlefilter.Move;
import com.example.krisma.architek.particlefilter.ParticleSet;
import com.example.krisma.architek.particlefilter.Pose;
import com.example.krisma.architek.trackers.AccelerometerTracker;
import com.example.krisma.architek.trackers.HeadingListener;
import com.example.krisma.architek.trackers.HeadingTracker;
import com.example.krisma.architek.trackers.LocationTracker;
import com.example.krisma.architek.trackers.MoveListener;
import com.example.krisma.architek.trackers.MovementTracker;
import com.example.krisma.architek.utils.Mapper;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smp on 14/07/15.
 */
public class DeadReckoning extends Service implements MoveListener, HeadingListener, LocationSource {

    private Context mContext;
    private Mapper mapper;
    private SensorManager sensorManager;

    // Trackers
    private LocationTracker locationTracker;
    private AccelerometerTracker accelerometerTracker;
    private MovementTracker movementTracker;
    private double heading;


    private ParticleSet particleSet;
    private HeadingTracker headingTracker;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;

        this.sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(transitionReceiver, new IntentFilter(LocationTracker.TRANSITION_ACTION));

        setupHeadingTracker();
        setupLocationTracker();
        setupMovementTracker();

        return Service.START_NOT_STICKY;
    }

    public DeadReckoning() {

    }


//    private LatLng getNorthWestCorner(){
//        try {
//            JSONArray coord1 = mapsActivity.getCurrentBuilding().getJSONObject("fourCoordinates").getJSONArray("coordinate1");
//
//            LatLng pos = new LatLng(coord1.getDouble(0), coord1.getDouble(1));
//            return pos;
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return new LatLng(Double.MIN_VALUE, Double.MIN_VALUE);
//
//    }

    private void setupLocationTracker() {
        locationTracker = new LocationTracker(mContext);
        locationTracker.addListener(headingTracker);
    }

    private void setupHeadingTracker() {
        headingTracker = new HeadingTracker(mContext);
        headingTracker.addListener(this);
    }

    private void setupAccelerometer() {
        accelerometerTracker = new AccelerometerTracker();
        accelerometerTracker.register(sensorManager);
    }

    private void setupMovementTracker() {
        movementTracker = new MovementTracker();
        movementTracker.register(sensorManager);
        movementTracker.addMoveListener(this);
    }

    public LocationTracker getLocationTracker() {
        return locationTracker;
    }

    @Override
    public void onMove(final Move m) {

        if(particleSet != null) {
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

            // Transform back to Google Map

            Pose averagePose = particleSet.getAveragePose();
            LatLng newPos = mapper.pointToLatLng(new Point((int) averagePose.getY(), (int) averagePose.getX()));

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

    private void retryTransition() {
        Log.w("Transition", "Location Was Null! Retrying in 2 seconds!");
        new Handler().postDelayed(transitionToIndoorRunnable, 2000);
    }

    BroadcastReceiver transitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transitionToIndoor();
        }
    };

    public void transitionToIndoor() {
        Location location = locationTracker.getCurrentLocation();

        if (location == null /*|| mapsActivity.getCurrentOverlayURL() == null*/) {
            retryTransition();
        } else {
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.wheeler11_edged, options);

                //bitmap = BitmapFactory.decodeStream(mapsActivity.getCurrentOverlayURL().openConnection().getInputStream());

            } catch (Exception e) {
                e.printStackTrace();
            }


            // Transformation to the Image

            Point iCenter = new Point(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
            Point iNW = new Point(0, 0);


            LatLng oCenter = new LatLng(43.693454, 7.236123); //LatLng oCenter = mapsActivity.getCurrentOverlayObject().getPosition(); TODO: debug


            LatLng oNW = new LatLng(43.693504, 7.235905); // LatLng oNW = getNorthWestCorner();  TODO: debug

            mapper = new Mapper(iCenter, iNW, oCenter, oNW);

            Point startPoint = mapper.latlngToPoint(loc);


            // Initialise Particle Set on user location on Image
            particleSet = new ParticleSet(mContext, 1000, bitmap, startPoint.x, startPoint.y);
        }
    }


    private Runnable transitionToIndoorRunnable = new Runnable() {
        @Override
        public void run() {
            transitionToIndoor();
        }
    };


    /**
     * 1. Retrive the Map Overlay
     * 2. Rotate user and corners Lat/Lng by Bearing
     * 3. Find the Lat/Lng position w.r.t. Center
     * 4. Find the Lat/Lng mapping to Px/Px by looking at the centers and dimensions
     * 5. Translate the Lat/Lng position w.r.t. center to the image frame with the above mapping
     *
     * @return
     */
//    private double[] mapToImageLocation(LatLng loc, int overlayWidth, int overlayHeight, LatLng NWcorner, LatLng NEcorner){
//        GroundOverlay overlay = mapsActivity.getCurrentOverlayObject();
//
//        float bearing = overlay.getBearing();
//        LatLng overlayCenter = overlay.getPosition();
//
//        // Rotate the Google Map location around the Overlay Center by the Bearing to get an upright image like the image we're loading later on
//        // TODO: Should we rotate with "-bearing"?
//        double rotatedLat = rotateLatitudeAround(loc.latitude, bearing,overlayCenter);
//        double rotatedLng = rotateLongitudeAround(loc.longitude, bearing, overlayCenter);
//
//        LatLng rotatedNWcorner = new LatLng(
//                rotateLatitudeAround(NWcorner.latitude, bearing, overlayCenter),
//                rotateLongitudeAround(NWcorner.longitude, bearing, overlayCenter)
//        );
//
//        LatLng rotatedNEcorner = new LatLng(
//                rotateLatitudeAround(NEcorner.latitude, bearing, overlayCenter),
//                rotateLongitudeAround(NEcorner.longitude, bearing, overlayCenter)
//        );
//
//        // Find mapping between Lat/Lng and Px/Px (x,y) on image
//        double ImageExtentLeft = overlayCenter.longitude - rotatedNWcorner.longitude;
//        double ImageExtentRight = rotatedNEcorner.longitude - overlayCenter.longitude;
//        double ImageExtentUp = rotatedNWcorner.latitude - overlayCenter.latitude;
//        double ImageExtentDown = overlayCenter.latitude - rotatedNWcorner.latitude;
//
//        double x = overlayWidth * ( rotatedLng - ImageExtentLeft ) / (ImageExtentRight - ImageExtentLeft);
//        double y = overlayHeight * ( 1 - ( rotatedLat - ImageExtentDown) / (ImageExtentUp - ImageExtentDown));
//
//        return new double[]{x,y};
//    }
//
//    private LatLng imageToMapLocation(GoogleMap map, int overlayWidth, int overlayHeight, LatLng NWcorner, LatLng NEcorner){
//
//        return new LatLng(0,0);
//    }
    public double rotateLatitudeAround(double lat, double angle, LatLng center) {
        double latitude = center.latitude + (Math.cos(Math.toRadians(angle)) * (lat - center.latitude) - Math.sin(Math.toRadians(angle)) * (lat - center.latitude));
        return latitude;
    }

    public double rotateLongitudeAround(double lon, double angle, LatLng center) {
        double longitude = center.longitude + (Math.sin(Math.toRadians(angle)) * (lon - center.longitude) + Math.cos(Math.toRadians(angle)) * (lon - center.longitude));
        return longitude;
    }

    public MovementTracker getMovementTracker() {
        return movementTracker;
    }

    public HeadingTracker getHeadingTracker() {
        return headingTracker;
    }

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

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public void setParticleSet(ParticleSet particleSet) {
        this.particleSet = particleSet;
    }

    public class LocalBinder extends Binder {
        DeadReckoning getService() {
            return DeadReckoning.this;
        }
    }
}
