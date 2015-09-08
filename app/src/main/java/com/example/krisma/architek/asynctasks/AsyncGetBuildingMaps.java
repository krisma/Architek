package com.example.krisma.architek.asynctasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.deadreckoning.utils.Helper;
import com.example.krisma.architek.storing.model.Building;
import com.example.krisma.architek.tools.OverlayHelper;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by smp on 02/09/15.
 */
public class AsyncGetBuildingMaps extends AsyncTask<Building, Void, JSONObject> {

    private static final Logger log = LoggerFactory.getLogger(AsyncGetBuildingMaps.class);
    private final Context context;
    private JSONObject jObject = null;
    private Building building;

    public AsyncGetBuildingMaps(Context context){
        this.context = context;
    }

    @Override
    protected JSONObject doInBackground(Building... params) {
        this.building = params[0];

        log.debug("Called: setFocusBuilding");
        URL url;
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String token = getPrefs.getString("token", "");
        InputStream is;
        HttpURLConnection con = null;
        String param = String.valueOf(building.center.latitude) + "," + String.valueOf(building.center.longitude);

        try {
            url = new URL("https://architek-server.herokuapp.com/getbuildingmaps?" +
                    "location=" + param);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.connect();

            int responseCode = con.getResponseCode();
            is = con.getInputStream();

            jObject = new JSONObject(Helper.convertStreamToString(is));

            log.debug(jObject.toString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(con != null) {
                    con.disconnect();
                }
                return jObject;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject jObj) {
        building.setFloors(jObj);
    }
}
