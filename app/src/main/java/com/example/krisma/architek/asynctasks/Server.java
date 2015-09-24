package com.example.krisma.architek.asynctasks;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by smp on 24/09/15.
 */
public class Server {
    public static final String URL_ROOT = "http://104.236.165.114/";

    public static URL createGetBuildingsURL(LatLng loc) throws MalformedURLException {
        String location = String.valueOf(loc.latitude) + "," + String.valueOf(loc.longitude);
        return new URL(URL_ROOT + "getbuildingmaps?" +
                "location=" + location);
    }

    public static URL createGetBuildingsNearbyURL(LatLng loc, String token) throws MalformedURLException {
        String location = String.valueOf(loc.latitude) + "," + String.valueOf(loc.longitude);
        return new URL("https://architek-server.herokuapp.com/getbuildingsnearby?" +
                "coordinate=" + location + "&" + "token=" + token);
    }

}
