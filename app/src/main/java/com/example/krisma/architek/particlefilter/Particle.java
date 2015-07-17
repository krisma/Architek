package com.example.krisma.architek.particlefilter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.krisma.architek.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Particle {
    private static Random rand = new Random();
    private Bitmap bitmap;
    private Context context;
    private Pose pose;
    private float weight = 1;
    List<Integer> blockingColors = new ArrayList<Integer>();


    public Particle(){
        blockingColors.add(Color.BLACK);
        blockingColors.add(Color.GRAY);
        blockingColors.add(Color.DKGRAY);
        blockingColors.add(Color.TRANSPARENT);
    }
    /**
     * Create a particle with a specific pose
     *
     * @param pose the pose
     */
    public Particle(Context context, Bitmap bitmap, Pose pose) {
        this.context = context;
        this.bitmap = bitmap;
        this.pose = pose;
        blockingColors.add(Color.BLACK);
        blockingColors.add(Color.GRAY);
        blockingColors.add(Color.DKGRAY);
        blockingColors.add(Color.TRANSPARENT);
    }

    /**
     * Set the weight for this particle
     *
     * @param weight the weight of this particle
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * Return the weight of this particle
     *
     * @return the weight
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Return the pose of this particle
     *
     * @return the pose
     */

    public Pose getPose() {
        return pose;
    }

    /**
     * Calculate the weight for this particle by looking at the color of the map directly beneath the particle.
     */
    public void calculateWeight(Bitmap floorplan) {

        double x = pose.getX();
        double y = pose.getY()+ 150;

        if(x > floorplan.getWidth()) x = floorplan.getWidth() - 1;
        if(y > floorplan.getHeight()) y = floorplan.getHeight() - 1;

        int mapColor = floorplan.getPixel((int)x, (int)y);

        if (mapColor == Color.BLACK) {
            weight = 0.1f; // Collision with wall
        } else if (mapColor == Color.WHITE) {
            weight = 0.9f; // no collision
        } else {
            weight = 0.0f; // outside of map
        }
    }



    /**
     * Apply the robot's move to the particle with a bit of random noise.
     * Only works for rotate or travel movements.
     *
     * @param move the robot's move
     */
    public void applyMove(Move move, float distanceNoiseFactor, float angleNoiseFactor) {
        // Apply Heading Noise
        pose.setHeading((float) (move.getHeading() + (angleNoiseFactor * rand.nextGaussian())));
        pose.setHeading((float) ((int) (pose.getHeading() + 0.5f) % 360));

        // Apply Location Noise
        double ym = move.getDistanceTraveled() * ((float) Math.sin(Math.toRadians(move.getHeading())));
        double xm = move.getDistanceTraveled() * ((float) Math.cos(Math.toRadians(move.getHeading())));
        xm = distanceNoiseFactor * xm * rand.nextGaussian();
        ym = distanceNoiseFactor * ym * rand.nextGaussian();


        // TODO: signs may be an issue
        //double y = pose.getY() + (180 / Math.PI) * (ym / 6378137);
        //double x = pose.getX() + (180 / Math.PI) * (xm / 6378137) / Math.cos(pose.getY());

        double y = pose.getY() + ym;
        double x = pose.getX() + xm;


        if(x > bitmap.getWidth()){
            x = bitmap.getWidth() - 1;
        }
        if(y > bitmap.getHeight()){
            y = bitmap.getHeight() -1;
        }
        pose.setX(x);
        pose.setY(y);
    }
}
