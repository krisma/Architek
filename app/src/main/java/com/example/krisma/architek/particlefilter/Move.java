package com.example.krisma.architek.particlefilter;

/**
 * Created by smp on 14/07/15.
 */
public class Move {

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    private double heading;
    private float distance;

    public Move(float distance, double heading){
        this.distance = distance;
        this.heading = heading;
    }

    public float getDistanceTraveled() {
        return distance;
    }

    public double getHeading() {
        return heading;
    }
}
