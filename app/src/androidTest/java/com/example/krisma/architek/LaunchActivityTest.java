package com.example.krisma.architek;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.test.ActivityUnitTestCase;
import android.util.Log;
import android.widget.Button;

import com.example.krisma.architek.utils.Mapper;
import com.google.android.gms.maps.model.LatLng;

public class LaunchActivityTest
        extends ActivityUnitTestCase<MapsActivity> {


    private Mapper mapper;

    public LaunchActivityTest(Class<MapsActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(),
                    R.drawable.wheeler11_edged, options);

            //bitmap = BitmapFactory.decodeStream(mapsActivity.getCurrentOverlayURL().openConnection().getInputStream());

        } catch (Exception e) {
            e.printStackTrace();
        }


        // Transformation to the Image

        Point iCenter = new Point(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Point iNW = new Point(0, 0);


        LatLng oCenter = new LatLng(43.693454, 7.236123); //LatLng oCenter = mapsActivity.getCurrentOverlayObject().getPosition(); TODO: debug


        LatLng oNW = new LatLng(43.693504, 7.235905); // LatLng oNW = getNorthWestCorner();  TODO: debug

        mapper = new Mapper(iCenter, iNW, oCenter, oNW);

        double[] xy = mapper.forward(new LatLng(0,0));
        Log.d("Debugger", "x: " + xy[0] + " , Y: " + xy[1]);
    }
}