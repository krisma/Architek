package com.example.krisma.architek.deadreckoning.utils;

import android.graphics.Point;

import com.example.krisma.architek.storing.model.Building;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Jama.Matrix;

/**
 * Created by smp on 18/07/15.
 */
public class Mapper {

    private static final Logger log = LoggerFactory.getLogger(Mapper.class);

    // Common Variables
    private final Point iCenter;
    private final LatLng oCenter;
    private final LatLng oNW;
    private final float bearing;

    // Variables used to map a Point to LatLng
    private double a;
    private double b;
    private double c;
    private double d;

    public Mapper(Point iCenter, LatLng oCenter, Building building, float bearing) throws JSONException {
        this.iCenter = iCenter;
        this.oCenter = oCenter;
        this.bearing = bearing;

        log.info("Top Left (C1): {}, Top Right (C2): {}, Bottom Right (C3): {}, Bottom Left (C4): {}", building.C1, building.C2, building.C3, building.C4);

        this.oNW = building.C1;
        solveTransformation();
    }

    public static double headingFromTo(LatLng from, LatLng to){
        double heading = Math.toDegrees(Math.atan2(
                Math.sin(to.longitude - from.longitude) * Math.cos(to.latitude),
                Math.cos(from.latitude) * Math.sin(to.latitude) - Math.sin(from.latitude) * Math.cos(to.latitude) * Math.cos(to.longitude - from.longitude)

        ));
        return heading;
    }

    public Point latlngToPoint( LatLng pos){
        double lat = pos.latitude;
        double lng = pos.longitude;

        double x = a * lng + b * lat + c;
        double y = b * lng - a * lat + d;

        int xOffset = 0;
        int yOffset = 0;

        return new Point((int) x - xOffset,(int) y - yOffset);
    }

    private void solveTransformation(){
        double[] uArray = {
                0,
                0,
                iCenter.x,
                iCenter.y
        };
        Matrix u = new Matrix(uArray, 4);

        double[][] array = {
                {oNW.longitude,     oNW.latitude,       1,  0},
                {-oNW.latitude,     oNW.longitude,      0,  1},
                {oCenter.longitude, oCenter.latitude,   1,  0},
                {-oCenter.latitude, oCenter.longitude,  0,  1}
        };
        Matrix m = new Matrix(array);

        Matrix v = m.inverse().times(u);

        a = v.get(0,0);
        b = v.get(1,0);
        c = v.get(2,0);
        d = v.get(3,0);
    }

    public LatLng pointToLatLng(Point pos){

        double x = pos.x;
        double y = pos.y;

        double pos_x = (a * x + b * y - b * d - a * c) / (a * a + b * b) ;
        double pos_y = (b * x - a * y - b * c + a * d) / (a * a + b * b) ;

        return new LatLng(pos_y, pos_x);
    }

    public double rotateLatitudeAround(double lat, double angle, LatLng center) {
        double latitude = center.latitude + (Math.cos(Math.toRadians(angle)) * (lat - center.latitude) - Math.sin(Math.toRadians(angle)) * (lat - center.latitude));
        return latitude;
    }

    public double rotateLongitudeAround(double lon, double angle, LatLng center) {
        double longitude = center.longitude + (Math.sin(Math.toRadians(angle)) * (lon - center.longitude) + Math.cos(Math.toRadians(angle)) * (lon - center.longitude));
        return longitude;
    }

    public Point latLngToRotatedPoint(LatLng loc){
        LatLng rotatedLoc = new LatLng(rotateLatitudeAround(loc.latitude, bearing, oCenter), rotateLongitudeAround(loc.longitude, bearing, oCenter));
        return latlngToPoint(rotatedLoc);
    }

    public LatLng pointToRotatedLatLng(Point pos){
        LatLng loc = pointToLatLng(pos);
        LatLng rotatedLoc = new LatLng(rotateLatitudeAround(loc.latitude, -bearing, oCenter), rotateLongitudeAround(loc.longitude, -bearing, oCenter));
        return rotatedLoc;
    }

}
