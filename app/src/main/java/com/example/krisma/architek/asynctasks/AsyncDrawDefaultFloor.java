package com.example.krisma.architek.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.example.krisma.architek.storing.model.Building;
import com.example.krisma.architek.tools.OverlayHelper;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class AsyncDrawDefaultFloor extends AsyncTask<String, Void, Bitmap> {
    private static final Logger log = LoggerFactory.getLogger(AsyncDrawDefaultFloor.class);

    private final OverlayHelper overlayHelper;

        private Building building = null;

        public AsyncDrawDefaultFloor(OverlayHelper overlayHelper, Building building) {
            this.overlayHelper = overlayHelper;
            this.building = building;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            URL url = null;
            Bitmap bmp = null;
            try {
                url = new URL(building.defaultFloor);
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                overlayHelper.getFloorplans().put(url, bmp);
                overlayHelper.setCurrentOverlayURL(url);
                log.debug(url.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bmp;

        }

        @Override
        protected void onPostExecute(Bitmap result) {
                overlayHelper.setCurrentOverlayBitmap(result);
                LatLng anchor = building.center;
                float height = (float) building.height;
                float width = (float) building.width;
                float bearing = (float) building.bearing;

                log.info("From Server || H: {}, W: {}, B: {}", height, width, bearing);

                GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(result))
                        .position(anchor, width, height);

                overlayHelper.setGroundOverlay(overlayHelper.getMapsActivity().getMap().addGroundOverlay(overlayOptions));
                overlayHelper.getGroundOverlay().setBearing(bearing);

                overlayHelper.getMapsActivity().getDeadReckoning().setGroundOverlay(overlayHelper.getGroundOverlay());
                overlayHelper.setCurrentBuilding(building);

                for (GroundOverlay g : overlayHelper.getOverlaysHash().values()) {
                    g.remove();
                }

                GroundOverlay temp = overlayHelper.getMapsActivity().getMap().addGroundOverlay(overlayOptions);
                overlayHelper.getOverlaysHash().put(anchor, temp);
                temp.setBearing(bearing);

                overlayHelper.transitionToIndoor();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

}