package com.example.krisma.architek.storing.model;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stefan on 17/08/15.
 */
public class Trip {

    public LatLng getBuildingPosition() {
        return buildingPosition;
    }

    private LatLng buildingPosition;
    private long startTime;
    private long endTime;
    private LatLng startPos;
    private LatLng endPos;
    private List<LatLng> positions = new ArrayList<>();

    public Trip(LatLng buildingPosition, long startTime, long endTime, LatLng startPos, LatLng endPos) {
        this.buildingPosition = buildingPosition;
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

    public TileOverlay addHeatmapToMap(GoogleMap map) {

        // Prepare Gradient
        int[] colors = {
                Color.rgb(102, 225, 0), // green
                Color.rgb(255, 0, 0)    // red
        };

        float[] startPoints = {
                0.2f, 1f
        };

        Gradient gradient = new Gradient(colors, startPoints);

        // Create a heat map tile provider, passing it the latlngs of the police stations.
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(positions)
                .gradient(gradient)
                .build();

        // Add a tile overlay to the map, using the heat map tile provider.
        TileOverlay overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        return overlay;
    }

    public void addPosition(LatLng latLng){
        this.positions.add(latLng);
        if(positions.size() % 100 == 0){
            Log.i("TRIP", positions.size() + " positions added.");
        }
    }

    public void printLatLngs(){
        String res = "";
        for(LatLng l : positions){
            res += l + "\n";
        }
        Log.i("TRIP", res);
    }

    public static class Builder {
        private LatLng buildingPosition;
        private long startTime;
        private long endTime;
        private LatLng startPos;
        private LatLng endPos;

        public Builder setStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder setEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder setStartPos(LatLng startPos) {
            this.startPos = startPos;
            return this;
        }

        public Builder setEndPos(LatLng endPos) {
            this.endPos = endPos;
            return this;
        }

        public Builder setBuildingPos(LatLng buildingPosition) {
            this.buildingPosition = buildingPosition;
            return this;
        }

        public Trip createTrip() {
            return new Trip(buildingPosition, startTime, endTime, startPos, endPos);
        }
    }
}