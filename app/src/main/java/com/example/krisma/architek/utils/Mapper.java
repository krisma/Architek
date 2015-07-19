package com.example.krisma.architek.utils;

import android.graphics.Point;

import com.google.android.gms.maps.model.LatLng;

import Jama.Matrix;

/**
 * Created by smp on 18/07/15.
 */
public class Mapper {

    // Common Variables
    private final Point iCenter;
    private final Point iNW;
    private final LatLng oCenter;
    private final LatLng oNW;

    // Variables used to map LatLng to a Point
    double lat_lng_to_point_a;
    double lat_lng_to_point_b;
    double lat_lng_to_point_c;
    double lat_lng_to_point_d;

    // Variables used to map a Point to LatLng
    private double point_to_lat_lng_a;
    private double point_to_lat_lng_b;
    private double point_to_lat_lng_c;
    private double point_to_lat_lng_d;

    public Mapper(Point iCenter, Point iNW, LatLng oCenter, LatLng oNW){
        this.iCenter = iCenter;
        this.iNW = iNW;
        this.oCenter = oCenter;
        this.oNW = oNW;

        solveTransformationLatToPoint();
        solveTransformationPointToLat();
    }

    private void solveTransformationLatToPoint(){
        double[] uArray = {oCenter.longitude,oCenter.latitude,oNW.longitude,oNW.latitude};
        Matrix u = new Matrix(uArray, 4);

        double[][] array = {{iCenter.x,iCenter.y,1, 0},{-iCenter.y,iCenter.x,0,1},{iNW.x,iNW.y,1, 0},{-iNW.y,iNW.x,0,1}};
        Matrix m = new Matrix(array);

        Matrix v = m.inverse().times(u);

        lat_lng_to_point_a = v.get(0,0);
        lat_lng_to_point_b = v.get(1,0);
        lat_lng_to_point_c = v.get(2,0);
        lat_lng_to_point_d = v.get(3,0);
    }

    public Point latlngToPoint( LatLng pos){

        // scale to isometric units
        double lng_to_lat_scale = 1.409;

        double pos_iso_lat = pos.latitude;
        double pos_iso_lng = pos.longitude *  lng_to_lat_scale;

        // TODO: Scale Lat/Lng to be equal
        double x = lat_lng_to_point_a * pos_iso_lng + lat_lng_to_point_b * pos_iso_lat + lat_lng_to_point_c;
        double y = lat_lng_to_point_b * pos_iso_lng - lat_lng_to_point_a * pos_iso_lat + lat_lng_to_point_d;

        return new Point((int)x,(int)y);
    }

    private void solveTransformationPointToLat(){
        double[] uArray = {iCenter.x,iCenter.y,iNW.x,iNW.y};
        Matrix u = new Matrix(uArray, 4);

        double[][] array = {{oCenter.longitude,oCenter.latitude,1, 0},{-oCenter.latitude,oCenter.longitude,0,1},{oNW.longitude,oNW.latitude,1, 0},{-oNW.latitude,oNW.longitude,0,1}};
        Matrix m = new Matrix(array);

        Matrix v = m.inverse().times(u);

        point_to_lat_lng_a = v.get(0,0);
        point_to_lat_lng_b = v.get(1,0);
        point_to_lat_lng_c = v.get(2,0);
        point_to_lat_lng_d = v.get(3,0);
    }

    public LatLng pointToLatLng(Point pos){

        double x = point_to_lat_lng_a * pos.x + point_to_lat_lng_b * pos.y + point_to_lat_lng_c;
        double y = point_to_lat_lng_b * pos.x - point_to_lat_lng_a * pos.y + point_to_lat_lng_d;

        // scale to isometric units
        double lng_to_lat_scale = 1.409;

        double pos_iso_lat = pos.y;
        double pos_iso_lng = pos.x /  lng_to_lat_scale;

        return new LatLng(pos_iso_lat, pos_iso_lng);
    }

}
