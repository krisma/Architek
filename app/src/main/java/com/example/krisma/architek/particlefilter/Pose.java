package com.example.krisma.architek.particlefilter;

import android.location.Location;
import android.location.LocationManager;

/**
 * Created by smp on 14/07/15.
 */
public class Pose {

    private double x;
    private double y;

    private double heading;

    public Pose(double y, double x, double heading){
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public void setHeading(float heading){
        this.heading = heading;
    }

    public double getHeading() {
        return heading;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        if(y <= 0){
            this.y = 0;
        } else {
            this.y = y;
        }
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        if(x <= 0){
            this.x = 0;
        } else {
            this.x = x;
        }
    }
}
