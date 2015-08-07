package com.example.krisma.architek.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.example.krisma.architek.tools.OverlayHelper;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class AsyncDrawNextFloor extends AsyncTask<String, Void, Bitmap> {
    private final OverlayHelper overlayHelper;

    public AsyncDrawNextFloor(OverlayHelper overlayHelper) {
        this.overlayHelper = overlayHelper;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        Bitmap bmp = null;
        URL url;
        try {
            url = new URL(overlayHelper.getCurrentBuildingMaps().getJSONObject(overlayHelper.getCurrentFloor()).getString("map")); // TODO: KRIS LOOK HERE -- NULL POINTER
            bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

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
        if (result != null) {
            for (GroundOverlay g : overlayHelper.getOverlaysHash().values()) {
                g.remove();
            }

            GroundOverlay toSetImage = overlayHelper.getOverlaysHash().get(overlayHelper.getCurrentBuildingLocation());
            toSetImage.setImage(BitmapDescriptorFactory.fromBitmap(result));

            overlayHelper.transitionToIndoor();
            overlayHelper.overlayCurrentFloor();
        }
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}