package com.example.krisma.architek.asynctasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.example.krisma.architek.tools.OverlayHelper;
import com.example.krisma.architek.deadreckoning.utils.Helper;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class AsyncUpdateCoordinatesHash extends AsyncTask<String, Void, JSONArray> {
    private final OverlayHelper overlayHelper;

        private final LatLng target;
    private final AsyncTaskListener listener;

    public AsyncUpdateCoordinatesHash(OverlayHelper overlayHelper, LatLng target, AsyncTaskListener listener) {
            this.overlayHelper = overlayHelper;
            this.target = target;
            this.listener = listener;
        }

        @Override
        protected JSONArray doInBackground(String... params) {
            URL url;
            HttpURLConnection con;
            JSONArray buildings = null;
            final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(overlayHelper.getMapsActivity().getBaseContext());
            String token = getPrefs.getString("token", "");

            InputStream is;
            try {
                url = Server.createGetBuildingsNearbyURL(target, token);

                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.connect();
                int responseCode = con.getResponseCode();
                is = con.getInputStream();
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(Helper.convertStreamToString(is));
                } catch (JSONException f) {
                    f.printStackTrace();
                }

                buildings = jObject.getJSONArray("buildings");
                return buildings;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buildings;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            overlayHelper.setCoordinatesHash(result);
            overlayHelper.updateOverlays();
            listener.onTaskCompleted();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

}