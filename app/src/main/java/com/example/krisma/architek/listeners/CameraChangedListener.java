package com.example.krisma.architek.listeners;

import android.os.AsyncTask;
import android.util.Log;

import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.asynctasks.AsyncSetFocusBuilding;
import com.example.krisma.architek.asynctasks.AsyncTaskListener;
import com.example.krisma.architek.asynctasks.AsyncUpdateCoordinatesHash;
import com.example.krisma.architek.deadreckoning.utils.Logger;
import com.example.krisma.architek.tools.OverlayHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class CameraChangedListener {

    private final MapsActivity mapsActivity;
    private GoogleMap.OnCameraChangeListener cameraListener;
    private OverlayHelper helper;
    private LatLngBounds updatedBounds;
    private Boolean updated = false;
    private AsyncUpdateCoordinatesHash task;
    Logger log = new Logger(CameraChangedListener.class);

    public GoogleMap.OnCameraChangeListener getCameraListener() {
        return cameraListener;
    }

    public CameraChangedListener(final MapsActivity mapsActivity) {
        this.mapsActivity = mapsActivity;
        this.helper = mapsActivity.getOverlayHelper();

        log.disableLogging();

        log.d("Created");

        this.cameraListener = new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(final CameraPosition cameraPosition) {
                log.d("Changed");
                if (helper == null) {
                    helper = mapsActivity.getOverlayHelper();
                    if (helper == null) {
                        log.d("Helper Null");
                        return;
                    }
                }

                if (task == null) {
                    log.d("First Focus");
                    updateBuildings(cameraPosition);
                }

                if (updated) {
                    if (helper.lastMoveFarFarEnough(cameraPosition.target, mapsActivity.getLastPosition())) {
                        updated = false;
                        updateBuildings(cameraPosition);
                    }

                    if (helper.lastMoveFarEnough(cameraPosition.target, mapsActivity.getLastPosition()) ) { // updatedBounds != null && !updatedBounds.contains(cameraPosition.target)
                        if (helper.getCoordinatesHash() != null && System.currentTimeMillis() > mapsActivity.getLastOverlayDetect() + 4000) {
                            mapsActivity.setLastOverlayDetect(System.currentTimeMillis());
                            log.d("Focussing");
                            focusBuilding(cameraPosition);
                        }
                    }
                }

            }

        };
    }

    public void updateBuildings(final CameraPosition cameraPosition) {

        if (task != null && (task.getStatus().equals(AsyncTask.Status.PENDING) || task.getStatus().equals(AsyncTask.Status.RUNNING))) {
            task.cancel(true);
        }
        log.d("Updating Buildings");
        task = new AsyncUpdateCoordinatesHash(helper, cameraPosition.target, new AsyncTaskListener() {
            @Override
            public void onTaskCompleted() {
                updated = true;
                log.d("Updated Hash");
                focusBuilding(cameraPosition);
            }
        });
        task.execute();
    }


    private void focusBuilding(CameraPosition cameraPosition) {
        LatLng tmp = helper.detectOverlay(cameraPosition.target);
        if(tmp != null) {
            boolean sameBuilding = tmp.equals(helper.getCurrentBuildingLocation());

            if (!sameBuilding) {
                new AsyncSetFocusBuilding(mapsActivity).execute(tmp);
                updatedBounds = mapsActivity.getMap().getProjection().getVisibleRegion().latLngBounds;
                mapsActivity.showFloorButtons(true);
                log.d( "BUILDING DETECTED!");
            } else {
                mapsActivity.showFloorButtons(true);
                log.d("SAME BUILDING");
            }
        }else {
            mapsActivity.showFloorButtons(false);
            log.d("No Building Detected");
        }
    }
}