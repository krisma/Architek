package com.example.krisma.architek.trackers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.example.krisma.architek.particlefilter.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smp on 14/07/15.
 */
public class MovementTracker implements SensorEventListener {

    private boolean useAccelerometer;
    private ArrayList<MoveListener> listeners = new ArrayList<>();
    private Sensor sensor;
    private float lowPassFilterValue = 0.6f;

    private float   mLimit = 10;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;

    public MovementTracker(Context context) {
        SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> supportedSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (supportedSensors.contains(sensor)) {
            Log.d("Movement", "Stepdetector Supported!");
        } else {
            Log.d("Movement", "Stepdetector NOT Supported! Using accelerometer instead.");
            useAccelerometer = true;
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            int h = 480; // TODO: remove this constant
            mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
            mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        }

        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, new Handler());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!useAccelerometer) {
            float distance = getStepLength(184) / 2;
            updateListeners(distance);
        } else {
            float xValue = event.values[0];
            float yValue = event.values[1];
            float zValue = event.values[2];

            // Low Pass Filter
            if(xValue < lowPassFilterValue){
                xValue = 0;
            }
            if(yValue < lowPassFilterValue){
                yValue = 0;
            }
            if(yValue < lowPassFilterValue){
                yValue = 0;
            }


            float vSum = xValue + yValue + zValue;

            int k = 0;
            float v = vSum / 3;

            float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
            if (direction == - mLastDirections[k]) {
                // Direction changed
                int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                mLastExtremes[extType][k] = mLastValues[k];
                float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                if (diff > mLimit) {

                    boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                    boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                    boolean isNotContra = (mLastMatch != 1 - extType);

                    if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                        Log.i("Movement", "step");
                        updateListeners(getStepLength(184));
                        mLastMatch = extType;
                    }
                    else {
                        mLastMatch = -1;
                    }
                }
                mLastDiff[k] = diff;
            }
            mLastDirections[k] = direction;
            mLastValues[k] = v;

        }
    }

    private void updateListeners(float distance) {
        Move move = new Move(distance, 0);
        for (MoveListener listener : listeners) {
            listener.onMove(move);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float getStepLength(float userHeight) {
        return 0.45f * userHeight;
    }

    public void addMoveListener(MoveListener listener) {

        this.listeners.add(listener);
    }
}
