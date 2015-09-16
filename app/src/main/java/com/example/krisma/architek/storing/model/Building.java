package com.example.krisma.architek.storing.model;

import android.content.Context;

import com.example.krisma.architek.asynctasks.AsyncGetBuildingMaps;
import com.example.krisma.architek.asynctasks.AsyncSetFocusBuilding;
import com.example.krisma.architek.deadreckoning.utils.Mapper;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by smp on 02/09/15.
 */
public class Building {
    public String name;
    public String id;

    public JSONArray floors;
    private int floorCount;
    public String defaultFloor;

    public final LatLng C1;
    public final LatLng C2;
    public final LatLng C3;
    public final LatLng C4;
    public LatLng center;
    public final double bearing;

    public final double width;
    public final double height;

    public Building(Context context, JSONObject building) throws JSONException {
        id = building.getString("_id");

        JSONObject corners = building.getJSONObject("fourCoordinates");

        C1 = new LatLng(corners.getJSONArray("coordinate1").getDouble(0), corners.getJSONArray("coordinate1").getDouble(1));
        C2 = new LatLng(corners.getJSONArray("coordinate2").getDouble(0), corners.getJSONArray("coordinate2").getDouble(1));
        C3 = new LatLng(corners.getJSONArray("coordinate3").getDouble(0), corners.getJSONArray("coordinate3").getDouble(1));
        C4 = new LatLng(corners.getJSONArray("coordinate4").getDouble(0), corners.getJSONArray("coordinate4").getDouble(1));

        bearing = Mapper.headingFromTo(C3, C2);

        try {
            center = new LatLngBounds(C4, C2).getCenter();
        } catch (IllegalArgumentException e){
            center = new LatLngBounds(C2, C4).getCenter();
        }

        defaultFloor = building.getString("defaultfloor");
        floors = building.getJSONArray("floors");
        floorCount = floors.length();

        width = building.getDouble("width");    // TODO: needs to be automated/calculated
        height = building.getDouble("height");  // TODO: needs to be automated/calculated

        new AsyncGetBuildingMaps(context).execute(this); // calls setFloors(...)
    }

    public void setFloors(JSONObject obj){

    }
}
