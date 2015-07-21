package com.example.krisma.architek.deadreckoning.utils;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by smp on 22/07/15.
 */
public class Helper {
    public static LatLng getLagLngFromLngLat(JSONArray coordinate) {
        try {
            return new LatLng(coordinate.getDouble(1), coordinate.getDouble(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ;
        return null;
    }

    public static  LatLngBounds getBoundsFromJSONObject(JSONObject twoCoordinates) {
        LatLng sw = null;
        LatLng ne = null;
        try {
            JSONArray coordinatesw = twoCoordinates.getJSONArray("coordinatesw");
            JSONArray coordinatene = twoCoordinates.getJSONArray("coordinatene");
            sw = new LatLng(coordinatesw.getDouble(0), coordinatesw.getDouble(1));
            ne = new LatLng(coordinatene.getDouble(0), coordinatene.getDouble(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new LatLngBounds(sw, ne);
    }

    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
