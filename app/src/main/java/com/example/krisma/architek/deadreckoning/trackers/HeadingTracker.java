package com.example.krisma.architek.deadreckoning.trackers;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import com.example.krisma.architek.deadreckoning.trackers.listeners.HeadingListener;
import com.example.krisma.architek.deadreckoning.utils.EvictingQueue;
import com.google.android.gms.maps.LocationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smp on 14/07/15.
 */
public class HeadingTracker implements SensorEventListener, LocationSource.OnLocationChangedListener {

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    float[] mGravity;
    float[] mGeomagnetic;
    private float azimut;
    private ArrayList<HeadingListener> listeners = new ArrayList<>();
    private GeomagneticField geoField;


    public HeadingTracker(Context context){

        // Fetch Sensor Manager
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> supportedSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // Fetch Sensors

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if(supportedSensors.contains(accelerometer)){
            Log.d("Sensors", "Accelerometer Supported!");
        } else {
            Log.d("Sensors", "Accelerometer NOT Supported!");
        }

        if(supportedSensors.contains(magnetometer)){
            Log.d("Sensors", "Magnetometer Supported!");
        } else {
            Log.d("Sensors", "Magnetometer NOT Supported!");
        }

        // Register Sensors
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }


    EvictingQueue<Float> events = new EvictingQueue<>(5);
    Long updateTimeBoundary = System.currentTimeMillis();
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null && geoField != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimuthInRadians = orientation[0];
                float azimuthInDegress = (float)Math.toDegrees(azimuthInRadians);

                float heading = azimuthInDegress + geoField.getDeclination() ;

                events.add(heading);

                // Round down to even 10's
                float average = (getAverage() + 175) % 360;

                float rest = average % 10;
                if(rest > 5){
                    average = average + (10 - rest);
                } else {
                    average = average - rest;
                }

                if(lastUpdate == -1337) lastUpdate = average;

                if(System.currentTimeMillis() > updateTimeBoundary + 500){
                    updateListeners(average);
                    lastUpdate = average;
                    updateTimeBoundary = System.currentTimeMillis();
                    Log.d("Heading", "H: " + average);

                }

            }
        }
    }

    float lastUpdate = -1337;

    private float getAverage(){
        float result = 0f;

        for(Float f : events){
            result += f;
        }
        result = result / events.size();

        return result;
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void addListener(HeadingListener listener){
        this.listeners.add(listener);
    }

    private void updateListeners(float heading){
        for(HeadingListener listener : listeners){
            listener.onHeadingChange(heading);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        geoField = new GeomagneticField(
                Double.valueOf(location.getLatitude()).floatValue(),
                Double.valueOf(location.getLongitude()).floatValue(),
                Double.valueOf(location.getAltitude()).floatValue(),
                System.currentTimeMillis()
        );
    }
}
