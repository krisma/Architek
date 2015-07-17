package com.example.krisma.architek.trackers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.example.krisma.architek.DeadReckoning;
import com.example.krisma.architek.particlefilter.Move;

import java.util.ArrayList;

/**
 * Created by smp on 14/07/15.
 */
public class MovementTracker implements SensorEventListener {

    private final DeadReckoning deadReckoning;
    private ArrayList<MoveListener> listeners = new ArrayList<>();

    public MovementTracker(DeadReckoning deadReckoning){
        this.deadReckoning = deadReckoning;
    }

    public boolean register(SensorManager sensorManager) {
        return sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_FASTEST, new Handler());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = getStepLength(184) / 10;
        updateListeners(distance);
    }

    private void updateListeners(float distance) {
        Move move = new Move(distance, 0);
        for(MoveListener listener : listeners){
            listener.onMove(move);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float getStepLength(float userHeight){
        return 0.45f * userHeight;
    }

    public void addMoveListener(MoveListener listener){

        this.listeners.add(listener);
    }
}
