package com.example.krisma.architek.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

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

        private JSONObject thisBuilding = null;

        public AsyncDrawDefaultFloor(OverlayHelper overlayHelper, JSONObject thisBuilding) {
            this.overlayHelper = overlayHelper;
            this.thisBuilding = thisBuilding;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            URL url = null;
            Bitmap bmp = null;
            try {
                url = new URL(thisBuilding.getString("defaultfloor"));
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                //bmp = BitmapFactory.decodeResource(getResources(), R.drawable.wheeler11_edged_test_coords);
                overlayHelper.getFloorplans().put(url, bmp);
                overlayHelper.setCurrentOverlayURL(url);
                log.debug(url.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bmp;

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            try {

                overlayHelper.setCurrentOverlayBitmap(result);
                LatLng anchor = new LatLng((Double) thisBuilding.getJSONArray("location").get(1),
                        (Double) thisBuilding.getJSONArray("location").get(0));
                Float height = new Float(thisBuilding.getDouble("height"));
                Float width = new Float(thisBuilding.getDouble("width"));
                Float bearing = new Float(thisBuilding.getDouble("bearing"));

                log.info("From Server || H: {}, W: {}, B: {}", height, width, bearing);

                GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(result))
                        .position(anchor, width, height);

                overlayHelper.setGroundOverlay(overlayHelper.getMapsActivity().getMap().addGroundOverlay(overlayOptions));
                overlayHelper.getGroundOverlay().setBearing(bearing);

                overlayHelper.getMapsActivity().getDeadReckoning().setGroundOverlay(overlayHelper.getGroundOverlay());
                overlayHelper.setCurrentBuilding(thisBuilding);

                for (GroundOverlay g : overlayHelper.getOverlaysHash().values()) {
                    g.remove();
                }

                GroundOverlay temp = overlayHelper.getMapsActivity().getMap().addGroundOverlay(overlayOptions);
                overlayHelper.getOverlaysHash().put(anchor, temp);
                temp.setBearing(bearing);

                overlayHelper.transitionToIndoor();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

}