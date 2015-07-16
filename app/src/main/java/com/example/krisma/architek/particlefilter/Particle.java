package com.example.krisma.architek.particlefilter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a particle for the particle filtering algorithm. The state of the
 * particle is the pose, which represents a possible pose of the robot.
 * The weight represents the relative probability that the robot has this
 * pose. Weights are from 0 to 1.
 * <p/>
 * This is a version of the leJOS class MCLParticle with a light sensor used to
 * detect black or white tiles in a 2D map of black/white tiles.
 *
 * @author Ole Caprani
 * @version 22.05.15
 */
public class Particle {
    private static Random rand = new Random();
    private Pose pose;
    private float weight = 1;
    List<Integer> blockingColors = new ArrayList<Integer>();


    public Particle(){
        blockingColors.add(Color.BLACK);
        blockingColors.add(Color.GRAY);
        blockingColors.add(Color.DKGRAY);
        blockingColors.add(Color.LTGRAY);
    }
    /**
     * Create a particle with a specific pose
     *
     * @param pose the pose
     */
    public Particle(Pose pose) {
        this.pose = pose;
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

        // TODO: X and Y must be over 0
        int mapColor = floorplan.getPixel((int)pose.getImageX(), (int)pose.getImageY());

        if (blockingColors.contains(mapColor))
            weight = 0.1f; // Collision with wall
        else
            weight = 0.9f; // No collision

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
        double y = pose.getImageY() + (180 / Math.PI) * (ym / 6378137);
        double x = pose.getImageX() + (180 / Math.PI) * (xm / 6378137) / Math.cos(pose.getImageY());


        pose.setImageX(x);
        pose.setImageY(y);
    }
}
