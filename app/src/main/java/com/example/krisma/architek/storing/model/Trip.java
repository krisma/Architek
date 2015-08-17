package com.example.krisma.architek.storing.model;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stefan on 17/08/15.
 */
public class Trip {

    private long startTime;
    private long endTime;
    private LatLng startPos;
    private LatLng endPos;
    private List<LatLng> positions = new ArrayList<>();

    public Trip(long startTime, long endTime, LatLng startPos, LatLng endPos) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public LatLng getStartPos() {
        return startPos;
    }

    public void setStartPos(LatLng startPos) {
        this.startPos = startPos;
    }

    public LatLng getEndPos() {
        return endPos;
    }

    public void setEndPos(LatLng endPos) {
        this.endPos = endPos;
    }

    public List<LatLng> getPositions() {
        return positions;
    }

    public void addPosition(LatLng latLng){
        this.positions.add(latLng);
        if(positions.size() % 100 == 0){
            Log.i("TRIP", positions.size() + " positions added.");
        }
    }

    public class TripBuilder {
        private long startTime;
        private long endTime;
        private LatLng startPos;
        private LatLng endPos;

        public TripBuilder setStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public TripBuilder setEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public TripBuilder setStartPos(LatLng startPos) {
            this.startPos = startPos;
            return this;
        }

        public TripBuilder setEndPos(LatLng endPos) {
            this.endPos = endPos;
            return this;
        }

        public Trip createTrip() {
            return new Trip(startTime, endTime, startPos, endPos);
        }
    }
}