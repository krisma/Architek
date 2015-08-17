package com.example.krisma.architek.storing.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class TripBuilder {
    private long startTime;
    private long endTime;
    private LatLng startPos;
    private LatLng endPos;
    private List<LatLng> positions;

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

    public TripBuilder setPositions(List<LatLng> positions) {
        this.positions = positions;
        return this;
    }

    public Trip createTrip() {
        return new Trip(startTime, endTime, startPos, endPos, positions);
    }
}