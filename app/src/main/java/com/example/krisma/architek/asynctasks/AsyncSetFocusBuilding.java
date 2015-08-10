package com.example.krisma.architek.asynctasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.deadreckoning.utils.Helper;
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
 * Created by smp on 07/08/15.
 */
public class AsyncSetFocusBuilding extends AsyncTask<LatLng, Void, Boolean> {

    private static final Logger log = LoggerFactory.getLogger(AsyncSetFocusBuilding.class);
    private final OverlayHelper overlayHelper;
    private final MapsActivity mapsActivity;
    private JSONObject jObject = null;
    private LatLng location;

    public AsyncSetFocusBuilding(MapsActivity mapsActivity){
        this.mapsActivity = mapsActivity;
        this.overlayHelper = mapsActivity.getOverlayHelper();
    }
    @Override
    protected Boolean doInBackground(LatLng... params) {
        location = params[0];

        log.debug("Called: setFocusBuilding");
        URL url;
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(mapsActivity.getBaseContext());
        String token = getPrefs.getString("token", "");
        InputStream is;
        HttpURLConnection con = null;
        String param = String.valueOf(location.latitude) + "," + String.valueOf(location.longitude);

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
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    @Override
    protected void onPostExecute(Boolean success){
        if(success) {
            try {
                overlayHelper.setCurrentBuildingMaps(jObject.getJSONArray("floors"));
                log.info(overlayHelper.getCurrentBuildingMaps().toString());

                overlayHelper.setCurrentFloorNumbers(overlayHelper.getCurrentBuildingMaps().length());
                overlayHelper.setCurrentBuildingLocation(location);

                overlayHelper.setCurrentFloor(0);
                mapsActivity.getFloorView().setText(0 + "");
                mapsActivity.getFloorView().invalidate();

                mapsActivity.showFloorButtons(true);

                overlayHelper.overlayCurrentFloor();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        log.debug("Execution failed. PostExecute aborted. (setFocusBuilding)");
    }
}
