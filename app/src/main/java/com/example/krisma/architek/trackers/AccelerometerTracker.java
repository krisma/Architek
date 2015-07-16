package com.example.krisma.architek.trackers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.example.krisma.architek.utils.EvictingQueue;

import adr.structures.Vector3D;

public class AccelerometerTracker implements SensorEventListener {
    private Vector3D accelerationVector;
    private EvictingQueue<Vector3D> data = new EvictingQueue<>(512);
    private float lowPassFilterValue = 0.6f;

    public AccelerometerTracker() {
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        float xValue = sensorEvent.values[0];
        float yValue = sensorEvent.values[1];
        float zValue = sensorEvent.values[2];

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

        System.out.println("X: " + xValue + " Y: " + yValue + " Z: " + zValue);

        float[] R = new float[9];
        float[] I = new float[9];
        float[] values = new float[3];

        // get azimuth
        SensorManager.getOrientation(R, values);

        float x = R[0] * sensorEvent.values[0] + R[1] * sensorEvent.values[1] + R[2] * sensorEvent.values[2];
        float y = R[3] * sensorEvent.values[0] + R[4] * sensorEvent.values[1] + R[5] * sensorEvent.values[2];
        float z = R[6] * sensorEvent.values[0] + R[7] * sensorEvent.values[1] + R[8] * sensorEvent.values[2];
        accelerationVector = new Vector3D(x,y,z);

        data.add(accelerationVector);

        AccelerometerTracker.accel = sensorEvent.values.clone();
    }

    public boolean register(SensorManager sensorManager) {
        return sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST,
                new Handler());
    }

    public void unregister(SensorManager sensorManager) {
        sensorManager.unregisterListener(this);
    }

    public void onStop() {
    }

    public static float[] accel = null;

    public Vector3D getAccelerationVector() {
        return accelerationVector;
    }

    public float getAverageMagnitude() {
        float sum = 0;
        for (int i = 0; i < data.size(); ++i) {
            Vector3D event = i == 0 ? data.get(0) : data.get(i - 1);
            sum += event.getMagnitude();
        }

        return sum / (float) data.size();
    }
}