package com.example.krisma.architek.listeners;

import android.view.View;

import com.example.krisma.architek.MapsActivity;

public class FloorDownButtonClickListener {
    View.OnClickListener minusButtonClickListener;

    public View.OnClickListener getMinusButtonClickListener() {
        return minusButtonClickListener;
    }

    public FloorDownButtonClickListener(final MapsActivity mapsActivity) {
        this.minusButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapsActivity.getOverlayHelper().setCurrentFloor(mapsActivity.getOverlayHelper().getCurrentFloor() - 1); // TODO: should not exceed the minimum floor level
                mapsActivity.getFloorView().setText(String.valueOf(mapsActivity.getOverlayHelper().getCurrentFloor()));

                mapsActivity.getOverlayHelper().changeFloor(mapsActivity.getOverlayHelper().getCurrentFloor());
            }
        };
    }
}