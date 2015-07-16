package com.example.krisma.architek.particlefilter;

import android.location.Location;
import android.location.LocationManager;

/**
 * Created by smp on 14/07/15.
 */
public class Pose {

    private double imageX;
    private double imageY;

    private double heading;

    public Pose(double y, double x, double heading){
        this.imageX = x;
        this.imageY = y;
        this.heading = heading;
    }

    public void setHeading(float heading){
        this.heading = heading;
    }

    public double getHeading() {
        return heading;
    }

    public double getImageY() {
        return imageY;
    }

    public void setImageY(double imageY) {
        this.imageY = imageY;
    }

    public double getImageX() {
        return imageX;
    }

    public void setImageX(double imageX) {
        this.imageX = imageX;
    }
}
