package com.example.krisma.architek.listeners;

import android.view.View;

import com.example.krisma.architek.MapsActivity;
import com.example.krisma.architek.asynctasks.AsyncSetFocusBuilding;
import com.example.krisma.architek.asynctasks.AsyncUpdateCoordinatesHash;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class CameraChangedListener {
    GoogleMap.OnCameraChangeListener cameraListener;

    public GoogleMap.OnCameraChangeListener getCameraListener() {
        return cameraListener;
    }

    public CameraChangedListener(final MapsActivity mapsActivity) {
        this.cameraListener = new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (mapsActivity.getOverlayHelper().lastMoveFarFarEnough(cameraPosition.target, mapsActivity.getLastPosition())) {
                    new AsyncUpdateCoordinatesHash(mapsActivity.getOverlayHelper(), cameraPosition.target).execute();
                }

                if (mapsActivity.getOverlayHelper().lastMoveFarEnough(cameraPosition.target, mapsActivity.getLastPosition())) {
                    if (mapsActivity.getOverlayHelper().getCoordinatesHash() != null && System.currentTimeMillis() > mapsActivity.getLastOverlayDetect() + 4000) {
                        mapsActivity.setLastOverlayDetect(System.currentTimeMillis());
                        LatLng tmp = mapsActivity.getOverlayHelper().detectOverlay(cameraPosition.target);

                        if (tmp != null) {
                            new AsyncSetFocusBuilding(mapsActivity).execute(tmp);
                            //overlayHelper.setFocusBuilding(tmp, MapsActivity.this);
                        } else {
                            mapsActivity.getmMinusOneButton().setVisibility(View.INVISIBLE);
                            mapsActivity.getmPlusOneButton().setVisibility(View.INVISIBLE);
                            mapsActivity.getFloorView().setVisibility(View.INVISIBLE);
                        }

                    }

                }

            }
        };
    }
}