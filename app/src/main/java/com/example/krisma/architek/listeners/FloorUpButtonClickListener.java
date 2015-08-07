package com.example.krisma.architek.listeners;

import android.view.View;

import com.example.krisma.architek.MapsActivity;

public class FloorUpButtonClickListener {
    View.OnClickListener plusButtonClickListener;

    public View.OnClickListener getPlusButtonClickListener() {
        return plusButtonClickListener;
    }

    public FloorUpButtonClickListener(final MapsActivity mapsActivity) {
        this.plusButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapsActivity.getOverlayHelper().getCurrentFloor() < mapsActivity.getOverlayHelper().getCurrentFloorNumbers()) { // TODO: Should not exceed max floors
                    mapsActivity.getOverlayHelper().setCurrentFloor(mapsActivity.getOverlayHelper().getCurrentFloor() + 1);

                    mapsActivity.getFloorView().setText(String.valueOf(mapsActivity.getOverlayHelper().getCurrentFloor()));

                    mapsActivity.getOverlayHelper().changeFloor(mapsActivity.getOverlayHelper().getCurrentFloor());
                }
            }
        };
    }
}