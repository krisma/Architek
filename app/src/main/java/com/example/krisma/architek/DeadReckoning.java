package com.example.krisma.architek;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Region;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krisma.architek.particlefilter.Move;
import com.example.krisma.architek.particlefilter.ParticleSet;
import com.example.krisma.architek.particlefilter.Pose;
import com.example.krisma.architek.trackers.AccelerometerTracker;
import com.example.krisma.architek.trackers.HeadingListener;
import com.example.krisma.architek.trackers.HeadingTracker;
import com.example.krisma.architek.trackers.LocationTracker;
import com.example.krisma.architek.trackers.MoveListener;
import com.example.krisma.architek.trackers.MovementTracker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;


/**
 * Created by smp on 14/07/15.
 */
public class DeadReckoning implements MoveListener, HeadingListener {

    private final Context mContext;
    private final GoogleMap map;
    private final MapsActivity mapsActivity;
    private SensorManager sensorManager;

    // Trackers
    private LocationTracker locationTracker;
    private AccelerometerTracker accelerometerTracker;
    private MovementTracker movementTracker;
    private double heading;
    private ParticleSet particleSet;
    private HeadingTracker headingTracker;
    private Point userLocation;

    public DeadReckoning(Context context, MapsActivity mapsActivity, GoogleMap map){
        mContext = context;
        this.map = map;
        this.mapsActivity = mapsActivity;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(transitionReceiver, new IntentFilter(LocationTracker.TRANSITION_ACTION));

        setupLocationTracker();
        setupHeadingTracker();
        setupMovementTracker();

//        mapsActivity.findViewById(R.id.debugView).setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                Region region = new Region(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
//
//                if(region.contains((int)event.getX(), (int)event.getY())){
//                    transitionToIndoor(userLocation);
//                    userLocation = new Point((int)event.getX(), (int)event.getY());
//                }
//                return false;
//            }
//        });

    }



    private void setupLocationTracker(){
        locationTracker = new LocationTracker(mContext);
    }

    private void setupHeadingTracker(){

        headingTracker = new HeadingTracker(mContext);
        headingTracker.addListener(this);
    }

    private void setupAccelerometer(){
        accelerometerTracker = new AccelerometerTracker();
        accelerometerTracker.register(sensorManager);
    }

    private void setupMovementTracker(){
        movementTracker = new MovementTracker(DeadReckoning.this);
        movementTracker.register(sensorManager);
        movementTracker.addMoveListener(this);
    }

    public LocationTracker getLocationTracker() {
        return locationTracker;
    }

    @Override
    public void onMove(Move move) {
        if(mapsActivity.getOiv().getParticleSet() != null) {
            Move tmp = move;
            tmp.setHeading(heading);
            mapsActivity.getOiv().getParticleSet().updateParticles(tmp);
            mapsActivity.getOiv().displayParticles();

            Log.d("Move", "Moved " + tmp.getDistanceTraveled() + " in heading: " + tmp.getHeading());
        }
    }

    @Override
    public void onHeadingChange(double heading) {
        this.heading = heading;
    }

    private void retryTransition(){
        Log.w("Transition", "Location Was Null! Retrying in 2 seconds!");
        new Handler().postDelayed(transitionToIndoorRunnable, 2000);
    }

    BroadcastReceiver transitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //transitionToIndoor();
        }
    };

    public void transitionToIndoor(Point user) {
//
//        Location loc = locationTracker.getCurrentLocation();
//
//        if(loc == null || mapsActivity.getCurrentOverlayURL() == null){
//            retryTransition();
//        } else {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try {
                Bitmap bitmap = BitmapFactory.decodeStream(mapsActivity.getCurrentOverlayURL().openConnection().getInputStream());

                try {
                    JSONArray coord1 = mapsActivity.getCurrentBuilding().getJSONObject("fourCoordinates").getJSONArray("coordinate1");
                    JSONArray coord2 = mapsActivity.getCurrentBuilding().getJSONObject("fourCoordinates").getJSONArray("coordinate2");

                    //double[] xyDimXDimY = mapToImageLocation(debug, bitmap.getWidth(), bitmap.getHeight(), new LatLng(coord1.getDouble(0), coord1.getDouble(1)), new LatLng(coord2.getDouble(0), coord2.getDouble(1))); // TODO: NEEDS FIX!!


                    this.particleSet = new ParticleSet(mContext, 100, bitmap, user.x, user.y);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
       // }
    }


    private Runnable transitionToIndoorRunnable = new Runnable() {
        @Override
        public void run() {
           //transitionToIndoor();
        }
    };

    /***
     *  1. Retrive the Map Overlay
     *  2. Rotate user and corners Lat/Lng by Bearing
     *  3. Find the Lat/Lng position w.r.t. Center
     *  4. Find the Lat/Lng mapping to Px/Px by looking at the centers and dimensions
     *  5. Translate the Lat/Lng position w.r.t. center to the image frame with the above mapping
     * @param NWcorner Northwestern Corner of the Overlay
     * @param NEcorner Northeastern Corner of the Overlay
     * @return
     */
    private double[] mapToImageLocation(LatLng loc, int overlayWidth, int overlayHeight, LatLng NWcorner, LatLng NEcorner){
        GroundOverlay overlay = mapsActivity.getCurrentOverlayObject();

        float bearing = overlay.getBearing();
        LatLng overlayCenter = overlay.getPosition();

        // Rotate the Google Map location around the Overlay Center by the Bearing to get an upright image like the image we're loading later on
        // TODO: Should we rotate with "-bearing"?
        double rotatedLat = rotateLatitudeAround(loc.latitude, bearing,overlayCenter);
        double rotatedLng = rotateLongitudeAround(loc.longitude, bearing, overlayCenter);

        LatLng rotatedNWcorner = new LatLng(
                rotateLatitudeAround(NWcorner.latitude, bearing, overlayCenter),
                rotateLongitudeAround(NWcorner.longitude, bearing, overlayCenter)
        );

        LatLng rotatedNEcorner = new LatLng(
                rotateLatitudeAround(NEcorner.latitude, bearing, overlayCenter),
                rotateLongitudeAround(NEcorner.longitude, bearing, overlayCenter)
        );

        // Find mapping between Lat/Lng and Px/Px (x,y) on image
        double ImageExtentLeft = overlayCenter.longitude - rotatedNWcorner.longitude;
        double ImageExtentRight = rotatedNEcorner.longitude - overlayCenter.longitude;
        double ImageExtentUp = rotatedNWcorner.latitude - overlayCenter.latitude;
        double ImageExtentDown = overlayCenter.latitude - rotatedNWcorner.latitude;

        double x = overlayWidth * ( rotatedLng - ImageExtentLeft ) / (ImageExtentRight - ImageExtentLeft);
        double y = overlayHeight * ( 1 - ( rotatedLat - ImageExtentDown) / (ImageExtentUp - ImageExtentDown));

        return new double[]{x,y};
    }

    private LatLng imageToMapLocation(GoogleMap map, int overlayWidth, int overlayHeight, LatLng NWcorner, LatLng NEcorner){

        return new LatLng(0,0);
    }

    public double rotateLatitudeAround(double lat, double angle, LatLng center) {
        double latitude = center.latitude + (Math.cos(Math.toRadians(angle)) * (lat - center.latitude) - Math.sin(Math.toRadians(angle)) * (lat - center.latitude));
        return latitude;
    }

    public double rotateLongitudeAround(double lon, double angle, LatLng center) {
        double longitude = center.longitude + (Math.sin(Math.toRadians(angle)) * (lon - center.longitude) + Math.cos(Math.toRadians(angle)) * (lon - center.longitude));
        return longitude;
    }
}
